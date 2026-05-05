package Controllers.Marketplace.Back;

public interface PageHost {
    Object loadPage(String fxmlPath);

    // PageHost hook (currently no-op).
    void refreshStatistics();
}
