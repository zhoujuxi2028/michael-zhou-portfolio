import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;

import com.ctg.ag.sdk.core.constant.Scheme;
import com.ctg.ag.sdk.core.model.ApiCallBack;

import com.ctg.ag.sdk.biz.AepProductManagementClient;
import com.ctg.ag.sdk.biz.aep_product_management.QueryProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.QueryProductResponse;
import com.ctg.ag.sdk.biz.aep_product_management.QueryProductListRequest;
import com.ctg.ag.sdk.biz.aep_product_management.QueryProductListResponse;
import com.ctg.ag.sdk.biz.aep_product_management.DeleteProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.DeleteProductResponse;
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductResponse;
import com.ctg.ag.sdk.biz.aep_product_management.UpdateProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.UpdateProductResponse;

/**
 * 增强版AEP产品管理演示
 *
 * 使用环境变量配置的AEP平台认证信息，实现产品列表查询功能
 *
 * 环境变量配置:
 * - AEP_APP_ID: 您的应用ID
 * - AEP_APP_KEY: 您的应用Key
 * - AEP_APP_SECRET: 您的应用Secret
 * - AEP_API_HOST: 您的API域名
 *
 * @author ZhongCheng Technology
 * @version 1.1
 * @since 2024-12-26
 */
public class AepProductManagementDemo_Enhanced {

    // AEP平台认证信息（从环境变量读取）
    private static final String APP_ID = System.getenv("AEP_APP_ID") != null ?
        System.getenv("AEP_APP_ID") : "YOUR_APP_ID";
    private static final String APP_KEY = System.getenv("AEP_APP_KEY") != null ?
        System.getenv("AEP_APP_KEY") : "YOUR_APP_KEY";
    private static final String APP_SECRET = System.getenv("AEP_APP_SECRET") != null ?
        System.getenv("AEP_APP_SECRET") : "YOUR_APP_SECRET";
    private static final String API_DOMAIN = System.getenv("AEP_API_HOST") != null ?
        System.getenv("AEP_API_HOST") : "YOUR_TENANT_ID.api.ctwing.cn";

    /**
     * 主要演示方法 - 查询您手上设备的产品列表
     */
    public void queryMyProductList() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("查询您的AEP平台产品列表");
        System.out.println("App ID: " + APP_ID);
        System.out.println("API域名: " + API_DOMAIN);
        System.out.println("=".repeat(60));

        // 创建带签名认证的客户端
        AepProductManagementClient client = AepProductManagementClient.newClient()
            .appKey(APP_KEY)
            .appSecret(APP_SECRET)
            .scheme(Scheme.HTTPS)
            .build();

        try {
            // 查询产品列表
            System.out.println("\n正在查询您的产品列表...");
            QueryProductListRequest request = new QueryProductListRequest();

            // 设置查询参数
            // request.setOffset(0);        // 偏移量，从0开始
            // request.setLimit(50);        // 每页数量，最大100
            // request.setSearchValue("");  // 搜索关键字（产品名称）

            System.out.println("发送查询请求...");
            QueryProductListResponse response = client.QueryProductList(request);

            // 处理响应结果
            if (response != null) {
                System.out.println("\n✅ 查询成功！");
                System.out.println("完整响应信息:");
                System.out.println("-".repeat(80));
                System.out.println(response.toString());
                System.out.println("-".repeat(80));

                System.out.println("\n📋 响应解析说明:");
                System.out.println("- 如果看到产品数据，说明查询到了您的设备产品列表");
                System.out.println("- 如果显示空列表或totalCount为0，说明您还未创建产品");
                System.out.println("- 具体的产品信息会在上面的响应详情中显示");

            } else {
                System.out.println("❌ 查询失败: 响应为空");
                System.out.println("可能原因:");
                System.out.println("- 网络连接问题");
                System.out.println("- 认证信息错误");
                System.out.println("- AEP平台服务异常");
            }

        } catch (Exception e) {
            System.err.println("❌ 查询过程中发生异常:");
            e.printStackTrace();

            // 提供故障排除建议
            System.out.println("\n故障排除建议:");
            System.out.println("1. 检查网络连接是否正常");
            System.out.println("2. 验证App Key和App Secret是否正确");
            System.out.println("3. 确认应用权限是否包含产品管理");
            System.out.println("4. 检查AEP平台服务是否正常");

        } finally {
            // 关闭客户端连接
            if (client != null) {
                client.shutdown();
                System.out.println("\n客户端连接已关闭");
            }
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("查询完成");
        System.out.println("=".repeat(60));
    }

    /**
     * 详细查询特定产品信息
     */
    public void querySpecificProduct() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("查询特定产品详情");
        System.out.println("=".repeat(60));

        AepProductManagementClient client = AepProductManagementClient.newClient()
            .appKey(APP_KEY)
            .appSecret(APP_SECRET)
            .scheme(Scheme.HTTPS)
            .build();

