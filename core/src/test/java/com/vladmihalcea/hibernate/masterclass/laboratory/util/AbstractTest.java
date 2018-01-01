package com.vladmihalcea.hibernate.masterclass.laboratory.util;

import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractTest {

    static {
        Thread.currentThread().setName("Alice");
    }

    protected enum LockType {
        LOCKS,
        MVLOCKS,
        MVCC
    }


    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread bob = new Thread(r);
        bob.setName("Bob");
        return bob;
    });

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());


    private EntityManagerFactory emf;
    private SessionFactory sf;

    @Before
    public void init() {
        if( nativeHibernateSessionFactoryBootstrap()) {
            sf = newSessionFactory();
        } else {
            emf = newEntityManagerFactory();
        }
    }

    @After
    public void destroy() {
        if( nativeHibernateSessionFactoryBootstrap()) {
            sf.close();
        } else {
            emf.close();
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    public SessionFactory getSessionFactory() {
        return nativeHibernateSessionFactoryBootstrap() ? sf : emf.unwrap( SessionFactory.class);
    }
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    protected abstract Class<?>[] entities();

    protected List<String> entityClassNames() {
        return Arrays.asList(entities()).stream().map(Class::getName).collect(Collectors.toList());
    }

    protected String[] packages() {
        return null;
    }

    protected Interceptor interceptor() {
        return null;
    }

    private SessionFactory newSessionFactory() {
        Properties properties = getProperties();
        Configuration configuration = new Configuration().addProperties(properties).configure();

        for(Class<?> entityClass : entities()) {
            configuration.addAnnotatedClass(entityClass);
        }

        String[] packages = packages();
        if(packages != null) {
            for(String scannedPackage : packages) {
                configuration.addPackage(scannedPackage);
            }
        }
        Interceptor interceptor = interceptor();
        if(interceptor != null) {
            configuration.setInterceptor(interceptor);
        }
        return configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder()
                        .applySettings(properties)
                        .build()
        );
    }

    protected EntityManagerFactory newEntityManagerFactory() {
        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(
            getClass().getSimpleName(), entityClassNames(), getProperties()
        );

        Map<String, Object> configuration = new HashMap<>();
        configuration.put(org.hibernate.jpa.AvailableSettings.INTERCEPTOR, interceptor());
        EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration
        );
        return entityManagerFactoryBuilder.build();
    }

    protected Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", getDataSourceProvider().hibernateDialect());
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        //log settings
        //properties.put("hibernate.show_sql", Boolean.TRUE.toString());
        //properties.put("hibernate.format_sql", Boolean.TRUE.toString());
        //properties.put("hibernate.use_sql_coments", Boolean.FALSE.toString());
        properties.put("hibernate.generate_statistics", Boolean.TRUE.toString());
        
        //data source settings
        properties.put("hibernate.connection.datasource", newDataSource());
        return properties;
    }

    protected DataSource newDataSource() {
        if (proxyDataSource()) {

            ChainListener listener = new ChainListener();
            SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
            loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
            listener.addListener(loggingListener);

            return ProxyDataSourceBuilder
                    .create(getDataSourceProvider().dataSource())
                    .name(getClass().getName())
                    .listener(listener)
                    .build();
        } else {
            return getDataSourceProvider().dataSource();
        }
    }

    protected boolean proxyDataSource() {
        return true;
    }

    protected DataSourceProvider getDataSourceProvider() {
        return new HsqldbDataSourceProvider();
    }

    protected <T> T doInTransaction(HibernateTransactionFunction<T> callable) {
        T result = null;
        Session session = null;
        Transaction txn = null;
        try {
            session = getSessionFactory().openSession();
            callable.beforeTransactionCompletion();
            txn = session.beginTransaction();

            result = callable.apply(session);
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive() ) txn.rollback();
            throw e;
        } finally {
            callable.afterTransactionCompletion();
            if (session != null) {
                session.close();
            }
        }
        return result;
    }

    protected void doInTransaction(HibernateTransactionConsumer callable) {
        Session session = null;
        Transaction txn = null;
        try {
            session = getSessionFactory().openSession();
            callable.beforeTransactionCompletion();
            txn = session.beginTransaction();

            callable.accept(session);
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive() ) txn.rollback();
            throw e;
        } finally {
            callable.afterTransactionCompletion();
            if (session != null) {
                session.close();
            }
        }
    }

    protected <T> T doInJPA(JPATransactionFunction<T> function) {
        T result = null;
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = emf.createEntityManager();
            function.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            result = function.apply(entityManager);
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive()) txn.rollback();
            throw e;
        } finally {
            function.afterTransactionCompletion();
            if (entityManager != null) {
                entityManager.close();
            }
        }
        return result;
    }

    protected void doInJPA(JPATransactionVoidFunction function) {
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = emf.createEntityManager();
            function.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            function.accept(entityManager);
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive()) txn.rollback();
            throw e;
        } finally {
            function.afterTransactionCompletion();
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    protected <T> T doInJDBC(ConnectionCallable<T> callable) {
        AtomicReference<T> result = new AtomicReference<>();
        Session session = null;
        Transaction txn = null;
        try {
            session = getSessionFactory().openSession();
            txn = session.beginTransaction();
            session.doWork(connection -> {
                result.set(callable.execute(connection));
            });
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive() ) txn.rollback();
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return result.get();
    }

    protected void doInJDBC(ConnectionVoidCallable callable) {
        Session session = null;
        Transaction txn = null;
        try {
            session = getSessionFactory().openSession();
            txn = session.beginTransaction();
            session.doWork(callable::execute);
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive() ) txn.rollback();
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected void executeSync(VoidCallable callable) {
        executeSync(Collections.singleton(callable));
    }

    protected void executeSync(Collection<VoidCallable> callables) {
        try {
            List<Future<Void>> futures = executorService.invokeAll(callables);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> void executeAsync(Runnable callable, final Runnable completionCallback) {
        final Future future = executorService.submit(callable);
        new Thread(() -> {
            while (!future.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            try {
                completionCallback.run();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).start();
    }

    protected Future<?> executeAsync(Runnable callable) {
        return executorService.submit(callable);
    }

    protected  void transact(Consumer<Connection> callback) {
        transact(callback, null);
    }

    protected  void transact(Consumer<Connection> callback, Consumer<Connection> before) {
        Connection connection = null;
        try {
            connection = newDataSource().getConnection();
            if (before != null) {
                before.accept(connection);
            }
            connection.setAutoCommit(false);
            callback.accept(connection);
            connection.commit();
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new DataAccessException(e);
                }
            }
            throw (e instanceof DataAccessException ?
                    (DataAccessException) e : new DataAccessException(e));
        } finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }

    protected LockType lockType() {
        return LockType.LOCKS;
    }

    protected void awaitOnLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void sleep(int millis) {
        sleep(millis, null);
    }

    protected <V> V sleep(int millis, Callable<V> callable) {
        V result = null;
        try {
            //LOGGER.debug("Wait {} ms!", millis);
            if (callable != null) {
                result = callable.call();
            }
            Thread.sleep(millis);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    protected String selectStringColumn(Connection connection, String sql) {
        try {
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                ResultSet resultSet = statement.executeQuery(sql);
                if(!resultSet.next()) {
                    throw new IllegalArgumentException("There was no row to be selected!");
                }
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int update(Connection connection, String sql) {
        try {
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                return statement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int update(Connection connection, String sql, Object[] params) {
        try {
            try(PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(1);
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                return statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int count(Connection connection, String sql) {
        try {
            try(Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(1);
                ResultSet resultSet = statement.executeQuery(sql);
                if(!resultSet.next()) {
                    throw new IllegalArgumentException("There was no row to be selected!");
                }
                return ((Number) resultSet.getObject(1)).intValue();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void printEntityCacheStats(String region, boolean printEntries) {
		SecondLevelCacheStatistics stats = getCacheStats(region);
		LOGGER.info(region + " Stats:  \n\n\t" + stats + "\n");
		if (printEntries) {
			@SuppressWarnings("rawtypes")
			Map cacheEntries = stats.getEntries();
			LOGGER.info(Arrays.toString(cacheEntries.entrySet().toArray()));
		}
	}
	
	protected void printEntityCacheStats(String region) {
		printEntityCacheStats(region, false);
	}
	
	protected void printQueryCacheStats(String region) {
		SecondLevelCacheStatistics stats = getCacheStats(region);
		LOGGER.info(region + " Stats:  \n\n\t" + stats + "\n");
	}

	protected SecondLevelCacheStatistics getCacheStats(String region) {
		SecondLevelCacheStatistics stats = getSessionFactory().getStatistics().getSecondLevelCacheStatistics(region);
		if (stats == null){
			LOGGER.warn("No such cache:  " + region);
		}
		return stats;
	}
    
	protected void print2LCRegionNames(){
		String[] arr = getSessionFactory().getStatistics().getSecondLevelCacheRegionNames();

		LOGGER.info("2LC Region names:");
		for (String rn : arr) {
			LOGGER.info("\t --->" + rn);
		}		
	}

    ;
}