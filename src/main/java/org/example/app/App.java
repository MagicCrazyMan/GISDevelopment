package org.example.app;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("linux")) {
            ArcGISRuntimeEnvironment.setInstallDirectory("/home/magiccrayman/Downloads/arcgis-runtime-sdk-java-100.9.0");
        } else if (os.startsWith("windows")) {
            ArcGISRuntimeEnvironment.setInstallDirectory("D:\\Data\\Learning\\ArcGISRuntimesSDK\\arcgis-runtime-sdk-java-100.9.0");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        new AppView().start(primaryStage);
    }
}
