package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件管理服务
 * 对应需求: FR-003-01 - 支持JSON格式导出
 * 对应需求: FR-003-02 - 支持CSV格式导出
 * 对应需求: NFR-003-04 - 导出文件权限控制
 * 设计模块: DM-012 - FileManager
 * 负责管理文件输出、格式化和权限控制
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class FileManager {

    private final ExportConfig config;
    private final String outputDirectory;
    private final SimpleDateFormat timestampFormat;

    /**
     * 构造函数
     * 实现: DM-012-01 - 文件管理器初始化
     */
    public FileManager(ExportConfig config) {
        this.config = config;
        this.outputDirectory = config.getOutputDirectory() != null ?
            config.getOutputDirectory() : "./output";
        this.timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    }

    /**
     * 创建输出目录
     * 实现: DM-012-02 - 目录管理
     */
    public boolean createOutputDirectory() {
        try {
            Path outputPath = Paths.get(outputDirectory);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            return true;
        } catch (IOException e) {
            throw new FileOperationException("Failed to create output directory: " + e.getMessage());
        }
    }

    /**
     * 写入JSON文件
     * 实现: DM-012-03 - JSON文件输出
     */
    public long writeJsonFile(String jsonData, String type) {
        try {
            String fileName = getFileName(type, "json");
            Path filePath = Paths.get(outputDirectory, fileName);

            // 格式化JSON（如果配置要求）
            String formattedJson = formatJson(jsonData);

            // 写入文件
            Files.write(filePath, formattedJson.getBytes("UTF-8"));

            // 设置文件权限
            if (config.getCreateBackup() == null || !config.getCreateBackup()) {
                setFilePermissions(filePath.toString(), "600");
            }

            return Files.size(filePath);
        } catch (IOException e) {
            throw new FileOperationException("Failed to write JSON file: " + e.getMessage());
        }
    }

    /**
     * 写入CSV文件
     * 实现: DM-012-04 - CSV文件输出
     */
    public long writeCsvFile(String[] headers, String[][] data, String type) {
        try {
            String fileName = getFileName(type, "csv");
            Path filePath = Paths.get(outputDirectory, fileName);
            String separator = config.getCsvSeparator() != null ? config.getCsvSeparator() : ",";

            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                // 写入标题行（如果配置要求）
                if (config.getCsvWithHeader() != null && config.getCsvWithHeader()) {
                    writer.write(String.join(separator, headers));
                    writer.newLine();
                }

                // 写入数据行
                for (String[] row : data) {
                    writer.write(String.join(separator, row));
                    writer.newLine();
                }
            }

            // 设置文件权限
            setFilePermissions(filePath.toString(), "600");

            return Files.size(filePath);
        } catch (IOException e) {
            throw new FileOperationException("Failed to write CSV file: " + e.getMessage());
        }
    }

    /**
     * 通用文件写入方法
     * 实现: DM-012-06 - 通用文件写入
     */
    public long writeFile(String data, String type, String extension) {
        try {
            createOutputDirectory();

            if ("json".equals(extension)) {
                return writeJsonFile(data, type);
            } else if ("csv".equals(extension)) {
                // 简化：将CSV字符串直接写入文件
                return writeCsvFromString(data, type);
            } else {
                throw new FileOperationException("不支持的文件格式: " + extension);
            }
        } catch (Exception e) {
            throw new FileOperationException("写入文件失败: " + e.getMessage());
        }
    }

    /**
     * 从CSV字符串写入文件
     */
    private long writeCsvFromString(String csvData, String type) {
        try {
            String fileName = getFileName(type, "csv");
            Path filePath = Paths.get(outputDirectory, fileName);

            Files.write(filePath, csvData.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            setFilePermissions(filePath.toString(), "600");

            return Files.size(filePath);
        } catch (IOException e) {
            throw new FileOperationException("写入CSV文件失败: " + e.getMessage());
        }
    }

    /**
     * 创建备份文件
     * 实现: DM-012-05 - 备份管理
     */
    public String createBackup(String originalFilePath) {
        try {
            Path originalPath = Paths.get(originalFilePath);
            if (!Files.exists(originalPath)) {
                return null;
            }

            String timestamp = timestampFormat.format(new Date());
            String originalName = originalPath.getFileName().toString();
            String backupName = "backup_" + timestamp + "_" + originalName;
            Path backupPath = originalPath.getParent().resolve(backupName);

            Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            return backupPath.toString();
        } catch (IOException e) {
            throw new FileOperationException("Failed to create backup: " + e.getMessage());
        }
    }

    /**
     * 设置文件权限
     * 实现: DM-012-06 - 权限控制
     */
    public boolean setFilePermissions(String filePath, String permissions) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return false;
            }

            // 尝试设置POSIX权限（Linux/macOS）
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> perms = new HashSet<>();

                // 解析权限字符串 (如 "600")
                if (permissions.length() >= 3) {
                    char owner = permissions.charAt(0);
                    char group = permissions.charAt(1);
                    char others = permissions.charAt(2);

                    // 所有者权限
                    if (owner >= '4') { perms.add(PosixFilePermission.OWNER_READ); }
                    if (owner == '6' || owner == '7') { perms.add(PosixFilePermission.OWNER_WRITE); }
                    if (owner == '5' || owner == '7') { perms.add(PosixFilePermission.OWNER_EXECUTE); }

                    // 组权限
                    if (group >= '4') { perms.add(PosixFilePermission.GROUP_READ); }
                    if (group == '6' || group == '7') { perms.add(PosixFilePermission.GROUP_WRITE); }
                    if (group == '5' || group == '7') { perms.add(PosixFilePermission.GROUP_EXECUTE); }

                    // 其他用户权限
                    if (others >= '4') { perms.add(PosixFilePermission.OTHERS_READ); }
                    if (others == '6' || others == '7') { perms.add(PosixFilePermission.OTHERS_WRITE); }
                    if (others == '5' || others == '7') { perms.add(PosixFilePermission.OTHERS_EXECUTE); }
                }

                Files.setPosixFilePermissions(path, perms);
                return true;
            } else {
                // Windows系统，设置只读属性
                if ("600".equals(permissions)) {
                    // 在Windows上，我们只能设置为只读或可写
                    // 600权限在Windows上近似为所有者可读写
                    path.toFile().setWritable(true, true);
                    path.toFile().setReadable(true, true);
                    path.toFile().setExecutable(false, false);
                }
                return true;
            }
        } catch (Exception e) {
            // 权限设置失败不抛异常，只返回false
            return false;
        }
    }

    /**
     * 获取文件大小
     * 实现: DM-012-07 - 文件信息查询
     */
    public long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.size(path);
            }
            return 0;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 检查文件是否存在
     * 实现: DM-012-07 - 文件信息查询
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 检查目录是否存在
     * 实现: DM-012-07 - 文件信息查询
     */
    public boolean directoryExists(String dirPath) {
        Path path = Paths.get(dirPath);
        return Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * 读取文件内容
     * 实现: DM-012-08 - 文件内容操作
     */
    public String readFile(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return new String(bytes, "UTF-8");
        } catch (IOException e) {
            throw new FileOperationException("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * 清理临时文件和目录
     * 实现: DM-012-09 - 清理管理
     */
    public void cleanup() {
        try {
            Path outputPath = Paths.get(outputDirectory);
            if (Files.exists(outputPath) && Files.isDirectory(outputPath)) {
                // 删除目录下的所有文件
                Files.walk(outputPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // 忽略删除失败的文件
                        }
                    });

                // 尝试删除空目录
                try {
                    Files.delete(outputPath);
                } catch (IOException e) {
                    // 目录不为空或删除失败，忽略
                }
            }
        } catch (IOException e) {
            // 清理失败不抛异常
        }
    }

    // 辅助方法

    /**
     * 根据类型获取文件名
     */
    private String getFileName(String type, String extension) {
        switch (type.toLowerCase()) {
            case "products":
                return config.getProductFileName() != null ?
                    config.getProductFileName() : "products." + extension;
            case "devices":
                return config.getDeviceFileName() != null ?
                    config.getDeviceFileName() : "devices." + extension;
            default:
                return type + "." + extension;
        }
    }

    /**
     * 格式化JSON
     */
    private String formatJson(String jsonData) {
        if (config.getJsonIndented() != null && config.getJsonIndented()) {
            // 简单的JSON格式化（生产环境应使用JSON库）
            return formatJsonSimple(jsonData);
        }
        return jsonData;
    }

    /**
     * 简单的JSON格式化
     */
    private String formatJsonSimple(String jsonData) {
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;
        boolean inQuotes = false;
        char prev = 0;

        for (char c : jsonData.toCharArray()) {
            switch (c) {
                case '"':
                    if (prev != '\\') {
                        inQuotes = !inQuotes;
                    }
                    formatted.append(c);
                    break;
                case '{':
                case '[':
                    formatted.append(c);
                    if (!inQuotes) {
                        formatted.append('\n');
                        indentLevel++;
                        addIndent(formatted, indentLevel);
                    }
                    break;
                case '}':
                case ']':
                    if (!inQuotes) {
                        formatted.append('\n');
                        indentLevel--;
                        addIndent(formatted, indentLevel);
                    }
                    formatted.append(c);
                    break;
                case ',':
                    formatted.append(c);
                    if (!inQuotes) {
                        formatted.append('\n');
                        addIndent(formatted, indentLevel);
                    }
                    break;
                case ':':
                    formatted.append(c);
                    if (!inQuotes) {
                        formatted.append(' ');
                    }
                    break;
                default:
                    formatted.append(c);
            }
            prev = c;
        }

        return formatted.toString();
    }

    /**
     * 添加缩进
     */
    private void addIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
    }

    /**
     * 文件操作异常类
     */
    public static class FileOperationException extends RuntimeException {
        public FileOperationException(String message) {
            super(message);
        }
    }
}