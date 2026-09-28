package one.jpro.platform.cssgrid.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import one.jpro.platform.css.DynamicCSSUtil;
import one.jpro.platform.cssgrid.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Interactive test application for the CssGrid layout.
 * Left sidebar: controls for all CssGrid properties and per-item placement.
 * Center: live CssGrid with colored child items.
 * Bottom: live CSS editor powered by DynamicCSSUtil.
 */
public class CssGridTestApp extends Application {

    private static final Color[] COLORS = {
            Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#2ecc71"),
            Color.web("#f39c12"), Color.web("#9b59b6"), Color.web("#1abc9c"),
            Color.web("#e67e22"), Color.web("#34495e")
    };

    private static final String DEFAULT_CSS =
            "/* ================================================\n" +
            "   CSS Grid — Uncomment to try!\n" +
            "   Track lists, areas and placements with fr, span,\n" +
            "   minmax(), repeat() or '/' must be quoted.\n" +
            "   ================================================ */\n" +
            "\n" +
            ".css-grid {\n" +
            "    -fx-background-color: #f9f9f9;\n" +
            "\n" +
            "    /* ── Grid container properties ────────────── */\n" +
            "\n" +
            "    /* grid-template-columns: \"repeat(3, 1fr)\"; */\n" +
            "    /* grid-template-rows: \"auto 1fr auto\"; */\n" +
            "    /* grid-template-areas: \"'header header' 'sidebar main'\"; */\n" +
            "    /* grid-auto-columns: \"minmax(80, auto)\"; */\n" +
            "    /* grid-auto-rows: 60; */\n" +
            "\n" +
            "    /* grid-auto-flow: row; */\n" +
            "    /*   row | column | row-dense | column-dense */\n" +
            "\n" +
            "    /* justify-items: stretch; */\n" +
            "    /* align-items: stretch; */\n" +
            "    /*   start | end | center | stretch */\n" +
            "\n" +
            "    /* justify-content: stretch; */\n" +
            "    /* align-content: stretch; */\n" +
            "    /*   start | end | center | stretch */\n" +
            "    /*   space-between | space-around | space-evenly */\n" +
            "\n" +
            "    /* row-gap: 12; */\n" +
            "    /* column-gap: 12; */\n" +
            "}\n" +
            "\n" +
            ".grid-item {\n" +
            "    -fx-background-radius: 6;\n" +
            "    -fx-border-radius: 6;\n" +
            "\n" +
            "    /* ── Per-item properties (GridItem) ──────────── */\n" +
            "\n" +
            "    /* grid-column: \"1 / 3\"; */\n" +
            "    /* grid-row: \"span 2\"; */\n" +
            "    /* grid-area: header; */\n" +
            "    /* grid-column-start: 2; */\n" +
            "    /* grid-row-end: \"span 2\"; */\n" +
            "    /* justify-self: center; */\n" +
            "    /* align-self: end; */\n" +
            "    /* order: 0; */\n" +
            "}\n" +
            "\n" +
            "/* .grid-item-0 { grid-column: \"1 / -1\"; } */\n";

    private final AtomicInteger itemCounter = new AtomicInteger(0);
    private final CssGrid grid = new CssGrid();
    private VBox itemControls = new VBox(4);
    private Scene appScene;

    private TextField templateColumnsField;
    private TextField templateRowsField;
    private TextField templateAreasField;
    private TextField autoColumnsField;
    private TextField autoRowsField;
    private ComboBox<GridAutoFlow> autoFlowBox;
    private ComboBox<GridItemAlignment> justifyItemsBox;
    private ComboBox<GridItemAlignment> alignItemsBox;
    private ComboBox<GridContentAlignment> justifyContentBox;
    private ComboBox<GridContentAlignment> alignContentBox;
    private Slider rowGapSlider;
    private Slider columnGapSlider;
    private Label rowGapLabel;
    private Label columnGapLabel;
    private boolean syncing;

