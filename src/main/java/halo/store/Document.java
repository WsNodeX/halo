package halo.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A source document: an identifier, its raw text and optional string metadata
 * (title, author, section, ...).
 */
public final class Document {

    private final String id;
    private final String text;
    private final Map<String, String> metadata;

    public Document(String id, String text) {
        this(id, text, Collections.emptyMap());
    }

    public Document(String id, String text, Map<String, String> metadata) {
        this.id = id;
        this.text = text != null ? text : "";
        this.metadata = metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>();
    }

    public String id() { return id; }
    public String text() { return text; }
    public Map<String, String> metadata() { return Collections.unmodifiableMap(metadata); }
}
