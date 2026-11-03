package halo.test;

import halo.util.Json;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestJson {

    @Test
    void roundTripObject() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "halo");
        m.put("count", 42L);
        m.put("ratio", 0.5);
        m.put("active", true);
        m.put("nil", null);
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("k", "v");
        m.put("nested", inner);

        String s = Json.write(m);
        Map<String, Object> back = Json.readObject(s);
        assertEquals("halo", back.get("name"));
        assertEquals(42.0, back.get("count"));
        assertEquals(0.5, back.get("ratio"));
        assertEquals(true, back.get("active"));
        assertNull(back.get("nil"));
        assertEquals("v", ((Map<?, ?>) back.get("nested")).get("k"));
    }

    @Test
    void roundTripArrayAndEscapes() {
        List<Object> arr = new ArrayList<>();
        arr.add("quote\" and \\ slash");
        arr.add("new\nline");
        arr.add(1.0);
        String s = Json.write(arr);
        List<Object> back = Json.readArray(s);
        assertEquals(3, back.size());
        assertEquals("quote\" and \\ slash", back.get(0));
        assertEquals("new\nline", back.get(1));
    }

    @Test
    void integersStayIntegral() {
        String s = Json.write(List.of(7L, 8L));
        @SuppressWarnings("unchecked")
        List<Object> back = (List<Object>) Json.read(s);
        assertTrue(back.get(0) instanceof Double);
        assertEquals(7.0, back.get(0));
    }

    @Test
    void unicodeStrings() {
        String s = Json.write(Map.of("t", "向量检索"));
        Map<String, Object> back = Json.readObject(s);
        assertEquals("向量检索", back.get("t"));
    }
}
