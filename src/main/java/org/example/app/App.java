package org.example.app;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
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
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(App.class.getResource("App.fxml")));
            Pane pane = fxmlLoader.load();
            AppController appController = fxmlLoader.getController();
            appController.setPrimaryStage(primaryStage);
            primaryStage.setScene(new Scene(pane));
            primaryStage.setTitle("Map Window");
            primaryStage.setOnShown(windowEvent -> {
                primaryStage.setMinHeight(primaryStage.getHeight());
                primaryStage.setMinWidth(primaryStage.getWidth());
            });
            primaryStage.setOnCloseRequest(windowEvent -> appController.dispose());
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
