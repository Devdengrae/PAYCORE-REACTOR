package com.example.paycore.provider

import com.example.paycore.domain.ProviderResult
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

interface PaymentProvider {
    fun initiate(paymentId: UUID, amount: BigDecimal, currency: String, method: String): Mono<ProviderResult>
}
