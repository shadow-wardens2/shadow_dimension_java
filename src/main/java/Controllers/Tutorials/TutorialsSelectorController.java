package Controllers.Tutorials;

import Controllers.Marketplace.Back.PageHost;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class TutorialsSelectorController {

    @FXML
    private VBox rootNode;

    private PageHost dashboardContext;

    public void setDashboardContext(PageHost dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    @FXML
    void handleFormations(ActionEvent event) {
        load("/Tutorials/ManagementFormation.fxml");
    }

    @FXML
    void handleJeux(ActionEvent event) {
        load("/Tutorials/ManagementJeu.fxml");
    }

    @FXML
    void handleLecons(ActionEvent event) {
        load("/Tutorials/ManagementLecon.fxml");
    }

    @FXML
    void handleQuizzes(ActionEvent event) {
        load("/Tutorials/ManagementQuiz.fxml");
    }

    @FXML
    void handleBackHome(ActionEvent event) {
        if (dashboardContext != null) {
            dashboardContext.loadPage("/HomeContent.fxml");
            return;
        }

        load("/HomeFront.fxml");
    }

    private void load(String fxml) {
        if (dashboardContext != null) {
            dashboardContext.loadPage(fxml);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            rootNode.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
