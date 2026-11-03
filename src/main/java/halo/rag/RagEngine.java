package halo.rag;

import halo.retrieval.RetrievalResult;
import halo.store.Collection;

import java.util.List;

/**
 * Retrieval-Augmented Generation engine.
 *
 * <p>Wires a {@link Collection} (retriever) to a language model (any object
 * exposing {@code complete(query, passages)}). {@link #ask} runs hybrid
 * retrieval, passes the top passages to the model as grounding context, and
 * returns both the generated answer and the citations it was based on.</p>
 */
public final class RagEngine {

    private final Collection collection;
    private final StubLlm llm;
    private int topK = 4;

    public RagEngine(Collection collection, StubLlm llm) {
        this.collection = collection;
        this.llm = llm;
    }

    public void setTopK(int topK) { this.topK = topK; }

    /** Run RAG for a query and return the answer plus its supporting passages. */
    public RagAnswer ask(String query) {
        List<RetrievalResult> hits = collection.search(query, topK);
        String answer = llm.complete(query, hits);
        return new RagAnswer(answer, hits);
    }

    /** The result of a RAG query: generated text + grounded citations. */
    public static final class RagAnswer {
        public final String answer;
        public final List<RetrievalResult> citations;

        public RagAnswer(String answer, List<RetrievalResult> citations) {
            this.answer = answer;
            this.citations = citations;
        }
    }
}
