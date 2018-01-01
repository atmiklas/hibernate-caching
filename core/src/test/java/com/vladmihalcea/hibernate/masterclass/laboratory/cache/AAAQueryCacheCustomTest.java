package com.vladmihalcea.hibernate.masterclass.laboratory.cache;

import com.vladmihalcea.hibernate.masterclass.laboratory.util.AbstractTest;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.management.ManagementService;
import org.hibernate.Session;
import org.junit.*;

import javax.management.MBeanServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * QueryCacheTest - Test to check the 2nd level query cache
 *
 * @author Vlad Mihalcea
 */
public class AAAQueryCacheCustomTest extends AbstractTest {

    public static final int INT = 100;

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

    @BeforeClass
    public static void mbeanInit() {

    }

    private static void registerMbean() {
        List<CacheManager> c = CacheManager.ALL_CACHE_MANAGERS;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        c.forEach((e) -> {
            ManagementService.registerMBeans(e, mBeanServer, true, true, true, true);
            System.err.println("Mbean initialized: " + e);
        });
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
    public void test2ndLevelCacheWithParametersLoop() throws Exception {
        registerMbean();
        loadCache(INT);

        System.out.println("#################################################");
        while(true) {
            TimeUnit.SECONDS.sleep(1);
            readCache(INT);
        }

//        System.out.println("#################################################");
//        System.out.println("Czekan na nacisniecie klawisza ... ");
//        System.in.read();
    }

    private void readCache(int anInt) throws Exception {

//        CacheManager c = CacheManager.getCacheManager("org.hibernate.cache.internal.StandardQueryCache");
//        Ehcache ehc = c.getEhcache("org.hibernate.cache.internal.StandardQueryCache");
//        Statistics statistics = ehc.getStatistics();
//        System.err.println(statistics);


//        net.sf.ehcache.statistics.StatisticsGateway.getLocalHeapSizeInBytes()
//        (StatisticsGateway retrieved by calling
//        net.sf.ehcache.Ehcache.getStatistics();

        loadCache(INT);
    }

    private void loadCache(int anInt) throws Exception {


        Runtime runtime = Runtime.getRuntime();
        String freeMem = getFreMemFromRuntime();

        doInTransaction(session -> {
            LOGGER.info("Query cache with basic type parameter");

            MemoryParametersHolder memoryParametersHolder = MemoryParametersHolder.builder()
                    .freeMemoryBefore(runtime.freeMemory()).maxMemory(runtime.maxMemory())
                    .totalMemory(runtime.totalMemory()).build();

            System.err.println("Free memory: " + memoryParametersHolder.getFreeMemoryBefore());

            for(int i = 0; i < anInt; i++) {
                System.err.println("###Counter: " + i);
                List<Post> posts = getLatestPostsByAuthorId(session, i);
            }
            memoryParametersHolder.setFreeMemoryAfter(runtime.freeMemory());


            System.err.println("Memory consumed: " + memoryParametersHolder.getConsumedMemory());
            System.err.println("MemoryParametersHolder: " + memoryParametersHolder);
//            assertEquals(1, posts.size());
        });
    }

    public static String getFreMemFromRuntime() throws IOException, InterruptedException
    {
        ProcessBuilder processBuilder = new ProcessBuilder("free",
                "-h");

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder processOutput = new StringBuilder();

        try (BufferedReader processOutputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));)
        {
            String readLine;

            while ((readLine = processOutputReader.readLine()) != null)
            {
                processOutput.append(readLine + System.lineSeparator());
            }

            process.waitFor();
        }

        return processOutput.toString().trim();
    }
}
