package org.example.app;

import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public class AppController {

    public void loadShapefile(@Nullable Window parentWindow, @NotNull MapView parentMapView) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Shapefile format filed (.shp)", "*.shp"));
        fileChooser.setTitle("Select shapefile");
        File file = fileChooser.showOpenDialog(parentWindow);
        if (Objects.nonNull(file)) {
            ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(file.getAbsolutePath());
            FeatureLayer featureLayer = new FeatureLayer(shapefileFeatureTable);
            // ATTENTION! setViewpoint by shapefile extent may has no effect when shapefile haven't done loading
            // difference from C# SDK, ShapefileFeatureTable doesn't have async method to load
            // so, in Java, we have to add a DoneLoadingListener to capture the done loading event, and set viewpoint after finishing loading
            featureLayer.addDoneLoadingListener(() -> parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent()));
            parentMapView.getMap().getOperationalLayers().add(featureLayer);
        }
    }
}

