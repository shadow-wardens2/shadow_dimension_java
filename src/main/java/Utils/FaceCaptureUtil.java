package Utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class FaceCaptureUtil {
    private static final int SAMPLE_BUFFER_SIZE = 12;
    private static final int PROFILE_SAMPLE_COUNT = 6;
    private static final int LOGIN_MIN_VISIBLE_FRAMES = 5;
    private static final int LOGIN_REQUIRED_MATCHES = 3;

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

            Canvas overlay = new Canvas(420, 315);
            drawGuideOverlay(overlay, Color.web("#c44b4b"), "Align your face inside the frame");
            StackPane previewPane = new StackPane(preview, overlay);
            previewPane.setAlignment(Pos.CENTER);

            Label infoLabel = new Label(description);
            infoLabel.setWrapText(true);
            infoLabel.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px;");

            Label noteLabel = new Label("Place your face in front of the camera, look straight ahead, then press Capture.");
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 12px;");

            VBox content = new VBox(14, infoLabel, previewPane, noteLabel);
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
                drawGuideOverlay(overlay, Color.web("#c44b4b"), "Ready to capture");
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

    public static List<BufferedImage> captureFaceProfile(Stage owner, String title, String description) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("Aucune webcam detectee sur cette machine.");
        }

        Timeline previewTimeline = null;
        List<BufferedImage> capturedFrames = new ArrayList<>();

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

            Canvas overlay = new Canvas(420, 315);
            drawGuideOverlay(overlay, Color.web("#c44b4b"), "Move slightly while staying centered");
            StackPane previewPane = new StackPane(preview, overlay);
            previewPane.setAlignment(Pos.CENTER);

            Label infoLabel = new Label(description);
            infoLabel.setWrapText(true);
            infoLabel.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px;");

            Label noteLabel = new Label("The app will build a stronger biometric profile from several recent frames.");
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 12px;");

            VBox content = new VBox(14, infoLabel, previewPane, noteLabel);
            content.setPadding(new Insets(18));
            content.setStyle("-fx-background-color: #15121a;");

            Deque<BufferedImage> recentFrames = new ArrayDeque<>();
            previewTimeline = new Timeline(new KeyFrame(Duration.millis(120), event -> {
                BufferedImage frame = webcam.getImage();
                if (frame == null) {
                    return;
                }

                BufferedImage snapshot = copy(frame);
                if (recentFrames.size() == SAMPLE_BUFFER_SIZE) {
                    recentFrames.removeFirst();
                }
                recentFrames.addLast(snapshot);

                WritableImage fxImage = SwingFXUtils.toFXImage(frame, null);
                preview.setImage(fxImage);
                drawGuideOverlay(overlay, Color.web("#c44b4b"), "Keep your face inside the frame");
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
            ButtonType enrollType = new ButtonType("Save Face ID");
            dialog.getDialogPane().getButtonTypes().setAll(enrollType, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == enrollType) {
                if (recentFrames.isEmpty()) {
                    throw new IllegalStateException("Impossible de lire une image depuis la webcam.");
                }
                capturedFrames.addAll(selectProfileSamples(recentFrames));
            }
        } finally {
            if (previewTimeline != null) {
                previewTimeline.stop();
            }
            if (webcam.isOpen()) {
                webcam.close();
            }
        }

        return capturedFrames;
    }

    public static <T> T recognizeFace(Stage owner, String title, String description, Function<BufferedImage, RecognitionFeedback<T>> recognizer) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("Aucune webcam detectee sur cette machine.");
        }

        Timeline previewTimeline = null;
        Dialog<ButtonType> dialog = new Dialog<>();
        AtomicReference<T> matchedResult = new AtomicReference<>();
        AtomicReference<Boolean> analyzing = new AtomicReference<>(false);
        AtomicReference<Integer> visibleFrameCount = new AtomicReference<>(0);
        AtomicReference<Integer> consecutiveMatches = new AtomicReference<>(0);

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

            Canvas overlay = new Canvas(420, 315);
            drawGuideOverlay(overlay, Color.web("#c44b4b"), "Scanning...");
            StackPane previewPane = new StackPane(preview, overlay);
            previewPane.setAlignment(Pos.CENTER);

            Label infoLabel = new Label(description);
            infoLabel.setWrapText(true);
            infoLabel.setStyle("-fx-text-fill: #e8e0eb; -fx-font-size: 13px;");

            Label noteLabel = new Label("Place your face in front of the camera. Recognition starts automatically.");
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: #adaaae; -fx-font-size: 12px;");

            VBox content = new VBox(14, infoLabel, previewPane, noteLabel);
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
                visibleFrameCount.set(visibleFrameCount.get() + 1);

                if (Boolean.TRUE.equals(analyzing.get())) {
                    return;
                }

                analyzing.set(true);
                BufferedImage snapshot = copy(frame);
                RecognitionFeedback<T> feedback = recognizer.apply(snapshot);
                if (feedback == null) {
                    feedback = RecognitionFeedback.noFace("Scanning...");
                }

                boolean previewReady = visibleFrameCount.get() >= LOGIN_MIN_VISIBLE_FRAMES;
                drawDetectionOverlay(
                        overlay,
                        feedback.faceBounds(),
                        feedback.overlayColor(),
                        feedback.statusText(),
                        frame.getWidth(),
                        frame.getHeight()
                );

                if (feedback.matchedResult() != null && previewReady) {
                    int matches = consecutiveMatches.get() + 1;
                    consecutiveMatches.set(matches);
                    noteLabel.setText("Possible match detected. Hold still for confirmation.");
                    drawDetectionOverlay(
                            overlay,
                            feedback.faceBounds(),
                            Color.web("#d1a33f"),
                            "Confirming...",
                            frame.getWidth(),
                            frame.getHeight()
                    );

                    if (matches >= LOGIN_REQUIRED_MATCHES) {
                        matchedResult.set(feedback.matchedResult());
                        noteLabel.setText("Face recognized. Opening your vault...");
                        drawDetectionOverlay(
                                overlay,
                                feedback.faceBounds(),
                                Color.web("#31b76a"),
                                "Recognized",
                                frame.getWidth(),
                                frame.getHeight()
                        );
                        dialog.close();
                        return;
                    }
                } else {
                    consecutiveMatches.set(0);
                }

                if (feedback.matchedResult() != null && !previewReady) {
                    noteLabel.setText("Camera ready. Hold still for a moment...");
                    drawDetectionOverlay(
                            overlay,
                            feedback.faceBounds(),
                            Color.web("#d1a33f"),
                            "Warming up...",
                            frame.getWidth(),
                            frame.getHeight()
                    );
                    analyzing.set(false);
                    return;
                }

                if (feedback.matchedResult() != null) {
                    analyzing.set(false);
                    return;
                }

                if (!previewReady) {
                    int remaining = Math.max(0, LOGIN_MIN_VISIBLE_FRAMES - visibleFrameCount.get());
                    noteLabel.setText("Opening camera... hold still.");
                    drawDetectionOverlay(
                            overlay,
                            feedback.faceBounds(),
                            Color.web("#c44b4b"),
                            remaining > 1 ? "Starting..." : "Ready",
                            frame.getWidth(),
                            frame.getHeight()
                    );
                    analyzing.set(false);
                    return;
                }

                noteLabel.setText(feedback.faceDetected()
                        ? "Scanning... keep your face centered and look straight at the camera."
                        : "No face detected. Move closer and face the camera.");
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

    private static List<BufferedImage> selectProfileSamples(Deque<BufferedImage> recentFrames) {
        List<BufferedImage> frames = new ArrayList<>(recentFrames);
        List<BufferedImage> samples = new ArrayList<>();
        int sampleCount = Math.min(PROFILE_SAMPLE_COUNT, frames.size());
        if (sampleCount == 0) {
            return samples;
        }

        if (sampleCount == 1) {
            samples.add(copy(frames.get(frames.size() - 1)));
            return samples;
        }

        for (int i = 0; i < sampleCount; i++) {
            int index = (int) Math.round(i * (frames.size() - 1.0) / (sampleCount - 1.0));
            samples.add(copy(frames.get(index)));
        }
        return samples;
    }

    private static void drawGuideOverlay(Canvas overlay, Color strokeColor, String statusText) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        double width = overlay.getWidth();
        double height = overlay.getHeight();
        double rectWidth = width * 0.48;
        double rectHeight = height * 0.62;
        double x = (width - rectWidth) / 2.0;
        double y = (height - rectHeight) / 2.0;

        gc.clearRect(0, 0, width, height);
        gc.setStroke(strokeColor);
        gc.setLineWidth(4);
        gc.strokeRoundRect(x, y, rectWidth, rectHeight, 18, 18);

        gc.setFill(Color.color(0.0, 0.0, 0.0, 0.32));
        gc.fillRoundRect(x, y + rectHeight + 10, rectWidth, 28, 12, 12);
        gc.setFill(strokeColor);
        gc.fillText(statusText, x + 12, y + rectHeight + 29);
    }

    private static void drawDetectionOverlay(Canvas overlay, Rectangle faceBounds, Color strokeColor, String statusText, int sourceWidth, int sourceHeight) {
        if (faceBounds == null) {
            drawGuideOverlay(overlay, strokeColor, statusText);
            return;
        }

        GraphicsContext gc = overlay.getGraphicsContext2D();
        double width = overlay.getWidth();
        double height = overlay.getHeight();
        double scaleX = width / Math.max(1.0, sourceWidth);
        double scaleY = height / Math.max(1.0, sourceHeight);
        double x = faceBounds.x * scaleX;
        double y = faceBounds.y * scaleY;
        double rectWidth = faceBounds.width * scaleX;
        double rectHeight = faceBounds.height * scaleY;

        gc.clearRect(0, 0, width, height);
        gc.setStroke(strokeColor);
        gc.setLineWidth(4);
        gc.strokeRoundRect(x, y, rectWidth, rectHeight, 18, 18);

        gc.setFill(Color.color(0.0, 0.0, 0.0, 0.32));
        gc.fillRoundRect(x, Math.min(height - 38, y + rectHeight + 10), Math.max(140, rectWidth), 28, 12, 12);
        gc.setFill(strokeColor);
        gc.fillText(statusText, x + 12, Math.min(height - 19, y + rectHeight + 29));
    }

    public record RecognitionFeedback<T>(T matchedResult, Rectangle faceBounds, boolean faceDetected, String statusText, Color overlayColor) {
        public static <T> RecognitionFeedback<T> noFace(String statusText) {
            return new RecognitionFeedback<>(null, null, false, statusText, Color.web("#c44b4b"));
        }

        public static <T> RecognitionFeedback<T> scanning(Rectangle faceBounds, String statusText) {
            return new RecognitionFeedback<>(null, faceBounds, true, statusText, Color.web("#c44b4b"));
        }

        public static <T> RecognitionFeedback<T> matched(T matchedResult, Rectangle faceBounds, String statusText) {
            return new RecognitionFeedback<>(matchedResult, faceBounds, true, statusText, Color.web("#31b76a"));
        }
    }
}
