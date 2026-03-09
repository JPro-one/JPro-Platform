package one.jpro.platform.flexbox;

import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.scene.text.Font;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FlexBoxCssTest extends FlexBoxTestBase {

    /** Subclass to access protected ParsedValue constructor in tests. */
    private static class TestParsedValue<V, T> extends ParsedValue<V, T> {
        TestParsedValue(V value, StyleConverter<V, T> converter) {
            super(value, converter);
        }
    }

    @Test
    void cssMetaDataContainsFlexProperties() {
        List<CssMetaData<? extends Styleable, ?>> meta = FlexBox.getClassCssMetaData();
        List<String> names = meta.stream()
                .map(CssMetaData::getProperty)
                .collect(Collectors.toList());

        assertTrue(names.contains("flex-direction"), "Should contain flex-direction");
        assertTrue(names.contains("flex-wrap"), "Should contain flex-wrap");
        assertTrue(names.contains("justify-content"), "Should contain justify-content");
        assertTrue(names.contains("align-items"), "Should contain align-items");
        assertTrue(names.contains("align-content"), "Should contain align-content");
        assertTrue(names.contains("gap"), "Should contain gap");
    }

    @Test
    void instanceCssMetaDataMatchesClass() {
        FlexBox box = new FlexBox();
        assertEquals(FlexBox.getClassCssMetaData(), box.getCssMetaData());
    }

    @Test
    void cssMetaDataIncludesParentProperties() {
        List<CssMetaData<? extends Styleable, ?>> meta = FlexBox.getClassCssMetaData();
        List<String> names = meta.stream()
                .map(CssMetaData::getProperty)
                .collect(Collectors.toList());

        assertTrue(names.contains("-fx-padding"), "Should inherit -fx-padding from Pane");
    }

    @Test
    void propertiesAreSettable() {
        FlexBox box = new FlexBox();
        List<CssMetaData<? extends Styleable, ?>> meta = FlexBox.getClassCssMetaData();

        for (CssMetaData<? extends Styleable, ?> css : meta) {
            if (css.getProperty().equals("flex-direction") ||
                    css.getProperty().equals("flex-wrap") ||
                    css.getProperty().equals("justify-content") ||
                    css.getProperty().equals("align-items") ||
                    css.getProperty().equals("align-content") ||
                    css.getProperty().equals("gap")) {
                @SuppressWarnings("unchecked")
                CssMetaData<FlexBox, ?> flexMeta = (CssMetaData<FlexBox, ?>) css;
                assertTrue(flexMeta.isSettable(box),
                        css.getProperty() + " should be settable");
            }
        }
    }

    @Test
    void enumConverterMapsValues() {
        FlexEnumConverter<FlexDirection> converter = new FlexEnumConverter<>(FlexDirection.class);

        assertEquals(FlexDirection.ROW,
                converter.convert(new TestParsedValue<>("row", null), null));
        assertEquals(FlexDirection.ROW_REVERSE,
                converter.convert(new TestParsedValue<>("row-reverse", null), null));
        assertEquals(FlexDirection.COLUMN,
                converter.convert(new TestParsedValue<>("column", null), null));
        assertEquals(FlexDirection.COLUMN_REVERSE,
                converter.convert(new TestParsedValue<>("column-reverse", null), null));
    }

    @Test
    void justifyContentConverterMapsValues() {
        FlexEnumConverter<FlexJustifyContent> converter = new FlexEnumConverter<>(FlexJustifyContent.class);

        assertEquals(FlexJustifyContent.FLEX_START,
                converter.convert(new TestParsedValue<>("flex-start", null), null));
        assertEquals(FlexJustifyContent.SPACE_BETWEEN,
                converter.convert(new TestParsedValue<>("space-between", null), null));
        assertEquals(FlexJustifyContent.SPACE_EVENLY,
                converter.convert(new TestParsedValue<>("space-evenly", null), null));
    }

    @Test
    void alignItemsConverterMapsValues() {
        FlexEnumConverter<FlexAlignItems> converter = new FlexEnumConverter<>(FlexAlignItems.class);

        assertEquals(FlexAlignItems.FLEX_START,
                converter.convert(new TestParsedValue<>("flex-start", null), null));
        assertEquals(FlexAlignItems.STRETCH,
                converter.convert(new TestParsedValue<>("stretch", null), null));
        assertEquals(FlexAlignItems.BASELINE,
                converter.convert(new TestParsedValue<>("baseline", null), null));
    }

    @Test
    void converterIsCaseInsensitive() {
        FlexEnumConverter<FlexDirection> converter = new FlexEnumConverter<>(FlexDirection.class);

        assertEquals(FlexDirection.ROW,
                converter.convert(new TestParsedValue<>("ROW", null), null));
        assertEquals(FlexDirection.ROW_REVERSE,
                converter.convert(new TestParsedValue<>("Row-Reverse", null), null));
    }

    @Test
    void converterThrowsOnInvalidValue() {
        FlexEnumConverter<FlexDirection> converter = new FlexEnumConverter<>(FlexDirection.class);

        assertThrows(IllegalArgumentException.class, () ->
                converter.convert(new TestParsedValue<>("invalid", null), null));
    }
}
