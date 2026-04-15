package Controllers.event;

import Controllers.Marketplace.PageHost;
import Entities.event.Category;
import Services.event.CategoryService;
import Utils.EventNavigationState;
import Utils.PdfExportUtil;
import Utils.VoiceToTextUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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

    private PageHost dashboardContext;

    @FXML
    private VBox categoriesContainer;
    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private Button btnMic;

    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Category> masterCategories = FXCollections.observableArrayList();
    private final ObservableList<Category> displayedCategories = FXCollections.observableArrayList();

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchAndSort();
        loadCategories();
    }

    private void setupSearchAndSort() {
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

        displayedCategories.clear();
        for (Category category : masterCategories) {
            if (search.isEmpty()) {
                displayedCategories.add(category);
                continue;
            }

            String name = safeLower(category.getNom());
            String description = safeLower(category.getDescription());
            String pricing = safeLower(category.getTypeTarification());
            String price = category.getPrix() == null ? "" : String.valueOf(category.getPrix());

            boolean match = name.contains(search)
                    || description.contains(search)
                    || pricing.contains(search)
                    || price.contains(search);

            if (match) {
                displayedCategories.add(category);
            }
        }

        String selectedSort = cbSort.getValue();
        Comparator<Category> comparator;

        if ("Oldest first".equals(selectedSort)) {
            comparator = Comparator.comparingInt(Category::getId);
        } else if ("Name A-Z".equals(selectedSort)) {
            comparator = Comparator.comparing(c -> safeLower(c.getNom()));
        } else if ("Name Z-A".equals(selectedSort)) {
            comparator = Comparator.comparing((Category c) -> safeLower(c.getNom())).reversed();
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

        displayedCategories.sort(comparator);
        renderCategories();
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
        EventNavigationState.clearEditingCategory();
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/AddCategory.fxml");
        }
    }

    @FXML
    private void handleGoBack() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EventSelector.fxml");
        }
    }

    @FXML
    void handleExportPdf(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Categories PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("categories-report.pdf");

        java.io.File file = fileChooser.showSaveDialog(tfSearch.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            List<Category> rows = new ArrayList<>(displayedCategories);
            PdfExportUtil.exportCategories(file.getAbsolutePath(), rows);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF exporte avec succes.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter le PDF: " + e.getMessage());
        }
    }

    private void loadCategories() {
        masterCategories.clear();
        try {
            masterCategories.addAll(categoryService.getAll());
            applyDynamicFilterAndSort();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void renderCategories() {
        categoriesContainer.getChildren().clear();
        if (displayedCategories.isEmpty()) {
            Label empty = new Label("No categories found.");
            empty.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 14px;");
            categoriesContainer.getChildren().add(empty);
            return;
        }

        for (Category category : displayedCategories) {
            categoriesContainer.getChildren().add(createCategoryCard(category));
        }
    }

    private HBox createCategoryCard(Category category) {
        Label idLabel = new Label("#" + category.getId());
        idLabel.setStyle("-fx-text-fill: #d6b2fc; -fx-font-weight: 700;");
        idLabel.setMinWidth(56);
        idLabel.setPrefWidth(56);

        Label nameLabel = new Label(truncate(safeDisplay(category.getNom()), 34));
        nameLabel.setStyle("-fx-text-fill: #f3eefc; -fx-font-size: 14px; -fx-font-weight: 700;");
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setMaxWidth(300);

        Label descriptionLabel = new Label(truncate(safeDisplay(category.getDescription()), 58));
        descriptionLabel.setStyle("-fx-text-fill: #9ea3b0; -fx-font-size: 12px;");
        descriptionLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        descriptionLabel.setMaxWidth(380);
        VBox identityBox = new VBox(2, nameLabel, descriptionLabel);
        identityBox.setMinWidth(260);
        identityBox.setPrefWidth(340);
        identityBox.setMaxWidth(380);

        Label pricingLabel = new Label(truncate(safeDisplay(category.getTypeTarification()), 16));
        pricingLabel.setStyle("-fx-text-fill: #c8b3ff; -fx-font-weight: 700;");
        pricingLabel.setMinWidth(120);
        pricingLabel.setPrefWidth(130);
        pricingLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        String priceText = category.getPrix() == null ? "-" : String.format(Locale.US, "%.2f", category.getPrix());
        Label priceLabel = new Label(priceText);
        priceLabel.setStyle("-fx-text-fill: #9ea3b0;");
        priceLabel.setMinWidth(90);
        priceLabel.setPrefWidth(100);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("edit-button");
        btnEdit.setOnAction(actionEvent -> openEditCategory(category));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(actionEvent -> deleteCategory(category));

        HBox actionsBox = new HBox(8, btnEdit, btnDelete);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.setMinWidth(170);
        actionsBox.setPrefWidth(170);
        actionsBox.setMaxWidth(170);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, idLabel, identityBox, pricingLabel, priceLabel, spacer, actionsBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-row-card");
        return row;
    }

    private void openEditCategory(Category category) {
        EventNavigationState.setEditingCategory(category);
        if (dashboardContext != null) {
            dashboardContext.loadPage("/event/EditCategory.fxml");
        }
    }

    private void deleteCategory(Category category) {
        try {
            categoryService.delete(category);
            loadCategories();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Categorie supprimee avec succes.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private String safeDisplay(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String safeLower(String value) {
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
