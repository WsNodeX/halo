package halo.index;

/**
 * A single retrieval hit: an identifier paired with its relevance score.
 * Scores are comparable only within the same index/query and larger means
 * "more relevant".
 */
public final class ScoredId implements Comparable<ScoredId> {

    public final String id;
    public final float score;

    public ScoredId(String id, float score) {
        this.id = id;
        this.score = score;
    }

    @Override
    public int compareTo(ScoredId o) {
        return Float.compare(o.score, this.score); // descending
    }

    @Override
    public String toString() {
        return id + ":" + String.format("%.4f", score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScoredId)) return false;
        ScoredId that = (ScoredId) o;
        return id.equals(that.id)
                && Float.compare(that.score, score) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + Float.hashCode(score);
    }
}
