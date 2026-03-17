# AEP设备档案
## API列表
|API名称 | 安全认证方式 | 签名认证方式 | 描述 |
|:-------|:------|:--------|:--------|
|DeleteArchivesInfo|none|hmac-sha1|删除设备档案|
|GetArchivesAttribute|none|hmac-sha1|查询设备档案属性列表|
|UpdateArchivesInfo|none|hmac-sha1|编辑设备档案|
|AddArchivesInfo|none|hmac-sha1|新增设备档案|
|GetArchivesInfo|none|hmac-sha1|查询设备档案|
|GetDeviceType|none|hmac-sha1|查询设备类型|

## API调用
### 请求地址

|环境 | HTTP请求地址  | HTTPS请求地址 |
|:-------|:------|:--------|
|正式环境|10433748.api.ctwing.cn/device_archives|10433748.api.ctwing.cn/device_archives|

### 公共入参

公共请求参数是指每个接口都需要使用到的请求参数。

|参数名称 | 含义  | 位置 | 描述|
|:-------|:------|:--------|:--------|
|application|应用标识|HEAD|应用的App Key，如果需要进行签名认证则需要填写，例如：dAaFG7DiPt8|
|signature|签名数据|HEAD|将业务数据连同timestamp、application一起签名后的数据，如果需要进行签名认证则需要填写|
|timestamp|UNIX格式时间戳|HEAD|如果需要进行签名认证则需要填写，例如：1515752817580|
|version|API版本号|HEAD|可以指定API版本号访问历史版本|

## API 文档说明
### API名称：DeleteArchivesInfo   版本号: 20231117042743

#### 描述

删除设备档案

#### 请求信息

请求路径：/deleteArchivesInfo

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "deletes": [
    {
      "deviceId": "string",
      "productId": 0
    },
    {
      "deviceId": "string",
      "productId": 0
    }
  ]
}

描述：
可批量删除
deviceId：设备Id，必填，不能超过64个字符
productId：对应设备的产品Id，必填

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": []
}

##### 异常返回示例
{
  "code": 0,
  "msg": "ok",
  "result": [deviceId1, deviceId2]
}

说明：删除失败会返回失败的deviceId列表

#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

### API名称：GetArchivesAttribute   版本号: 20231117042748

#### 描述

查询设备档案属性列表

#### 请求信息

请求路径：/getArchivesAttr

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "attributeType": 0,
  "pageNow": 0,
  "pageSize": 0,
  "searchIndex": 0,
  "searchValue": "string"
}

描述：
attributeType: 查询属性类型, 0-基本属性, 1-自定义属性, 
searchIndex: 搜索条件的索引,0-无条件、1-属性名称、2-属性标识、3-数据类型、4-数据定义, 
searchValue: 搜索条件的值, 
             当searchIndex为0时，searchValue的值不需要填写，会查询所有属性
             当searchIndex为3时，searchValue的值可为0-字符、1-日期、2-数字、3-布尔
             当searchIndex为4时，searchValue的值可为0-必选、1-可选
pageNow: 当前页码, 
pageSize: 每页条数

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "pageNum": 1,
    "pageSize": 5,
    "total": 5,
    "list": [
      {
        "attributeName": "设备ID",
        "attributeMark": "device_id",
        "datatype": 0,
        "selected": 0,
        "description": "设备ID"
      },
      {
        "attributeName": "设备名称",
        "attributeMark": "device_name",
        "datatype": 0,
        "selected": 0,
        "description": "设备名称"
      },
      {
        "attributeName": "产品ID",
        "attributeMark": "product_id",
        "datatype": 2,
        "selected": 0,
        "description": "产品ID"
      },
      {
        "attributeName": "租户ID",
        "attributeMark": "tenant_id",
        "datatype": 0,
        "selected": 0,
        "description": "租户ID"
      },
      {
        "attributeName": "设备编码",
        "attributeMark": "device_code",
        "datatype": 0,
        "selected": 1,
        "description": "设备标准编码/工业标识"
      }
    ]
  }
}

##### 异常返回示例


#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

### API名称：UpdateArchivesInfo   版本号: 20231117042738

#### 描述

编辑设备档案

#### 请求信息

请求路径：/updateArchivesInfo

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "constructionContact": "string",
  "constructionName": "string",
  "constructionPhone": "string",
  "customAttributes": {},
  "deviceAddress": "string",
  "deviceCode": "string",
  "deviceId": "string",
  "installTime": "yyyy-MM-dd HH:mm:ss",
  "latitude": "string",
  "longitude": "string",
  "maintenanceContact": "string",
  "maintenanceName": "string",
  "maintenancePhone": "string",
  "managementContact": "string",
  "managementName": "string",
  "managementPhone": "string",
  "mapType": 0,
  "operator": "string",
  "productId": 0,
  "regionNote": "string",
  "deviceType": "string",
  "remarks": "string"
}

