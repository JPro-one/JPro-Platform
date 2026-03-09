package one.jpro.platform.flexbox.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import one.jpro.platform.flexbox.*;
import one.jpro.platform.css.DynamicCSSUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interactive test application for the FlexBox layout.
 * Left sidebar: controls to change all FlexBox properties.
 * Center: live FlexBox with colored child items.
 * Bottom: live CSS editor powered by DynamicCSSUtil.
 */
public class FlexBoxTestApp extends Application {

    private static final Color[] COLORS = {
            Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#2ecc71"),
            Color.web("#f39c12"), Color.web("#9b59b6"), Color.web("#1abc9c"),
            Color.web("#e67e22"), Color.web("#34495e")
    };

    private static final String DEFAULT_CSS =
            "/* ================================================\n" +
            "   FlexBox CSS — Uncomment to try!\n" +
            "   ================================================ */\n" +
            "\n" +
            ".flex-box {\n" +
            "    -fx-background-color: #f9f9f9;\n" +
            "\n" +
            "    /* ── FlexBox layout properties ────────────── */\n" +
            "\n" +
            "    /* flex-direction: row; */\n" +
            "    /*   row | row-reverse | column | column-reverse */\n" +
            "\n" +
            "    /* flex-wrap: nowrap; */\n" +
            "    /*   nowrap | wrap | wrap-reverse */\n" +
            "\n" +
            "    /* justify-content: flex-start; */\n" +
            "    /*   flex-start | flex-end | center */\n" +
            "    /*   space-between | space-around | space-evenly */\n" +
            "\n" +
            "    /* align-items: stretch; */\n" +
            "    /*   flex-start | flex-end | center | stretch | baseline */\n" +
            "\n" +
            "    /* align-content: stretch; */\n" +
            "    /*   flex-start | flex-end | center | stretch */\n" +
            "    /*   space-between | space-around | space-evenly */\n" +
            "\n" +
            "    /* row-gap: 12; */\n" +
            "    /* column-gap: 12; */\n" +
            "}\n" +
            "\n" +
            ".flex-item {\n" +
            "    -fx-background-radius: 6;\n" +
            "    -fx-border-radius: 6;\n" +
            "    /* -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 2, 2); */\n" +
            "\n" +
            "    /* ── Per-item FlexBox properties (FlexItem) ── */\n" +
            "\n" +
            "    /* flex-grow: 0; */\n" +
            "    /* flex-shrink: 1; */\n" +
            "    /* flex-basis: -1; */\n" +
            "    /*   -1 = auto (use pref size) */\n" +
            "    /* order: 0; */\n" +
            "    /* align-self: center; */\n" +
            "    /*   flex-start | flex-end | center | stretch | baseline */\n" +
            "}\n";

    private final AtomicInteger itemCounter = new AtomicInteger(0);
    private final FlexBox flexBox = new FlexBox();
    private VBox itemControls = new VBox(4);
    private Scene appScene;

    // Control panel widgets (kept as fields so presets can sync them)
    private ComboBox<FlexDirection> directionBox;
    private ComboBox<FlexWrap> wrapBox;
    private ComboBox<FlexJustifyContent> justifyBox;
    private ComboBox<FlexAlignItems> alignBox;
    private ComboBox<FlexAlignContent> alignContentBox;
    private Slider rowGapSlider;
    private Slider columnGapSlider;
    private Label rowGapLabel;
    private Label columnGapLabel;

