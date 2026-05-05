package Controllers.Artworks;

import Entities.Artworks.Artworks;
import Entities.Artworks.Categories;
import Services.Artworks.ServiceArtworks;
import Services.Artworks.ServiceCategories;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import Services.Artworks.GeminiDescriptionService;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.util.Duration;
import java.util.Random;

public class ArtworksFrontController implements Initializable {

    @FXML private AnchorPane rootNode;
    @FXML private VBox categorySectionsContainer;
    @FXML private Label itemCountLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private TextFlow curatorMessageFlow;
    
    @FXML private Button btnFantasy;
    @FXML private Button btnAction;
    @FXML private Button btnLove;
    @FXML private Button btnMystic;
    @FXML private Button btnDark;
    @FXML private Button btnCyberpunk;
    @FXML private Button mascotTriggerBtn;
    
    @FXML private VBox mascotBubble;
    @FXML private TextField mascotInput;
    @FXML private VBox aiSuggestionsContainer;
    @FXML private FlowPane suggestionsGrid;
    
    // Ghost Mascot Components
    @FXML private Group ghostGroup;
    @FXML private Region leftPupil;
    @FXML private Region rightPupil;
    @FXML private StackPane leftEye;
    @FXML private StackPane rightEye;
    @FXML private Region leftBlush;
    @FXML private Region rightBlush;

    private Timeline blinkTimeline;
    
    private java.util.Set<String> selectedThemes = new java.util.HashSet<>();

    private ServiceArtworks serviceArtworks = new ServiceArtworks();
    private ServiceCategories serviceCategories = new ServiceCategories();
    private GeminiDescriptionService geminiService = new GeminiDescriptionService();
    
    private ObservableList<Artworks> artworksList = FXCollections.observableArrayList();
    private ObservableList<Categories> allCategoriesList = FXCollections.observableArrayList();
    private java.util.Map<String, String> categorySearchMap = new java.util.HashMap<>();
    private java.util.Map<String, String> categorySortMap = new java.util.HashMap<>();
    private SortedList<Artworks> sortedData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sortedData = new SortedList<>(artworksList);
        
        sortedData.addListener((javafx.collections.ListChangeListener<Artworks>) c -> refreshGrid());

        loadData();
        