        try {
            // 如果您知道特定的产品ID，可以在这里设置
            String productId = "your_product_id_here"; // 请替换为实际的产品ID

            QueryProductRequest request = new QueryProductRequest();
            // request.setProductId(Long.parseLong(productId));

            System.out.println("查询产品ID: " + productId);
            QueryProductResponse response = client.QueryProduct(request);

            if (response != null) {
                System.out.println("\n✅ 产品详情查询成功！");
                System.out.println("完整响应信息:");
                System.out.println("-".repeat(40));
                System.out.println(response.toString());
                System.out.println("-".repeat(40));
            } else {
                System.out.println("❌ 产品查询失败");
            }

        } catch (Exception e) {
            System.err.println("❌ 查询特定产品时发生异常:");
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    /**
     * 异步查询产品列表
     */
    public void queryProductListAsync() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("异步查询产品列表");
        System.out.println("=".repeat(60));

        AepProductManagementClient client = AepProductManagementClient.newClient()
            .appKey(APP_KEY)
            .appSecret(APP_SECRET)
            .scheme(Scheme.HTTPS)
            .build();

        try {
            QueryProductListRequest request = new QueryProductListRequest();

            Future<QueryProductListResponse> futureResponse = client.QueryProductList(request,
                new ApiCallBack<QueryProductListRequest, QueryProductListResponse>() {
                    @Override
                    public void onFailure(QueryProductListRequest request, Exception e) {
                        System.err.println("❌ 异步查询失败:");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(QueryProductListRequest request, QueryProductListResponse response) {
                        System.out.println("✅ 异步查询成功，收到响应数据");
                        if (response != null) {
                            System.out.println("响应内容: " + response.toString());
                        }
                    }
                });

            // 等待异步结果
            QueryProductListResponse response = futureResponse.get();
            System.out.println("最终响应: " + response);

        } catch (Exception e) {
            System.err.println("❌ 异步查询异常:");
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    /**
     * 创建新产品示例（请谨慎使用，会在您的AEP平台创建实际产品）
     */
    public void createProductExample() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("创建新产品示例（谨慎使用）");
        System.out.println("=".repeat(60));

        AepProductManagementClient client = AepProductManagementClient.newClient()
            .appKey(APP_KEY)
            .appSecret(APP_SECRET)
            .scheme(Scheme.HTTPS)
            .build();

        try {
            CreateProductRequest request = new CreateProductRequest();

            // 设置产品创建参数
            // request.setProductName("ZhongCheng测试产品");
            // request.setProductType(1);  // 产品类型
            // request.setDataFormat(1);   // 数据格式
            // request.setIndustryId(1);   // 行业ID
            // request.setDescription("众成科技测试产品");

            System.out.println("⚠️  注意: 此操作会在您的AEP平台创建实际产品");
            System.out.println("如需测试，请取消注释上述参数设置代码");

            // 取消注释下面的代码来执行创建操作
            /*
            CreateProductResponse response = client.CreateProduct(request);
            if (response != null) {
                System.out.println("✅ 产品创建成功!");
                System.out.println("响应: " + response.toString());
            }
            */

        } catch (Exception e) {
            System.err.println("❌ 创建产品异常:");
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    /**
     * 测试连接和认证
     */
    public void testConnection() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("测试AEP平台连接和认证");
        System.out.println("=".repeat(60));

        // 测试基础连接
        System.out.println("1. 测试基础连接（无签名）...");
        AepProductManagementClient basicClient = AepProductManagementClient.newClient().build();

        try {
            QueryProductListRequest basicRequest = new QueryProductListRequest();
            QueryProductListResponse basicResponse = basicClient.QueryProductList(basicRequest);
            System.out.println("基础连接响应: " + (basicResponse != null ? "成功" : "失败"));
        } catch (Exception e) {
            System.out.println("基础连接测试异常: " + e.getMessage());
        } finally {
            basicClient.shutdown();
        }

        // 测试认证连接
        System.out.println("\n2. 测试认证连接（带签名）...");
        AepProductManagementClient authClient = AepProductManagementClient.newClient()
            .appKey(APP_KEY)
            .appSecret(APP_SECRET)
            .scheme(Scheme.HTTPS)
            .build();

        try {
            QueryProductListRequest authRequest = new QueryProductListRequest();
            QueryProductListResponse authResponse = authClient.QueryProductList(authRequest);
            System.out.println("认证连接响应: " + (authResponse != null ? "成功" : "失败"));

            if (authResponse != null) {
                System.out.println("完整响应: " + authResponse.toString());
            }

        } catch (Exception e) {
            System.out.println("认证连接测试异常: " + e.getMessage());
        } finally {
            authClient.shutdown();
        }

        System.out.println("\n连接测试完成");
    }

    /**
     * 主方法 - 直接运行查询
     */
    public static void main(String[] args) {
        System.out.println("启动AEP产品管理演示程序");

        AepProductManagementDemo_Enhanced demo = new AepProductManagementDemo_Enhanced();

        try {
            // 执行主要功能：查询产品列表
            demo.queryMyProductList();

            // 可选：测试连接
            System.out.println("\n是否需要测试连接？（取消注释下面的代码）");
            // demo.testConnection();

        } catch (Exception e) {
            System.err.println("程序执行异常:");
            e.printStackTrace();
        }

        System.out.println("\n程序执行完成");
    }
}