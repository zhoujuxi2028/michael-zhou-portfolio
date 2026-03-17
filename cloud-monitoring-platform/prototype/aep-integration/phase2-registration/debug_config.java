import com.aep.registration.model.ExportConfig;

public class debug_config {
    public static void main(String[] args) {
        System.out.println("=== Debug Configuration Loading ===");

        // Check environment variables
        System.out.println("Environment variables:");
        System.out.println("AEP_APP_KEY: " + System.getenv("AEP_APP_KEY"));
        System.out.println("AEP_APP_SECRET: " + System.getenv("AEP_APP_SECRET"));
        System.out.println("AEP_API_HOST: " + System.getenv("AEP_API_HOST"));
        System.out.println("AEP_APP_ID: " + System.getenv("AEP_APP_ID"));

        try {
            System.out.println("\nAttempting to create ExportConfig from environment...");
            ExportConfig config = ExportConfig.fromEnvironment();
            System.out.println("✅ Configuration created successfully!");
            System.out.println("Config: " + config.toString());
        } catch (Exception e) {
            System.out.println("❌ Failed to create configuration:");
            e.printStackTrace();
        }

        System.out.println("\n=== Debug Complete ===");
    }
}