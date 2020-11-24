package org.example.app;

import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.Objects;

public class DrawController extends AController {
    public ToggleGroup drawButtons;
    private AppController appController;
    private final CommonController commonController = new CommonController();

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    public void onDrawPoint(ActionEvent actionEvent) {
        System.out.println(appController.drawingType);
        if (!appController.drawingType.equals(AppController.DrawingType.MARKER)) {
            appController.clickBehaviour = AppController.ClickBehaviours.DRAWING;
            commonController.clearTemporaryGeometry(appController.mainMapView, appController.drawingCollection);
            appController.drawingType = AppController.DrawingType.MARKER;
        } else {
            appController.clickBehaviour = AppController.ClickBehaviours.NULL;
            appController.drawingType = AppController.DrawingType.NULL;
        }
    }

    public void onDrawPolyline(ActionEvent actionEvent) {
        if (!appController.drawingType.equals(AppController.DrawingType.POLYLINE)) {
            appController.clickBehaviour = AppController.ClickBehaviours.DRAWING;
            commonController.clearTemporaryGeometry(appController.mainMapView, appController.drawingCollection);
            appController.drawingType = AppController.DrawingType.POLYLINE;
        } else {
            appController.clickBehaviour = AppController.ClickBehaviours.NULL;
            appController.drawingType = AppController.DrawingType.NULL;
        }
    }

    public void onDrawPolygon(ActionEvent actionEvent) {
        if (!appController.drawingType.equals(AppController.DrawingType.POLYGON)) {
            appController.clickBehaviour = AppController.ClickBehaviours.DRAWING;
            commonController.clearTemporaryGeometry(appController.mainMapView, appController.drawingCollection);
            appController.drawingType = AppController.DrawingType.POLYGON;
        } else {
            appController.clickBehaviour = AppController.ClickBehaviours.NULL;
            appController.drawingType = AppController.DrawingType.NULL;
        }
    }

    public void onDrawClearAll(ActionEvent actionEvent) {
        commonController.clearTemporaryGeometry(appController.mainMapView, appController.drawingCollection);
    }

    public void onDrawOptions(ActionEvent actionEvent) {
        if (!appController.runtimeStages.containsKey(AppController.RuntimeStageType.DRAW_OPTIONS)) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(AppController.class.getResource("DrawOptions.fxml")));
                Pane pane = fxmlLoader.load();
                DrawOptionsController controller = fxmlLoader.getController();
                controller.setAppController(appController);
                Stage drawingOptionsStage = new Stage();
                controller.setParentStage(drawingOptionsStage);
                drawingOptionsStage.setScene(new Scene(pane));
                drawingOptionsStage.setResizable(false);
                drawingOptionsStage.setTitle("Draw Options");
                drawingOptionsStage.setOnCloseRequest(windowEvent -> appController.runtimeStages.remove(AppController.RuntimeStageType.DRAW_OPTIONS));
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
