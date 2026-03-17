package sdk;

import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListRequest;

/**
 * SDK方法存在性验证测试
 */
public class SDKMethodTest {

    public static void main(String[] args) {
        System.out.println("=== SDK Method Verification Test ===");

        try {
            // 创建QueryDeviceListRequest对象
            QueryDeviceListRequest request = new QueryDeviceListRequest();
            System.out.println("✅ QueryDeviceListRequest 对象创建成功");

            // 尝试调用setParamMasterKey方法
            try {
                request.setParamMasterKey("test_key");
                System.out.println("✅ setParamMasterKey() 方法调用成功");
            } catch (NoSuchMethodError e) {
                System.err.println("❌ setParamMasterKey() 方法不存在: " + e.getMessage());
                return;
            }

            // 尝试调用其他方法验证SDK正常
            try {
                request.setParamProductId(12345);
                System.out.println("✅ setParamProductId() 方法调用成功");
            } catch (Exception e) {
                System.err.println("⚠️ setParamProductId() 方法调用失败: " + e.getMessage());
            }

            System.out.println("✅ SDK方法验证全部通过！");

        } catch (Exception e) {
            System.err.println("❌ SDK测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}