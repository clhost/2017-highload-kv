package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;

/**
 * Starts storage and waits for shutdown
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public final class Server {
    private static final int PORT = 1337;

    private Server() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        // Temporary storage in the file system
        final File data = Files.createTempDirectory();

        // Start the storage
        final KVService storage =
                KVServiceFactory.create(
                        PORT,
                        data,
                        Collections.singleton("http://localhost:" + PORT));
        storage.start();
        Runtime.getRuntime().addShutdownHook(new Thread(storage::stop));
    }
}
