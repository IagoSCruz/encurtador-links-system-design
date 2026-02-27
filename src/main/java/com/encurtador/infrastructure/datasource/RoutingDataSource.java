package com.encurtador.infrastructure.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DataSource roteador — decide automaticamente entre PRIMARY e RÉPLICA.
 *
 * Regra de roteamento:
 * - @Transactional(readOnly = true) → RÉPLICA (round-robin entre as 3)
 * - @Transactional (default) → PRIMARY
 *
 * Sem transação ativa → PRIMARY (fail-safe).
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    private final List<String> replicaKeys;
    private final AtomicInteger replicaCounter = new AtomicInteger(0);

    public RoutingDataSource(List<String> replicaKeys) {
        this.replicaKeys = replicaKeys;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

        if (isReadOnly && !replicaKeys.isEmpty()) {
            // Round-robin entre as réplicas
            int index = Math.abs(replicaCounter.getAndIncrement() % replicaKeys.size());
            return replicaKeys.get(index);
        }

        return DataSourceType.PRIMARY.name();
    }
}
