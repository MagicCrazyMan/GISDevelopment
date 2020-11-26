module GISDevelopment {
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.esri.arcgisruntime;
    requires org.jetbrains.annotations;
    opens org.example.app to javafx.fxml, javafx.controls, javafx.graphics, javafx.base;
}