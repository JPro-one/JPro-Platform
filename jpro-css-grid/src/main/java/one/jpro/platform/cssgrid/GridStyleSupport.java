package one.jpro.platform.cssgrid;

import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.StyleableObjectProperty;
import javafx.scene.text.Font;

import java.util.function.Function;

/**
 * CSS plumbing shared by {@link CssGrid} and {@link GridItem}.
 * <p>
 * JavaFX's CSS parser converts plain numbers itself (e.g. {@code grid-column-start: 2} arrives as a
 * {@code Double}, {@code grid-template-columns: 100 200} as a {@code Number[]}) and only hands identifiers and
 * quoted strings to the property's own converter. The coercers below accept all of these shapes.
 */
final class GridStyleSupport {

    private GridStyleSupport() {
    }

    static GridTrackList toTrackList(Object value) {
        if (value == null) return GridTrackList.NONE;
        if (value instanceof GridTrackList) return (GridTrackList) value;
        if (value instanceof Number) return GridTrackList.of(GridTrack.px(((Number) value).doubleValue()));
        return GridTrackList.parse(toText(value));
    }

    static GridLine toLine(Object value) {
        if (value == null) return GridLine.AUTO;
        if (value instanceof GridLine) return (GridLine) value;
        if (value instanceof Number) return GridLine.at(((Number) value).intValue());
        return GridLine.parse(value.toString());
    }

    static GridTemplateAreas toAreas(Object value) {
        if (value == null) return GridTemplateAreas.NONE;
        if (value instanceof GridTemplateAreas) return (GridTemplateAreas) value;
        return GridTemplateAreas.parse(toText(value));
    }

    static String toText(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return GridTrack.formatNumber(((Number) value).doubleValue());
        if (value instanceof Object[]) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (Object[]) value) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(toText(o));
            }
            return sb.toString();
        }
        return value.toString();
    }

    /** A StyleConverter that delegates to one of the coercers above. */
    static final class Converter<T> extends StyleConverter<Object, T> {
        private final Function<Object, T> coerce;

        Converter(Function<Object, T> coerce) {
            this.coerce = coerce;
        }

        @Override
        public T convert(ParsedValue<Object, T> value, Font font) {
            return coerce.apply(value.getValue());
        }
    }

    /**
     * A styleable property that coerces whatever the CSS engine passes to {@link #applyStyle} into the property type.
     * Declared raw on purpose: a generic subclass would cast the value before {@code applyStyle} could coerce it.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class CoercingProperty extends StyleableObjectProperty {
        private final Object bean;
        private final String name;
        private final CssMetaData metaData;
        private final Function<Object, Object> coerce;
        private final Runnable onInvalidated;
        private final Runnable onApplied;

        private CoercingProperty(Object bean, String name, CssMetaData metaData, Object initialValue,
                                 Function<Object, Object> coerce, Runnable onInvalidated, Runnable onApplied) {
            super(initialValue);
            this.bean = bean;
            this.name = name;
            this.metaData = metaData;
            this.coerce = coerce;
            this.onInvalidated = onInvalidated;
            this.onApplied = onApplied;
        }

        static <T> StyleableObjectProperty<T> create(Object bean, String name, CssMetaData<?, T> metaData, T initialValue,
                                                     Function<Object, T> coerce, Runnable onInvalidated) {
            return new CoercingProperty(bean, name, metaData, initialValue, (Function) coerce, onInvalidated, null);
        }

        /** A property whose {@code onApplied} callback runs after CSS applied a value and its origin is known. */
        static <T> StyleableObjectProperty<T> createCssApplied(Object bean, String name, CssMetaData<?, T> metaData,
                                                               Function<Object, T> coerce, Runnable onApplied) {
            return new CoercingProperty(bean, name, metaData, null, (Function) coerce, null, onApplied);
        }

        @Override
        public void applyStyle(StyleOrigin origin, Object value) {
            super.applyStyle(origin, coerce.apply(value));
            if (onApplied != null) onApplied.run();
        }

        @Override
        protected void invalidated() {
            if (onInvalidated != null) onInvalidated.run();
        }

        @Override public Object getBean() { return bean; }
        @Override public String getName() { return name; }
        @Override public CssMetaData getCssMetaData() { return metaData; }
    }
}
