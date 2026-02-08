#!/bin/bash

# 文件迁移和组织脚本
# 用于帮助将新文件放置到正确的目录结构中

echo "Cloud Monitoring Platform - 文件组织工具"
echo "========================================="

# 检查参数
if [ $# -eq 0 ]; then
    echo "使用方法: $0 [文件路径]"
    echo ""
    echo "支持的文件类型："
    echo "  *.csv (设备数据) -> data/devices/"
    echo "  *.sql (数据库导出) -> data/sql/"
    echo "  *analysis*.md (分析报告) -> reports/"
    echo "  *_sorted.txt (中间处理文件) -> analysis/gap_analysis/working/"
    echo "  *common*.txt (分析结果) -> analysis/gap_analysis/results/"
    exit 1
fi

FILE=$1
BASENAME=$(basename "$FILE")

# 检查文件是否存在
if [ ! -f "$FILE" ]; then
    echo "错误: 文件 '$FILE' 不存在"
    exit 1
fi

# 根据文件名模式决定目标目录
if [[ $BASENAME == *.csv ]]; then
    if [[ $BASENAME == *device* ]]; then
        TARGET_DIR="data/devices"
    else
        TARGET_DIR="data"
    fi
elif [[ $BASENAME == *.sql ]]; then
    TARGET_DIR="data/sql"
elif [[ $BASENAME == *analysis*.md ]]; then
    TARGET_DIR="reports"
elif [[ $BASENAME == *_sorted.txt ]]; then
    TARGET_DIR="analysis/gap_analysis/working"
elif [[ $BASENAME == *common*.txt ]] || [[ $BASENAME == only_in*.txt ]]; then
    TARGET_DIR="analysis/gap_analysis/results"
elif [[ $BASENAME == *ccid*.txt ]]; then
    if [[ $BASENAME == *common* ]]; then
        TARGET_DIR="analysis/ccid_analysis/results"
    else
        TARGET_DIR="analysis/ccid_analysis/raw_data"
    fi
else
    echo "警告: 无法识别文件类型，请手动移动文件"
    echo "文件: $FILE"
    exit 1
fi

# 创建目标目录（如果不存在）
mkdir -p "$TARGET_DIR"

# 移动文件
echo "移动文件: $FILE -> $TARGET_DIR/"
mv "$FILE" "$TARGET_DIR/"

echo "完成!"