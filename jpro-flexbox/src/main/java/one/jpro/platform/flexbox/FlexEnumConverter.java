package one.jpro.platform.flexbox;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic StyleConverter that maps hyphenated CSS strings to enum constants.
 * E.g. "row-reverse" → ROW_REVERSE, "space-between" → SPACE_BETWEEN.
 */
class FlexEnumConverter<E extends Enum<E>> extends StyleConverter<String, E> {

    private final Map<String, E> lookup = new HashMap<>();

    FlexEnumConverter(Class<E> enumClass) {
        for (E constant : enumClass.getEnumConstants()) {
            // "ROW_REVERSE" → "row-reverse"
            String cssName = constant.name().toLowerCase().replace('_', '-');
            lookup.put(cssName, constant);
        }
    }

    @Override
    public E convert(ParsedValue<String, E> value, Font font) {
        String raw = value.getValue();
        if (raw == null) return null;
        String key = raw.trim().toLowerCase();
        E result = lookup.get(key);
        if (result == null) {
            throw new IllegalArgumentException("Unknown CSS value: '" + raw + "'. Valid values: " + lookup.keySet());
        }
        return result;
    }
}
