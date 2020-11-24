package org.example.app;

import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DrawOptionsController extends AController {
    public ChoiceBox<SimpleMarkerSymbol.Style> markerStyleChoiceBox;
    public ColorPicker markerColorPicker;
    public Slider markerSizeSlider;
    public Label markerSizeLabel;
    public Button markerApplyBtn;
    public ChoiceBox<SimpleLineSymbol.MarkerStyle> polylineMarkerStyleChoiceBox;
    public ChoiceBox<SimpleLineSymbol.Style> polylineStyleChoiceBox;
    public ChoiceBox<SimpleLineSymbol.MarkerPlacement> polylineMarkerPlacementChoiceBox;
    public ColorPicker polylineColorPicker;
    public Slider polylineSizeSlider;
    public Label polylineSizeLabel;
    public Button polylineApplyBtn;
    public TabPane tabPane;
    public Pane mainPane;
    public Button polygonApplyBtn;
    public ChoiceBox<SimpleLineSymbol.Style> polygonOutlineStyleChoiceBox;
    public ColorPicker polygonOutlineColorPicker;
    public Slider polygonOutlineSizeSlider;
    public Label polylineOutlineSizeLabel;
    public ColorPicker polygonFillColorPicker;
    public ListView<SimpleFillSymbol.Style> polygonFillStyleListView;
    public ChoiceBox<SimpleLineSymbol.Style> markerOutlineStyleChoiceBox;
    public ColorPicker markerOutlineColorPicker;

    private AppController appController;
    private CommonController commonController = new CommonController();

    public void setAppController(AppController appController) {
        this.appController = appController;

        // init marker symbol options from existed options
        markerStyleChoiceBox.setValue(appController.simpleSymbolContainer.markerSymbol.getStyle());
        markerSizeSlider.setValue(appController.simpleSymbolContainer.markerSymbol.getSize());
        markerColorPicker.setValue(commonController.int2color(appController.simpleSymbolContainer.markerSymbol.getColor()));
        markerOutlineStyleChoiceBox.setValue(appController.simpleSymbolContainer.markerSymbol.getOutline().getStyle());
        markerOutlineColorPicker.setValue(commonController.int2color(appController.simpleSymbolContainer.markerSymbol.getOutline().getColor()));

        // init polyline symbol options from existed options
        polylineStyleChoiceBox.setValue(appController.simpleSymbolContainer.lineSymbol.getStyle());
        polylineMarkerStyleChoiceBox.setValue(appController.simpleSymbolContainer.lineSymbol.getMarkerStyle());
        polylineMarkerPlacementChoiceBox.setValue(appController.simpleSymbolContainer.lineSymbol.getMarkerPlacement());
        polylineSizeSlider.setValue(appController.simpleSymbolContainer.lineSymbol.getWidth());
        polylineColorPicker.setValue(commonController.int2color(appController.simpleSymbolContainer.lineSymbol.getColor()));

        // init polygon symbol options from existed options
        SimpleLineSymbol outlineSymbol = (SimpleLineSymbol) appController.simpleSymbolContainer.fillSymbol.getOutline();
        polygonOutlineStyleChoiceBox.setValue(outlineSymbol.getStyle());
        polygonOutlineSizeSlider.setValue(outlineSymbol.getWidth());
        polygonOutlineColorPicker.setValue(commonController.int2color(outlineSymbol.getColor()));
        polygonFillColorPicker.setValue(commonController.int2color(appController.simpleSymbolContainer.fillSymbol.getColor()));
        polygonFillStyleListView.getSelectionModel().select(appController.simpleSymbolContainer.fillSymbol.getStyle());
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

    @FXML
    public void initialize() {
        // init marker
        markerStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleMarkerSymbol.Style style) {
                return style.name();
            }

            @Override
            public SimpleMarkerSymbol.Style fromString(String s) {
                return SimpleMarkerSymbol.Style.valueOf(s);
            }
        });
        markerStyleChoiceBox.getItems().addAll(SimpleMarkerSymbol.Style.values());
        markerSizeSlider.valueProperty().addListener((observableValue, number, t1) -> markerSizeLabel.setText(String.format("%d px", t1.intValue())));
        markerOutlineStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.Style markerStyle) {
                return markerStyle.name();
            }

            @Override
            public SimpleLineSymbol.Style fromString(String s) {
                return SimpleLineSymbol.Style.valueOf(s);
            }
        });
        markerOutlineStyleChoiceBox.getItems().addAll(SimpleLineSymbol.Style.values());

        // init polyline
        polylineMarkerStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.MarkerStyle markerStyle) {
                return markerStyle.name();
            }

            @Override
            public SimpleLineSymbol.MarkerStyle fromString(String s) {
                return SimpleLineSymbol.MarkerStyle.valueOf(s);
            }
        });
        polylineMarkerStyleChoiceBox.getItems().addAll(SimpleLineSymbol.MarkerStyle.values());
        polylineStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.Style style) {
                return style.name();
            }

            @Override
            public SimpleLineSymbol.Style fromString(String s) {
                return SimpleLineSymbol.Style.valueOf(s);
            }
        });
        polylineStyleChoiceBox.getItems().addAll(SimpleLineSymbol.Style.values());
        polylineMarkerPlacementChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.MarkerPlacement markerPlacement) {
                return markerPlacement.name();
            }

            @Override
            public SimpleLineSymbol.MarkerPlacement fromString(String s) {
                return SimpleLineSymbol.MarkerPlacement.valueOf(s);
            }
        });
        polylineMarkerPlacementChoiceBox.getItems().addAll(SimpleLineSymbol.MarkerPlacement.values());
        polylineSizeSlider.valueProperty().addListener((observableValue, number, t1) -> polylineSizeLabel.setText(String.format("%d px", t1.intValue())));

        // init polygon
        polygonOutlineStyleChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleLineSymbol.Style style) {
                return style.name();
            }

            @Override
            public SimpleLineSymbol.Style fromString(String s) {
                return SimpleLineSymbol.Style.valueOf(s);
            }
        });
        polygonOutlineStyleChoiceBox.getItems().addAll(SimpleLineSymbol.Style.values());
        polygonOutlineSizeSlider.valueProperty().addListener((observableValue, number, t1) -> polylineOutlineSizeLabel.setText(String.format("%d px", t1.intValue())));
        polygonFillStyleListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        polygonFillStyleListView.setCellFactory(styleListView -> new FillStyleCell());
        polygonFillStyleListView.getItems().addAll(SimpleFillSymbol.Style.values());

        // init scene responsive scene size
        tabPane.getSelectionModel().selectedItemProperty().addListener((observableValue, tab, t1) -> {
            StackPane header = (StackPane) tabPane.lookup(".tab-header-background");
            double originStageHeight = parentStage.getHeight(); // stage height contains window manager framework height
            double originSceneHeight = parentStage.getScene().getHeight();
            double newSceneHeight = ((VBox) t1.getContent()).getHeight() + header.getHeight();
            parentStage.setHeight(originStageHeight - originSceneHeight + newSceneHeight);
        });
    }

    public void onMarkerStyleApply(ActionEvent actionEvent) {
        appController.simpleSymbolContainer.markerSymbol.setSize((float) markerSizeSlider.getValue());
        appController.simpleSymbolContainer.markerSymbol.setStyle(markerStyleChoiceBox.getValue());
        appController.simpleSymbolContainer.markerSymbol.setColor(commonController.color2int(markerColorPicker.getValue()));
        appController.simpleSymbolContainer.markerSymbol.getOutline().setColor(commonController.color2int(markerOutlineColorPicker.getValue()));
        appController.simpleSymbolContainer.markerSymbol.getOutline().setStyle(markerOutlineStyleChoiceBox.getValue());
    }

    public void onPolylineStyleApply(ActionEvent actionEvent) {
        appController.simpleSymbolContainer.lineSymbol.setMarkerStyle(polylineMarkerStyleChoiceBox.getValue());
        appController.simpleSymbolContainer.lineSymbol.setMarkerPlacement(polylineMarkerPlacementChoiceBox.getValue());
        appController.simpleSymbolContainer.lineSymbol.setStyle(polylineStyleChoiceBox.getValue());
        appController.simpleSymbolContainer.lineSymbol.setColor(commonController.color2int(polylineColorPicker.getValue()));
        appController.simpleSymbolContainer.lineSymbol.setWidth((float) polylineSizeSlider.getValue());
    }

    public void onPolygonStyleApply(ActionEvent actionEvent) {
        SimpleLineSymbol outlineSymbol = (SimpleLineSymbol) appController.simpleSymbolContainer.fillSymbol.getOutline();
        outlineSymbol.setStyle(polygonOutlineStyleChoiceBox.getValue());
        outlineSymbol.setColor(commonController.color2int(polygonOutlineColorPicker.getValue()));
        outlineSymbol.setWidth((float) polygonOutlineSizeSlider.getValue());
        appController.simpleSymbolContainer.fillSymbol.setStyle(polygonFillStyleListView.getSelectionModel().getSelectedItem());
        appController.simpleSymbolContainer.fillSymbol.setColor(commonController.color2int(polygonFillColorPicker.getValue()));
    }

    public static class FillStyleCell extends ListCell<SimpleFillSymbol.Style> {
        final static Map<SimpleFillSymbol.Style, String> imagesMap = new HashMap<>();
        final static Map<SimpleFillSymbol.Style, Image> imagesLoadedMap = new HashMap<>();

        static {
            imagesMap.put(SimpleFillSymbol.Style.SOLID, "FSolid.png");
            imagesMap.put(SimpleFillSymbol.Style.BACKWARD_DIAGONAL, "FXie.png");
            imagesMap.put(SimpleFillSymbol.Style.CROSS, "FVerHori.png");
            imagesMap.put(SimpleFillSymbol.Style.DIAGONAL_CROSS, "FDialog2.png");
            imagesMap.put(SimpleFillSymbol.Style.FORWARD_DIAGONAL, "FXie2.png");
            imagesMap.put(SimpleFillSymbol.Style.HORIZONTAL, "FHori.png");
            imagesMap.put(SimpleFillSymbol.Style.VERTICAL, "FVer.png");
            imagesMap.put(SimpleFillSymbol.Style.NULL, "FNo.png");
        }

        @Override
        protected void updateItem(SimpleFillSymbol.Style item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || Objects.isNull(item)) {
                setText(null);
                setGraphic(null);
            } else {
                if (Objects.isNull(getGraphic())) {
                    Image image;
                    if (imagesLoadedMap.containsKey(item)) {
                        image = imagesLoadedMap.get(item);
                    } else {
                        image = new Image(DrawOptionsController.class.getResourceAsStream("images/" + imagesMap.get(item)));
                        imagesLoadedMap.put(item, image);
                    }
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(20);
                    imageView.setFitHeight(20);
                    Label label = new Label(item.name());
                    HBox.setHgrow(label, Priority.ALWAYS);

                    HBox hBox = new HBox(imageView, label);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    hBox.setSpacing(5);

                    setGraphic(hBox);
                } else {
                    HBox hBox = (HBox) getGraphic();
                    Image image;
                    if (imagesLoadedMap.containsKey(item)) {
                        image = imagesLoadedMap.get(item);
                    } else {
                        image = new Image(DrawOptionsController.class.getResourceAsStream("images/" + imagesMap.get(item)));
                    }
                    ImageView imageView = (ImageView) hBox.getChildren().get(0);
                    Label label = (Label) hBox.getChildren().get(1);

                    imageView.setImage(image);
                    label.setText(item.name());
                }
            }
        }
    }

}
