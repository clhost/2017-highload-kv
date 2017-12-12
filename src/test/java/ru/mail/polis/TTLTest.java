package ru.mail.polis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TTLTest extends ClusterTestBase {
    @Rule
    public final Timeout globalTimeout = Timeout.seconds(15);

    @Test
    public void twoNodesSingleKey() throws Exception {
        int port0 = randomPort();
        int port1 = randomPort();
        File data0 = Files.createTempDirectory();
        File data1 = Files.createTempDirectory();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1)));
        KVService storage0 = KVServiceFactory.create(port0, data0, endpoints);
        KVService storage1 = KVServiceFactory.create(port1, data1, endpoints);

        storage0.start();
        storage1.start();

        final String key = "bihrvdfomkpd12352WFDAD";
        upsertWithTtl(0, key, randomValue(), 2, 2, 5000).getStatusLine().getStatusCode();
        Thread.sleep(7000);

        assertEquals(404, get(0, key, 2, 2).getStatusLine().getStatusCode());

        storage0.stop();
        storage1.stop();
    }

    @Test
    public void singleKeyWithUnreachableNode() throws Exception {
        int port0 = randomPort();
        int port1 = randomPort();
        File data0 = Files.createTempDirectory();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1)));
        KVService storage0 = KVServiceFactory.create(port0, data0, endpoints);

        storage0.start();

        final String key = "bihrvdfomkpd12352WFDAD";
        upsertWithTtl(0, key, randomValue(), 2, 2, 5000).getStatusLine().getStatusCode();
        Thread.sleep(7000);

        assertEquals(404, get(0, key, 1, 2).getStatusLine().getStatusCode());

        storage0.stop();
    }


    @Test
    public void singleKeyWithStopNode() throws Exception {
        int port0 = randomPort();
        int port1 = randomPort();
        File data0 = Files.createTempDirectory();
        File data1 = Files.createTempDirectory();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1)));
        KVService storage0 = KVServiceFactory.create(port0, data0, endpoints);
        KVService storage1 = KVServiceFactory.create(port1, data1, endpoints);

        storage0.start();
        storage1.start();

        final String key = "bihrvdfomkpd12352WFDAD";
        upsertWithTtl(0, key, randomValue(), 2, 2, 5000).getStatusLine().getStatusCode();
        storage1.stop();
        Thread.sleep(2000);

        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();
        Thread.sleep(5000);

        assertEquals(404, get(0, key, 2, 2).getStatusLine().getStatusCode());

        storage0.stop();
        storage1.stop();
    }


    @Test
    public void threeNodes30Keys() throws Exception {
        int port0 = randomPort();
        int port1 = randomPort();
        int port2 = randomPort();
        File data0 = Files.createTempDirectory();
        File data1 = Files.createTempDirectory();
        File data2 = Files.createTempDirectory();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1), endpoint(port2)));
        KVService storage0 = KVServiceFactory.create(port0, data0, endpoints);
        KVService storage1 = KVServiceFactory.create(port1, data1, endpoints);
        KVService storage2 = KVServiceFactory.create(port2, data2, endpoints);

        storage0.start();
        storage1.start();
        storage2.start();

        for (int i = 0; i < 10; i++) {
            upsertWithTtl(0, "1213" + i, randomValue(), 3, 3, 2000)
                    .getStatusLine()
                    .getStatusCode();
            upsertWithTtl(1, "1214" + i, randomValue(), 3, 3, 2000)
                    .getStatusLine()
                    .getStatusCode();
            upsertWithTtl(2, "1215" + i, randomValue(), 3, 3, 2000)
                    .getStatusLine()
                    .getStatusCode();
            Thread.sleep(200);
        }

        assertEquals(404, get(0, "1213" + new Random().nextInt(9), 3, 3)
                .getStatusLine()
                .getStatusCode());

        storage0.stop();
        storage1.stop();
        storage2.stop();
    }
}
