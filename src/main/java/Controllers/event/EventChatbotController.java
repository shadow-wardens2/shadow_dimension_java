package Controllers.event;

// Service that talks to OpenRouter using event/category context.
import Services.event.EventAiAssistantService;
// Injects FXML nodes into this controller.
import javafx.fxml.FXML;
// JavaFX button control.
import javafx.scene.control.Button;
// Chat transcript area.
import javafx.scene.control.TextArea;
// Input field for user question.
import javafx.scene.control.TextField;

// Async helper for non-blocking AI calls.
import java.util.concurrent.CompletableFuture;

// Controller for the small Event chatbot popup window.
public class EventChatbotController {

    // Displays the full conversation history.
    @FXML
    private TextArea taChat;
    // Input where user types a question.
    @FXML
    private TextField tfChatInput;
    // Main send button.
    @FXML
    private Button btnSendChat;
    // Suggestion shortcut button #1.
    @FXML
    private Button btnSuggestion1;
    // Suggestion shortcut button #2.
    @FXML
    private Button btnSuggestion2;
    // Suggestion shortcut button #3.
    @FXML
    private Button btnSuggestion3;

    // Service instance used to generate AI answers.
    private final EventAiAssistantService aiAssistantService = new EventAiAssistantService();

    // JavaFX initialization callback.
    @FXML
    public void initialize() {
        // Clears any default text from scene builder/runtime remnants.
        taChat.clear();
        // Prints welcome line when popup opens.
        taChat.appendText("Oracle: Greetings, Seeker of the Shadow Dimensions. Ask me about your events and categories.\n\n");

        // Loads predefined starter prompts from service.
        String[] suggestions = aiAssistantService.starterSuggestions();
        // Binds suggestion text to quick-action buttons.
        btnSuggestion1.setText(suggestions[0]);
        btnSuggestion2.setText(suggestions[1]);
        btnSuggestion3.setText(suggestions[2]);

        // Allows pressing Enter inside text field to send message.
        tfChatInput.setOnAction(event -> handleSendChat());
    }

    // Handles click on one of the suggestion buttons.
    @FXML
    private void handleSuggestion(javafx.event.ActionEvent event) {
        // Confirms source is indeed a button then reuses generic send flow.
        if (event.getSource() instanceof Button suggestionButton) {
            // Writes selected suggestion into input field.
            tfChatInput.setText(suggestionButton.getText());
            // Sends it immediately as a normal question.
            handleSendChat();
        }
    }

    // Sends user question to AI and appends answer asynchronously.
    @FXML
    private void handleSendChat() {
        // Reads and trims user input.
        String question = tfChatInput.getText() == null ? "" : tfChatInput.getText().trim();
        // Stops on empty messages.
        if (question.isEmpty()) {
            return;
        }

        // Appends user message to transcript.
        taChat.appendText("You: " + question + "\n");
        // Clears input box for next message.
        tfChatInput.clear();
        // Prevents spam clicks while request is running.
        btnSendChat.setDisable(true);

        // Calls AI on background thread.
        CompletableFuture.supplyAsync(() -> aiAssistantService.askQuestion(question))
                // Switches back to UI thread to update controls safely.
                .thenAccept(answer -> javafx.application.Platform.runLater(() -> {
                    // Appends AI answer to transcript.
                    taChat.appendText("Oracle: " + answer + "\n\n");
                    // Re-enables send button after completion.
                    btnSendChat.setDisable(false);
                }));
    }
}
