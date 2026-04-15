package Controllers.event;

import Entities.event.Category;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EventFormValidator {

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_IMAGE = "image";
    public static final String FIELD_CAPACITY = "capacity";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_LOCATION_TYPE = "locationType";
    public static final String FIELD_FORM = "form";

    private EventFormValidator() {
    }

    public static Result validate(
            String title,
            String description,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String image,
            String capacityText,
            Category category,
            String status,
            String locationType,
            boolean hasLoggedUser
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        String cleanTitle = safeTrim(title);
        String cleanDescription = safeTrim(description);
        String cleanLocation = safeTrim(location);
        String cleanImage = safeTrim(image);
        String cleanCapacityText = safeTrim(capacityText);
        String cleanStatus = safeTrim(status);
        String cleanLocationType = safeTrim(locationType);

        if (cleanTitle.isEmpty()) {
            errors.put(FIELD_TITLE, "Le titre est obligatoire.");
        }
        if (cleanDescription.isEmpty()) {
            errors.put(FIELD_DESCRIPTION, "La description est obligatoire.");
        }
        if (cleanLocation.isEmpty()) {
            errors.put(FIELD_LOCATION, "La localisation est obligatoire.");
        }
        if (startDate == null) {
            errors.put(FIELD_START_DATE, "La date de debut est obligatoire.");
        }
        if (endDate == null) {
            errors.put(FIELD_END_DATE, "La date de fin est obligatoire.");
        }
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            errors.put(FIELD_END_DATE, "La date de fin doit etre apres la date de debut.");
        }
        if (cleanImage.isEmpty()) {
            errors.put(FIELD_IMAGE, "L'image est obligatoire.");
        }
        if (cleanCapacityText.isEmpty()) {
            errors.put(FIELD_CAPACITY, "La capacite est obligatoire.");
        }
        if (category == null) {
            errors.put(FIELD_CATEGORY, "La categorie est obligatoire.");
        }
        if (cleanStatus.isEmpty()) {
            errors.put(FIELD_STATUS, "Le status est obligatoire.");
        }
        if (cleanLocationType.isEmpty()) {
            errors.put(FIELD_LOCATION_TYPE, "Le type de lieu est obligatoire.");
        }
        if (!hasLoggedUser) {
            errors.put(FIELD_FORM, "Aucun utilisateur connecte.");
        }

        int capacity = -1;
        if (!cleanCapacityText.isEmpty()) {
            try {
                capacity = Integer.parseInt(cleanCapacityText);
                if (capacity <= 0) {
                    errors.put(FIELD_CAPACITY, "La capacite doit etre superieure a 0.");
                }
            } catch (NumberFormatException ex) {
                errors.put(FIELD_CAPACITY, "La capacite doit etre un nombre entier.");
            }
        }

        return new Result(
                errors,
                cleanTitle,
                cleanDescription,
                cleanLocation,
                startDate,
                endDate,
                cleanImage,
                capacity,
                category,
                cleanStatus,
                cleanLocationType
        );
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        private final Map<String, String> errors;
        private final String title;
        private final String description;
        private final String location;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String image;
        private final int capacity;
        private final Category category;
        private final String status;
        private final String locationType;

        private Result(
                Map<String, String> errors,
                String title,
                String description,
                String location,
                LocalDate startDate,
                LocalDate endDate,
                String image,
                int capacity,
                Category category,
                String status,
                String locationType
        ) {
            this.errors = errors;
            this.title = title;
            this.description = description;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.image = image;
            this.capacity = capacity;
            this.category = category;
            this.status = status;
            this.locationType = locationType;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation() {
            return location;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getImage() {
            return image;
        }

        public int getCapacity() {
            return capacity;
        }

        public Category getCategory() {
            return category;
        }

        public String getStatus() {
            return status;
        }

        public String getLocationType() {
            return locationType;
        }
    }
}
