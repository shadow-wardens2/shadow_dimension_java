import Services.Tutorials.FormationAiService;
import java.lang.reflect.Method;

public class TestOracle {
    public static void main(String[] args) throws Exception {
        FormationAiService service = new FormationAiService();
        
        Method m = FormationAiService.class.getDeclaredMethod("callAi", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, "Test Context");
        
        System.out.println("Result: " + result);
    }
}
