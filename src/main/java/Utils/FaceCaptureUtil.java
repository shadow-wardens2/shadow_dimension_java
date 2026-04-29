package Utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class FaceCaptureUtil {

    private FaceCaptureUtil() {
    }

    public static BufferedImage captureFace(Stage owner, String title, String description) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("Aucune webcam detectee sur cette machine.");
        }

        BufferedImage capturedFrame = null;
        Timeline previewTimeline = null;

        try {
            Dimension preferredSize = WebcamResolution.VGA.getSize();
            webcam.setViewSize(preferredSize);
            webcam.open();

            ImageView preview = new ImageView();
            preview.setFitWidth(420);
            preview.setFitHeight(315);
            preview.setPreserveRatio(true);
            preview.setSmooth(true);
            preview.setStyle("-fx-background-color: #0e0e11; -fx-background-radius: 18;");

            Label infoLabel = new Label(description);
            infoLabel.setWrapText(true);
            infoLabel.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px;");

            Label noteLabel = new Label("Place your face in front of the camera, look straight ahead, then press Capture.");
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 12px;");

            VBox content = new VBox(14, infoLabel, preview, noteLabel);
            content.setPadding(new Insets(18));
            content.setStyle("-fx-background-color: #15121a;");

            AtomicReference<BufferedImage> currentFrame = new AtomicReference<>();
            previewTimeline = new Timeline(new KeyFrame(Duration.millis(120), event -> {
                BufferedImage frame = webcam.getImage();
                if (frame == null) {
                    return;
                }
                currentFrame.set(copy(frame));
                WritableImage fxImage = SwingFXUtils.toFXImage(frame, null);
                preview.setImage(fxImage);
            }));
            previewTimeline.setCycleCount(Animation.INDEFINITE);
            previewTimeline.play();

            Dialog<ButtonType> dialog = new Dialog<>();
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setHeaderText(null);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setStyle("-fx-background-color: #15121a;");
            ButtonType captureType = new ButtonType("Capture");
            dialog.getDialogPane().getButtonTypes().setAll(captureType, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == captureType) {
                BufferedImage frame = currentFrame.get();
                if (frame == null) {
                    throw new IllegalStateException("Impossible de lire une image depuis la webcam.");
                }
                capturedFrame = copy(frame);
            }
        } finally {
            if (previewTimeline != null) {
                previewTimeline.stop();
            }
            if (webcam.isOpen()) {
                webcam.close();
            }
        }

        return capturedFrame;
    }

    public static <T> T recognizeFace(Stage owner, String title, String description, Function<BufferedImage, T> recognizer) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("Aucune webcam detectee sur cette machine.");
        }

        Timeline previewTimeline = null;
        Dialog<ButtonType> dialog = new Dialog<>();
        AtomicReference<T> matchedResult = new AtomicReference<>();
        AtomicReference<Boolean> analyzing = new AtomicReference<>(false);

        try {
            Dimension preferredSize = WebcamResolution.VGA.getSize();
            webcam.setViewSize(preferredSize);
            webcam.open();

            ImageView preview = new ImageView();
            preview.setFitWidth(420);
            preview.setFitHeight(315);
            preview.setPreserveRatio(true);
            preview.setSmooth(true);
            preview.setStyle("-fx-background-color: #0e0e11; -fx-background-radius: 18;");

            Label infoLabel = new Label(description);
            infoLabel.setWrapText(true);
            infoLabel.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px;");

            Label noteLabel = new Label("Place your face in front of the camera. Recognition starts automatically.");
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 12px;");

            VBox content = new VBox(14, infoLabel, preview, noteLabel);
            content.setPadding(new Insets(18));
            content.setStyle("-fx-background-color: #15121a;");

            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setHeaderText(null);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setStyle("-fx-background-color: #15121a;");
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);

            previewTimeline = new Timeline(new KeyFrame(Duration.millis(180), event -> {
                BufferedImage frame = webcam.getImage();
                if (frame == null) {
                    return;
                }

                preview.setImage(SwingFXUtils.toFXImage(frame, null));

                if (Boolean.TRUE.equals(analyzing.get())) {
                    return;
                }

                analyzing.set(true);
                BufferedImage snapshot = copy(frame);
                T result = recognizer.apply(snapshot);
                if (result != null) {
                    matchedResult.set(result);
                    noteLabel.setText("Face recognized. Opening your vault...");
                    dialog.close();
                    return;
                }

                noteLabel.setText("Scanning... keep your face centered and look straight at the camera.");
                analyzing.set(false);
            }));
            previewTimeline.setCycleCount(Animation.INDEFINITE);
            previewTimeline.play();

            dialog.showAndWait();
            return matchedResult.get();
        } finally {
            if (previewTimeline != null) {
                previewTimeline.stop();
            }
            if (webcam.isOpen()) {
                webcam.close();
            }
        }
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }
}
