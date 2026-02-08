/**
 * AEP设备列表数据解析器
 * 用于解析和处理AEP设备管理API返回的设备列表数据
 *
 * @author AEP Integration Team
 * @version 1.0
 * @date 2024-12-29
 */

class AEPDeviceListParser {
    constructor() {
        // 设备状态映射
        this.deviceStatusMap = {
            0: '已注册',
            1: '已激活',
            2: '已注销'
        };

        // 网络状态映射
        this.netStatusMap = {
            1: '在线',
            2: '离线',
            null: '未知'
        };

        // 协议类型映射
        this.protocolMap = {
            1: 'T-LINK',
            2: 'MQTT',
            3: 'LWM2M',
            4: 'TUP',
            5: 'HTTP',
            6: 'JT/T808',
            7: 'TCP',
            8: '私有TCP',
            9: '私有UDP',
            10: '网关产品MQTT',
            11: '南向云'
        };
    }

    /**
     * 解析设备列表响应数据
     * @param {Object} response - API响应对象
     * @returns {Object} 解析后的数据结构
     */
    parseResponse(response) {
        if (!response || response.code !== '0') {
            throw new Error('Invalid response or API error: ' + (response?.msg || 'Unknown error'));
        }

        const result = response.result;
        return {
            pagination: {
                pageNum: result.pageNum,
                pageSize: result.pageSize,
                total: result.total,
                totalPages: Math.ceil(result.total / result.pageSize)
            },
            devices: result.list.map(device => this.parseDevice(device))
        };
    }

    /**
     * 解析单个设备对象
     * @param {Object} device - 原始设备对象
     * @returns {Object} 解析后的设备对象
     */
    parseDevice(device) {
        return {
            // 基本信息
            id: device.deviceId,
            name: device.deviceName,
            sn: device.deviceSn,
            tenantId: device.tenantId,
            productId: device.productId,

            // 状态信息
            status: {
                device: {
                    code: device.deviceStatus,
                    text: this.deviceStatusMap[device.deviceStatus] || '未知状态'
                },
                network: {
                    code: device.netStatus,
                    text: this.netStatusMap[device.netStatus] || '未知状态'
                }
            },

            // 协议信息
            protocol: {
                code: device.productProtocol,
                name: this.protocolMap[device.productProtocol] || '未知协议'
            },

            // 版本信息
            firmware: device.firmwareVersion || null,

            // 时间信息
            timestamps: {
                created: device.createTime ? new Date(device.createTime) : null,
                updated: device.updateTime ? new Date(device.updateTime) : null,
                activated: device.activeTime ? new Date(device.activeTime) : null,
                loggedOut: device.logoutTime ? new Date(device.logoutTime) : null,
                lastOnline: device.onlineAt ? new Date(device.onlineAt) : null,
                lastOffline: device.offlineAt ? new Date(device.offlineAt) : null
            },

            // 原始数据(用于调试)
            _raw: device
        };
    }

    /**
     * 格式化时间戳
     * @param {number} timestamp - Unix时间戳(毫秒)
     * @returns {string} 格式化后的时间字符串
     */
    formatTimestamp(timestamp) {
        if (!timestamp) return '未设置';
        return new Date(timestamp).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }

    /**
     * 计算设备在线时长
     * @param {Object} device - 解析后的设备对象
     * @returns {string} 在线时长描述
     */
    calculateUptime(device) {
        const { lastOnline, lastOffline } = device.timestamps;

        if (!lastOnline) return '从未上线';
        if (device.status.network.code === 1) {
            // 设备在线，计算从上次上线到现在的时长
            const uptimeMs = Date.now() - lastOnline.getTime();
            return this.formatDuration(uptimeMs);
        } else if (lastOffline && lastOffline > lastOnline) {
            // 设备离线，计算上次的在线时长
            const uptimeMs = lastOffline.getTime() - lastOnline.getTime();
            return '上次在线: ' + this.formatDuration(uptimeMs);
        }

        return '状态未知';
    }

    /**
     * 格式化时长
     * @param {number} durationMs - 时长(毫秒)
     * @returns {string} 格式化后的时长字符串
     */
    formatDuration(durationMs) {
        const seconds = Math.floor(durationMs / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) return `${days}天${hours % 24}小时`;
        if (hours > 0) return `${hours}小时${minutes % 60}分钟`;
        if (minutes > 0) return `${minutes}分钟${seconds % 60}秒`;
        return `${seconds}秒`;
    }

    /**
     * 验证设备数据完整性
     * @param {Object} device - 设备对象
     * @returns {Object} 验证结果
     */
    validateDevice(device) {
        const issues = [];

        // 检查必填字段
        const requiredFields = ['deviceId', 'deviceName', 'tenantId', 'productId', 'deviceStatus'];
        for (const field of requiredFields) {
            if (!device[field] && device[field] !== 0) {
                issues.push(`缺少必填字段: ${field}`);
            }
        }

        // 检查IMEI格式(如果适用)
        if (device.deviceSn && /^\d{15}$/.test(device.deviceSn)) {
            // IMEI应该是15位数字
            const checkDigit = this.calculateIMEIChecksum(device.deviceSn.substring(0, 14));
            if (checkDigit !== parseInt(device.deviceSn.charAt(14))) {
                issues.push('IMEI校验位不正确');
            }
        }

        // 检查时间逻辑
        if (device.createTime && device.activeTime && device.activeTime < device.createTime) {
            issues.push('激活时间不能早于创建时间');
        }

        return {
            valid: issues.length === 0,
            issues: issues
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
}

// 使用示例
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AEPDeviceListParser;
} else if (typeof window !== 'undefined') {
    window.AEPDeviceListParser = AEPDeviceListParser;
}

// 示例用法
/*
const fs = require('fs');
const parser = new AEPDeviceListParser();

// 从文件读取原始响应数据
const rawData = JSON.parse(fs.readFileSync('raw-response.json', 'utf8'));

// 解析数据
const parsedData = parser.parseResponse(rawData);

console.log('分页信息:', parsedData.pagination);
console.log('设备总数:', parsedData.devices.length);

// 显示前5个设备的详细信息
parsedData.devices.slice(0, 5).forEach((device, index) => {
    console.log(`\n设备 ${index + 1}:`);
    console.log('  名称:', device.name);
    console.log('  状态:', device.status.device.text);
    console.log('  网络:', device.status.network.text);
    console.log('  协议:', device.protocol.name);
    console.log('  创建时间:', parser.formatTimestamp(device.timestamps.created?.getTime()));
    console.log('  在线时长:', parser.calculateUptime(device));

    // 验证数据完整性
    const validation = parser.validateDevice(device._raw);
    if (!validation.valid) {
        console.log('  数据问题:', validation.issues.join(', '));
    }
});
*/