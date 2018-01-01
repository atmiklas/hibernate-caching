package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import java.util.concurrent.Callable;

/**
 * Created by amik on 12/26/17.
 */
@FunctionalInterface
public interface VoidCallable extends Callable<Void> {

    void execute();

    default Void call() throws Exception {
        execute();
        return null;
    }
}
