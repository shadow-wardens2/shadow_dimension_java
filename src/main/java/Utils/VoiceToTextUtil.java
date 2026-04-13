package Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class VoiceToTextUtil {

    private VoiceToTextUtil() {
    }

    public static String recognizeOnce(int timeoutSeconds) throws IOException, InterruptedException {
        if (!isWindows()) {
            return "";
        }

        String script = "$ErrorActionPreference='SilentlyContinue';"
                + "Add-Type -AssemblyName System.Speech;"
                + "$r=New-Object System.Speech.Recognition.SpeechRecognitionEngine;"
                + "$r.SetInputToDefaultAudioDevice();"
                + "$r.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar));"
                + "$res=$r.Recognize([TimeSpan]::FromSeconds(" + timeoutSeconds + "));"
                + "if($res){$res.Text}";

        ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", script);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    output.append(line).append(' ');
                }
            }
        }

        process.waitFor();
        return output.toString().trim();
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }
}
