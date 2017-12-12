package ru.mail.polis.netty.services.ttl;

import ru.mail.polis.netty.dao.EntityDao;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TTLWatcher extends Thread {
    private EntityDao dao;
    private TTLSaver ttlSaver;

    public TTLWatcher(EntityDao dao, TTLSaver ttlSaver) {
        this.ttlSaver = ttlSaver;
        this.dao = dao;
    }

    @Override
    public void run() {
        while (true) {
            long currentTime = System.currentTimeMillis();
            ConcurrentHashMap<String, Long> ttlmap = ttlSaver.getTtlmap();

            if (ttlmap == null) {
                Thread.currentThread().interrupt();
                break;
            }

            ArrayList<String> willDelete = new ArrayList<>();

            // сбор имен объектов на удаление
            if (!ttlmap.isEmpty()) {
                for (Map.Entry<String, Long> entry : ttlmap.entrySet()) {
                    if (entry.getValue() <= currentTime) {
                        String fileName = entry.getKey();
                        willDelete.add(fileName);
                    }
                }

                // удаление объектов
                for (String fileName : willDelete) {
                    ttlSaver.remove(fileName);
                    dao.delete(fileName);
                }
            }
        }
    }
}
