---
name: zhihuiyuanqu-permission-check
description: |
  智慧园区后端权限注解语法审核 Skill。触发词：权限审核、检查权限注解、审核@PreAuthorize、权限表达式检查、preauthorize检查。
  扫描所有 Controller 的 @PreAuthorize 表达式，检出会导致 Spring EL 求值失败(500)的语法错误。
---

# 后端权限注解语法审核 Skill

## 背景

RuoYi 的 `@PreAuthorize("@ss.xxx(...)")` 是 Spring EL 表达式，在**请求到达接口时才求值**。若表达式语法错误（方法签名不匹配、方法名拼错），不会在编译期报错，而是运行时抛 `Failed to evaluate expression`，返回 500——只有真正调用该接口才暴露，隐蔽性强。

**典型事故**：`@ss.hasAnyPermi('a','b')` 写成两个参数，但 `hasAnyPermi(String)` 只接受一个逗号分隔字符串 → 现场登记提交 500。

## PermissionService(`@ss` bean) 唯一合法方法集

| 方法 | 参数 | 说明 |
|------|------|------|
| `hasPermi(String)` | 1个权限字符 | 单权限校验 |
| `lacksPermi(String)` | 1个权限字符 | 无此权限 |
| `hasAnyPermi(String)` | **逗号分隔字符串** | 任一权限，如 `'a:b:c,d:e:f'` |
| `hasRole(String)` | 1个角色 | 单角色 |
| `lacksRole(String)` | 1个角色 | 无此角色 |
| `hasAnyRoles(String)` | **逗号分隔字符串** | 任一角色 |

> ⚠️ `hasAnyPermi` / `hasAnyRoles` 是**单参数**（内部 split 逗号），不是多参数。

## 审核流程

### Step 1: 扫描所有 @PreAuthorize

```bash
grep -rn "@PreAuthorize" ruoyi-admin/src/main/java --include="*.java"
```

### Step 2: 逐条检查 5 类问题

| 类型 | 问题 | 错误示例 | 正确写法 |
|------|------|---------|---------|
| **P1 多参数** | hasAnyPermi/hasAnyRoles 写成多参数 | `hasAnyPermi('a','b')` | `hasAnyPermi('a,b')` |
| **P2 方法名错** | 调用不存在的方法 | `hasPermission(...)`, `hasAllPermi(...)` | 用上表6个方法之一 |
| **P3 bean名错** | `@ss` 写错 | `@ps.hasPermi(...)` | `@ss.hasPermi(...)` |
| **P4 权限格式** | 权限字符不规范 | `deviceList`（无冒号） | `device:list` |
| **P5 引号括号** | EL 语法错误 | 引号/括号不匹配 | 配对完整 |

**P1 和 P2 最严重**——直接导致运行时 500。

### Step 3: 快速定位 P1/P2 的正则

```bash
# P1: hasAnyPermi/hasAnyRoles 带多个引号参数（逗号在引号外）
grep -rnE "hasAny(Permi|Roles)\('[^']*',\s*'" ruoyi-admin/src/main/java --include="*.java"

# P2: 检查所有 @ss 后的方法名，人工比对是否在合法方法集
grep -roE "@ss\.[a-zA-Z]+" ruoyi-admin/src/main/java --include="*.java" | sort -u
```

### Step 4: 修复

对每个 P1：将 `hasAnyPermi('a','b')` 改为 `hasAnyPermi('a,b')`。
对每个 P2：改为合法方法名。
逐一 Edit 修复。

### Step 5: 编译验证 + 部署

```bash
cd <项目根> && mvn clean package -DskipTests -q
```
编译通过后，用 zhihuiyuanqu-deploy Skill 部署后端（仅后端模式）。

## 审核报告格式

```
@PreAuthorize 总数：XX
P1 多参数错误：X 处 [文件:行号]
P2 方法名错误：X 处 [文件:行号]
P3-P5：X 处
修复：逐条列出 文件:行号 + 旧写法 → 新写法
```

## 注意事项

1. **单参数 hasPermi 不受影响**——只有 `hasAnyPermi`/`hasAnyRoles` 有多参数陷阱。
2. **权限字符与 DB 一致性不在本 Skill 范围**——那是三方对照审计（见 docs/permission-audit）。本 Skill 只查**表达式语法**。
3. 修复后必须重新编译部署后端，因为 @PreAuthorize 是运行时求值。
