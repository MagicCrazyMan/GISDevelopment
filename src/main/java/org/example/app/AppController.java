package org.example.app;

import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.internal.util.StringUtil;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.WmsLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

    public void loadOnlineData(@NotNull MapView parentMapView, @NotNull String url, AppView.OnlineDataType type) {
        if (StringUtil.isNullOrEmpty(url)) {
            return;
        }

        switch (type) {
            case WMS_SERVICE: {
                List<String> names = Collections.singletonList("1");
                WmsLayer wmsLayer = new WmsLayer(url, names);
                wmsLayer.addDoneLoadingListener(() -> {
                    if (wmsLayer.getLoadStatus() == LoadStatus.LOADED) {
                        parentMapView.setViewpointGeometryAsync(wmsLayer.getFullExtent());
                    } else if (wmsLayer.getLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load WMS layer");
                        alert.setContentText(wmsLayer.getLoadError().getMessage());
                        alert.showAndWait();
                    }
                });
                parentMapView.getMap().getOperationalLayers().add(wmsLayer);
                break;
            }
            case ESRI_SERVICE: {
                ServiceFeatureTable featureTable = new ServiceFeatureTable(url);
                FeatureLayer featureLayer = new FeatureLayer(featureTable);
                parentMapView.getMap().getOperationalLayers().add(featureLayer);
                featureLayer.addDoneLoadingListener(() -> {
                    if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
                        featureLayer.setRenderer(getOnlineDataRenderer(featureLayer.getFeatureTable().getGeometryType()));
                        parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent());
                    } else if (featureLayer.getLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load eSRI online data");
                        alert.setContentText(featureLayer.getLoadError().getMessage());
                        alert.showAndWait();
                    }
                });
                break;
            }
        }
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

    SimpleMarkerSymbol callOutMarker = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFFFF00, 10);

    public void showCallOut(@NotNull MapView parentMapView, double longitude, double latitude) {
        Callout callout = parentMapView.getCallout();
        callout.setTitle(String.format("Longitude: %05f, Latitude: %.5f", longitude, latitude));
        callout.setDetail("");
        callout.showCalloutAt(Point.createWithM(longitude, latitude, 0, SpatialReference.create(4326)));
    }

    public void showCallOut(@NotNull MapView parentMapView, @NotNull String longitude, @NotNull String latitude) {
        if (StringUtil.isNullOrEmpty(longitude) || StringUtil.isNullOrEmpty(latitude)) {
            return;
        }

        double lon = Double.parseDouble(longitude);
        double lat = Double.parseDouble(latitude);
        this.showCallOut(parentMapView, lon, lat);
    }

    public void showCallOut(@NotNull MapView parentMapView, @NotNull Point point) {
        this.showCallOut(parentMapView, point.getX(), point.getY());
    }

    private File selectSingleFile(@Nullable Window parentWindow, @NotNull String title, @Nullable FileChooser.ExtensionFilter... extensionFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(extensionFilter);
        fileChooser.setTitle(title);
        return fileChooser.showOpenDialog(parentWindow);
    }
}

