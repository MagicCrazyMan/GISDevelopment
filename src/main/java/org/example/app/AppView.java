package org.example.app;

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.lang.reflect.Method;
import java.util.Objects;

public class AppView {

    private final int MAIN_WINDOW_HEIGHT = 700;
    private final int MAIN_WINDOW_WIDTH = 1200;

    Stage primaryStage;
    MenuBar menuBar;
    Menu fileMenu;
    Menu operationMenu;
    StackPane mainPane;
    ArcGISMap mainMap;
    ArcGISMap eagleMap;
    Button loadShapefileBtn;
    Button loadGeoDatabaseBtn;
    Button loadOnlineDataBtn;
    TextField onlineDataURLText;
    ChoiceBox<Basemap.Type> basemapMenuBtn;
    Button callOutBtn;
    TextField longitudeText;
    TextField latitudeText;

    MapView mainMapView;
    MapView eagleMapView;

    private AppController controller = new AppController();

    public void start(Stage stage) {
        drawMainWindow(stage);
    }

    private void drawMainWindow(Stage stage) {
        primaryStage = stage;
        initMainWindow();
        initMenuBar();
        initMapView();
        initEagleMap();
        initShapefileButton();
        initGeoDatabaseButton();
        initOnlineDataButton();
        initBasemapSelector();
        initCallOutButton();
        initClickToShowCallOut();
    }

    private void initMenuBar() {
        menuBar = new MenuBar();
        menuBar.setViewOrder(-1);

        fileMenu = new Menu("Files");
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(actionEvent -> System.exit(0));
        fileMenu.getItems().addAll(exitMenuItem);

        operationMenu = new Menu("Operations");
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
        operationMenu.getItems().addAll(zoomIn, zoomOut, zoomFullExtent, clockwiseRotate,counterclockwiseRotate);

        menuBar.getMenus().addAll(fileMenu, operationMenu);
        StackPane.setAlignment(menuBar, Pos.TOP_LEFT);
        StackPane.setMargin(menuBar, new Insets(0, 0, 20, 0));
        mainPane.getChildren().add(menuBar);
    }

    private void initMainWindow() {
        primaryStage.setTitle("Map Window");
        primaryStage.setMinWidth(MAIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MAIN_WINDOW_HEIGHT);
        primaryStage.show();

        mainPane = new StackPane();
        Scene scene = new Scene(mainPane);
        primaryStage.setScene(scene);
    }

    private void initMapView() {
        mainMapView = new MapView();
        mainMapView.setPrefSize(MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
        mainPane.getChildren().add(mainMapView);
        mainMap = new ArcGISMap(Basemap.createImagery());
        StackPane.setAlignment(mainMapView, Pos.TOP_LEFT);
        StackPane.setMargin(mainMapView, new Insets(30, 0, 0, 0));
        mainMapView.setMap(mainMap);
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
        mainPane.getChildren().add(eagleMapView);

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

    private void initBasemapSelector() {
        basemapMenuBtn = new ChoiceBox<>();
        basemapMenuBtn.setMinWidth(300);
        basemapMenuBtn.setConverter(new StringConverter<>() {
            @Override
            public String toString(Basemap.Type type) {
                return type.name();
            }

            @Override
            public Basemap.Type fromString(String s) {
                return Basemap.Type.valueOf(s);
            }
        });
        basemapMenuBtn.getItems().addAll(Basemap.Type.values());
        basemapMenuBtn.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> changeBasemapByType(basemapMenuBtn.getValue()));
        basemapMenuBtn.getSelectionModel().select(Basemap.Type.IMAGERY);
        StackPane.setAlignment(basemapMenuBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(basemapMenuBtn, new Insets(250, 15, 15, 15));
        mainPane.getChildren().add(basemapMenuBtn);
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
        loadShapefileBtn.setText("load shapefile");
        loadShapefileBtn.setDefaultButton(true);
        loadShapefileBtn.setOnMouseClicked(mouseEvent -> controller.loadShapefile(primaryStage, mainMapView));
        StackPane.setAlignment(loadShapefileBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(loadShapefileBtn, new Insets(15));
        mainPane.getChildren().add(loadShapefileBtn);
    }

    private void initGeoDatabaseButton() {
        loadGeoDatabaseBtn = new Button();
        loadGeoDatabaseBtn.setText("load geodatabse");
        loadGeoDatabaseBtn.setOnMouseClicked(mouseEvent -> controller.loadGeoDatabase(primaryStage, mainMapView));
        StackPane.setAlignment(loadGeoDatabaseBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(loadGeoDatabaseBtn, new Insets(15, 130, 15, 15));
        mainPane.getChildren().add(loadGeoDatabaseBtn);
    }

    private void initOnlineDataButton() {
        onlineDataURLText = new TextField();
        onlineDataURLText.setMaxWidth(400);
        StackPane.setAlignment(onlineDataURLText, Pos.BOTTOM_LEFT);
        StackPane.setMargin(onlineDataURLText, new Insets(15, 15, 15, 150));

        loadOnlineDataBtn = new Button();
        loadOnlineDataBtn.setText("load online data");
        loadOnlineDataBtn.setOnMouseClicked(mouseEvent -> controller.loadOnlineData(mainMapView, onlineDataURLText.getText()));
        StackPane.setAlignment(loadOnlineDataBtn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(loadOnlineDataBtn, new Insets(15));

        mainPane.getChildren().addAll(loadOnlineDataBtn, onlineDataURLText);
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
        mainPane.getChildren().addAll(callOutBtn, longitudeText, latitudeText);
    }

    private void initClickToShowCallOut () {
        mainMapView.setOnMouseClicked(mouseEvent -> {
            Point point = mainMapView.screenToLocation(new Point2D(mouseEvent.getX(), mouseEvent.getY()));
            point = (Point)GeometryEngine.project(point, SpatialReference.create(4326));
            controller.showCallOut(mainMapView, point);
        });
    }

    private void initLoadWMTSMap() {

    }

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
