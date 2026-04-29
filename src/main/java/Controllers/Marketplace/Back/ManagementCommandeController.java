package Controllers.Marketplace.Back;

import Entities.Marketplace.Commande;
import Services.Marketplace.ServiceCommande;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.collections.transformation.FilteredList;

import java.sql.SQLException;

public class ManagementCommandeController {

    @FXML private VBox ordersContainer;
    @FXML private TextField searchField;
    private PageHost dashboardContext;
    
    private ServiceCommande serviceCommande = new ServiceCommande();
    private ObservableList<Commande> observableCommandes = FXCollections.observableArrayList();
    private FilteredList<Commande> filteredData;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void goBack(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/Marketplace/Back/MarketplaceSelector.fxml");
        }
    }

    @FXML
    public void initialize() {
        setupData();
        refreshList();
    }

    private void setupData() {
        try {
            observableCommandes.setAll(serviceCommande.getAll());
            filteredData = new FilteredList<>(observableCommandes, c -> true);
            
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(commande -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return String.valueOf(commande.getId()).contains(lower) ||
                           (commande.getNom() != null && commande.getNom().toLowerCase().contains(lower)) ||
                           (commande.getPrenom() != null && commande.getPrenom().toLowerCase().contains(lower)) ||
                           (commande.getEmail() != null && commande.getEmail().toLowerCase().contains(lower));
                });
                refreshList();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshList() {
        ordersContainer.getChildren().clear();
        for (Commande c : filteredData) {
            HBox row = new HBox(20);
            row.setStyle("-fx-padding: 15; -fx-background-color: #1c1922; -fx-background-radius: 8;");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            VBox info = new VBox(5);
            Label idLbl = new Label("Order #" + c.getId() + " - " + c.getDateCommande());
            idLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            Label customerLbl = new Label(c.getPrenom() + " " + c.getNom() + " (" + c.getEmail() + ")");
            customerLbl.setStyle("-fx-text-fill: #a1a1aa;");
            info.getChildren().addAll(idLbl, customerLbl);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label totalLbl = new Label(String.format("%.2f TND", c.getTotalAmount()));
            totalLbl.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            
            Label statusLbl = new Label(c.getStatus());
            statusLbl.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
            
            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-background-radius: 6; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> deleteOrder(c));
            
            row.getChildren().addAll(info, spacer, totalLbl, statusLbl, deleteBtn);
            ordersContainer.getChildren().add(row);
        }
    }

    private void deleteOrder(Commande c) {
        try {
            serviceCommande.delete(c);
            observableCommandes.remove(c);
            refreshList();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Order deleted successfully.");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not delete");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
