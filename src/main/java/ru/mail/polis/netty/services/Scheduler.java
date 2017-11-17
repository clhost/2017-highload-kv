package ru.mail.polis.netty.services;


import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.apache.http.client.fluent.Request;
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

    public synchronized void check() { //fixme: получается синхронно
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
            writer.write(request.method() + " " + request.uri() + " " + filePath + "/" + fileName);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDelRequest(HttpRequest request) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(schedulePath, true))) {
            writer.write(request.method() + " " + request.uri());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void restoreState() { // сейчас DELETE only
        try (BufferedReader reader = new BufferedReader(new FileReader(schedulePath))) {
            String line = reader.readLine();
            if (!(line == null)) {
                String[] params = line.split(" ");
                if (params[0].equals("DELETE")) {
                    deletionSet.add(buildBasicRequest(HttpMethod.DELETE, new URI(params[1])));
                }
                if (params[0].equals("PUT")) {
                    putSet.add(new PutPair(buildBasicRequest(HttpMethod.PUT, new URI(params[1])),
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
