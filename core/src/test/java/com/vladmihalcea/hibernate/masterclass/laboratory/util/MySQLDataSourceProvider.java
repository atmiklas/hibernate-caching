package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by amik on 12/26/17.
 */
public class MySQLDataSourceProvider implements DataSourceProvider {

    private boolean rewriteBatchedStatements = true;

    private boolean cachePrepStmts = false;

    private boolean useServerPrepStmts = false;

    public boolean isRewriteBatchedStatements() {
        return rewriteBatchedStatements;
    }

    public void setRewriteBatchedStatements(boolean rewriteBatchedStatements) {
        this.rewriteBatchedStatements = rewriteBatchedStatements;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public void setCachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
    }

    public boolean isUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public void setUseServerPrepStmts(boolean useServerPrepStmts) {
        this.useServerPrepStmts = useServerPrepStmts;
    }

    @Override
    public String hibernateDialect() {
        return "org.hibernate.dialect.MySQL5Dialect";
    }

    @Override
    public DataSource dataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://localhost/high_performance_java_persistence?user=mysql&password=admin" +
                "&rewriteBatchedStatements=" + rewriteBatchedStatements +
                "&cachePrepStmts=" + cachePrepStmts +
                "&useServerPrepStmts=" + useServerPrepStmts
        );
        return dataSource;
    }

    @Override
    public Class<? extends DataSource> dataSourceClassName() {
        return MysqlDataSource.class;
    }

    @Override
    public Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("url", "jdbc:mysql://localhost/high_performance_java_persistence?user=mysql&password=admin");
        return properties;
    }

    @Override
    public List<IdentifierStrategy> identifierStrategies() {
        return Arrays.asList(IdentifierStrategy.IDENTITY);
    }

    @Override
    public Database database() {
        return Database.MYSQL;
    }

    @Override
    public String toString() {
        return "MySQLDataSourceProvider{" +
                "rewriteBatchedStatements=" + rewriteBatchedStatements +
                ", cachePrepStmts=" + cachePrepStmts +
                ", useServerPrepStmts=" + useServerPrepStmts +
                '}';
    }
}
