package Controllers.Tutorials;

import Entities.Tutorials.Lecon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class LessonViewerController {

    @FXML
    private Label lbTitle;

    @FXML
    private Label lbContent;

    @FXML
    private Button btnDownload;

    private Lecon currentLecon;
    private Parent previousView;

    public void setLesson(Lecon lecon, Parent previousView) {
        this.currentLecon = lecon;
        this.previousView = previousView;

        lbTitle.setText(lecon.getTitre());
        lbContent.setText(lecon.getContenu());

        if (lecon.getDocumentUrl() != null && !lecon.getDocumentUrl().isEmpty()) {
            btnDownload.setVisible(true);
        }
    }

    @FXML
    private void handleOpenDocument() {
        String url = currentLecon.getDocumentUrl();
        if (url == null || url.isEmpty())
            return;

        System.out.println("Attempting to resolve document: " + url);

        String projectDir = System.getProperty("user.dir");
        String documentsFolder = projectDir + java.io.File.separator + "documents";

        try {
            if (url.startsWith("http")) {
                // Web URL
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    new ProcessBuilder("cmd", "/c", "start", "", url).start();
                }
            } else {
                // Local file resolution
                java.io.File file = new java.io.File(url);

                // Fallback to the 'documents' folder in the project
                if (!file.exists()) {
                    String fileName = new java.io.File(url).getName();
                    file = new java.io.File(documentsFolder, fileName);
                    System.out.println("DEBUG: Trying local documents folder: " + file.getAbsolutePath());
                }

                if (file.exists()) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(file);
                    } else {
                        new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
                    }
                } else {
                    System.err.println("CRITICAL: File not found in database path OR project documents folder: "
                            + file.getAbsolutePath());
                    // Final attempt: try launching with the raw string
                    new ProcessBuilder("cmd", "/c", "start", "", url).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBack() {
        if (previousView != null) {
            lbTitle.getScene().setRoot(previousView);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tutorials/TutorialsFront.fxml"));
                lbTitle.getScene().setRoot(loader.load());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
