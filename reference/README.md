# Cloud Monitoring Platform - Reference Data

本目录包含云监控平台相关的参考数据、分析结果和报告文档。

## 目录结构

### data/ - 原始数据
存储从各个系统导出的原始数据文件

- **devices/** - 设备相关数据
  - `current_device.csv` - 当前设备状态数据
  - `device_registration_YYYYMMDD.csv` - 设备注册数据
  - `lbs_sim_info.txt` - LBS SIM卡信息

- **sql/** - 数据库导出文件
  - `device_data_YYYYMMDD.sql` - 设备数据表导出文件

### analysis/ - 数据分析
包含各类数据分析的过程文件和结果

- **gap_analysis/** - 数据差异分析
  - `scripts/` - 分析脚本
  - `results/` - 最终分析结果
  - `working/` - 中间处理文件

- **ccid_analysis/** - CCID专项分析
  - `raw_data/` - 原始CCID数据
  - `results/` - 分析结果

- **nb_devices/** - NB设备专项分析

### reports/ - 分析报告
存储生成的分析报告和文档

- 使用命名格式：`{报告类型}_analysis_YYYYMMDD.md`

### archives/ - 历史归档
存储历史版本的数据快照和归档文件

### scripts/ - 工具脚本
包含数据处理、迁移等自动化脚本

## 文件命名规范

1. **使用英文命名**：避免中文字符和空格
2. **时间格式统一**：使用 YYYYMMDD 格式
3. **功能明确**：文件名应清晰表达文件用途
4. **版本控制**：重要文件应包含日期版本信息

## 数据处理流程

1. 原始数据放入 `data/` 目录
2. 处理过程和中间文件保存在 `analysis/{具体分析}/working/`
3. 最终结果保存在 `analysis/{具体分析}/results/`
4. 生成的报告保存在 `reports/`
5. 完成的分析项目归档到 `archives/`

## 使用说明

- 新增数据分析项目时，在 `analysis/` 下创建相应的子目录
- 每个分析项目都应包含说明文档
- 定期将完成的项目移至 `archives/` 目录

最后更新：2025-12-21