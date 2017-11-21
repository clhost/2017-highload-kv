package ru.mail.polis.netty.services.ttl;

import ru.mail.polis.netty.dao.EntityDao;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TTLWatcher extends Thread {
    private EntityDao dao;
    private TTLSaver ttlSaver;

    public TTLWatcher(EntityDao dao, TTLSaver ttlSaver) {
        this.dao = dao;
        this.ttlSaver = ttlSaver;
    }

    @Override
    public void run() {
        while (true) {
            long currentTime = System.currentTimeMillis();
            ConcurrentHashMap<String, Long> ttlmap = ttlSaver.getTtlmap();

            ArrayList<String> willDelete = new ArrayList<>();

            // сбор имен объектов на удаление
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
