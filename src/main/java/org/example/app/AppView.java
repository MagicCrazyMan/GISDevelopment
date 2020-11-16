package org.example.app;

import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.*;

public class AppView {

    private final int MAIN_WINDOW_HEIGHT = 800;
    private final int MAIN_WINDOW_WIDTH = 1400;

    public enum OnlineDataType {
        ESRI_SERVICE,
        WMS_SERVICE
    }

    Stage primaryStage;
    MenuBar menuBar;
    Menu fileMenu;
    Menu operationMenu;
    Menu queryMenu;
    Menu editMenu;
    GridPane mainPane;
    StackPane contentPane;
    VBox layerPane;
    ArcGISMap mainMap;
    ArcGISMap eagleMap;
    Button refreshButton;
    Button loadShapefileBtn;
    Button loadGeoDatabaseBtn;
    Button loadOnlineDataBtn;
    Button loadRasterBtn;
    ChoiceBox<OnlineDataType> onlineDataTypeChoiceBox;
    TextField onlineDataURLText;
    ChoiceBox<Basemap.Type> basemapChoiceBox;
    Button callOutBtn;
    TextField longitudeText;
    TextField latitudeText;
    ToolBar bottomRightToolbar;

    MapView mainMapView;
    MapView eagleMapView;

    enum RuntimeStageType {
        SIMPLE_QUERY,
        CLICK_QUERY,
        DRAW_GEOMETRY,
        DRAW_OPTIONS
    }

    Map<RuntimeStageType, Stage> runtimeStages = new HashMap<>();

    private enum ClickBehaviours {
        NULL,
        SELECTED_FEATURE,
        IDENTITY,
        DRAWING,
    }

    ClickBehaviours clickBehaviour = ClickBehaviours.NULL;

    private static class DrawingOptions {

        final SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol();
        final SimpleLineSymbol lineSymbol = new SimpleLineSymbol();
        final SimpleFillSymbol fillSymbol = new SimpleFillSymbol();

        public DrawingOptions() {
            // default style
            markerSymbol.setStyle(SimpleMarkerSymbol.Style.CIRCLE);
            markerSymbol.setSize(5);
            markerSymbol.setColor(0xFF0000FF);

            lineSymbol.setStyle(SimpleLineSymbol.Style.SOLID);
            lineSymbol.setMarkerStyle(SimpleLineSymbol.MarkerStyle.NONE);
            lineSymbol.setMarkerPlacement(SimpleLineSymbol.MarkerPlacement.BEGIN);
            lineSymbol.setWidth(2);
            lineSymbol.setColor(0xFF0000FF);

            fillSymbol.setStyle(SimpleFillSymbol.Style.SOLID);
            fillSymbol.setColor(0xFFFFFFFF);
            fillSymbol.setOutline(lineSymbol);
        }
    }

    DrawingOptions drawingOptions;
    PointCollection drawingCollection;

    public enum DrawingType {
        MARKER, LINE, POLYGON
    }

    DrawingType drawingType = DrawingType.MARKER;

    private final AppController controller = new AppController();

    public void start(Stage stage) {
        drawMainWindow(stage);
    }

    private void drawMainWindow(Stage stage) {
        primaryStage = stage;
        initMainWindow();
        initMenuBar();
        initLayersManager();
        initMapView();
        initToolbars();
        initRefreshButton();
        initEagleMap();
        initShapefileButton();
        initGeoDatabaseButton();
        initOnlineDataButton();
        initRasterButton();
        initBasemapSelector();
        initCallOutButton();
        initMapViewClicker();
        primaryStage.show();
    }

