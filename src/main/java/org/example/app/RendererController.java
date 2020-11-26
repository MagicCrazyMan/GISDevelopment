package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class RendererController extends AController {

    public TabPane tabPane;
    public ChoiceBox<SimpleMarkerSymbol.Style> simpleRendererMarkerStyleChoiceBox;
    public ColorPicker simpleRendererMarkerColorPicker;
    public Slider simpleRendererMarkerSizeSlider;
    public ChoiceBox<SimpleLineSymbol.Style> simpleRendererOutlineStyleChoiceBox;
    public ColorPicker simpleRendererOutlineColorPicker;
    public Slider simpleRendererOutlineSizeSlider;
    public Label simpleRendererOutlineSizeLabel;
    public Label simpleRendererMarkerSizeLabel;
    public ListView<SimpleFillSymbol.Style> simpleRendererFillStyleListView;
    public ColorPicker simpleRendererFillColorPicker;
    public ChoiceBox<SimpleLineSymbol.MarkerStyle> simpleRendererOutlineArrowStyleChoiceBox;
    public ChoiceBox<SimpleLineSymbol.MarkerPlacement> simpleRendererOutlineArrowPlacementChoiceBox;
    public Tab uniqueRendererTab;
    public TableView<UniqueRendererRowProperty> uniqueRendererFieldsTableView;
    public TableColumn<UniqueRendererRowProperty, Object> uniqueRendererValuesColumn;
    public TableColumn<UniqueRendererRowProperty, Symbol> uniqueRendererSymbolColumn;
    public ChoiceBox<Field> uniqueRendererFieldsChoiceBox;

    private final AppController.SimpleSymbolContainer symbolContainer = new AppController.SimpleSymbolContainer();
    private final CommonController commonController = new CommonController();
    private FeatureLayer featureLayer;

    private MapView parentMapView;

    public MapView getParentMapView() {
        return parentMapView;
    }

    public void setParentMapView(MapView parentMapView) {
        this.parentMapView = parentMapView;
    }

    public void setFeatureLayer(FeatureLayer featureLayer) {
        this.featureLayer = featureLayer;
    }

    @FXML
    public void initialize() {
        symbolContainer.fillSymbol.setOutline(symbolContainer.lineSymbol);
        symbolContainer.markerSymbol.setOutline(symbolContainer.lineSymbol);

        simpleRendererMarkerStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleMarkerSymbol.Style style) {
                return style.name();
            }

            @Override
            public SimpleMarkerSymbol.Style fromString(String s) {
                return SimpleMarkerSymbol.Style.valueOf(s);
            }
        });
        simpleRendererMarkerStyleChoiceBox.getItems().addAll(SimpleMarkerSymbol.Style.values());
        simpleRendererMarkerSizeSlider.valueProperty().addListener((observableValue, number, t1) -> simpleRendererMarkerSizeLabel.setText(String.format("%d px", t1.intValue())));

        simpleRendererOutlineStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.Style style) {
                return style.name();
            }

            @Override
            public SimpleLineSymbol.Style fromString(String s) {
                return SimpleLineSymbol.Style.valueOf(s);
            }
        });
        simpleRendererOutlineStyleChoiceBox.getItems().addAll(SimpleLineSymbol.Style.values());
        simpleRendererOutlineSizeSlider.valueProperty().addListener((observableValue, number, t1) -> simpleRendererOutlineSizeLabel.setText(String.format("%d px", t1.intValue())));
        simpleRendererFillStyleListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        simpleRendererFillStyleListView.setCellFactory(styleListView -> new DrawOptionsController.FillStyleCell());
        simpleRendererFillStyleListView.getItems().addAll(SimpleFillSymbol.Style.values());

        simpleRendererOutlineArrowStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.MarkerStyle markerStyle) {
                return markerStyle.name();
            }

            @Override
            public SimpleLineSymbol.MarkerStyle fromString(String s) {
                return SimpleLineSymbol.MarkerStyle.valueOf(s);
            }
        });
        simpleRendererOutlineArrowStyleChoiceBox.getItems().addAll(SimpleLineSymbol.MarkerStyle.values());
        simpleRendererOutlineArrowPlacementChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.MarkerPlacement markerPlacement) {
                return markerPlacement.name();
            }

            @Override
            public SimpleLineSymbol.MarkerPlacement fromString(String s) {
                return SimpleLineSymbol.MarkerPlacement.valueOf(s);
            }
        });
        simpleRendererOutlineArrowPlacementChoiceBox.getItems().addAll(SimpleLineSymbol.MarkerPlacement.values());


        // init marker symbol options from existed options
        simpleRendererMarkerStyleChoiceBox.setValue(symbolContainer.markerSymbol.getStyle());
        simpleRendererMarkerSizeSlider.setValue(symbolContainer.markerSymbol.getSize());
        simpleRendererMarkerColorPicker.setValue(commonController.int2color(symbolContainer.markerSymbol.getColor()));

        // init polygon symbol options from existed options
        SimpleLineSymbol outlineSymbol = (SimpleLineSymbol) symbolContainer.fillSymbol.getOutline();
        simpleRendererOutlineStyleChoiceBox.setValue(outlineSymbol.getStyle());
        simpleRendererOutlineSizeSlider.setValue(outlineSymbol.getWidth());
        simpleRendererOutlineColorPicker.setValue(commonController.int2color(outlineSymbol.getColor()));
        simpleRendererFillColorPicker.setValue(commonController.int2color(symbolContainer.fillSymbol.getColor()));
        simpleRendererFillStyleListView.getSelectionModel().select(symbolContainer.fillSymbol.getStyle());

        // init unique fields choice box
        uniqueRendererValuesColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        uniqueRendererSymbolColumn.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        // init double to zoom to specified feature
        uniqueRendererFieldsTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        uniqueRendererFieldsTableView.setRowFactory(uniqueRendererFieldCellTableView -> {
            TableRow<UniqueRendererRowProperty> cellTableRow = new TableRow<>();
            cellTableRow.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() >= 2) {
                    if (Objects.nonNull(parentMapView)) {
                        UniqueRendererRowProperty cell = uniqueRendererFieldCellTableView.getSelectionModel().getSelectedItem();
                        QueryParameters queryParameters = new QueryParameters();
                        queryParameters.setWhereClause(String.format("\"%s\" = '%s'", uniqueRendererFieldsChoiceBox.getSelectionModel().getSelectedItem().getName(), cell.getValue()));
                        commonController.simpleQuery(parentMapView, featureLayer, queryParameters);
                    }
                }
            });
            return cellTableRow;
        });
        uniqueRendererSymbolColumn.setCellFactory(uniqueRendererFieldCellSymbolTableColumn -> new UniqueRendererSymbolTableCell());

        tabPane.getSelectionModel().selectedItemProperty().addListener((observableValue, tab, t1) -> {
            StackPane header = (StackPane) tabPane.lookup(".tab-header-background");
            double originStageHeight = parentStage.getHeight(); // stage height contains window manager framework height
            double originSceneHeight = parentStage.getScene().getHeight();
            double newSceneHeight = ((Pane) t1.getContent()).getHeight() + header.getHeight();
            parentStage.setHeight(originStageHeight - originSceneHeight + newSceneHeight);

            if (t1.equals(uniqueRendererTab)) {
                uniqueRendererFieldsChoiceBox.setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Field feature) {
                        return feature.getName();
                    }

                    @Override
                    public Field fromString(String s) {
                        return featureLayer.getFeatureTable().getField(s);
                    }
                });
                uniqueRendererFieldsChoiceBox.getItems().clear();
                uniqueRendererFieldsChoiceBox.getItems().addAll(featureLayer.getFeatureTable().getFields());
                uniqueRendererFieldsChoiceBox.getSelectionModel().selectFirst();
            }
        });
    }

    @Override
    public void setParentStage(Stage stage) {
        super.setParentStage(stage);
        stage.setOnShown(windowEvent -> {
            StackPane header = (StackPane) tabPane.lookup(".tab-header-background");
            double originStageHeight = parentStage.getHeight();
            double originSceneHeight = parentStage.getScene().getHeight();
            double newSceneHeight = ((Pane) tabPane.getSelectionModel().getSelectedItem().getContent()).getHeight() + header.getHeight();
            parentStage.setHeight(originStageHeight - originSceneHeight + newSceneHeight);
        });
    }

    public void onSimpleRendererApply(ActionEvent actionEvent) {
        updateSimpleRendererSymbol();

        SimpleRenderer simpleRenderer = new SimpleRenderer();
        GeometryType geometryType = featureLayer.getFeatureTable().getGeometryType();
        if (geometryType.equals(GeometryType.POINT) || geometryType.equals(GeometryType.MULTIPOINT)) {
            simpleRenderer.setSymbol(symbolContainer.markerSymbol);
        } else if (geometryType.equals(GeometryType.POLYLINE)) {
            simpleRenderer.setSymbol(symbolContainer.fillSymbol.getOutline());
        } else if (geometryType.equals(GeometryType.POLYGON)) {
            simpleRenderer.setSymbol(symbolContainer.fillSymbol);
        }
        featureLayer.setRenderer(simpleRenderer);
    }

    private void updateSimpleRendererSymbol() {
        symbolContainer.markerSymbol.setColor(commonController.color2int(simpleRendererMarkerColorPicker.getValue()));
        symbolContainer.markerSymbol.setStyle(simpleRendererMarkerStyleChoiceBox.getValue());
        symbolContainer.markerSymbol.setSize((float) simpleRendererMarkerSizeSlider.getValue());
        symbolContainer.lineSymbol.setColor(commonController.color2int(simpleRendererOutlineColorPicker.getValue()));
        symbolContainer.lineSymbol.setStyle(simpleRendererOutlineStyleChoiceBox.getValue());
        symbolContainer.lineSymbol.setWidth((float) simpleRendererOutlineSizeSlider.getValue());
        symbolContainer.lineSymbol.setMarkerPlacement(Objects.nonNull(simpleRendererOutlineArrowPlacementChoiceBox.getValue()) ? simpleRendererOutlineArrowPlacementChoiceBox.getValue() : SimpleLineSymbol.MarkerPlacement.BEGIN);
        symbolContainer.lineSymbol.setMarkerStyle(Objects.nonNull(simpleRendererOutlineArrowStyleChoiceBox.getValue()) ? simpleRendererOutlineArrowStyleChoiceBox.getValue() : SimpleLineSymbol.MarkerStyle.NONE);
        symbolContainer.fillSymbol.setColor(commonController.color2int(simpleRendererFillColorPicker.getValue()));
        symbolContainer.fillSymbol.setStyle(simpleRendererFillStyleListView.getSelectionModel().getSelectedItem());
    }

    public void onUniqueRendererLoadField(ActionEvent actionEvent) {
        Field field = uniqueRendererFieldsChoiceBox.getSelectionModel().getSelectedItem();
        if (Objects.nonNull(field)) {
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.setWhereClause("1=1"); // this query param will query all features
            ListenableFuture<FeatureQueryResult> features = featureLayer.getFeatureTable().queryFeaturesAsync(queryParameters);

            features.addDoneListener(() -> {
                try {
                    GeometryType geometryType = featureLayer.getFeatureTable().getGeometryType();

                    // query unique value with a HashSet
                    Set<Object> uniqueValueNameSet = new HashSet<>();
                    for (Feature feature : features.get()) {
                        uniqueValueNameSet.add(feature.getAttributes().get(field.getName()));
                    }

                    // add each value to tableView
                    ObservableList<UniqueRendererRowProperty> cells = uniqueRendererFieldsTableView.getItems();
                    cells.clear();
                    for (Object valueName : uniqueValueNameSet) {
                        UniqueRendererRowProperty cell;
                        if (geometryType.equals(GeometryType.POLYGON) || geometryType.equals(GeometryType.ENVELOPE)) {
                            // create a random color for each value firstly
                            SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol();
                            simpleFillSymbol.setColor(commonController.color2int(Color.rgb(
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    Math.random())
                            ));
                            simpleFillSymbol.setOutline(
                                    new SimpleLineSymbol(
                                            SimpleLineSymbol.Style.SOLID,
                                            commonController.color2int(Color.BLACK),
                                            2
                                    )
                            );

                            cell = new UniqueRendererRowProperty(field, valueName, simpleFillSymbol);
                        } else {
                            break;
                        }
                        cells.add(cell);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void onUniqueRendererApply(ActionEvent actionEvent) {
        UniqueValueRenderer uniqueValueRenderer = new UniqueValueRenderer();
        uniqueValueRenderer.setDefaultSymbol(new SimpleFillSymbol(
                SimpleFillSymbol.Style.SOLID,
                commonController.color2int(Color.rgb(0, 0, 0, 0.5)),
                new SimpleLineSymbol(
                        SimpleLineSymbol.Style.SOLID,
                        commonController.color2int(Color.BLACK),
                        1
                )
        ));

        for (UniqueRendererRowProperty property : uniqueRendererFieldsTableView.getItems()) {
            String fieldName = property.getField().getName();
            String value = property.getValue().toString();
            Symbol symbol = property.getSymbol();

            if (!uniqueValueRenderer.getFieldNames().contains(fieldName)) {
                uniqueValueRenderer.getFieldNames().add(fieldName);
            }

            List<Object> list = new ArrayList<>(1);
            list.add(value);
            uniqueValueRenderer.getUniqueValues().add(new UniqueValueRenderer.UniqueValue(
                    fieldName,
                    value,
                    symbol,
                    list)
            );
        }

        featureLayer.setRenderer(uniqueValueRenderer);
    }

    public static class UniqueRendererRowProperty {
        private final SimpleObjectProperty<Field> field = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Object> value = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Symbol> symbol = new SimpleObjectProperty<>();

        public UniqueRendererRowProperty(Field field, Object value, Symbol symbol) {
            this.field.set(field);
            this.value.set(value);
            this.symbol.set(symbol);
        }

        public Field getField() {
            return field.get();
        }

        public SimpleObjectProperty<Field> fieldProperty() {
            return field;
        }

        public void setField(Field field) {
            this.field.set(field);
        }

        public Object getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }

        public SimpleObjectProperty<Object> valueProperty() {
            return value;
        }

        public Symbol getSymbol() {
            return symbol.get();
        }

        public SimpleObjectProperty<Symbol> symbolProperty() {
            return symbol;
        }

        public void setSymbol(Symbol symbol) {
            this.symbol.set(symbol);
        }
    }

    public static class UniqueRendererSymbolTableCell extends TableCell<UniqueRendererRowProperty, Symbol> {
        final CommonController commonController = new CommonController();

        @Override
        protected void updateItem(Symbol item, boolean empty) {
            super.updateItem(item, empty);

            if (Objects.isNull(item) || empty) {
                setGraphic(null);
                setText(null);
            } else {
                if (Objects.isNull(getGraphic())) {
                    HBox hBox = new HBox();
                    hBox.setSpacing(5);
                    hBox.setAlignment(Pos.CENTER);
                    StackPane.setAlignment(hBox, Pos.CENTER);
                    StackPane stackPane = new StackPane(hBox);

                    ColorPicker colorPicker = new ColorPicker();
                    Canvas canvas;
                    if (item instanceof SimpleFillSymbol) {
                        SimpleFillSymbol symbol = (SimpleFillSymbol) item;
                        canvas = new Canvas(30, 20);
                        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
                        // fill color followed symbol color
                        graphicsContext.setFill(commonController.int2color(symbol.getColor()));
                        graphicsContext.fillRect(0, 0, 30, 20);
                        // draw border
                        graphicsContext.setStroke(commonController.int2color(symbol.getOutline().getColor()));
                        graphicsContext.setLineWidth(1);
                        graphicsContext.strokeRect(0, 0, 30, 20);

                        // set colorPicker
                        colorPicker.setValue(commonController.int2color(symbol.getColor()));
                        colorPicker.valueProperty().addListener((observableValue, color, t1) -> {
                            // update symbol's color
                            symbol.setColor(commonController.color2int(t1));
                            // redraw canvas
                            graphicsContext.setFill(t1);
                            graphicsContext.fillRect(0, 0, 30, 20);
                        });
                    } else {
                        return;
                    }

                    hBox.getChildren().addAll(canvas, colorPicker);
                    setGraphic(stackPane);
                }
            }
        }
    }
}
