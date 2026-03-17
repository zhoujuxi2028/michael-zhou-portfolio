package com.aep.registration.service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.*;

/**
 * Phase2产品注册日志管理器
 * 基于Phase1.1-export的LogManager，针对产品注册功能优化
 * 功能特性:
 * - 支持ERROR, WARN, INFO, DEBUG四个级别
 * - 按程序运行次数进行日志轮转
 * - 自动删除旧日志文件，只保留最新3个
 * - 线程安全的异步日志写入
 * - Phase2特定的审计日志支持
 *
 * @author ZCT Phase2 Registration Tool
 * @version 2.0 (基于Phase1.1)
 */
public class LogManager {

    public enum LogLevel {
        ERROR(1, "ERROR"),
        WARN(2, "WARN "),
        INFO(3, "INFO "),
        DEBUG(4, "DEBUG");

        private final int priority;
        private final String name;

        LogLevel(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }

        public int getPriority() { return priority; }
        public String getName() { return name; }
    }

    private static LogManager instance;
    private final String logDirectory;
    private final String currentLogFile;
    private final LogLevel currentLevel;
    private final SimpleDateFormat timestampFormat;
    private final SimpleDateFormat fileNameFormat;
    private final PrintWriter logWriter;
    private final ConcurrentLinkedQueue<String> logQueue;
    private final AtomicBoolean isRunning;
    private final Thread logWriterThread;

