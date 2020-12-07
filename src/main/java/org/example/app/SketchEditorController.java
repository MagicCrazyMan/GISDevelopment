package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class SketchEditorController extends AController {

    public enum SpatialAnalysisType {
        CUT, CLIP, BUFFER, UNION, INTERSECTION, INTERSECTIONS, SIMPLIFY
    }

    public ChoiceBox<SketchCreationMode> drawTypeChoiceBox;
    public ToggleButton editButton;
    public Button undoButton;
    public Button redoButton;
    public Button finishButton;
    public Button cancelButton;
    public Button clearButton;
    public RadioButton startDrawButton;
    public MenuButton spatialAnalysisMenuButton;

    private MapView parentMapView;
    private AppController appController;
    private CommonController commonController = new CommonController();
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
            if (stopDraw()) {
                appController.runtimeStages.remove(AppController.RuntimeStageType.SKETCH_EDITOR);
            } else {
                windowEvent.consume();
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
        sketchEditor = new SketchEditor();
        sketchEditor.getSketchEditConfiguration().setVertexEditMode(SketchEditConfiguration.SketchVertexEditMode.INTERACTION_EDIT);
        sketchEditor.getSketchEditConfiguration().setRequireSelectionBeforeDrag(true);
        sketchEditor.getSketchEditConfiguration().setAllowPartSelection(true);
        sketchEditor.getSketchEditConfiguration().setContextMenuEnabled(true);
        sketchEditor.addGeometryChangedListener(sketchGeometryChangedEvent -> {
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
        drawTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
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
            sketchEditor.start(newValue);
        });
        editButton.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue) {
                if (Objects.nonNull(sketchEditor.getGeometry()) && !sketchEditor.getGeometry().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("CONFIRMATION");
                    alert.setContentText("Existing non finished sketch edit, do you want to continue?");
                    Optional<ButtonType> button = alert.showAndWait();
                    if (button.isEmpty() || button.get().equals(ButtonType.CANCEL)) {
                        editButton.setSelected(false);
                        return;
                    }
                }

                if (Objects.isNull(editEventHandler)) {
                    editEventHandler = mouseEvent -> {
                        if (Objects.nonNull(sketchEditor.getGeometry()) && !sketchEditor.getGeometry().isEmpty()) {
                            return;
                        }
                        ListenableFuture<IdentifyGraphicsOverlayResult> resultListenableFuture = parentMapView.identifyGraphicsOverlayAsync(sketchEditGraphicOverlay, new Point2D(mouseEvent.getX(), mouseEvent.getY()), 5, false);
                        resultListenableFuture.addDoneListener(() -> {
                            try {
                                if (!resultListenableFuture.get().getGraphics().isEmpty()) {
                                    if (Objects.isNull(sketchEditor.getGeometry()) || !resultListenableFuture.get().getGraphics().get(0).getGeometry().equals(sketchEditor.getGeometry())) {
                                        Graphic graphic = resultListenableFuture.get().getGraphics().get(0);
                                        sketchEditor.start(graphic.getGeometry());
                                        sketchEditGraphicOverlay.getGraphics().remove(graphic);
                                    }
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });
                    };
                }
                sketchEditor.stop();
                drawTypeChoiceBox.setDisable(true);
                parentMapView.addEventHandler(MouseEvent.MOUSE_CLICKED, editEventHandler);
            } else {
                sketchEditor.start(drawTypeChoiceBox.getValue());
                drawTypeChoiceBox.setDisable(false);
                parentMapView.removeEventHandler(MouseEvent.MOUSE_CLICKED, editEventHandler);
            }
        });

        for (SpatialAnalysisType spatialAnalysisType : SpatialAnalysisType.values()) {
            MenuItem menuItem = new MenuItem(spatialAnalysisType.name());
            switch (spatialAnalysisType) {
                case CUT:
                    menuItem.setOnAction(event -> {
                        if (sketchEditGraphicOverlay.getGraphics().size() >= 2) {
                            Graphic graphic0 = sketchEditGraphicOverlay.getGraphics().get(0);
                            Graphic graphic1 = sketchEditGraphicOverlay.getGraphics().get(1);

                            Geometry geometry0 = graphic0.getGeometry();
                            Geometry geometry1 = graphic1.getGeometry();

                            Geometry geometry;
                            Polyline cutter;
                            if (!geometry0.getGeometryType().equals(GeometryType.POLYLINE) && !geometry1.getGeometryType().equals(GeometryType.POLYLINE)) {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setContentText("At least one geometry is polyline");
                                alert.showAndWait();
                                return;
                            } else {
                                if (geometry0.getGeometryType().equals(GeometryType.POLYLINE)) {
                                    cutter = (Polyline) geometry0;
                                    geometry = geometry1;
                                } else {
                                    cutter = (Polyline) geometry1;
                                    geometry = geometry0;
                                }
                            }

                            List<Geometry> geometryList = GeometryEngine.cut(geometry, cutter);
                            sketchEditGraphicOverlay.getGraphics().clear();
                            geometryList.forEach(newGeometry -> {
                                Graphic graphic = new Graphic(newGeometry);
                                switch (graphic.getGeometry().getGeometryType()) {
                                    case POINT:
                                    case MULTIPOINT:
                                        graphic.setSymbol(sketchEditor.getSketchStyle().getVertexSymbol());
                                        break;
                                    case ENVELOPE:
                                    case POLYGON:
                                        graphic.setSymbol(sketchEditor.getSketchStyle().getFillSymbol());
                                        break;
                                    case POLYLINE:
                                        graphic.setSymbol(sketchEditor.getSketchStyle().getLineSymbol());
                                        break;
                                    case UNKNOWN:
                                        break;
                                }
                                sketchEditGraphicOverlay.getGraphics().add(graphic);
                            });
                        }
                    });
                    break;
                case CLIP:
                    menuItem.setOnAction(event -> {
                        if (sketchEditGraphicOverlay.getGraphics().size() >= 2) {
                            Graphic graphic0 = sketchEditGraphicOverlay.getGraphics().get(0);
                            Graphic graphic1 = sketchEditGraphicOverlay.getGraphics().get(1);

                            Geometry geometry0 = graphic0.getGeometry();
                            Geometry geometry1 = graphic1.getGeometry();

                            Geometry newGeometry = GeometryEngine.clip(geometry0, geometry1.getExtent());
                            Graphic graphic = new Graphic(newGeometry);
                            switch (graphic.getGeometry().getGeometryType()) {
                                case POINT:
                                case MULTIPOINT:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getVertexSymbol());
                                    break;
                                case ENVELOPE:
                                case POLYGON:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getFillSymbol());
                                    break;
                                case POLYLINE:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getLineSymbol());
                                    break;
                                case UNKNOWN:
                                    break;
                            }
                            sketchEditGraphicOverlay.getGraphics().clear();
                            sketchEditGraphicOverlay.getGraphics().add(graphic);
                        }
                    });
                    break;
                case BUFFER:
                    menuItem.setOnAction(event -> {
                        if (sketchEditGraphicOverlay.getGraphics().size() >= 1) {
                            final SimpleFillSymbol fillSymbol = new SimpleFillSymbol();
                            fillSymbol.setStyle(SimpleFillSymbol.Style.SOLID);
                            fillSymbol.setColor(commonController.color2int(Color.rgb(255,255,255,0.6)));
                            fillSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, commonController.color2int(Color.BLACK), 2));

                            for (Graphic graphic : sketchEditGraphicOverlay.getGraphics()) {
                                Geometry geometry = graphic.getGeometry();

                                System.out.println(parentMapView.getMapScale());
                                Polygon newGeometry = GeometryEngine.buffer(geometry, 500000.0);
                                graphic.setGeometry(newGeometry);
                                graphic.setSymbol(fillSymbol);
                            }
                        }
                    });
                    break;
                case UNION:
                    menuItem.setOnAction(event -> {
                        if (sketchEditGraphicOverlay.getGraphics().size() >= 2) {
                            List<Geometry> geometryList = new ArrayList<>(sketchEditGraphicOverlay.getGraphics().size());
                            sketchEditGraphicOverlay.getGraphics().forEach(graphic -> geometryList.add(graphic.getGeometry()));

                            Geometry newGeometry = GeometryEngine.union(geometryList);
                            Graphic graphic = new Graphic(newGeometry);
                            switch (graphic.getGeometry().getGeometryType()) {
                                case POINT:
                                case MULTIPOINT:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getVertexSymbol());
                                    break;
                                case ENVELOPE:
                                case POLYGON:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getFillSymbol());
                                    break;
                                case POLYLINE:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getLineSymbol());
                                    break;
                                case UNKNOWN:
                                    break;
                            }
                            sketchEditGraphicOverlay.getGraphics().clear();
                            sketchEditGraphicOverlay.getGraphics().add(graphic);
                        }
                    });
                    break;
                case INTERSECTION:
                    menuItem.setOnAction(event -> {
                        if (sketchEditGraphicOverlay.getGraphics().size() >= 2) {
                            Iterator<Graphic> iterator = sketchEditGraphicOverlay.getGraphics().iterator();
                            Geometry lastGeometry = iterator.next().getGeometry();
                            do {
                                Geometry currentGeometry = iterator.next().getGeometry();
                                lastGeometry = GeometryEngine.intersection(lastGeometry, currentGeometry);
                            } while (iterator.hasNext());

                            sketchEditGraphicOverlay.getGraphics().clear();
                            Graphic graphic = new Graphic(lastGeometry);
                            switch (graphic.getGeometry().getGeometryType()) {
                                case POINT:
                                case MULTIPOINT:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getVertexSymbol());
                                    break;
                                case ENVELOPE:
                                case POLYGON:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getFillSymbol());
                                    break;
                                case POLYLINE:
                                    graphic.setSymbol(sketchEditor.getSketchStyle().getLineSymbol());
                                    break;
                                case UNKNOWN:
                                    break;
                            }
                            sketchEditGraphicOverlay.getGraphics().add(graphic);
                        }
                    });
                    break;
                case INTERSECTIONS:
                    menuItem.setOnAction(event -> {
                        if (sketchEditGraphicOverlay.getGraphics().size() >= 2) {
                            Graphic graphic0 = sketchEditGraphicOverlay.getGraphics().get(0);
                            Graphic graphic1 = sketchEditGraphicOverlay.getGraphics().get(1);

                            Geometry geometry0 = graphic0.getGeometry();
                            Geometry geometry1 = graphic1.getGeometry();

                            List<Geometry> geometries = GeometryEngine.intersections(geometry0, geometry1);
                            sketchEditGraphicOverlay.getGraphics().clear();
                            geometries.forEach(newGeometry -> {
                                Graphic graphic = new Graphic(newGeometry);
                                switch (graphic.getGeometry().getGeometryType()) {
                                    case POINT:
                                    case MULTIPOINT:
                                        graphic.setSymbol(sketchEditor.getSketchStyle().getVertexSymbol());
                                        break;
                                    case ENVELOPE:
                                    case POLYGON:
                                        graphic.setSymbol(sketchEditor.getSketchStyle().getFillSymbol());
                                        break;
                                    case POLYLINE:
                                        graphic.setSymbol(sketchEditor.getSketchStyle().getLineSymbol());
                                        break;
                                    case UNKNOWN:
                                        break;
                                }
                                sketchEditGraphicOverlay.getGraphics().add(graphic);
                            });
                        }
                    });
                    break;
                case SIMPLIFY:
                    menuItem.setOnAction(event -> {
                        for (Graphic graphic : sketchEditGraphicOverlay.getGraphics()) {
                            Geometry geometry = GeometryEngine.simplify(graphic.getGeometry());
                            graphic.setGeometry(geometry);
                        }
                    });
                    break;
                default:
                    break;
            }
            spatialAnalysisMenuButton.getItems().addAll(menuItem);
        }
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
        if (Objects.nonNull(sketchEditor.getGeometry()) && !sketchEditor.getGeometry().isEmpty()) {
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
            }
            sketchEditGraphicOverlay.getGraphics().add(graphic);
            sketchEditor.clearGeometry();
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