描述：
deviceId：设备Id，必填，不能超过64个字符
productId：产品Id，必填
deviceCode：设备编码，非必填，不能超过256个字符
regionNote：所属区域名称，必填，不能超过128个字符，如“江苏省-南京市-鼓楼区”，区域名称需真实有效，具体请参考“控制台→设备档案”的所属区域选择框
deviceAddress：详细安装地址，必填，不能超过128个字符
installTime：安装时间，必填，格式必须是“yyyy-MM-dd HH:mm:ss”的形式
mapType：地图坐标系类型，必填，1-WGS84，2-高德，3-百度
longitude：经度，必填，数值范围(-180.0,180.0)
latitude：纬度，必填，数值范围(-90.0,90.0)
constructionName：建设单位名称，必填，不能超过128个字符
constructionContact：建设单位联系人姓名，必填，不能超过32个字符
constructionPhone：建设单位联系人电话号码，必填，不能超过32个字符
managementName：管理单位名称，必填，不能超过128个字符
managementContact：管理单位联系人姓名，必填，不能超过32个字符
managementPhone：管理单位联系人电话号码，必填，不能超过32个字符
maintenanceName：维护单位名称，必填，不能超过128个字符
maintenanceContact：维护单位联系人姓名，必填，不能超过32个字符
maintenancePhone：维护单位联系人电话号码，必填，不能超过32个字符
deviceType：设备类型，非必填，不能超过10个字符
remarks：备注，非必填，不能超过128个字符
customAttributes：自定义属性，必填，属性个数不超过10个，字符总长度不超过2048
operator：操作人，必填，不能超过32个字符

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": "更新设备档案成功"
}

##### 异常返回示例


#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

### API名称：AddArchivesInfo   版本号: 20231215034317

#### 描述

新增设备档案

#### 请求信息

请求路径：/addArchivesInfo

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "constructionContact": "string",
  "constructionName": "string",
  "constructionPhone": "string",
  "customAttributes": {},
  "deviceAddress": "string",
  "deviceCode": "string",
  "deviceId": "string",
  "installTime": "yyyy-MM-dd HH:mm:ss",
  "latitude": "string",
  "longitude": "string",
  "maintenanceContact": "string",
  "maintenanceName": "string",
  "maintenancePhone": "string",
  "managementContact": "string",
  "managementName": "string",
  "managementPhone": "string",
  "mapType": 0,
  "operator": "string",
  "productId": 0,
  "regionNote": "string",
  "deviceType": "string",
  "remarks": "string"
}

描述：
deviceId：设备Id，必填，不能超过64个字符
productId：产品Id，必填
deviceCode：设备编码，非必填，不能超过256个字符
regionNote：所属区域名称，必填，不能超过128个字符，如“江苏省-南京市-鼓楼区”，区域名称需真实有效，具体请参考“控制台→设备档案”的所属区域选择框
deviceAddress：详细安装地址，必填，不能超过128个字符
installTime：安装时间，必填，格式必须是“yyyy-MM-dd HH:mm:ss”的形式
mapType：地图坐标系类型，必填，1-WGS84，2-高德，3-百度
longitude：经度，必填，数值范围(-180.0,180.0)
latitude：纬度，必填，数值范围(-90.0,90.0)
constructionName：建设单位名称，必填，不能超过128个字符
constructionContact：建设单位联系人姓名，必填，不能超过32个字符
constructionPhone：建设单位联系人电话号码，必填，不能超过32个字符
managementName：管理单位名称，必填，不能超过128个字符
managementContact：管理单位联系人姓名，必填，不能超过32个字符
managementPhone：管理单位联系人电话号码，必填，不能超过32个字符
maintenanceName：维护单位名称，必填，不能超过128个字符
maintenanceContact：维护单位联系人姓名，必填，不能超过32个字符
maintenancePhone：维护单位联系人电话号码，必填，不能超过32个字符
deviceType：设备类型，非必填，不能超过10个字符
remarks：备注，非必填，不能超过128个字符
customAttributes：自定义属性，必填，属性个数不超过10个，字符总长度不超过2048
operator：操作人，必填，不能超过32个字符

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": "添加设备档案成功"
}

##### 异常返回示例
{
  "code": 8001,
  "msg": "error",
  "result": "设备档案创建失败"
}

#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

### API名称：GetArchivesInfo   版本号: 20231215034340

#### 描述

查询设备档案

#### 请求信息

请求路径：/getArchivesInfo

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|searchValueType|QUERY|Long|false|1：按设备id查询，2：按设备类型查询|
|searchValue|QUERY|String|false||
|pageNow|QUERY|Long|true||
|pageSize|QUERY|Long|true||


#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "list": [
      {
        "deviceId": "string",
        "deviceName": "string",
        "productId": 0,
        "deviceCode": "string",
        "region": 0,
        "zone": "string",
        "deviceAddress": "string",
        "installTime": "string",
        "mapType": 1,
        "longitude": 1,
        "latitude": 1,
        "constructionName": "string",
        "constructionContact": "string",
        "constructionPhone": "111",
        "managementName": "string",
        "managementContact": "string",
        "managementPhone": "111",
        "maintenanceName": "string",
        "maintenanceContact": "string",
        "maintenancePhone": "111",
        "deviceType": "string",
        "remarks": "string",
        "customAttributes": null
      }
    ]
  }
}

##### 异常返回示例
{
  "code": 8011,
  "msg": "error",
  "result": "设备档案查询失败"
}

#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

### API名称：GetDeviceType   版本号: 20231215034248

#### 描述

查询设备类型

#### 请求信息

请求路径：/getDeviceType

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "pageNow": 0,
  "pageSize": 0,
  "searchValue": "string"
}

描述：
pageNow：当前页码，必填，>0
pageSize：每页条数，必填，范围1-100
searchValue：搜索条件，非必填，不超过10个字符

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "list": [
      {
        "typeName": "string",
        "id": 1,
        "tenantId": "string",
        "operator": "string",
        "operateTime": 1701238141000
      }
    ]
  }
}

##### 异常返回示例
{
  "code": 8803,
  "msg": "搜索条件中的值长度不超过10",
  "result": null
}

#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

