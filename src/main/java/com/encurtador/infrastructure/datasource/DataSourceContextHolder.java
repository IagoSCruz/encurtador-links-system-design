package com.encurtador.infrastructure.datasource;

/**
 * ThreadLocal que guarda o tipo de datasource para a transação corrente.
 * Automaticamente limpo após cada requisição para evitar vazamento de contexto.
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void setPrimary() {
        CONTEXT.set(DataSourceType.PRIMARY);
    }

    public static void setReplica() {
        CONTEXT.set(DataSourceType.REPLICA);
    }

    public static DataSourceType get() {
        DataSourceType type = CONTEXT.get();
        return type != null ? type : DataSourceType.PRIMARY;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
