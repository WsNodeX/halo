package halo.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny zero-dependency JSON reader/writer.
 *
 * <p>Supports the subset of JSON needed to persist collections: objects
 * (ordered {@link LinkedHashMap}), arrays ({@link List}), strings, numbers
 * ({@link Double}), booleans and null. Not a general-purpose codec, but it is
 * deterministic and sufficient for on-disk storage.</p>
 */
public final class Json {

    private Json() {}

    // ---------------------------------------------------------------- writer

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Map) {
            writeObject(sb, (Map<String, Object>) v);
        } else if (v instanceof List) {
            writeArray(sb, (List<Object>) v);
        } else if (v instanceof String) {
            writeString(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d == Math.rint(d) && !Double.isInfinite(d)) {
                sb.append(Long.toString((long) d));
            } else {
                sb.append(Double.toString(d));
            }
        } else if (v instanceof double[]) {
            List<Object> arr = new ArrayList<>();
            for (double x : (double[]) v) arr.add(x);
            writeArray(sb, arr);
        } else if (v instanceof float[]) {
            List<Object> arr = new ArrayList<>();
            for (float x : (float[]) v) arr.add((double) x);
            writeArray(sb, arr);
        } else if (v instanceof int[]) {
            List<Object> arr = new ArrayList<>();
            for (int x : (int[]) v) arr.add((long) x);
            writeArray(sb, arr);
        } else {
            writeString(sb, v.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<String, Object> m) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, e.getKey());
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<Object> list) {
        sb.append('[');
        boolean first = true;
        for (Object o : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, o);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- reader

    public static Object read(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.parseValue();
        p.skipWs();
        if (!p.atEnd()) throw new IllegalArgumentException("trailing characters at " + p.pos);
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readObject(String text) {
        return (Map<String, Object>) read(text);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> readArray(String text) {
        return (List<Object>) read(text);
    }

    private static final class Parser {
        final String s;
        int pos = 0;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object parseValue() {
            skipWs();
            if (pos >= s.length()) throw new IllegalArgumentException("unexpected end");
            char c = s.charAt(pos);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
                    throw new IllegalArgumentException("unexpected char '" + c + "' at " + pos);
            }
        }

        Map<String, Object> parseObject() {
            LinkedHashMap<String, Object> m = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                if (peek() != '"') throw new IllegalArgumentException("expected key at " + pos);
                String key = parseString();
                skipWs();
                if (peek() != ':') throw new IllegalArgumentException("expected ':' at " + pos);
                pos++;
                Object val = parseValue();
                m.put(key, val);
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; break; }
                throw new IllegalArgumentException("expected ',' or '}' at " + pos);
            }
            return m;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWs();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; break; }
                throw new IllegalArgumentException("expected ',' or ']' at " + pos);
            }
            return list;
        }

        String parseString() {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            String hex = s.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default: throw new IllegalArgumentException("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'
                        || c == '+' || c == '-') {
                    pos++;
                } else break;
            }
            String num = s.substring(start, pos);
            if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                return Double.parseDouble(num);
            }
            long l = Long.parseLong(num);
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (double) l : (double) l;
        }

        char peek() {
            if (pos >= s.length()) throw new IllegalArgumentException("unexpected end");
            return s.charAt(pos);
        }

        void expect(String lit) {
            if (!s.startsWith(lit, pos)) {
                throw new IllegalArgumentException("expected '" + lit + "' at " + pos);
            }
            pos += lit.length();
        }
    }
}
