package org.example.app;

import javafx.stage.Stage;

public abstract class AController {

    Stage parentStage;

    public void setParentStage(Stage stage) {
        this.parentStage = stage;
    }

    public Stage getParentStage() {
        return this.parentStage;
    }
}
