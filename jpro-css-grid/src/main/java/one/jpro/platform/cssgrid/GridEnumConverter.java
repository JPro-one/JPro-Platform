package one.jpro.platform.cssgrid;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps hyphenated CSS keywords to enum constants, e.g. "space-between" → SPACE_BETWEEN.
 * Whitespace is treated like a hyphen, so "row dense" and "row-dense" are equivalent.
 */
class GridEnumConverter<E extends Enum<E>> extends StyleConverter<String, E> {

    private final Map<String, E> lookup = new HashMap<>();

    GridEnumConverter(Class<E> enumClass) {
        for (E constant : enumClass.getEnumConstants()) {
            lookup.put(constant.name().toLowerCase().replace('_', '-'), constant);
        }
    }

    GridEnumConverter<E> alias(String cssName, E value) {
        lookup.put(cssName, value);
        return this;
    }

    E convertString(String raw) {
        String key = raw.trim().toLowerCase().replaceAll("\\s+", "-");
        E result = lookup.get(key);
        if (result == null) {
            throw new IllegalArgumentException("Unknown CSS value: '" + raw + "'. Valid values: " + lookup.keySet());
        }
        return result;
    }

    @Override
    public E convert(ParsedValue<String, E> value, Font font) {
        String raw = value.getValue();
        return raw == null ? null : convertString(raw);
    }
}
