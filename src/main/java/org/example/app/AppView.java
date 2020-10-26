package org.example.app;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
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
        eagleMapView.setMap(eagleMap);
        StackPane.setMargin(eagleMapView, new Insets(15));
        StackPane.setAlignment(eagleMapView, Pos.BOTTOM_LEFT);
        mainPane.getChildren().add(eagleMapView);
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

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
    }
}
