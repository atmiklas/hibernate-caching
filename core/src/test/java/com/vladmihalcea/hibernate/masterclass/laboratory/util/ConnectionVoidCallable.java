package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by amik on 12/26/17.
 */
@FunctionalInterface
public interface ConnectionVoidCallable {
    void execute(Connection connection) throws SQLException;
}
