package Controllers.Marketplace.Back;

public interface PageHost {
    void loadPage(String fxmlPath);

    // PageHost hook (currently no-op).
    void refreshStatistics();
}
