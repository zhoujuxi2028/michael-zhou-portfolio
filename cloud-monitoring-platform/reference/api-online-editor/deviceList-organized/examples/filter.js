/**
 * AEP设备数据过滤和查询工具
 * 提供各种设备数据过滤和查询功能
 *
 * @author AEP Integration Team
 * @version 1.0
 * @date 2024-12-29
 */

class AEPDeviceFilter {
    constructor(devices = []) {
        this.devices = devices;
    }

    /**
     * 设置设备数据
     * @param {Array} devices - 设备数组
     * @returns {AEPDeviceFilter} 支持链式调用
     */
    setDevices(devices) {
        this.devices = devices;
        return this;
    }

    /**
     * 按设备状态过滤
     * @param {number|Array} status - 设备状态(0:已注册, 1:已激活, 2:已注销)
     * @returns {Array} 过滤后的设备列表
     */
    filterByDeviceStatus(status) {
        const statusArray = Array.isArray(status) ? status : [status];
        return this.devices.filter(device =>
            statusArray.includes(device.deviceStatus)
        );
    }

    /**
     * 按网络状态过滤
     * @param {number|Array} netStatus - 网络状态(1:在线, 2:离线, null:未知)
     * @returns {Array} 过滤后的设备列表
     */
    filterByNetworkStatus(netStatus) {
        const statusArray = Array.isArray(netStatus) ? netStatus : [netStatus];
        return this.devices.filter(device =>
            statusArray.includes(device.netStatus)
        );
    }

    /**
     * 按协议类型过滤
     * @param {number|Array} protocol - 协议类型
     * @returns {Array} 过滤后的设备列表
     */
    filterByProtocol(protocol) {
        const protocolArray = Array.isArray(protocol) ? protocol : [protocol];
        return this.devices.filter(device =>
            protocolArray.includes(device.productProtocol)
        );
    }

    /**
     * 按设备名称模糊搜索
     * @param {string} keyword - 搜索关键词
     * @param {boolean} caseSensitive - 是否区分大小写，默认false
     * @returns {Array} 匹配的设备列表
     */
    searchByName(keyword, caseSensitive = false) {
        const searchTerm = caseSensitive ? keyword : keyword.toLowerCase();
        return this.devices.filter(device => {
            const deviceName = caseSensitive ? device.deviceName : device.deviceName.toLowerCase();
            return deviceName.includes(searchTerm);
        });
    }

    /**
     * 按IMEI搜索
     * @param {string} imei - IMEI号(可以是部分IMEI)
     * @returns {Array} 匹配的设备列表
     */
    searchByIMEI(imei) {
        return this.devices.filter(device =>
            device.deviceSn && device.deviceSn.includes(imei)
        );
    }

    /**
     * 按设备ID搜索
     * @param {string} deviceId - 设备ID(可以是部分ID)
     * @returns {Array} 匹配的设备列表
     */
    searchByDeviceId(deviceId) {
        return this.devices.filter(device =>
            device.deviceId.includes(deviceId)
        );
    }

    /**
     * 按时间范围过滤
     * @param {number} startTime - 开始时间戳(毫秒)
     * @param {number} endTime - 结束时间戳(毫秒)
     * @param {string} timeField - 时间字段名(createTime, updateTime, activeTime等)
     * @returns {Array} 过滤后的设备列表
     */
    filterByTimeRange(startTime, endTime, timeField = 'createTime') {
        return this.devices.filter(device => {
            const deviceTime = device[timeField];
            return deviceTime && deviceTime >= startTime && deviceTime <= endTime;
        });
    }

    /**
     * 按最近活动时间过滤
     * @param {number} hours - 最近N小时内活动的设备
     * @returns {Array} 最近活动的设备列表
     */
    filterByRecentActivity(hours = 24) {
        const cutoffTime = Date.now() - (hours * 60 * 60 * 1000);
        return this.devices.filter(device => {
            const lastOnline = device.onlineAt;
            return lastOnline && lastOnline >= cutoffTime;
        });
    }

    /**
     * 获取在线设备
     * @returns {Array} 在线设备列表
     */
    getOnlineDevices() {
        return this.filterByNetworkStatus(1);
    }

    /**
     * 获取离线设备
     * @returns {Array} 离线设备列表
     */
    getOfflineDevices() {
        return this.filterByNetworkStatus(2);
    }

    /**
     * 获取已激活设备
     * @returns {Array} 已激活设备列表
     */
    getActivatedDevices() {
        return this.filterByDeviceStatus(1);
    }

    /**
     * 获取未激活设备
     * @returns {Array} 未激活设备列表
     */
    getInactiveDevices() {
        return this.filterByDeviceStatus([0, 2]);
    }

    /**
     * 获取MQTT设备
     * @returns {Array} MQTT协议设备列表
     */
    getMQTTDevices() {
        return this.filterByProtocol(2);
    }

    /**
     * 获取异常设备(长时间离线或状态异常)
     * @param {number} offlineHours - 离线超过N小时视为异常，默认24小时
     * @returns {Object} 异常设备分类
     */
    getAbnormalDevices(offlineHours = 24) {
        const cutoffTime = Date.now() - (offlineHours * 60 * 60 * 1000);

        const longOffline = this.devices.filter(device =>
            device.netStatus === 2 &&
            device.offlineAt &&
            device.offlineAt < cutoffTime
        );

        const neverOnline = this.devices.filter(device =>
            device.deviceStatus === 1 && // 已激活但从未上线
            !device.onlineAt
        );

        const deactivated = this.filterByDeviceStatus(2); // 已注销设备

        return {
            longOffline,        // 长时间离线
            neverOnline,        // 从未上线
            deactivated,        // 已注销
            total: longOffline.length + neverOnline.length + deactivated.length
        };
    }