    @Override
    public void start(Stage stage) {
        grid.getStyleClass().add("css-grid");
        grid.setPadding(new Insets(8));
        grid.setTemplateColumns("repeat(3, 1fr)");

        for (int i = 0; i < 5; i++) addItem();

        VBox controls = createControlPanel();
        controls.setPrefWidth(320);
        controls.setMinWidth(320);
        controls.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 12;");

        ScrollPane controlScroll = new ScrollPane(controls);
        controlScroll.setFitToWidth(true);
        controlScroll.setMinWidth(340);

        VBox cssEditor = createCssEditor();

        Label bottomBoundary = new Label("Bottom Boundary");
        bottomBoundary.setMaxWidth(Double.MAX_VALUE);
        bottomBoundary.setAlignment(Pos.CENTER);
        bottomBoundary.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6;");

        Label rightBoundary = new Label("R\ni\ng\nh\nt");
        rightBoundary.setAlignment(Pos.CENTER);
        rightBoundary.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6;");

        VBox gridWithBottom = new VBox(grid, bottomBoundary);
        HBox gridWithBoundaries = new HBox(gridWithBottom, rightBoundary);
        HBox.setHgrow(gridWithBottom, Priority.ALWAYS);

        ScrollPane boundaryScroll = new ScrollPane(gridWithBoundaries);
        boundaryScroll.setFitToWidth(true);
        SplitPane centerSplit = new SplitPane(boundaryScroll, cssEditor);
        centerSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        centerSplit.setDividerPositions(0.7);

        BorderPane root = new BorderPane();
        root.setLeft(controlScroll);
        root.setCenter(centerSplit);
        root.setTop(createPresets());

        appScene = new Scene(root, 1250, 820);
        stage.setTitle("CSS Grid Test Application");
        stage.setScene(appScene);
        stage.show();

        DynamicCSSUtil.setCssString(appScene, DEFAULT_CSS);
    }

