import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备JSON转CSV工具 - 将设备数据按产品ID分别导出为CSV文件
 * 目标：导出1424个设备信息分成2个产品文件
 */
public class DeviceJsonToCsvConverter {

    public static void main(String[] args) {
        try {
            // 处理产品16980130 (532个设备)
            processProductDevices(16980130L, "RepeaterLTE01");

            // 处理产品16857118 (892个设备)
            processProductDevices(16857118L, "RepeaterLTE");

            System.out.println("🎉 所有设备数据转换完成！");
            System.out.println("📊 总计导出1424个设备信息");

        } catch (Exception e) {
            System.err.println("❌ 转换失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理指定产品的设备数据转换
     */
    public static void processProductDevices(Long productId, String productName) throws IOException {
        System.out.println("\n🔄 处理产品" + productId + " (" + productName + ")的设备数据...");

        String outputFile = "validation_results/devices_product_" + productId + ".csv";
        List<String> deviceJsonList = new ArrayList<>();

        // 收集该产品的所有设备JSON文件
        int pageNum = 1;
        while (true) {
            String inputFile = "validation_results/2025-12-28_12-58-55_devices_product_" + productId + "_page_" + pageNum + ".json";
            File file = new File(inputFile);

            if (!file.exists()) {
                break; // 没有更多页面
            }

            System.out.println("  📄 读取第" + pageNum + "页数据: " + inputFile);

            // 读取并解析该页设备数据
            String pageDevices = extractDevicesFromFile(inputFile);
            if (!pageDevices.isEmpty()) {
                deviceJsonList.add(pageDevices);
            }

            pageNum++;
        }

        // 写入CSV文件
        writeDevicesToCsv(deviceJsonList, outputFile, productId, productName);

        System.out.println("  ✅ 产品" + productId + "设备数据转换完成: " + outputFile);
    }

    /**
     * 从设备JSON文件中提取设备列表数据
     */
    public static String extractDevicesFromFile(String inputFile) throws IOException {
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        String jsonStr = jsonContent.toString();

        // 提取JSON body部分 (AEP SDK的toString()输出格式)
        Pattern bodyPattern = Pattern.compile("body=\\{(.+)\\}\\s*$");
        Matcher bodyMatcher = bodyPattern.matcher(jsonStr);

        if (!bodyMatcher.find()) {
            System.out.println("  ⚠️ 文件" + inputFile + "无法解析JSON body");
            return "";
        }

        String bodyJson = "{" + bodyMatcher.group(1) + "}";

        // 提取设备列表数据 (注意：需要处理嵌套的JSON结构)
        Pattern listPattern = Pattern.compile("\"list\":\\s*\\[(.*?)\\]\\s*\\}\\s*\\}\\s*$");
        Matcher listMatcher = listPattern.matcher(bodyJson);

        if (listMatcher.find()) {
            String listData = listMatcher.group(1);
            if (!listData.trim().isEmpty()) {
                return listData;
            }
        }

        return "";
    }

    /**
     * 将设备数据写入CSV文件
     */
    public static void writeDevicesToCsv(List<String> deviceJsonList, String outputFile,
                                        Long productId, String productName) throws IOException {

        int totalDevices = 0;

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // 写入CSV头
            writer.println("ProductID,ProductName,DeviceID,DeviceName,IMEI,MAC,DeviceType,Status,CreateTime,LastActiveTime,FirmwareVersion,Location");

            // 处理每页的设备数据
            for (String devicesJson : deviceJsonList) {
                if (devicesJson.trim().isEmpty()) continue;

                // 分割设备对象
                String[] devices = splitDevices(devicesJson);

                for (String device : devices) {
                    if (device.trim().isEmpty()) continue;

                    // 提取设备字段
                    String deviceId = extractStringField(device, "deviceId");
                    String deviceName = extractStringField(device, "deviceName");
                    String imei = extractStringField(device, "imei");
                    String mac = extractStringField(device, "mac");
                    String deviceType = extractStringField(device, "deviceType");
                    String status = extractField(device, "status");
                    String createTime = formatTimestamp(extractField(device, "createTime"));
                    String lastActiveTime = formatTimestamp(extractField(device, "lastActiveTime"));
                    String firmwareVersion = extractStringField(device, "firmwareVersion");
                    String location = extractStringField(device, "location");

                    // 写入CSV行
                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        productId, productName,
                        escapeCsv(deviceId), escapeCsv(deviceName), escapeCsv(imei), escapeCsv(mac),
                        escapeCsv(deviceType), status, createTime, lastActiveTime,
                        escapeCsv(firmwareVersion), escapeCsv(location));

                    totalDevices++;
                }
            }
        }

        System.out.println("  📊 共导出" + totalDevices + "个设备");
    }

    /**
     * 按设备对象分割JSON数组
     */
    private static String[] splitDevices(String devicesJson) {
        // 简单的设备对象分割（基于 },{）
        return devicesJson.split("\\},\\s*\\{");
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

    /**
     * CSV字段转义
     */
    private static String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}