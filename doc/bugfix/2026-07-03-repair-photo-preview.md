# Bug修复：维修确认页照片上传后无法预览

> 日期：2026-07-03 | 严重度：P1 | 状态：已修复

## 问题现象

设备维修确认页面（`/repair-complete?token=xxx`）上传维修照片后，照片区显示空白，`<img>` 标签无法正常渲染图片。

## 根因分析

### 数据流追踪

```
前端上传                              后端保存
──────────                            ──────────
FormData → POST /device/repair/      保存到 /home/ruoyi/uploadPath/
  public/upload                       repair/{UUID}.jpg
                                     返回 AjaxResult { url: "/profile/repair/{UUID}.jpg" }

前端预览                               Nginx 处理
──────────                            ──────────
<img src="http://1.94.26.126/        请求 → 匹配 location /
  profile/repair/{UUID}.jpg">        → try_files $uri $uri/ /index.html
                                     → 文件不存在于 dist/ 目录
                                     → 回退返回 /index.html (HTML)
                                     → <img> 尝试渲染 HTML → 失败
```

### 根因

**Nginx 没有配置 `/profile/` 静态资源映射。** 

后端将文件保存到 `/home/ruoyi/uploadPath/repair/`，返回的 URL 路径为 `/profile/repair/{UUID}.jpg`。但 Nginx 的 `location /` 用 `try_files` 将不存在的文件兜底到 `/index.html`（SPA 模式），浏览器收到 HTML 而非图片数据，`<img>` 标签无法渲染。

## 修复方案

在 Nginx 配置中添加 `/profile/` 静态资源映射，直接提供文件服务：

```nginx
location /profile/ {
    alias /home/ruoyi/uploadPath/;
    expires 7d;
}
```

### 为什么用 `alias` 而非 `proxy_pass`

- `alias` 直接从磁盘读取文件，性能更高
- 不经过 Spring Boot，避免额外的网络开销
- 上传文件不需要后端逻辑，纯静态服务即可

## 验证方法

```bash
# 1. 上传一张测试图片
curl -X POST http://1.94.26.126/prod-api/device/repair/public/upload \
  -F "file=@test.jpg"

# 2. 验证图片可访问
curl -o /dev/null -w '%{http_code} %{content_type}' \
  http://1.94.26.126/profile/repair/{UUID}.jpg
# 预期: 200 image/jpeg
```

## 涉及文件

| 文件 | 操作 |
|------|------|
| `/etc/nginx/conf.d/zhihuiyuanqu.conf` | 添加 location /profile/ 块 |
| `deploy/nginx.conf` | 更新模板配置（proxy_pass → alias） |

## 相关知识

### 此项目的文件上传路径约定

- 配置项：`ruoyi.profile = /home/ruoyi/uploadPath`（application.yml）
- 后端返回格式：`/profile/{模块}/{UUID}.{ext}`
- Nginx 映射：`/profile/` → `/home/ruoyi/uploadPath/`

### 类似问题的排查方法

当遇到"上传成功但无法预览/下载"的问题时：

1. 确认文件是否真的保存到了磁盘 → `ls /home/ruoyi/uploadPath/`
2. 确认后端返回的 URL 路径 → 检查 AjaxResult 的 `url` 字段
3. 确认 Nginx 是否配置了该路径的映射 → `grep -r "profile" /etc/nginx/`
4. 确认 `location /` 是否用 `try_files` 兜底了请求 → 这是常见陷阱
