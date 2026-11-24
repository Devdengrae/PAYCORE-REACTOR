package com.example.paycore.provider

import com.example.paycore.domain.PaymentStatus
import com.example.paycore.domain.ProviderResult
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

@Component
class MockPaymentProvider : PaymentProvider {
    override fun initiate(
        paymentId: UUID, amount: BigDecimal, currency: String, method: String
    ): Mono<ProviderResult> =
        Mono.just(ProviderResult("mock-$paymentId", PaymentStatus.PENDING))
}
