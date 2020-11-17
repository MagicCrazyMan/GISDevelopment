package org.example.app;

import javafx.stage.Stage;

public abstract class AController {

    Stage parentStage;

    public void setParentStage(Stage stage) {
        parentStage = stage;
    }

    public Stage getParentStage() {
        return parentStage;
    }
}
