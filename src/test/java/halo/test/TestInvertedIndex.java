package halo.test;

import halo.index.InvertedIndex;
import halo.index.ScoredId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestInvertedIndex {

    @Test
    void exactKeywordRanksAboveUnrelated() {
        InvertedIndex idx = new InvertedIndex();
        idx.add("a", "the JVM garbage collector reclaims unreachable objects");
        idx.add("b", "basketball is a popular sport played worldwide");
        idx.add("c", "distributed consensus uses the raft algorithm");
        List<ScoredId> r = idx.search("garbage collector", 3);
        assertEquals("a", r.get(0).id);
    }

    @Test
    void rarestTermDominates() {
        InvertedIndex idx = new InvertedIndex();
        idx.add("a", "zebra zebra zebra uncommonword");
        idx.add("b", "common common common common common");
        List<ScoredId> r = idx.search("uncommonword", 2);
        assertEquals("a", r.get(0).id);
    }

    @Test
    void noMatchReturnsEmpty() {
        InvertedIndex idx = new InvertedIndex();
        idx.add("a", "hello world");
        assertTrue(idx.search("nonexistentterm", 3).isEmpty());
    }

    @Test
    void replaceDocumentKeepsConsistentSize() {
        InvertedIndex idx = new InvertedIndex();
        idx.add("x", "alpha beta gamma");
        idx.add("x", "delta epsilon");
        assertEquals(1, idx.size());
        assertEquals("x", idx.search("delta", 1).get(0).id);
    }
}
