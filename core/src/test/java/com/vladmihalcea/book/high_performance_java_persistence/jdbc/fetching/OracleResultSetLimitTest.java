package com.vladmihalcea.book.high_performance_java_persistence.jdbc.fetching;

import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.BatchEntityProvider;
import com.vladmihalcea.hibernate.masterclass.laboratory.util.DataSourceProviderIntegrationTest;
import org.hibernate.engine.spi.RowSelection;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * OracleResultSetLimitTest - Test limiting result set vs fetching and discarding rows
 *
 * @author Vlad Mihalcea
 */
public class OracleResultSetLimitTest extends DataSourceProviderIntegrationTest {
    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String SELECT_POST =
        "SELECT p.id AS p_id  " +
        "FROM post p ";

    private BatchEntityProvider entityProvider = new BatchEntityProvider();

    public OracleResultSetLimitTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        List<DataSourceProvider[]> providers = new ArrayList<>();
        providers.add(new DataSourceProvider[]{new OracleDataSourceProvider()});
        return providers;
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    public void init() {
        super.init();
        doInConnection(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
            ) {
                int postCount = getPostCount();

                int index;

                for (int i = 0; i < postCount; i++) {
                    index = 0;
                    postStatement.setString(++index, String.format("Post no. %1$d", i));
                    postStatement.setInt(++index, 0);
                    postStatement.setLong(++index, i);
                    postStatement.addBatch();
                    if(i % 100 == 0) {
                        postStatement.executeBatch();
                    }
                }
                postStatement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testLimit() {
        RowSelection rowSelection = new RowSelection();
        rowSelection.setMaxRows(getMaxRows());
        long startNanos = System.nanoTime();
        doInConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_POST)
            ) {
                statement.setMaxRows(getMaxRows());
                assertEquals(getMaxRows(), processResultSet(statement));
            } catch (SQLException e) {
                fail(e.getMessage());
            }

        });
        LOGGER.info("{} Result Set with limit took {} millis",
                getDataSourceProvider().database(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    protected int processResultSet(PreparedStatement statement) throws SQLException {
        statement.execute();
        int count = 0;
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            resultSet.getLong(1);
            count++;
        }
        return count;
    }

    protected int getPostCount() {
        return 100;
    }

    protected int getPostCommentCount() {
        return 10;
    }

    protected int getMaxRows() {
        return 5;
    }


    @Override
    protected boolean proxyDataSource() {
        return false;
    }
}
