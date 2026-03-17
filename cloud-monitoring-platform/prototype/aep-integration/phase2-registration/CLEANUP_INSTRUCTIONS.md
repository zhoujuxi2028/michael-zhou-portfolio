# 🧹 AEP环境清理指令 - 快速参考

## ⚠️ 需要清理的真实数据

### 1. **创建的测试设备** (建议删除)
- **设备ID**: `16980130TEST_SN_1769354052974`
- **设备名**: `TestDevice_1769354052972`
- **产品**: RepeaterLTE01 (16980130)

### 2. **修改的现有设备** (建议恢复)
- **设备ID**: `16980130866877072647500`
- **当前名称**: `Updated_Device_1769354053037`
- **原名称**: `866877072647500` (推测)

## 🚀 快速清理命令

### 编译测试程序
```bash
cd /Users/michael_zhou/Documents/ZCT/github/Cloud-Monitoring-Platform/protoType/aep-integration/phase2-registration
javac -cp "lib/*:." DeviceManagementTest.java
```

### 删除测试设备
```bash
# TODO: 需要编写删除设备的测试方法
```

### 恢复设备名称
```bash
# TODO: 需要编写恢复名称的测试方法
```

## 📄 详细文档位置
**完整操作记录**: `docs/REAL_AEP_TEST_OPERATIONS_LOG.md`