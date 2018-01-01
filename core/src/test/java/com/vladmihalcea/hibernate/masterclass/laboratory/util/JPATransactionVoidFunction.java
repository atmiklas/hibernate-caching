package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import javax.persistence.EntityManager;
import java.util.function.Consumer;

/**
 * Created by amik on 12/26/17.
 */
@FunctionalInterface
public interface JPATransactionVoidFunction extends Consumer<EntityManager> {
    default void beforeTransactionCompletion() {

    }

    default void afterTransactionCompletion() {

    }
}
