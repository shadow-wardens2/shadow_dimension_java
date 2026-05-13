package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Services.Artworks.ServiceArtworks;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import Controllers.Marketplace.Back.PageHost;
import Services.Artworks.GeminiDescriptionService;
import Entities.Artworks.Evaluation;
import Services.Artworks.ServiceEvaluations;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

public class DetailArtworkController {

    @FXML private HBox adminActions;
    @FXML private Button backButton;
    @FXML private ImageView artworkImage;
    @FXML private Label statusBadge;
    @FXML private Label categoryLabel;
    @FXML private Text titleText;
    @FXML private Text priceText;
    @FXML private Label idLabel;
    @FXML private Label descriptionLabel;
    @FXML private Button btnAnalyzePdfDetail;
    @FXML private VBox aiSummarySection;
    @FXML private Label aiSummaryLabel;
    @FXML private HBox starContainer;
    @FXML private TextArea commentArea;
    @FXML private VBox evaluationsContainer;
    @FXML private VBox evaluationSection;

    private int selectedRating = 1;
    private ServiceEvaluations serviceEvaluations = new ServiceEvaluations();

    private Artworks artwork;
    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private GeminiDescriptionService geminiService = new GeminiDescriptionService();
    private PageHost dashboardContext;
    private boolean isFrontOffice = false;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
        this.isFrontOffice = false;
        if (adminActions != null) adminActions.setVisible(true);
        if (evaluationSection != null) {
            evaluationSection.setVisible(false);
            evaluationSection.setManaged(false);
        }
    }

    public void setIsFrontOffice(boolean isFrontOffice) {
        this.isFrontOffice = isFrontOffice;
        if (adminActions != null) adminActions.setVisible(!isFrontOffice);
        if (evaluationSection != null) {
            evaluationSection.setVisible(isFrontOffice);
            evaluationSection.setManaged(isFrontOffice);
        }
    }

    public void setArtworkData(Artworks artwork) {
        this.artwork = artwork;
        
        idLabel.setText("#" + artwork.getId());
        titleText.setText(artwork.getTitle());
        descriptionLabel.setText(artwork.getDescription());
        
        // Only show AI Summary button for Literature (Category ID 4)
        boolean isLiterature = (artwork.getCategoryID() == 4);
        btnAnalyzePdfDetail.setVisible(isLiterature);
        btnAnalyzePdfDetail.setManaged(isLiterature);

        if (artwork.getAiSummary() != null && !artwork.getAiSummary().isEmpty()) {
            aiSummaryLabel.setText(artwork.getAiSummary());
            aiSummarySection.setVisible(true);
            aiSummarySection.setManaged(true);
        } else {
            aiSummarySection.setVisible(false);
            aiSummarySection.setManaged(false);
        }
        
        priceText.setText(artwork.getPrice() + " DT");
        statusBadge.setText(artwork.getStatus().toUpperCase());
        categoryLabel.setText("CATEGORY ID: " + artwork.getCategoryID());

        // Status styling
        statusBadge.getStyleClass().removeAll("status-available", "status-sold");
        if ("Available".equalsIgnoreCase(artwork.getStatus())) {
            statusBadge.getStyleClass().add("status-available");
        } else {
            statusBadge.getStyleClass().add("status-sold");
        }

        // Image loading
        if (artwork.getImageurl() != null && !artwork.getImageurl().isEmpty()) {
            try {
                String path = artwork.getImageurl();
                File file;
                if (path.startsWith("/uploads/") || path.startsWith("\\uploads\\")) {
                    file = new File(System.getProperty("user.dir") + path);
                } else if (!path.startsWith("http") && !path.contains(":") && !path.startsWith("data:")) {
                    file = new File(System.getProperty("user.dir") + "/uploads/artworks/" + path);
                } else {
                    file = new File(path);
                }

                if (file.exists()) {
                    artworkImage.setImage(new Image(file.toURI().toString()));
                } else {
                    artworkImage.setImage(new Image(path, true));
                }
            } catch (Exception e) {
                System.err.println("Could not load image: " + artwork.getImageurl());
            }
        }

        loadEvaluations();
    }

    private void loadEvaluations() {
        try {
            evaluationsContainer.getChildren().clear();
            List<Evaluation> evals = serviceEvaluations.getByArtwork(artwork.getId());
            
            if (evals.isEmpty()) {
                Label noEchos = new Label("No echoes manifested yet. Be the first to leave a mark.");
                noEchos.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                evaluationsContainer.getChildren().add(noEchos);
            }

            for (Evaluation ev : evals) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 15; -fx-background-radius: 12;");
                
                HBox header = new HBox(10);
                header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                Label stars = new Label("★".repeat(ev.getRating()));
                stars.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 16px;");
                
                Label date = new Label(ev.getDate());
                date.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
                
                header.getChildren().addAll(stars, date);
                
                Label comment = new Label(ev.getComment());
                comment.setWrapText(true);
                comment.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px;");
                
                card.getChildren().addAll(header, comment);
                evaluationsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleRate1() { updateStars(1); }
    @FXML private void handleRate2() { updateStars(2); }
    @FXML private void handleRate3() { updateStars(3); }
    @FXML private void handleRate4() { updateStars(4); }
    @FXML private void handleRate5() { updateStars(5); }

    private void updateStars(int rating) {
        selectedRating = rating;
        for (int i = 0; i < starContainer.getChildren().size(); i++) {
            Button btn = (Button) starContainer.getChildren().get(i);
            if (i < rating) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b5cf6; -fx-font-size: 24px; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #adaaae; -fx-font-size: 24px; -fx-cursor: hand;");
            }
        }
    }

    @FXML
    private void handleSubmitEvaluation(ActionEvent event) {
        String comment = commentArea.getText();
        if (comment == null) comment = "";

        try {
            Evaluation ev = new Evaluation();
            ev.setArtworkId(artwork.getId());
            // In a real app, get current user ID. Defaulting to 1 for now if no session.
            ev.setUserId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);
            ev.setRating(selectedRating);
            ev.setComment(comment);
            
            serviceEvaluations.add(ev);
            commentArea.clear();
            showSuccessAlert("Evaluation Manifested", "Your echo has been recorded in the dimension.");
            loadEvaluations();
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Ritual Failed", "Could not save your evaluation: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        navigateToGallery();
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        if (dashboardContext != null) {
            Object controller = dashboardContext.loadPage("/Artworks/AjouterArtwork.fxml");
            if (controller instanceof AjouterArtworkController) {
                ((AjouterArtworkController) controller).setArtworkData(artwork);
                ((AjouterArtworkController) controller).setDashboardContext(dashboardContext);
            }
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Artwork: " + artwork.getTitle());
        alert.setContentText("Are you sure you want to permanentely delete this artwork?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    serviceArtworks.delete(artwork);
                    navigateToGallery();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleAnalyzePdf(ActionEvent event) {
        File selectedFile = resolveArtworkPdfFile();

        // Fallback to FileChooser if not in DB or file doesn't exist locally
        if (selectedFile == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner le PDF du livre pour analyse");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            selectedFile = fileChooser.showOpenDialog(btnAnalyzePdfDetail.getScene().getWindow());
        }
        
        if (selectedFile == null) {
            showErrorAlert("Fichier manquant", "Aucun PDF disponible pour générer le résumé.");
            return;
        }

        File finalFile = selectedFile;
        btnAnalyzePdfDetail.setText("⏳ Résumé en cours...");
        btnAnalyzePdfDetail.setDisable(true);

        new Thread(() -> {
            try {
                // Specialized prompt for summary
                String summaryPrompt = "Lisez le texte suivant extrait d'un livre et générez un résumé clair, structuré et court (10–15 lignes maximum) en français. Utilisez des emojis pertinents.";
                String aiSummary = geminiService.analyzePdfWithPrompt(finalFile, summaryPrompt);
                
                // Update Database
                artwork.setAiSummary(aiSummary);
                serviceArtworks.update(artwork);

                Platform.runLater(() -> {
                    aiSummaryLabel.setText(aiSummary);
                    aiSummarySection.setVisible(true);
                    aiSummarySection.setManaged(true);
                    
                    btnAnalyzePdfDetail.setText("✨ Générer un résumé IA");
                    btnAnalyzePdfDetail.setDisable(false);
                    showSuccessAlert("Résumé Généré", "Le résumé IA a été créé avec succès !");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    btnAnalyzePdfDetail.setText("✨ Générer un résumé IA");
                    btnAnalyzePdfDetail.setDisable(false);
                    showErrorAlert("Erreur d'analyse", "La génération du résumé a échoué : " + e.getMessage());
                });
            }
        }).start();
    }

    private File resolveArtworkPdfFile() {
        if (artwork == null || artwork.getPdfUrl() == null || artwork.getPdfUrl().isBlank()) {
            return null;
        }

        String rawPath = artwork.getPdfUrl().trim().replace("\\", "/");
        String relativePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        Path[] candidates = new Path[] {
                Paths.get(rawPath),
                projectRoot.resolve(relativePath),
                projectRoot.resolve("uploads/pdfs").resolve(relativePath),
                projectRoot.resolve("uploads").resolve(relativePath),
                projectRoot.resolve("target/classes").resolve(relativePath)
        };

        for (Path candidate : candidates) {
            try {
                File file = candidate.normalize().toFile();
                if (file.exists() && file.isFile()) {
                    return file;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private void showSuccessAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show();
        });
    }

    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show();
        });
    }

    private void navigateToGallery() {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Artworks/ListerArtworks.fxml");
        } else if (isFrontOffice) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/ArtworksFront.fxml"));
                artworkImage.getScene().setRoot(loader.load());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (artworkImage.getScene().getWindow() instanceof javafx.stage.Stage) {
             ((javafx.stage.Stage) artworkImage.getScene().getWindow()).close();
        }
    }
}
