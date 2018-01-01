package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import javax.persistence.EntityManager;
import java.util.function.Function;

/**
 * Created by amik on 12/26/17.
 */
@FunctionalInterface
public interface JPATransactionFunction<T> extends Function<EntityManager, T> {
    default void beforeTransactionCompletion() {

    }

    default void afterTransactionCompletion() {

    }
}
