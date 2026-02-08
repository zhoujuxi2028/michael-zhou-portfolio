#!/usr/bin/env python3
"""
测试电信IOT平台连接的Python脚本
用于验证APPID和SECRET是否有效
"""

import requests
import json
import ssl
from urllib3.exceptions import InsecureRequestWarning

# 禁用SSL警告
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# 电信IOT平台配置
BASE_URL = "https://device.api.ct10649.com:8743"
LOGIN_URL = f"{BASE_URL}/iocm/app/sec/v1.1.0/login"

# 从toyou/zc_backend项目获取的认证信息
APP_ID = "ed5a4f1fcb364575a614f70d52a5a1ac"
SECRET = "f8a8df37f85a4b6892a7c058b5bfb655"

def test_login():
    """测试登录认证"""
    print("=" * 50)
    print("测试电信IOT平台连接")
    print("=" * 50)
    print(f"平台地址: {BASE_URL}")
    print(f"登录接口: {LOGIN_URL}")
    print(f"APPID: {APP_ID}")
    print(f"SECRET: {SECRET}")
    print("=" * 50)

    try:
        # 准备登录参数
        login_data = {
            "appId": APP_ID,
            "secret": SECRET
        }

        # 发送登录请求
        print("正在发送登录请求...")
        response = requests.post(
            LOGIN_URL,
            data=login_data,
            verify=False,  # 忽略SSL证书验证
            timeout=30
        )

        print(f"HTTP状态码: {response.status_code}")
        print(f"响应头: {dict(response.headers)}")
        print(f"响应内容: {response.text}")

        if response.status_code == 200:
            try:
                result = response.json()
                if "accessToken" in result:
                    print("✅ 认证成功！获取到accessToken")
                    print(f"AccessToken: {result['accessToken'][:50]}...")
                    return result["accessToken"]
                else:
                    print("❌ 认证失败：未返回accessToken")
                    print(f"错误信息: {result}")
            except json.JSONDecodeError:
                print("❌ 响应不是有效的JSON格式")
        else:
            print(f"❌ 认证失败：HTTP {response.status_code}")

    except requests.exceptions.ConnectTimeout:
        print("❌ 连接超时：无法连接到电信IOT平台")
    except requests.exceptions.ConnectionError as e:
        print(f"❌ 连接错误：{str(e)}")
    except Exception as e:
        print(f"❌ 未知错误：{str(e)}")

    return None

def test_device_query(access_token):
    """测试设备查询"""
    if not access_token:
        print("跳过设备查询测试：无有效token")
        return

    print("\n" + "=" * 50)
    print("测试设备查询接口")
    print("=" * 50)

    query_url = f"{BASE_URL}/iocm/app/dm/v1.4.0/devices"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "app_key": APP_ID,
        "Content-Type": "application/json"
    }

    try:
        print(f"查询地址: {query_url}")
        response = requests.get(
            query_url,
            headers=headers,
            params={"pageNo": 1, "pageSize": 10},
            verify=False,
            timeout=30
        )

        print(f"HTTP状态码: {response.status_code}")
        print(f"响应内容: {response.text}")

        if response.status_code == 200:
            print("✅ 设备查询成功！")
        else:
            print(f"❌ 设备查询失败：HTTP {response.status_code}")

    except Exception as e:
        print(f"❌ 查询错误：{str(e)}")

if __name__ == "__main__":
    # 测试登录
    token = test_login()

    # 测试设备查询
    test_device_query(token)

    print("\n" + "=" * 50)
    print("测试完成")
    print("=" * 50)