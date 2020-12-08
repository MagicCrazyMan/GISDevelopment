package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.view.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.util.concurrent.ExecutionException;

public class QueryByLocationController extends AController {
    public enum SelectMethod {
        POINT, ENVELOP
    }

    public ListView<CheckBox> fromFeatureLayersListView;
    public ChoiceBox<SelectMethod> selectMethodsChoiceBox;
    private AppController appController;
    private CommonController commonController = new CommonController();
    private SketchEditor sketchEditor = new SketchEditor();
    final EventHandler<MouseEvent> handler = mouseEvent -> {
        if (mouseEvent.getClickCount() >= 2) {
            Geometry geometry = sketchEditor.getGeometry();
            if (!geometry.isEmpty()) {
                QueryParameters queryParameters = new QueryParameters();
                queryParameters.setGeometry(geometry);
                fromFeatureLayersListView.getItems().forEach(checkBox -> {
                    if (checkBox.isSelected()) {
                        FeatureLayer featureLayer = (FeatureLayer) checkBox.getUserData();
                        ListenableFuture<FeatureQueryResult> future = featureLayer.selectFeaturesAsync(queryParameters, FeatureLayer.SelectionMode.NEW);
                        future.addDoneListener(() -> {
                            try {
                                commonController.showQueryResult(future.get());
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                });
            }
        }
    };

    public AppController getAppController() {
        return appController;
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
        for (Layer layer : this.appController.mainMap.getOperationalLayers()) {
            if (layer instanceof FeatureLayer) {
                CheckBox checkBox = new CheckBox();
                checkBox.setText(layer.getName());
                checkBox.setUserData(layer);
                fromFeatureLayersListView.getItems().add(checkBox);
            }
        }
        this.appController.mainMapView.setSketchEditor(sketchEditor);
    }

    @Override
    public void setParentStage(Stage stage) {
        super.setParentStage(stage);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowEvent -> sketchEditor.stop());
    }

    @FXML
    public void initialize() {
        sketchEditor.getSketchEditConfiguration().setVertexEditMode(SketchEditConfiguration.SketchVertexEditMode.INTERACTION_EDIT);
        sketchEditor.getSketchEditConfiguration().setRequireSelectionBeforeDrag(true);
        sketchEditor.getSketchEditConfiguration().setAllowPartSelection(true);
        sketchEditor.getSketchEditConfiguration().setContextMenuEnabled(true);

        selectMethodsChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SelectMethod selectMethod) {
                return selectMethod.name();
            }

            @Override
            public SelectMethod fromString(String s) {
                return SelectMethod.valueOf(s);
            }
        });
        selectMethodsChoiceBox.getItems().addAll(SelectMethod.values());
        selectMethodsChoiceBox.getSelectionModel().selectFirst();

        fromFeatureLayersListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }


    public void onApply(ActionEvent actionEvent) {
        if (((ToggleButton) actionEvent.getSource()).isSelected()) {
            appController.clickBehaviour = AppController.ClickBehaviours.QUERY_BY_LOCATION;
            selectMethodsChoiceBox.setDisable(true);
            switch (selectMethodsChoiceBox.getSelectionModel().getSelectedItem()) {
                case POINT:
                    sketchEditor.start(SketchCreationMode.POINT);
                    break;
                case ENVELOP:
                    sketchEditor.start(SketchCreationMode.POLYGON);
                    break;
                default:
                    return;
            }
            appController.mainMapView.addEventHandler(MouseEvent.MOUSE_CLICKED, handler);
        } else {
            appController.clickBehaviour = AppController.ClickBehaviours.SELECTED_FEATURE;
            selectMethodsChoiceBox.setDisable(false);
            sketchEditor.stop();
            appController.mainMapView.removeEventHandler(MouseEvent.MOUSE_CLICKED, handler);
        }
    }

    public void onCancel(ActionEvent actionEvent) {
        WindowEvent windowEvent = new WindowEvent(parentStage, WindowEvent.WINDOW_CLOSE_REQUEST);
        WindowEvent.fireEvent(parentStage, windowEvent);
        if (!windowEvent.isConsumed()) {
            parentStage.close();
        }
    }
}
