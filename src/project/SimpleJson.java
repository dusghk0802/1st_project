package project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON parser for the TMDB batch importer; no external JSON library required. */
final class SimpleJson {
    private final String text;
    private int index;
    private SimpleJson(String text) { this.text = text; }
    static Object parse(String text) { SimpleJson p = new SimpleJson(text); Object v = p.value(); p.space(); if (p.index != text.length()) throw new IllegalArgumentException("Unexpected JSON trailing content."); return v; }
    private Object value() { space(); if (index >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON."); char c = text.charAt(index); if (c == '{') return object(); if (c == '[') return array(); if (c == '"') return string(); if (text.startsWith("true", index)) { index += 4; return Boolean.TRUE; } if (text.startsWith("false", index)) { index += 5; return Boolean.FALSE; } if (text.startsWith("null", index)) { index += 4; return null; } return number(); }
    private Map<String,Object> object() { Map<String,Object> r = new LinkedHashMap<>(); index++; space(); if (consume('}')) return r; while (true) { space(); String k = string(); space(); expect(':'); r.put(k, value()); space(); if (consume('}')) return r; expect(','); } }
    private List<Object> array() { List<Object> r = new ArrayList<>(); index++; space(); if (consume(']')) return r; while (true) { r.add(value()); space(); if (consume(']')) return r; expect(','); } }
    private String string() { expect('"'); StringBuilder r = new StringBuilder(); while (index < text.length()) { char c = text.charAt(index++); if (c == '"') return r.toString(); if (c != '\\') { r.append(c); continue; } if (index >= text.length()) throw new IllegalArgumentException("Invalid JSON escape."); char e = text.charAt(index++); switch (e) { case '"': r.append('"'); break; case '\\': r.append('\\'); break; case '/': r.append('/'); break; case 'b': r.append('\b'); break; case 'f': r.append('\f'); break; case 'n': r.append('\n'); break; case 'r': r.append('\r'); break; case 't': r.append('\t'); break; case 'u': if (index + 4 > text.length()) throw new IllegalArgumentException("Invalid unicode escape."); r.append((char) Integer.parseInt(text.substring(index, index + 4), 16)); index += 4; break; default: throw new IllegalArgumentException("Invalid JSON escape."); } } throw new IllegalArgumentException("Unclosed JSON string."); }
    private Number number() { int start = index; consume('-'); while (index < text.length() && Character.isDigit(text.charAt(index))) index++; boolean decimal = consume('.'); if (decimal) while (index < text.length() && Character.isDigit(text.charAt(index))) index++; if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) { decimal = true; index++; if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) index++; while (index < text.length() && Character.isDigit(text.charAt(index))) index++; } String raw = text.substring(start, index); return decimal ? Double.parseDouble(raw) : Long.parseLong(raw); }
    private void space() { while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++; }
    private boolean consume(char c) { if (index < text.length() && text.charAt(index) == c) { index++; return true; } return false; }
    private void expect(char c) { if (!consume(c)) throw new IllegalArgumentException("Expected '" + c + "' in JSON."); }
}
