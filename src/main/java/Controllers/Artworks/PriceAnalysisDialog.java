package Controllers.Artworks;

import Entities.Artworks.PriceAnalysis;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A fully custom modal dialog that displays the AI price analysis result:
 * suggested price, per-criterion scores/weights with visual progress bars,
 * and a market insight paragraph.
 */
public class PriceAnalysisDialog {

    private final PriceAnalysis analysis;
    private Integer             result = null; // non-null if user clicks "Appliquer"

    public PriceAnalysisDialog(PriceAnalysis analysis) {
        this.analysis = analysis;
    }

    /**
     * Shows the dialog and blocks until it is closed.
     *
     * @param owner the parent stage (for centering)
     * @return the accepted price, or null if cancelled
     */
    public Integer showAndWait(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Analyse du Prix IA");

        VBox root = buildRoot(dialog);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0a0a14; -fx-background-color: #0a0a14; -fx-border-color: transparent;");

        Scene scene = new Scene(scroll, 520, 640);
        dialog.setScene(scene);
        if (owner != null) dialog.centerOnScreen();
        dialog.showAndWait();
        return result;
    }

    // ------------------------------------------------------------------
    // UI construction
    // ------------------------------------------------------------------

    private VBox buildRoot(Stage dialog) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0a0a14;");

