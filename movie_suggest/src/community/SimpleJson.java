package community;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON reader for API responses; keeps this project framework-free. */
final class SimpleJson {
    private final String text;
    private int index;

    private SimpleJson(String text) { this.text = text; }

    static Object parse(String text) {
        SimpleJson parser = new SimpleJson(text);
        Object value = parser.value();
        parser.space();
        if (parser.index != text.length()) throw new IllegalArgumentException("Unexpected JSON trailing content.");
        return value;
    }

    private Object value() {
        space();
        if (index >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON.");
        char current = text.charAt(index);
        if (current == '{') return object();
        if (current == '[') return array();
        if (current == '"') return string();
        if (text.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
        if (text.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
        if (text.startsWith("null", index)) { index += 4; return null; }
        return number();
    }

    private Map<String, Object> object() {
        Map<String, Object> result = new LinkedHashMap<>();
        index++;
        space();
        if (consume('}')) return result;
        while (true) {
            space();
            String key = string();
            space(); expect(':');
            result.put(key, value());
            space();
            if (consume('}')) return result;
            expect(',');
        }
    }

    private List<Object> array() {
        List<Object> result = new ArrayList<>();
        index++;
        space();
        if (consume(']')) return result;
        while (true) {
            result.add(value());
            space();
            if (consume(']')) return result;
            expect(',');
        }
    }

    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '"') return result.toString();
            if (c != '\\') { result.append(c); continue; }
            if (index >= text.length()) throw new IllegalArgumentException("Invalid JSON escape.");
            char escaped = text.charAt(index++);
            switch (escaped) {
                case '"': result.append('"'); break;
                case '\\': result.append('\\'); break;
                case '/': result.append('/'); break;
                case 'b': result.append('\b'); break;
                case 'f': result.append('\f'); break;
                case 'n': result.append('\n'); break;
                case 'r': result.append('\r'); break;
                case 't': result.append('\t'); break;
                case 'u':
                    if (index + 4 > text.length()) throw new IllegalArgumentException("Invalid unicode escape.");
                    result.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                    index += 4; break;
                default: throw new IllegalArgumentException("Invalid JSON escape.");
            }
        }
        throw new IllegalArgumentException("Unclosed JSON string.");
    }

    private Number number() {
        int start = index;
        if (consume('-')) { }
        while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
        boolean decimal = consume('.');
        if (decimal) while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
        if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            decimal = true; index++; if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) index++;
            while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
        }
        String raw = text.substring(start, index);
        try { return decimal ? Double.parseDouble(raw) : Long.parseLong(raw); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid JSON number: " + raw, exception); }
    }

    private void space() { while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++; }
    private boolean consume(char expected) { if (index < text.length() && text.charAt(index) == expected) { index++; return true; } return false; }
    private void expect(char expected) { if (!consume(expected)) throw new IllegalArgumentException("Expected '" + expected + "' in JSON."); }
}
