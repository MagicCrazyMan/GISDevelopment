package org.example.app;

import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class DrawOptionsController {
    public ChoiceBox<SimpleMarkerSymbol.Style> markerStyleChoiceBox;
    public ColorPicker markerColorPicker;
    public Slider markerSizeSlider;
    public Label markerSizeLabel;
    public Button markerApplyBtn;

    private AppController appController;
    private final CommonController commonController = new CommonController();

    public void setAppController(AppController appController) {
        this.appController = appController;
        markerStyleChoiceBox.setValue(appController.drawingOptions.markerSymbol.getStyle());
        markerSizeSlider.setValue(appController.drawingOptions.markerSymbol.getSize());
        int color = appController.drawingOptions.markerSymbol.getColor();
        markerColorPicker.setValue(Color.rgb((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, ((color >>> 24) & 0xFF) / 255.0));
    }

    @FXML
    public void initialize() {
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
        markerSizeSlider.valueProperty().addListener((observableValue, number, t1) -> markerSizeLabel.setText(String.format("Current Size: %d", t1.intValue())));
    }

    public void onMarkerStyleApply(ActionEvent actionEvent) {
        appController.drawingOptions.markerSymbol.setSize((float) markerSizeSlider.getValue());
        appController.drawingOptions.markerSymbol.setStyle(markerStyleChoiceBox.getValue());
        Color newColor = markerColorPicker.getValue();
        int r = ((int) newColor.getRed() * 255) << 16;
        int g = ((int) newColor.getGreen() * 255) << 8;
        int b = ((int) newColor.getBlue() * 255);
        int a = ((int) newColor.getOpacity() * 255) << 24;
        appController.drawingOptions.markerSymbol.setColor(a | r | g | b);
    }

}
