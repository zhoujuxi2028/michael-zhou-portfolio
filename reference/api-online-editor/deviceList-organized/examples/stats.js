/**
 * AEP设备数据统计分析工具
 * 提供设备数据的统计分析功能
 *
 * @author AEP Integration Team
 * @version 1.0
 * @date 2024-12-29
 */

class AEPDeviceStats {
    constructor(devices = []) {
        this.devices = devices;
    }

    /**
     * 设置设备数据
     * @param {Array} devices - 设备数组
     * @returns {AEPDeviceStats} 支持链式调用
     */
    setDevices(devices) {
        this.devices = devices;
        return this;
    }

    /**
     * 基础统计信息
     * @returns {Object} 基础统计结果
     */
    getBasicStats() {
        const total = this.devices.length;

        // 设备状态统计
        const statusStats = this.devices.reduce((acc, device) => {
            acc[device.deviceStatus] = (acc[device.deviceStatus] || 0) + 1;
            return acc;
        }, {});

        // 网络状态统计
        const netStats = this.devices.reduce((acc, device) => {
            const status = device.netStatus === null ? 'unknown' : device.netStatus;
            acc[status] = (acc[status] || 0) + 1;
            return acc;
        }, {});

        // 协议统计
        const protocolStats = this.devices.reduce((acc, device) => {
            acc[device.productProtocol] = (acc[device.productProtocol] || 0) + 1;
            return acc;
        }, {});

        return {
            total,
            deviceStatus: {
                registered: statusStats[0] || 0,    // 已注册
                activated: statusStats[1] || 0,     // 已激活
                deactivated: statusStats[2] || 0    // 已注销
            },
            networkStatus: {
                online: netStats[1] || 0,           // 在线
                offline: netStats[2] || 0,          // 离线
                unknown: netStats['unknown'] || 0   // 未知
            },
            protocols: protocolStats
        };
    }

    /**
     * 时间相关统计
     * @returns {Object} 时间统计结果
     */
    getTimeStats() {
        const now = Date.now();
        const oneHour = 60 * 60 * 1000;
        const oneDay = 24 * oneHour;
        const oneWeek = 7 * oneDay;
        const oneMonth = 30 * oneDay;

        // 创建时间分析
        const createTimes = this.devices
            .map(d => d.createTime)
            .filter(t => t)
            .sort((a, b) => a - b);

        // 最近活动分析
        const recentActivity = {
            lastHour: 0,
            lastDay: 0,
            lastWeek: 0,
            lastMonth: 0
        };

        this.devices.forEach(device => {
            const lastActivity = device.onlineAt || device.updateTime || device.createTime;
            if (!lastActivity) return;

            const timeDiff = now - lastActivity;
            if (timeDiff <= oneHour) recentActivity.lastHour++;
            if (timeDiff <= oneDay) recentActivity.lastDay++;
            if (timeDiff <= oneWeek) recentActivity.lastWeek++;
            if (timeDiff <= oneMonth) recentActivity.lastMonth++;
        });

        return {
            creation: {
                earliest: createTimes[0] ? new Date(createTimes[0]) : null,
                latest: createTimes[createTimes.length - 1] ? new Date(createTimes[createTimes.length - 1]) : null,
                span: createTimes.length > 1 ? createTimes[createTimes.length - 1] - createTimes[0] : 0
            },
            recentActivity
        };
    }

    /**
     * IMEI分析
     * @returns {Object} IMEI统计结果
     */
    getIMEIAnalysis() {
        const imeiData = this.devices
            .filter(d => d.deviceSn && /^\d{15}$/.test(d.deviceSn))
            .map(d => d.deviceSn);

        // TAC分析(前8位)
        const tacStats = imeiData.reduce((acc, imei) => {
            const tac = imei.substring(0, 8);
            acc[tac] = (acc[tac] || 0) + 1;
            return acc;
        }, {});

        // 运营商分析(前6位)
        const operatorStats = imeiData.reduce((acc, imei) => {
            const prefix = imei.substring(0, 6);
            acc[prefix] = (acc[prefix] || 0) + 1;
            return acc;
        }, {});

        // IMEI有效性检查
        const invalidIMEIs = imeiData.filter(imei => {
            const checkDigit = this.calculateIMEIChecksum(imei.substring(0, 14));
            return checkDigit !== parseInt(imei.charAt(14));
        });

        return {
            total: imeiData.length,
            tacDistribution: tacStats,
            operatorDistribution: operatorStats,
            invalidCount: invalidIMEIs.length,
            validityRate: imeiData.length > 0 ? ((imeiData.length - invalidIMEIs.length) / imeiData.length * 100).toFixed(2) + '%' : '0%'
        };
    }

