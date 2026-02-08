import java.io.*;
import java.net.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;

public class AepProductQuerySimple {

    // 认证信息从环境变量读取，确保安全性
    private static final String APP_KEY = System.getenv("AEP_APP_KEY") != null ?
        System.getenv("AEP_APP_KEY") : "YOUR_APP_KEY_HERE";
    private static final String APP_SECRET = System.getenv("AEP_APP_SECRET") != null ?
        System.getenv("AEP_APP_SECRET") : "YOUR_APP_SECRET_HERE";
    private static final String API_HOST = System.getenv("AEP_API_HOST") != null ?
        System.getenv("AEP_API_HOST") : "YOUR_TENANT_ID.api.ctwing.cn";

    public static void main(String[] args) {
        // 验证配置
        if (!validateConfiguration()) {
            System.err.println("❌ 配置验证失败！");
            System.err.println("请设置以下环境变量：");
            System.err.println("  export AEP_APP_KEY=your_app_key");
            System.err.println("  export AEP_APP_SECRET=your_app_secret");
            System.err.println("  export AEP_API_HOST=your_tenant_id.api.ctwing.cn");
            return;
        }

        try {
            // 查询产品列表
            String result = queryProducts();

            // 显示结果
            showProducts(result);

        } catch (Exception e) {
            System.out.println("查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证配置是否有效
     */
    private static boolean validateConfiguration() {
        return !APP_KEY.equals("YOUR_APP_KEY_HERE") &&
               !APP_SECRET.equals("YOUR_APP_SECRET_HERE") &&
               !API_HOST.equals("YOUR_TENANT_ID.api.ctwing.cn");
    }

    // 查询产品
    private static String queryProducts() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String url = "https://" + API_HOST + "/aep_product_management/products";

        // 生成签名
        String signText = "application:" + APP_KEY + "\n" +
                         "timestamp:" + timestamp + "\n" +
                         "pageNow:\n" +
                         "pageSize:\n" +
                         "searchValue:\n";

        String signature = hmacSha1(signText, APP_SECRET);

        // 发送请求
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("application", APP_KEY);
        conn.setRequestProperty("timestamp", timestamp);
        conn.setRequestProperty("version", "20190507004824");
        conn.setRequestProperty("signature", signature);

        // 读取响应
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        return response.toString();
    }

    // HMAC-SHA1签名
    private static String hmacSha1(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1"));
        byte[] result = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(result);
    }

    // 显示产品列表
    private static void showProducts(String json) {
        System.out.println("=== 您的产品列表 ===");

        if (json.contains("\"code\":0")) {
            // 提取产品数量
            String total = getValue(json, "total");
            System.out.println("总产品数: " + total);
            System.out.println();

            // 显示产品信息
            if (json.contains("\"list\":[")) {
                String[] products = json.split("\\{\"productId\":");
                for (int i = 1; i < products.length; i++) {
                    String product = "{\"productId\":" + products[i];
                    String id = getValue(product, "productId");
                    String name = getValue(product, "productName");
                    String deviceCount = getValue(product, "deviceCount");
                    String type = getValue(product, "thirdTypeValue");

                    System.out.println((i) + ". " + name);
                    System.out.println("   产品ID: " + id);
                    System.out.println("   类型: " + type);
                    System.out.println("   设备数: " + deviceCount + "台");
                    System.out.println();
                }
            }
        } else {
            System.out.println("查询失败或无产品数据");
            System.out.println("原始响应: " + json);
        }
    }

    // 简单的JSON值提取
    private static String getValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return "";

            start += pattern.length();
            if (json.charAt(start) == '"') {
                start++;
                int end = json.indexOf('"', start);
                return json.substring(start, end);
            } else {
                int end = start;
                while (end < json.length() &&
                       Character.isDigit(json.charAt(end))) {
                    end++;
                }
                return json.substring(start, end);
            }
        } catch (Exception e) {
            return "";
        }
    }
}