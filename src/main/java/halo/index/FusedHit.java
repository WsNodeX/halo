package halo.index;

/**
 * A hybrid retrieval hit that keeps both the fused ranking score and the raw
 * dense / sparse contributions, so a downstream reranker can re-weight them.
 */
public final class FusedHit {

    public final String id;
    public final float fused;
    public final float dense;
    public final float sparse;

    public FusedHit(String id, float fused, float dense, float sparse) {
        this.id = id;
        this.fused = fused;
        this.dense = dense;
        this.sparse = sparse;
    }

    public ScoredId toScoredId() { return new ScoredId(id, fused); }
}
