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
import javafx.util.Callback;
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

    private AppController appController;
    private final CommonController commonController = new CommonController();

    public void setAppController(AppController appController) {
        this.appController = appController;

        // init marker symbol options from existed options
        markerStyleChoiceBox.setValue(appController.drawingOptions.markerSymbol.getStyle());
        markerSizeSlider.setValue(appController.drawingOptions.markerSymbol.getSize());
        markerColorPicker.setValue(int2color(appController.drawingOptions.markerSymbol.getColor()));

        // init polyline symbol options from existed options
        polylineStyleChoiceBox.setValue(appController.drawingOptions.lineSymbol.getStyle());
        polylineMarkerStyleChoiceBox.setValue(appController.drawingOptions.lineSymbol.getMarkerStyle());
        polylineMarkerPlacementChoiceBox.setValue(appController.drawingOptions.lineSymbol.getMarkerPlacement());
        polylineSizeSlider.setValue(appController.drawingOptions.lineSymbol.getWidth());
        polylineColorPicker.setValue(int2color(appController.drawingOptions.lineSymbol.getColor()));

        // init polygon symbol options from existed options
        SimpleLineSymbol outlineSymbol = (SimpleLineSymbol) appController.drawingOptions.fillSymbol.getOutline();
        polygonOutlineStyleChoiceBox.setValue(outlineSymbol.getStyle());
        polygonOutlineSizeSlider.setValue(outlineSymbol.getWidth());
        polygonOutlineColorPicker.setValue(int2color(outlineSymbol.getColor()));
        polygonFillColorPicker.setValue(int2color(appController.drawingOptions.fillSymbol.getColor()));
        polygonFillStyleListView.getSelectionModel().select(appController.drawingOptions.fillSymbol.getStyle());
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
        polygonFillStyleListView.setCellFactory(styleListView -> new FillCell());
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
        appController.drawingOptions.markerSymbol.setSize((float) markerSizeSlider.getValue());
        appController.drawingOptions.markerSymbol.setStyle(markerStyleChoiceBox.getValue());
        appController.drawingOptions.markerSymbol.setColor(color2int(markerColorPicker.getValue()));
    }

    public void onPolylineStyleApply(ActionEvent actionEvent) {
        appController.drawingOptions.lineSymbol.setMarkerStyle(polylineMarkerStyleChoiceBox.getValue());
        appController.drawingOptions.lineSymbol.setMarkerPlacement(polylineMarkerPlacementChoiceBox.getValue());
        appController.drawingOptions.lineSymbol.setStyle(polylineStyleChoiceBox.getValue());
        appController.drawingOptions.lineSymbol.setColor(color2int(polylineColorPicker.getValue()));
        appController.drawingOptions.lineSymbol.setWidth((float) polylineSizeSlider.getValue());
    }

    public void onPolygonStyleApply(ActionEvent actionEvent) {
        SimpleLineSymbol outlineSymbol = (SimpleLineSymbol) appController.drawingOptions.fillSymbol.getOutline();
        outlineSymbol.setStyle(polygonOutlineStyleChoiceBox.getValue());
        outlineSymbol.setColor(color2int(polygonOutlineColorPicker.getValue()));
        outlineSymbol.setWidth((float) polygonOutlineSizeSlider.getValue());
        appController.drawingOptions.fillSymbol.setStyle(polygonFillStyleListView.getSelectionModel().getSelectedItem());
        appController.drawingOptions.fillSymbol.setColor(color2int(polygonFillColorPicker.getValue()));
    }

    private int color2int(Color color) {
        int r = ((int) (color.getRed() * 255)) << 16;
        int g = ((int) (color.getGreen() * 255)) << 8;
        int b = ((int) (color.getBlue() * 255));
        int a = ((int) (color.getOpacity() * 255)) << 24;
        return (a | r | g | b);
    }

    private Color int2color(int color) {
        return Color.rgb(
                (color >>> 16) & 0xFF,
                (color >>> 8) & 0xFF,
                color & 0xFF,
                ((color >>> 24) & 0xFF) / 255.0
        );
    }

    public static class FillCell extends ListCell<SimpleFillSymbol.Style> {
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
