package com.example.paycore.service

import com.example.paycore.domain.*
import com.example.paycore.persistence.PaymentRepository
import com.example.paycore.provider.PaymentProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

@Service
class PaymentService(
    private val payments: PaymentRepository,
    private val provider: PaymentProvider,
    private val ledger: LedgerService,
    private val outbox: OutboxService,
    private val tx: TransactionalOperator,
    private val mapper: ObjectMapper
) {
    fun create(request: CreatePaymentRequest, key: String): Mono<Payment> {
        require(key.isNotBlank() && key.length <= 160) { "A valid Idempotency-Key is required" }
        require(request.amount.signum() > 0) { "Amount must be positive" }
        require(request.currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter code" }

        val fingerprint = fingerprint(request)

        // First check: if this key already exists, return the original payment.
        return payments.findByIdempotencyKey(request.merchantId, key)
            .flatMap { existing ->
                if (existingFingerprintMatches(request, key, fingerprint)) Mono.just(existing)
                else Mono.error(IllegalArgumentException("Idempotency key reused with a different request"))
            }
            .switchIfEmpty(Mono.defer {
                createNew(request, key, fingerprint)
            })
    }

    private fun createNew(request: CreatePaymentRequest, key: String, fingerprint: String): Mono<Payment> {
        val payment = Payment(
            id = UUID.randomUUID(),
            merchantId = request.merchantId,
            amount = request.amount,
            currency = request.currency,
            paymentMethod = request.paymentMethod,
            status = PaymentStatus.CREATED
        )

        return payments.insert(payment, key, fingerprint)
            .then(provider.initiate(payment.id, payment.amount, payment.currency, payment.paymentMethod))
            .flatMap { providerResult ->
                PaymentStateMachine.validate(PaymentStatus.CREATED, providerResult.initialStatus)
                payments.updateProviderResult(payment.id, providerResult.initialStatus, providerResult.reference)
                    .then(outbox.save(
                        payment.id,
                        "PaymentInitiated",
                        mapper.writeValueAsString(mapOf(
                            "paymentId" to payment.id,
                            "merchantId" to payment.merchantId,
                            "amount" to payment.amount,
                            "currency" to payment.currency,
                            "status" to providerResult.initialStatus
                        ))
                    ))
                    .then(payments.findById(payment.id))
            }
            // This wraps the DB writes in a reactive transaction. Provider calls should ideally
            // happen outside long DB transactions in a hardened production design.
            .`as`(tx::transactional)
            .onErrorMap(DuplicateKeyException::class.java) {
                IllegalStateException("A concurrent request used this key; retry with the same key")
            }
    }

    fun get(paymentId: UUID): Mono<Payment> = payments.findById(paymentId)

    // Placeholder hook: this starter stores request fingerprint in the payment table.
    // A dedicated query returning the fingerprint is a useful next exercise.
    private fun existingFingerprintMatches(
        request: CreatePaymentRequest, key: String, expected: String
    ): Boolean = true

    private fun fingerprint(request: CreatePaymentRequest): String {
        val canonical = listOf(
            request.merchantId, request.amount.stripTrailingZeros().toPlainString(),
            request.currency, request.paymentMethod
        ).joinToString("|")
        val bytes = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return HexFormat.of().formatHex(bytes)
    }

    fun applyCallback(callback: ProviderCallback): Mono<Void> {
        // Intentionally simplified callback path. Production version should atomically
        // deduplicate callback ID, lock/conditionally update payment, post ledger and write outbox.
        return payments.findById(callback.paymentId)
            .switchIfEmpty(Mono.error(IllegalArgumentException("Payment not found")))
            .flatMap { payment ->
                PaymentStateMachine.validate(payment.status, callback.status)
                payments.updateProviderResult(payment.id, callback.status, payment.providerReference ?: "mock")
                    .then(
                        if (callback.status == PaymentStatus.SUCCESS)
                            ledger.postCapture(payment.id, payment.amount, payment.currency)
                        else Mono.empty()
                    )
                    .then(outbox.save(payment.id, "PaymentStatusChanged",
                        mapper.writeValueAsString(mapOf("paymentId" to payment.id, "status" to callback.status))))
            }
            .`as`(tx::transactional)
    }
}
