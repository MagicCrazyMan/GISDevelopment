package org.example.app;

import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Point;
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
import com.esri.arcgisruntime.util.ListChangedEvent;
import com.esri.arcgisruntime.util.ListChangedListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Stack;

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
    GridPane mainPane;
    StackPane contentPane;
    ListView<RadioButton> layerPane;
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
    ToolBar bottomRightToolbar;
    ToolBar bottomLeftToolbar;
    ToolBar topLeftToolbar;
    ToolBar topRightToolbar;

    MapView mainMapView;
    MapView eagleMapView;

    Stage simpleQueryStage;
    Stage clickQueryStage;
    ClickQueryType clickQueryType = ClickQueryType.NULL;

    private enum ClickQueryType {
        NULL,
        SELECTED_FEATURE,
        IDENTITY
    }

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
        initMapViewClicker();
        primaryStage.show();
    }

    private void initMainWindow() {
        primaryStage.setTitle("Map Window");
        primaryStage.setOnShown(windowEvent -> {
            primaryStage.setMinHeight(primaryStage.getHeight());
            primaryStage.setMinWidth(primaryStage.getWidth());
        });
        primaryStage.setOnCloseRequest(windowEvent -> dispose());

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
            if (Objects.isNull(simpleQueryStage)) {
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

                simpleQueryStage = new Stage();
                simpleQueryStage.setTitle("Simple Query");
                simpleQueryStage.setResizable(false);
                simpleQueryStage.setScene(new Scene(simpleQueryPane));
                simpleQueryStage.setOnCloseRequest(windowEvent -> simpleQueryStage = null);
                simpleQueryStage.show();
            } else {
                simpleQueryStage.toFront();
            }
        });
        clickQuery.setOnAction(actionEvent -> {
            if (Objects.isNull(clickQueryStage)) {
                StackPane stackPane = new StackPane();
                ToolBar toolBar = new ToolBar();
                RadioButton selectFeatureBtn = new RadioButton("Select Feature");
                RadioButton identityBtn = new RadioButton("Identity");
                clickQueryType = ClickQueryType.SELECTED_FEATURE;
                selectFeatureBtn.setSelected(true); // select feature is default
                selectFeatureBtn.setOnAction(actionEvent1 -> {
                    identityBtn.setSelected(false);
                    clickQueryType = ClickQueryType.SELECTED_FEATURE;
                });
                identityBtn.setOnAction(actionEvent1 -> {
                    selectFeatureBtn.setSelected(false);
                    clickQueryType = ClickQueryType.IDENTITY;
                });
                toolBar.getItems().addAll(selectFeatureBtn, identityBtn);
                stackPane.getChildren().add(toolBar);

                clickQueryStage = new Stage();
                clickQueryStage.setScene(new Scene(stackPane));
                clickQueryStage.setResizable(false);
                clickQueryStage.setTitle("Click Query");
                clickQueryStage.setOnCloseRequest(windowEvent -> {
                    clickQueryStage = null;
                    clickQueryType = ClickQueryType.NULL;
                });
                clickQueryStage.show();
            } else {
                clickQueryStage.toFront();
            }
        });
        queryMenu.getItems().addAll(simpleQuery, clickQuery);

        menuBar.getMenus().addAll(fileMenu, operationMenu, queryMenu);
        GridPane.setColumnSpan(menuBar, GridPane.REMAINING);
        mainPane.add(menuBar, 0, 0);
    }

    private void initLayersManager() {
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setMinWidth(200);
        columnConstraints.setPrefWidth(200);
        columnConstraints.setMaxWidth(200);
        mainPane.getColumnConstraints().add(0, columnConstraints);

        layerPane = new ListView<>();
        layerPane.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // drag and drop should first calls acceptTransferModes() on setOnDragOver()
        // and when things dropped, setOnDragDropped() will be called, otherwise, setOnDragDropped() may not raise
        layerPane.setOnDragOver(dragEvent -> {
            Dragboard db = dragEvent.getDragboard();
            if (db.hasFiles()) {
                dragEvent.acceptTransferModes(TransferMode.ANY);
            } else {
                dragEvent.consume();
            }
        });
        layerPane.setOnDragDropped(dragEvent -> {
            Dragboard db = dragEvent.getDragboard();
            if (db.hasFiles() && db.getFiles().size() > 0) {
                for (File file : db.getFiles()) {
                    controller.routeLoadFile(mainMapView, file);
                }
            }
        });
        mainPane.add(layerPane, 0, 1);
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
                                layerPane.getItems().add(radioButton);
                            }
                        });
                        break;
                    }
                    case REMOVED: {
                        Layer layer = listChangedEvent.getItems().get(0);
                        layerPane.getItems().removeIf(node -> node.getId().equals(layer.getId()));
                        break;
                    }
                }
            }
        });

        contentPane.getChildren().add(mainMapView);
    }

    private void initToolbars() {
        bottomRightToolbar = new ToolBar();
        bottomLeftToolbar = new ToolBar();
        topLeftToolbar = new ToolBar();
        topRightToolbar = new ToolBar();

        bottomRightToolbar.setPrefSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        bottomRightToolbar.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        bottomRightToolbar.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        bottomRightToolbar.setStyle("-fx-background-color: transparent");
        StackPane.setAlignment(bottomRightToolbar, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bottomRightToolbar, new Insets(15));
        bottomLeftToolbar.setPrefSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        bottomLeftToolbar.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        bottomLeftToolbar.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        bottomLeftToolbar.setStyle("-fx-background-color: transparent");
        StackPane.setAlignment(bottomLeftToolbar, Pos.BOTTOM_LEFT);
        StackPane.setMargin(bottomLeftToolbar, new Insets(15));
        topLeftToolbar.setPrefSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        topLeftToolbar.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        topLeftToolbar.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        topLeftToolbar.setStyle("-fx-background-color: transparent");
        StackPane.setAlignment(topLeftToolbar, Pos.TOP_LEFT);
        StackPane.setMargin(topLeftToolbar, new Insets(15));
        topRightToolbar.setPrefSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        topRightToolbar.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        topRightToolbar.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
        topRightToolbar.setStyle("-fx-background-color: transparent");
        topRightToolbar.setOrientation(Orientation.VERTICAL);
        StackPane.setAlignment(topRightToolbar, Pos.TOP_RIGHT);
        StackPane.setMargin(topRightToolbar, new Insets(15));

        contentPane.getChildren().addAll(bottomRightToolbar, bottomLeftToolbar, topLeftToolbar, topRightToolbar);
    }

    private void initEagleMap() {
        eagleMapView = new MapView();
        eagleMapView.setPrefSize(300, 200);
        eagleMap = new ArcGISMap(Basemap.createStreets());
        eagleMapView.setEnableMousePan(false);
        eagleMapView.setEnableMouseZoom(false);
        eagleMapView.setEnableTouchPan(false);
        eagleMapView.setEnableTouchRotate(false);
        eagleMapView.setEnableTouchZoom(false);
        eagleMapView.setMap(eagleMap);
        topRightToolbar.getItems().add(eagleMapView);

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
        topLeftToolbar.getItems().add(refreshButton);
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

        topRightToolbar.getItems().add(basemapChoiceBox);
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
        onlineDataURLText.setPrefWidth(200);

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

        loadOnlineDataBtn = new Button();
        loadOnlineDataBtn.setText("Load Online Data");
        loadOnlineDataBtn.setOnMouseClicked(mouseEvent -> controller.loadOnlineData(mainMapView, onlineDataURLText.getText(), onlineDataTypeChoiceBox.getSelectionModel().getSelectedItem()));

        bottomLeftToolbar.getItems().addAll(loadOnlineDataBtn, onlineDataURLText, onlineDataTypeChoiceBox);
    }

    private void initRasterButton() {
        loadRasterBtn = new Button("Load Raster");
        loadRasterBtn.setOnAction(actionEvent -> controller.loadRaster(primaryStage, mainMapView));
        bottomRightToolbar.getItems().add(loadRasterBtn);
    }

    boolean isDragging = false;

    private void initMapViewClicker() {
        mainMapView.setOnDragDetected(mouseEvent -> isDragging = true);
        mainMapView.setOnMouseClicked(mouseEvent -> {
            if (!isDragging) {
                Point2D screenPoint = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                Point mapPoint = mainMapView.screenToLocation(screenPoint);
                if (Objects.nonNull(mapPoint)) {
                    if (clickQueryType == ClickQueryType.NULL) {
                        controller.showCallOut(mainMapView, mapPoint);
                    } else {
                        if (layerPane.getSelectionModel().getSelectedIndex() != -1) {
                            Layer layer = findLayerById(layerPane.getSelectionModel().getSelectedItem().getId());
                            if (layer instanceof FeatureLayer) {
                                switch (clickQueryType) {
                                    case IDENTITY: {
                                        controller.clickQuery(mainMapView, (FeatureLayer) layer, screenPoint);
                                        break;
                                    }
                                    case SELECTED_FEATURE: {
                                        controller.clickQuery(mainMapView, (FeatureLayer) layer, mapPoint);
                                        break;
                                    }
                                }
                            }
                        } else {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("No Selected Layer");
                            alert.setContentText("Please select a layer first from Layer Manager on the left");
                            alert.showAndWait();
                        }
                    }
                }
            } else {
                isDragging = false;
            }
        });
    }

    private Layer findLayerById(@NotNull String id) {
        if (Objects.nonNull(mainMap)) {
            for (Layer layer : mainMap.getOperationalLayers()) {
                if (layer.getId().equals(id)) {
                    return layer;
                }
            }
        }
        return null;
    }

    public void dispose() {
        if (Objects.nonNull(simpleQueryStage)) {
            simpleQueryStage.close();
        }
        if (Objects.nonNull(clickQueryStage)) {
            clickQueryStage.close();
        }
        if (Objects.nonNull(eagleMapView)) {
            eagleMapView.dispose();
        }
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