    /**
     * 设备健康度评估
     * @returns {Object} 健康度统计
     */
    getHealthStats() {
        const now = Date.now();
        const oneDay = 24 * 60 * 60 * 1000;
        const oneWeek = 7 * oneDay;

        let healthy = 0;        // 健康: 在线且最近7天内有活动
        let warning = 0;        // 警告: 已激活但离线超过1天
        let critical = 0;       // 危急: 长时间离线或从未上线
        let inactive = 0;       // 非活跃: 未激活或已注销

        this.devices.forEach(device => {
            if (device.deviceStatus !== 1) {
                // 未激活或已注销
                inactive++;
                return;
            }

            const lastActivity = device.onlineAt || device.updateTime;

            if (device.netStatus === 1 && lastActivity && (now - lastActivity) <= oneWeek) {
                // 在线且最近有活动
                healthy++;
            } else if (device.netStatus === 2 && lastActivity && (now - lastActivity) <= oneDay) {
                // 离线但最近有活动
                warning++;
            } else {
                // 长时间离线或从未上线
                critical++;
            }
        });

        const total = this.devices.length;
        return {
            healthy: { count: healthy, percentage: (healthy / total * 100).toFixed(1) },
            warning: { count: warning, percentage: (warning / total * 100).toFixed(1) },
            critical: { count: critical, percentage: (critical / total * 100).toFixed(1) },
            inactive: { count: inactive, percentage: (inactive / total * 100).toFixed(1) }
        };
    }

    /**
     * 按时间段统计设备创建趋势
     * @param {string} period - 统计周期('hour', 'day', 'week', 'month')
     * @returns {Object} 时间段统计结果
     */
    getCreationTrend(period = 'day') {
        const trend = {};
        const devices = this.devices.filter(d => d.createTime);

        devices.forEach(device => {
            const date = new Date(device.createTime);
            let key;

            switch (period) {
                case 'hour':
                    key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
                    break;
                case 'day':
                    key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
                    break;
                case 'week':
                    const weekStart = new Date(date);
                    weekStart.setDate(date.getDate() - date.getDay());
                    key = `${weekStart.getFullYear()}-W${String(Math.ceil((weekStart.getDate()) / 7)).padStart(2, '0')}`;
                    break;
                case 'month':
                    key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
                    break;
            }

            trend[key] = (trend[key] || 0) + 1;
        });

        // 转换为数组格式并排序
        const trendArray = Object.entries(trend)
            .map(([period, count]) => ({ period, count }))
            .sort((a, b) => a.period.localeCompare(b.period));

        return {
            period,
            data: trendArray,
            total: devices.length,
            peak: trendArray.reduce((max, item) => item.count > max.count ? item : max, { count: 0 })
        };
    }

    /**
     * 在线率统计
     * @returns {Object} 在线率统计
     */
    getOnlineRateStats() {
        const activatedDevices = this.devices.filter(d => d.deviceStatus === 1);
        const onlineDevices = activatedDevices.filter(d => d.netStatus === 1);

        const totalRate = this.devices.length > 0 ?
            (onlineDevices.length / this.devices.length * 100).toFixed(2) : 0;

        const activatedRate = activatedDevices.length > 0 ?
            (onlineDevices.length / activatedDevices.length * 100).toFixed(2) : 0;

        return {
            total: {
                online: onlineDevices.length,
                total: this.devices.length,
                rate: totalRate + '%'
            },
            activated: {
                online: onlineDevices.length,
                total: activatedDevices.length,
                rate: activatedRate + '%'
            }
        };
    }

    /**
     * 生成完整统计报告
     * @returns {Object} 完整统计报告
     */
    generateReport() {
        return {
            timestamp: new Date(),
            basic: this.getBasicStats(),
            time: this.getTimeStats(),
            imei: this.getIMEIAnalysis(),
            health: this.getHealthStats(),
            onlineRate: this.getOnlineRateStats(),
            creationTrend: this.getCreationTrend('day')
        };
    }

