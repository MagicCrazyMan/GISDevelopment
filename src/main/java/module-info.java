module GISDevelopment {
    requires javafx.graphics;
    requires javafx.controls;
    requires com.esri.arcgisruntime;
    requires org.jetbrains.annotations;
    exports org.example.app to javafx.fxml, javafx.controls, javafx.graphics;
}