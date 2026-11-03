package halo.store;

import halo.embed.Embedder;
import halo.index.FusedHit;
import halo.index.HybridIndex;
import halo.index.InvertedIndex;
import halo.index.VectorIndex;
import halo.index.HnswIndex;
import halo.retrieval.RetrievalResult;
import halo.util.Json;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A persisted, searchable corpus.
 *
 * <p>A collection chunks each document, embeds the chunks into a dense
 * {@link VectorIndex}, indexes their tokens into a {@link InvertedIndex}, and
 * fuses the two with {@link HybridIndex}. It can be serialized to JSON and
 * reloaded (embeddings are recomputed on load, keeping the file compact).</p>
 */
public final class Collection {

    private final String name;
    private final Embedder embedder;
    private final int chunkSize;
    private final int overlap;
    private final HybridIndex hybrid;
    private final Map<String, Document> documents = new LinkedHashMap<>();
    private final Map<String, Chunk> chunks = new LinkedHashMap<>();

    private static final class Chunk {
        final String id;
        final String docId;
        final String text;
        final Map<String, String> metadata;
        Chunk(String id, String docId, String text, Map<String, String> metadata) {
            this.id = id; this.docId = docId; this.text = text;
            this.metadata = metadata;
        }
    }

    public Collection(String name, Embedder embedder) {
        this(name, embedder, 120, 20);
    }

    public Collection(String name, Embedder embedder, int chunkSize, int overlap) {
        this.name = name;
        this.embedder = embedder;
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        VectorIndex dense = new HnswIndex(embedder.dim());
        this.hybrid = new HybridIndex(dense, new InvertedIndex(), embedder);
    }

    /** Add a document (and its derived chunks) to the index. */
    public synchronized void add(Document doc) {
        documents.put(doc.id(), doc);
        List<String> pieces = chunk(doc.text(), chunkSize, overlap);
        for (int i = 0; i < pieces.size(); i++) {
            String cid = doc.id() + "#" + i;
            Chunk c = new Chunk(cid, doc.id(), pieces.get(i), doc.metadata());
            chunks.put(cid, c);
            hybrid.add(cid, pieces.get(i));
        }
    }

    /** Retrieve the top {@code topK} passages for a query. */
    public List<RetrievalResult> search(String query, int topK) {
        List<FusedHit> hits = hybrid.searchDetailed(query, topK);
        List<RetrievalResult> out = new ArrayList<>(hits.size());
        for (FusedHit h : hits) {
            Chunk c = chunks.get(h.id);
            if (c == null) continue;
            RetrievalResult r = new RetrievalResult(h.id, c.docId, c.text, c.metadata);
            r.setDenseScore(h.dense);
            r.setSparseScore(h.sparse);
            r.setScore(h.fused);
            out.add(r);
        }
        return out;
    }

    public int documentCount() { return documents.size(); }
    public int chunkCount() { return chunks.size(); }
    public String name() { return name; }

    // ---- chunking -----------------------------------------------------------

    private List<String> chunk(String text, int size, int overlap) {
        List<String> words = new ArrayList<>();
        for (String w : text.split("\\s+")) {
            if (!w.isEmpty()) words.add(w);
        }
        List<String> out = new ArrayList<>();
        if (words.isEmpty()) { out.add(text); return out; }
        int stride = Math.max(1, size - overlap);
        for (int start = 0; start < words.size(); start += stride) {
            int end = Math.min(words.size(), start + size);
            out.add(String.join(" ", words.subList(start, end)));
            if (end == words.size()) break;
        }
        return out;
    }

    // ---- persistence --------------------------------------------------------

    /** Serialize the corpus (documents + chunking config) to JSON. */
    public synchronized void save(File file) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", name);
        root.put("chunkSize", chunkSize);
        root.put("overlap", overlap);
        List<Object> docs = new ArrayList<>();
        for (Document d : documents.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("text", d.text());
            m.put("metadata", new LinkedHashMap<>(d.metadata()));
            docs.add(m);
        }
        root.put("documents", docs);
        Files.writeString(file.toPath(), Json.write(root), StandardCharsets.UTF_8);
    }

    /** Load a previously saved collection. The same embedder must be supplied. */
    @SuppressWarnings("unchecked")
    public static Collection load(File file, Embedder embedder) throws IOException {
        Map<String, Object> root = Json.readObject(
                Files.readString(file.toPath(), StandardCharsets.UTF_8));
        int cs = ((Number) root.getOrDefault("chunkSize", 120)).intValue();
        int ov = ((Number) root.getOrDefault("overlap", 20)).intValue();
        Collection c = new Collection((String) root.getOrDefault("name", "collection"),
                embedder, cs, ov);
        List<Object> docs = (List<Object>) root.get("documents");
        if (docs != null) {
            for (Object o : docs) {
                Map<String, Object> m = (Map<String, Object>) o;
                Map<String, String> meta = new LinkedHashMap<>();
                Map<String, Object> rawMeta = (Map<String, Object>) m.get("metadata");
                if (rawMeta != null) for (Map.Entry<String, Object> e : rawMeta.entrySet())
                    meta.put(e.getKey(), String.valueOf(e.getValue()));
                c.add(new Document((String) m.get("id"), (String) m.get("text"), meta));
            }
        }
        return c;
    }
}
