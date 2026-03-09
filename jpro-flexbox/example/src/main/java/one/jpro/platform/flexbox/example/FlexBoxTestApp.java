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
            "    /* gap: 12; */\n" +
            "}\n" +
            "\n" +
            ".flex-item {\n" +
            "    -fx-background-radius: 6;\n" +
            "    -fx-border-radius: 6;\n" +
            "    /* -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 2, 2); */\n" +
            "}\n" +
            "\n" +
            "/* ── Try these presets ─────────────────────────── */\n" +
            "\n" +
            "/* Centered card grid:\n" +
            ".flex-box {\n" +
            "    flex-wrap: wrap;\n" +
            "    justify-content: center;\n" +
            "    align-content: flex-start;\n" +
            "    gap: 16;\n" +
            "} */\n" +
            "\n" +
            "/* Vertical stack:\n" +
            ".flex-box {\n" +
            "    flex-direction: column;\n" +
            "    align-items: stretch;\n" +
            "    gap: 8;\n" +
            "} */\n" +
            "\n" +
            "/* Nav bar:\n" +
            ".flex-box {\n" +
            "    flex-direction: row;\n" +
            "    justify-content: space-between;\n" +
            "    align-items: center;\n" +
            "} */\n";

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
    private Slider gapSlider;
    private Label gapLabel;

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

        HBox presets = createPresets();
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

        // Apply CSS on every text change
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

        gapSlider = new Slider(0, 50, 0);
        gapSlider.setShowTickLabels(true);
        gapSlider.setShowTickMarks(true);
        gapLabel = new Label("Gap: 0");
        gapSlider.valueProperty().addListener((obs, o, n) -> {
            flexBox.setGap(n.doubleValue());
            gapLabel.setText(String.format("Gap: %.0f", n.doubleValue()));
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
                gapLabel, gapSlider,
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
        if (gapSlider != null) {
            gapSlider.setValue(flexBox.getGap());
            gapLabel.setText(String.format("Gap: %.0f", flexBox.getGap()));
        }
    }

    private HBox createPresets() {
        Button navBar = new Button("Nav Bar");
        navBar.setOnAction(e -> {
            resetAll();
            for (int i = 0; i < 5; i++) addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setJustifyContent(FlexJustifyContent.SPACE_BETWEEN);
            flexBox.setAlignItems(FlexAlignItems.CENTER);
            syncControls();
            refreshItemControls();
        });

        Button centered = new Button("Centered");
        centered.setOnAction(e -> {
            resetAll();
            addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setJustifyContent(FlexJustifyContent.CENTER);
            flexBox.setAlignItems(FlexAlignItems.CENTER);
            syncControls();
            refreshItemControls();
        });

        Button equalCols = new Button("Equal Columns");
        equalCols.setOnAction(e -> {
            resetAll();
            for (int i = 0; i < 3; i++) addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setAlignItems(FlexAlignItems.STRETCH);
            flexBox.setGap(12);
            flexBox.getChildren().forEach(c -> FlexBox.setGrow(c, 1));
            syncControls();
            refreshItemControls();
        });

        Button wrappingCards = new Button("Wrapping Cards");
        wrappingCards.setOnAction(e -> {
            resetAll();
            for (int i = 0; i < 9; i++) addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setWrap(FlexWrap.WRAP);
            flexBox.setAlignContent(FlexAlignContent.FLEX_START);
            flexBox.setAlignItems(FlexAlignItems.FLEX_START);
            flexBox.setGap(12);
            flexBox.getChildren().forEach(c -> FlexBox.setBasis(c, 150));
            syncControls();
            refreshItemControls();
        });

        Button holyGrail = new Button("Holy Grail");
        holyGrail.setOnAction(e -> {
            resetAll();
            for (int i = 0; i < 3; i++) addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setAlignItems(FlexAlignItems.STRETCH);
            flexBox.setGap(8);
            FlexBox.setBasis(flexBox.getChildren().get(0), 100);
            FlexBox.setGrow(flexBox.getChildren().get(1), 1);
            FlexBox.setBasis(flexBox.getChildren().get(2), 100);
            syncControls();
            refreshItemControls();
        });

        Button shrinkDemo = new Button("Shrink Demo");
        shrinkDemo.setOnAction(e -> {
            resetAll();
            for (int i = 0; i < 4; i++) addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setAlignItems(FlexAlignItems.STRETCH);
            for (javafx.scene.Node c : flexBox.getChildren()) {
                FlexBox.setBasis(c, 200);
            }
            FlexBox.setShrink(flexBox.getChildren().get(0), 0);
            syncControls();
            refreshItemControls();
        });

        Button orderDemo = new Button("Order Demo");
        orderDemo.setOnAction(e -> {
            resetAll();
            for (int i = 0; i < 5; i++) addItem();
            flexBox.setDirection(FlexDirection.ROW);
            flexBox.setGap(8);
            for (int i = 0; i < flexBox.getChildren().size(); i++) {
                FlexBox.setOrder(flexBox.getChildren().get(i), flexBox.getChildren().size() - i);
            }
            syncControls();
            refreshItemControls();
        });

        for (Button b : new Button[]{navBar, centered, equalCols, wrappingCards, holyGrail, shrinkDemo, orderDemo}) {
            b.setStyle("-fx-text-fill: white; -fx-background-color: #7f8c8d; -fx-cursor: hand;");
        }

        Label title = new Label("Presets:");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 0 0 0;");

        HBox presets = new HBox(8, title, navBar, centered, equalCols, wrappingCards, holyGrail, shrinkDemo, orderDemo);
        presets.setAlignment(Pos.CENTER_LEFT);
        return presets;
    }

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

    private void addItem() {
        int idx = itemCounter.getAndIncrement();
        Color color = COLORS[idx % COLORS.length];

        Region item = new Region();
        item.setPrefSize(80, 60);
        item.setMinSize(20, 20);
        item.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        String hex = String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        item.setStyle("-fx-background-color: " + hex + ";"
                + " -fx-background-radius: 6;"
                + " -fx-border-color: derive(" + hex + ", -20%);"
                + " -fx-border-radius: 6;");

        Label label = new Label("Item " + idx);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        StackPane wrapper = new StackPane(item, label);
        wrapper.getStyleClass().addAll("flex-item", "flex-item-" + idx);
        wrapper.setMinSize(20, 20);
        wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        wrapper.setPrefSize(80, 60);

        flexBox.getChildren().add(wrapper);
        refreshItemControls();
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
            int idx = i;

            Spinner<Double> growSpinner = new Spinner<>(0.0, 10.0, FlexBox.getGrow(child), 0.5);
            growSpinner.setEditable(true);
            growSpinner.setPrefWidth(70);
            growSpinner.valueProperty().addListener((obs, o, n) -> FlexBox.setGrow(child, n));

            Spinner<Double> shrinkSpinner = new Spinner<>(0.0, 10.0, FlexBox.getShrink(child), 0.5);
            shrinkSpinner.setEditable(true);
            shrinkSpinner.setPrefWidth(70);
            shrinkSpinner.valueProperty().addListener((obs, o, n) -> FlexBox.setShrink(child, n));

            Spinner<Double> basisSpinner = new Spinner<>(-1.0, 500.0, FlexBox.getBasis(child), 10);
            basisSpinner.setEditable(true);
            basisSpinner.setPrefWidth(70);
            basisSpinner.valueProperty().addListener((obs, o, n) -> FlexBox.setBasis(child, n));

            Spinner<Integer> orderSpinner = new Spinner<>(-10, 10, FlexBox.getOrder(child));
            orderSpinner.setEditable(true);
            orderSpinner.setPrefWidth(60);
            orderSpinner.valueProperty().addListener((obs, o, n) -> FlexBox.setOrder(child, n));

            ComboBox<String> alignSelfBox = new ComboBox<>();
            alignSelfBox.getItems().add("auto");
            for (FlexAlignItems v : FlexAlignItems.values()) alignSelfBox.getItems().add(v.name());
            FlexAlignItems selfVal = FlexBox.getAlignSelf(child);
            alignSelfBox.setValue(selfVal == null ? "auto" : selfVal.name());
            alignSelfBox.setPrefWidth(100);
            alignSelfBox.setOnAction(e -> {
                String val = alignSelfBox.getValue();
                if ("auto".equals(val)) {
                    child.getProperties().remove("flexbox-align-self");
                    flexBox.requestLayout();
                } else {
                    FlexBox.setAlignSelf(child, FlexAlignItems.valueOf(val));
                }
            });

            VBox itemBox = new VBox(2,
                    new Label("#" + idx + " ──────────"),
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
