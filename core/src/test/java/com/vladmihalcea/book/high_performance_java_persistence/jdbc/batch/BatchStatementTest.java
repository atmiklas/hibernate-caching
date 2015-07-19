package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import org.junit.runners.Parameterized;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * BatchStatementTest - Test batching with Statements
 *
 * @author Vlad Mihalcea
 */
public class BatchStatementTest extends AbstractBatchStatementTest {

    public BatchStatementTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();
        providers.add(new DataSourceProvider[]{new PostgreSQLDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new OracleDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new MySQLDataSourceProvider()});
        providers.add(new DataSourceProvider[]{new SQLServerDataSourceProvider()});
        return providers;
    }

    @Override
    protected void onStatement(Statement statement, String dml) throws SQLException {
       statement.addBatch(dml);
    }

    @Override
    protected void onEnd(Statement statement) throws SQLException {
        int[] updateCount = statement.executeBatch();
        statement.clearBatch();
    }

    @Override
    protected void onFlush(Statement statement) throws SQLException {
        statement.executeBatch();
    }

    @Override
    protected int getBatchSize() {
        return 1000;
    }
}
