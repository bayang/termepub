package me.myself.termepub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jline.builtins.Source;

public class StringSource implements Source {

    final String content;
    final String name;

    public StringSource(String content, String name) {
        this.content = content;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream read() throws IOException {
        return new ByteArrayInputStream(content.getBytes());
    }

}
