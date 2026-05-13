package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.GeminiDescriptionService;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import Controllers.Marketplace.Back.PageHost;
import Entities.Artworks.PriceAnalysis;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterArtworkController implements Initializable {

    @FXML private Text      titleText;
    @FXML private TextField txtTitle;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtPrice;
    @FXML private TextField txtImage;
    @FXML private TextField txtPdfUrl;
    @FXML private ComboBox<String>     comboStatus;
    @FXML private ComboBox<Categories> comboCategory;
    @FXML private Button    btnSave;
    @FXML private Button    btnGenerateDesc;
    @FXML private Button    btnGeneratePrice;
    @FXML private Button    btnAnalyzePdf;
    @FXML private Label     labelPdf;
    @FXML private HBox panePdf;

    private final ServiceArtworks        serviceArtworks   = new ServiceArtworks();
    private final ServiceCategories      serviceCategories = new ServiceCategories();
    private final GeminiDescriptionService geminiService   = new GeminiDescriptionService();

    private Artworks  currentArtwork   = null;
    private PageHost  dashboardContext;

    public void setDashboardContext(PageHost ctx) {
        this.dashboardContext = ctx;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        comboStatus.getItems().addAll("Available", "Sold", "In Exhibition");
        try {
            comboCategory.getItems().addAll(serviceCategories.getAll());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Categories Error", "Failed to load categories: " + e.getMessage());
        }

        // Setup visibility listener for PDF elements
        updatePdfVisibility(null);
        comboCategory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updatePdfVisibility(newVal);
        });
    }

    private void updatePdfVisibility(Categories category) {
        boolean isLiterature = category != null && "4".equals(category.getID());
        labelPdf.setVisible(isLiterature);
        labelPdf.setManaged(isLiterature);
        panePdf.setVisible(isLiterature);
        panePdf.setManaged(isLiterature);
        btnAnalyzePdf.setVisible(isLiterature);
        btnAnalyzePdf.setManaged(isLiterature);
    }

    public void setArtworkData(Artworks a) {
        this.currentArtwork = a;
        titleText.setText("EDIT ARTWORK");
        btnSave.setText("Update");

        txtTitle.setText(a.getTitle());
        txtDescription.setText(a.getDescription());
        txtPrice.setText(String.valueOf(a.getPrice()));
        txtImage.setText(a.getImageurl());
        txtPdfUrl.setText(a.getPdfUrl());
        comboStatus.setValue(a.getStatus());
        for (Categories c : comboCategory.getItems()) {
            if (c.getID() != null && c.getID().equals(String.valueOf(a.getCategoryID()))) {
                comboCategory.setValue(c);
                break;
            }
        }
    }

    // ---------------------------------------------------------------
    //  AI Description Generation
    // ---------------------------------------------------------------

    @FXML
    private void handleGenerateDescription(ActionEvent event) {
        String imageUrl = txtImage.getText() == null ? "" : txtImage.getText().trim();

        if (imageUrl.isEmpty()) {
            showAlert("Données manquantes",
                      "Veuillez saisir un titre ou une URL d'image pour générer une description.");
            return;
        }

        if (!imageUrl.toLowerCase().startsWith("http://") &&
            !imageUrl.toLowerCase().startsWith("https://") &&
            !imageUrl.startsWith("data:")) {
            showAlert("URL invalide",
                      "La description IA doit analyser une image. Utilisez une URL directe qui commence par http:// ou https://.");
            return;
        }

        // Visual feedback: disable button + show loading text
        btnGenerateDesc.setDisable(true);
        btnGenerateDesc.setText("⏳ Génération en cours...");
        txtDescription.setText("L'IA analyse vos données, veuillez patienter…");

        // Run API call on a background thread to keep the UI responsive
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return geminiService.generateVisualDescriptionFromUrl(imageUrl);
            }
        };

        task.setOnSucceeded(e -> {
            String description = task.getValue();
            txtDescription.setText(description);
            resetGenerateButton();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            txtDescription.clear();
            resetGenerateButton();
            showAlert("Erreur Gemini AI",
                      "La génération de description a échoué :\n" +
                      (ex != null ? ex.getMessage() : "Erreur inconnue"));
            ex.printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void resetGenerateButton() {
        Platform.runLater(() -> {
            btnGenerateDesc.setDisable(false);
            btnGenerateDesc.setText("✨ Générer Description IA");
        });
    }

    // ---------------------------------------------------------------
    //  AI Price Generation
    // ---------------------------------------------------------------

    @FXML
    private void handleGeneratePrice(ActionEvent event) {
        String imageUrl = txtImage.getText() == null ? "" : txtImage.getText().trim();

        if (imageUrl.isEmpty()) {
            showAlert("Image URL requise",
                      "Veuillez d'abord saisir l'URL de l'image avant de générer un prix.");
            return;
        }
        if (!imageUrl.toLowerCase().startsWith("http://") &&
            !imageUrl.toLowerCase().startsWith("https://")) {
            showAlert("URL invalide", "L'URL de l'image doit commencer par http:// ou https://");
            return;
        }

        String description = txtDescription.getText() == null ? "" : txtDescription.getText().trim();

        btnGeneratePrice.setDisable(true);
        btnGeneratePrice.setText("⏳ Analyse en cours...");

        Task<PriceAnalysis> task = new Task<>() {
            @Override
            protected PriceAnalysis call() throws Exception {
                return geminiService.generatePrice(imageUrl, description);
            }
        };

        task.setOnSucceeded(e -> {
            PriceAnalysis analysis = task.getValue();
            resetGeneratePriceButton();
            // Show detailed dialog on JavaFX thread
            Stage owner = (Stage) btnGeneratePrice.getScene().getWindow();
            PriceAnalysisDialog dialog = new PriceAnalysisDialog(analysis);
            Integer chosenPrice = dialog.showAndWait(owner);
            if (chosenPrice != null) {
                txtPrice.setText(String.valueOf(chosenPrice));
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            resetGeneratePriceButton();
            showAlert("Erreur Gemini AI",
                      "La génération de prix a échoué :\n" +
                      (ex != null ? ex.getMessage() : "Erreur inconnue"));
            ex.printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void resetGeneratePriceButton() {
        Platform.runLater(() -> {
            btnGeneratePrice.setDisable(false);
            btnGeneratePrice.setText("💰 Générer Prix IA");
        });
    }

    // ---------------------------------------------------------------
    //  Save / Cancel
    // ---------------------------------------------------------------

    @FXML
    private void handleSave(ActionEvent event) {
        if (txtTitle.getText().isEmpty() || txtPrice.getText().isEmpty()
                || txtImage.getText().isEmpty() || comboCategory.getValue() == null) {
            showAlert("Validation Error", "Title, Price, Image URL, and Category are required.");
            return;
        }

        if (txtDescription.getText() == null || txtDescription.getText().trim().length() < 15) {
            showAlert("Validation Error", "The description must be at least 15 characters long.");
            return;
        }

        String imageUrl = txtImage.getText().trim();
        if (!imageUrl.toLowerCase().startsWith("http://") && !imageUrl.toLowerCase().startsWith("https://")) {
            showAlert("Validation Error", "Image URL must be a valid link starting with http:// or https://");
            return;
        }

        try {
            int price      = Integer.parseInt(txtPrice.getText());
            int categoryId = Integer.parseInt(comboCategory.getValue().getID());

            String pdf = txtPdfUrl.getText();
            String finalPdfPath = pdf;
            if (pdf != null && !pdf.isEmpty() && !pdf.startsWith("/uploads/")) {
                File sourceFile = new File(pdf);
                if (sourceFile.exists()) {
                    try {
                        String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
                        Path targetDir = Paths.get("uploads/pdfs");
                        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
                        Path targetPath = targetDir.resolve(fileName);
                        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                        finalPdfPath = "/uploads/pdfs/" + fileName;
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlert("File Error", "Failed to copy PDF: " + e.getMessage());
                    }
                }
            }

            if (currentArtwork == null) {
                Artworks a = new Artworks();
                a.setTitle(txtTitle.getText());
                a.setDescription(txtDescription.getText());
                a.setPrice(price);
                a.setImageurl(txtImage.getText());
                a.setPdfUrl(finalPdfPath);
                a.setStatus(comboStatus.getValue());
                a.setCategoryID(categoryId);
                serviceArtworks.add(a);
            } else {
                currentArtwork.setTitle(txtTitle.getText());
                currentArtwork.setDescription(txtDescription.getText());
                currentArtwork.setPrice(price);
                currentArtwork.setImageurl(txtImage.getText());
                currentArtwork.setPdfUrl(finalPdfPath);
                currentArtwork.setStatus(comboStatus.getValue());
                currentArtwork.setCategoryID(categoryId);
                serviceArtworks.update(currentArtwork);
            }

            closeWindow();
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Price must be a valid number.");
        } catch (SQLException e) {
            showAlert("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/ListerArtworks.fxml");
        } else {
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void handleAnalyzePdf(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le PDF du livre");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File selectedFile = fileChooser.showOpenDialog(((Button)event.getSource()).getScene().getWindow());
        
        if (selectedFile == null) return;

        txtPdfUrl.setText(selectedFile.getAbsolutePath());
        btnAnalyzePdf.setText("🔍 Analyse du PDF...");
        btnAnalyzePdf.setDisable(true);

        new Thread(() -> {
            try {
                String description = geminiService.analyzePdf(selectedFile);
                Platform.runLater(() -> {
                    txtDescription.setText(description);
                    btnAnalyzePdf.setText("📂 Analyser PDF Livre");
                    btnAnalyzePdf.setDisable(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Analyse échouée", "Impossible d'analyser le PDF : " + e.getMessage());
                    btnAnalyzePdf.setText("📂 Analyser PDF Livre");
                    btnAnalyzePdf.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleBrowsePdf(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le fichier PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File selectedFile = fileChooser.showOpenDialog(txtPdfUrl.getScene().getWindow());
        if (selectedFile != null) {
            txtPdfUrl.setText(selectedFile.getAbsolutePath());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
