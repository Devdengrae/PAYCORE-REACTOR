package com.example.paycore.service

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

@Service
class LedgerService(private val db: DatabaseClient) {

    // Educational capture posting:
    // Debit processor clearing (asset), credit merchant payable (liability).
    fun postCapture(paymentId: UUID, amount: BigDecimal, currency: String): Mono<Void> {
        val ledgerId = UUID.randomUUID()
        return db.sql("""
            INSERT INTO ledger_transactions(id, payment_id, transaction_type, currency)
            VALUES (:id, :paymentId, 'CAPTURE', :currency)
        """.trimIndent())
            .bind("id", ledgerId).bind("paymentId", paymentId).bind("currency", currency)
            .fetch().rowsUpdated()
            .then(insertEntry(ledgerId, "PROCESSOR_CLEARING", "DEBIT", amount, currency))
            .then(insertEntry(ledgerId, "MERCHANT_PAYABLE", "CREDIT", amount, currency))
    }

    private fun insertEntry(
        ledgerId: UUID, account: String, side: String, amount: BigDecimal, currency: String
    ): Mono<Void> =
        db.sql("""
            INSERT INTO ledger_entries(id, ledger_transaction_id, account_code, entry_side, amount, currency)
            VALUES (:id, :ledgerId, :account, :side, :amount, :currency)
        """.trimIndent())
            .bind("id", UUID.randomUUID()).bind("ledgerId", ledgerId)
            .bind("account", account).bind("side", side)
            .bind("amount", amount).bind("currency", currency)
            .fetch().rowsUpdated().then()
}
