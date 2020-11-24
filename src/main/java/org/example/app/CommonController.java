package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.internal.util.StringUtil;
import com.esri.arcgisruntime.layers.FeatureCollectionLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.layers.WmsLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.symbology.*;
import com.esri.arcgisruntime.util.ListenableList;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CommonController {
    public void routeLoadFile(@NotNull MapView parentMapView, @NotNull File file) {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".shp")) {
            loadShapefile(parentMapView, file);
        } else if (filename.endsWith(".geodatabase")) {
            loadGeoDatabase(parentMapView, file);
        } else if (filename.endsWith(".tif") || filename.endsWith(".tiff")) {
            loadRaster(parentMapView, file);
        }
    }

    public void loadShapefile(@NotNull MapView parentMapView, @NotNull File file) {
        ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(file.getAbsolutePath());
        FeatureLayer featureLayer = new FeatureLayer(shapefileFeatureTable);
        featureLayer.setName(file.getName());
        featureLayer.setId(file.getAbsolutePath());
        // ATTENTION! setViewpoint by shapefile extent may has no effect when shapefile haven't done loading
        // difference from C# SDK, ShapefileFeatureTable doesn't have async method to load
        // so, in Java, we have to add a DoneLoadingListener to capture the done loading event, and set viewpoint after finishing loading
        featureLayer.addDoneLoadingListener(() -> parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent(), 50));
        parentMapView.getMap().getOperationalLayers().add(featureLayer);
    }

    public void loadShapefile(@Nullable Window parentWindow, @NotNull MapView parentMapView) {
        File file = selectSingleFile(parentWindow, "Select shapefile", new FileChooser.ExtensionFilter("Shapefile format filed (.shp)", "*.shp"));

        if (Objects.nonNull(file)) {
            loadShapefile(parentMapView, file);
        }
    }

    public void loadGeoDatabase(@NotNull MapView parentMapView, @NotNull File file) {
        Geodatabase geodatabase = new Geodatabase(file.getAbsolutePath());
        geodatabase.loadAsync();
        geodatabase.addDoneLoadingListener(() -> {
            List<GeodatabaseFeatureTable> list = geodatabase.getGeodatabaseFeatureTables();
            for (GeodatabaseFeatureTable geodatabaseFeatureTable : list) {
                FeatureLayer featureLayer = new FeatureLayer(geodatabaseFeatureTable);
                featureLayer.setName(geodatabaseFeatureTable.getDisplayName());
                featureLayer.setId(geodatabaseFeatureTable.getTableName());
                parentMapView.getMap().getOperationalLayers().add(featureLayer);
                featureLayer.addDoneLoadingListener(() -> parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent(), 50));
            }
        });
    }

    public void loadGeoDatabase(@Nullable Window parentWindow, @NotNull MapView parentMapView) {
        File file = selectSingleFile(parentWindow, "Select GeoDatabase", new FileChooser.ExtensionFilter("eSRI GeoDatabase (.geodatabase)", "*.geodatabase"));
        if (Objects.nonNull(file)) {
            loadGeoDatabase(parentMapView, file);
        }
    }

    public void loadRaster(@NotNull MapView parentMapView, @NotNull File file) {
        Raster raster = new Raster(file.getAbsolutePath());
        RasterLayer rasterLayer = new RasterLayer(raster);
        rasterLayer.setName(file.getName());
        rasterLayer.setId(file.getAbsolutePath());
        parentMapView.getMap().getOperationalLayers().add(rasterLayer);
        rasterLayer.addDoneLoadingListener(() -> {
            if (rasterLayer.getLoadStatus() == LoadStatus.LOADED) {
                parentMapView.setViewpointGeometryAsync(rasterLayer.getFullExtent(), 50);
            }
        });
    }

    public void loadRaster(@Nullable Window parentWindow, @NotNull MapView parentMapView) {
        File file = selectSingleFile(parentWindow, "Select Raster", new FileChooser.ExtensionFilter("GeoTiff format file", "*.tif", "*.tiff"));
        if (Objects.nonNull(file)) {
            loadRaster(parentMapView, file);
        }
    }

    public void loadOnlineData(@NotNull MapView parentMapView, @NotNull String url, AppController.OnlineDataType type) {
        if (StringUtil.isNullOrEmpty(url)) {
            return;
        }

        switch (type) {
            case WMS_SERVICE: {
                List<String> names = Collections.singletonList("1");
                WmsLayer wmsLayer = new WmsLayer(url, names);
                wmsLayer.addDoneLoadingListener(() -> {
                    if (wmsLayer.getLoadStatus() == LoadStatus.LOADED) {
                        parentMapView.setViewpointGeometryAsync(wmsLayer.getFullExtent(), 50);
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
                featureLayer.setName(featureTable.getDisplayName());
                featureLayer.setId(featureTable.getTableName());
                parentMapView.getMap().getOperationalLayers().add(featureLayer);
                featureLayer.addDoneLoadingListener(() -> {
                    if (featureLayer.getLoadStatus() == LoadStatus.LOADED) {
                        featureLayer.setMaxScale(0);
                        featureLayer.setMinScale(Double.MAX_VALUE);
                        featureLayer.setRenderer(getOnlineDataRenderer(featureLayer.getFeatureTable().getGeometryType()));
                        parentMapView.setViewpointGeometryAsync(featureLayer.getFullExtent(), 50);
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
        point = (Point) GeometryEngine.project(point, SpatialReference.create(4326));
        this.showCallOut(parentMapView, point.getX(), point.getY());
    }

    public void simpleQuery(@NotNull MapView parentMapView, @NotNull FeatureLayer featureLayer, @NotNull QueryParameters queryParameters) {
        ListenableFuture<FeatureQueryResult> results = featureLayer.selectFeaturesAsync(queryParameters, FeatureLayer.SelectionMode.NEW);
        results.addDoneListener(() -> {
            try {
                EnvelopeBuilder envelopeBuilder = new EnvelopeBuilder(featureLayer.getSpatialReference());
                results.get().iterator().forEachRemaining(feature -> envelopeBuilder.unionOf(feature.getGeometry().getExtent()));
                parentMapView.setViewpointGeometryAsync(envelopeBuilder.toGeometry(), 50);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public void clickQuery(@NotNull MapView parentView, @NotNull FeatureLayer featureLayer, @NotNull Point point) {
        final double error = 3; // set error
        double mapError = error * parentView.getUnitsPerDensityIndependentPixel(); // convert error from pixels to specified units
        if (parentView.isWrapAroundEnabled()) { // if map is warp around, geometry should be normalized firstly
            point = (Point) GeometryEngine.normalizeCentralMeridian(point);
        }

        // construct search envelop & search parameters
        Envelope envelope = new Envelope(point.getX() - mapError, point.getY() - mapError, point.getX() + mapError, point.getY() + mapError, parentView.getSpatialReference());
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setGeometry(envelope);
        queryParameters.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);

        ListenableFuture<FeatureQueryResult> results = featureLayer.selectFeaturesAsync(queryParameters, FeatureLayer.SelectionMode.NEW);
        results.addDoneListener(() -> {
            try {
                EnvelopeBuilder envelopeBuilder = new EnvelopeBuilder(results.get().getSpatialReference());
                results.get().iterator().forEachRemaining(feature -> {
                    envelopeBuilder.unionOf(feature.getGeometry().getExtent());
                    // iterate all features and get all fields and its values
                    clickQueryFeaturesProcess(featureLayer, feature.getAttributes());
                });
                parentView.setViewpointGeometryAsync(envelopeBuilder.toGeometry(), 50);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public void clickQuery(@NotNull MapView parentView, @NotNull FeatureLayer featureLayer, @NotNull Point2D point) {
        final double error = 3; // set error
        ListenableFuture<IdentifyLayerResult> results = parentView.identifyLayerAsync(featureLayer, point, error, false);
        results.addDoneListener(() -> {
            try {
                if (!results.get().getElements().isEmpty()) {
                    GeoElement geoElement = results.get().getElements().get(0); // only the first element is needed
                    clickQueryFeaturesProcess(featureLayer, geoElement.getAttributes());
                    parentView.setViewpointGeometryAsync(geoElement.getGeometry(), 50);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    private void clickQueryFeaturesProcess(@NotNull FeatureLayer featureLayer, @NotNull Map<String, Object> attributes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            stringBuilder.append(attribute.getKey()).append(": ").append(attribute.getValue()).append(System.lineSeparator());
        }

        // show message box
        if (!stringBuilder.toString().isEmpty()) {
            Alert messageBox = new Alert(Alert.AlertType.INFORMATION);
            messageBox.setTitle(featureLayer.getName() + " fields information");
            messageBox.setContentText(stringBuilder.toString());
            messageBox.setResizable(false);
            messageBox.show();
        }
    }

    private static final GraphicsOverlay temporaryGraphicOverlay = new GraphicsOverlay();

    public void drawTemporaryGeometry(@NotNull MapView parentView, @NotNull AppController.DrawingType drawingType, @NotNull Symbol symbol, @NotNull PointCollection points, boolean clearExisted) {
        if (clearExisted) temporaryGraphicOverlay.getGraphics().clear(); // clear existing graphics
        switch (drawingType) {
            case POLYLINE: {
                if (points.size() >= 2) {
                    Graphic graphic = new Graphic(new Polyline(points), symbol);
                    temporaryGraphicOverlay.getGraphics().add(graphic);
                }
                break;
            }
            case MARKER: {
                if (points.size() >= 1) {
                    for (Point point : points) {
                        Graphic graphic = new Graphic(point, symbol);
                        temporaryGraphicOverlay.getGraphics().add(graphic);
                    }
                }
                break;
            }
            case POLYGON: {
                if (points.size() >= 3) {
                    Graphic graphic = new Graphic(new Polygon(points), symbol);
                    temporaryGraphicOverlay.getGraphics().add(graphic);
                }
                break;
            }
        }


        ListenableList<GraphicsOverlay> graphicsOverlays = parentView.getGraphicsOverlays();
        if (!graphicsOverlays.contains(temporaryGraphicOverlay)) { // if graphic overlay is not included in map view ,add it
            graphicsOverlays.add(temporaryGraphicOverlay);
        }
    }

    public void clearTemporaryGeometry(@NotNull MapView parentView, @NotNull PointCollection points) {
        points.clear();
        if (parentView.getGraphicsOverlays().contains(temporaryGraphicOverlay)) {
            temporaryGraphicOverlay.getGraphics().clear();
        }
    }


    private File lastVisitedDir = null;

    private File selectSingleFile(@Nullable Window parentWindow, @NotNull String title, @Nullable FileChooser.ExtensionFilter... extensionFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(Objects.isNull(lastVisitedDir) ? new File(System.getProperty("user.home")) : lastVisitedDir);
        fileChooser.getExtensionFilters().addAll(extensionFilter);
        fileChooser.setTitle(title);
        File file = fileChooser.showOpenDialog(parentWindow);
        if (Objects.nonNull(file)) {
            lastVisitedDir = file.getParentFile();
        }
        return file;
    }

    public int color2int(Color color) {
        int r = ((int) (color.getRed() * 255)) << 16;
        int g = ((int) (color.getGreen() * 255)) << 8;
        int b = ((int) (color.getBlue() * 255));
        int a = ((int) (color.getOpacity() * 255)) << 24;
        return (a | r | g | b);
    }

    public Color int2color(int color) {
        return Color.rgb(
                (color >>> 16) & 0xFF,
                (color >>> 8) & 0xFF,
                color & 0xFF,
                ((color >>> 24) & 0xFF) / 255.0
        );
    }
}

