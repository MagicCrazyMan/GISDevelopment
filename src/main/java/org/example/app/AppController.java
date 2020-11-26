package org.example.app;

import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AppController extends AController {
    public StackPane eagleMapPane;
    public StackPane mainMapPane;
    public VBox layerPane;
    public ScrollPane layerScrollPane;
    public Button loadShapefileBtn;
    public Button loadGeoDatabaseBtn;
    public Button loadOnlineDataBtn;
    public Button loadRasterBtn;
    public Button callOutBtn;
    public TextField onlineDataURLText;
    public ChoiceBox<OnlineDataType> onlineDataTypeChoiceBox;
    public ChoiceBox<Basemap.Type> basemapChoiceBox;
    public TextField longitudeText;
    public TextField latitudeText;

    public enum OnlineDataType {
        ESRI_SERVICE,
        WMS_SERVICE
    }

    public enum RuntimeStageType {
        SIMPLE_QUERY,
        CLICK_QUERY,
        DRAW,
        DRAW_OPTIONS
    }

    public enum ClickBehaviours {
        NULL,
        SELECTED_FEATURE,
        SELECTED_FEATURE_AND_QUERY,
        IDENTITY_QUERY,
        DRAWING,
    }

    public enum DrawingType {
        MARKER, POLYLINE, POLYGON, NULL
    }

    public static class SimpleSymbolContainer {

        final SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol();
        final SimpleLineSymbol lineSymbol = new SimpleLineSymbol();
        final SimpleFillSymbol fillSymbol = new SimpleFillSymbol();

        public SimpleSymbolContainer() {
            // default style
            SimpleLineSymbol markerOutline = new SimpleLineSymbol();
            markerOutline.setStyle(SimpleLineSymbol.Style.SOLID);
            markerOutline.setWidth(2);
            markerOutline.setColor(0xFF00FFFF);
            markerSymbol.setStyle(SimpleMarkerSymbol.Style.CIRCLE);
            markerSymbol.setOutline(markerOutline);
            markerSymbol.setSize(5);
            markerSymbol.setColor(0xFF0000FF);

            lineSymbol.setStyle(SimpleLineSymbol.Style.SOLID);
            lineSymbol.setMarkerStyle(SimpleLineSymbol.MarkerStyle.NONE);
            lineSymbol.setMarkerPlacement(SimpleLineSymbol.MarkerPlacement.BEGIN);
            lineSymbol.setWidth(2);
            lineSymbol.setColor(0xFF0000FF);

            SimpleLineSymbol polygonOutlineSymbol = new SimpleLineSymbol();
            polygonOutlineSymbol.setStyle(SimpleLineSymbol.Style.SOLID);
            polygonOutlineSymbol.setMarkerStyle(SimpleLineSymbol.MarkerStyle.NONE);
            polygonOutlineSymbol.setMarkerPlacement(SimpleLineSymbol.MarkerPlacement.BEGIN);
            polygonOutlineSymbol.setWidth(2);
            polygonOutlineSymbol.setColor(0xFF0000FF);
            fillSymbol.setStyle(SimpleFillSymbol.Style.SOLID);
            fillSymbol.setColor(0xFFFFFFFF);
            fillSymbol.setOutline(polygonOutlineSymbol);
        }
    }

    ArcGISMap mainMap;
    ArcGISMap eagleMap;
    MapView mainMapView;
    MapView eagleMapView;
    private final CommonController commonController = new CommonController();
    Map<RuntimeStageType, Stage> runtimeStages = new HashMap<>();

    ClickBehaviours clickBehaviour = ClickBehaviours.SELECTED_FEATURE;
    SimpleSymbolContainer simpleSymbolContainer;
    PointCollection drawingCollection;
    DrawingType drawingType = DrawingType.NULL;

    @FXML
    public void initialize() {
        initMainMap();
        initEagleMap();
        initLayerPane();
        initBasemapSelector();
        initOnlineDataTypeSelector();
        initMapViewClicker();
    }

    private void initLayerPane() {
        layerScrollPane.setOnDragOver(dragEvent -> {
            Dragboard db = dragEvent.getDragboard();
            if (db.hasFiles()) {
                dragEvent.acceptTransferModes(TransferMode.COPY);
            } else {
                dragEvent.consume();
            }
        });
        layerScrollPane.setOnDragDropped(dragEvent -> {
            Dragboard db = dragEvent.getDragboard();
            if (db.hasFiles() && db.getFiles().size() > 0) {
                File file = db.getFiles().get(0);
                commonController.routeLoadFile(mainMapView, file);
            }
        });
    }

    private void initMainMap() {
        mainMapView = new MapView();
        mainMapView.setViewOrder(-1);
        mainMap = new ArcGISMap(Basemap.createImagery());
        mainMap.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            Timer timer;

            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
                if (loadStatusChangedEvent.getNewLoadStatus().equals(LoadStatus.LOADING)) {
                    timer = new Timer();
                    // 10s内不能加载在线底图，就使用空白底图
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mainMap.setBasemap(new Basemap());
                            drawingCollection = new PointCollection(SpatialReference.create(3857));
                            basemapChoiceBox.setDisable(true);
                        }
                    }, 10 * 1000);
                } else if (loadStatusChangedEvent.getNewLoadStatus().equals(LoadStatus.LOADED)) {
                    timer.cancel();
                    drawingCollection = new PointCollection(mainMap.getSpatialReference());
                } else {
                    timer.cancel();
                }
            }
        });

        // add listener, any layer changed will notify layer manager
        mainMap.getOperationalLayers().addListChangedListener(listChangedEvent -> {
            if (Objects.nonNull(layerPane)) {
                switch (listChangedEvent.getAction()) {
                    case ADDED: {
                        Layer layer = listChangedEvent.getItems().get(0);
                        layer.addDoneLoadingListener(() -> {
                            if (layer.getLoadStatus().equals(LoadStatus.LOADED)) {
                                // init a pane as layer graphic
                                String name = layer.getName();
                                String id = layer.getId();
                                RadioButton radioButton = new RadioButton(name);
                                radioButton.setId(id);
                                radioButton.setSelected(true);
                                radioButton.selectedProperty().addListener((observableValue, aBoolean, t1) -> layer.setVisible(t1));
                                radioButton.setOnContextMenuRequested(contextMenuEvent -> {
                                    if (radioButton.contextMenuProperty().isNull().get()) {
                                        ContextMenu contextMenu = new ContextMenu();
                                        MenuItem zoomTo = new MenuItem("Zoom To");
                                        MenuItem remove = new MenuItem("Remove");
                                        zoomTo.setOnAction(actionEvent -> mainMapView.setViewpointGeometryAsync(layer.getFullExtent(), 50));
                                        if (layer instanceof FeatureLayer) {
                                            MenuItem renderer = new MenuItem("Renderer");
                                            renderer.setOnAction(new EventHandler<>() {
                                                Stage stage;
                                                @Override
                                                public void handle(ActionEvent actionEvent) {
                                                    try {
                                                        if (Objects.isNull(stage)) {
                                                            FXMLLoader fxmlLoader = new FXMLLoader(AppController.class.getResource("Renderer.fxml"));
                                                            Pane pane = fxmlLoader.load();
                                                            RendererController controller = fxmlLoader.getController();
                                                            stage = new Stage();
                                                            controller.setFeatureLayer((FeatureLayer) layer);
                                                            controller.setParentStage(stage);
                                                            controller.setParentMapView(mainMapView);
                                                            stage.setTitle("Renderer");
                                                            stage.setScene(new Scene(pane));
                                                            stage.setResizable(false);
                                                            stage.setOnCloseRequest(windowEvent -> stage = null);
                                                            stage.showAndWait();
                                                        } else {
                                                            stage.toFront();
                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                            contextMenu.getItems().add(renderer);
                                        }
                                        remove.setOnAction(actionEvent -> mainMap.getOperationalLayers().remove(layer));

                                        contextMenu.getItems().addAll(zoomTo, remove);
                                        radioButton.contextMenuProperty().set(contextMenu);
                                    }
                                });
                                layerPane.getChildren().add(radioButton);
                            }
                        });
                        break;
                    }
                    case REMOVED: {
                        Layer layer = listChangedEvent.getItems().get(0);
                        layerPane.getChildren().removeIf(node -> node.getId().equals(layer.getId()));
                        break;
                    }
                }
            }
        });

        mainMapView.setMap(mainMap);
        StackPane.setAlignment(mainMapView, Pos.TOP_LEFT);
        mainMapPane.getChildren().add(mainMapView);
    }

    private void initEagleMap() {
        eagleMapView = new MapView();
        eagleMap = new ArcGISMap(Basemap.createStreets());
        eagleMapView.setEnableMousePan(false);
        eagleMapView.setEnableMouseZoom(false);
        eagleMapView.setEnableTouchPan(false);
        eagleMapView.setEnableTouchRotate(false);
        eagleMapView.setEnableTouchZoom(false);
        eagleMapView.setMap(eagleMap);
        StackPane.setAlignment(eagleMapView, Pos.TOP_RIGHT);
        eagleMapPane.getChildren().add(eagleMapView);

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

    private void initOnlineDataTypeSelector() {
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
    }


    boolean isDragging = false;

    private void initMapViewClicker() {
        mainMapView.setOnDragDetected(mouseEvent -> isDragging = true);
        mainMapView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 1) {
                if (!isDragging) {
                    Point2D screenPoint = new Point2D(mouseEvent.getX(), mouseEvent.getY());
                    Point mapPoint = mainMapView.screenToLocation(screenPoint);
                    if (Objects.nonNull(mapPoint)) {
                        switch (clickBehaviour) {
                            case IDENTITY_QUERY: {
                                if (mainMap.getOperationalLayers().size() > 0) {
                                    commonController.clickQuery(mainMapView, (FeatureLayer) mainMap.getOperationalLayers().get(0), screenPoint);
                                }
                                break;
                            }
                            case SELECTED_FEATURE: {
                                if (mainMap.getOperationalLayers().size() > 0) {
                                    commonController.clickQuery(mainMapView, (FeatureLayer) mainMap.getOperationalLayers().get(0), mapPoint,false);
                                }
                                break;
                            }
                            case SELECTED_FEATURE_AND_QUERY:{
                                if (mainMap.getOperationalLayers().size() > 0) {
                                    commonController.clickQuery(mainMapView, (FeatureLayer) mainMap.getOperationalLayers().get(0), mapPoint,true);
                                }
                                break;
                            }
                            case DRAWING: {
                                drawingCollection.add(mapPoint);
                                switch (drawingType) {
                                    case POLYGON:
                                        commonController.drawTemporaryGeometry(mainMapView, DrawingType.MARKER, simpleSymbolContainer.markerSymbol, drawingCollection, true);
                                        commonController.drawTemporaryGeometry(mainMapView, DrawingType.POLYGON, simpleSymbolContainer.fillSymbol, drawingCollection, false);
                                        break;
                                    case POLYLINE:
                                        commonController.drawTemporaryGeometry(mainMapView, DrawingType.MARKER, simpleSymbolContainer.markerSymbol, drawingCollection, true);
                                        commonController.drawTemporaryGeometry(mainMapView, DrawingType.POLYLINE, simpleSymbolContainer.lineSymbol, drawingCollection, false);
                                        break;
                                    case MARKER:
                                        commonController.drawTemporaryGeometry(mainMapView, DrawingType.MARKER, simpleSymbolContainer.markerSymbol, drawingCollection, true);
                                        break;
                                }
                                break;
                            }
                        }
                        // controller.showCallOut(mainMapView, mapPoint);
                    }
                } else {
                    isDragging = false;
                }
            } else {
                if (clickBehaviour == ClickBehaviours.DRAWING) {
                    switch (drawingType) {
                        case POLYGON:
                        case POLYLINE:
                        case MARKER:
                            break;
                    }
                }
            }
        });
    }

    public void dispose() {
        if (Objects.nonNull(mainMapView)) {
            mainMapView.dispose();
        }
        for (Stage stage : runtimeStages.values()) {
            stage.close();
        }
    }

    public void onExit(ActionEvent actionEvent) {
        if (Objects.nonNull(parentStage.getOnCloseRequest())) {
            parentStage.getOnCloseRequest().handle(new WindowEvent(parentStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
        parentStage.onHiddenProperty().addListener((observableValue, windowEventEventHandler, t1) -> System.out.println(222));
        parentStage.close();
    }

    public void onZoomIn(ActionEvent actionEvent) {
        mainMapView.setViewpointScaleAsync(mainMapView.getMapScale() / 4);
    }

    public void onZoomOut(ActionEvent actionEvent) {
        mainMapView.setViewpointScaleAsync(mainMapView.getMapScale() * 4);
    }

    public void onFullExtent(ActionEvent actionEvent) {
        // this is a tricky way to achieve zooming to full extent, all we have to do is that, set scale to a large enough value (but not too large)
        // MapView will automatically limit scale to the max scale value
        mainMapView.setViewpointScaleAsync(1E20);

    }

    public void onClockwiseRotate(ActionEvent actionEvent) {
        mainMapView.setViewpointRotationAsync(mainMapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getRotation() + Math.PI / 2);
    }

    public void onCounterClockwiseRotate(ActionEvent actionEvent) {
        mainMapView.setViewpointRotationAsync(mainMapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getRotation() - Math.PI / 2);
    }

    public void onSimpleQuery(ActionEvent actionEvent) {
        if (!runtimeStages.containsKey(RuntimeStageType.SIMPLE_QUERY)) {
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
                        commonController.simpleQuery(mainMapView, (FeatureLayer) layers.get(0), queryParameters);
                    }
                }
            });
            nameText.setMinWidth(100);
            valueText.setMinWidth(100);
            toolBar.getItems().addAll(nameLabel, nameText, valueLabel, valueText, queryBtn);
            simpleQueryPane.getChildren().add(toolBar);

            Stage simpleQueryStage = new Stage();
            simpleQueryStage.setTitle("Simple Query");
            simpleQueryStage.setResizable(false);
            simpleQueryStage.setScene(new Scene(simpleQueryPane));
            simpleQueryStage.setOnCloseRequest(windowEvent -> runtimeStages.remove(RuntimeStageType.SIMPLE_QUERY));
            simpleQueryStage.show();
            runtimeStages.put(RuntimeStageType.SIMPLE_QUERY, simpleQueryStage);
        } else {
            runtimeStages.get(RuntimeStageType.SIMPLE_QUERY).toFront();
        }
    }

    public void onClickQuery(ActionEvent actionEvent) {
        if (!runtimeStages.containsKey(RuntimeStageType.CLICK_QUERY)) {
            StackPane stackPane = new StackPane();
            ToolBar toolBar = new ToolBar();
            RadioButton selectFeatureBtn = new RadioButton("Select Feature");
            RadioButton identityBtn = new RadioButton("Identity");
            ToggleGroup toggleGroup = new ToggleGroup();
            identityBtn.setToggleGroup(toggleGroup);
            selectFeatureBtn.setToggleGroup(toggleGroup);
            clickBehaviour = ClickBehaviours.SELECTED_FEATURE_AND_QUERY;
            selectFeatureBtn.setSelected(true); // select feature is default
            selectFeatureBtn.setOnAction(actionEvent1 -> {
                identityBtn.setSelected(false);
                clickBehaviour = ClickBehaviours.SELECTED_FEATURE_AND_QUERY;
            });
            identityBtn.setOnAction(actionEvent1 -> {
                selectFeatureBtn.setSelected(false);
                clickBehaviour = ClickBehaviours.IDENTITY_QUERY;
            });
            toolBar.getItems().addAll(selectFeatureBtn, identityBtn);
            stackPane.getChildren().add(toolBar);

            Stage clickQueryStage = new Stage();
            clickQueryStage.setScene(new Scene(stackPane));
            clickQueryStage.setResizable(false);
            clickQueryStage.setTitle("Click Query");
            clickQueryStage.setOnCloseRequest(windowEvent -> {
                runtimeStages.remove(RuntimeStageType.CLICK_QUERY);
                clickBehaviour = ClickBehaviours.SELECTED_FEATURE;
            });
            clickQueryStage.show();
            runtimeStages.put(RuntimeStageType.CLICK_QUERY, clickQueryStage);
        } else {
            runtimeStages.get(RuntimeStageType.CLICK_QUERY).toFront();
        }
    }

    public void onDraw(ActionEvent actionEvent) {
        simpleSymbolContainer = new SimpleSymbolContainer();
        if (!runtimeStages.containsKey(RuntimeStageType.DRAW)) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(AppController.class.getResource("Draw.fxml")));
                Pane pane = fxmlLoader.load();
                DrawController drawController = fxmlLoader.getController();
                drawController.setAppController(this);
                Stage drawGeometryStage = new Stage();
                drawController.setParentStage(drawGeometryStage);
                drawGeometryStage.setScene(new Scene(pane));
                drawGeometryStage.setTitle("Draw");
                drawGeometryStage.setResizable(false);
                drawGeometryStage.setOnCloseRequest(windowEvent -> runtimeStages.remove(RuntimeStageType.DRAW));
                drawGeometryStage.show();
                runtimeStages.put(RuntimeStageType.DRAW, drawGeometryStage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            runtimeStages.get(RuntimeStageType.DRAW).toFront();
        }
    }

    public void onLoadShapefile(ActionEvent actionEvent) {
        commonController.loadShapefile(parentStage, mainMapView);
    }

    public void onLoadGeoDatabase(ActionEvent actionEvent) {
        commonController.loadGeoDatabase(parentStage, mainMapView);
    }

    public void onLoadRaster(ActionEvent actionEvent) {
        commonController.loadRaster(parentStage, mainMapView);
    }

    public void onShowCellOut(ActionEvent actionEvent) {
        commonController.showCallOut(mainMapView, longitudeText.getText(), latitudeText.getText());
    }

    public void onLoadOnlineData(ActionEvent actionEvent) {
        commonController.loadOnlineData(mainMapView, onlineDataURLText.getText(), onlineDataTypeChoiceBox.getSelectionModel().getSelectedItem());
    }
}
