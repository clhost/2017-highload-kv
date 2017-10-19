package ru.mail.polis.netty.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.Files;

import java.io.File;
import java.io.IOException;

public class EntityService implements EntityDao {
    private Logger logger = LogManager.getLogger(EntityService.class);
    private File file;
    private String TEMP_DIRECTORY;

    public EntityService(File file) {
        this.file = file;
        TEMP_DIRECTORY = file.getAbsolutePath();
    }

    @Override
    public void delete(@NotNull String key) {
        try {
            Files.deleteFileByName(TEMP_DIRECTORY, key);
        } catch (IOException e) {
            logger.error("Can't delete file or file doesn't exist.");
        }
    }

    @Override
    public void upsert(@NotNull String key, @NotNull byte[] value) {
        try {
            Files.createFileAndWriteValue(TEMP_DIRECTORY, key, value);
        } catch (IOException e) {
            logger.error("Can't create or rewrite file.");
        }
    }

    @Override
    public byte[] get(@NotNull String key) throws IllegalArgumentException {
        byte[] bytes = null;
        try {
            bytes = Files.getValueFromFile(TEMP_DIRECTORY, key);
        } catch (IOException e) {
            logger.error("File " + key + " doesn't exist.");
        }
        return bytes;
    }
}
