package com.example.paycore.domain

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class PaymentStatus { CREATED, PENDING, SUCCESS, FAILED }

data class Payment(
    val id: UUID,
    val merchantId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentMethod: String,
    val status: PaymentStatus,
    val providerReference: String? = null,
    val createdAt: OffsetDateTime? = null
)

data class CreatePaymentRequest(
    val merchantId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentMethod: String
)

data class ProviderCallback(
    val providerEventId: String,
    val paymentId: UUID,
    val status: PaymentStatus
)

data class ProviderResult(val reference: String, val initialStatus: PaymentStatus)
