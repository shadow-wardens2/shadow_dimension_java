package Controllers.event;

// Ordered map implementation used for deterministic error ordering.
import java.util.LinkedHashMap;
// Map interface for field->error messages.
import java.util.Map;

// Centralized validation logic for category add/edit forms.
public final class CategoryFormValidator {

    // Error-map key for category name field.
    public static final String FIELD_NOM = "nom";
    // Error-map key for description field.
    public static final String FIELD_DESCRIPTION = "description";
    // Error-map key for pricing type field.
    public static final String FIELD_TARIFICATION = "tarification";
    // Error-map key for price field.
    public static final String FIELD_PRIX = "prix";

    // Private constructor because this is a utility class.
    private CategoryFormValidator() {
    }

    // Validates raw category form values and returns normalized result + errors.
    public static Result validate(String nom, String description, String tarification, String prixText) {
        // Holds all field-specific error messages.
        Map<String, String> errors = new LinkedHashMap<>();

        // Normalizes text inputs by trimming null-safe values.
        String cleanNom = safeTrim(nom);
        String cleanDescription = safeTrim(description);
        String cleanTarification = safeTrim(tarification);
        String cleanPrixText = safeTrim(prixText);

        // Name is mandatory.
        if (cleanNom.isEmpty()) {
            errors.put(FIELD_NOM, "Le nom est obligatoire.");
        }
        // Description is mandatory.
        if (cleanDescription.isEmpty()) {
            errors.put(FIELD_DESCRIPTION, "La description est obligatoire.");
        }
        // Pricing type is mandatory.
        if (cleanTarification.isEmpty()) {
            errors.put(FIELD_TARIFICATION, "Le type de tarification est obligatoire.");
        }

        // Price stays null for FREE categories by design.
        Double prix = null;
        // Additional checks only for PAID categories.
        if ("PAID".equals(cleanTarification)) {
            // Price is required when category is paid.
            if (cleanPrixText.isEmpty()) {
                errors.put(FIELD_PRIX, "Le prix est obligatoire pour une categorie payante.");
            } else {
                try {
                    // Parses numeric price value.
                    prix = Double.parseDouble(cleanPrixText);
                    // Price must be strictly positive.
                    if (prix <= 0) {
                        errors.put(FIELD_PRIX, "Le prix doit etre superieur a 0.");
                    }
                } catch (NumberFormatException e) {
                    // Handles malformed number values.
                    errors.put(FIELD_PRIX, "Le prix doit etre un nombre valide.");
                }
            }
        }

        // Returns immutable validation result wrapper.
        return new Result(errors, cleanNom, cleanDescription, cleanTarification, prix);
    }

    // Utility to null-safe trim incoming text fields.
    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    // Value object carrying normalized fields and validation errors.
    public static final class Result {
        // All errors indexed by field keys.
        private final Map<String, String> errors;
        // Normalized category name.
        private final String nom;
        // Normalized description.
        private final String description;
        // Normalized pricing type.
        private final String tarification;
        // Parsed price (null when not applicable).
        private final Double prix;

        // Constructor storing computed validation outcome.
        private Result(Map<String, String> errors, String nom, String description, String tarification, Double prix) {
            this.errors = errors;
            this.nom = nom;
            this.description = description;
            this.tarification = tarification;
            this.prix = prix;
        }

        // True when no validation error exists.
        public boolean isValid() {
            return errors.isEmpty();
        }

        // Returns field->message validation map.
        public Map<String, String> getErrors() {
            return errors;
        }

        // Returns normalized name value.
        public String getNom() {
            return nom;
        }

        // Returns normalized description value.
        public String getDescription() {
            return description;
        }

        // Returns normalized pricing type value.
        public String getTarification() {
            return tarification;
        }

        // Returns parsed/validated price value.
        public Double getPrix() {
            return prix;
        }
    }
}
