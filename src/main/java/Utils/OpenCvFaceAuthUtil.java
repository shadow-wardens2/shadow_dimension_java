package Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class OpenCvFaceAuthUtil {
    private static final String DETECTION_MODEL_RESOURCE = "models/face/face_detection_yunet_2023mar.onnx";
    private static final String RECOGNITION_MODEL_RESOURCE = "models/face/face_recognition_sface_2021dec.onnx";
    private static final String SFACE_PREFIX = "SFACE_V1:";
    private static final double SFACE_COSINE_THRESHOLD = 0.35;

    private static final int NORMALIZED_FACE_SIZE = 160;
    private static final double LEGACY_MAX_FACE_DISTANCE = 3200.0;

    private static volatile FaceDetectorYN detector;
    private static volatile FaceRecognizerSF recognizer;

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    private OpenCvFaceAuthUtil() {
    }

    public static String buildFaceSignature(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image faciale invalide.");
        }
        return buildFaceSignature(List.of(image));
    }

    public static String buildFaceSignature(List<BufferedImage> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Aucun echantillon facial fourni.");
        }

        List<Mat> features = new ArrayList<>();
        for (BufferedImage image : images) {
            if (image == null) {
                continue;
            }

            FaceAnalysis analysis = analyzeFace(image);
            if (analysis.faceDetected()) {
                features.add(analysis.feature());
            }
        }

        if (features.isEmpty()) {
            throw new IllegalArgumentException("Aucun visage detecte dans les captures fournies.");
        }

        Mat averagedFeature = averageFeatures(features);
        return encodeSFaceSignature(averagedFeature);
    }

    public static FaceAnalysis analyzeFace(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image faciale invalide.");
        }

        Mat source = bufferedImageToMat(image);
        if (source.empty()) {
            throw new IllegalArgumentException("Image faciale invalide.");
        }

        ensureModelsLoaded();

        FaceDetectorYN faceDetector = detector;
        faceDetector.setInputSize(source.size());

        Mat faces = new Mat();
        faceDetector.detect(source, faces);
        if (faces.empty() || faces.rows() == 0) {
            return FaceAnalysis.noFace();
        }

        Mat bestFace = selectBestFace(faces);
        Rectangle bounds = toRectangle(bestFace);

        Mat aligned = new Mat();
        recognizer.alignCrop(source, bestFace, aligned);

        Mat feature = new Mat();
        recognizer.feature(aligned, feature);
        if (feature.empty()) {
            return FaceAnalysis.noFace();
        }

        Mat normalizedFeature = new Mat();
        Core.normalize(feature, normalizedFeature);
        return new FaceAnalysis(bounds, normalizedFeature);
    }

    public static MatchResult matchStoredSignature(String storedSignature, FaceAnalysis analysis, BufferedImage fallbackImage) {
        if (storedSignature == null || storedSignature.isBlank()) {
            return MatchResult.noMatch();
        }

        if (analysis == null || !analysis.faceDetected()) {
            if (isSFaceSignature(storedSignature)) {
                return MatchResult.noMatch();
            }
            return legacyMatches(storedSignature, fallbackImage)
                    ? new MatchResult(true, 0.40)
                    : MatchResult.noMatch();
        }

        if (isSFaceSignature(storedSignature)) {
            Mat storedFeature = decodeSFaceSignature(storedSignature);
            if (storedFeature.empty()) {
                return MatchResult.noMatch();
            }

            double similarity = recognizer.match(storedFeature, analysis.feature(), FaceRecognizerSF.FR_COSINE);
            return new MatchResult(similarity >= SFACE_COSINE_THRESHOLD, similarity);
        }

        return legacyMatches(storedSignature, fallbackImage)
                ? new MatchResult(true, 0.40)
                : MatchResult.noMatch();
    }

    public static boolean matches(String storedSignature, BufferedImage candidateImage) {
        FaceAnalysis analysis = null;
        try {
            analysis = analyzeFace(candidateImage);
        } catch (RuntimeException ignored) {
            // Fall back to legacy comparison below when possible.
        }

        return matchStoredSignature(storedSignature, analysis, candidateImage).matched();
    }

    public static boolean matches(String storedSignature, String candidateSignature) {
        if (storedSignature == null || storedSignature.isBlank() || candidateSignature == null || candidateSignature.isBlank()) {
            return false;
        }

        if (isSFaceSignature(storedSignature) && isSFaceSignature(candidateSignature)) {
            ensureModelsLoaded();
            Mat storedFeature = decodeSFaceSignature(storedSignature);
            Mat candidateFeature = decodeSFaceSignature(candidateSignature);
            if (storedFeature.empty() || candidateFeature.empty()) {
                return false;
            }
            double similarity = recognizer.match(storedFeature, candidateFeature, FaceRecognizerSF.FR_COSINE);
            return similarity >= SFACE_COSINE_THRESHOLD;
        }

        Mat stored = decodeLegacySignature(storedSignature);
        Mat candidate = decodeLegacySignature(candidateSignature);
        return legacyCompare(stored, candidate);
    }

    private static boolean isSFaceSignature(String storedSignature) {
        return storedSignature != null && storedSignature.startsWith(SFACE_PREFIX);
    }

    private static String encodeSFaceSignature(Mat feature) {
        float[] values = new float[(int) (feature.total() * feature.channels())];
        feature.get(0, 0, values);

        ByteBuffer buffer = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) {
            buffer.putFloat(value);
        }

        String payload = Base64.getEncoder().encodeToString(buffer.array());
        return SFACE_PREFIX + feature.rows() + ":" + feature.cols() + ":" + feature.type() + ":" + payload;
    }

    private static Mat decodeSFaceSignature(String signature) {
        try {
            String body = signature.substring(SFACE_PREFIX.length());
            String[] parts = body.split(":", 4);
            if (parts.length != 4) {
                return new Mat();
            }

            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            int type = Integer.parseInt(parts[2]);
            byte[] payload = Base64.getDecoder().decode(parts[3]);
            ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

            int valueCount = payload.length / Float.BYTES;
            float[] values = new float[valueCount];
            for (int i = 0; i < valueCount; i++) {
                values[i] = buffer.getFloat();
            }

            Mat decoded = new Mat(rows, cols, type);
            decoded.put(0, 0, values);
            return decoded;
        } catch (RuntimeException ex) {
            return new Mat();
        }
    }

    private static Mat averageFeatures(List<Mat> features) {
        Mat accumulator = Mat.zeros(features.get(0).rows(), features.get(0).cols(), CvType.CV_32FC1);

        for (Mat feature : features) {
            Mat feature32 = new Mat();
            feature.convertTo(feature32, CvType.CV_32FC1);
            Core.add(accumulator, feature32, accumulator);
        }

        Core.multiply(
                accumulator,
                new Mat(accumulator.size(), accumulator.type(), new Scalar(1.0 / features.size())),
                accumulator
        );

        Mat normalizedAverage = new Mat();
        Core.normalize(accumulator, normalizedAverage);
        return normalizedAverage;
    }

    private static Mat selectBestFace(Mat faces) {
        int bestRow = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        float[] faceData = new float[15];

        for (int row = 0; row < faces.rows(); row++) {
            faces.row(row).get(0, 0, faceData);
            float score = faceData[14];
            if (score > bestScore) {
                bestScore = score;
                bestRow = row;
            }
        }

        return faces.row(bestRow).clone();
    }

    private static Rectangle toRectangle(Mat faceRow) {
        float[] values = new float[15];
        faceRow.get(0, 0, values);

        int x = Math.max(0, Math.round(values[0]));
        int y = Math.max(0, Math.round(values[1]));
        int width = Math.max(1, Math.round(values[2]));
        int height = Math.max(1, Math.round(values[3]));
        return new Rectangle(x, y, width, height);
    }

    private static void ensureModelsLoaded() {
        if (detector != null && recognizer != null) {
            return;
        }

        synchronized (OpenCvFaceAuthUtil.class) {
            if (detector != null && recognizer != null) {
                return;
            }

            String detectorPath = resolveModelPath(DETECTION_MODEL_RESOURCE);
            String recognizerPath = resolveModelPath(RECOGNITION_MODEL_RESOURCE);

            detector = FaceDetectorYN.create(detectorPath, "", new Size(320, 320), 0.8f, 0.3f, 5000);
            recognizer = FaceRecognizerSF.create(recognizerPath, "");
        }
    }

    private static String resolveModelPath(String resourcePath) {
        try {
            URL resource = OpenCvFaceAuthUtil.class.getClassLoader().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Modele biometrique introuvable: " + resourcePath);
            }

            if ("file".equalsIgnoreCase(resource.getProtocol())) {
                return Path.of(resource.toURI()).toAbsolutePath().toString();
            }

            byte[] bytes = resource.openStream().readAllBytes();
            Path tempFile = Files.createTempFile("shadow-face-model-", "-" + Path.of(resourcePath).getFileName());
            Files.write(tempFile, bytes);
            tempFile.toFile().deleteOnExit();
            return tempFile.toAbsolutePath().toString();
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Impossible de charger le modele biometrique: " + resourcePath, ex);
        }
    }

    private static boolean legacyMatches(String storedSignature, BufferedImage candidateImage) {
        if (storedSignature == null || storedSignature.isBlank() || candidateImage == null) {
            return false;
        }

        Mat stored = decodeLegacySignature(storedSignature);
        if (stored.empty()) {
            return false;
        }

        for (Mat candidate : buildLegacyCandidateNormalizations(candidateImage)) {
            if (legacyCompare(stored, candidate)) {
                return true;
            }
        }

        return false;
    }

    private static List<Mat> buildLegacyCandidateNormalizations(BufferedImage image) {
        Mat source = bufferedImageToMat(image);
        if (source.empty()) {
            throw new IllegalArgumentException("Image faciale invalide.");
        }

        Mat grayscale = new Mat();
        if (source.channels() == 1) {
            grayscale = source.clone();
        } else {
            org.opencv.imgproc.Imgproc.cvtColor(source, grayscale, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY);
        }

        List<Mat> candidates = new ArrayList<>();
        for (Rect roi : buildLegacyCandidateSquares(grayscale)) {
            Mat cropped = new Mat(grayscale, roi);

            Mat resized = new Mat();
            org.opencv.imgproc.Imgproc.resize(cropped, resized, new Size(NORMALIZED_FACE_SIZE, NORMALIZED_FACE_SIZE), 0, 0, org.opencv.imgproc.Imgproc.INTER_AREA);
            org.opencv.imgproc.Imgproc.equalizeHist(resized, resized);
            org.opencv.imgproc.Imgproc.GaussianBlur(resized, resized, new Size(3, 3), 0);
            candidates.add(resized);
        }
        return candidates;
    }

    private static List<Rect> buildLegacyCandidateSquares(Mat grayscale) {
        int maxSide = Math.min(grayscale.width(), grayscale.height());
        List<Rect> rois = new ArrayList<>();
        double[] scaleFactors = {1.0, 0.92, 0.84};
        double[] shiftFactors = {0.0, -0.08, 0.08};

        for (double scaleFactor : scaleFactors) {
            int side = Math.max(32, (int) Math.round(maxSide * scaleFactor));
            int baseX = Math.max(0, (grayscale.width() - side) / 2);
            int baseY = Math.max(0, (grayscale.height() - side) / 2);
            int shift = Math.max(4, (int) Math.round(side * 0.08));

            for (double shiftXFactor : shiftFactors) {
                for (double shiftYFactor : shiftFactors) {
                    int x = clamp(baseX + (int) Math.round(shift * shiftXFactor / 0.08), 0, grayscale.width() - side);
                    int y = clamp(baseY + (int) Math.round(shift * shiftYFactor / 0.08), 0, grayscale.height() - side);
                    rois.add(new Rect(x, y, side, side));
                }
            }
        }

        return rois;
    }

    private static boolean legacyCompare(Mat stored, Mat candidate) {
        if (stored.empty() || candidate.empty() || stored.rows() != candidate.rows() || stored.cols() != candidate.cols()) {
            return false;
        }

        double distance = Core.norm(stored, candidate, Core.NORM_L2);
        return distance <= LEGACY_MAX_FACE_DISTANCE;
    }

    private static Mat decodeLegacySignature(String signature) {
        byte[] bytes = Base64.getDecoder().decode(signature);
        if (bytes.length != NORMALIZED_FACE_SIZE * NORMALIZED_FACE_SIZE) {
            return new Mat();
        }

        Mat mat = new Mat(NORMALIZED_FACE_SIZE, NORMALIZED_FACE_SIZE, CvType.CV_8UC1);
        mat.put(0, 0, bytes);
        return mat;
    }

    private static Mat bufferedImageToMat(BufferedImage image) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            MatOfByte mob = new MatOfByte(outputStream.toByteArray());
            return Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de traiter l'image de la webcam.", e);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record FaceAnalysis(Rectangle faceBounds, Mat feature) {
        public static FaceAnalysis noFace() {
            return new FaceAnalysis(null, new Mat());
        }

        public boolean faceDetected() {
            return faceBounds != null && feature != null && !feature.empty();
        }
    }

    public record MatchResult(boolean matched, double score) {
        public static MatchResult noMatch() {
            return new MatchResult(false, Double.NEGATIVE_INFINITY);
        }
    }
}
