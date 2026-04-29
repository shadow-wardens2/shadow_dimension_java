package Controllers.Artworks;

import Entities.Artworks.Categories;
import Services.Artworks.ServiceCategories;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import Controllers.Marketplace.PageHost;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterCategoryController implements Initializable {
    
    @FXML private Text titleText;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private Button btnSave;

    private ServiceCategories serviceCategories = new ServiceCategories();
    private Categories currentCategory = null;
    private PageHost dashboardContext;

    public void setDashboardContext(PageHost ctx) {
        this.dashboardContext = ctx;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setCategoryData(Categories c) {
        this.currentCategory = c;
        if (c == null) return;
        
        titleText.setText("EDIT CATEGORY");
        btnSave.setText("Update Category");
        
        txtName.setText(c.getTitle());
        txtDescription.setText(c.getDescription());
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (txtName.getText().isEmpty() || txtDescription.getText().isEmpty()) {
            showAlert("Validation Error", "Name and Description are required.");
            return;
        }

        try {
            if (currentCategory == null) {
                Categories c = new Categories();
                c.setTitle(txtName.getText());
                c.setDescription(txtDescription.getText());
                serviceCategories.add(c);
                showAlert("Success", "Category added successfully!");
            } else {
                currentCategory.setTitle(txtName.getText());
                currentCategory.setDescription(txtDescription.getText());
                serviceCategories.update(currentCategory);
                showAlert("Success", "Category updated successfully!");
            }
            closeWindow();
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
            dashboardContext.loadPage("/Artworks/ListerCategories.fxml");
        } else {
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
