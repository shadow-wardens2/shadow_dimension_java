package Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class VoiceToTextUtil {
    private static final String NO_RECOGNIZER_TOKEN = "__VOICE_NO_RECOGNIZER__";

    private VoiceToTextUtil() {
    }

    public static String recognizeOnce(int timeoutSeconds) throws IOException, InterruptedException {
        if (!isWindows()) {
            return "";
        }

        String script = "$ErrorActionPreference='Stop';"
                + "Add-Type -AssemblyName System.Speech;"
                + "$recognizers=[System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers();"
                + "if(-not $recognizers -or $recognizers.Count -eq 0){Write-Output '" + NO_RECOGNIZER_TOKEN + "'; exit 0};"
                + "$recognizerInfo=$recognizers | Select-Object -First 1;"
                + "$r=New-Object System.Speech.Recognition.SpeechRecognitionEngine($recognizerInfo);"
                + "$r.InitialSilenceTimeout=[TimeSpan]::FromSeconds(4);"
                + "$r.BabbleTimeout=[TimeSpan]::FromSeconds(2);"
                + "$r.EndSilenceTimeout=[TimeSpan]::FromSeconds(1);"
                + "$r.EndSilenceTimeoutAmbiguous=[TimeSpan]::FromSeconds(1);"
                + "$r.SetInputToDefaultAudioDevice();"
                + "$r.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar));"
                + "$res=$r.Recognize([TimeSpan]::FromSeconds(" + timeoutSeconds + "));"
                + "if($res -and $res.Text){Write-Output $res.Text}";

        ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-STA", "-Command", script);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    output.append(line).append(' ');
                }
            }
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    errorOutput.append(line).append(' ');
                }
            }
        }

        int exitCode = process.waitFor();
        String recognized = output.toString().trim();
        if (recognized.equals(NO_RECOGNIZER_TOKEN)) {
            throw new IOException("No Windows speech recognizer is installed on this machine.");
        }
        if (exitCode != 0) {
            String detail = errorOutput.toString().trim();
            if (detail.isBlank()) {
                detail = "Speech recognition process failed.";
            }
            throw new IOException(detail);
        }

        return recognized;
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }
}
