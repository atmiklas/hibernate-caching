package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import org.hibernate.Session;

import java.util.function.Function;

/**
 *
 * @param <T>
 */
@FunctionalInterface
public interface HibernateTransactionFunction<T> extends Function<Session, T> {
    default void beforeTransactionCompletion() {

    }

    default void afterTransactionCompletion() {

    }
}
