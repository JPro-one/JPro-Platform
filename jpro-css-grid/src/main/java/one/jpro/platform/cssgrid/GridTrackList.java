package one.jpro.platform.cssgrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An immutable list of {@link GridTrack}s, the value of {@code grid-template-columns},
 * {@code grid-template-rows}, {@code grid-auto-columns} and {@code grid-auto-rows}.
 * <p>
 * CSS syntax accepted by {@link #parse(String)}:
 * <pre>
 * 200 1fr 2fr
 * 100px auto minmax(150, 1fr)
 * repeat(3, 1fr)
 * repeat(auto-fill, minmax(200, 1fr))
 * 50% 50%
 * none
 * </pre>
 */
public final class GridTrackList {

    /** The empty track list ({@code none}). */
    public static final GridTrackList NONE = new GridTrackList(Collections.emptyList());

    /** A single {@code auto} track, the default for {@code grid-auto-columns} and {@code grid-auto-rows}. */
    public static final GridTrackList AUTO = new GridTrackList(Collections.singletonList(GridTrack.auto()));

    private final List<GridTrack> tracks;

    private GridTrackList(List<GridTrack> tracks) {
        this.tracks = tracks;
    }

    /** Creates a track list; at most one {@code auto-fill}/{@code auto-fit} repeat is allowed. */
    public static GridTrackList of(GridTrack... tracks) {
        return of(Arrays.asList(tracks));
    }

    /** Creates a track list from a copy of the given tracks; at most one {@code auto-fill}/{@code auto-fit} repeat is allowed. */
    public static GridTrackList of(List<GridTrack> tracks) {
        Objects.requireNonNull(tracks, "tracks");
        if (tracks.isEmpty()) return NONE;
        int autoRepeats = 0;
        for (GridTrack t : tracks) {
            Objects.requireNonNull(t, "track");
            if (t.getKind() == GridTrack.Kind.REPEAT && t.getRepeatMode() != GridTrack.RepeatMode.COUNT) autoRepeats++;
        }
        if (autoRepeats > 1) throw new IllegalArgumentException("Only one auto-fill/auto-fit repeat is allowed per track list");
        return new GridTrackList(Collections.unmodifiableList(new ArrayList<>(tracks)));
    }

    /**
     * Parses CSS track list syntax. Plain numbers are pixels; {@code px}, {@code %} and {@code fr} units are supported.
     *
     * @throws IllegalArgumentException if the text is not a valid track list
     */
    public static GridTrackList parse(String css) {
        Objects.requireNonNull(css, "css");
        return of(new Parser(css).parseList());
    }

    /** The unmodifiable list of entries; {@code repeat()} groups are not expanded. */
    public List<GridTrack> getTracks() { return tracks; }

    /** True for {@code none}. */
    public boolean isEmpty() { return tracks.isEmpty(); }

    /** True if the list contains an {@code auto-fill} or {@code auto-fit} repeat. */
    public boolean hasAutoRepeat() {
        for (GridTrack t : tracks) {
            if (t.getKind() == GridTrack.Kind.REPEAT && t.getRepeatMode() != GridTrack.RepeatMode.COUNT) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof GridTrackList && tracks.equals(((GridTrackList) o).tracks));
    }

    @Override
    public int hashCode() { return tracks.hashCode(); }

    /** CSS representation, e.g. {@code 200 1fr repeat(2, 100)}; {@code none} when empty. */
    @Override
    public String toString() {
        return tracks.isEmpty() ? "none" : join(tracks);
    }

    static String join(List<GridTrack> tracks) {
        StringBuilder sb = new StringBuilder();
        for (GridTrack t : tracks) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString();
    }

    // ── Parser ────────────────────────────────────────────────────────

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        List<GridTrack> parseList() {
            List<GridTrack> result = new ArrayList<>();
            skipWhitespace();
            if (pos == text.length()) return result;
            if (peekIdent("none")) {
                pos += 4;
                skipWhitespace();
                if (pos != text.length()) fail("Unexpected content after 'none'");
                return result;
            }
            while (pos < text.length()) {
                result.add(parseEntry(true));
                skipWhitespace();
            }
            return result;
        }

        private GridTrack parseEntry(boolean allowRepeat) {
            skipWhitespace();
            if (pos >= text.length()) fail("Expected a track size");
            char c = text.charAt(pos);
            if (Character.isDigit(c) || c == '.') return parseNumber();
            if (Character.isLetter(c)) {
                String ident = readIdent();
                if (peekChar('(')) {
                    pos++;
                    GridTrack fn = parseFunction(ident, allowRepeat);
                    expect(')');
                    return fn;
                }
                switch (ident) {
                    case "auto":        return GridTrack.auto();
                    case "min-content": return GridTrack.minContent();
                    case "max-content": return GridTrack.maxContent();
                    default: fail("Unknown track size '" + ident + "'");
                }
            }
            fail("Unexpected character '" + c + "'");
            return null;
        }

        private GridTrack parseFunction(String name, boolean allowRepeat) {
            switch (name) {
                case "minmax": {
                    GridTrack min = parseEntry(false);
                    skipWhitespace();
                    expect(',');
                    GridTrack max = parseEntry(false);
                    skipWhitespace();
                    return GridTrack.minmax(min, max);
                }
                case "repeat": {
                    if (!allowRepeat) fail("repeat() cannot be nested");
                    skipWhitespace();
                    GridTrack.RepeatMode mode;
                    int count = 0;
                    if (peekIdent("auto-fill")) {
                        pos += "auto-fill".length();
                        mode = GridTrack.RepeatMode.AUTO_FILL;
                    } else if (peekIdent("auto-fit")) {
                        pos += "auto-fit".length();
                        mode = GridTrack.RepeatMode.AUTO_FIT;
                    } else {
                        mode = GridTrack.RepeatMode.COUNT;
                        String num = readNumberText();
                        try {
                            count = Integer.parseInt(num);
                        } catch (NumberFormatException e) {
                            fail("Invalid repeat() count '" + num + "'");
                        }
                        if (count < 1) fail("repeat() count must be >= 1");
                    }
                    skipWhitespace();
                    expect(',');
                    List<GridTrack> tracks = new ArrayList<>();
                    skipWhitespace();
                    while (pos < text.length() && text.charAt(pos) != ')') {
                        tracks.add(parseEntry(false));
                        skipWhitespace();
                    }
                    return GridTrack.repeat(mode, count, tracks);
                }
                default:
                    fail("Unknown function '" + name + "()'");
                    return null;
            }
        }

        private GridTrack parseNumber() {
            String num = readNumberText();
            double value;
            try {
                value = Double.parseDouble(num);
            } catch (NumberFormatException e) {
                fail("Invalid number '" + num + "'");
                return null;
            }
            if (peekChar('%')) {
                pos++;
                return GridTrack.percent(value);
            }
            int start = pos;
            while (pos < text.length() && Character.isLetter(text.charAt(pos))) pos++;
            String unit = text.substring(start, pos).toLowerCase();
            switch (unit) {
                case "":
                case "px": return GridTrack.px(value);
                case "fr": return GridTrack.fr(value);
                default:   fail("Unsupported unit '" + unit + "'");
                           return null;
            }
        }

        private String readNumberText() {
            int start = pos;
            while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) pos++;
            if (start == pos) fail("Expected a number");
            return text.substring(start, pos);
        }

        private String readIdent() {
            int start = pos;
            while (pos < text.length() && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '-')) pos++;
            return text.substring(start, pos).toLowerCase();
        }

        private boolean peekIdent(String ident) {
            return text.regionMatches(true, pos, ident, 0, ident.length())
                    && (pos + ident.length() == text.length()
                        || !(Character.isLetterOrDigit(text.charAt(pos + ident.length())) || text.charAt(pos + ident.length()) == '-'));
        }

        private boolean peekChar(char c) {
            return pos < text.length() && text.charAt(pos) == c;
        }

        private void expect(char c) {
            skipWhitespace();
            if (!peekChar(c)) fail("Expected '" + c + "'");
            pos++;
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }

        private void fail(String message) {
            throw new IllegalArgumentException("Invalid track list '" + text + "' at " + pos + ": " + message);
        }
    }
}