    @Override
    public void start(Stage stage) {
        flexBox.getStyleClass().add("flex-box");
        flexBox.setPadding(new Insets(8));

        for (int i = 0; i < 5; i++) addItem();

        VBox controls = createControlPanel();
        controls.setPrefWidth(300);
        controls.setMinWidth(300);
        controls.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 12;");

        ScrollPane controlScroll = new ScrollPane(controls);
        controlScroll.setFitToWidth(true);
        controlScroll.setMinWidth(320);

        // CSS editor at the bottom
        VBox cssEditor = createCssEditor();

        // Center: FlexBox above, CSS editor below
        SplitPane centerSplit = new SplitPane(flexBox, cssEditor);
        centerSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        centerSplit.setDividerPositions(0.7);

        BorderPane root = new BorderPane();
        root.setLeft(controlScroll);
        root.setCenter(centerSplit);

        FlexBox presets = createPresets();
        presets.setStyle("-fx-background-color: #2c3e50; -fx-padding: 8;");
        root.setTop(presets);

        appScene = new Scene(root, 1200, 800);
        stage.setTitle("FlexBox Test Application");
        stage.setScene(appScene);
        stage.show();

        // Apply initial CSS
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
            if (appScene != null) {
                DynamicCSSUtil.setCssString(appScene, newVal);
            }
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

    private VBox createControlPanel() {
        directionBox = new ComboBox<>();
        directionBox.getItems().addAll(FlexDirection.values());
        directionBox.setValue(flexBox.getDirection());
        directionBox.setOnAction(e -> flexBox.setDirection(directionBox.getValue()));

        wrapBox = new ComboBox<>();
        wrapBox.getItems().addAll(FlexWrap.values());
        wrapBox.setValue(flexBox.getWrap());
        wrapBox.setOnAction(e -> flexBox.setWrap(wrapBox.getValue()));

        justifyBox = new ComboBox<>();
        justifyBox.getItems().addAll(FlexJustifyContent.values());
        justifyBox.setValue(flexBox.getJustifyContent());
        justifyBox.setOnAction(e -> flexBox.setJustifyContent(justifyBox.getValue()));

        alignBox = new ComboBox<>();
        alignBox.getItems().addAll(FlexAlignItems.values());
        alignBox.setValue(flexBox.getAlignItems());
        alignBox.setOnAction(e -> flexBox.setAlignItems(alignBox.getValue()));

        alignContentBox = new ComboBox<>();
        alignContentBox.getItems().addAll(FlexAlignContent.values());
        alignContentBox.setValue(flexBox.getAlignContent());
        alignContentBox.setOnAction(e -> flexBox.setAlignContent(alignContentBox.getValue()));

        rowGapSlider = new Slider(0, 50, 0);
        rowGapSlider.setShowTickLabels(true);
        rowGapSlider.setShowTickMarks(true);
        rowGapLabel = new Label("Row Gap: 0");
        rowGapSlider.valueProperty().addListener((obs, o, n) -> {
            flexBox.setRowGap(n.doubleValue());
            rowGapLabel.setText(String.format("Row Gap: %.0f", n.doubleValue()));
        });

        columnGapSlider = new Slider(0, 50, 0);
        columnGapSlider.setShowTickLabels(true);
        columnGapSlider.setShowTickMarks(true);
        columnGapLabel = new Label("Column Gap: 0");
        columnGapSlider.valueProperty().addListener((obs, o, n) -> {
            flexBox.setColumnGap(n.doubleValue());
            columnGapLabel.setText(String.format("Column Gap: %.0f", n.doubleValue()));
        });

        Button addBtn = new Button("+ Add Item");
        addBtn.setOnAction(e -> addItem());
        Button removeBtn = new Button("- Remove Item");
        removeBtn.setOnAction(e -> removeItem());
        HBox addRemove = new HBox(8, addBtn, removeBtn);

        itemControls = new VBox(4);
        refreshItemControls();

        VBox panel = new VBox(10,
                new Label("Direction:"), directionBox,
                new Label("Wrap:"), wrapBox,
                new Label("Justify Content:"), justifyBox,
                new Label("Align Items:"), alignBox,
                new Label("Align Content:"), alignContentBox,
                rowGapLabel, rowGapSlider,
                columnGapLabel, columnGapSlider,
                new Separator(),
                addRemove,
                new Separator(),
                new Label("Per-Item Controls:"),
                itemControls
        );
        return panel;
    }

    private void syncControls() {
        if (directionBox != null) directionBox.setValue(flexBox.getDirection());
        if (wrapBox != null) wrapBox.setValue(flexBox.getWrap());
        if (justifyBox != null) justifyBox.setValue(flexBox.getJustifyContent());
        if (alignBox != null) alignBox.setValue(flexBox.getAlignItems());
        if (alignContentBox != null) alignContentBox.setValue(flexBox.getAlignContent());
        if (rowGapSlider != null) {
            rowGapSlider.setValue(flexBox.getRowGap());
            rowGapLabel.setText(String.format("Row Gap: %.0f", flexBox.getRowGap()));
        }
        if (columnGapSlider != null) {
            columnGapSlider.setValue(flexBox.getColumnGap());
            columnGapLabel.setText(String.format("Column Gap: %.0f", flexBox.getColumnGap()));
        }
    }

    private FlexBox createPresets() {
        Button headerBar = preset("Header Bar", this::presetHeaderBar);
        Button sidebar = preset("Sidebar Layout", this::presetSidebarLayout);
        Button dashboard = preset("Dashboard", this::presetDashboard);
        Button tagCloud = preset("Tag Cloud", this::presetTagCloud);
        Button form = preset("Form Layout", this::presetFormLayout);
        Button gallery = preset("Photo Gallery", this::presetPhotoGallery);
        Button footer = preset("Footer", this::presetFooter);
        Button appStack = preset("App Stack", this::presetAppStack);

        Label title = new Label("Presets:");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 0 0 0;");
        title.setMinWidth(Region.USE_PREF_SIZE);

        FlexBox presets = new FlexBox();
        presets.setWrap(FlexWrap.WRAP);
        presets.setAlignItems(FlexAlignItems.CENTER);
        presets.setGap(8);
        presets.getChildren().addAll(title, headerBar, sidebar, dashboard, tagCloud, form, gallery, footer, appStack);
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

    // ── Presets ──────────────────────────────────────────────────────────

    private void presetHeaderBar() {
        resetAll();
        addItem("Logo", 120, 45).setFlexShrink(0);
        FlexItem spacer = addItem("", 0, 45);
        spacer.setOpacity(0);
        spacer.setFlexGrow(1);
        addItem("Home", 60, 45);
        addItem("About", 70, 45);
        addItem("Contact", 85, 45);

        flexBox.setAlignItems(FlexAlignItems.CENTER);
        flexBox.setColumnGap(8);
    }

    private void presetSidebarLayout() {
        resetAll();
        FlexItem sidebar = addItem("Sidebar", 180, 300);
        sidebar.setFlexBasis(180);
        sidebar.setFlexShrink(0);
        FlexItem content = addItem("Content", 400, 300);
        content.setFlexBasis(300);
        content.setFlexGrow(1);
        FlexItem aside = addItem("Aside", 140, 300);
        aside.setFlexBasis(140);
        aside.setFlexShrink(0);

        flexBox.setAlignItems(FlexAlignItems.STRETCH);
        flexBox.setColumnGap(8);
    }

    private void presetDashboard() {
        resetAll();
        double[][] cards = {{250, 120}, {150, 120}, {150, 120}, {400, 180}, {200, 180}, {150, 100}};
        String[] names = {"Revenue", "Users", "Orders", "Chart", "Activity", "Alerts"};
        double[] bases = {250, 150, 150, 400, 200, 150};
        for (int i = 0; i < names.length; i++) {
            FlexItem item = addItem(names[i], cards[i][0], cards[i][1]);
            item.setFlexGrow(1);
            item.setFlexBasis(bases[i]);
        }
        ((FlexItem) flexBox.getChildren().get(3)).setFlexGrow(2); // Chart grows faster

        flexBox.setWrap(FlexWrap.WRAP);
        flexBox.setAlignItems(FlexAlignItems.STRETCH);
        flexBox.setAlignContent(FlexAlignContent.FLEX_START);
        flexBox.setRowGap(12);
        flexBox.setColumnGap(12);
    }

    private void presetTagCloud() {
        resetAll();
        String[] tags = {"JavaFX", "CSS", "FlexBox", "Layout", "Responsive",
                "Design", "UI", "Components", "Platform", "Web", "Styling", "Pane"};
        int[] widths = {70, 45, 75, 65, 100, 65, 35, 105, 80, 45, 70, 55};
        for (int i = 0; i < tags.length; i++) {
            addItem(tags[i], widths[i], 32);
        }

        flexBox.setWrap(FlexWrap.WRAP);
        flexBox.setAlignItems(FlexAlignItems.CENTER);
        flexBox.setAlignContent(FlexAlignContent.FLEX_START);
        flexBox.setRowGap(8);
        flexBox.setColumnGap(8);
    }

    private void presetFormLayout() {
        resetAll();
        addItem("Name", 500, 40).setFlexGrow(1);
        addItem("Email", 500, 40).setFlexGrow(1);
        FlexItem street = addItem("Street", 350, 40);
        street.setFlexGrow(1);
        street.setFlexBasis(350);
        FlexItem city = addItem("City", 200, 40);
        city.setFlexGrow(1);
        city.setFlexBasis(200);
        FlexItem zip = addItem("ZIP", 100, 40);
        zip.setFlexBasis(100);
        addItem("Message", 500, 100).setFlexGrow(1);
        FlexItem submit = addItem("Submit", 120, 45);
        submit.setAlignSelf(FlexAlignItems.FLEX_END);

        flexBox.setWrap(FlexWrap.WRAP);
        flexBox.setAlignItems(FlexAlignItems.FLEX_START);
        flexBox.setAlignContent(FlexAlignContent.FLEX_START);
        flexBox.setRowGap(8);
        flexBox.setColumnGap(8);
    }

    private void presetPhotoGallery() {
        resetAll();
        String[] names = {"Landscape", "Portrait", "Square", "Panorama", "Landscape 2", "Tall", "Wide", "Small"};
        double[][] sizes = {{200, 130}, {130, 200}, {150, 150}, {300, 100}, {180, 120}, {120, 220}, {250, 110}, {100, 100}};
        for (int i = 0; i < names.length; i++) {
            FlexItem item = addItem(names[i], sizes[i][0], sizes[i][1]);
            item.setFlexGrow(1);
            item.setFlexBasis(sizes[i][0]);
        }

        flexBox.setWrap(FlexWrap.WRAP);
        flexBox.setAlignItems(FlexAlignItems.STRETCH);
        flexBox.setAlignContent(FlexAlignContent.FLEX_START);
        flexBox.setRowGap(6);
        flexBox.setColumnGap(6);
    }

    private void presetFooter() {
        resetAll();
        String[] names = {"About Us", "Links", "Support", "Newsletter"};
        double[] widths = {250, 150, 150, 200};
        double[] grows = {2, 1, 1, 1};
        for (int i = 0; i < names.length; i++) {
            FlexItem item = addItem(names[i], widths[i], 150);
            item.setFlexBasis(widths[i]);
            item.setFlexGrow(grows[i]);
        }

        flexBox.setWrap(FlexWrap.WRAP);
        flexBox.setJustifyContent(FlexJustifyContent.SPACE_BETWEEN);
        flexBox.setAlignItems(FlexAlignItems.FLEX_START);
        flexBox.setAlignContent(FlexAlignContent.FLEX_START);
        flexBox.setRowGap(16);
        flexBox.setColumnGap(16);
    }

    private void presetAppStack() {
        resetAll();
        addItem("App Header", 500, 50).setFlexShrink(0);
        addItem("Toolbar", 500, 35).setFlexShrink(0);
        addItem("Content Area", 500, 300).setFlexGrow(1);
        addItem("Status Bar", 500, 30).setFlexShrink(0);

        flexBox.setDirection(FlexDirection.COLUMN);
        flexBox.setAlignItems(FlexAlignItems.STRETCH);
    }

    // ── Item management ─────────────────────────────────────────────────

    private void resetAll() {
        flexBox.getChildren().clear();
        itemCounter.set(0);
        flexBox.setDirection(FlexDirection.ROW);
        flexBox.setWrap(FlexWrap.NOWRAP);
        flexBox.setJustifyContent(FlexJustifyContent.FLEX_START);
        flexBox.setAlignItems(FlexAlignItems.STRETCH);
        flexBox.setAlignContent(FlexAlignContent.STRETCH);
        flexBox.setGap(0);
    }

    /** Add a default-sized item. */
    private FlexItem addItem() {
        return addItem("Item " + itemCounter.get(), 80, 60);
    }

    /** Add an item with a custom label and size. */
    private FlexItem addItem(String labelText, double prefWidth, double prefHeight) {
        int idx = itemCounter.getAndIncrement();
        Color color = COLORS[idx % COLORS.length];

        Region bg = new Region();
        bg.setMinSize(20, 20);
        bg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        String hex = String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        bg.setStyle("-fx-background-color: " + hex + ";"
                + " -fx-background-radius: 6;"
                + " -fx-border-color: derive(" + hex + ", -20%);"
                + " -fx-border-radius: 6;");

        Label label = new Label(labelText);
        double fontSize = prefHeight < 40 ? 11 : 13;
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + fontSize + ";");
        label.setMouseTransparent(true);

        FlexItem wrapper = new FlexItem(bg, label);
        wrapper.getStyleClass().addAll("flex-item", "flex-item-" + idx);
        wrapper.setMinSize(20, 20);
        wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        wrapper.setPrefSize(prefWidth, prefHeight);

        flexBox.getChildren().add(wrapper);
        refreshItemControls();
        return wrapper;
    }

    private void removeItem() {
        if (!flexBox.getChildren().isEmpty()) {
            flexBox.getChildren().remove(flexBox.getChildren().size() - 1);
            refreshItemControls();
        }
    }

    private void refreshItemControls() {
        itemControls.getChildren().clear();
        for (int i = 0; i < flexBox.getChildren().size(); i++) {
            javafx.scene.Node child = flexBox.getChildren().get(i);
            FlexItem fi = child instanceof FlexItem ? (FlexItem) child : null;

            String name = "";
            if (child instanceof StackPane) {
                for (javafx.scene.Node n : ((StackPane) child).getChildren()) {
                    if (n instanceof Label) {
                        name = ((Label) n).getText();
                        break;
                    }
                }
            }
            if (name.isEmpty()) name = "#" + i;

            Spinner<Double> growSpinner = new Spinner<>(0.0, 10.0, FlexBox.getGrow(child), 0.5);
            growSpinner.setEditable(true);
            growSpinner.setPrefWidth(70);
            growSpinner.valueProperty().addListener((obs, o, n) -> {
                if (fi != null) fi.setFlexGrow(n); else FlexBox.setGrow(child, n);
            });

            Spinner<Double> shrinkSpinner = new Spinner<>(0.0, 10.0, FlexBox.getShrink(child), 0.5);
            shrinkSpinner.setEditable(true);
            shrinkSpinner.setPrefWidth(70);
            shrinkSpinner.valueProperty().addListener((obs, o, n) -> {
                if (fi != null) fi.setFlexShrink(n); else FlexBox.setShrink(child, n);
            });

            Spinner<Double> basisSpinner = new Spinner<>(-1.0, 500.0, FlexBox.getBasis(child), 10);
            basisSpinner.setEditable(true);
            basisSpinner.setPrefWidth(70);
            basisSpinner.valueProperty().addListener((obs, o, n) -> {
                if (fi != null) fi.setFlexBasis(n); else FlexBox.setBasis(child, n);
            });

            Spinner<Integer> orderSpinner = new Spinner<>(-10, 10, FlexBox.getOrder(child));
            orderSpinner.setEditable(true);
            orderSpinner.setPrefWidth(60);
            orderSpinner.valueProperty().addListener((obs, o, n) -> {
                if (fi != null) fi.setFlexOrder(n); else FlexBox.setOrder(child, n);
            });

            ComboBox<String> alignSelfBox = new ComboBox<>();
            alignSelfBox.getItems().add("auto");
            for (FlexAlignItems v : FlexAlignItems.values()) alignSelfBox.getItems().add(v.name());
            FlexAlignItems selfVal = FlexBox.getAlignSelf(child);
            alignSelfBox.setValue(selfVal == null ? "auto" : selfVal.name());
            alignSelfBox.setPrefWidth(100);
            alignSelfBox.setOnAction(e -> {
                String val = alignSelfBox.getValue();
                if ("auto".equals(val)) {
                    if (fi != null) fi.setAlignSelf(null);
                    else { child.getProperties().remove("flexbox-align-self"); flexBox.requestLayout(); }
                } else {
                    if (fi != null) fi.setAlignSelf(FlexAlignItems.valueOf(val));
                    else FlexBox.setAlignSelf(child, FlexAlignItems.valueOf(val));
                }
            });

            VBox itemBox = new VBox(2,
                    new Label(name + " ──────────"),
                    new HBox(4, new Label("grow:"), growSpinner, new Label("shrink:"), shrinkSpinner),
                    new HBox(4, new Label("basis:"), basisSpinner, new Label("order:"), orderSpinner),
                    new HBox(4, new Label("align-self:"), alignSelfBox)
            );
            for (javafx.scene.Node n : itemBox.getChildren()) {
                if (n instanceof HBox) ((HBox) n).setAlignment(Pos.CENTER_LEFT);
            }
            itemControls.getChildren().add(itemBox);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