    private void initMainWindow() {
        primaryStage.setTitle("Map Window");
        primaryStage.setOnShown(windowEvent -> {
            primaryStage.setMinHeight(primaryStage.getHeight());
            primaryStage.setMinWidth(primaryStage.getWidth());
        });
        primaryStage.setOnCloseRequest(windowEvent -> {
            for (Stage stage : runtimeStages.values()) {
                stage.close();
            }
        });

        mainPane = new GridPane();
        mainPane.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        mainPane.setPrefSize(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
        RowConstraints rowConstraintsHeader = new RowConstraints(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        rowConstraintsHeader.setFillHeight(false);
        RowConstraints rowConstraintsContent = new RowConstraints(0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE);
        rowConstraintsContent.setFillHeight(true);
        rowConstraintsContent.setVgrow(Priority.SOMETIMES);
        ColumnConstraints columnConstraints = new ColumnConstraints(0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE);
        columnConstraints.setFillWidth(true);
        columnConstraints.setHgrow(Priority.SOMETIMES);
        mainPane.getRowConstraints().addAll(rowConstraintsHeader, rowConstraintsContent);
        mainPane.getColumnConstraints().add(columnConstraints);

        primaryStage.setScene(new Scene(mainPane));
    }

    private void initMenuBar() {
        menuBar = new MenuBar();
        menuBar.setViewOrder(-1);

        fileMenu = new Menu("Files");
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(actionEvent -> System.exit(0));
        fileMenu.getItems().addAll(exitMenuItem);

        operationMenu = new Menu("View");
        MenuItem zoomIn = new MenuItem("Zoom In");
        MenuItem zoomOut = new MenuItem("Zoom Out");
        MenuItem zoomFullExtent = new MenuItem("Full Extent");
        MenuItem clockwiseRotate = new MenuItem("Clockwise Rotate");
        MenuItem counterclockwiseRotate = new MenuItem("Counterclockwise Rotate");
        zoomIn.setOnAction(actionEvent -> mainMapView.setViewpointScaleAsync(mainMapView.getMapScale() / 4));
        zoomOut.setOnAction(actionEvent -> mainMapView.setViewpointScaleAsync(mainMapView.getMapScale() * 4));
        // this is a tricky way to achieve zooming to full extent, all we have to do is that, set scale to a large enough value (but not too large)
        // MapView will automatically limit scale to the max scale value
        zoomFullExtent.setOnAction(actionEvent -> mainMapView.setViewpointScaleAsync(1E20));
        clockwiseRotate.setOnAction(actionEvent -> mainMapView.setViewpointRotationAsync(mainMapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getRotation() + Math.PI / 2));
        counterclockwiseRotate.setOnAction(actionEvent -> mainMapView.setViewpointRotationAsync(mainMapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getRotation() - Math.PI / 2));
        operationMenu.getItems().addAll(zoomIn, zoomOut, zoomFullExtent, clockwiseRotate, counterclockwiseRotate);

        queryMenu = new Menu("Query");
        MenuItem simpleQuery = new MenuItem("Simple Query");
        MenuItem clickQuery = new MenuItem("Click Query");
        simpleQuery.setOnAction(actionEvent -> {
            if (!runtimeStages.containsKey(RuntimeStageType.SIMPLE_QUERY)) {
                StackPane simpleQueryPane = new StackPane();
                ToolBar toolBar = new ToolBar();
                Label nameLabel = new Label("Field Name");
                Label valueLabel = new Label("Field Value");
                TextField nameText = new TextField();
                TextField valueText = new TextField();
                Button queryBtn = new Button("Query");
                queryBtn.setOnAction(actionEvent1 -> {
                    if (!nameText.getText().isEmpty() && !valueText.getText().isEmpty()) {
                        QueryParameters queryParameters = new QueryParameters();
                        queryParameters.setWhereClause(String.format("upper(%s) LIKE '%%%s%%'", nameText.getText(), valueText.getText()));
                        LayerList layers = mainMap.getOperationalLayers();
                        if (layers.size() > 0) {
                            controller.simpleQuery(mainMapView, (FeatureLayer) layers.get(0), queryParameters);
                        }
                    }
                });
                nameText.setMinWidth(100);
                valueText.setMinWidth(100);
                toolBar.getItems().addAll(nameLabel, nameText, valueLabel, valueText, queryBtn);
                simpleQueryPane.getChildren().add(toolBar);

                Stage simpleQueryStage = new Stage();
                simpleQueryStage.setTitle("Simple Query");
                simpleQueryStage.setResizable(false);
                simpleQueryStage.setScene(new Scene(simpleQueryPane));
                simpleQueryStage.setOnCloseRequest(windowEvent -> runtimeStages.remove(RuntimeStageType.SIMPLE_QUERY));
                simpleQueryStage.show();
                runtimeStages.put(RuntimeStageType.SIMPLE_QUERY, simpleQueryStage);
            } else {
                runtimeStages.get(RuntimeStageType.SIMPLE_QUERY).toFront();
            }
        });
        clickQuery.setOnAction(actionEvent -> {
            if (!runtimeStages.containsKey(RuntimeStageType.CLICK_QUERY)) {
                StackPane stackPane = new StackPane();
                ToolBar toolBar = new ToolBar();
                RadioButton selectFeatureBtn = new RadioButton("Select Feature");
                RadioButton identityBtn = new RadioButton("Identity");
                clickBehaviour = ClickBehaviours.SELECTED_FEATURE;
                selectFeatureBtn.setSelected(true); // select feature is default
                selectFeatureBtn.setOnAction(actionEvent1 -> {
                    identityBtn.setSelected(false);
                    clickBehaviour = ClickBehaviours.SELECTED_FEATURE;
                });
                identityBtn.setOnAction(actionEvent1 -> {
                    selectFeatureBtn.setSelected(false);
                    clickBehaviour = ClickBehaviours.IDENTITY;
                });
                toolBar.getItems().addAll(selectFeatureBtn, identityBtn);
                stackPane.getChildren().add(toolBar);

                Stage clickQueryStage = new Stage();
                clickQueryStage.setScene(new Scene(stackPane));
                clickQueryStage.setResizable(false);
                clickQueryStage.setTitle("Click Query");
                clickQueryStage.setOnCloseRequest(windowEvent -> runtimeStages.remove(RuntimeStageType.CLICK_QUERY));
                clickQueryStage.show();
                runtimeStages.put(RuntimeStageType.CLICK_QUERY, clickQueryStage);
            } else {
                runtimeStages.get(RuntimeStageType.CLICK_QUERY).toFront();
            }
        });
        queryMenu.getItems().addAll(simpleQuery, clickQuery);

        editMenu = new Menu("Edit");
        MenuItem drawGeometry = new MenuItem("Draw");
        drawingOptions = new DrawingOptions();
        final Map<String, EventHandler<ActionEvent>> eventHandlerMap = new LinkedHashMap<>();
//        eventHandlerMap.put("Add Layer", actionEvent12 -> {

//        });
        eventHandlerMap.put("Draw Point", actionEvent12 -> {
            clickBehaviour = ClickBehaviours.DRAWING;
            if (!drawingType.equals(DrawingType.MARKER)) {
                drawingCollection.clear();
                drawingType = DrawingType.MARKER;
            }
        });
        eventHandlerMap.put("Draw Line", actionEvent12 -> {
            clickBehaviour = ClickBehaviours.DRAWING;
            if (!drawingType.equals(DrawingType.LINE)) {
                drawingCollection.clear();
                drawingType = DrawingType.LINE;
            }
        });
        eventHandlerMap.put("Draw Polygon", actionEvent12 -> {
            clickBehaviour = ClickBehaviours.DRAWING;
            if (!drawingType.equals(DrawingType.POLYGON)) {
                drawingCollection.clear();
                drawingType = DrawingType.POLYGON;
            }
        });
        eventHandlerMap.put("Clear All", actionEvent12 -> controller.clearTemporaryGeometry(mainMapView, drawingCollection));
        eventHandlerMap.put("Options", actionEvent12 -> {
            if (!runtimeStages.containsKey(RuntimeStageType.DRAW_OPTIONS)) {
                TabPane tabPane = new TabPane();
                VBox markerPane = new VBox();
                StackPane linePane = new StackPane();
                StackPane polygonPane = new StackPane();

                // init marker pane
                {
                    markerPane.setSpacing(5);
                    ToolBar styleToolbar = new ToolBar();
                    styleToolbar.setStyle("-fx-background-color: transparent");
                    Label styleLabel = new Label("Style");
                    ChoiceBox<SimpleMarkerSymbol.Style> styleChoiceBox = new ChoiceBox<>();
                    styleChoiceBox.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(SimpleMarkerSymbol.Style style) {
                            return style.name();
                        }

                        @Override
                        public SimpleMarkerSymbol.Style fromString(String s) {
                            return SimpleMarkerSymbol.Style.valueOf(s);
                        }
                    });
                    styleChoiceBox.getItems().addAll(SimpleMarkerSymbol.Style.values());
                    styleChoiceBox.setValue(drawingOptions.markerSymbol.getStyle());
                    styleChoiceBox.setPrefWidth(200);
                    styleToolbar.getItems().addAll(styleLabel, styleChoiceBox);

                    ToolBar sizeToolbar = new ToolBar();
                    sizeToolbar.setStyle("-fx-background-color: transparent");
                    Label sizeLabel = new Label("Size");
                    Label currentSizeLabel = new Label("Current Size: " + (int) drawingOptions.markerSymbol.getSize());
                    Slider sizeSlider = new Slider();
                    sizeSlider.setShowTickLabels(true);
                    sizeSlider.setShowTickMarks(true);
                    sizeSlider.setSnapToTicks(true);
                    sizeSlider.setMin(1);
                    sizeSlider.setMax(50);
                    sizeSlider.setValue(drawingOptions.markerSymbol.getSize());
                    sizeSlider.valueProperty().addListener((observableValue, number, t1) -> currentSizeLabel.setText(String.format("Current Size: %d", t1.intValue())));
                    sizeToolbar.getItems().addAll(sizeLabel, sizeSlider, currentSizeLabel);

                    ToolBar colorToolbar = new ToolBar();
                    colorToolbar.setStyle("-fx-background-color: transparent");
                    Label colorLabel = new Label("Color");
                    ColorPicker colorPicker = new ColorPicker();
                    int color = drawingOptions.markerSymbol.getColor();
                    colorPicker.setValue(Color.rgb((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, ((color >>> 24) & 0xFF) / 255.0));
                    colorToolbar.getItems().addAll(colorLabel, colorPicker);

                    ToolBar submitToolbar = new ToolBar();
                    submitToolbar.setStyle("-fx-background-color: transparent");
                    Button applyBtn = new Button("Apply");
                    applyBtn.setOnAction(actionEvent -> {
                        drawingOptions.markerSymbol.setSize((float) sizeSlider.getValue());
                        drawingOptions.markerSymbol.setStyle(styleChoiceBox.getValue());
                        Color newColor = colorPicker.getValue();
                        int r = ((int) newColor.getRed() * 255) << 16;
                        int g = ((int) newColor.getGreen() * 255) << 8;
                        int b = ((int) newColor.getBlue() * 255);
                        int a = ((int) newColor.getOpacity() * 255) << 24;
                        drawingOptions.markerSymbol.setColor(a | r | g | b);
                    });
                    submitToolbar.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                    submitToolbar.getItems().add(applyBtn);

                    markerPane.getChildren().addAll(styleToolbar, sizeToolbar, colorToolbar, submitToolbar);
                }

                Tab markerTab = new Tab("Marker Symbol");
                markerTab.setContent(markerPane);
                markerTab.setClosable(false);
                Tab lineTab = new Tab("Line Symbol");
                lineTab.setContent(linePane);
                lineTab.setClosable(false);
                Tab polygonTab = new Tab("Polygon Symbol");
                polygonTab.setContent(polygonPane);
                polygonTab.setClosable(false);
                tabPane.getTabs().addAll(markerTab, lineTab, polygonTab);

                Stage drawingOptionsStage = new Stage();
                drawingOptionsStage.setTitle("Draw Options");
                drawingOptionsStage.setResizable(false);
                drawingOptionsStage.setScene(new Scene(tabPane));
                drawingOptionsStage.setOnCloseRequest(windowEvent -> runtimeStages.remove(RuntimeStageType.DRAW_OPTIONS));
                drawingOptionsStage.show();
                runtimeStages.put(RuntimeStageType.DRAW_OPTIONS, drawingOptionsStage);
            } else {
                runtimeStages.get(RuntimeStageType.DRAW_OPTIONS).toFront();
            }
        });
        drawGeometry.setOnAction(actionEvent -> {
            if (!runtimeStages.containsKey(RuntimeStageType.DRAW_GEOMETRY)) {
                StackPane stackPane = new StackPane();
                ToolBar toolBar = new ToolBar();
                for (Map.Entry<String, EventHandler<ActionEvent>> eventHandlerEntry : eventHandlerMap.entrySet()) {
                    Button btn = new Button(eventHandlerEntry.getKey());
                    btn.setOnAction(eventHandlerEntry.getValue());
                    btn.setPrefWidth(250);
                    toolBar.getItems().add(btn);
                }
                toolBar.setStyle("-fx-background-color: transparent");
                toolBar.setOrientation(Orientation.VERTICAL);
                stackPane.getChildren().add(toolBar);

                Stage drawGeometryStage = new Stage();
                drawGeometryStage.setTitle("Draw");
                drawGeometryStage.setResizable(false);
                drawGeometryStage.setScene(new Scene(stackPane));
                drawGeometryStage.setOnCloseRequest(windowEvent -> runtimeStages.remove(RuntimeStageType.CLICK_QUERY));
                drawGeometryStage.show();
                runtimeStages.put(RuntimeStageType.DRAW_GEOMETRY, drawGeometryStage);
            } else {
                runtimeStages.get(RuntimeStageType.DRAW_GEOMETRY).toFront();
            }
        });
        editMenu.getItems().add(drawGeometry);

        menuBar.getMenus().addAll(fileMenu, operationMenu, queryMenu, editMenu);
        GridPane.setColumnSpan(menuBar, GridPane.REMAINING);
        mainPane.add(menuBar, 0, 0);
    }

    private void initLayersManager() {
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setMinWidth(200);
        columnConstraints.setPrefWidth(200);
        columnConstraints.setMaxWidth(200);
        mainPane.getColumnConstraints().add(0, columnConstraints);

        ScrollPane scrollPane = new ScrollPane();
        layerPane = new VBox();
        layerPane.setSpacing(15);
        layerPane.setPadding(new Insets(5));
        scrollPane.setOnDragOver(dragEvent -> {
            Dragboard db = dragEvent.getDragboard();
            if (db.hasFiles()) {
                dragEvent.acceptTransferModes(TransferMode.COPY);
            } else {
                dragEvent.consume();
            }
        });
        scrollPane.setOnDragDropped(dragEvent -> {
            Dragboard db = dragEvent.getDragboard();
            if (db.hasFiles() && db.getFiles().size() > 0) {
                File file = db.getFiles().get(0);
                controller.loadShapefile(mainMapView, file);
            }
        });
        scrollPane.setContent(layerPane);
        mainPane.add(scrollPane, 0, 1);
    }

    private void initMapView() {
        contentPane = new StackPane();
        contentPane.setMinSize(0, 0);
        contentPane.setPrefSize(StackPane.USE_COMPUTED_SIZE, StackPane.USE_COMPUTED_SIZE);
        contentPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mainPane.add(contentPane, 1, 1);

        mainMapView = new MapView();
        mainMap = new ArcGISMap(Basemap.createImagery());
        StackPane.setAlignment(mainMapView, Pos.TOP_LEFT);
        mainMapView.setMap(mainMap);

        mainMap.addDoneLoadingListener(() -> {
            if (mainMap.getLoadStatus().equals(LoadStatus.LOADED)) {
                drawingCollection = new PointCollection(mainMap.getSpatialReference());
            }
        });
        // add listener, any layer changed will notify layer manager
        mainMap.getOperationalLayers().addListChangedListener(listChangedEvent -> {
            if (Objects.nonNull(layerPane)) {
                switch (listChangedEvent.getAction()) {
                    case ADDED: {
                        Layer layer = listChangedEvent.getItems().get(0);
                        layer.addDoneLoadingListener(() -> {
                            if (layer.getLoadStatus().equals(LoadStatus.LOADED)) {
                                String name = layer.getName();
                                String id = layer.getId();
                                RadioButton radioButton = new RadioButton(name);
                                radioButton.setId(id);
                                radioButton.setSelected(true);
                                radioButton.selectedProperty().addListener((observableValue, aBoolean, t1) -> layer.setVisible(t1));
                                radioButton.setOnContextMenuRequested(contextMenuEvent -> {
                                    ContextMenu contextMenu = new ContextMenu();
                                    MenuItem zoomTo = new MenuItem("Zoom To");
                                    MenuItem remove = new MenuItem("Remove");
                                    zoomTo.setOnAction(actionEvent -> mainMapView.setViewpointGeometryAsync(layer.getFullExtent(), 50));
                                    remove.setOnAction(actionEvent -> mainMap.getOperationalLayers().remove(layer));

                                    contextMenu.getItems().addAll(zoomTo, remove);
                                    contextMenu.show(radioButton, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
                                });
                                layerPane.getChildren().add(radioButton);
                            }
                        });
                        break;
                    }
                    case REMOVED: {
                        Layer layer = listChangedEvent.getItems().get(0);
                        layerPane.getChildren().removeIf(node -> node.getId().equals(layer.getId()));
                        break;
                    }
                }
            }
        });

        contentPane.getChildren().add(mainMapView);
    }

    private void initToolbars() {
        bottomRightToolbar = new ToolBar();
        bottomRightToolbar.setPrefSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        bottomRightToolbar.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        bottomRightToolbar.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        bottomRightToolbar.setStyle("-fx-background-color: transparent");
        StackPane.setAlignment(bottomRightToolbar, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bottomRightToolbar, new Insets(15));

        contentPane.getChildren().addAll(bottomRightToolbar);
    }

    private void initEagleMap() {
        eagleMapView = new MapView();
        eagleMapView.setMaxSize(300, 200);
        eagleMap = new ArcGISMap(Basemap.createStreets());
        eagleMapView.setEnableMousePan(false);
        eagleMapView.setEnableMouseZoom(false);
        eagleMapView.setEnableTouchPan(false);
        eagleMapView.setEnableTouchRotate(false);
        eagleMapView.setEnableTouchZoom(false);
        eagleMapView.setMap(eagleMap);
        StackPane.setMargin(eagleMapView, new Insets(30, 15, 15, 15));
        StackPane.setAlignment(eagleMapView, Pos.TOP_RIGHT);
        contentPane.getChildren().add(eagleMapView);

        GraphicsOverlay extentGraphicOverlay = new GraphicsOverlay();
        eagleMapView.getGraphicsOverlays().add(extentGraphicOverlay);
        SimpleLineSymbol extentPolygonOutline = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFF0000, 2);
        SimpleFillSymbol extentPolygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0x44FF0000, extentPolygonOutline);
        // add viewpoint changed listener to mainMapView, and sync the viewpoint to eagleMapView
        mainMapView.addViewpointChangedListener(viewpointChangedEvent -> {
            // setViewpointAsync may act in a higher performance, but it will also cause delay (because eagleMapView update viewpoint async from mainMapView, not sync)
            // so, for better visual effect, we will use sync method here
//            eagleMapView.setViewpoint(viewpointChangedEvent.getSource().getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY));
            // and update extent polygon here
            extentGraphicOverlay.getGraphics().clear();
            extentGraphicOverlay.getGraphics().add(new Graphic(mainMapView.getVisibleArea().getExtent(), extentPolygonSymbol));
        });
    }

