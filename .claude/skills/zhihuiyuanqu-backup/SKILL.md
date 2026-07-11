---
name: zhihuiyuanqu-backup
description: |
  智慧园区管理系统代码备份 Skill。触发词：备份代码、代码备份、备份数据库、全量备份。
  每次备份创建独立文件夹（项目名+时间戳），内含代码 tar.gz + 可选数据库 sql.gz，最后 git commit & push。
---

# 代码备份 Skill

## 备份模式

| 用户说 | 执行 |
|--------|------|
| "备份代码"、"备份" | `bash backup.sh`（仅代码） |
| "备份代码和数据库" | `bash backup.sh --db`（代码 + MySQL） |
| "完整备份"、"全量备份" | `bash backup.sh --db`（代码 + 数据库） |

## 备份产物

每次备份创建一个**独立文件夹**，结构如下：

```
backup/
├── RuoYi-Vue-backup-20260711_143022/        ← 本次备份文件夹
│   ├── RuoYi-Vue-backup-20260711_143022.tar.gz   ← 源代码
│   └── ry101-backup-20260711_143022.sql.gz       ← 数据库 (--db)
├── RuoYi-Vue-backup-20260710_091530/        ← 上次备份
│   └── ...
└── ...                                        ← 最多保留 7 个
```

## 执行流程

### 1. 确认模式

如果用户未明确是否含数据库，问一句即可。

### 2. 执行脚本

```bash
cd <项目根目录> && bash backup.sh [--db] [--keep 7]
```

**脚本自动完成**：
1. 创建 `backup/RuoYi-Vue-backup-{时间戳}/` 文件夹
2. 打包源代码 tar.gz（排除 .git/node_modules/target/dist/.omc/backup/.idea）
3. 如 `--db`：交互式输入 MySQL 密码 → 远程 dump → gzip 压缩
4. `git add backup/` → `git commit -m "backup: {时间戳}"` → `git push`

### 3. 汇报结果

输出：文件夹名、文件列表 + 大小、git commit hash（如有）。

## 数据库连接

| 配置项 | 来源 |
|--------|------|
| 主机/端口/库名 | `application-druid.yml` → `url` 自动解析 |
| 用户名 | `application-druid.yml` → `username` |
| 密码 | `read -s` 交互式输入（不存储不回显） |

## 恢复方法

```bash
# 代码恢复
tar -xzf backup/RuoYi-Vue-backup-{时间戳}/RuoYi-Vue-backup-{时间戳}.tar.gz -C /目标路径/

# 数据库恢复
zcat backup/RuoYi-Vue-backup-{时间戳}/ry101-backup-{时间戳}.sql.gz | mysql -h主机 -u用户 -p 数据库名
```
