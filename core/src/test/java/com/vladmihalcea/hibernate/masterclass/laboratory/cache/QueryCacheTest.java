package com.vladmihalcea.hibernate.masterclass.laboratory.cache;

import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractTest;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * QueryCacheTest - Test to check the 2nd level query cache
 *
 * @author Vlad Mihalcea
 */
public class QueryCacheTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                Author.class,
        };
    }

    @Override
    protected Properties getProperties() {
        Properties properties = super.getProperties();
        properties.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
        properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString());

        return properties;
    }

    @Before
    public void init() {
        super.init();
        //
        // Do in transaction przyjmuje Consumera, bo nie zwraca zadnej wartosci, tylko zapisuje encje.
        // Gdyby dla sesji zwracala wartosc, to przekazalbym fukcje.
        doInTransaction(session -> {
            Author author = new Author("Vlad");
            //session.persist(author);

            Post post = new Post("Hibernate Master Class", author);
            //session.persist(post);
        });
    }

    @After
    public void destroy() {
        getSessionFactory().getCache().evictAllRegions();
        super.destroy();
    }

    @SuppressWarnings("unchecked")
    private List<Post> getLatestPosts(Session session) {
        return (List<Post>) session.createQuery(
            "select p " +
            "from Post p " +
            "order by p.createdOn desc")
            .setMaxResults(10)
            .setCacheable(true)
            .list();
    }

    @SuppressWarnings("unchecked")
    private List<Post> getLatestPostsByAuthorId(Session session, long id) {
        return (List<Post>) session.createQuery(
            "select p " +
            "from Post p " +
            "join p.author a " +
            "where a.id = :authorId " +
            "order by p.createdOn desc")
            .setParameter("authorId", id)
            .setMaxResults(10)
            .setCacheable(true)
            .list();
    }

    @SuppressWarnings("unchecked")
    private List<Post> getLatestPostsByAuthor(Session session) {
        Author author = (Author) session.get(Author.class, 1L);
        return (List<Post>) session.createQuery(
                "select p " +
                "from Post p " +
                "join p.author a " +
                "where a = :author " +
                "order by p.createdOn desc")
                .setParameter("author", author)
                .setMaxResults(10)
                .setCacheable(true)
                .list();
    }

    @Test
    @Ignore
    public void test2ndLevelCacheWithQuery() {
        doInTransaction(session -> {
            LOGGER.info("Evict regions and run query");
            session.getSessionFactory().getCache().evictAllRegions();
            assertEquals(1, getLatestPosts(session).size());
        });

        doInTransaction(session -> {
            LOGGER.info("Check get entity is cached");
            Post post = (Post) session.get(Post.class, 1L);
        });

        doInTransaction(session -> {
            LOGGER.info("Check query is cached");
            assertEquals(1, getLatestPosts(session).size());
        });
    }


    @Test
    public void test2ndLevelCacheWithParametersLoop() {

        Runtime runtime = Runtime.getRuntime();

        doInTransaction(session -> {
            LOGGER.info("Query cache with basic type parameter");

            MemoryParametersHolder memoryParametersHolder = MemoryParametersHolder.builder()
                    .freeMemoryBefore(runtime.freeMemory()).maxMemory(runtime.maxMemory())
                    .totalMemory(runtime.totalMemory()).build();

            System.err.println("Free memory: " + memoryParametersHolder.getFreeMemoryBefore());

            for(int i = 0; i < 1000; i++) {
                System.err.println("###Counter: " + i);
                List<Post> posts = getLatestPostsByAuthorId(session, i);
            }
            memoryParametersHolder.setFreeMemoryAfter(runtime.freeMemory());


            System.err.println("Memory consumed: " + memoryParametersHolder.getConsumedMemory());
            System.err.println("MemoryParametersHolder: " + memoryParametersHolder);
//            assertEquals(1, posts.size());
        });
    }

    @Test
    public void test2ndLevelCacheWithParametersLoopAgain() {

        Runtime runtime = Runtime.getRuntime();

        doInTransaction(session -> {
            LOGGER.info("Query cache with basic type parameter");

            MemoryParametersHolder memoryParametersHolder = MemoryParametersHolder.builder()
                    .freeMemoryBefore(runtime.freeMemory()).maxMemory(runtime.maxMemory())
                    .totalMemory(runtime.totalMemory()).build();

            System.err.println("Free memory: " + memoryParametersHolder.getFreeMemoryBefore());

            for(int i = 0; i < 1000; i++) {
                System.err.println("###Counter: " + i);
                List<Post> posts = getLatestPostsByAuthorId(session, i);
            }
            memoryParametersHolder.setFreeMemoryAfter(runtime.freeMemory());


            System.err.println("Memory consumed: " + memoryParametersHolder.getConsumedMemory());
            System.err.println("MemoryParametersHolder: " + memoryParametersHolder);
//            assertEquals(1, posts.size());
        });
    }


    @Test
    @Ignore
    public void test2ndLevelCacheWithParameters() {
        doInTransaction(session -> {
            LOGGER.info("Query cache with basic type parameter");
            List<Post> posts = getLatestPostsByAuthorId(session, 1L);
            assertEquals(1, posts.size());
        });
        doInTransaction(session -> {
            LOGGER.info("Query cache with entity type parameter");
            List<Post> posts = getLatestPostsByAuthor(session);
            assertEquals(1, posts.size());
        });
    }

    @Test
    @Ignore
    public void test2ndLevelCacheWithQueryInvalidation() {
        doInTransaction(session -> {
            Author author = (Author)
                session.get(Author.class, 1L);
            assertEquals(1, getLatestPosts(session).size());

            LOGGER.info("Insert a new Post");
            Post newPost = new Post("Hibernate Book", author);
            session.persist(newPost);
            session.flush();

            LOGGER.info("Query cache is invalidated");
            assertEquals(2, getLatestPosts(session).size());
        });

        doInTransaction(session -> {
            LOGGER.info("Check Query cache");
            assertEquals(2, getLatestPosts(session).size());
        });
    }

    @Test
    @Ignore
    public void test2ndLevelCacheWithNativeQueryInvalidation() {
        doInTransaction(session -> {
            assertEquals(1, getLatestPosts(session).size());

            LOGGER.info("Execute native query");
            assertEquals(1, session.createSQLQuery(
                "update Author set name = '\"'||name||'\"' "
            ).executeUpdate());

            LOGGER.info("Check query cache is invalidated");
            assertEquals(1, getLatestPosts(session).size());
        });
    }

    @Test
    @Ignore
    public void test2ndLevelCacheWithNativeQuerySynchronization() {
        doInTransaction(session -> {
            assertEquals(1, getLatestPosts(session).size());

            LOGGER.info("Execute native query with synchronization");
            assertEquals(1, session.createSQLQuery(
                    "update Author set name = '\"'||name||'\"' "
            ).addSynchronizedEntityClass(Author.class)
            .executeUpdate());

            LOGGER.info("Check query cache is not invalidated");
            assertEquals(1, getLatestPosts(session).size());
        });
    }

}
