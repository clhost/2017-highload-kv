package ru.mail.polis.netty.dao;

import org.jetbrains.annotations.NotNull;


public interface EntityDao {
    void delete(@NotNull final String key);
    void upsert(@NotNull final String key, @NotNull final byte[] value);
    byte[] get(@NotNull final String key);

    void delete(@NotNull final String key, @NotNull final String path);
    void upsert(@NotNull final String key, @NotNull final byte[] value, @NotNull final String path);
    byte[] get(@NotNull final String key, @NotNull final String path);
}
