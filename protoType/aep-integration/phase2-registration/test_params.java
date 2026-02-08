// 测试CreateProductRequest支持的参数名
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductRequest;

public class test_params {
    public static void main(String[] args) {
        CreateProductRequest request = new CreateProductRequest();

        // 测试常见的参数名
        String[] paramNames = {
            "productName", "name", "product_name",
            "deviceType", "type", "device_type",
            "description", "desc",
            "networkType", "network_type",
            "dataFormat", "data_format"
        };

        System.out.println("测试CreateProductRequest支持的参数名:");
        for (String paramName : paramNames) {
            try {
                request.setParam(paramName, "test_value");
                System.out.println("✅ " + paramName + " - 支持");
            } catch (Exception e) {
                System.out.println("❌ " + paramName + " - 不支持: " + e.getMessage());
            }
        }
    }
}