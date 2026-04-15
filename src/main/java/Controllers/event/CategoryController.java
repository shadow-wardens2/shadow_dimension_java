package Controllers.event;

import Entities.event.Category;
import Services.event.CategoryService;
import Utils.PdfExportUtil;
import Utils.VoiceToTextUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import javafx.stage.FileChooser;

public class CategoryController implements Initializable {

    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> colId;
    @FXML
    private TableColumn<Category, String> colNom;
    @FXML
    private TableColumn<Category, String> colDescription;
    @FXML
    private TableColumn<Category, String> colTarification;
    @FXML
    private TableColumn<Category, Double> colPrix;
    @FXML
    private TableColumn<Category, Integer> colActions;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Button btnMic;

    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Category> observableCategories = FXCollections.observableArrayList();
    private FilteredList<Category> filteredCategories;
    private SortedList<Category> sortedCategories;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colTarification.setCellValueFactory(new PropertyValueFactory<>("typeTarification"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));

        setupSearchAndSort();
        loadCategories();

        colActions.setCellFactory(param -> new TableCell<Category, Integer>() {
            private final Button btnUpdate = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnUpdate, btnDelete);

            {
                btnUpdate.getStyleClass().add("edit-button");
                btnDelete.getStyleClass().add("delete-button");

                btnDelete.setOnAction(event -> {
                    Category c = getTableView().getItems().get(getIndex());
                    try {
                        categoryService.delete(c);
                        loadCategories();
                        showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie supprimee avec succes.");
                    } catch (SQLException ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });

                btnUpdate.setOnAction(event -> {
                    Category c = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/EditCategory.fxml"));
                        Parent root = loader.load();
                        EditCategoryController controller = loader.getController();
                        controller.setCategory(c);
                        Stage stage = new Stage();
                        stage.setTitle("Edit Category");
                        stage.setScene(new Scene(root));
                        stage.showAndWait();
                        loadCategories();
                    } catch (IOException ex) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupSearchAndSort() {
        filteredCategories = new FilteredList<>(observableCategories, c -> true);
        sortedCategories = new SortedList<>(filteredCategories);
        categoryTable.setItems(sortedCategories);

        cbSort.setItems(FXCollections.observableArrayList(
                "Newest first",
                "Oldest first",
                "Name A-Z",
                "Name Z-A",
                "Price high-low",
                "Price low-high"
        ));
        cbSort.getSelectionModel().select("Newest first");

        tfSearch.textProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());
        cbSort.valueProperty().addListener((obs, oldValue, newValue) -> applyDynamicFilterAndSort());

        applyDynamicFilterAndSort();
    }

    private void applyDynamicFilterAndSort() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);

        filteredCategories.setPredicate(category -> {
            if (search.isEmpty()) {
                return true;
            }

            String name = safe(category.getNom());
            String description = safe(category.getDescription());
            String pricing = safe(category.getTypeTarification());
            String price = category.getPrix() == null ? "" : String.valueOf(category.getPrix());

            return name.contains(search)
                    || description.contains(search)
                    || pricing.contains(search)
                    || price.contains(search);
        });

        String selectedSort = cbSort.getValue();
        Comparator<Category> comparator;

        if ("Oldest first".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Category::getId);
        } else if ("Name A-Z".equals(selectedSort)) {
            comparator = Comparator.comparing(c -> safe(c.getNom()));
        } else if ("Name Z-A".equals(selectedSort)) {
            comparator = Comparator.comparing((Category c) -> safe(c.getNom())).reversed();
        } else if ("Price high-low".equals(selectedSort)) {
            comparator = Comparator.comparing(
                    (Category c) -> c.getPrix() == null ? Double.NEGATIVE_INFINITY : c.getPrix()
            ).reversed();
        } else if ("Price low-high".equals(selectedSort)) {
            comparator = Comparator.comparing(
                    c -> c.getPrix() == null ? Double.POSITIVE_INFINITY : c.getPrix()
            );
        } else {
            comparator = Comparator.comparingInt(Category::getId).reversed();
        }

        sortedCategories.setComparator(comparator);
    }

    @FXML
    private void handleVoiceSearch() {
        btnMic.setDisable(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                return VoiceToTextUtil.recognizeOnce(6);
            } catch (Exception e) {
                return "";
            }
        }).thenAccept(text -> javafx.application.Platform.runLater(() -> {
            btnMic.setDisable(false);
            if (text != null && !text.isBlank()) {
                tfSearch.setText(text);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Microphone", "Aucune voix detectee. Reessayez.");
            }
        }));
    }

    @FXML
    void handleAddCategory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/event/AddCategory.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add New Category");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void handleExportPdf(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Categories PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("categories-report.pdf");

        java.io.File file = fileChooser.showSaveDialog(categoryTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            List<Category> rows = new ArrayList<>(sortedCategories);
            PdfExportUtil.exportCategories(file.getAbsolutePath(), rows);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF exporte avec succes.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter le PDF: " + e.getMessage());
        }
    }

    private void loadCategories() {
        observableCategories.clear();
        try {
            observableCategories.addAll(categoryService.getAll());
            applyDynamicFilterAndSort();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