        root.getChildren().addAll(
                buildHeader(dialog),
                buildPriceBadge(),
                buildCriteriaSection(),
                buildInsightSection(),
                buildFooter(dialog)
        );
        return root;
    }

    /** Dark gradient header bar with title and close button. */
    private HBox buildHeader(Stage dialog) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right, #0d0d1f, #1a0a2e);" +
                        "-fx-border-color: #2d1b5e; -fx-border-width: 0 0 1 0;");

        Label title = new Label("💰  ANALYSE DU PRIX IA");
        title.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 15px;" +
                       "-fx-font-weight: bold; -fx-text-fill: #ba9eff;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 14px; -fx-cursor: hand;");
        close.setOnMouseEntered(e -> close.setStyle(close.getStyle().replace("-fx-text-fill: #666", "-fx-text-fill: #ef4444")));
        close.setOnMouseExited(e  -> close.setStyle(close.getStyle().replace("-fx-text-fill: #ef4444", "-fx-text-fill: #666")));
        close.setOnAction(e -> dialog.close());

        header.getChildren().addAll(title, close);
        return header;
    }

    /** Large centred green price badge. */
    private VBox buildPriceBadge() {
        VBox wrapper = new VBox();
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(28, 24, 24, 24));
        wrapper.setStyle("-fx-background-color: #0a0a14;");

        VBox badge = new VBox(4);
        badge.setAlignment(Pos.CENTER);
        badge.setMaxWidth(260);
        badge.setPadding(new Insets(20, 40, 20, 40));
        badge.setStyle("-fx-background-color: linear-gradient(to bottom right, #052e1a, #064e3b);" +
                       "-fx-background-radius: 16;" +
                       "-fx-border-color: #10b981; -fx-border-radius: 16; -fx-border-width: 1.5;" +
                       "-fx-effect: dropshadow(gaussian, #10b98155, 20, 0, 0, 0);");

        Label sub = new Label("Prix Suggéré par l'IA");
        sub.setStyle("-fx-text-fill: #6ee7b7; -fx-font-size: 11px; -fx-font-weight: bold;" +
                     "-fx-letter-spacing: 1;");

        Label price = new Label("$" + analysis.price);
        price.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 36px; -fx-font-weight: bold;");

        Label currency = new Label("USD");
        currency.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px; -fx-font-weight: bold;");

        badge.getChildren().addAll(sub, price, currency);
        wrapper.getChildren().add(badge);
        return wrapper;
    }

    /** Section with all criterion rows. */
    private VBox buildCriteriaSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(0, 24, 20, 24));

        Label heading = new Label("Analyse des Critères");
        heading.setStyle("-fx-text-fill: #ba9eff; -fx-font-size: 13px; -fx-font-weight: bold;" +
                         "-fx-padding: 0 0 4 0;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2d1b5e;");

        section.getChildren().addAll(heading, sep);

        for (PriceAnalysis.Criterion c : analysis.criteria) {
            section.getChildren().add(buildCriterionCard(c));
        }
        return section;
    }

    /** One criterion card: name, weight pill, score chip, custom progress bar, note. */
    private VBox buildCriterionCard(PriceAnalysis.Criterion c) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle("-fx-background-color: #12121e; -fx-background-radius: 10;" +
                      "-fx-border-color: #1e1e35; -fx-border-radius: 10; -fx-border-width: 1;");

        // ── Row 1: name + weight pill + score chip ──────────────────────
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(c.name);
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px;");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label weightPill = new Label("Poids : " + c.weight + "%");
        weightPill.setStyle("-fx-background-color: #1e1235; -fx-text-fill: #a78bfa;" +
                            "-fx-background-radius: 20; -fx-padding: 2 8;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;");

        String scoreColor = scoreColor(c.score);
        Label scorePill = new Label("Score : " + c.score + "/100");
        scorePill.setStyle("-fx-background-color: " + scoreBg(c.score) + "; -fx-text-fill: " + scoreColor + ";" +
                           "-fx-background-radius: 20; -fx-padding: 2 8;" +
                           "-fx-font-size: 10px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(name, weightPill, scorePill);

        // ── Row 2: custom progress bar ───────────────────────────────────
        StackPane track = new StackPane();
        track.setStyle("-fx-background-color: #1e1e35; -fx-background-radius: 4;");
        track.setPrefHeight(8);
        track.setMaxHeight(8);
        track.setMinHeight(8);

        Region fill = new Region();
        fill.setStyle("-fx-background-color: " + scoreColor + "; -fx-background-radius: 4;");
        fill.setPrefHeight(8);
        fill.setMaxHeight(8);
        fill.setMinHeight(8);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        // Bind fill width to track width once layout is done
        final double ratio = c.score / 100.0;
        track.widthProperty().addListener((obs, o, w) -> fill.setPrefWidth(w.doubleValue() * ratio));

        // ── Row 3: score % label ────────────────────────────────────────
        HBox barRow = new HBox(8);
        barRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(track, Priority.ALWAYS);
        Label pct = new Label(c.score + "%");
        pct.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 32;");
        barRow.getChildren().addAll(track, pct);

        // ── Row 4: note text ─────────────────────────────────────────────
        Label note = new Label(c.note);
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #888aaa; -fx-font-size: 11px;");

        card.getChildren().addAll(topRow, barRow, note);
        return card;
    }

    /** Market insight paragraph with a warm amber tone. */
    private VBox buildInsightSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(0, 24, 24, 24));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2d1b5e;");

        Label heading = new Label("💡  Insight Marché");
        heading.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox insightCard = new VBox();
        insightCard.setPadding(new Insets(14));
        insightCard.setStyle("-fx-background-color: #151008; -fx-background-radius: 10;" +
                             "-fx-border-color: #92400e; -fx-border-radius: 10; -fx-border-width: 1;");

        Label text = new Label(analysis.marketInsight);
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: #d4b896; -fx-font-size: 12px; -fx-line-spacing: 4;");
        insightCard.getChildren().add(text);

        section.getChildren().addAll(sep, heading, insightCard);
        return section;
    }

    /** Footer with "Appliquer" and "Annuler" buttons. */
    private HBox buildFooter(Stage dialog) {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));
        footer.setStyle("-fx-background-color: #0d0d1f; -fx-border-color: #2d1b5e; -fx-border-width: 1 0 0 0;");

        Button cancel = new Button("✕  Annuler");
        cancel.setStyle("-fx-background-color: #1e1e2e; -fx-text-fill: #888; -fx-font-size: 12px;" +
                        "-fx-padding: 9 20; -fx-background-radius: 8; -fx-cursor: hand;");
        cancel.setOnMouseEntered(e -> cancel.setStyle(cancel.getStyle().replace("#888", "#fff")));
        cancel.setOnMouseExited(e  -> cancel.setStyle(cancel.getStyle().replace("#fff", "#888")));
        cancel.setOnAction(e -> dialog.close());

        Button apply = new Button("✓  Appliquer  $" + analysis.price);
        apply.setStyle("-fx-background-color: linear-gradient(to right, #065f46, #10b981);" +
                       "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;" +
                       "-fx-padding: 9 22; -fx-background-radius: 8; -fx-cursor: hand;" +
                       "-fx-effect: dropshadow(gaussian, #10b98166, 10, 0, 0, 2);");
        apply.setOnAction(e -> {
            result = analysis.price;
            dialog.close();
        });

        footer.getChildren().addAll(cancel, apply);
        return footer;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String scoreColor(int score) {
        if (score >= 80) return "#10b981"; // emerald
        if (score >= 60) return "#f59e0b"; // amber
        if (score >= 40) return "#f97316"; // orange
        return "#ef4444";                  // red
    }

    private String scoreBg(int score) {
        if (score >= 80) return "#052e1a";
        if (score >= 60) return "#1c1200";
        if (score >= 40) return "#1c0a00";
        return "#1c0000";
    }
}
