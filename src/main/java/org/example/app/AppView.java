package org.example.app;

import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
        SimpleFillSymbol extentPolygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0x99FF0000, extentPolygonOutline);
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

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
