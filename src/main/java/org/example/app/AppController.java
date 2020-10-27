package org.example.app;

import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.internal.util.StringUtil;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class AppController {

    public void loadShapefile(@Nullable Window parentWindow, @NotNull MapView parentMapView) {
        File file = selectSingleFile(parentWindow, "Select shapefile", new FileChooser.ExtensionFilter("Shapefile format filed (.shp)", "*.shp"));
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

    public void loadGeoDatabase(@Nullable Window parentWindow, @NotNull MapView parentMapView) {
        File file = selectSingleFile(parentWindow, "Select GeoDatabase", new FileChooser.ExtensionFilter("eSRI GeoDatabase (.geodatabase)", "*.geodatabase"));
        if (Objects.nonNull(file)) {
            Geodatabase geodatabase = new Geodatabase(file.getAbsolutePath());
            geodatabase.loadAsync();
            geodatabase.addDoneLoadingListener(() -> {
                List<GeodatabaseFeatureTable> list = geodatabase.getGeodatabaseFeatureTables();
                for (GeodatabaseFeatureTable geodatabaseFeatureTable : list) {
                    FeatureLayer featureLayer = new FeatureLayer(geodatabaseFeatureTable);
                    parentMapView.getMap().getOperationalLayers().add(featureLayer);
                    featureLayer.addDoneLoadingListener(() -> parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent()));
                }
            });
        }
    }

    public void loadOnlineData(@NotNull MapView parentMapView, @NotNull String url) {
        if (StringUtil.isNullOrEmpty(url)) {
            return;
        }
        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(url);
        FeatureLayer featureLayer = new FeatureLayer(serviceFeatureTable);
        parentMapView.getMap().getOperationalLayers().add(featureLayer);
        featureLayer.addDoneLoadingListener(() -> {
            featureLayer.setRenderer(getOnlineDataRenderer(featureLayer.getFeatureTable().getGeometryType()));
            parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent());
        });
    }

    private SimpleRenderer getOnlineDataRenderer(GeometryType geometryType) {
        switch (geometryType) {
            case POINT:
            case MULTIPOINT: {
                SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0x990000FF, 4);
                return new SimpleRenderer(simpleMarkerSymbol);
            }
            case POLYGON:
            case ENVELOPE: {
                SimpleLineSymbol simpleLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF00FF00, 2);
                SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0x99FF0000, simpleLineSymbol);
                return new SimpleRenderer(simpleFillSymbol);
            }
            case POLYLINE: {
                SimpleLineSymbol simpleLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF00FF00, 2);
                return new SimpleRenderer(simpleLineSymbol);
            }
            default:
                return null;
        }
    }

    private File selectSingleFile(@Nullable Window parentWindow, @NotNull String title, @Nullable FileChooser.ExtensionFilter... extensionFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(extensionFilter);
        fileChooser.setTitle(title);
        return fileChooser.showOpenDialog(parentWindow);
    }
}

