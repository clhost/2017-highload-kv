package ru.mail.polis.netty.services.server_side_processing;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Reader {
    private final String filePath;

    Reader(@NotNull final String filePath) {
        this.filePath = filePath;
    }

    public List<String> read() {
        ArrayList<String> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            // do nothing
        }
        return list;
    }
}
