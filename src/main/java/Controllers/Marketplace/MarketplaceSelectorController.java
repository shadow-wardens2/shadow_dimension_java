package Controllers.Marketplace;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import Utils.PdfExporter;

public class MarketplaceSelectorController {

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/HomeContent.fxml");
        }
    }

    @FXML
    void handleCategories(ActionEvent event) {
        dashboardContext.loadPage("/Marketplace/ManagementCategorie.fxml");
    }

    @FXML
    void handleProducts(ActionEvent event) {
        dashboardContext.loadPage("/Marketplace/ManagementProduit.fxml");
    }

    @FXML
    void handleTypes(ActionEvent event) {
        dashboardContext.loadPage("/Marketplace/ManagementType.fxml");
    }

    @FXML
    void handleDownloadReport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Marketplace Report");
        fileChooser.setInitialFileName("Marketplace_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(null); // Passing null is okay if we can't easily get the window

        if (file != null) {
            try {
                PdfExporter.generateMarketplaceReport(file);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Report saved successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
