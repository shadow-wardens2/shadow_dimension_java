package Controllers.event;

import Services.event.EventAiAssistantService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;

public class EventChatbotController {

    @FXML
    private TextArea taChat;
    @FXML
    private TextField tfChatInput;
    @FXML
    private Button btnSendChat;
    @FXML
    private Button btnSuggestion1;
    @FXML
    private Button btnSuggestion2;
    @FXML
    private Button btnSuggestion3;

    private final EventAiAssistantService aiAssistantService = new EventAiAssistantService();

    @FXML
    public void initialize() {
        taChat.clear();
        taChat.appendText("Oracle: Greetings, Seeker of the Shadow Dimensions. Ask me about your events and categories.\n\n");

        String[] suggestions = aiAssistantService.starterSuggestions();
        btnSuggestion1.setText(suggestions[0]);
        btnSuggestion2.setText(suggestions[1]);
        btnSuggestion3.setText(suggestions[2]);

        tfChatInput.setOnAction(event -> handleSendChat());
    }

    @FXML
    private void handleSuggestion(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Button suggestionButton) {
            tfChatInput.setText(suggestionButton.getText());
            handleSendChat();
        }
    }

    @FXML
    private void handleSendChat() {
        String question = tfChatInput.getText() == null ? "" : tfChatInput.getText().trim();
        if (question.isEmpty()) {
            return;
        }

        taChat.appendText("You: " + question + "\n");
        tfChatInput.clear();
        btnSendChat.setDisable(true);

        CompletableFuture.supplyAsync(() -> aiAssistantService.askQuestion(question))
                .thenAccept(answer -> javafx.application.Platform.runLater(() -> {
                    taChat.appendText("Oracle: " + answer + "\n\n");
                    btnSendChat.setDisable(false);
                }));
    }
}
