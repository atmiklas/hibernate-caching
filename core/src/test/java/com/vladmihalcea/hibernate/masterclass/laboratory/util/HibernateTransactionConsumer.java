package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import org.hibernate.Session;

import java.util.function.Consumer;

/**
 * Created by amik on 12/26/17.
 */
@FunctionalInterface
public interface HibernateTransactionConsumer extends Consumer<Session> {
    default void beforeTransactionCompletion() {

    }

    default void afterTransactionCompletion() {

    }
}
