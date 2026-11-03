package halo.examples;

import halo.embed.HashEmbedder;
import halo.rag.RagEngine;
import halo.rag.StubLlm;
import halo.retrieval.Evaluator;
import halo.retrieval.RetrievalResult;
import halo.store.Collection;
import halo.store.Document;

import java.util.Arrays;
import java.util.List;

/**
 * End-to-end showcase: build a small knowledge base, run hybrid retrieval, and
 * answer a question with retrieval-augmented generation. Run with
 * {@code java halo.examples.Demo}.
 */
public final class Demo {

    public static void main(String[] args) {
        Collection kb = new Collection("engineering-notes", new HashEmbedder(1024));
        for (Document d : SAMPLE_DOCS) kb.add(d);

        String question = "How does garbage collection work in the JVM?";
        System.out.println("== Hybrid retrieval ==");
        System.out.println("query: " + question);
        List<RetrievalResult> hits = kb.search(question, 3);
        for (RetrievalResult r : hits) {
            System.out.printf("  %-12s dense=%.3f sparse=%.3f fused=%.3f%n",
                    r.id(), r.denseScore(), r.sparseScore(), r.score());
            System.out.println("     " + clip(r.text(), 90));
        }

        System.out.println("\n== Retrieval-augmented generation ==");
        RagEngine rag = new RagEngine(kb, new StubLlm());
        RagEngine.RagAnswer ans = rag.ask(question);
        System.out.println(ans.answer);

        System.out.println("\n== Offline evaluation ==");
        List<Evaluator.Query> queries = Arrays.asList(
                new Evaluator.Query("JVM garbage collection", "jvm-gc"),
                new Evaluator.Query("distributed consensus", "raft"),
                new Evaluator.Query("database indexing b-tree", "btree"));
        System.out.println("  " + Evaluator.evaluate(kb, queries, 3));
    }

    private static String clip(String s, int n) {
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static final List<Document> SAMPLE_DOCS = Arrays.asList(
        new Document("jvm-gc",
                "The JVM garbage collector reclaims objects that are no longer reachable, so "
                + "developers do not free memory by hand. Generational collection splits the heap "
                + "into young and old spaces; the young generation is collected frequently with a "
                + "copying collector while the old generation is collected less often with a "
                + "mark-sweep-compact approach. Stop-the-world pauses occur when the collector "
                + "suspends application threads to trace live objects."),
        new Document("raft",
                "Raft is a consensus algorithm that keeps a replicated log consistent across "
                + "servers. A single elected leader accepts writes, appends them to its log, and "
                + "replicates entries to followers. If the leader fails, a new election picks a "
                + "candidate with the most up-to-date log. Consensus guarantees that once an entry "
                + "is committed it will never be lost."),
        new Document("btree",
                "A B-tree is a balanced search tree used by almost every relational database for "
                + "indexing. All leaf nodes sit at the same depth, and each internal node holds many "
                + "keys with many children, which keeps the height small. Range scans are efficient "
                + "because leaves are linked, and point lookups cost only a few disk pages."),
        new Document("vector-search",
                "Vector search finds items whose embedding is close to a query embedding. Exact "
                + "search compares against every vector, while approximate methods such as HNSW "
                + "build a navigable graph to answer in sub-linear time at the cost of a small "
                + "recall loss. Hybrid retrieval combines dense vectors with lexical BM25 to cover "
                + "both paraphrase and keyword matches."));
}
