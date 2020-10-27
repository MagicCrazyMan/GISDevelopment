package org.example.app;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    static {
        ArcGISRuntimeEnvironment.setInstallDirectory("D:\\Data\\Learning\\ArcGISRuntimesSDK\\arcgis-runtime-sdk-java-100.9.0");
    }

    private AppView appView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        appView = new AppView();
        appView.start(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (Objects.nonNull(appView)) {
            appView.dispose();
        }
    }
}
