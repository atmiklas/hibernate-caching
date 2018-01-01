package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import org.hsqldb.jdbc.JDBCDataSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by amik on 12/26/17.
 */
public class HsqldbDataSourceProvider implements DataSourceProvider {

    @Override
    public String hibernateDialect() {
        return "org.hibernate.dialect.HSQLDialect";
    }

    @Override
    public DataSource dataSource() {
        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:test");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return JDBCDataSource.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("url", "jdbc:hsqldb:mem:test");
        properties.setProperty("user", "sa");
        properties.setProperty("password", "");
        return properties;
    }

    @Override
    public List<IdentifierStrategy> identifierStrategies() {
        return Arrays.asList(IdentifierStrategy.IDENTITY, IdentifierStrategy.SEQUENCE);
    }

    @Override
    public Database database() {
        return Database.HSQLDB;
    }
}
