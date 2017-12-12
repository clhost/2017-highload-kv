package ru.mail.polis;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    private static final int VALUE_LENGTH = 1024;
    private String endpoint = endpoint(1337);

    public static void main(String[] args) throws InterruptedException, IOException, NoSuchAlgorithmException {
        Client client = new Client();
        Thread.sleep(1000);

        for (int i = 0; i < 100; i++) {
            int code = client.upsert(randomKey(), randomValue()).getStatusLine().getStatusCode();
            System.out.println(code);
            Thread.sleep(300);
        }
    }

    @NotNull
    private static String endpoint(final int port) {
        return "http://localhost:" + port;
    }

    @NotNull
    static String randomKey() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    @NotNull
    static byte[] randomValue() {
        final byte[] result = new byte[VALUE_LENGTH];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }

    @NotNull
    private String url(@NotNull final String id) {
        return endpoint + "/v0/entity?id=" + id;
    }

    private HttpResponse get(@NotNull final String key) throws IOException {
        return Request.Get(url(key)).execute().returnResponse();
    }

    private HttpResponse delete(@NotNull final String key) throws IOException {
        return Request.Delete(url(key)).execute().returnResponse();
    }

    private HttpResponse upsert(
            @NotNull final String key,
            @NotNull final byte[] data) throws IOException {
        return Request.Put(url(key)).bodyByteArray(data).execute().returnResponse();
    }
}
