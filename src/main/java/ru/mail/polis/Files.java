package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility classes for handling files
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public final class Files {
    private static final String TEMP_PREFIX = "highload-kv";

    private Files() {
        // Don't instantiate
    }

    public static File createTempDirectory() throws IOException {
        final File data = java.nio.file.Files.createTempDirectory(TEMP_PREFIX).toFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (data.exists()) {
                    recursiveDelete(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return data;
    }

    public static void createFileAndWriteValue(@NotNull final String path, @NotNull final String name,
                                               @NotNull final byte[] value) throws IOException {
        File file = new File(path + File.separator + name);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file.getAbsolutePath())) {
            fileOutputStream.write(value);
            fileOutputStream.flush();
        }
    }

    public static void deleteFileByName(@NotNull final String path, @NotNull final String name) throws IOException {
        File file = new File(path + File.separator + name);
        recursiveDelete(file);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static byte[] getValueFromFile(@NotNull final String path, @NotNull final String name) throws IOException {
        File file = new File(path + File.separator + name);

        byte[] bytes;
        try (FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath())) {
            bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes, 0, fileInputStream.available());
        }

        return bytes;
    }

    static void recursiveDelete(@NotNull final File path) throws IOException {
        java.nio.file.Files.walkFileTree(
                path.toPath(),
                new SimpleFileVisitor<Path>() {
                    private void remove(@NotNull final Path file) throws IOException {
                        if (!file.toFile().delete()) {
                            throw new IOException("Can't delete " + file);
                        }
                    }

                    @NotNull
                    @Override
                    public FileVisitResult visitFile(
                            @NotNull final Path file,
                            @NotNull final BasicFileAttributes attrs) throws IOException {
                        remove(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        remove(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
