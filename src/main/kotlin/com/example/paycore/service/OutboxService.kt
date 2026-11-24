package com.example.paycore.service

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class OutboxService(private val db: DatabaseClient) {
    fun save(aggregateId: UUID, eventType: String, payload: String): Mono<Void> =
        db.sql("""
            INSERT INTO outbox_events(id, aggregate_id, event_type, payload)
            VALUES (:id, :aggregateId, :eventType, :payload)
        """.trimIndent())
            .bind("id", UUID.randomUUID())
            .bind("aggregateId", aggregateId)
            .bind("eventType", eventType)
            .bind("payload", payload)
            .fetch().rowsUpdated().then()
}