    /**
     * 计算IMEI校验位
     * @param {string} imei14 - 前14位IMEI
     * @returns {number} 校验位
     */
    calculateIMEIChecksum(imei14) {
        let sum = 0;
        for (let i = 0; i < 14; i++) {
            let digit = parseInt(imei14.charAt(i));
            if (i % 2 === 1) {
                digit *= 2;
                if (digit > 9) digit = Math.floor(digit / 10) + (digit % 10);
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * 格式化统计报告为可读文本
     * @param {Object} report - 统计报告
     * @returns {string} 格式化的报告文本
     */
    static formatReport(report) {
        const lines = [];
        lines.push('=== AEP设备统计报告 ===');
        lines.push(`生成时间: ${report.timestamp.toLocaleString('zh-CN')}`);
        lines.push('');

        // 基础统计
        lines.push('📊 基础统计:');
        lines.push(`  设备总数: ${report.basic.total} 台`);
        lines.push(`  已注册: ${report.basic.deviceStatus.registered} 台`);
        lines.push(`  已激活: ${report.basic.deviceStatus.activated} 台`);
        lines.push(`  已注销: ${report.basic.deviceStatus.deactivated} 台`);
        lines.push('');

        // 网络状态
        lines.push('🌐 网络状态:');
        lines.push(`  在线: ${report.basic.networkStatus.online} 台`);
        lines.push(`  离线: ${report.basic.networkStatus.offline} 台`);
        lines.push(`  未知: ${report.basic.networkStatus.unknown} 台`);
        lines.push(`  在线率: ${report.onlineRate.activated.rate}`);
        lines.push('');

        // 健康度
        lines.push('💚 设备健康度:');
        lines.push(`  健康: ${report.health.healthy.count} 台 (${report.health.healthy.percentage}%)`);
        lines.push(`  警告: ${report.health.warning.count} 台 (${report.health.warning.percentage}%)`);
        lines.push(`  危急: ${report.health.critical.count} 台 (${report.health.critical.percentage}%)`);
        lines.push(`  非活跃: ${report.health.inactive.count} 台 (${report.health.inactive.percentage}%)`);
        lines.push('');

        // IMEI分析
        lines.push('📱 IMEI分析:');
        lines.push(`  有效IMEI: ${report.imei.total} 个`);
        lines.push(`  有效性: ${report.imei.validityRate}`);
        lines.push(`  无效IMEI: ${report.imei.invalidCount} 个`);
        lines.push('');

        // 协议分布
        lines.push('🔌 协议分布:');
        Object.entries(report.basic.protocols).forEach(([protocol, count]) => {
            const protocolNames = {
                1: 'T-LINK', 2: 'MQTT', 3: 'LWM2M', 4: 'TUP',
                5: 'HTTP', 6: 'JT/T808', 7: 'TCP', 8: '私有TCP',
                9: '私有UDP', 10: '网关MQTT', 11: '南向云'
            };
            lines.push(`  ${protocolNames[protocol] || '未知'}: ${count} 台`);
        });
        lines.push('');

        // 创建趋势
        if (report.creationTrend.data.length > 0) {
            lines.push('📈 创建趋势 (最近10天):');
            report.creationTrend.data.slice(-10).forEach(item => {
                lines.push(`  ${item.period}: ${item.count} 台`);
            });
            lines.push(`  峰值: ${report.creationTrend.peak.period} (${report.creationTrend.peak.count} 台)`);
        }

        return lines.join('\n');
    }
}

// 导出模块
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AEPDeviceStats;
} else if (typeof window !== 'undefined') {
    window.AEPDeviceStats = AEPDeviceStats;
}

// 使用示例
/*
const fs = require('fs');

// 读取设备数据
const rawData = JSON.parse(fs.readFileSync('raw-response.json', 'utf8'));
const devices = rawData.result.list;

// 创建统计分析器
const stats = new AEPDeviceStats(devices);

// 生成完整报告
const report = stats.generateReport();

// 输出格式化报告
console.log(AEPDeviceStats.formatReport(report));

// 单独查看某些统计
console.log('\n=== 详细健康度分析 ===');
console.log(JSON.stringify(stats.getHealthStats(), null, 2));

console.log('\n=== IMEI分析 ===');
const imeiAnalysis = stats.getIMEIAnalysis();
console.log(`TAC分布:`, Object.entries(imeiAnalysis.tacDistribution)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([tac, count]) => `${tac}: ${count}台`)
    .join(', '));

console.log('\n=== 创建趋势分析 ===');
const trend = stats.getCreationTrend('week');
console.log(`按周统计:`);
trend.data.forEach(item => {
    console.log(`  ${item.period}: ${item.count} 台`);
});
*/