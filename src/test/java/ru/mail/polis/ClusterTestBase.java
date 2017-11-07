package ru.mail.polis;

import com.google.common.collect.Iterators;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Facilities for cluster tests
 *
 * @author Vadim Tsesko <incubos@yandex.com>
 */
abstract class ClusterTestBase extends TestBase {
    Set<String> endpoints;

    @NotNull
    private String url(
            final int node,
            @NotNull final String id,
            final int ack,
            final int from) {
        final String endpoint = Iterators.get(endpoints.iterator(), node);
        /*String endpoint = null;
        if (node == 0) endpoint = endpoints.iterator().next();
        if (node == 1) {
            endpoints.iterator().next();
            endpoint = endpoints.iterator().next();
        }*/
        return endpoint + "/v0/entity?id=" + id + "&replicas=" + ack + "/" + from;
    }

    HttpResponse get(
            final int node,
            @NotNull final String key,
            final int ack,
            final int from) throws IOException {
        return Request.Get(url(node, key, ack, from)).execute().returnResponse();
    }

    HttpResponse delete(
            final int node,
            @NotNull final String key,
            final int ack,
            final int from) throws IOException {
        return Request.Delete(url(node, key, ack, from)).execute().returnResponse();
    }

    HttpResponse upsert(
            final int node,
            @NotNull final String key,
            @NotNull final byte[] data,
            final int ack,
            final int from) throws IOException {
        return Request.Put(url(node, key, ack, from)).bodyByteArray(data).execute().returnResponse();
    }
}