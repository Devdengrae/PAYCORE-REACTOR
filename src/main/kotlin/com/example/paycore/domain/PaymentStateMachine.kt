package com.example.paycore.domain

object PaymentStateMachine {
    private val allowed = mapOf(
        PaymentStatus.CREATED to setOf(PaymentStatus.PENDING, PaymentStatus.FAILED),
        PaymentStatus.PENDING to setOf(PaymentStatus.SUCCESS, PaymentStatus.FAILED)
    )

    fun validate(from: PaymentStatus, to: PaymentStatus) {
        require(to in allowed[from].orEmpty()) {
            "Invalid payment transition: $from -> $to"
        }
    }
}
