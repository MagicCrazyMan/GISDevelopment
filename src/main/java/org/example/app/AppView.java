package org.example.app;

import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

import java.util.Objects;

public class AppView {

    private final int MAIN_WINDOW_HEIGHT = 700;
    private final int MAIN_WINDOW_WIDTH = 1200;

    Stage primaryStage;
    StackPane mainPane;
    ArcGISMap mainMap;
    ArcGISMap eagleMap;
    Button loadShapefileBtn;
    Button loadGeoDatabaseBtn;
    Button loadOnlineDataBtn;
    TextField onlineDataURLText;
    MenuButton basemapMenuBtn;

    MapView mainMapView;
    MapView eagleMapView;

    private AppController controller = new AppController();

    public void start(Stage stage) {
        drawMainWindow(stage);
    }

    private void drawMainWindow(Stage stage) {
        primaryStage = stage;
        initMainWindow();
        initMapView();
        initEagleMap();
        initShapefileButton();
        initGeoDatabaseButton();
        initOnlineDataButton();
        initBasemapSelector();
    }

    private void initMainWindow() {
        primaryStage.setTitle("Map Window");
        primaryStage.setWidth(MAIN_WINDOW_WIDTH);
        primaryStage.setHeight(MAIN_WINDOW_HEIGHT);
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
        StackPane.setMargin(eagleMapView, new Insets(15));
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
        basemapMenuBtn = new MenuButton(Basemap.Type.IMAGERY.name());
        basemapMenuBtn.setMinWidth(300);
        for (Basemap.Type type : Basemap.Type.values()) {
            MenuItem menuItem = new MenuItem(type.name());
            menuItem.setOnAction(actionEvent -> changeBasemapByType(type));
            basemapMenuBtn.getItems().add(menuItem);
        }
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
        basemapMenuBtn.setText(type.name());
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

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
