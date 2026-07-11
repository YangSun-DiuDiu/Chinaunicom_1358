# 权限不一致修复 — 设计方案

> **日期**: 2026-06-26 | **状态**: 已审核

## 问题分析

后端 Controller `@PreAuthorize` 权限与数据库 `sys_menu.perms` 权限不一致，导致非 admin 用户即使被授予菜单权限也无法访问对应 API（403）。

## 不一致清单

| 文件 | Controller 权限 | 数据库菜单权限 | 修复 |
|------|----------------|---------------|------|
| VisitorAppointmentController.java:62 | `visitor:appointment:pending` | `visitor:approval:list` | 改为 matching DB |
| 前端路由 router/index.js:202 | `visitor:approval:list` (已修复) | `visitor:approval:list` | ✅ 一致 |

## 修复

后端 Controller 权限改为与数据库菜单一致：
`visitor:appointment:pending` → `visitor:approval:list`

## 验证
- 前端调用 `/visitor/appointment/pending` 不再返回 403
- zhaojg (有 `visitor:approval:list` 权限) 可正常访问审批页
