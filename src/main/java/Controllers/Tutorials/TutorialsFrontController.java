package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Lecon;
import Services.Tutorials.FormationAiService;
import Services.Tutorials.ServiceFormation;
import Services.Tutorials.ServiceLecon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TutorialsFrontController implements Initializable {

    @FXML
    private FlowPane formationsContainer;

    @FXML
    private TextField searchBar;

    @FXML
    private Label resultCountLabel;

    @FXML
    private Label lbCalculatedRank;

    @FXML
    private HBox hbAiRecommendations;
    @FXML
    private Label lbOracleFormation;

    private ServiceFormation serviceFormation = new ServiceFormation();
    private List<Formation> allFormations = new ArrayList<>();
    private List<Lecon> allLessons = new ArrayList<>();
    private ServiceLecon serviceLecon = new ServiceLecon();
    private FormationAiService aiService = new FormationAiService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
        loadAiRecommendations();

        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });
    }

    private void loadAiRecommendations() {
        hbAiRecommendations.getChildren().clear();

        // Calculate Rank for UI Header
        Services.Tutorials.ServiceQuizProgress sp = new Services.Tutorials.ServiceQuizProgress();
        Entities.User.User user = Utils.SessionManager.getCurrentUser();
        if (user != null) {
            int count = sp.getCompletedQuizzesCount(user.getId());
            String rank = "NEOPHYTE";
            if (count >= 3)
                rank = "ADEPT";
            if (count >= 10)
                rank = "SHADOW MASTER";
            lbCalculatedRank.setText("CALCULATED RANK: " + rank);
        }

            if (allFormations == null || allFormations.isEmpty()) {
                System.err.println("CRITICAL: No formations were loaded from the database! Check your DB connection.");
            } else {
                System.out.println("Loaded " + allFormations.size() + " formations for matching.");
            }
            
            new Thread(() -> {
            String fullAiResponse = aiService.getPersonalizedRecommendation();
            System.out.println("AI RESPONSE: " + fullAiResponse);
            Platform.runLater(() -> {
                // Split by ||| and handle multiple lines/spaces
                String[] recommendations = fullAiResponse.split("\\|\\|\\|");

                if (recommendations.length > 0) {
                    String firstRec = recommendations[0].trim();
                    if (firstRec.contains("|")) {
                        String oracleChoice = firstRec.split("\\|")[0].replace("TITLE:", "").replace("AI RESPONSE:", "").trim();
                        lbOracleFormation.setText(oracleChoice.replace("[", "").replace("]", ""));
                    }
                }

                int displayedCount = 0;
                for (String rec : recommendations) {
                    rec = rec.trim();
                    if (displayedCount >= 5 || !rec.contains("|"))
                        continue;

                    String[] parts = rec.split("\\|");
                    if (parts.length < 2) continue;

                    String titlePart = parts[0].replace("TITLE:", "").replace("AI RESPONSE:", "").trim().toLowerCase();
                    // Remove brackets
                    titlePart = titlePart.replace("[", "").replace("]", "").trim();
                    String reasonPart = parts[1].replace("REASON:", "").trim();

                    System.out.println("Trying to match AI recommendation: '" + titlePart + "'");

                    // Find the formation object by title - more flexible matching
                    Formation matched = null;
                    for (Formation f : allFormations) {
                        String fTitle = f.getTitre().toLowerCase();
                        if (fTitle.equals(titlePart) || titlePart.contains(fTitle) || fTitle.contains(titlePart)) {
                            matched = f;
                            System.out.println("Match found: " + f.getTitre());
                            break;
                        }
                    }

                    if (matched != null) {
                        hbAiRecommendations.getChildren().add(createAiRecommendationCard(matched, reasonPart));
                        displayedCount++;
                    } else {
                        System.out.println("No match found for: " + titlePart);
                    }
                }

                // Fallback if no matches found
                if (hbAiRecommendations.getChildren().isEmpty()) {
                    System.out.println("No AI matches found, showing fallback.");
                    for (int i = 0; i < Math.min(5, allFormations.size()); i++) {
                        hbAiRecommendations.getChildren().add(createAiRecommendationCard(allFormations.get(i),
                                "The shadows hide many secrets... explore this path."));
                    }
                }
            });
        }).start();
    }

    private VBox createAiRecommendationCard(Formation f, String aiReason) {
        VBox card = new VBox(20);
        card.getStyleClass().add("panel-card");
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setMaxHeight(350);
        card.setStyle(
                "-fx-background-color: #0d0d12; -fx-background-radius: 25; -fx-padding: 30; -fx-border-color: rgba(139, 92, 246, 0.1); -fx-border-radius: 25; -fx-border-width: 1;");
        card.setAlignment(Pos.TOP_LEFT);

        Label rank = new Label("FOR BEGGINERS");
        rank.setStyle(
                "-fx-background-color: rgba(139, 92, 246, 0.1); -fx-text-fill: #a78bfa; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 8px; -fx-font-weight: bold; -fx-letter-spacing: 1;");

        String gameStr = f.getTitre().contains(":") ? f.getTitre().split(":")[0] : f.getTitre().split(" ")[0];
        Label game = new Label(gameStr.toUpperCase());
        game.setStyle("-fx-text-fill: #666; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label title = new Label(f.getTitre().toUpperCase());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Inter';");
        title.setWrapText(true);
        title.setPrefHeight(60);

        Label desc = new Label(aiReason);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");
        desc.setPrefHeight(80);
        desc.setOpacity(0.8);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnEnter = new Button("ENTER VOID");
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setStyle(
                "-fx-background-color: #1a1a24; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnEnter.setOnAction(e -> handleEnterFormation(f));

        card.getChildren().addAll(rank, game, title, desc, spacer, btnEnter);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-border-color: #6d28d9; -fx-border-width: 2;"));
        card.setOnMouseExited(
                e -> card.setStyle(card.getStyle().replace("-fx-border-color: #6d28d9; -fx-border-width: 2;",
                        "-fx-border-color: rgba(139, 92, 246, 0.1); -fx-border-width: 1;")));

        return card;
    }

    private void loadData() {
        try {
            allFormations = serviceFormation.getAll();
            allLessons = serviceLecon.getAll();
            displayFormations(allFormations);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayFormations(List<Formation> formations) {
        formationsContainer.getChildren().clear();
        for (Formation f : formations) {
            formationsContainer.getChildren().add(createFormationCard(f));
        }
        resultCountLabel.setText(formations.size() + " FORMATIONS FOUND");
    }

    private VBox createFormationCard(Formation f) {
        VBox card = new VBox(0);
        card.getStyleClass().add("artifact-card");
        card.setPrefWidth(310);
        card.setMinWidth(310);
        card.setMaxWidth(310);
        card.setPadding(Insets.EMPTY);
        card.setStyle(
                "-fx-background-color: #0d0d12; -fx-background-radius: 20; -fx-background-radius: 20; -fx-border-radius: 20;");

        // Image Section with Badge
        StackPane imgStack = new StackPane();
        ImageView iv = new ImageView();
        iv.setFitHeight(180);
        iv.setFitWidth(310);
        iv.setPreserveRatio(false);

        try {
            if (f.getImage() != null && !f.getImage().isEmpty()) {
                String imgPath = f.getImage();
                if (!imgPath.startsWith("http") && !imgPath.startsWith("/"))
                    imgPath = "/" + imgPath;
                URL res = getClass().getResource(imgPath);
                iv.setImage(new Image(res != null ? res.toExternalForm() : imgPath, true));
            }
        } catch (Exception e) {
        }

        Rectangle clip = new Rectangle(310, 180);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        iv.setClip(clip);

        Label badge = new Label(f.getNiveau().toUpperCase());
        badge.setStyle(
                "-fx-background-color: rgba(139, 92, 246, 0.4); -fx-text-fill: #d3bbff; -fx-padding: 4 12; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(15));

        imgStack.getChildren().addAll(iv, badge);

        // Content Section
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Meta Row (Game name + Tags)
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Simplified game name extraction
        String gName = f.getTitre().contains(":") ? f.getTitre().split(":")[0] : f.getTitre().split(" ")[0];
        Label gameName = new Label(gName.toUpperCase());
        gameName.setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1;");

        long videoCount = allLessons.stream().filter(l -> l.getFormation() != null
                && l.getFormation().getId() == f.getId() && l.getVideoUrl() != null && !l.getVideoUrl().isEmpty())
                .count();
        long docCount = allLessons.stream().filter(l -> l.getFormation() != null
                && l.getFormation().getId() == f.getId() && l.getDocumentUrl() != null && !l.getDocumentUrl().isEmpty())
                .count();

        HBox tags = new HBox(5);
        if (videoCount > 0) {
            Label vTag = new Label("🎬 " + videoCount + " VIDEOS");
            vTag.setStyle(
                    "-fx-background-color: #1c1922; -fx-text-fill: #ff9e9e; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 9px;");
            tags.getChildren().add(vTag);
        }
        if (docCount > 0) {
            Label dTag = new Label("📄 " + docCount + " PDF");
            dTag.setStyle(
                    "-fx-background-color: #1c1922; -fx-text-fill: #ff9e9e; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 9px;");
            tags.getChildren().add(dTag);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        metaRow.getChildren().addAll(gameName, spacer, tags);

        Label title = new Label(f.getTitre().toUpperCase());
        title.setStyle(
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 17px; -fx-font-family: 'Inter', 'Arial';");
        title.setWrapText(true);

        content.getChildren().addAll(metaRow, title);
        card.getChildren().addAll(imgStack, content);

        card.setOnMouseClicked(e -> handleEnterFormation(f));
        card.setCursor(javafx.scene.Cursor.HAND);

        return card;
    }

    private String currentCategoryFilter = "ALL";

    private void applyFilters() {
        String query = searchBar.getText() != null ? searchBar.getText().toLowerCase() : "";
        List<Formation> filtered = allFormations.stream()
                .filter(f -> {
                    boolean matchesSearch = query.isEmpty() || 
                            f.getTitre().toLowerCase().contains(query) || 
                            f.getDescription().toLowerCase().contains(query) ||
                            (f.getJeu() != null && f.getJeu().getNom().toLowerCase().contains(query));
                            
                    boolean matchesCategory = "ALL".equalsIgnoreCase(currentCategoryFilter) ||
                            (f.getJeu() != null && f.getJeu().getGenre() != null && f.getJeu().getGenre().equalsIgnoreCase(currentCategoryFilter));
                            
                    return matchesSearch && matchesCategory;
                })
                .collect(Collectors.toList());
        displayFormations(filtered);
    }

    @FXML
    void handleCategoryFilter(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        currentCategoryFilter = btn.getText();
        
        FlowPane parent = (FlowPane) btn.getParent();
        for (javafx.scene.Node node : parent.getChildren()) {
            if (node instanceof Button) {
                Button b = (Button) node;
                if (b == btn) {
                    b.getStyleClass().remove("secondary-button");
                    if (!b.getStyleClass().contains("glow-button")) {
                        b.getStyleClass().add("glow-button");
                    }
                    b.setStyle("-fx-border-radius: 20; -fx-background-radius: 20; -fx-font-size: 12px; -fx-padding: 6 20;");
                } else {
                    b.getStyleClass().remove("glow-button");
                    if (!b.getStyleClass().contains("secondary-button")) {
                        b.getStyleClass().add("secondary-button");
                    }
                    b.setStyle("-fx-background-color: #1c1922; -fx-border-color: transparent; -fx-font-size: 12px; -fx-padding: 6 20;");
                }
            }
        }
        applyFilters();
    }

    @FXML
    void handleBack() {
        loadPage("/HomeFront.fxml");
    }

    private void handleEnterFormation(Formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/FormationDetails.fxml"));
            Parent root = loader.load();

            FormationDetailsController controller = loader.getController();
            controller.setFormation(f);

            formationsContainer.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleAiRecommendation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/FormationRecommendations.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            formationsContainer.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
