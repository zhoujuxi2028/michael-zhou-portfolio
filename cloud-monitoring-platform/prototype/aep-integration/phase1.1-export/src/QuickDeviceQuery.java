import com.ctg.ag.sdk.core.constant.Scheme;
import com.ctg.ag.sdk.biz.AepDeviceManagementClient;
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListRequest;
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListResponse;

/**
 * 快速查询产品16857118的设备信息
 */
public class QuickDeviceQuery {

    public static void main(String[] args) {
        String appKey = System.getenv("AEP_APP_KEY");
        String appSecret = System.getenv("AEP_APP_SECRET");

        // 产品16857118的MasterKey (从环境变量读取)
        String masterKey = System.getenv("AEP_MASTER_KEY");
        long productId = 16857118L;

        if (appKey == null || appSecret == null || masterKey == null) {
            System.err.println("❌ 缺少必要的环境变量，程序退出:");
            System.err.println("  AEP_APP_KEY: " + (appKey == null ? "未设置" : "已设置"));
            System.err.println("  AEP_APP_SECRET: " + (appSecret == null ? "未设置" : "已设置"));
            System.err.println("  AEP_MASTER_KEY: " + (masterKey == null ? "未设置" : "已设置"));
            System.err.println();
            System.err.println("请设置环境变量后重新运行:");
            System.err.println("  export AEP_APP_KEY=your_app_key");
            System.err.println("  export AEP_APP_SECRET=your_app_secret");
            System.err.println("  export AEP_MASTER_KEY=your_master_key");
            System.exit(1);
        }

        System.out.println("使用App Key: " + appKey.substring(0,4) + "****" + appKey.substring(appKey.length()-4));

        try {
            System.out.println("============================================================");
            System.out.println("查询产品 16857118 (RepeaterLTE) 的设备信息");
            System.out.println("预期设备数量: 892");
            System.out.println("============================================================");

            AepDeviceManagementClient client = AepDeviceManagementClient.newClient()
                .appKey(appKey)
                .appSecret(appSecret)
                .scheme(Scheme.HTTPS)
                .build();

            // 查询第一页设备（最多100个）
            QueryDeviceListRequest request = new QueryDeviceListRequest();
            request.setParamMasterKey(masterKey);
            request.setParamProductId((int)productId);
            request.setParamPageNow(1);
            request.setParamPageSize(10); // 先查询10个设备看结构

            System.out.println("🔄 发送设备查询请求...");
            System.out.println("参数: productId=" + productId + ", pageNow=1, pageSize=10");

            QueryDeviceListResponse response = client.QueryDeviceList(request);

            if (response != null) {
                System.out.println("\n✅ 设备查询成功！");
                System.out.println("完整响应数据:");
                System.out.println("=" .repeat(80));
                System.out.println(response.toString());
                System.out.println("=" .repeat(80));
            } else {
                System.err.println("❌ 设备查询失败：响应为空");
            }

            client.shutdown();

        } catch (Exception e) {
            System.err.println("❌ 查询失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n查询完成");
    }
}