        try {
            List<Categories> categories = serviceCategories.getAll();
            allCategoriesList.addAll(categories);
            for (Categories c : categories) {
                categorySearchMap.put(c.getID(), "");
                categorySortMap.put(c.getID(), "Order of Manifestation");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        loadData();
        
        
        refreshGrid();
        setupGhostMascot();
    }

    private void setupGhostMascot() {
        if (ghostGroup == null) return;

        // 1. Floating Animation
        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(3), ghostGroup);
        floatAnim.setByY(-10);
        floatAnim.setCycleCount(Animation.INDEFINITE);
        floatAnim.setAutoReverse(true);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        floatAnim.play();

        // 2. Blinking Animation
        Random random = new Random();
        blinkTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            ScaleTransition blinkL = new ScaleTransition(Duration.millis(150), leftEye);
            blinkL.setToY(0.1); blinkL.setCycleCount(2); blinkL.setAutoReverse(true);
            ScaleTransition blinkR = new ScaleTransition(Duration.millis(150), rightEye);
            blinkR.setToY(0.1); blinkR.setCycleCount(2); blinkR.setAutoReverse(true);
            blinkL.play(); blinkR.play();
            blinkTimeline.setRate(0.5 + random.nextDouble());
        }));
        blinkTimeline.setCycleCount(Animation.INDEFINITE);
        blinkTimeline.play();

        // 3. Eye Tracking
        javafx.application.Platform.runLater(() -> {
            if (rootNode.getScene() != null) {
                rootNode.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
                    updatePupil(leftPupil, event.getScreenX(), event.getScreenY());
                    updatePupil(rightPupil, event.getScreenX(), event.getScreenY());
                });
            }
        });
    }

    private void updatePupil(Region pupil, double mouseX, double mouseY) {
        Point2D pPos = pupil.localToScreen(pupil.getWidth() / 2, pupil.getHeight() / 2);
        if (pPos == null) return;
        double dx = mouseX - pPos.getX();
        double dy = mouseY - pPos.getY();
        double angle = Math.atan2(dy, dx);
        double dist = Math.min(3, Math.sqrt(dx * dx + dy * dy) / 40);
        pupil.setTranslateX(Math.cos(angle) * dist);
        pupil.setTranslateY(Math.sin(angle) * dist);
    }

    private void loadData() {
        try {
            artworksList.clear();
            List<Artworks> data = serviceArtworks.getAll();
            artworksList.addAll(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshGrid() {
        categorySectionsContainer.getChildren().clear();
        
        // Iterate through categories to build sections
        for (Categories cat : allCategoriesList) {
            // Check if there are ANY artworks for this category to avoid empty realms
            boolean hasArtworks = false;
            for (Artworks a : sortedData) {
                if (String.valueOf(a.getCategoryID()).equals(cat.getID())) {
                    hasArtworks = true;
                    break;
                }
            }
            if (!hasArtworks) continue;

            // Section Container
            VBox sectionContainer = new VBox(15);
            
            HBox header = new HBox(15);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            header.setPadding(new javafx.geometry.Insets(30, 0, 10, 0));

            VBox titleBox = new VBox(5);
            Label sectionTitle = new Label(cat.getTitle().toUpperCase());
            sectionTitle.setStyle("-fx-font-family: 'Cinzel', serif; -fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
            
            Label sectionSubtitle = new Label("Artifacts in this Realm");
            sectionSubtitle.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 14px; -fx-font-weight: bold;");
            titleBox.getChildren().addAll(sectionTitle, sectionSubtitle);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Search Field
            TextField localSearch = new TextField(categorySearchMap.getOrDefault(cat.getID(), ""));
            localSearch.setPromptText("Search...");
            localSearch.getStyleClass().add("search-field");
            localSearch.setPrefWidth(180);
            
            // Sort ComboBox
            ComboBox<String> localSort = new ComboBox<>();
            localSort.getItems().addAll("Order of Manifestation", "Value: Low to High", "Value: High to Low", "Echoes (A-Z)");
            localSort.setValue(categorySortMap.getOrDefault(cat.getID(), "Order of Manifestation"));
            localSort.getStyleClass().add("combo-box");
            localSort.setPrefWidth(200);
            
            // Create FlowPane for this category
            FlowPane grid = new FlowPane(30, 30);
            grid.setAlignment(javafx.geometry.Pos.TOP_LEFT);

            localSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                categorySearchMap.put(cat.getID(), newVal);
                updateCategorySection(cat.getID(), newVal, categorySortMap.get(cat.getID()), grid, sectionSubtitle);
            });

            localSort.valueProperty().addListener((obs, oldVal, newVal) -> {
                categorySortMap.put(cat.getID(), newVal);
                updateCategorySection(cat.getID(), categorySearchMap.get(cat.getID()), newVal, grid, sectionSubtitle);
            });

            header.getChildren().addAll(titleBox, spacer, localSearch, localSort);

            Separator sep = new Separator();
            sep.setStyle("-fx-opacity: 0.1;");
            
            updateCategorySection(cat.getID(), categorySearchMap.getOrDefault(cat.getID(), ""), categorySortMap.get(cat.getID()), grid, sectionSubtitle);
            
            sectionContainer.getChildren().addAll(header, sep, grid);
            categorySectionsContainer.getChildren().add(sectionContainer);
        }
        
        updateTotalCount();
    }

    private void updateCategorySection(String catId, String filter, String sort, FlowPane grid, Label countLabel) {
        grid.getChildren().clear();
        String lowerFilter = filter.toLowerCase();
        
        // Find artworks for this category from sortedData
        java.util.List<Artworks> filtered = new java.util.ArrayList<>();
        for (Artworks a : sortedData) {
            String artworkCatId = String.valueOf(a.getCategoryID());
            if (artworkCatId.equals(catId)) {
                if (a.getTitle().toLowerCase().contains(lowerFilter) || 
                    a.getDescription().toLowerCase().contains(lowerFilter)) {
                    filtered.add(a);
                }
            }
        }
        
        if (filtered.isEmpty() && lowerFilter.isEmpty()) {
            System.out.println("No artworks found for category ID: " + catId);
        }

        // Apply local sort
        if (sort != null && !sort.equals("Order of Manifestation")) {
            filtered.sort((a1, a2) -> {
                switch (sort) {
                    case "Value: Low to High":
                        return Integer.compare(a1.getPrice(), a2.getPrice());
                    case "Value: High to Low":
                        return Integer.compare(a2.getPrice(), a1.getPrice());
                    case "Echoes (A-Z)":
                        return a1.getTitle().compareToIgnoreCase(a2.getTitle());
                    default:
                        return 0;
                }
            });
        }

        for (Artworks artwork : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/FrontArtworkCard.fxml"));
                Parent card = loader.load();
                FrontArtworkCardController controller = loader.getController();
                controller.setData(artwork, this);
                grid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        countLabel.setText(filtered.size() + " Artifacts in this Realm");
        updateTotalCount();
    }

    private void updateTotalCount() {
        int total = 0;
        for (Artworks a : sortedData) {
            String filter = categorySearchMap.getOrDefault(String.valueOf(a.getCategoryID()), "").toLowerCase();
            if (a.getTitle().toLowerCase().contains(filter)) {
                total++;
            }
        }
        itemCountLabel.setText(total + " RELICS FOUND");
    }

    private void applySort(String criteria) {
        // This is now handled independently per category
    }

    @FXML
    private void toggleMascot() {
        mascotBubble.setVisible(!mascotBubble.isVisible());
        
        // Surprise reaction on click! 😳
        if (ghostGroup != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), ghostGroup);
            st.setToX(1.3); st.setToY(1.3); st.setCycleCount(2); st.setAutoReverse(true);
            st.play();
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(50), ghostGroup);
            tt.setByX(5); tt.setCycleCount(6); tt.setAutoReverse(true);
            tt.play();

            if (leftBlush != null) {
                leftBlush.setVisible(true); rightBlush.setVisible(true);
                Timeline hide = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> {
                    leftBlush.setVisible(false); rightBlush.setVisible(false);
                }));
                hide.play();
            }
        }
    }

    @FXML
    private void handleThemeSelection(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        String theme = source.getText();
        
        if (selectedThemes.contains(theme)) {
            selectedThemes.remove(theme);
            source.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 15;");
        } else {
            selectedThemes.add(theme);
            source.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 15; -fx-background-color: #8b5cf6; -fx-text-fill: white;");
        }
    }

    @FXML
    private void handleAiSuggestion() {
        StringBuilder combinedDesire = new StringBuilder();
        for (String theme : selectedThemes) {
            combinedDesire.append(theme).append(" ");
        }
        String customInput = mascotInput.getText();
        if (customInput != null && !customInput.trim().isEmpty()) {
            combinedDesire.append(customInput);
        }

        String desire = combinedDesire.toString().trim();
        if (desire.isEmpty()) return;

        // Get button reference and original text early for all blocks to use
        Button manifestBtn = (Button) mascotBubble.getChildren().get(mascotBubble.getChildren().size() - 1);
        String originalText = manifestBtn.getText();

        // Check if we have any artworks to suggest from
        if (artworksList.isEmpty()) {
            System.err.println("No artworks available for suggestions.");
            manifestBtn.setText("Empty Collection!");
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> manifestBtn.setText(originalText));
            }).start();
            return;
        }

        manifestBtn.setText("Manifesting...");
        manifestBtn.setDisable(true);

        new Thread(() -> {
            try {
                System.out.println("AI Seeking: " + desire);
                GeminiDescriptionService.CuratorResponse response = geminiService.suggestArtworks(desire, artworksList);
                
                javafx.application.Platform.runLater(() -> {
                    if (response.suggestedIds.isEmpty()) {
                        manifestBtn.setText("No Matches Found");
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                            javafx.application.Platform.runLater(() -> {
                                manifestBtn.setText(originalText);
                                manifestBtn.setDisable(false);
                            });
                        }).start();
                    } else {
                        displaySuggestions(response.message, response.suggestedIds);
                        mascotBubble.setVisible(false);
                        mascotInput.clear();
                        manifestBtn.setText(originalText);
                        manifestBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    manifestBtn.setText("Connection Error");
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                        javafx.application.Platform.runLater(() -> {
                            manifestBtn.setText(originalText);
                            manifestBtn.setDisable(false);
                        });
                    }).start();
                });
            }
        }).start();
    }

    private void displaySuggestions(String message, java.util.List<Integer> ids) {
        updateCuratorFlow(message);
        suggestionsGrid.getChildren().clear();
        boolean foundAny = false;

        for (Integer id : ids) {
            Artworks match = artworksList.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
            if (match != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/FrontArtworkCard.fxml"));
                    Parent card = loader.load();
                    FrontArtworkCardController controller = loader.getController();
                    controller.setData(match, this);
                    suggestionsGrid.getChildren().add(card);
                    foundAny = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (foundAny) {
            aiSuggestionsContainer.setVisible(true);
            aiSuggestionsContainer.setManaged(true);
            // Scroll to top to see suggestions
            mainScrollPane.setVvalue(0.0);
        } else {
            System.out.println("No matching artworks found for the suggested IDs.");
        }
    }

    @FXML
    private void closeSuggestions() {
        aiSuggestionsContainer.setVisible(false);
        aiSuggestionsContainer.setManaged(false);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomeFront.fxml"));
            rootNode.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void navigateToDetails(Artworks a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Artworks/DetailArtwork.fxml"));
            Parent root = loader.load();
            DetailArtworkController controller = loader.getController();
            controller.setArtworkData(a);
            controller.setIsFrontOffice(true);
            
            rootNode.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCuratorFlow(String message) {
        curatorMessageFlow.getChildren().clear();
        
        // Simple regex to find emojis and text segments
        // This is a simplified version focusing on common emojis used by the curator
        java.util.Map<String, String> emojiMap = new java.util.HashMap<>();
        emojiMap.put("✨", "/icons/sparkles.png");
        emojiMap.put("🧡", "/icons/heart.png");
        emojiMap.put("🔮", "/icons/crystal.png");
        emojiMap.put("💎", "/icons/gem.png");
        emojiMap.put("🌌", "/icons/galaxy.png");
        
        String currentText = message;
        for (java.util.Map.Entry<String, String> entry : emojiMap.entrySet()) {
            String emoji = entry.getKey();
            String path = entry.getValue();
            
            if (currentText.contains(emoji)) {
                String[] parts = currentText.split(java.util.regex.Pattern.quote(emoji), -1);
                curatorMessageFlow.getChildren().clear();
                
                for (int i = 0; i < parts.length; i++) {
                    Text textNode = new Text(parts[i]);
                    textNode.setFill(Color.web("#e2e8f0"));
                    textNode.setStyle("-fx-font-size: 15px;");
                    curatorMessageFlow.getChildren().add(textNode);
                    
                    if (i < parts.length - 1) {
                        try {
                            Image img = new Image(getClass().getResourceAsStream(path));
                            ImageView iv = new ImageView(img);
                            iv.setFitHeight(20);
                            iv.setFitWidth(20);
                            curatorMessageFlow.getChildren().add(iv);
                        } catch (Exception e) {}
                    }
                }
                return; 
            }
        }
        
        // Fallback if no specific emojis found
        javafx.scene.text.Text textNode = new javafx.scene.text.Text(message);
        textNode.setFill(javafx.scene.paint.Color.web("#e2e8f0"));
        textNode.setStyle("-fx-font-size: 15px;");
        curatorMessageFlow.getChildren().add(textNode);
    }
}