    private void initRefreshButton() {
        refreshButton = new Button("Refresh Basemap");
        refreshButton.setOnAction(actionEvent -> mainMap.loadAsync());
        StackPane.setAlignment(refreshButton, Pos.TOP_LEFT);
        StackPane.setMargin(refreshButton, new Insets(15));
        contentPane.getChildren().add(refreshButton);
    }

    private void initBasemapSelector() {
        basemapChoiceBox = new ChoiceBox<>();
        basemapChoiceBox.setMinWidth(300);
        basemapChoiceBox.getItems().addAll(Basemap.Type.values());
        basemapChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Basemap.Type type) {
                return type.name();
            }

            @Override
            public Basemap.Type fromString(String s) {
                return Basemap.Type.valueOf(s);
            }
        });
        basemapChoiceBox.getSelectionModel().selectedItemProperty().addListener((observableValue, type, t1) -> {
            if (Objects.nonNull(mainMapView)) {
                changeBasemapByType(t1);
            }
        });
        basemapChoiceBox.setValue(Basemap.Type.IMAGERY);

        StackPane.setAlignment(basemapChoiceBox, Pos.TOP_RIGHT);
        StackPane.setMargin(basemapChoiceBox, new Insets(250, 15, 15, 15));
        contentPane.getChildren().add(basemapChoiceBox);
    }

    private void changeBasemapByType(Basemap.Type type) {
        switch (type) {
            case OCEANS:
                mainMapView.getMap().setBasemap(Basemap.createOceans());
                break;
            case IMAGERY:
                mainMapView.getMap().setBasemap(Basemap.createImagery());
                break;
            case STREETS:
                mainMapView.getMap().setBasemap(Basemap.createStreets());
                break;
            case TOPOGRAPHIC:
                mainMapView.getMap().setBasemap(Basemap.createTopographic());
                break;
            case STREETS_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createStreetsVector());
                break;
            case OPEN_STREET_MAP:
                mainMapView.getMap().setBasemap(Basemap.createOpenStreetMap());
                break;
            case LIGHT_GRAY_CANVAS:
                mainMapView.getMap().setBasemap(Basemap.createLightGrayCanvas());
                break;
            case NAVIGATION_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createNavigationVector());
                break;
            case TOPOGRAPHIC_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createTopographicVector());
                break;
            case IMAGERY_WITH_LABELS:
                mainMapView.getMap().setBasemap(Basemap.createImageryWithLabels());
                break;
            case NATIONAL_GEOGRAPHIC:
                mainMapView.getMap().setBasemap(Basemap.createNationalGeographic());
                break;
            case TERRAIN_WITH_LABELS:
                mainMapView.getMap().setBasemap(Basemap.createTerrainWithLabels());
                break;
            case STREETS_NIGHT_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createStreetsNightVector());
                break;
            case DARK_GRAY_CANVAS_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createDarkGrayCanvasVector());
                break;
            case LIGHT_GRAY_CANVAS_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createLightGrayCanvasVector());
                break;
            case IMAGERY_WITH_LABELS_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createImageryWithLabelsVector());
                break;
            case STREETS_WITH_RELIEF_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createStreetsWithReliefVector());
                break;
            case TERRAIN_WITH_LABELS_VECTOR:
                mainMapView.getMap().setBasemap(Basemap.createTerrainWithLabelsVector());
                break;
        }
    }

    private void initShapefileButton() {
        loadShapefileBtn = new Button();
        loadShapefileBtn.setText("Load Shapefile");
        loadShapefileBtn.setDefaultButton(true);
        loadShapefileBtn.setOnMouseClicked(mouseEvent -> controller.loadShapefile(primaryStage, mainMapView));
        bottomRightToolbar.getItems().add(loadShapefileBtn);
    }

    private void initGeoDatabaseButton() {
        loadGeoDatabaseBtn = new Button();
        loadGeoDatabaseBtn.setText("Load GeoDatabase");
        loadGeoDatabaseBtn.setOnMouseClicked(mouseEvent -> controller.loadGeoDatabase(primaryStage, mainMapView));
        bottomRightToolbar.getItems().add(loadGeoDatabaseBtn);
    }

    private void initOnlineDataButton() {
        onlineDataURLText = new TextField();
        onlineDataURLText.setMaxWidth(400);
        StackPane.setAlignment(onlineDataURLText, Pos.BOTTOM_LEFT);
        StackPane.setMargin(onlineDataURLText, new Insets(15, 15, 15, 150));

        onlineDataTypeChoiceBox = new ChoiceBox<>();
        onlineDataTypeChoiceBox.setMinWidth(100);
        onlineDataTypeChoiceBox.getItems().addAll(OnlineDataType.values());
        onlineDataTypeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(OnlineDataType onlineDataType) {
                return onlineDataType.name();
            }

            @Override
            public OnlineDataType fromString(String s) {
                return OnlineDataType.valueOf(s);
            }
        });
        onlineDataTypeChoiceBox.getSelectionModel().select(OnlineDataType.ESRI_SERVICE);
        StackPane.setAlignment(onlineDataTypeChoiceBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(onlineDataTypeChoiceBox, new Insets(15, 15, 15, 560));

        loadOnlineDataBtn = new Button();
        loadOnlineDataBtn.setText("load online data");
        loadOnlineDataBtn.setOnMouseClicked(mouseEvent -> controller.loadOnlineData(mainMapView, onlineDataURLText.getText(), onlineDataTypeChoiceBox.getSelectionModel().getSelectedItem()));
        StackPane.setAlignment(loadOnlineDataBtn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(loadOnlineDataBtn, new Insets(15));

        contentPane.getChildren().addAll(loadOnlineDataBtn, onlineDataURLText, onlineDataTypeChoiceBox);
    }

    private void initRasterButton() {
        loadRasterBtn = new Button("Load Raster");
        loadRasterBtn.setOnAction(actionEvent -> controller.loadRaster(primaryStage, mainMapView));
        bottomRightToolbar.getItems().add(loadRasterBtn);
    }

    private void initCallOutButton() {
        callOutBtn = new Button("Show Call Out At Position");
        longitudeText = new TextField();
        latitudeText = new TextField();
        longitudeText.setMaxWidth(100);
        latitudeText.setMaxWidth(100);
        callOutBtn.setOnMouseClicked(mouseEvent -> controller.showCallOut(mainMapView, longitudeText.getText(), latitudeText.getText()));
        StackPane.setAlignment(callOutBtn, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(longitudeText, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(latitudeText, Pos.BOTTOM_LEFT);
        StackPane.setMargin(callOutBtn, new Insets(15, 15, 50, 15));
        StackPane.setMargin(longitudeText, new Insets(15, 15, 50, 210));
        StackPane.setMargin(latitudeText, new Insets(15, 15, 50, 315));
        contentPane.getChildren().addAll(callOutBtn, longitudeText, latitudeText);
    }

    boolean isDragging = false;

    private void initMapViewClicker() {
        mainMapView.setOnDragDetected(mouseEvent -> isDragging = true);
        mainMapView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 1) {
                if (!isDragging) {
                    Point2D screenPoint = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                    Point mapPoint = mainMapView.screenToLocation(screenPoint);
                    if (Objects.nonNull(mapPoint)) {
                        switch (clickBehaviour) {
                            case IDENTITY: {
                                if (mainMap.getOperationalLayers().size() > 0) {
                                    controller.clickQuery(mainMapView, (FeatureLayer) mainMap.getOperationalLayers().get(0), screenPoint);
                                }
                                break;
                            }
                            case SELECTED_FEATURE: {
                                if (mainMap.getOperationalLayers().size() > 0) {
                                    controller.clickQuery(mainMapView, (FeatureLayer) mainMap.getOperationalLayers().get(0), mapPoint);
                                }
                                break;
                            }
                            case DRAWING: {
                                drawingCollection.add(mapPoint);
                                switch (drawingType) {
                                    case POLYGON:
                                        controller.drawTemporaryGeometry(mainMapView, drawingType, drawingOptions.fillSymbol, drawingCollection);
                                        break;
                                    case LINE:
                                        controller.drawTemporaryGeometry(mainMapView, drawingType, drawingOptions.lineSymbol, drawingCollection);
                                        break;
                                    case MARKER:
                                        controller.drawTemporaryGeometry(mainMapView, drawingType, drawingOptions.markerSymbol, drawingCollection);
                                        break;
                                }
                                break;
                            }
                        }
                        // controller.showCallOut(mainMapView, mapPoint);
                    }
                } else {
                    isDragging = false;
                }
            } else {
                if (clickBehaviour == ClickBehaviours.DRAWING) {
                    switch (drawingType) {
                        case POLYGON:
                        case LINE:
                        case MARKER:
                            break;
                    }
                }
            }
        });
    }

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
