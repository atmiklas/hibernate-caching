package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by amik on 12/26/17.
 */
public class PostgreSQLDataSourceProvider implements DataSourceProvider {
    @Override
    public String hibernateDialect() {
        return "org.hibernate.dialect.PostgreSQL9Dialect";
    }

    @Override
    public DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName("high_performance_java_persistence");
        dataSource.setServerName("localhost");
        dataSource.setUser("postgres");
        dataSource.setPassword("admin");
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return PGSimpleDataSource.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("databaseName", "high_performance_java_persistence");
        properties.setProperty("serverName", "localhost");
        properties.setProperty("user", "postgres");
        properties.setProperty("password", "admin");
        return properties;
    }

    @Override
    public List<IdentifierStrategy> identifierStrategies() {
        return Arrays.asList(IdentifierStrategy.SEQUENCE);
    }

    @Override
    public Database database() {
        return Database.POSTGRESQL;
    }
}
