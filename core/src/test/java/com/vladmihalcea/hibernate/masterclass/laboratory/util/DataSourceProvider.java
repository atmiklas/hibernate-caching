package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

/**
 * Created by amik on 12/26/17.
 */
interface DataSourceProvider {

    enum IdentifierStrategy {
        IDENTITY,
        SEQUENCE
    }

    enum Database {
        HSQLDB,
        POSTGRESQL,
        ORACLE,
        MYSQL,
        SQLSERVER
    }

    String hibernateDialect();

    DataSource dataSource();

    Class<? extends DataSource> dataSourceClassName();

    Properties dataSourceProperties();

    List<IdentifierStrategy> identifierStrategies();

    Database database();
}
