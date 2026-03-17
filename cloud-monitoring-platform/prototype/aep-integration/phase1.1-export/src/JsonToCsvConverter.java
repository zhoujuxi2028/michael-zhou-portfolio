import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JSON转CSV工具 - 专门用于转换AEP产品数据
 * 将验证输出的JSON格式产品数据转换为CSV格式便于核对
 */
public class JsonToCsvConverter {

    public static void main(String[] args) {
        String inputFile = "validation_results/2025-12-28_12-58-55_products.json";
        String outputFile = "validation_results/2025-12-28_12-58-55_products.csv";

        try {
            convertProductJsonToCsv(inputFile, outputFile);
            System.out.println("✅ 转换完成！");
            System.out.println("📄 输入文件: " + inputFile);
            System.out.println("📊 输出文件: " + outputFile);
        } catch (Exception e) {
            System.err.println("❌ 转换失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将产品JSON数据转换为CSV格式
     */
    public static void convertProductJsonToCsv(String inputFile, String outputFile) throws IOException {
        // 读取JSON文件内容
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        String jsonStr = jsonContent.toString();

        // 提取JSON body部分 (这是AEP SDK的toString()输出格式)
        Pattern bodyPattern = Pattern.compile("body=\\{(.+)\\}\\s*$");
        Matcher bodyMatcher = bodyPattern.matcher(jsonStr);

        if (!bodyMatcher.find()) {
            throw new IOException("无法找到JSON body部分");
        }

        String bodyJson = "{" + bodyMatcher.group(1) + "}";

        // 提取产品列表数据
        Pattern listPattern = Pattern.compile("\"list\":\\s*\\[(.*?)\\]");
        Matcher listMatcher = listPattern.matcher(bodyJson);

        if (!listMatcher.find()) {
            throw new IOException("无法找到产品列表数据");
        }

        String listData = listMatcher.group(1);

        // 写入CSV文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // 写入CSV头
            writer.println("ProductID,ProductName,TenantID,DeviceCount,ApiKey,ProductType,SecondaryType,ThirdType,CreateTime,UpdateTime,TupDeviceModel");

            // 解析每个产品对象
            String[] products = splitProducts(listData);

            for (String product : products) {
                if (product.trim().isEmpty()) continue;

                // 提取产品字段
                String productId = extractField(product, "productId");
                String productName = extractStringField(product, "productName");
                String tenantId = extractStringField(product, "tenantId");
                String deviceCount = extractField(product, "deviceCount");
                String apiKey = extractStringField(product, "apiKey");
                String productType = extractField(product, "productType");
                String secondaryType = extractField(product, "secondaryType");
                String thirdType = extractField(product, "thirdType");
                String createTime = formatTimestamp(extractField(product, "createTime"));
                String updateTime = formatTimestamp(extractField(product, "updateTime"));
                String tupDeviceModel = extractStringField(product, "tupDeviceModel");

                // 写入CSV行
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    productId, productName, tenantId, deviceCount, apiKey,
                    productType, secondaryType, thirdType, createTime, updateTime, tupDeviceModel);
            }
        }

        System.out.println("📋 CSV转换摘要:");
        System.out.println("  - 包含字段: ProductID, ProductName, TenantID, DeviceCount, ApiKey等");
        System.out.println("  - 时间戳已转换为可读格式");
        System.out.println("  - ApiKey就是MasterKey，可直接用于设备查询");
    }

    /**
     * 按产品对象分割JSON数组
     */
    private static String[] splitProducts(String listData) {
        // 简单的产品对象分割（基于 },{）
        return listData.split("\\},\\s*\\{");
    }

    /**
     * 提取数字字段
     */
    private static String extractField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 提取字符串字段
     */
    private static String extractStringField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 格式化时间戳为可读格式
     */
    private static String formatTimestamp(String timestamp) {
        if (timestamp.isEmpty()) return "";

        try {
            long ts = Long.parseLong(timestamp);
            java.util.Date date = new java.util.Date(ts);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(date);
        } catch (Exception e) {
            return timestamp; // 返回原始值
        }
    }
}