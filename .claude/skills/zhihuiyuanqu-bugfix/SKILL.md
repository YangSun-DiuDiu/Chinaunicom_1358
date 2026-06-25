---
name: zhihuiyuanqu-bugfix
description: |
  智慧园区管理系统常见 Bug 诊断与修复。当页面报错、功能异常、SQL报错、上传/预览问题、短信模板问题、权限问题、404端点时使用。
  触发词：报错、bug、异常、修复、不工作、空白页、404、500、SQL错误、上传失败、预览不了、短信问题、权限问题。
---

# 智慧园区管理系统 — Bug 诊断与修复

## 快速诊断流程

收到报错时按以下顺序排查：

### 1. 后端 SQL 错误

症状：`bad SQL grammar`、`Unknown column`、`Table doesn't exist`

排查：
```bash
# 查看后端日志
ssh root@1.94.26.126 'tail -100 /root/zhihuiyuanqu20260625/app.log | grep -i "error\|exception\|Preparing"'

# 检查表结构
mysql -h 1.94.26.126 -P 3306 -u sadmin -pChinaunicom@1358 ry101 -e "DESC <table_name>;"
```

常见根因：
- 缺少列 → `ALTER TABLE ADD COLUMN`
- XML 别名不匹配 → 检查 FROM 子句的别名
- 列名拼写错误 → 对照实体类和 XML resultMap

### 2. 404 端点错误

症状：`No static resource xxx for request '/xxx'`

排查：
```bash
# 搜索是否有对应 Controller
grep -r "RequestMapping\|GetMapping" ruoyi-admin/src/main/java/ --include="*.java"
```

修复：新建 Controller 或添加缺失的 `@GetMapping` 方法。

### 3. 文件上传/预览问题

症状：上传成功但无法预览、`FileNotFoundException`

排查：
```bash
# 1. 检查 application.yml 的 profile 路径（Linux 用 /home/xxx，非 Windows D:/）
grep "profile:" ruoyi-admin/src/main/resources/application.yml

# 2. 检查 Nginx 是否有 /profile/ 代理
ssh root@1.94.26.126 'grep -A3 "location /profile" /etc/nginx/conf.d/zhihuiyuanqu.conf'

# 3. 检查文件是否存在
ssh root@1.94.26.126 'ls -la /home/ruoyi/uploadPath/repair/'

# 4. 测试文件访问
curl -I http://1.94.26.126:8090/profile/repair/test.jpg
```

修复：
- 路径不匹配 → 统一 `RuoYiConfig.getProfile()` 
- Nginx 无代理 → 添加 `location /profile/` 规则
- 前端 baseUrl 错误 → 设为 `window.location.origin`

### 4. 短信模板问题

症状：短信内容不对、发2条、不发短信

排查：
```bash
# 检查所有 smsService 调用点
grep -rn "smsService\." ruoyi-*/src/main/java/ --include="*.java"

# 检查数据库配置
mysql ... -e "SELECT config_key, config_value FROM sys_config WHERE config_key LIKE '%sms%';"
```

关键文件：
- `SmsServiceImpl.java` — 短信模板定义
- `DeviceRepairController.java` — 手动报修短信
- `DeviceHeartbeatTask.java` — 离线自动报修
- `DeviceController.java` — 旧报修接口（可能遗留）

### 5. 权限问题

症状：`403 没有权限`

排查：
```bash
# 检查菜单权限 vs Controller 权限是否一致
grep -rn "PreAuthorize.*hasPermi" ruoyi-admin/src/main/java/ --include="*.java" | grep <模块>
mysql ... -e "SELECT menu_id, perms FROM sys_menu WHERE perms LIKE '%<模块>%';"
```

常见不匹配：菜单存 `device:list:list` 但 Controller 检查 `device:list`

### 6. 部署后不生效

症状：改了代码但线上没变化

排查：
```bash
# 1. 确认 JAR 已上传并重启
ssh root@1.94.26.126 'ls -la /root/zhihuiyuanqu20260625/ruoyi-admin.jar'

# 2. 确认前端已更新
ssh root@1.94.26.126 'ls -la /usr/share/nginx/zhihuiyuanqu/dist/index.html'

# 3. 确认 Nginx 已重载
ssh root@1.94.26.126 'nginx -s reload'
```

## 项目关键信息卡

| 项目 | 值 |
|------|-----|
| 后端端口 | 8090 |
| 前端端口 | 80 (Nginx) |
| 部署目录 | /root/zhihuiyuanqu20260625/ |
| 前端目录 | /usr/share/nginx/zhihuiyuanqu/dist/ |
| Nginx配置 | /etc/nginx/conf.d/zhihuiyuanqu.conf |
| 上传目录 | /home/ruoyi/uploadPath/ |
| 数据库 | 1.94.26.126:3306/ry101 |
| Redis | 1.94.26.126:6379 |
| GitHub | https://github.com/YangSun-DiuDiu/Chinaunicom_1358 |
