package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.symbology.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.Iterator;
import java.util.Objects;
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
    public TableView<UniqueRendererFieldCell> uniqueRendererFieldsTableView;
    public TableColumn<UniqueRendererFieldCell, String> uniqueRendererFeaturesColumn;
    public TableColumn uniqueRendererSymbolColumn;

    private AppController.SimpleSymbolContainer symbolContainer = new AppController.SimpleSymbolContainer();
    private CommonController commonController = new CommonController();
    private FeatureLayer featureLayer;

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
        uniqueRendererFeaturesColumn.setCellValueFactory(uniqueRendererFieldCellStringCellDataFeatures -> null);

        tabPane.getSelectionModel().selectedItemProperty().addListener((observableValue, tab, t1) -> {
            StackPane header = (StackPane) tabPane.lookup(".tab-header-background");
            double originStageHeight = parentStage.getHeight(); // stage height contains window manager framework height
            double originSceneHeight = parentStage.getScene().getHeight();
            double newSceneHeight = ((Pane) t1.getContent()).getHeight() + header.getHeight();
            parentStage.setHeight(originStageHeight - originSceneHeight + newSceneHeight);

            if (t1.equals(uniqueRendererTab)) {
                uniqueRendererFieldsTableView.getItems().clear();
                QueryParameters queryParameters = new QueryParameters();
                queryParameters.setWhereClause("1=1");
                ListenableFuture<FeatureQueryResult> features = featureLayer.getFeatureTable().queryFeaturesAsync(queryParameters);
                features.addDoneListener(() -> {
                    try {
                        for (Feature feature : features.get()) {
                            UniqueRendererFieldCell cell = new UniqueRendererFieldCell();
                            cell.setGeometry(feature.getGeometry());
                            uniqueRendererFieldsTableView.getItems().add(cell);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });            }
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

    static class UniqueRendererFieldCell {
        final SimpleObjectProperty<Geometry> geometry = new SimpleObjectProperty<>();

        public Geometry getGeometry() {
            return geometry.get();
        }

        public void setGeometry(Geometry geometry) {
            this.geometry.set(geometry);
        }
    }
}