    // 单例模式 - 确保全局只有一个LogManager实例
    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    private LogManager() {
        this.logDirectory = "./logs/";
        this.currentLevel = LogLevel.DEBUG; // Phase2默认DEBUG级别
        this.timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.fileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = new AtomicBoolean(true);

        // 创建日志目录
        try {
            Files.createDirectories(Paths.get(logDirectory));
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }

        // 清理旧日志文件（保留最新3个）
        cleanupOldLogFiles();

        // 创建当前日志文件
        this.currentLogFile = logDirectory + "aep-registration-" + fileNameFormat.format(new Date()) + ".log";

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(currentLogFile, true));
            // 写入启动标识
            writer.println("=".repeat(80));
            writer.println("AEP Product Registration Tool Log Started at " + timestampFormat.format(new Date()));
            writer.println("Log Level: " + currentLevel.getName());
            writer.println("=".repeat(80));
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }
        this.logWriter = writer;

        // 启动异步日志写入线程
        this.logWriterThread = new Thread(this::processLogQueue);
        this.logWriterThread.setDaemon(true);
        this.logWriterThread.setName("AEP-Registration-LogWriter");
        this.logWriterThread.start();

        // JVM关闭时的清理钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * 清理旧日志文件，只保留最新的3个
     */
    private void cleanupOldLogFiles() {
        try {
            File logDir = new File(logDirectory);
            if (!logDir.exists()) return;

            File[] logFiles = logDir.listFiles((dir, name) ->
                name.startsWith("aep-registration-") && name.endsWith(".log"));

            if (logFiles != null && logFiles.length > 3) {
                // 按修改时间排序，删除最旧的文件
                Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));

                int filesToDelete = logFiles.length - 2; // 保留最新3个，当前文件还没创建
                for (int i = 0; i < filesToDelete; i++) {
                    if (logFiles[i].delete()) {
                        System.out.println("Deleted old log file: " + logFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup old log files: " + e.getMessage());
        }
    }

    /**
     * 异步日志队列处理线程
     */
    private void processLogQueue() {
        while (isRunning.get() || !logQueue.isEmpty()) {
            String logEntry = logQueue.poll();
            if (logEntry != null) {
                if (logWriter != null) {
                    logWriter.println(logEntry);
                    logWriter.flush();
                }
                // 同时输出到控制台
                System.out.println(logEntry);
            } else {
                try {
                    Thread.sleep(10); // 避免空轮询
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 记录日志的通用方法
     */
    private void log(LogLevel level, String step, String className, String message, Throwable throwable) {
        if (level.getPriority() > currentLevel.getPriority()) {
            return; // 当前级别不记录此日志
        }

        StringBuilder logEntry = new StringBuilder();
        logEntry.append("[").append(timestampFormat.format(new Date())).append("] ");
        logEntry.append("[").append(level.getName()).append("] ");
        if (step != null && !step.trim().isEmpty()) {
            logEntry.append("[").append(step).append("] ");
        }
        logEntry.append("[").append(className).append("] - ");
        logEntry.append(message);

        // 添加异常堆栈信息
        if (throwable != null) {
            logEntry.append("\n").append(getStackTrace(throwable));
        }

        // 加入异步队列
        logQueue.offer(logEntry.toString());
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    // 公共日志方法 (继承Phase1.1接口)
    public void error(String step, String className, String message) {
        log(LogLevel.ERROR, step, className, message, null);
    }

    public void error(String step, String className, String message, Throwable throwable) {
        log(LogLevel.ERROR, step, className, message, throwable);
    }

    public void warn(String step, String className, String message) {
        log(LogLevel.WARN, step, className, message, null);
    }

    public void warn(String step, String className, String message, Throwable throwable) {
        log(LogLevel.WARN, step, className, message, throwable);
    }

    public void warning(String step, String className, String message) {
        warn(step, className, message);
    }

    public void info(String step, String className, String message) {
        log(LogLevel.INFO, step, className, message, null);
    }

    public void debug(String step, String className, String message) {
        log(LogLevel.DEBUG, step, className, message, null);
    }

    // 简化的日志方法（不需要指定步骤）
    public void error(String className, String message) {
        error(null, className, message);
    }

    public void error(String className, String message, Throwable throwable) {
        error(null, className, message, throwable);
    }

    public void warn(String className, String message) {
        warn(null, className, message);
    }

    public void info(String className, String message) {
        info(null, className, message);
    }

    public void debug(String className, String message) {
        debug(null, className, message);
    }

    /**
     * Phase2专用产品注册流程日志方法
     */
    public void logRegistrationStart(String className, String message) {
        info("产品创建", className, message);
    }

    public void logRegistrationSuccess(String className, String message) {
        info("创建成功", className, message);
    }

    public void logRegistrationError(String className, String message) {
        error("创建失败", className, message);
    }

    public void logUpdateStart(String className, String message) {
        info("产品更新", className, message);
    }

    public void logUpdateSuccess(String className, String message) {
        info("更新成功", className, message);
    }

    public void logUpdateError(String className, String message) {
        error("更新失败", className, message);
    }

    public void logDeleteStart(String className, String message) {
        info("产品删除", className, message);
    }

    public void logDeleteSuccess(String className, String message) {
        info("删除成功", className, message);
    }

    public void logDeleteError(String className, String message) {
        error("删除失败", className, message);
    }

    /**
     * 审计日志专用方法 (Phase2新增)
     * 用于记录所有产品管理操作的审计轨迹
     */
    public void audit(String step, String className, String message) {
        // 审计日志使用INFO级别，并添加特殊标识
        String auditMessage = "[AUDIT] " + message;
        log(LogLevel.INFO, step, className, auditMessage, null);
    }

    /**
     * 检查调试日志是否启用 (Phase2新增)
     */
    public boolean isDebugEnabled() {
        return currentLevel.getPriority() >= LogLevel.DEBUG.getPriority();
    }

    /**
     * 性能日志方法 (Phase2新增)
     */
    public void performance(String operation, long durationMs, String className) {
        String perfMessage = String.format("[PERF] %s completed in %dms", operation, durationMs);
        if (durationMs > 5000) {
            warn("性能警告", className, perfMessage);
        } else {
            debug("性能监控", className, perfMessage);
        }
    }

    /**
     * 获取当前日志文件路径
     */
    public String getCurrentLogFile() {
        return currentLogFile;
    }

    /**
     * 获取日志级别
     */
    public LogLevel getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 关闭日志管理器
     */
    public void shutdown() {
        isRunning.set(false);

        if (logWriterThread != null && logWriterThread.isAlive()) {
            try {
                logWriterThread.join(1000); // 等待1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (logWriter != null) {
            logWriter.println("=".repeat(80));
            logWriter.println("AEP Product Registration Tool Log Ended at " + timestampFormat.format(new Date()));
            logWriter.println("=".repeat(80));
            logWriter.close();
        }
    }
}