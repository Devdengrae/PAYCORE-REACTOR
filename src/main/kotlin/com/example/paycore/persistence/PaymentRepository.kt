package com.example.paycore.persistence

import com.example.paycore.domain.Payment
import com.example.paycore.domain.PaymentStatus
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

@Repository
class PaymentRepository(private val db: DatabaseClient) {

    fun findByIdempotencyKey(merchantId: String, key: String): Mono<Payment> =
        db.sql("""
            SELECT id, merchant_id, amount, currency, payment_method, status, provider_reference
            FROM payments WHERE merchant_id = :merchantId AND idempotency_key = :key
        """.trimIndent())
            .bind("merchantId", merchantId)
            .bind("key", key)
            .map { row, _ ->
                Payment(
                    id = row.get("id", UUID::class.java)!!,
                    merchantId = row.get("merchant_id", String::class.java)!!,
                    amount = row.get("amount", BigDecimal::class.java)!!,
                    currency = row.get("currency", String::class.java)!!.trim(),
                    paymentMethod = row.get("payment_method", String::class.java)!!,
                    status = PaymentStatus.valueOf(row.get("status", String::class.java)!!),
                    providerReference = row.get("provider_reference", String::class.java)
                )
            }.one()

    fun insert(payment: Payment, key: String, fingerprint: String): Mono<Void> =
        db.sql("""
            INSERT INTO payments(id, merchant_id, idempotency_key, request_fingerprint,
              amount, currency, payment_method, status)
            VALUES (:id, :merchant, :key, :fingerprint, :amount, :currency, :method, :status)
        """.trimIndent())
            .bind("id", payment.id)
            .bind("merchant", payment.merchantId)
            .bind("key", key)
            .bind("fingerprint", fingerprint)
            .bind("amount", payment.amount)
            .bind("currency", payment.currency)
            .bind("method", payment.paymentMethod)
            .bind("status", payment.status.name)
            .fetch().rowsUpdated().then()

    fun updateProviderResult(id: UUID, status: PaymentStatus, reference: String): Mono<Void> =
        db.sql("UPDATE payments SET status=:status, provider_reference=:ref WHERE id=:id")
            .bind("status", status.name).bind("ref", reference).bind("id", id)
            .fetch().rowsUpdated().then()

    fun findById(id: UUID): Mono<Payment> =
        db.sql("""
            SELECT id, merchant_id, amount, currency, payment_method, status, provider_reference
            FROM payments WHERE id=:id
        """.trimIndent()).bind("id", id)
            .map { row, _ ->
                Payment(
                    row.get("id", UUID::class.java)!!,
                    row.get("merchant_id", String::class.java)!!,
                    row.get("amount", BigDecimal::class.java)!!,
                    row.get("currency", String::class.java)!!.trim(),
                    row.get("payment_method", String::class.java)!!,
                    PaymentStatus.valueOf(row.get("status", String::class.java)!!),
                    row.get("provider_reference", String::class.java)
                )
            }.one()
}
