package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.util.ListChangedEvent;
import com.esri.arcgisruntime.util.ListChangedListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class SketchEditorController extends AController {
    public ChoiceBox<SketchCreationMode> drawTypeChoiceBox;
    public ToggleButton editButton;
    public Button undoButton;
    public Button redoButton;
    public Button finishButton;
    public Button cancelButton;
    public Button clearButton;
    public RadioButton startDrawButton;

    private MapView parentMapView;
    private AppController appController;
    private SketchEditor sketchEditor;
    private GraphicsOverlay sketchEditGraphicOverlay = new GraphicsOverlay();
    private EventHandler<MouseEvent> editEventHandler;

    public AppController getAppController() {
        return appController;
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    public MapView getParentMapView() {
        return parentMapView;
    }

    @Override
    public void setParentStage(Stage stage) {
        super.setParentStage(stage);
        stage.setOnCloseRequest(windowEvent -> {
            if (!sketchEditor.getGeometry().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("CONFIRMATION");
                alert.setContentText("Existing non finished sketch edit, do you want to continue?");
                Optional<ButtonType> button = alert.showAndWait();
                if (button.isPresent() && button.get().equals(ButtonType.OK)) {
                    windowEvent.consume();
                }
            }
        });
    }

    public void setParentMapView(MapView parentMapView) {
        this.parentMapView = parentMapView;
        this.parentMapView.setSketchEditor(sketchEditor);
        this.parentMapView.getGraphicsOverlays().add(sketchEditGraphicOverlay);
        sketchEditGraphicOverlay.getGraphics().addListChangedListener(graphicListChangedEvent -> editButton.setDisable(graphicListChangedEvent.getItems().isEmpty()));
    }

    @FXML
    public void initialize() {
        this.sketchEditor = new SketchEditor();
        this.sketchEditor.getSketchEditConfiguration().setVertexEditMode(SketchEditConfiguration.SketchVertexEditMode.INTERACTION_EDIT);
        this.sketchEditor.getSketchEditConfiguration().setRequireSelectionBeforeDrag(true);
        this.sketchEditor.getSketchEditConfiguration().setAllowPartSelection(true);
        this.sketchEditor.getSketchEditConfiguration().setContextMenuEnabled(true);
        this.sketchEditor.addGeometryChangedListener(sketchGeometryChangedEvent -> {
            SketchEditor sketchEditor = sketchGeometryChangedEvent.getSource();
            undoButton.setDisable(!sketchEditor.canUndo());
            redoButton.setDisable(!sketchEditor.canRedo());
            finishButton.setDisable(sketchEditor.getGeometry().isEmpty());
            clearButton.setDisable(sketchEditor.getGeometry().isEmpty());
        });

        drawTypeChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SketchCreationMode sketchCreationMode) {
                return sketchCreationMode.name();
            }

            @Override
            public SketchCreationMode fromString(String s) {
                return SketchCreationMode.valueOf(s);
            }
        });
        drawTypeChoiceBox.getItems().addAll(SketchCreationMode.values());
        drawTypeChoiceBox.getSelectionModel().select(0);
        drawTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((observableValue, sketchCreationMode, t1) -> {
            if (!sketchEditor.getGeometry().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("CONFIRMATION");
                alert.setContentText("Existing non finished sketch edit, do you want to continue?");
                Optional<ButtonType> button = alert.showAndWait();
                if (button.isEmpty() || button.get().equals(ButtonType.CANCEL)) {
                    return;
                }
            }

            sketchEditor.clearGeometry();
            sketchEditor.start(t1);
        });
        editButton.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (t1) {
                if (Objects.isNull(editEventHandler)) {
                    editEventHandler = mouseEvent -> {
                        ListenableFuture<IdentifyGraphicsOverlayResult> resultListenableFuture = parentMapView.identifyGraphicsOverlayAsync(sketchEditGraphicOverlay, new Point2D(mouseEvent.getX(), mouseEvent.getY()), 5, false);
                        resultListenableFuture.addDoneListener(() -> {
                            try {
                                if (!resultListenableFuture.get().getGraphics().isEmpty()) {
                                    if (Objects.isNull(sketchEditor.getGeometry()) || !resultListenableFuture.get().getGraphics().get(0).getGeometry().equals(sketchEditor.getGeometry())) {
                                        sketchEditor.start(resultListenableFuture.get().getGraphics().get(0).getGeometry());
                                    }
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });
                    };
                }
                parentMapView.addEventHandler(MouseEvent.MOUSE_CLICKED, editEventHandler);
            } else {
                parentMapView.removeEventHandler(MouseEvent.MOUSE_CLICKED, editEventHandler);
            }
        });
    }

    public void onStartDraw(ActionEvent actionEvent) {
        if (startDrawButton.isSelected()) {
            startDraw();
        } else {
            startDrawButton.setSelected(!stopDraw());
        }
    }

    private void startDraw() {
        SketchCreationMode sketchCreationMode = drawTypeChoiceBox.getValue();
        sketchEditor.start(sketchCreationMode);
        appController.clickBehaviour = AppController.ClickBehaviours.SKETCH_EDITOR;

        drawTypeChoiceBox.setDisable(false);
        editButton.setSelected(false);
        cancelButton.setDisable(false);
        startDrawButton.setSelected(true);
    }

    private boolean stopDraw() {
        if (!sketchEditor.getGeometry().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("CONFIRMATION");
            alert.setContentText("Existing non finished sketch edit, do you want to continue?");
            Optional<ButtonType> button = alert.showAndWait();
            if (button.isEmpty() || button.get().equals(ButtonType.CANCEL)) {
                return false;
            }
        }

        sketchEditor.stop();
        appController.clickBehaviour = AppController.ClickBehaviours.SELECTED_FEATURE;

        drawTypeChoiceBox.setDisable(true);
        editButton.setDisable(true);
        editButton.setSelected(false);
        undoButton.setDisable(true);
        redoButton.setDisable(true);
        finishButton.setDisable(true);
        cancelButton.setDisable(true);
        clearButton.setDisable(true);
        startDrawButton.setSelected(false);

        return true;
    }

    public void onEdit(ActionEvent actionEvent) {
        if (editButton.isSelected()) {
            if (!sketchEditor.getGeometry().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("CONFIRMATION");
                alert.setContentText("Existing non finished sketch edit, do you want to continue?");
                Optional<ButtonType> button = alert.showAndWait();
                if (button.isEmpty() || button.get().equals(ButtonType.CANCEL)) {
                    editButton.setSelected(false);
                    return;
                }
            }
            sketchEditor.stop();
        } else {
            sketchEditor.start(drawTypeChoiceBox.getValue());
        }
    }

    public void onUndo(ActionEvent actionEvent) {
        sketchEditor.undo();
    }

    public void onRedo(ActionEvent actionEvent) {
        sketchEditor.redo();
    }

    public void onFinish(ActionEvent actionEvent) {
        if (!sketchEditor.getGeometry().isEmpty()) {
            Graphic graphic = new Graphic(sketchEditor.getGeometry());
            switch (sketchEditor.getGeometry().getGeometryType()) {
                case POINT:
                case MULTIPOINT:
                    graphic.setSymbol(sketchEditor.getSketchStyle().getVertexSymbol());
                    break;
                case POLYGON:
                case ENVELOPE:
                    graphic.setSymbol(sketchEditor.getSketchStyle().getFillSymbol());
                    break;
                case POLYLINE:
                    graphic.setSymbol(sketchEditor.getSketchStyle().getLineSymbol());
                    break;
            }

            if (editButton.isSelected()) {
                sketchEditor.stop();
                sketchEditor.clearGeometry();
            } else {
                sketchEditGraphicOverlay.getGraphics().add(graphic);
                sketchEditor.clearGeometry();
            }
        }
    }

    public void onCancel(ActionEvent actionEvent) {
        stopDraw();
        onClear(actionEvent);
    }

    public void onClear(ActionEvent actionEvent) {
        sketchEditor.clearGeometry();
    }
}
