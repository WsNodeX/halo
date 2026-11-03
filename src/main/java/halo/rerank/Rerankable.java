package halo.rerank;

import java.util.Map;

/**
 * Anything a {@link Reranker} can re-rank: it exposes an id plus the raw signal
 * scores that fusion produced. Additional domain features (recency, popularity,
 * title match, ...) can be attached through {@link #features()}.
 */
public interface Rerankable {

    String id();

    float denseScore();

    float sparseScore();

    Map<String, Float> features();
}
