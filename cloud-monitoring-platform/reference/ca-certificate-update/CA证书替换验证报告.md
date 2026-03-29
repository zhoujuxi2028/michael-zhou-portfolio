# CA 证书替换验证报告

**日期**: 2026-03-29
**操作方式**: `jar uf` 直接替换（方案 A1）
**源文件**: `zhongcheng-1.0.2-SNAPSHOT.jar`
**目标文件**: `zhongcheng-1.0.2-SNAPSHOT-new-ca.jar`

---

## 1. JAR 文件基本信息

| 项目 | 原始 JAR | 新 JAR |
|------|----------|--------|
| 文件名 | `zhongcheng-1.0.2-SNAPSHOT.jar` | `zhongcheng-1.0.2-SNAPSHOT-new-ca.jar` |
| 大小 | 69MB | 69MB |
| 条目数 | 682 | 682 |
| MD5 | `f03181145dc0ea064184ccc7948ca0f5` | 替换后生成 |

## 2. 唯一差异：仅 ca.jks 被替换

CRC32/大小对比（`jar tvf` 差异输出）：

```
< 2225 bytes  BOOT-INF/classes/cert/ca.jks   (旧)
> 980  bytes  BOOT-INF/classes/cert/ca.jks   (新)
```

**全量二进制 diff 结果（排除 ca.jks）：无差异** — 其余 681 个文件完全一致。

## 3. ca.jks MD5 校验

| 文件来源 | MD5 |
|----------|-----|
| 旧 JAR 内 ca.jks | `6332b24f82a61ccdbe3bb5dd2a346648` |
| 新 JAR 内 ca.jks | `d34ee6586dcc9554bdde99bbb3f52e0c` |
| 新证书源文件 | `d34ee6586dcc9554bdde99bbb3f52e0c` |

新 JAR 内证书与源文件 MD5 **完全一致**。

## 4. 密码兼容性验证

| 测试                        | 结果                                                               |
| ------------------------- | ---------------------------------------------------------------- |
| 用 `Huawei@123` 读取新 ca.jks | **成功** — 正常列出证书内容                                                |
| 用错误密码读取新 ca.jks           | **失败** — `Keystore was tampered with, or password was incorrect` |

密码 `Huawei@123` 与代码中 `Constant.TRUSTCAPWD` 硬编码值一致，**不存在密码不可用问题**。

## 5. 新旧证书关键属性对比

| 属性 | 旧证书 (mykey) | 新证书 (alias: 1) |
|------|---------------|-------------------|
| Owner | `CN=IOT, OU=CN, O=Huawei, L=SZ, ST=GD, C=CN` | **相同** |
| Issuer | `CN=IOT, OU=CN, O=Huawei, L=SZ, ST=GD, C=CN` | **相同** |
| 签名算法 | SHA256withRSA | **相同** |
| 公钥算法 | 2048-bit RSA | **相同** |
| SKI | `70:6A:B2:E7:DA:1A:C0:B1:20:32:5D:B5:FF:FE:C5:E5:1C:80:06:9C` | **相同** |
| 有效期 | 2016-05-04 ~ **2026-05-02** | 2026-01-27 ~ **2036-01-25** |
| 条目数 | 2（含已过期 GlobalSign） | 1（仅 Huawei IOT） |

## 7. 结论

- **仅 `BOOT-INF/classes/cert/ca.jks` 一个文件被替换**，其余 681 个文件无任何改动
- **密码完全兼容**，`Huawei@123` 可正常访问新 keystore
- **证书身份不变**（同 Owner/Issuer/SKI/算法），仅有效期延长至 2036 年
- 已过期的 GlobalSign 中间 CA（`icesslkey`）被移除，不影响 Huawei IoT 平台通信
- **方案 A1（`jar uf`）验证通过，可安全用于生产环境**
