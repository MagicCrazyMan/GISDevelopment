package org.example.app;

import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.Objects;

public class DrawController {
    private AppController appController;
    private final CommonController commonController = new CommonController();

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    public void onDrawPoint(ActionEvent actionEvent) {
        appController.clickBehaviour = AppController.ClickBehaviours.DRAWING;
        if (!appController.drawingType.equals(AppController.DrawingType.MARKER)) {
            appController.drawingCollection.clear();
            appController.drawingType = AppController.DrawingType.MARKER;
        }
    }

    public void onDrawPolyline(ActionEvent actionEvent) {
        appController.clickBehaviour = AppController.ClickBehaviours.DRAWING;
        if (!appController.drawingType.equals(AppController.DrawingType.POLYLINE)) {
            appController.drawingCollection.clear();
            appController.drawingType = AppController.DrawingType.POLYLINE;
        }
    }

    public void onDrawPolygon(ActionEvent actionEvent) {
        appController.clickBehaviour = AppController.ClickBehaviours.DRAWING;
        if (!appController.drawingType.equals(AppController.DrawingType.POLYGON)) {
           appController. drawingCollection.clear();
           appController. drawingType = AppController.DrawingType.POLYGON;
        }
    }

    public void onDrawClearAll(ActionEvent actionEvent) {
        commonController.clearTemporaryGeometry(appController.mainMapView, appController.drawingCollection);
    }

    public void onDrawOptions(ActionEvent actionEvent) {
        if (!appController.runtimeStages.containsKey(AppController.RuntimeStageType.DRAW_OPTIONS)) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(AppController.class.getResource("DrawOptions.fxml")));
                TabPane pane = fxmlLoader.load();
                DrawOptionsController controller = fxmlLoader.getController();
                controller.setAppController(appController);
                Stage drawingOptionsStage = new Stage();
                drawingOptionsStage.setScene(new Scene(pane));
                drawingOptionsStage.setResizable(false);
                drawingOptionsStage.setTitle("Draw Options");
                drawingOptionsStage.show();
                appController.runtimeStages.put(AppController.RuntimeStageType.DRAW_OPTIONS, drawingOptionsStage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            appController.runtimeStages.get(AppController.RuntimeStageType.DRAW_OPTIONS).toFront();
        }
    }

}
