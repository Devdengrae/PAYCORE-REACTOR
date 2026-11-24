package com.example.paycore.domain

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PaymentStateMachineTest {
    @Test fun pendingCanBecomeSuccess() {
        assertDoesNotThrow { PaymentStateMachine.validate(PaymentStatus.PENDING, PaymentStatus.SUCCESS) }
    }
    @Test fun successCannotBecomeFailed() {
        assertThrows(IllegalArgumentException::class.java) {
            PaymentStateMachine.validate(PaymentStatus.SUCCESS, PaymentStatus.FAILED)
        }
    }
}
