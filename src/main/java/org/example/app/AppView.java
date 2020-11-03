package org.example.app;

import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Objects;

public class AppView {

    private final int MAIN_WINDOW_HEIGHT = 700;
    private final int MAIN_WINDOW_WIDTH = 1200;

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
    ArcGISMap mainMap;
    ArcGISMap eagleMap;
    Button refreshButton;
    Button loadShapefileBtn;
    Button loadGeoDatabaseBtn;
    Button loadOnlineDataBtn;
    ChoiceBox<OnlineDataType> onlineDataTypeChoiceBox;
    TextField onlineDataURLText;
    ChoiceBox<Basemap.Type> basemapChoiceBox;
    Button callOutBtn;
    TextField longitudeText;
    TextField latitudeText;

    MapView mainMapView;
    MapView eagleMapView;

    Stage simpleQueryStage;

    private final AppController controller = new AppController();

    public void start(Stage stage) {
        drawMainWindow(stage);
    }

    private void drawMainWindow(Stage stage) {
        primaryStage = stage;
        initMainWindow();
        initMenuBar();
        initMapView();
        initRefreshButton();
        initEagleMap();
        initShapefileButton();
        initGeoDatabaseButton();
        initOnlineDataButton();
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
            if (Objects.nonNull(simpleQueryStage)) {
                simpleQueryStage.close();
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
        MenuItem identifyQuery = new MenuItem("Identify Query");
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
            }
        });
        queryMenu.getItems().addAll(simpleQuery, identifyQuery);

        menuBar.getMenus().addAll(fileMenu, operationMenu, queryMenu);
        StackPane.setAlignment(menuBar, Pos.TOP_LEFT);
        StackPane.setMargin(menuBar, new Insets(0, 0, 20, 0));
        mainPane.add(menuBar, 0, 0);
    }

    private void initMapView() {
        contentPane = new StackPane();
        contentPane.setMinSize(0, 0);
        contentPane.setPrefSize(StackPane.USE_COMPUTED_SIZE, StackPane.USE_COMPUTED_SIZE);
        contentPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mainPane.add(contentPane, 0, 1);

        mainMapView = new MapView();
        mainMap = new ArcGISMap(Basemap.createImagery());
        StackPane.setAlignment(mainMapView, Pos.TOP_LEFT);
        mainMapView.setMap(mainMap);

        contentPane.getChildren().add(mainMapView);
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
        StackPane.setAlignment(loadShapefileBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(loadShapefileBtn, new Insets(15));
//        mainPane.getChildren().add(loadShapefileBtn);
        contentPane.getChildren().add(loadShapefileBtn);
    }

    private void initGeoDatabaseButton() {
        loadGeoDatabaseBtn = new Button();
        loadGeoDatabaseBtn.setText("Load GeoDatabase");
        loadGeoDatabaseBtn.setOnMouseClicked(mouseEvent -> controller.loadGeoDatabase(primaryStage, mainMapView));
        StackPane.setAlignment(loadGeoDatabaseBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(loadGeoDatabaseBtn, new Insets(15, 130, 15, 15));
        contentPane.getChildren().add(loadGeoDatabaseBtn);
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
            if (!isDragging) {
                Point point = mainMapView.screenToLocation(new Point2D(mouseEvent.getX(), mouseEvent.getY()));
                if (Objects.nonNull(point)) {
                    if (mainMap.getOperationalLayers().size() > 0)
                        controller.clickQuery(mainMapView, (FeatureLayer) mainMap.getOperationalLayers().get(0), point);
                    controller.showCallOut(mainMapView, point);
                }
            } else {
                isDragging = false;
            }
        });
    }

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
