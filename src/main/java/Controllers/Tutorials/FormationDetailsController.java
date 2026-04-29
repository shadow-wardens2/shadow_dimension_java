package Controllers.Tutorials;

import Entities.Tutorials.Formation;
import Entities.Tutorials.Lecon;
import Entities.Tutorials.Quiz;
import Services.Tutorials.ServiceLecon;
import Services.Tutorials.ServiceQuiz;
import Services.Tutorials.ServiceQuizProgress;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FormationDetailsController implements Initializable {

    @FXML
    private Label lbTitle, lbDescription, lbLevel, lbLessonCount, lbQuizCount;
    @FXML
    private VBox lessonsContainer, quizzesContainer;
    @FXML
    private Button btnEnroll;

    private Formation currentFormation;
    private ServiceLecon serviceLecon = new ServiceLecon();
    private ServiceQuiz serviceQuiz = new ServiceQuiz();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data usually set via external call to setFormation
    }

    public void setFormation(Formation formation) {
        this.currentFormation = formation;
        lbTitle.setText(formation.getTitre());
        lbDescription.setText(formation.getDescription());
        lbLevel.setText("LEVEL: " + formation.getNiveau().toUpperCase());

        loadContent();
    }

    public void refresh() {
        loadContent();
    }

    private void loadContent() {
        try {
            // Check enrollment/progress to set button state
            int userId = (Utils.SessionManager.getCurrentUser() != null) ? Utils.SessionManager.getCurrentUser().getId()
                    : -1;
            boolean hasProgress = false;

            List<Quiz> allFormationQuizzes = serviceQuiz.getAll().stream()
                    .filter(q -> q.getFormation() != null && q.getFormation().getId() == currentFormation.getId())
                    .sorted((a, b) -> Integer.compare(a.getOrdre(), b.getOrdre()))
                    .collect(Collectors.toList());

            if (userId != -1) {
                ServiceQuizProgress sp = new ServiceQuizProgress();
                for (Quiz q : allFormationQuizzes) {
                    if (sp.isQuizCompleted(userId, q.getId())) {
                        hasProgress = true;
                        break;
                    }
                }
            }

            if (hasProgress) {
                btnEnroll.setText("Quit Course ×");
                btnEnroll.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");
                btnEnroll.getStyleClass().remove("glow-button");
                btnEnroll.getStyleClass().add("secondary-button");
            } else {
                btnEnroll.setText("Enroll Now");
                btnEnroll.setStyle("");
                btnEnroll.getStyleClass().remove("secondary-button");
                btnEnroll.getStyleClass().add("glow-button");
            }

            List<Lecon> lessons = serviceLecon.getAll().stream()
                    .filter(l -> l.getFormation() != null && l.getFormation().getId() == currentFormation.getId())
                    .sorted((a, b) -> Integer.compare(a.getOrdre(), b.getOrdre()))
                    .collect(Collectors.toList());

            displayLessons(lessons);
            displayQuizzes(allFormationQuizzes);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayLessons(List<Lecon> lessons) {
        lessonsContainer.getChildren().clear();
        lbLessonCount.setText("(" + lessons.size() + ")");

        ServiceQuizProgress progressService = new ServiceQuizProgress();
        int userId = (Utils.SessionManager.getCurrentUser() != null) ? Utils.SessionManager.getCurrentUser().getId() : -1;

        // Fetch sorted quizzes to find the progression threshold
        List<Quiz> sortedQuizzes = null;
        try {
            sortedQuizzes = serviceQuiz.getAll().stream()
                    .filter(q -> q.getFormation() != null && q.getFormation().getId() == currentFormation.getId())
                    .sorted((a, b) -> Integer.compare(a.getOrdre(), b.getOrdre()))
                    .collect(Collectors.toList());
        } catch (SQLException e) { e.printStackTrace(); }

        // Find the first uncompleted quiz's order
        int thresholdOrder = Integer.MAX_VALUE;
        if (sortedQuizzes != null && userId != -1) {
            for (Quiz q : sortedQuizzes) {
                if (!progressService.isQuizCompleted(userId, q.getId())) {
                    thresholdOrder = q.getOrdre();
                    break;
                }
            }
        }

        for (Lecon l : lessons) {
            // A lesson is locked if it comes AFTER an uncompleted quiz
            boolean isLocked = l.getOrdre() > thresholdOrder;
            lessonsContainer.getChildren().add(createLessonRow(l, isLocked, false));
        }
    }

    private void displayQuizzes(List<Quiz> quizzes) {
        quizzesContainer.getChildren().clear();
        lbQuizCount.setText("(" + quizzes.size() + ")");

        List<Quiz> sortedQuizzes = quizzes.stream()
                .sorted((q1, q2) -> Integer.compare(q1.getOrdre(), q2.getOrdre()))
                .collect(Collectors.toList());

        ServiceQuizProgress progressService = new ServiceQuizProgress();
        int userId = (Utils.SessionManager.getCurrentUser() != null) ? Utils.SessionManager.getCurrentUser().getId() : -1;

        // Find the first uncompleted quiz's order
        int thresholdOrder = Integer.MAX_VALUE;
        if (userId != -1) {
            for (Quiz q : sortedQuizzes) {
                if (!progressService.isQuizCompleted(userId, q.getId())) {
                    thresholdOrder = q.getOrdre();
                    break;
                }
            }
        }

        for (Quiz q : sortedQuizzes) {
            boolean isAttempted = (userId != -1) && progressService.isQuizCompleted(userId, q.getId());
            boolean isPassed = (userId != -1) && progressService.isQuizPassed(userId, q.getId());
            
            // A quiz is locked if it comes AFTER an uncompleted quiz
            boolean isLocked = q.getOrdre() > thresholdOrder;

            quizzesContainer.getChildren().add(createQuizRow(q, isLocked, isPassed));
        }
    }

    private HBox createLessonRow(Lecon l, boolean isLocked, boolean isCompleted) {
        HBox row = new HBox(20);
        row.getStyleClass().add("artifact-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(15, 25, 15, 25));

        if (isLocked) {
            row.setOpacity(0.5);
        }

        Label order = new Label(String.format("%02d", l.getOrdre()));
        order.getStyleClass().add("label-sigil");
        order.setOpacity(0.5);

        VBox textContent = new VBox(5);
        Label title = new Label(l.getTitre());
        title.getStyleClass().add("title-lg");
        title.setStyle("-fx-font-size: 18px;");

        String metaText = l.getVideoDuration() != null ? l.getVideoDuration() : "Reading Material";
        if (isCompleted) metaText += " ✓";
        else if (isLocked) metaText = "Locked 🔒";

        Label meta = new Label(metaText);
        meta.getStyleClass().add("body-md");
        meta.setOpacity(0.5);
        meta.setStyle("-fx-font-size: 12px;");

        textContent.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnView = new Button(isCompleted ? "Review" : "View");
        btnView.getStyleClass().add(isLocked ? "secondary-button" : "glow-button");
        btnView.setDisable(isLocked);
        btnView.setOnAction(e -> {
            if (l.getVideoUrl() != null && !l.getVideoUrl().isEmpty()) {
                navigateToPlayer(l);
            } else if ((l.getContenu() != null && !l.getContenu().isEmpty())
                    || (l.getDocumentUrl() != null && !l.getDocumentUrl().isEmpty())) {
                navigateToViewer(l);
            } else {
                System.out.println("Nothing to view for: " + l.getTitre());
            }
        });

        row.getChildren().addAll(order, textContent, spacer, btnView);
        return row;
    }

    private HBox createQuizRow(Quiz q, boolean isLocked, boolean isCompleted) {
        HBox row = new HBox(20);
        row.getStyleClass().add("artifact-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(15, 25, 15, 25));
        row.setStyle(row.getStyle() + "; -fx-border-color: rgba(139, 92, 246, 0.1); -fx-border-width: 0 0 0 4;");

        if (isLocked) {
            row.setOpacity(0.5);
        }

        VBox textContent = new VBox(5);
        Label title = new Label(q.getTitre());
        title.getStyleClass().add("title-lg");
        title.setStyle("-fx-font-size: 18px;");

        Label meta = new Label(
                isCompleted ? "Knowledge Mastery ✓" : (isLocked ? "Trial Locked 🔒" : "Knowledge Trial"));
        meta.getStyleClass().add("label-sigil");
        meta.setOpacity(0.7);

        textContent.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnStart = new Button(isCompleted ? "Retake" : "Begin");
        btnStart.getStyleClass().add(isLocked ? "secondary-button" : "glow-button");
        btnStart.setPrefWidth(80);
        btnStart.setDisable(isLocked);

        btnStart.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/QuizPlay.fxml"));
                Parent root = loader.load();

                QuizPlayController controller = loader.getController();
                controller.startQuiz(q, lbTitle.getScene().getRoot(), this);

                lbTitle.getScene().setRoot(root);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        row.getChildren().addAll(textContent, spacer, btnStart);
        return row;
    }

    @FXML
    void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/TutorialsFront.fxml"));
            lbTitle.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleEnroll() {
        int userId = (Utils.SessionManager.getCurrentUser() != null) ? Utils.SessionManager.getCurrentUser().getId()
                : -1;
        if (userId == -1) {
            System.err.println("Must be logged in to enroll");
            return;
        }

        if (btnEnroll.getText().contains("Quit")) {
            // Already enrolled, now quitting (resetting)
            ServiceQuizProgress progressService = new ServiceQuizProgress();
            progressService.resetProgress(userId, currentFormation.getId());

            btnEnroll.setText("Enroll Now");
            btnEnroll.setStyle("");
            btnEnroll.getStyleClass().remove("secondary-button");
            btnEnroll.getStyleClass().add("glow-button");

            System.out.println("Progress reset for: " + currentFormation.getTitre());
            loadContent(); // Refresh UI to lock quizzes
        } else {
            // Enrolling
            btnEnroll.setText("Quit Course ×");
            btnEnroll.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");
            btnEnroll.getStyleClass().remove("glow-button");
            btnEnroll.getStyleClass().add("secondary-button");
            System.out.println("Enrolled in formation: " + currentFormation.getTitre());
            loadContent();
        }
    }

    private void navigateToPlayer(Lecon l) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/VideoPlayer.fxml"));
            javafx.scene.Parent root = loader.load();

            VideoPlayerController controller = loader.getController();
            controller.loadVideo(l, lbTitle.getScene().getRoot());

            lbTitle.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToViewer(Lecon l) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/LessonViewer.fxml"));
            javafx.scene.Parent root = loader.load();

            LessonViewerController controller = loader.getController();
            controller.setLesson(l, lbTitle.getScene().getRoot());

            lbTitle.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
