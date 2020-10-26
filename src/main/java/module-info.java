module GISDevelopment {
    requires javafx.graphics;
    requires javafx.controls;
    requires com.esri.arcgisruntime;
    exports org.example.app to javafx.fxml, javafx.controls, javafx.graphics;
}