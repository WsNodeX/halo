package halo.test;

import halo.cli.Cli;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestCli {

    @Test
    void versionExitsZero() throws IOException {
        assertEquals(0, Cli.run(new String[]{"version"}));
    }

    @Test
    void demoExitsZero() throws IOException {
        assertEquals(0, Cli.run(new String[]{"demo"}));
    }

    @Test
    void indexThenQueryRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("halo-cli");
        Files.writeString(dir.resolve("note.txt"), "the JVM garbage collector reclaims memory automatically");
        File out = File.createTempFile("halo-cli-collection", ".json");

        assertEquals(0, Cli.run(new String[]{"index", dir.toString(), out.getAbsolutePath()}));

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            assertEquals(0, Cli.run(new String[]{"query", out.getAbsolutePath(), "garbage collector", "2"}));
        } finally {
            System.setOut(original);
        }
        String output = buf.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("note.txt"), "query output should reference the indexed file");
    }
}
