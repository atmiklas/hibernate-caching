package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import net.sourceforge.jtds.jdbcx.JtdsDataSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by amik on 12/26/17.
 */
public class JTDSDataSourceProvider implements DataSourceProvider {
    @Override
    public String hibernateDialect() {
        return "org.hibernate.dialect.SQLServer2012Dialect";
    }

    @Override
    public DataSource dataSource() {
        JtdsDataSource dataSource = new JtdsDataSource();
        dataSource.setServerName("localhost");
        dataSource.setDatabaseName("high_performance_java_persistence");
        dataSource.setInstance("SQLEXPRESS");
        dataSource.setUser("sa");
        dataSource.setPassword("adm1n");
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return JtdsDataSource.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("databaseName", "high_performance_java_persistence");
        properties.setProperty("serverName", "localhost");
        properties.setProperty("instance", "SQLEXPRESS");
        properties.setProperty("user", "sa");
        properties.setProperty("password", "adm1n");
        return properties;
    }

    @Override
    public List<IdentifierStrategy> identifierStrategies() {
        return Arrays.asList(IdentifierStrategy.IDENTITY, IdentifierStrategy.SEQUENCE);
    }

    @Override
    public Database database() {
        return Database.SQLSERVER;
    }
}
