package Controllers.event;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CategoryFormValidator {

    public static final String FIELD_NOM = "nom";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_TARIFICATION = "tarification";
    public static final String FIELD_PRIX = "prix";

    private CategoryFormValidator() {
    }

    public static Result validate(String nom, String description, String tarification, String prixText) {
        Map<String, String> errors = new LinkedHashMap<>();

        String cleanNom = safeTrim(nom);
        String cleanDescription = safeTrim(description);
        String cleanTarification = safeTrim(tarification);
        String cleanPrixText = safeTrim(prixText);

        if (cleanNom.isEmpty()) {
            errors.put(FIELD_NOM, "Le nom est obligatoire.");
        }
        if (cleanDescription.isEmpty()) {
            errors.put(FIELD_DESCRIPTION, "La description est obligatoire.");
        }
        if (cleanTarification.isEmpty()) {
            errors.put(FIELD_TARIFICATION, "Le type de tarification est obligatoire.");
        }

        Double prix = null;
        if ("PAID".equals(cleanTarification)) {
            if (cleanPrixText.isEmpty()) {
                errors.put(FIELD_PRIX, "Le prix est obligatoire pour une categorie payante.");
            } else {
                try {
                    prix = Double.parseDouble(cleanPrixText);
                    if (prix <= 0) {
                        errors.put(FIELD_PRIX, "Le prix doit etre superieur a 0.");
                    }
                } catch (NumberFormatException e) {
                    errors.put(FIELD_PRIX, "Le prix doit etre un nombre valide.");
                }
            }
        }

        return new Result(errors, cleanNom, cleanDescription, cleanTarification, prix);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        private final Map<String, String> errors;
        private final String nom;
        private final String description;
        private final String tarification;
        private final Double prix;

        private Result(Map<String, String> errors, String nom, String description, String tarification, Double prix) {
            this.errors = errors;
            this.nom = nom;
            this.description = description;
            this.tarification = tarification;
            this.prix = prix;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public String getNom() {
            return nom;
        }

        public String getDescription() {
            return description;
        }

        public String getTarification() {
            return tarification;
        }

        public Double getPrix() {
            return prix;
        }
    }
}