    private VBox createCssEditor() {
        Label title = new Label("Live CSS Editor");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        TextArea cssArea = new TextArea(DEFAULT_CSS);
        cssArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
        cssArea.setWrapText(true);
        VBox.setVgrow(cssArea, Priority.ALWAYS);
        cssArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (appScene != null) DynamicCSSUtil.setCssString(appScene, newVal);
        });

        Button resetCss = new Button("Reset CSS");
        resetCss.setOnAction(e -> cssArea.setText(DEFAULT_CSS));

        HBox toolbar = new HBox(8, title, resetCss);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4));

        VBox editor = new VBox(4, toolbar, cssArea);
        editor.setPadding(new Insets(4));
        editor.setStyle("-fx-background-color: #ecf0f1;");
        return editor;
    }

    // ── Control panel ────────────────────────────────────────────────────

    private VBox createControlPanel() {
        templateColumnsField = trackListField(grid.getTemplateColumns(), grid::setTemplateColumns);
        templateRowsField = trackListField(grid.getTemplateRows(), grid::setTemplateRows);
        templateAreasField = textField(grid.getTemplateAreas().toString(), text -> grid.setTemplateAreas(GridTemplateAreas.parse(text)));
        autoColumnsField = trackListField(grid.getAutoColumns(), grid::setAutoColumns);
        autoRowsField = trackListField(grid.getAutoRows(), grid::setAutoRows);

        autoFlowBox = enumBox(GridAutoFlow.values(), grid.getAutoFlow(), grid::setAutoFlow);
        justifyItemsBox = enumBox(GridItemAlignment.values(), grid.getJustifyItems(), grid::setJustifyItems);
        alignItemsBox = enumBox(GridItemAlignment.values(), grid.getAlignItems(), grid::setAlignItems);
        justifyContentBox = enumBox(GridContentAlignment.values(), grid.getJustifyContent(), grid::setJustifyContent);
        alignContentBox = enumBox(GridContentAlignment.values(), grid.getAlignContent(), grid::setAlignContent);

        rowGapSlider = new Slider(0, 50, 0);
        rowGapSlider.setShowTickLabels(true);
        rowGapLabel = new Label("Row Gap: 0");
        rowGapSlider.valueProperty().addListener((obs, o, n) -> {
            if (syncing) return;
            grid.setRowGap(n.doubleValue());
            rowGapLabel.setText(String.format("Row Gap: %.0f", n.doubleValue()));
        });

        columnGapSlider = new Slider(0, 50, 0);
        columnGapSlider.setShowTickLabels(true);
        columnGapLabel = new Label("Column Gap: 0");
        columnGapSlider.valueProperty().addListener((obs, o, n) -> {
            if (syncing) return;
            grid.setColumnGap(n.doubleValue());
            columnGapLabel.setText(String.format("Column Gap: %.0f", n.doubleValue()));
        });

        Button addBtn = new Button("+ Add Item");
        addBtn.setOnAction(e -> addItem());
        Button removeBtn = new Button("- Remove Item");
        removeBtn.setOnAction(e -> removeItem());
        HBox addRemove = new HBox(8, addBtn, removeBtn);

        itemControls = new VBox(4);
        refreshItemControls();

        Label hint = new Label("Track lists use CSS syntax, e.g. 200 1fr repeat(2, minmax(100, auto))");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 11; -fx-text-fill: #7f8c8d;");

        return new VBox(8,
                new Label("grid-template-columns:"), templateColumnsField,
                new Label("grid-template-rows:"), templateRowsField,
                new Label("grid-template-areas:"), templateAreasField,
                new Label("grid-auto-columns:"), autoColumnsField,
                new Label("grid-auto-rows:"), autoRowsField,
                hint,
                new Label("grid-auto-flow:"), autoFlowBox,
                new Label("justify-items:"), justifyItemsBox,
                new Label("align-items:"), alignItemsBox,
                new Label("justify-content:"), justifyContentBox,
                new Label("align-content:"), alignContentBox,
                rowGapLabel, rowGapSlider,
                columnGapLabel, columnGapSlider,
                new Separator(),
                addRemove,
                new Separator(),
                new Label("Per-Item Placement:"),
                itemControls
        );
    }

    private TextField trackListField(GridTrackList initial, Consumer<GridTrackList> setter) {
        return textField(initial.toString(), text -> setter.accept(GridTrackList.parse(text)));
    }

    /** A text field that applies its text on Enter and turns red while the text is invalid. */
    private TextField textField(String initial, Consumer<String> apply) {
        TextField field = new TextField(initial);
        Runnable commit = () -> {
            if (syncing) return;
            try {
                apply.accept(field.getText());
                field.setStyle("");
            } catch (IllegalArgumentException ex) {
                field.setStyle("-fx-border-color: #e74c3c;");
                field.setTooltip(new Tooltip(ex.getMessage()));
            }
        };
        field.setOnAction(e -> commit.run());
        field.focusedProperty().addListener((obs, o, focused) -> { if (!focused) commit.run(); });
        return field;
    }

    private <E extends Enum<E>> ComboBox<E> enumBox(E[] values, E initial, Consumer<E> setter) {
        ComboBox<E> box = new ComboBox<>();
        box.getItems().addAll(values);
        box.setValue(initial);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setOnAction(e -> { if (!syncing) setter.accept(box.getValue()); });
        return box;
    }

    private void syncControls() {
        syncing = true;
        try {
            templateColumnsField.setText(grid.getTemplateColumns().toString());
            templateRowsField.setText(grid.getTemplateRows().toString());
            templateAreasField.setText(grid.getTemplateAreas().toString());
            autoColumnsField.setText(grid.getAutoColumns().toString());
            autoRowsField.setText(grid.getAutoRows().toString());
            autoFlowBox.setValue(grid.getAutoFlow());
            justifyItemsBox.setValue(grid.getJustifyItems());
            alignItemsBox.setValue(grid.getAlignItems());
            justifyContentBox.setValue(grid.getJustifyContent());
            alignContentBox.setValue(grid.getAlignContent());
            rowGapSlider.setValue(grid.getRowGap());
            rowGapLabel.setText(String.format("Row Gap: %.0f", grid.getRowGap()));
            columnGapSlider.setValue(grid.getColumnGap());
            columnGapLabel.setText(String.format("Column Gap: %.0f", grid.getColumnGap()));
        } finally {
            syncing = false;
        }
    }

    // ── Presets ──────────────────────────────────────────────────────────

    private Node createPresets() {
        Label title = new Label("Presets:");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        title.setMinWidth(Region.USE_PREF_SIZE);

        FlowPane presets = new FlowPane(8, 8,
                title,
                preset("Holy Grail", this::presetHolyGrail),
                preset("Dashboard", this::presetDashboard),
                preset("Photo Gallery", this::presetPhotoGallery),
                preset("Form", this::presetForm),
                preset("Calendar", this::presetCalendar),
                preset("Dense Packing", this::presetDense),
                preset("Column Flow", this::presetColumnFlow),
                preset("Centered", this::presetCentered));
        presets.setAlignment(Pos.CENTER_LEFT);
        presets.setStyle("-fx-background-color: #2c3e50; -fx-padding: 8;");
        return presets;
    }

    private Button preset(String label, Runnable action) {
        Button b = new Button(label);
        b.setStyle("-fx-text-fill: white; -fx-background-color: #7f8c8d; -fx-cursor: hand;");
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setOnAction(e -> {
            action.run();
            syncControls();
            refreshItemControls();
        });
        return b;
    }

    private void presetHolyGrail() {
        resetAll();
        grid.setTemplateAreas("header header header", "nav main aside", "footer footer footer");
        grid.setTemplateColumns("150 1fr 120");
        grid.setTemplateRows("60 1fr 40");
        grid.setGap(8);
        addItem("Header", 100, 60).setArea("header");
        addItem("Nav", 100, 100).setArea("nav");
        addItem("Main", 100, 300).setArea("main");
        addItem("Aside", 100, 100).setArea("aside");
        addItem("Footer", 100, 40).setArea("footer");
    }

    private void presetDashboard() {
        resetAll();
        grid.setTemplateColumns("repeat(4, 1fr)");
        grid.setAutoRows("minmax(100, auto)");
        grid.setGap(12);
        addItem("Revenue", 80, 100);
        addItem("Users", 80, 100);
        addItem("Orders", 80, 100);
        addItem("Alerts", 80, 100);
        GridItem chart = addItem("Chart", 80, 220);
        chart.setColumnSpan(3);
        chart.setRowSpan(2);
        addItem("Activity", 80, 100);
        addItem("Tasks", 80, 100);
        GridItem log = addItem("Log", 80, 100);
        log.setColumnSpan(2);
        addItem("Status", 80, 100);
        addItem("Notes", 80, 100);
    }

    private void presetPhotoGallery() {
        resetAll();
        grid.setTemplateColumns("repeat(auto-fill, minmax(160, 1fr))");
        grid.setAutoRows("140");
        grid.setGap(10);
        String[] names = {"Landscape", "Portrait", "Square", "Panorama", "Sunset", "Forest", "City", "Lake", "Desert", "Snow"};
        for (String name : names) addItem(name, 100, 140);
        ((GridItem) grid.getChildren().get(3)).setColumnSpan(2);
        ((GridItem) grid.getChildren().get(6)).setRowSpan(2);
    }

    private void presetForm() {
        resetAll();
        grid.setTemplateColumns("auto 1fr auto 1fr");
        grid.setAlignItems(GridItemAlignment.CENTER);
        grid.setAlignContent(GridContentAlignment.START);
        grid.setGap(10);
        String[][] rows = {{"First name", "Last name"}, {"Email", "Phone"}, {"Street", "City"}};
        for (String[] row : rows) {
            for (String label : row) {
                addItem(label, 90, 32).setJustifySelf(GridItemAlignment.END);
                addItem("", 200, 32);
            }
        }
        GridItem message = addItem("Message", 90, 32);
        message.setJustifySelf(GridItemAlignment.END);
        message.setAlignSelf(GridItemAlignment.START);
        GridItem textArea = addItem("", 200, 90);
        textArea.setColumnSpan(3);
        GridItem submit = addItem("Submit", 120, 36);
        submit.setColumn(4);
        submit.setJustifySelf(GridItemAlignment.END);
    }

    private void presetCalendar() {
        resetAll();
        grid.setTemplateColumns("repeat(7, 1fr)");
        grid.setTemplateRows("30");
        grid.setAutoRows("70");
        grid.setGap(4);
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) addItem(day, 40, 30);
        GridItem first = addItem("1", 40, 70);
        first.setColumn(3);
        for (int d = 2; d <= 30; d++) addItem(String.valueOf(d), 40, 70);
    }

    private void presetDense() {
        resetAll();
        grid.setTemplateColumns("repeat(5, 1fr)");
        grid.setAutoRows("70");
        grid.setAutoFlow(GridAutoFlow.ROW_DENSE);
        grid.setGap(6);
        int[] spans = {2, 3, 1, 2, 4, 1, 3, 2, 1, 2, 5, 1};
        for (int i = 0; i < spans.length; i++) {
            GridItem item = addItem("span " + spans[i], 60, 70);
            item.setColumnSpan(spans[i]);
        }
    }

    private void presetColumnFlow() {
        resetAll();
        grid.setTemplateRows("repeat(3, 80)");
        grid.setAutoColumns("minmax(120, auto)");
        grid.setAutoFlow(GridAutoFlow.COLUMN);
        grid.setJustifyContent(GridContentAlignment.START);
        grid.setGap(8);
        for (int i = 0; i < 8; i++) addItem("Card " + (i + 1), 120, 80);
    }

    private void presetCentered() {
        resetAll();
        grid.setTemplateColumns("repeat(2, 140)");
        grid.setTemplateRows("repeat(2, 90)");
        grid.setJustifyContent(GridContentAlignment.CENTER);
        grid.setAlignContent(GridContentAlignment.CENTER);
        grid.setJustifyItems(GridItemAlignment.CENTER);
        grid.setAlignItems(GridItemAlignment.CENTER);
        grid.setGap(16);
        for (int i = 0; i < 4; i++) addItem("Card " + (i + 1), 100, 60);
    }

    // ── Item management ──────────────────────────────────────────────────

    private void resetAll() {
        grid.getChildren().clear();
        itemCounter.set(0);
        grid.setTemplateColumns(GridTrackList.NONE);
        grid.setTemplateRows(GridTrackList.NONE);
        grid.setTemplateAreas(GridTemplateAreas.NONE);
        grid.setAutoColumns(GridTrackList.AUTO);
        grid.setAutoRows(GridTrackList.AUTO);
        grid.setAutoFlow(GridAutoFlow.ROW);
        grid.setJustifyItems(GridItemAlignment.STRETCH);
        grid.setAlignItems(GridItemAlignment.STRETCH);
        grid.setJustifyContent(GridContentAlignment.STRETCH);
        grid.setAlignContent(GridContentAlignment.STRETCH);
        grid.setGap(0);
    }

    private GridItem addItem() {
        return addItem("Item " + itemCounter.get(), 80, 60);
    }

    private GridItem addItem(String labelText, double prefWidth, double prefHeight) {
        int idx = itemCounter.getAndIncrement();
        Color color = COLORS[idx % COLORS.length];

        Region bg = new Region();
        bg.setMinSize(20, 20);
        bg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        String hex = String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
        bg.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 6;"
                + " -fx-border-color: derive(" + hex + ", -20%); -fx-border-radius: 6;");

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (prefHeight < 40 ? 11 : 13) + ";");
        label.setMouseTransparent(true);

        GridItem wrapper = new GridItem(bg, label);
        wrapper.getStyleClass().addAll("grid-item", "grid-item-" + idx);
        wrapper.setMinSize(20, 20);
        wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        wrapper.setPrefSize(prefWidth, prefHeight);

        grid.getChildren().add(wrapper);
        refreshItemControls();
        return wrapper;
    }

    private void removeItem() {
        if (!grid.getChildren().isEmpty()) {
            grid.getChildren().remove(grid.getChildren().size() - 1);
            refreshItemControls();
        }
    }

    private void refreshItemControls() {
        if (itemControls == null) return;
        itemControls.getChildren().clear();
        for (int i = 0; i < grid.getChildren().size(); i++) {
            Node child = grid.getChildren().get(i);
            if (!(child instanceof GridItem)) continue;
            GridItem item = (GridItem) child;

            String name = "#" + i;
            for (Node n : item.getChildren()) {
                if (n instanceof Label && !((Label) n).getText().isEmpty()) {
                    name = ((Label) n).getText();
                    break;
                }
            }

            TextField columnField = lineField(item.getColumnStart(), item.getColumnEnd(),
                    (s, e) -> { item.setColumnStart(s); item.setColumnEnd(e); });
            TextField rowField = lineField(item.getRowStart(), item.getRowEnd(),
                    (s, e) -> { item.setRowStart(s); item.setRowEnd(e); });

            ComboBox<String> justifySelfBox = selfAlignmentBox(item.getJustifySelf(), item::setJustifySelf);
            ComboBox<String> alignSelfBox = selfAlignmentBox(item.getAlignSelf(), item::setAlignSelf);

            Spinner<Integer> orderSpinner = new Spinner<>(-10, 10, item.getOrder());
            orderSpinner.setEditable(true);
            orderSpinner.setPrefWidth(60);
            orderSpinner.valueProperty().addListener((obs, o, n) -> item.setOrder(n));

            VBox itemBox = new VBox(2,
                    new Label(name + " ──────────"),
                    row(new Label("grid-column:"), columnField, new Label("grid-row:"), rowField),
                    row(new Label("justify-self:"), justifySelfBox, new Label("align-self:"), alignSelfBox),
                    row(new Label("order:"), orderSpinner)
            );
            itemControls.getChildren().add(itemBox);
        }
    }

    private interface LinePair {
        void apply(GridLine start, GridLine end);
    }

    /** Text field with {@code start / end} placement syntax, e.g. {@code 1 / 3}, {@code span 2} or {@code header}. */
    private TextField lineField(GridLine start, GridLine end, LinePair setter) {
        String initial = end.isAuto() ? start.toString() : start + " / " + end;
        TextField field = textField(initial, text -> {
            String[] parts = text.split("/");
            if (parts.length > 2) throw new IllegalArgumentException("Expected 'start / end'");
            GridLine s = GridLine.parse(parts[0]);
            GridLine e = parts.length == 2 ? GridLine.parse(parts[1]) : (s.isNamed() ? s : GridLine.AUTO);
            setter.apply(s, e);
        });
        field.setPrefWidth(80);
        return field;
    }

    private ComboBox<String> selfAlignmentBox(GridItemAlignment current, Consumer<GridItemAlignment> setter) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().add("auto");
        for (GridItemAlignment v : GridItemAlignment.values()) box.getItems().add(v.name());
        box.setValue(current == null ? "auto" : current.name());
        box.setPrefWidth(95);
        box.setOnAction(e -> setter.accept("auto".equals(box.getValue()) ? null : GridItemAlignment.valueOf(box.getValue())));
        return box;
    }

    private static HBox row(Node... nodes) {
        HBox box = new HBox(4, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
