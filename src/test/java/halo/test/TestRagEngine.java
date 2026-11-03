package halo.test;

import halo.embed.HashEmbedder;
import halo.rag.RagEngine;
import halo.rag.StubLlm;
import halo.store.Collection;
import halo.store.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestRagEngine {

    @Test
    void askReturnsGroundedAnswerWithCitation() {
        Collection c = new Collection("kb", new HashEmbedder(256));
        c.add(new Document("gc", "The JVM garbage collector reclaims unreachable objects so developers do not free memory by hand."));
        RagEngine rag = new RagEngine(c, new StubLlm());
        RagEngine.RagAnswer ans = rag.ask("how does garbage collection work?");
        assertTrue(ans.answer.contains("garbage collector") || ans.answer.contains("JVM"));
        assertFalse(ans.citations.isEmpty());
        assertEquals("gc", ans.citations.get(0).docId());
    }

    @Test
    void noContextProducesHonestAnswer() {
        Collection c = new Collection("empty", new HashEmbedder(128));
        RagEngine rag = new RagEngine(c, new StubLlm());
        RagEngine.RagAnswer ans = rag.ask("anything");
        assertTrue(ans.answer.toLowerCase().contains("no relevant context")
                || ans.citations.isEmpty());
    }
}
