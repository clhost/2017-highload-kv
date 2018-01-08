package ru.mail.polis.netty.services.server_side_processing;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;


public class Acceptor extends Thread {
    private Scanner scanner;
    private static final String COMMAND = "/script";
    private Performer performer;
    private Reader reader;

    public Acceptor() {
        scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            String[] params = scanner.nextLine().split(" ");

            if (params.length == 2) {
                if (!params[0].equals(COMMAND)) {
                    System.err.println("Syntax error: Invalid command.");
                } else {
                    File file = new File(params[1]);
                    if (file.exists() && !file.isDirectory()) {
                        reader = new Reader(params[1]);
                        performer = new Performer(reader.read());
                        performer.perform();
                    } else {
                        System.err.println("Error: File not found.");
                    }
                }
            } else {
                System.err.println("Syntax error: The number of arguments is not equal to two.");
            }
        }
    }

    public static void main(String[] args) {
        new Acceptor().start();
    }
}
