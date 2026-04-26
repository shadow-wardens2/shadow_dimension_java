package Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class OpenCvFaceAuthUtil {
    private static final int NORMALIZED_FACE_SIZE = 160;
    private static final double MAX_FACE_DISTANCE = 2350.0;

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    private OpenCvFaceAuthUtil() {
    }

    public static String buildFaceSignature(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image faciale invalide.");
        }

        Mat normalized = normalize(image);
        byte[] bytes = new byte[(int) (normalized.total() * normalized.elemSize())];
        normalized.get(0, 0, bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static boolean matches(String storedSignature, String candidateSignature) {
        if (storedSignature == null || storedSignature.isBlank() || candidateSignature == null || candidateSignature.isBlank()) {
            return false;
        }

        Mat stored = decodeSignature(storedSignature);
        Mat candidate = decodeSignature(candidateSignature);

        if (stored.empty() || candidate.empty() || stored.rows() != candidate.rows() || stored.cols() != candidate.cols()) {
            return false;
        }

        double distance = Core.norm(stored, candidate, Core.NORM_L2);
        return distance <= MAX_FACE_DISTANCE;
    }

    private static Mat normalize(BufferedImage image) {
        Mat source = bufferedImageToMat(image);
        if (source.empty()) {
            throw new IllegalArgumentException("Image faciale invalide.");
        }

        Mat grayscale = new Mat();
        if (source.channels() == 1) {
            grayscale = source.clone();
        } else {
            Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGR2GRAY);
        }

        Rect roi = buildCenteredSquare(grayscale);
        Mat cropped = new Mat(grayscale, roi);

        Mat resized = new Mat();
        Imgproc.resize(cropped, resized, new Size(NORMALIZED_FACE_SIZE, NORMALIZED_FACE_SIZE), 0, 0, Imgproc.INTER_AREA);
        Imgproc.equalizeHist(resized, resized);
        Imgproc.GaussianBlur(resized, resized, new Size(3, 3), 0);
        return resized;
    }

    private static Rect buildCenteredSquare(Mat grayscale) {
        int side = Math.min(grayscale.width(), grayscale.height());
        int x = Math.max(0, (grayscale.width() - side) / 2);
        int y = Math.max(0, (grayscale.height() - side) / 2);
        return new Rect(x, y, side, side);
    }

    private static Mat decodeSignature(String signature) {
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
}
