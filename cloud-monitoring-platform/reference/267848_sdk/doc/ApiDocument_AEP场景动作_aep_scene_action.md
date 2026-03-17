# AEP场景动作
## API列表
|API名称 | 安全认证方式 | 签名认证方式 | 描述 |
|:-------|:------|:--------|:--------|
|QueryActionList|none|hmac-sha1|查询动作列表|
|QuerySceneList|none|hmac-sha1|查询场景列表|
|ActionExecute|none|hmac-sha1|动作执行触发API|
|SceneExecute|none|hmac-sha1|场景执行触发API|

## API调用
### 请求地址

|环境 | HTTP请求地址  | HTTPS请求地址 |
|:-------|:------|:--------|
|正式环境|10433748.api.ctwing.cn/aep_scene_action|10433748.api.ctwing.cn/aep_scene_action|

### 公共入参

公共请求参数是指每个接口都需要使用到的请求参数。

|参数名称 | 含义  | 位置 | 描述|
|:-------|:------|:--------|:--------|
|application|应用标识|HEAD|应用的App Key，如果需要进行签名认证则需要填写，例如：dAaFG7DiPt8|
|signature|签名数据|HEAD|将业务数据连同timestamp、application一起签名后的数据，如果需要进行签名认证则需要填写|
|timestamp|UNIX格式时间戳|HEAD|如果需要进行签名认证则需要填写，例如：1515752817580|
|version|API版本号|HEAD|可以指定API版本号访问历史版本|

## API 文档说明
### API名称：QueryActionList   版本号: 20240126035641

#### 描述

查询动作列表

#### 请求信息

请求路径：/getActionList

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|productId|QUERY|Long|false|可通过产品ID搜索|
|pageNow|QUERY|Long|false|当前页|
|pageSize|QUERY|Long|false|每页记录数|


#### 返回信息

##### 返回参数类型
json

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
        "actionId": 12,
        "tenantId": "300",
        "deptCode": "1_1",
        "productId": 15198014,
        "actionName": "test2",
        "action": {
          "payload": "{\"abc\":1}"
        },
        "createTime": 1703141234000,
        "updateTime": 1703473698000,
        "actionDesc": "ddd"
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

### API名称：QuerySceneList   版本号: 20240126035701

#### 描述

查询场景列表

#### 请求信息

请求路径：/getSceneList

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|pageNow|QUERY|Long|false||
|pageSize|QUERY|Long|false||


#### 返回信息

##### 返回参数类型
json

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
        "sceneId": 4,
        "tenantId": "300",
        "deptCode": "0",
        "sceneName": "test4",
        "actionGroups": [
          {
            "productId": 15196582,
            "actionIds": [1001,1002]
          }
        ]
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

### API名称：ActionExecute   版本号: 20240126035654

#### 描述

动作执行触发API

#### 请求信息

请求路径：/action/execute

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "actionId": 1,
  "deviceIds": [
    "2000435412334567890"
  ],
  "operator": "admin",
  "productId": "20004354"
}

actionId:动作ID（必填),
deviceIds:设备ID列表 (必填),列表最多5个设备ID
operator:操作者(必填),
productId:产品ID(必填)

#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
    "code": 0,
    "msg": "ok",
    "result": {
        "productId": 20004354,
        "operator": "admin",
        "actionId": 1,
        "id": "XeDtd2",
        "actionName": "tempControl",
        "timeStamp": 1702364474873,
        "commandResps": [
            {
                "commandId": "49007",
                "command": "{\"a1\":10,\"str_id\":\"1111111111111111111111\"}",
                "commandStatus": "指令已保存",
                "productId": 20004354,
                "deviceId": "2000435412334567890",
                "imei": "923456789423584",
                "createBy": "admin",
                "createTime": 1702364474784,
                "ttl": 7200,
                "resultCode": 0,
                "reason": "ok"
            }
        ]
    }
}

##### 异常返回示例
{
  "code": 8803,
  "msg": "产品ID无效",
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

### API名称：SceneExecute   版本号: 20240126035634

#### 描述

场景执行触发API

#### 请求信息

请求路径：/scene/execute

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "actionGroupArgs": [
    {
      "deviceIds": [
        "2000435412334567890"
      ],
      "productId": 20004354
    }
  ],
  "operator": "admin",
  "sceneId": 1
}

actionGroupArgs:动作组参数列表(必填),最大5个
deviceIds:设备ID列表(必填),最大5个
20004354:产品ID(必填),
operator:操作者(必填),
sceneId:场景ID（必填)

#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
    "code": 0,
    "msg": "ok",
    "result": {
        "sceneId": 1,
        "sceneName": "gohome",
        "timeStamp": 1702364613236,
        "id": "Exfec3f",
        "actionGroupResps": [
            {
                "productId": 20004354,
                "actionGroupSeq": 1,
                "actionExecResps": [
                    {
                        "productId": 20004354,
                        "operator": "admin",
                        "actionId": 1,
                        "id": "beFtd2",
                        "actionName": "tempControl",
                        "timeStamp": 1702364613238,
                        "commandResps": [
                            {
                                "commandId": "4238",
                                "command": "{\"a1\":10,\"str_id\":\"1111111111111111111111\"}",
                                "commandStatus": "指令已保存",
                                "productId": 20004354,
                                "deviceId": "2000435412334567890",
                                "imei": "923456789423584",
                                "createBy": "admin",
                                "createTime": 1702364613358,
                                "ttl": 7200,
                                "resultCode": 0,
                                "reason": "ok"
                            }
                        ],
                        "actionSeq": 1,
                        "sceneId": 1,
                        "resultCode": 0,
                        "reason": "ok"
                    }
                ]
            }
        ]
    }
}

##### 异常返回示例
{
  "code": 8803,
  "msg": "产品ID无效",
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

