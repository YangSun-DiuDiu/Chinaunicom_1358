---
name: zhihuiyuanqu-upload-debug
description: 针对智慧园区项目文件上传后无法预览/访问的问题进行系统排查。当用户报告上传成功但图片不显示、文件无法下载、静态资源404时使用此技能。
---

# 智慧园区文件上传预览调试

## 触发场景

- 照片/文件上传成功，但 `<img>` 无法显示或文件无法下载
- 后端返回了正确的 URL（如 `/profile/xxx.jpg`），但浏览器访问 404/200 却渲染失败
- Nginx + Spring Boot 部署环境下的上传文件访问问题

## 调试流程

### 第 1 步：确认文件是否真的保存到了磁盘

```bash
ssh root@服务器IP "ls -la /home/ruoyi/uploadPath/<子目录>/"
```

**提示**：上传路径由 `application.yml` 的 `ruoyi.profile` 配置项决定，默认为 `/home/ruoyi/uploadPath`。

### 第 2 步：确认后端返回的 URL 格式

检查 Controller 的 upload 方法中 `AjaxResult` 返回的 `url` 字段：

```java
// 正确示例
ajax.put("url", "/profile/repair/" + fileName);
```

**常见陷阱**：`AjaxResult.success()` 的 `data` 字段与自定义 `put("url", ...)` 是平级的。前端取 URL 时需要确认用 `d.url` 还是 `d.data`。

### 第 3 步：确认前端构造的完整 URL

检查前端代码中图片 `<img :src="xxx">` 的 URL 拼接逻辑：

```javascript
// 正确：拼接完整 URL
const fullUrl = url.startsWith('http') ? url : (window.location.origin + url)
```

**常见陷阱**：相对路径直接给 `<img>` 会被浏览器当作相对于当前页面的路径。必须拼上 `origin`。

### 第 4 步：确认 Nginx 配置了该路径的映射

**这是最常见的根因。** 查看 Nginx 配置：

```bash
ssh root@服务器IP "grep -A3 'profile' /etc/nginx/conf.d/*.conf"
```

**如果没有任何匹配结果**，说明 Nginx 缺少静态资源映射。请求被 `location /` 的 `try_files $uri $uri/ /index.html`（SPA 兜底）拦截，返回 HTML 而非图片数据。

**修复**：添加配置块：

```nginx
location /profile/ {
    alias /home/ruoyi/uploadPath/;
    expires 7d;
}
```

```bash
ssh root@服务器IP "nginx -t && nginx -s reload"
```

### 第 5 步：验证修复

```bash
# 用 curl 直接测试图片 URL
curl -o /dev/null -w '%{http_code} %{content_type}' http://服务器IP/profile/<子目录>/xxx.jpg
# 预期输出: 200 image/jpeg (或 image/png)
```

### 为什么用 `alias` 而非 `proxy_pass`

- `alias` 直接从磁盘读取 → 零延迟，不消耗应用资源
- `proxy_pass` 将请求转发给 Spring Boot → 额外网络开销，且需要后端配置静态资源映射

## 此项目的关键约定

| 项目 | 值 |
|------|-----|
| 上传根目录 | `/home/ruoyi/uploadPath` |
| 访问 URL 前缀 | `/profile/` |
| 配置位置 | `application.yml` → `ruoyi.profile` |
| Nginx 配置 | `/etc/nginx/conf.d/zhihuiyuanqu.conf` |

## 修复完成后

将 Nginx 配置同步到 `deploy/nginx.conf` 模板文件，确保下次部署不会丢失此配置。
