package Controllers.Marketplace;

import Utils.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import Entities.User.User;

import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.stage.FileChooser;
import java.awt.Color;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import Entities.Marketplace.Type;
import Utils.PdfExporter;
import java.sql.SQLException;

public class MarketplaceManagementController implements PageHost {

    @FXML
    private StackPane contentArea;

    @FXML
    private Label lbUserName;

    @FXML
    private Label lbUserTier;

    @FXML
    private Button btnTopAuth;

    @FXML
    private Button btnBottomAuth;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) {
            // Need to wait until the scene is set to redirect
            Platform.runLater(this::redirectToLogin);
            return;
        }
        refreshAuthUi();
        // Load the Home page upon initialization
        loadPage("/HomeContent.fxml");
    }

    private void refreshAuthUi() {
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            String username = user.getUsername() == null || user.getUsername().isBlank() ? "Shadow Dweller" : user.getUsername();
            lbUserName.setText(username);
            lbUserTier.setText(user.isAdmin() ? "ADMIN" : "MEMBER");
            btnTopAuth.setText("Logout");
            btnBottomAuth.setText("Logout");
        } else {
            lbUserName.setText("Shadow Dweller");
            lbUserTier.setText("GUEST");
            btnTopAuth.setText("Connect Soul");
            btnBottomAuth.setText("Connect Soul");
        }
    }

    @FXML
    void handleAuthAction(ActionEvent event) {
        if (SessionManager.isLoggedIn()) {
            SessionManager.clear();
            refreshAuthUi();
            redirectToLogin();
            return;
        }
        redirectToLogin();
    }

    private void redirectToLogin() {
        showAlert(Alert.AlertType.WARNING, "Accès Restreint", "Veuillez vous connecter pour accéder à la gestion du marketplace.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/ConnectSoul.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("Connect Soul");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException | NullPointerException e) {
            // If we can't get the stage yet, it might be because the FXML hasn't been shown
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    @FXML
    void openHome(ActionEvent event) {
        loadPage("/HomeContent.fxml");
    }

    @FXML
    void openMarketplaceSelector(ActionEvent event) {
        loadPage("/Marketplace/MarketplaceSelector.fxml");
    }

    // Helper method to swap out the center FXML content dynamically
    public Object loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // If the loaded page is the MarketplaceSelector, we want to inject this parent
            // controller
            // so from inside the selector they can swap the entire dashboard to
            // Prod/Cat/Type
            Object controller = loader.getController();
            if (controller instanceof MarketplaceSelectorController) {
                ((MarketplaceSelectorController) controller).setDashboardContext(this);
            } else if (controller instanceof ManagementCategorieController) {
                ((ManagementCategorieController) controller).setDashboardContext(this);
            } else if (controller instanceof ManagementProduitController) {
                ((ManagementProduitController) controller).setDashboardContext(this);
            } else if (controller instanceof ManagementTypeController) {
                ((ManagementTypeController) controller).setDashboardContext(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    void handleDownloadReport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Marketplace Report");
        fileChooser.setInitialFileName("Marketplace_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(contentArea.getScene().getWindow());

        if (file != null) {
            try {
                PdfExporter.generateMarketplaceReport(file);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Report saved successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate PDF: " + e.getMessage());
            }
        }
    }
}
