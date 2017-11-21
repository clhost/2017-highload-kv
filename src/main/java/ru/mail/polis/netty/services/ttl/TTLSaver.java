package ru.mail.polis.netty.services.ttl;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.mail.polis.netty.services.schedule.Scheduler;
import ru.mail.polis.netty.utils.UriDecoder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class TTLSaver {
    /**
     * String - path to file
     * Long - its time to live
     */
    private ConcurrentHashMap<String, Long> ttlmap = new ConcurrentHashMap<>();
    private String filesPath;
    private String ttlDataPath;
    private Logger logger = LogManager.getLogger(TTLSaver.class);


    public TTLSaver(String workDir) {
        this.ttlDataPath = workDir + "/" + "ttl.txt";
        this.filesPath = workDir;
        File file = new File(ttlDataPath);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void accept(HttpRequest request) {
        String str = request.headers().get("Expires");
        String fileName = UriDecoder.getParameter(request.uri(), "id");

        if (str != null && fileName != null) {
            DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault());
            Date date;
            try {
                date = format.parse(str);
                long dateTime = date.getTime();

                // fixme: Если время жизни оказалось меньше текущего, нужно ли хранить такие файлы на нодах?
                if (dateTime < System.currentTimeMillis()) {
                    logger.error("Time-expired.");
                } else {
                    if (!ttlmap.containsKey(fileName)) {
                        System.out.println("saved: " + fileName);

                        saveState(fileName, dateTime);
                        ttlmap.put(fileName, dateTime);
                    }
                }
            } catch (ParseException e) {
                logger.error("Parse exception.");
            }
        }
    }

    ConcurrentHashMap<String, Long> getTtlmap() {
        return ttlmap;
    }

    void remove(String filePath) {
        System.out.println("removed: " + filePath);
        ttlmap.remove(filePath);
    }

    private void saveState(String key, Long value) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ttlDataPath, true))) {
            writer.write(key + " " + value);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ttlDataPath))) {
            String line = reader.readLine();
            if (!(line == null)) {
                String[] params = line.split(" ");
                ttlmap.put(params[0], Long.valueOf(params[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
