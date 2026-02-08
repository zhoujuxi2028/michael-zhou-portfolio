#!/usr/bin/env node

/**
 * AEP设备列表数据处理演示脚本
 * 展示如何使用解析器、过滤器和统计工具
 *
 * 使用方法:
 *   node usage-demo.js
 *
 * @author AEP Integration Team
 * @version 1.0
 * @date 2024-12-29
 */

const fs = require('fs');
const path = require('path');

// 导入工具类
const AEPDeviceListParser = require('./parser.js');
const AEPDeviceFilter = require('./filter.js');
const AEPDeviceStats = require('./stats.js');

// 主演示函数
async function runDemo() {
    console.log('🚀 AEP设备列表数据处理演示');
    console.log('================================\n');

    try {
        // 1. 读取原始数据
        console.log('📖 步骤1: 读取原始数据');
        const dataPath = path.join(__dirname, '..', 'raw-response.json');

        if (!fs.existsSync(dataPath)) {
            console.log('❌ 未找到原始数据文件: raw-response.json');
            console.log('请确保文件存在于正确位置');
            return;
        }

        const rawData = JSON.parse(fs.readFileSync(dataPath, 'utf8'));
        console.log(`✅ 成功读取设备数据，包含 ${rawData.result.total} 台设备\n`);

        // 2. 解析数据
        console.log('🔍 步骤2: 解析设备数据');
        const parser = new AEPDeviceListParser();
        const parsedData = parser.parseResponse(rawData);

        console.log(`📊 分页信息:`);
        console.log(`   当前页: ${parsedData.pagination.pageNum}`);
        console.log(`   每页大小: ${parsedData.pagination.pageSize}`);
        console.log(`   总设备数: ${parsedData.pagination.total}`);
        console.log(`   总页数: ${parsedData.pagination.totalPages}\n`);

        // 3. 数据过滤演示
        console.log('🔧 步骤3: 数据过滤演示');
        const devices = rawData.result.list;
        const filter = new AEPDeviceFilter(devices);

        // 获取在线设备
        const onlineDevices = filter.getOnlineDevices();
        console.log(`🟢 在线设备: ${onlineDevices.length} 台`);

        // 获取MQTT协议设备
        const mqttDevices = filter.getMQTTDevices();
        console.log(`📡 MQTT设备: ${mqttDevices.length} 台`);

        // 获取已激活设备
        const activatedDevices = filter.getActivatedDevices();
        console.log(`✅ 已激活设备: ${activatedDevices.length} 台`);

        // 复合查询：在线的MQTT设备
        const onlineMQTT = filter.query({
            netStatus: 1,
            protocol: 2
        });
        console.log(`🔍 在线MQTT设备: ${onlineMQTT.length} 台`);

        // 异常设备分析
        const abnormal = filter.getAbnormalDevices(24);
        console.log(`⚠️  异常设备统计:`);
        console.log(`   长时间离线: ${abnormal.longOffline.length} 台`);
        console.log(`   从未上线: ${abnormal.neverOnline.length} 台`);
        console.log(`   已注销: ${abnormal.deactivated.length} 台\n`);

        // 4. 统计分析演示
        console.log('📈 步骤4: 统计分析演示');
        const stats = new AEPDeviceStats(devices);
        const report = stats.generateReport();

        // 输出格式化报告
        console.log(AEPDeviceStats.formatReport(report));

        // 5. 设备详情展示
        console.log('\n📱 步骤5: 设备详情展示（前5台设备）');
        console.log('=' .repeat(60));

        parsedData.devices.slice(0, 5).forEach((device, index) => {
            console.log(`\n设备 ${index + 1}:`);
            console.log(`  📝 名称: ${device.name}`);
            console.log(`  🔑 ID: ${device.id}`);
            console.log(`  📍 状态: ${device.status.device.text}`);
            console.log(`  🌐 网络: ${device.status.network.text}`);
            console.log(`  🔌 协议: ${device.protocol.name}`);
            console.log(`  📅 创建: ${parser.formatTimestamp(device.timestamps.created?.getTime())}`);
            console.log(`  ⏰ 在线时长: ${parser.calculateUptime(device)}`);

            // 验证数据完整性
            const validation = parser.validateDevice(device._raw);
            if (!validation.valid) {
                console.log(`  ⚠️  数据问题: ${validation.issues.join(', ')}`);
            }
        });

        // 6. 数据导出演示
        console.log('\n💾 步骤6: 数据导出演示');
        const exportData = {
            metadata: {
                exportTime: new Date().toISOString(),
                totalDevices: devices.length,
                dataSource: 'AEP Platform - Tenant 10433748'
            },
            summary: report.basic,
            healthAnalysis: report.health,
            devices: parsedData.devices.slice(0, 10) // 导出前10台设备作为示例
        };

        const exportPath = path.join(__dirname, '..', 'processed-data.json');
        fs.writeFileSync(exportPath, JSON.stringify(exportData, null, 2));
        console.log(`✅ 已将处理后的数据导出到: ${exportPath}`);

        // 7. 性能统计
        console.log('\n⚡ 步骤7: 性能统计');
        console.log(`📊 原始数据大小: ${JSON.stringify(rawData).length} 字节`);
        console.log(`📈 处理后数据大小: ${JSON.stringify(exportData).length} 字节`);
        console.log(`🔍 解析的设备数量: ${parsedData.devices.length} 台`);
        console.log(`⏱️  处理完成`);

    } catch (error) {
        console.error('❌ 演示过程中出现错误:');
        console.error(error.message);
        console.error('\n请检查:');
        console.error('1. raw-response.json 文件是否存在');
        console.error('2. 文件格式是否正确');
        console.error('3. 权限是否足够');
    }
}

// 工具函数：格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 运行演示
if (require.main === module) {
    runDemo().catch(console.error);
}

module.exports = {
    runDemo,
    AEPDeviceListParser,
    AEPDeviceFilter,
    AEPDeviceStats
};