package ru.mail.polis.netty.services.schedule;


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mail.polis.Files;
import ru.mail.polis.netty.utils.UriDecoder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

public class Scheduler {
    private String schedulePath;
    private String filesPath;
    private Set<HttpRequest> deletionSet = new HashSet<>();
    private Set<PutPair> putSet = new HashSet<>();

    private Logger logger = LogManager.getLogger(Scheduler.class);

    public Scheduler(String workDir) {
        this.schedulePath = workDir + "/schedule.txt";
        this.filesPath = workDir;
        File file = new File(schedulePath);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void check() {
        /*
        * Позже можно реализовать так, чтобы взаимоисключающие PUT и DELETE не выполнялись
        * */
        checkDelete();
        checkPut();
    }

    private void checkDelete() {
        Iterator<HttpRequest> iterator = deletionSet.iterator();
        while (iterator.hasNext()) {
            HttpRequest request = iterator.next();
            try {
                int code = Request.Delete(request.uri()).execute().returnResponse().getStatusLine().getStatusCode();
                if (code == 202) {
                    iterator.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkPut() {
        Iterator<PutPair> iterator = putSet.iterator();
        while (iterator.hasNext()) {
            PutPair pair = iterator.next();
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(Paths.get(pair.path));
                int code = Request.Put(pair.request.uri()).bodyByteArray(bytes).execute().returnResponse().getStatusLine().getStatusCode();
                if (code == 201) {
                    iterator.remove(); // fixme: копию файла оставлять на этой реплике или нет?
                }
            } catch (HttpHostConnectException h) {
                logger.warn("Connection refused to :" + pair.request.uri());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void save(HttpRequest request) {
        if (request != null && request.method() == HttpMethod.DELETE) {
            deletionSet.add(request);
            saveDelRequest(request);
        }

        if (request != null && request.method() == HttpMethod.PUT) {
            String fileName = UriDecoder.getParameter(request.uri(), "id");
            PutPair pair = new PutPair(request, filesPath + "/" + fileName);

            putSet.add(pair);
            savePutRequest(request, filesPath, fileName);
        }
    }

    private void savePutRequest(HttpRequest request, String filePath, String fileName) {
        // check if file exists
        File file = new File(filePath + "/" + fileName);
        if (!file.exists()) {
            try {
                ByteBuf buf = ((FullHttpRequest) request).content().retain();
                byte[] bytes = new byte[buf.readableBytes()];
                while (buf.isReadable()) {
                    bytes[buf.readerIndex()] = buf.readByte();
                }
                Files.createFileAndWriteValue(filePath, fileName, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(schedulePath, true))) {
            StringBuilder builder = new StringBuilder();
            builder.append(request.method()).
                    append(" ").
                    append(request.uri()).
                    append(" ").
                    append(filePath).
                    append("/").
                    append(fileName);

            for (Map.Entry<String, String> entry : request.headers()) {
                builder.append(" ").append(entry.getKey()).append(" ").append(entry.getValue());
            }

            writer.write(builder.toString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDelRequest(HttpRequest request) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(schedulePath, true))) {
            StringBuilder builder = new StringBuilder();
            builder.append(request.method()).
                    append(" ").
                    append(request.uri());

            for (Map.Entry<String, String> entry : request.headers()) {
                builder.append(" ").append(entry.getKey()).append(" ").append(entry.getValue());
            }

            writer.write(builder.toString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void restoreState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(schedulePath))) {
            String line = reader.readLine();
            if (!(line == null)) {
                String[] params = line.split(" ");
                if (params[0].equals("DELETE")) {
                    HttpRequest request = buildBasicRequest(HttpMethod.DELETE, new URI(params[1]));

                    int i = 2;
                    while (i <= params.length - 2) {
                        request.headers().set(params[i], params[i + 1]);
                        i += 2;
                    }

                    deletionSet.add(request);
                }
                if (params[0].equals("PUT")) {
                    HttpRequest request = buildBasicRequest(HttpMethod.PUT, new URI(params[1]));

                    int i = 3;
                    while (i <= params.length - 2) {
                        request.headers().set(params[i], params[i + 1]);
                        i += 2;
                    }

                    putSet.add(new PutPair(request,
                                           params[2]));
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private HttpRequest buildBasicRequest(HttpMethod method, URI uri) {
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, method, uri.toASCIIString()
        );
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        return request;
    }

    class PutPair {
        HttpRequest request;
        String path;

        PutPair(HttpRequest request, String path) {
            this.request = request;
            this.path = path;
        }
    }
}
