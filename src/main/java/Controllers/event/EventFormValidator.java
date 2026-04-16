package Controllers.event;

// Category model used in validation result payload.
import Entities.event.Category;

// Date type used by event start/end fields.
import java.time.LocalDate;
// Ordered map implementation for stable error ordering.
import java.util.LinkedHashMap;
// Map interface for field-specific error messages.
import java.util.Map;

// Centralized validator for Event add/edit forms.
public final class EventFormValidator {

    // Error-map key for title field.
    public static final String FIELD_TITLE = "title";
    // Error-map key for description field.
    public static final String FIELD_DESCRIPTION = "description";
    // Error-map key for location field.
    public static final String FIELD_LOCATION = "location";
    // Error-map key for start-date field.
    public static final String FIELD_START_DATE = "startDate";
    // Error-map key for end-date field.
    public static final String FIELD_END_DATE = "endDate";
    // Error-map key for image field.
    public static final String FIELD_IMAGE = "image";
    // Error-map key for capacity field.
    public static final String FIELD_CAPACITY = "capacity";
    // Error-map key for category selection.
    public static final String FIELD_CATEGORY = "category";
    // Error-map key for status selection.
    public static final String FIELD_STATUS = "status";
    // Error-map key for location type selection.
    public static final String FIELD_LOCATION_TYPE = "locationType";
    // Error-map key for global form-level errors.
    public static final String FIELD_FORM = "form";

    // Private constructor because this is a pure utility class.
    private EventFormValidator() {
    }

    // Validates raw event form values and returns normalized result with errors.
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
        // Holds all field-level errors.
        Map<String, String> errors = new LinkedHashMap<>();

        // Normalizes all text values once for downstream checks.
        String cleanTitle = safeTrim(title);
        String cleanDescription = safeTrim(description);
        String cleanLocation = safeTrim(location);
        String cleanImage = safeTrim(image);
        String cleanCapacityText = safeTrim(capacityText);
        String cleanStatus = safeTrim(status);
        String cleanLocationType = safeTrim(locationType);

        // Title is required.
        if (cleanTitle.isEmpty()) {
            errors.put(FIELD_TITLE, "Le titre est obligatoire.");
        }
        // Description is required.
        if (cleanDescription.isEmpty()) {
            errors.put(FIELD_DESCRIPTION, "La description est obligatoire.");
        }
        // Location is required.
        if (cleanLocation.isEmpty()) {
            errors.put(FIELD_LOCATION, "La localisation est obligatoire.");
        }
        // Start date is required.
        if (startDate == null) {
            errors.put(FIELD_START_DATE, "La date de debut est obligatoire.");
        }
        // End date is required.
        if (endDate == null) {
            errors.put(FIELD_END_DATE, "La date de fin est obligatoire.");
        }
        // End date must be after start date.
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            errors.put(FIELD_END_DATE, "La date de fin doit etre apres la date de debut.");
        }
        // Image is required.
        if (cleanImage.isEmpty()) {
            errors.put(FIELD_IMAGE, "L'image est obligatoire.");
        }
        // Capacity text is required.
        if (cleanCapacityText.isEmpty()) {
            errors.put(FIELD_CAPACITY, "La capacite est obligatoire.");
        }
        // Category selection is required.
        if (category == null) {
            errors.put(FIELD_CATEGORY, "La categorie est obligatoire.");
        }
        // Status selection is required.
        if (cleanStatus.isEmpty()) {
            errors.put(FIELD_STATUS, "Le status est obligatoire.");
        }
        // Location type selection is required.
        if (cleanLocationType.isEmpty()) {
            errors.put(FIELD_LOCATION_TYPE, "Le type de lieu est obligatoire.");
        }
        // Logged-in user is required for createdBy linkage.
        if (!hasLoggedUser) {
            errors.put(FIELD_FORM, "Aucun utilisateur connecte.");
        }

        // Parsed capacity value; remains -1 when not parseable.
        int capacity = -1;
        // Parses only if user provided some value.
        if (!cleanCapacityText.isEmpty()) {
            try {
                // Parses integer capacity.
                capacity = Integer.parseInt(cleanCapacityText);
                // Ensures strictly positive capacity.
                if (capacity <= 0) {
                    errors.put(FIELD_CAPACITY, "La capacite doit etre superieure a 0.");
                }
            } catch (NumberFormatException ex) {
                // Handles non-numeric input.
                errors.put(FIELD_CAPACITY, "La capacite doit etre un nombre entier.");
            }
        }

        // Returns immutable result with errors + normalized values.
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

    // Null-safe trim helper for raw text fields.
    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    // Immutable object that carries validation outcome and normalized values.
    public static final class Result {
        // Field-level error map.
        private final Map<String, String> errors;
        // Normalized title.
        private final String title;
        // Normalized description.
        private final String description;
        // Normalized location.
        private final String location;
        // Start date value.
        private final LocalDate startDate;
        // End date value.
        private final LocalDate endDate;
        // Normalized image string.
        private final String image;
        // Parsed capacity value.
        private final int capacity;
        // Selected category object.
        private final Category category;
        // Normalized status value.
        private final String status;
        // Normalized location type value.
        private final String locationType;

        // Constructor used internally by validate().
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

        // True when validation passed without errors.
        public boolean isValid() {
            return errors.isEmpty();
        }

        // Returns field-level error map.
        public Map<String, String> getErrors() {
            return errors;
        }

        // Returns normalized title.
        public String getTitle() {
            return title;
        }

        // Returns normalized description.
        public String getDescription() {
            return description;
        }

        // Returns normalized location.
        public String getLocation() {
            return location;
        }

        // Returns validated start date.
        public LocalDate getStartDate() {
            return startDate;
        }

        // Returns validated end date.
        public LocalDate getEndDate() {
            return endDate;
        }

        // Returns normalized image string.
        public String getImage() {
            return image;
        }

        // Returns parsed capacity integer.
        public int getCapacity() {
            return capacity;
        }

        // Returns selected category.
        public Category getCategory() {
            return category;
        }

        // Returns normalized status.
        public String getStatus() {
            return status;
        }

        // Returns normalized location type.
        public String getLocationType() {
            return locationType;
        }
    }
}
