package halo.test;

import halo.embed.Tokenizers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestTokenizers {

    @Test
    void wordsLowerCaseAlnum() {
        List<String> t = Tokenizers.words("The Quick brown Fox, jumps 123!");
        assertEquals(List.of("the", "quick", "brown", "fox", "jumps", "123"), t);
    }

    @Test
    void charNgrams() {
        assertEquals(List.of("ab", "bc", "cd"), Tokenizers.charNgrams("abcd", 2));
        assertEquals(List.of("a"), Tokenizers.charNgrams("a", 2));
    }

    @Test
    void wordNgrams() {
        assertEquals(List.of("the quick", "quick fox", "fox runs"),
                Tokenizers.wordNgrams("the quick fox runs", 2));
        // fewer than n words -> empty
        assertTrue(Tokenizers.wordNgrams("one two", 3).isEmpty());
    }
}
