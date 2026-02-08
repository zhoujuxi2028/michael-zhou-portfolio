package com.aep.export.service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.*;

/**
 * 日志管理器
 * 对应需求: 添加日志文件功能，保持最新3个日志文件
 * 功能特性:
 * - 支持ERROR, WARN, INFO, DEBUG四个级别
 * - 按程序运行次数进行日志轮转
 * - 自动删除旧日志文件，只保留最新3个
 * - 线程安全的异步日志写入
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
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
        this.currentLevel = LogLevel.DEBUG; // 用户选择了DEBUG级别
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
        this.currentLogFile = logDirectory + "aep-export-" + fileNameFormat.format(new Date()) + ".log";

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(currentLogFile, true));
            // 写入启动标识
            writer.println("=".repeat(80));
            writer.println("AEP Export Tool Log Started at " + timestampFormat.format(new Date()));
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
        this.logWriterThread.setName("AEP-LogWriter");
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
                name.startsWith("aep-export-") && name.endsWith(".log"));

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

    // 公共日志方法
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
     * 记录四步流程的专用方法
     */
    public void logStep1(String className, String message) {
        info("产品查询", className, message);
    }

    public void logStep2(String className, String message) {
        info("设备查询", className, message);
    }

    public void logStep3(String className, String message) {
        info("数据合并", className, message);
    }

    public void logStep4(String className, String message) {
        info("导出", className, message);
    }

    /**
     * 获取当前日志文件路径
     */
    public String getCurrentLogFile() {
        return currentLogFile;
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
            logWriter.println("AEP Export Tool Log Ended at " + timestampFormat.format(new Date()));
            logWriter.println("=".repeat(80));
            logWriter.close();
        }
    }
}