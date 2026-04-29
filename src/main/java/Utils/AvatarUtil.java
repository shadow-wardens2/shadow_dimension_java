package Utils;

import Entities.User.User;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public final class AvatarUtil {

    private AvatarUtil() {
    }

    public static void applyDiceBearAvatar(ImageView imageView, Label fallbackLabel, User user, double size) {
        String initials = user == null ? "SD" : user.getAvatarInitials();
        String fallbackStyle = user == null
                ? "-fx-background-color: #19191d; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center;"
                : user.getAvatarStyle();

        fallbackLabel.setText(initials);
        fallbackLabel.setStyle(fallbackStyle);
        fallbackLabel.setVisible(true);
        fallbackLabel.setManaged(true);

        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);
        imageView.setClip(new Circle(size / 2, size / 2, size / 2));

        if (user == null) {
            imageView.setImage(null);
            return;
        }

        Image image = new Image(user.getDiceBearAvatarUrl((int) size), size, size, false, true, true);
        imageView.setImage(image);

        ChangeListener<Number> progressListener = (obs, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    fallbackLabel.setVisible(false);
                    fallbackLabel.setManaged(false);
                });
            }
        };
        image.progressProperty().addListener(progressListener);
        image.errorProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                Platform.runLater(() -> {
                    fallbackLabel.setVisible(true);
                    fallbackLabel.setManaged(true);
                    imageView.setImage(null);
                });
            }
        });
    }
}
