package halo.rerank;

import halo.index.ScoredId;

import java.util.List;

/**
 * Re-ranks an already-fused candidate list using one or more learned or
 * heuristic signals. Reranking is the final, typically cheaper, stage of a
 * retrieval pipeline and is where business rules are easiest to inject.
 */
public interface Reranker {

    List<ScoredId> rerank(String query, List<? extends Rerankable> candidates);
}
