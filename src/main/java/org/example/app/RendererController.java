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
    public TableView<SelectableRowProperty> uniqueRendererFieldsTableView;
    public TableColumn<SelectableRowProperty, Object> uniqueRendererValuesColumn;
    public TableColumn<SelectableRowProperty, Symbol> uniqueRendererSymbolColumn;
    public ChoiceBox<Field> uniqueRendererFieldsChoiceBox;
    public ChoiceBox<Field> classBreakRendererFieldsChoiceBox;
    public TableView<SelectableRowProperty> classBreakRendererFieldsTableView;
    public TableColumn<SelectableRowProperty, ClassBreakerRowValueRange> classBreakRendererValuesColumn;
    public TableColumn<SelectableRowProperty, Symbol> classBreakRendererSymbolColumn;
    public Tab classBreakRendererTab;
    public Spinner<Double> classBreakerLevelText;

    private final AppController.SimpleSymbolContainer symbolContainer = new AppController.SimpleSymbolContainer();
    private final CommonController commonController = new CommonController();
    private FeatureLayer featureLayer;
    private final Symbol defaultSymbol = new SimpleFillSymbol(
            SimpleFillSymbol.Style.SOLID,
            commonController.color2int(Color.rgb(0, 0, 0, 0.5)),
            new SimpleLineSymbol(
                    SimpleLineSymbol.Style.SOLID,
                    commonController.color2int(Color.BLACK),
                    1
            )
    );

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
            TableRow<SelectableRowProperty> cellTableRow = new TableRow<>();
            cellTableRow.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() >= 2) {
                    if (Objects.nonNull(parentMapView)) {
                        SelectableRowProperty cell = uniqueRendererFieldCellTableView.getSelectionModel().getSelectedItem();
                        QueryParameters queryParameters = new QueryParameters();
                        queryParameters.setWhereClause(String.format("\"%s\" = '%s'", uniqueRendererFieldsChoiceBox.getSelectionModel().getSelectedItem().getName(), cell.getValue()));
                        commonController.simpleQuery(parentMapView, featureLayer, queryParameters);
                    }
                }
            });
            return cellTableRow;
        });
        uniqueRendererSymbolColumn.setCellFactory(selectableRowPropertySymbolTableColumn -> new SelectableTableCell());

        // init class breaker
        classBreakRendererFieldsTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        classBreakRendererFieldsTableView.setRowFactory(uniqueRendererFieldCellTableView -> {
            TableRow<SelectableRowProperty> cellTableRow = new TableRow<>();
            cellTableRow.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() >= 2) {
                    if (Objects.nonNull(parentMapView)) {
                        SelectableRowProperty cell = uniqueRendererFieldCellTableView.getSelectionModel().getSelectedItem();
                        QueryParameters queryParameters = new QueryParameters();
                        queryParameters.setWhereClause(String.format("\"%s\" = '%s'", classBreakRendererFieldsChoiceBox.getSelectionModel().getSelectedItem().getName(), cell.getValue()));
                        commonController.simpleQuery(parentMapView, featureLayer, queryParameters);
                    }
                }
            });
            return cellTableRow;
        });
        classBreakRendererSymbolColumn.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        classBreakRendererValuesColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        classBreakRendererSymbolColumn.setCellFactory(selectableRowPropertySymbolTableColumn -> new SelectableTableCell());
        classBreakRendererValuesColumn.setCellFactory(selectableRowPropertyClassBreakerRowValueRangeTableColumn -> new ClassBreakerClassesTableCell());
        classBreakerLevelText.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1, Double.MAX_VALUE, 4));

        tabPane.getSelectionModel().selectedItemProperty().addListener((observableValue, tab, t1) -> {
            StackPane header = (StackPane) tabPane.lookup(".tab-header-background");
            double originStageHeight = parentStage.getHeight(); // stage height contains window manager framework height
            double originSceneHeight = parentStage.getScene().getHeight();
            double newSceneHeight = ((Pane) t1.getContent()).getHeight() + header.getHeight();
            parentStage.setHeight(originStageHeight - originSceneHeight + newSceneHeight);

            if (t1.equals(uniqueRendererTab) && Objects.isNull(t1.getUserData())) {
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
                t1.setUserData(new Object());
            } else if (t1.equals(classBreakRendererTab) && Objects.isNull(classBreakRendererTab.getUserData())) {
                classBreakRendererFieldsChoiceBox.setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Field field) {
                        return field.getName();
                    }

                    @Override
                    public Field fromString(String s) {
                        return featureLayer.getFeatureTable().getField(s);
                    }
                });

                // only numerical fieldType will be added to classBreaker renderer
                for (Field field : featureLayer.getFeatureTable().getFields()) {
                    Field.Type type = field.getFieldType();
                    if (type.equals(Field.Type.DOUBLE) || type.equals(Field.Type.FLOAT) || type.equals(Field.Type.INTEGER) || type.equals(Field.Type.SHORT)) {
                        classBreakRendererFieldsChoiceBox.getItems().add(field);
                    }
                }
                classBreakRendererFieldsChoiceBox.getSelectionModel().selectFirst();
                classBreakRendererTab.setUserData(new Object());
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
                    ObservableList<SelectableRowProperty> cells = uniqueRendererFieldsTableView.getItems();
                    cells.clear();
                    for (Object valueName : uniqueValueNameSet) {
                        SelectableRowProperty cell;
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

                            cell = new SelectableRowProperty(field, valueName, simpleFillSymbol);
                        } else if (geometryType.equals(GeometryType.POLYLINE)) {
                            SimpleLineSymbol simpleLineSymbol = new SimpleLineSymbol();
                            simpleLineSymbol.setColor(commonController.color2int(Color.rgb(
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    Math.random())
                            ));

                            cell = new SelectableRowProperty(field, valueName, simpleLineSymbol);
                        } else if (geometryType.equals(GeometryType.MULTIPOINT) || geometryType.equals(GeometryType.POINT)) {
                            SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol();
                            simpleMarkerSymbol.setColor(commonController.color2int(Color.rgb(
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    Math.random())
                            ));
                            simpleMarkerSymbol.setOutline(
                                    new SimpleLineSymbol(
                                            SimpleLineSymbol.Style.SOLID,
                                            commonController.color2int(Color.BLACK),
                                            1
                                    )
                            );

                            cell = new SelectableRowProperty(field, valueName, simpleMarkerSymbol);
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
        uniqueValueRenderer.setDefaultSymbol(defaultSymbol);

        for (SelectableRowProperty property : uniqueRendererFieldsTableView.getItems()) {
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

    public void onClassBreakRendererLoadField(ActionEvent actionEvent) {
        Field field = classBreakRendererFieldsChoiceBox.getValue();
        if (Objects.nonNull(field)) {
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.setWhereClause("1=1");
            ListenableFuture<FeatureQueryResult> future = featureLayer.getFeatureTable().queryFeaturesAsync(queryParameters);
            future.addDoneListener(() -> {
                try {
                    double maxValue = Double.MIN_VALUE;
                    double minValue = Double.MAX_VALUE;
                    GeometryType geometryType = future.get().getGeometryType();
                    for (Feature feature : future.get()) {
                        double value = ((Number) feature.getAttributes().get(field.getName())).doubleValue();
                        if (value > maxValue) {
                            maxValue = value;
                        }
                        if (value < minValue) {
                            minValue = value;
                        }
                    }

                    double interval = (maxValue - minValue) / classBreakerLevelText.getValue();
                    ObservableList<SelectableRowProperty> items = classBreakRendererFieldsTableView.getItems();
                    items.clear();
                    for (double currentValue = minValue, nextValue; currentValue < maxValue; currentValue += interval) {
                        Symbol symbol;
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
                            symbol = simpleFillSymbol;
                        } else if (geometryType.equals(GeometryType.POLYLINE)) {
                            SimpleLineSymbol simpleLineSymbol = new SimpleLineSymbol();
                            simpleLineSymbol.setColor(commonController.color2int(Color.rgb(
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    Math.random())
                            ));
                            symbol = simpleLineSymbol;
                        } else if (geometryType.equals(GeometryType.MULTIPOINT) || geometryType.equals(GeometryType.POINT)) {
                            SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol();
                            simpleMarkerSymbol.setColor(commonController.color2int(Color.rgb(
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    (int) (Math.random() * 256),
                                    Math.random())
                            ));
                            simpleMarkerSymbol.setOutline(
                                    new SimpleLineSymbol(
                                            SimpleLineSymbol.Style.SOLID,
                                            commonController.color2int(Color.BLACK),
                                            1
                                    )
                            );
                            symbol = simpleMarkerSymbol;
                        } else {
                            break;
                        }

                        nextValue = Math.min(currentValue + interval, maxValue);
                        items.add(new SelectableRowProperty(field, new ClassBreakerRowValueRange(currentValue, nextValue, interval), symbol));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void onClassBreakRendererApply(ActionEvent actionEvent) {
        ClassBreaksRenderer classBreaksRenderer = new ClassBreaksRenderer();
        classBreaksRenderer.setDefaultSymbol(defaultSymbol);

        List<ClassBreaksRenderer.ClassBreak> classBreaks = classBreaksRenderer.getClassBreaks();
        for (int i = 0; i < classBreakRendererFieldsTableView.getItems().size(); i++) {
            SelectableRowProperty rowProperty = classBreakRendererFieldsTableView.getItems().get(i);

            if (Objects.isNull(classBreaksRenderer.getFieldName()) || classBreaksRenderer.getFieldName().isBlank()) {
                classBreaksRenderer.setFieldName(rowProperty.getField().getName());
            }

            ClassBreakerRowValueRange rowValueRange = (ClassBreakerRowValueRange) rowProperty.getValue();
            classBreaks.add(new ClassBreaksRenderer.ClassBreak(
                    rowProperty.getField().getName(),
                    String.format("%s - %s", rowValueRange.getStart(), rowValueRange.getEnd()),
                    rowValueRange.getStart().doubleValue(),
                    rowValueRange.getEnd().doubleValue(),
                    rowProperty.getSymbol()
            ));
        }
        featureLayer.setRenderer(classBreaksRenderer);
    }

    public static class ClassBreakerRowValueRange {
        private Number start;
        private Number end;
        private Number interval;

        public ClassBreakerRowValueRange(Number start, Number end, Number interval) {
            this.start = start;
            this.end = end;
            this.interval = interval;
        }

        public Number getStart() {
            return start;
        }

        public void setStart(Number start) {
            this.start = start;
        }

        public Number getEnd() {
            return end;
        }

        public void setEnd(Number end) {
            this.end = end;
        }
    }

    public static class SelectableRowProperty {
        private final SimpleObjectProperty<Field> field = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Object> value = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Symbol> symbol = new SimpleObjectProperty<>();

        public SelectableRowProperty(Field field, Object value, Symbol symbol) {
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

    public static class ClassBreakerClassesTableCell extends TableCell<SelectableRowProperty, ClassBreakerRowValueRange> {
        @Override
        protected void updateItem(ClassBreakerRowValueRange item, boolean empty) {
            super.updateItem(item, empty);
            if (Objects.isNull(item) || empty) {
                setGraphic(null);
                setText(null);
            } else {
                setText(String.format("%s - %s", item.getStart().toString(), item.getEnd().toString()));
            }
        }
    }

    public static class SelectableTableCell extends TableCell<SelectableRowProperty, Symbol> {
        final CommonController commonController = new CommonController();

        @Override
        protected void updateItem(Symbol item, boolean empty) {
            super.updateItem(item, empty);

            if (Objects.isNull(item) || empty) {
                setGraphic(null);
                setText(null);
            } else {
                HBox hBox = new HBox();
                hBox.setSpacing(5);
                hBox.setAlignment(Pos.CENTER);
                StackPane.setAlignment(hBox, Pos.CENTER);
                StackPane stackPane = new StackPane(hBox);

                ColorPicker colorPicker = new ColorPicker();
                Canvas canvas = new Canvas(30, 20);
                if (item instanceof SimpleFillSymbol) {
                    FillSymbol fillSymbol = (FillSymbol) item;
                    GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
                    // fill color followed symbol color
                    graphicsContext.setFill(commonController.int2color(fillSymbol.getColor()));
                    graphicsContext.fillRect(0, 0, 30, 20);
                    // draw border
                    graphicsContext.setStroke(commonController.int2color(fillSymbol.getOutline().getColor()));
                    graphicsContext.setLineWidth(1);
                    graphicsContext.strokeRect(0, 0, 30, 20);

                    // set colorPicker
                    colorPicker.setValue(commonController.int2color(fillSymbol.getColor()));
                    colorPicker.valueProperty().addListener((observableValue, color, t1) -> {
                        // update symbol's color
                        fillSymbol.setColor(commonController.color2int(t1));
                        // redraw canvas
                        graphicsContext.setFill(t1);
                        graphicsContext.fillRect(0, 0, 30, 20);
                        graphicsContext.setStroke(commonController.int2color(fillSymbol.getOutline().getColor()));
                        graphicsContext.setLineWidth(1);
                        graphicsContext.strokeRect(0, 0, 30, 20);
                    });
                } else if (item instanceof SimpleLineSymbol) {
                    LineSymbol lineSymbol = (LineSymbol) item;
                    GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
                    // do the same things as above
                    graphicsContext.setStroke(commonController.int2color(lineSymbol.getColor()));
                    graphicsContext.setLineWidth(1);
                    graphicsContext.strokeLine(0, 10, 30, 10);

                    colorPicker.setValue(commonController.int2color(lineSymbol.getColor()));
                    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                        lineSymbol.setColor(commonController.color2int(newValue));
                        graphicsContext.setStroke(newValue);
                        graphicsContext.setLineWidth(1);
                        graphicsContext.strokeLine(0, 10, 30, 10);
                    });
                } else if (item instanceof SimpleMarkerSymbol) {
                    SimpleMarkerSymbol markerSymbol = (SimpleMarkerSymbol) item;
                    GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

                    graphicsContext.setFill(commonController.int2color(markerSymbol.getColor()));
                    graphicsContext.fillOval(5, 0, 20, 20);

                    graphicsContext.setStroke(commonController.int2color(markerSymbol.getOutline().getColor()));
                    graphicsContext.setLineWidth(1);
                    graphicsContext.strokeOval(6, 1, 18, 18);

                    colorPicker.setValue(commonController.int2color(markerSymbol.getColor()));
                    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                        markerSymbol.setColor(commonController.color2int(newValue));
                        graphicsContext.setFill(newValue);
                        graphicsContext.fillOval(5, 0, 20, 20);
                        graphicsContext.setStroke(commonController.int2color(markerSymbol.getOutline().getColor()));
                        graphicsContext.setLineWidth(1);
                        graphicsContext.strokeOval(6, 1, 18, 18);
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
