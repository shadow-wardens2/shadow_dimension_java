package Utils;

import Entities.event.Category;
import Entities.event.Event;

public final class EventNavigationState {

    private static Event editingEvent;
    private static Category editingCategory;

    private EventNavigationState() {
    }

    public static Event getEditingEvent() {
        return editingEvent;
    }

    public static void setEditingEvent(Event event) {
        editingEvent = event;
    }

    public static void clearEditingEvent() {
        editingEvent = null;
    }

    public static Category getEditingCategory() {
        return editingCategory;
    }

    public static void setEditingCategory(Category category) {
        editingCategory = category;
    }

    public static void clearEditingCategory() {
        editingCategory = null;
    }
}
