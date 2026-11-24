package com.example.paycore.api

import com.example.paycore.domain.CreatePaymentRequest
import com.example.paycore.domain.Payment
import com.example.paycore.domain.ProviderCallback
import com.example.paycore.service.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

data class CreatePaymentBody(
    val merchantId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentMethod: String
)
data class CallbackBody(val providerEventId: String, val paymentId: UUID, val status: String)

@RestController
@RequestMapping("/api/v1")
class PaymentController(private val service: PaymentService) {

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("Idempotency-Key") key: String,
        @RequestBody body: CreatePaymentBody
    ): Mono<Payment> = service.create(
        CreatePaymentRequest(body.merchantId, body.amount, body.currency, body.paymentMethod), key
    )

    @GetMapping("/payments/{id}")
    fun get(@PathVariable id: UUID): Mono<Payment> = service.get(id)

    @PostMapping("/provider/callbacks/mock")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun callback(@RequestBody body: CallbackBody): Mono<Void> =
        service.applyCallback(ProviderCallback(
            body.providerEventId, body.paymentId,
            runCatching { com.example.paycore.domain.PaymentStatus.valueOf(body.status) }
                .getOrElse { throw IllegalArgumentException("Unknown callback status") }
        ))
}
