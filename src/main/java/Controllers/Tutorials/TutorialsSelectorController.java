package Controllers.Tutorials;

import Controllers.Marketplace.PageHost;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class TutorialsSelectorController {

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

    private void load(String fxml) {
        if (dashboardContext != null) {
            dashboardContext.loadPage(fxml);
        }
    }
}
