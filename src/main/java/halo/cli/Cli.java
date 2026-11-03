package halo.cli;

import halo.embed.HashEmbedder;
import halo.rag.RagEngine;
import halo.rag.StubLlm;
import halo.retrieval.Evaluator;
import halo.retrieval.RetrievalResult;
import halo.store.Collection;
import halo.store.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command-line front-end for Halo.
 *
 * <pre>
 *   java halo.cli.Cli version
 *   java halo.cli.Cli demo
 *   java halo.cli.Cli index   &lt;dir-of-.txt&gt; &lt;out.json&gt;
 *   java halo.cli.Cli query   &lt;collection.json&gt; "&lt;question&gt;" [topK]
 *   java halo.cli.Cli ask     &lt;collection.json&gt; "&lt;question&gt;" [topK]
 *   java halo.cli.Cli eval    &lt;collection.json&gt; &lt;queries.json&gt;
 * </pre>
 */
public final class Cli {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage: halo <version|demo|index|query|ask|eval> ...");
            System.exit(2);
            return;
        }
        try {
            int code = run(args);
            System.exit(code);
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int run(String[] args) throws IOException {
        String cmd = args[0];
        switch (cmd) {
            case "version":
                System.out.println("halo 1.0.0");
                return 0;
            case "demo":
                runDemo();
                return 0;
            case "index": {
                require(args, 3);
                index(new File(args[1]), new File(args[2]));
                return 0;
            }
            case "query": {
                require(args, 3);
                int topK = args.length > 3 ? Integer.parseInt(args[3]) : 3;
                query(new File(args[1]), args[2], topK);
                return 0;
            }
            case "ask": {
                require(args, 3);
                int topK = args.length > 3 ? Integer.parseInt(args[3]) : 3;
                ask(new File(args[1]), args[2], topK);
                return 0;
            }
            case "eval": {
                require(args, 3);
                eval(new File(args[1]), new File(args[2]));
                return 0;
            }
            default:
                System.err.println("unknown command: " + cmd);
                return 2;
        }
    }

    private static void runDemo() {
        Collection kb = new Collection("demo", new HashEmbedder(1024));
        kb.add(new Document("gc", "The JVM garbage collector reclaims unreachable objects using "
                + "generational copy and mark-sweep-compact collectors with stop-the-world pauses."));
        kb.add(new Document("raft", "Raft elects a leader that replicates a consistent log to "
                + "followers and runs a new election if the leader fails."));
        System.out.println("Indexed " + kb.documentCount() + " documents, "
                + kb.chunkCount() + " chunks.");
        for (RetrievalResult r : kb.search("JVM garbage collection", 2)) {
            System.out.printf("  %s  fused=%.3f%n", r.id(), r.score());
        }
    }

    private static void index(File dir, File out) throws IOException {
        if (!dir.isDirectory()) throw new IllegalArgumentException("not a directory: " + dir);
        Collection kb = new Collection(dir.getName(), new HashEmbedder(1024));
        List<File> files = new ArrayList<>();
        try (var stream = Files.list(dir.toPath())) {
            stream.filter(p -> p.toString().endsWith(".txt"))
                  .map(Path::toFile).sorted().forEach(files::add);
        }
        for (File f : files) {
            String text = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            kb.add(new Document(f.getName(), text, Map.of("title", f.getName())));
        }
        kb.save(out);
        System.out.println("Indexed " + kb.documentCount() + " documents into " + out);
    }

    private static void query(File coll, String q, int topK) throws IOException {
        Collection kb = Collection.load(coll, new HashEmbedder(1024));
        for (RetrievalResult r : kb.search(q, topK)) {
            System.out.printf("  %-18s fused=%.3f  %s%n", r.id(), r.score(),
                    clip(r.text(), 70));
        }
    }

    private static void ask(File coll, String q, int topK) throws IOException {
        Collection kb = Collection.load(coll, new HashEmbedder(1024));
        RagEngine rag = new RagEngine(kb, new StubLlm());
        rag.setTopK(topK);
        System.out.println(rag.ask(q).answer);
    }

    private static void eval(File coll, File queriesFile) throws IOException {
        Collection kb = Collection.load(coll, new HashEmbedder(1024));
        List<?> raw = halo.util.Json.readArray(
                Files.readString(queriesFile.toPath(), StandardCharsets.UTF_8));
        List<Evaluator.Query> queries = Evaluator.parseQueries(raw);
        System.out.println("  " + Evaluator.evaluate(kb, queries, 3));
    }

    private static String clip(String s, int n) {
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static void require(String[] args, int n) {
        if (args.length < n) throw new IllegalArgumentException("missing arguments for " + args[0]);
    }
}