    /**
     * 复合查询
     * @param {Object} criteria - 查询条件
     * @returns {Array} 符合条件的设备列表
     */
    query(criteria = {}) {
        let result = this.devices;

        // 按设备状态过滤
        if (criteria.deviceStatus !== undefined) {
            result = result.filter(device =>
                Array.isArray(criteria.deviceStatus) ?
                criteria.deviceStatus.includes(device.deviceStatus) :
                device.deviceStatus === criteria.deviceStatus
            );
        }

        // 按网络状态过滤
        if (criteria.netStatus !== undefined) {
            result = result.filter(device =>
                Array.isArray(criteria.netStatus) ?
                criteria.netStatus.includes(device.netStatus) :
                device.netStatus === criteria.netStatus
            );
        }

        // 按协议过滤
        if (criteria.protocol !== undefined) {
            result = result.filter(device =>
                Array.isArray(criteria.protocol) ?
                criteria.protocol.includes(device.productProtocol) :
                device.productProtocol === criteria.protocol
            );
        }

        // 按名称搜索
        if (criteria.name) {
            const searchTerm = criteria.caseSensitive ? criteria.name : criteria.name.toLowerCase();
            result = result.filter(device => {
                const deviceName = criteria.caseSensitive ? device.deviceName : device.deviceName.toLowerCase();
                return deviceName.includes(searchTerm);
            });
        }

        // 按IMEI搜索
        if (criteria.imei) {
            result = result.filter(device =>
                device.deviceSn && device.deviceSn.includes(criteria.imei)
            );
        }

        // 按时间范围过滤
        if (criteria.timeRange) {
            const { start, end, field = 'createTime' } = criteria.timeRange;
            result = result.filter(device => {
                const deviceTime = device[field];
                return deviceTime &&
                       (!start || deviceTime >= start) &&
                       (!end || deviceTime <= end);
            });
        }

        return result;
    }

    /**
     * 排序设备列表
     * @param {Array} devices - 设备列表
     * @param {string} field - 排序字段
     * @param {string} order - 排序方向('asc'或'desc')
     * @returns {Array} 排序后的设备列表
     */
    static sortDevices(devices, field, order = 'asc') {
        return devices.sort((a, b) => {
            let valueA = a[field];
            let valueB = b[field];

            // 处理null/undefined值
            if (valueA == null && valueB == null) return 0;
            if (valueA == null) return order === 'asc' ? 1 : -1;
            if (valueB == null) return order === 'asc' ? -1 : 1;

            // 字符串比较
            if (typeof valueA === 'string' && typeof valueB === 'string') {
                valueA = valueA.toLowerCase();
                valueB = valueB.toLowerCase();
            }

            if (valueA < valueB) return order === 'asc' ? -1 : 1;
            if (valueA > valueB) return order === 'asc' ? 1 : -1;
            return 0;
        });
    }

    /**
     * 分页处理
     * @param {Array} devices - 设备列表
     * @param {number} page - 页码(从1开始)
     * @param {number} pageSize - 每页大小
     * @returns {Object} 分页结果
     */
    static paginate(devices, page = 1, pageSize = 10) {
        const total = devices.length;
        const totalPages = Math.ceil(total / pageSize);
        const startIndex = (page - 1) * pageSize;
        const endIndex = startIndex + pageSize;

        return {
            data: devices.slice(startIndex, endIndex),
            pagination: {
                current: page,
                pageSize,
                total,
                totalPages,
                hasNext: page < totalPages,
                hasPrev: page > 1
            }
        };
    }
}

// 导出模块
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AEPDeviceFilter;
} else if (typeof window !== 'undefined') {
    window.AEPDeviceFilter = AEPDeviceFilter;
}

// 使用示例
/*
const fs = require('fs');

// 读取设备数据
const rawData = JSON.parse(fs.readFileSync('raw-response.json', 'utf8'));
const devices = rawData.result.list;

// 创建过滤器
const filter = new AEPDeviceFilter(devices);

// 示例查询
console.log('=== 设备过滤示例 ===');

// 1. 获取在线设备
const onlineDevices = filter.getOnlineDevices();
console.log(`在线设备: ${onlineDevices.length} 台`);

// 2. 获取MQTT协议设备
const mqttDevices = filter.getMQTTDevices();
console.log(`MQTT设备: ${mqttDevices.length} 台`);

// 3. 搜索特定IMEI
const devicesByIMEI = filter.searchByIMEI('866877072647');
console.log(`IMEI包含"866877072647"的设备: ${devicesByIMEI.length} 台`);

// 4. 复合查询: 在线的MQTT设备
const onlineMQTT = filter.query({
    netStatus: 1,
    protocol: 2
});
console.log(`在线MQTT设备: ${onlineMQTT.length} 台`);

// 5. 获取异常设备
const abnormal = filter.getAbnormalDevices(24);
console.log(`异常设备统计:`);
console.log(`  长时间离线: ${abnormal.longOffline.length} 台`);
console.log(`  从未上线: ${abnormal.neverOnline.length} 台`);
console.log(`  已注销: ${abnormal.deactivated.length} 台`);

// 6. 最近24小时活跃设备
const recentActive = filter.filterByRecentActivity(24);
console.log(`最近24小时活跃设备: ${recentActive.length} 台`);

// 7. 排序和分页示例
const sortedDevices = AEPDeviceFilter.sortDevices(devices, 'createTime', 'desc');
const paginatedResult = AEPDeviceFilter.paginate(sortedDevices, 1, 10);
console.log(`第1页设备(按创建时间倒序): ${paginatedResult.data.length} 台`);
console.log(`总页数: ${paginatedResult.pagination.totalPages}`);
*/