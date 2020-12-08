package org.example.app;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class QueryByAttributeController extends AController {

    public ChoiceBox<FeatureLayer> featureLayersChoiceBox;
    public ListView<Field> featureFieldsListView;
    public ListView<Object> featureUniqueAttributesListView;
    public TextArea sqlStatementText;
    public Button fetchUniqueValueButton;
    public Label featureLayerText;
    private AppController appController;
    private CommonController commonController = new CommonController();

    public AppController getAppController() {
        return appController;
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
        for (Layer layer : appController.mainMap.getOperationalLayers()) {
            if (layer instanceof FeatureLayer) {
                featureLayersChoiceBox.getItems().add((FeatureLayer) layer);
            }
        }
        featureLayersChoiceBox.getSelectionModel().selectFirst();
    }

    @FXML
    public void initialize() {
        featureFieldsListView.setCellFactory(param -> new SelectableFieldCell());
        featureFieldsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        featureFieldsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            fetchUniqueValueButton.setDisable(false);
            featureUniqueAttributesListView.getItems().clear();
            featureLayerText.setText(String.format("SELECT * FROM \"%s\" WHERE:", newValue.getName()));
        });
        featureFieldsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                insertSqlStatement(String.format("\"%s\"", featureFieldsListView.getSelectionModel().getSelectedItem().getName()));
            }
        });
        featureUniqueAttributesListView.setCellFactory(param -> new SelectableAttributeCell());
        featureUniqueAttributesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        featureUniqueAttributesListView.getItems().addListener((ListChangeListener<Object>) c -> featureUniqueAttributesListView.setDisable(c.getList().isEmpty()));
        featureUniqueAttributesListView.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                Object attribute = featureUniqueAttributesListView.getSelectionModel().getSelectedItem();
                insertSqlStatement((attribute instanceof Number ? attribute.toString() : String.format("'%s'", attribute.toString())));
            }
        });

        featureLayersChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FeatureLayer object) {
                return object.getName();
            }

            @Override
            public FeatureLayer fromString(String string) {
                for (Layer layer : appController.mainMap.getOperationalLayers()) {
                    if (layer instanceof FeatureLayer && layer.getName().equals(string)) {
                        return (FeatureLayer) layer;
                    }
                }
                return null;
            }
        });
        featureLayersChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            fetchUniqueValueButton.setDisable(false);
            featureFieldsListView.getItems().clear();
            featureUniqueAttributesListView.getItems().clear();
            featureFieldsListView.getItems().addAll(newValue.getFeatureTable().getFields());

            featureFieldsListView.getSelectionModel().selectFirst();
        });
    }

    public Set<Object> fetchUniqueValue(FeatureLayer featureLayer, Field field) {
        Set<Object> values = new HashSet<>();
        if (Objects.isNull(featureLayer) || Objects.isNull(field)) {
            return values;
        }

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setWhereClause("1=1");
        ListenableFuture<FeatureQueryResult> future = featureLayer.getFeatureTable().queryFeaturesAsync(queryParameters);
        try {
            for (Feature feature : future.get()) {
                values.add(feature.getAttributes().get(field.getName()));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return values;
    }

    public void insertSqlStatement(String insertion) {
        if (sqlStatementText.getCaretPosition() == 0) {
            sqlStatementText.setText(sqlStatementText.getText() + insertion + " ");
        } else {
            sqlStatementText.insertText(sqlStatementText.getCaretPosition(), insertion + " ");
        }
    }

    public void onSqlEquals(ActionEvent actionEvent) {
        insertSqlStatement("=");
    }

    public void onSqlNotEquals(ActionEvent actionEvent) {
        insertSqlStatement("<>");
    }

    public void onSqlLike(ActionEvent actionEvent) {
        insertSqlStatement("LIKE");
    }

    public void onSqlAnd(ActionEvent actionEvent) {
        insertSqlStatement("AND");
    }

    public void onSqlLessAndEquals(ActionEvent actionEvent) {
        insertSqlStatement("<=");
    }

    public void onSqlLess(ActionEvent actionEvent) {
        insertSqlStatement("<");
    }

    public void onSqlLarger(ActionEvent actionEvent) {
        insertSqlStatement(">");
    }

    public void onSqlLargerAndEquals(ActionEvent actionEvent) {
        insertSqlStatement(">=");
    }

    public void onSqlOr(ActionEvent actionEvent) {
        insertSqlStatement("OR");
    }

    public void onFetchUniqueValue(ActionEvent actionEvent) {
        List<Object> attributes = new ArrayList<>(fetchUniqueValue(featureLayersChoiceBox.getValue(), featureFieldsListView.getSelectionModel().getSelectedItem()));
        attributes.sort((o1, o2) -> {
            if (o1 instanceof Number && o2 instanceof Number) {
                double diff = ((Number) o1).doubleValue() - ((Number) o2).doubleValue();
                if (diff > 0) {
                    return 1;
                } else if (diff < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
            return 0;
        });
        featureUniqueAttributesListView.getItems().clear();
        featureUniqueAttributesListView.getItems().addAll(attributes);
        featureUniqueAttributesListView.getItems();
        fetchUniqueValueButton.setDisable(true);
    }

    public void onSqlApply(ActionEvent actionEvent) {
        if (Objects.isNull(featureLayersChoiceBox.getValue())) {
            return;
        }

        if (sqlStatementText.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No SQL Statement");
            alert.setContentText("SQL Statement is empty");
            alert.showAndWait();
            return;
        }

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.setWhereClause(sqlStatementText.getText());
        ListenableFuture<FeatureQueryResult> future = featureLayersChoiceBox.getValue().selectFeaturesAsync(queryParameters, FeatureLayer.SelectionMode.NEW);
        future.addDoneListener(() -> {
            try {
                commonController.showQueryResult(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public void onCancel(ActionEvent actionEvent) {
        WindowEvent windowEvent = new WindowEvent(parentStage, WindowEvent.WINDOW_CLOSE_REQUEST);
        WindowEvent.fireEvent(parentStage, windowEvent);
        if (!windowEvent.isConsumed()) {
            parentStage.close();
        }
    }

    public void onSqlClear(ActionEvent actionEvent) {
        sqlStatementText.setText("");
    }

    public static class SelectableFieldCell extends ListCell<Field> {
        @Override
        protected void updateItem(Field item, boolean empty) {
            super.updateItem(item, empty);

            if (Objects.isNull(item) || empty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(String.format("\"%s\"", item.getName()));
            }
        }
    }

    public static class SelectableAttributeCell extends ListCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);

            if (Objects.isNull(item) || empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (item instanceof Number) {
                    setText(item.toString());
                } else {
                    setText(String.format("'%s'", item.toString()));
                }
            }
        }
    }
}
