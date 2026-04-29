package Controllers.Marketplace.Back;

public interface PageHost {
    Object loadPage(String fxmlPath);

    void refreshStatistics();
}
