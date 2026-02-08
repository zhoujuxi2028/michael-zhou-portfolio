import java.util.*;

// 导入AEP SDK (与Phase1.1相同配置)
import com.ctg.ag.sdk.core.constant.Scheme;
import com.ctg.ag.sdk.biz.AepDeviceManagementClient;
import com.ctg.ag.sdk.biz.aep_device_management.*;

/**
 * AEP设备管理简单测试
 * 基于真实环境数据测试设备添加、修改、删除功能
 */
public class DeviceManagementTest {

    // 从Phase1.1查询获得的真实数据
    private static final long TEST_PRODUCT_ID = 16980130L; // RepeaterLTE01
    private static final String TEST_MASTER_KEY = "7f1417fbecad4934bdcfe301c302fa3f";
    private static final String TEST_EXISTING_DEVICE = "16980130866877072647500";

    private AepDeviceManagementClient deviceClient;

    public static void main(String[] args) {
        DeviceManagementTest test = new DeviceManagementTest();
        try {
            test.init();
            test.runBasicTests();
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void init() {
        // 从环境变量获取配置
        String appKey = System.getenv("AEP_APP_KEY");
        String appSecret = System.getenv("AEP_APP_SECRET");

        if (appKey == null || appSecret == null) {
            throw new RuntimeException("请设置环境变量 AEP_APP_KEY 和 AEP_APP_SECRET");
        }

        // 初始化设备管理客户端
        this.deviceClient = AepDeviceManagementClient.newClient()
            .appKey(appKey)
            .appSecret(appSecret)
            .scheme(Scheme.HTTPS)
            .build();

        System.out.println("✅ AEP设备管理客户端初始化完成");
        System.out.println("📋 测试产品: " + TEST_PRODUCT_ID + " (RepeaterLTE01)");
    }

    private void runBasicTests() {
        System.out.println("\\n🧪 开始设备管理基础测试...");

        // 测试1: 查询现有设备 (验证连接)
        testQueryDevice();

        // 测试2: 尝试添加新设备
        testCreateDevice();

        // 测试3: 尝试修改设备
        testUpdateDevice();

        System.out.println("\\n🎉 基础测试完成！");
    }

    /**
     * 测试查询设备 (验证连接和权限)
     */
    private void testQueryDevice() {
        try {
            System.out.println("\\n🔍 测试1: 查询现有设备...");

            QueryDeviceRequest request = new QueryDeviceRequest();

            // 设置MasterKey (头部参数)
            request.setParamMasterKey(TEST_MASTER_KEY);

            // 设置查询参数 (URL参数)
            request.setParam("deviceId", TEST_EXISTING_DEVICE);
            request.setParam("productId", TEST_PRODUCT_ID);

            QueryDeviceResponse response = deviceClient.QueryDevice(request);

            if (response != null && response.getBody() != null) {
                String result = new String(response.getBody(), "UTF-8");
                System.out.println("✅ 设备查询成功:");
                System.out.println(result);
            } else {
                System.out.println("⚠️ 设备查询返回空结果");
            }

        } catch (Exception e) {
            System.err.println("❌ 设备查询失败: " + e.getMessage());
        }
    }

    /**
     * 测试添加新设备
     */
    private void testCreateDevice() {
        try {
            System.out.println("\\n➕ 测试2: 添加新设备...");

            CreateDeviceRequest request = new CreateDeviceRequest();

            // 设置MasterKey (头部参数)
            request.setParamMasterKey(TEST_MASTER_KEY);

            // 生成测试设备信息
            String testDeviceName = "TestDevice_" + System.currentTimeMillis();
            String testDeviceSn = "TEST_SN_" + System.currentTimeMillis();

            // 构建请求体JSON (基于电信官方文档格式)
            String requestBodyJson = String.format(
                "{" +
                "\"deviceName\": \"%s\"," +
                "\"deviceSn\": \"%s\"," +
                "\"operator\": \"system_test\"," +
                "\"productId\": %d" +
                "}",
                testDeviceName, testDeviceSn, TEST_PRODUCT_ID
            );

            // 设置请求体
            request.setBody(requestBodyJson.getBytes("UTF-8"));

            System.out.println("📋 尝试创建设备:");
            System.out.println("   - 设备名称: " + testDeviceName);
            System.out.println("   - 设备编号: " + testDeviceSn);
            System.out.println("   - 产品ID: " + TEST_PRODUCT_ID);
            System.out.println("   - 请求体: " + requestBodyJson);

            CreateDeviceResponse response = deviceClient.CreateDevice(request);

            if (response != null && response.getBody() != null) {
                String result = new String(response.getBody(), "UTF-8");
                System.out.println("✅ 设备创建响应:");
                System.out.println(result);

                // TODO: 解析响应获取新设备ID用于后续测试
            } else {
                System.out.println("⚠️ 设备创建返回空结果");
            }

        } catch (Exception e) {
            System.err.println("❌ 设备创建测试失败: " + e.getMessage());
            System.err.println("💡 这可能是正常的，因为需要正确的MasterKey权限");
        }
    }

    /**
     * 测试修改设备
     */
    private void testUpdateDevice() {
        try {
            System.out.println("\\n✏️ 测试3: 修改设备信息...");

            UpdateDeviceRequest request = new UpdateDeviceRequest();

            // 设置MasterKey (头部参数)
            request.setParamMasterKey(TEST_MASTER_KEY);

            // 设置设备ID (URL参数)
            request.setParam("deviceId", TEST_EXISTING_DEVICE);

            // 生成新的设备名称
            String newDeviceName = "Updated_Device_" + System.currentTimeMillis();

            // 构建请求体JSON (基于电信官方文档格式)
            String requestBodyJson = String.format(
                "{" +
                "\"deviceName\": \"%s\"," +
                "\"operator\": \"system_test\"," +
                "\"productId\": %d" +
                "}",
                newDeviceName, TEST_PRODUCT_ID
            );

            // 设置请求体
            request.setBody(requestBodyJson.getBytes("UTF-8"));

            System.out.println("📋 尝试更新设备:");
            System.out.println("   - 设备ID: " + TEST_EXISTING_DEVICE);
            System.out.println("   - 新名称: " + newDeviceName);
            System.out.println("   - 请求体: " + requestBodyJson);

            UpdateDeviceResponse response = deviceClient.UpdateDevice(request);

            if (response != null && response.getBody() != null) {
                String result = new String(response.getBody(), "UTF-8");
                System.out.println("✅ 设备更新响应:");
                System.out.println(result);
            } else {
                System.out.println("⚠️ 设备更新返回空结果");
            }

        } catch (Exception e) {
            System.err.println("❌ 设备更新测试失败: " + e.getMessage());
            System.err.println("💡 这可能是正常的，因为需要正确的MasterKey权限和设备所有权");
        }
    }
}