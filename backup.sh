#!/bin/bash
# ============================================================================
# 智慧园区管理系统 — 代码备份脚本
# 用法: bash backup.sh [--db] [--keep N]
#   --db      同时备份 MySQL 数据库
#   --keep N  保留最近 N 个备份文件夹，删除旧备份（默认 7）
#
# 备份结构:
#   backup/
#   └── RuoYi-Vue-backup-20260711_143022/   ← 每次备份一个独立文件夹
#       ├── RuoYi-Vue-backup-20260711_143022.tar.gz   ← 源代码
#       └── ry101-backup-20260711_143022.sql.gz       ← 数据库 (--db)
#
#   git add + commit + push 自动完成
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_NAME="RuoYi-Vue"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_ROOT="${SCRIPT_DIR}/backup"
BACKUP_DIR="${BACKUP_ROOT}/${PROJECT_NAME}-backup-${TIMESTAMP}"
KEEP_COUNT=7
DO_DB=false

# ---- 解析参数 ----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --db)   DO_DB=true; shift ;;
    --keep) KEEP_COUNT="$2"; shift 2 ;;
    *)      shift ;;
  esac
done

# ---- 创建备份文件夹 ----
mkdir -p "${BACKUP_DIR}"

echo ""
echo "========================================"
echo "  智慧园区管理系统 — 代码备份"
echo "  时间: ${TIMESTAMP}"
echo "  目录: ${BACKUP_DIR}"
echo "========================================"

# ====================================================================
# Step 1: 源代码打包
# ====================================================================
echo ""
echo "[1/3] 打包源代码..."

CODE_FILE="${BACKUP_DIR}/${PROJECT_NAME}-backup-${TIMESTAMP}.tar.gz"

tar -czf "${CODE_FILE}" \
  --exclude='.git' \
  --exclude='node_modules' \
  --exclude='dist' \
  --exclude='target' \
  --exclude='build' \
  --exclude='.gradle' \
  --exclude='*.tar.gz' \
  --exclude='*.sql.gz' \
  --exclude='*.sql' \
  --exclude='.omc' \
  --exclude='backup' \
  --exclude='.idea' \
  --exclude='*.iml' \
  --exclude='*.jar' \
  --exclude='.DS_Store' \
  --exclude='.playwright-mcp' \
  --exclude='.playwright-cli' \
  --exclude='.superpowers' \
  --exclude='__pycache__' \
  -C "$(dirname "${SCRIPT_DIR}")" \
  "$(basename "${SCRIPT_DIR}")"

CODE_SIZE=$(du -h "${CODE_FILE}" | cut -f1)
echo "  ✓ ${CODE_FILE}"
echo "    大小: ${CODE_SIZE}"

# ====================================================================
# Step 2: 数据库备份（可选）
# ====================================================================
if $DO_DB; then
  echo ""
  echo "[2/3] 备份数据库..."

  # 从 application-druid.yml 读取配置
  DRUID_YML="${SCRIPT_DIR}/ruoyi-admin/src/main/resources/application-druid.yml"
  DB_HOST="192.168.1.55"
  DB_PORT="3306"
  DB_NAME="ry101"
  DB_USER="root"

  if [[ -f "${DRUID_YML}" ]]; then
    DB_USER=$(grep -A1 'username:' "${DRUID_YML}" | tail -1 | sed 's/.*username: *//' | tr -d '\r' || echo "root")
    # 尝试从 url 中解析库名
    DB_URL=$(grep 'url:' "${DRUID_YML}" | head -1 | sed 's/.*url: *//' | tr -d '\r')
    if [[ "$DB_URL" =~ jdbc:mysql://([^:/]+):?([0-9]*)/([^?]+) ]]; then
      DB_HOST="${BASH_REMATCH[1]}"
      [[ -n "${BASH_REMATCH[2]}" ]] && DB_PORT="${BASH_REMATCH[2]}"
      DB_NAME="${BASH_REMATCH[3]}"
    fi
  fi
  [[ -z "$DB_USER" ]] && DB_USER="root"

  # 交互式输入密码
  echo -n "  请输入 MySQL 密码 (${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}): "
  read -s DB_PASS
  echo ""

  DB_FILE="${BACKUP_DIR}/${DB_NAME}-backup-${TIMESTAMP}.sql.gz"

  if command -v mysqldump &>/dev/null; then
    mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
      --single-transaction --routines --triggers --events \
      --add-drop-table --default-character-set=utf8mb4 \
      "${DB_NAME}" 2>/dev/null | gzip > "${DB_FILE}"
  else
    # 无本地 mysqldump，SSH 到服务器 dump
    echo "  本地无 mysqldump，通过 SSH 远程 dump..."
    DEPLOY_JSON="${SCRIPT_DIR}/deploy.json"
    SVR_IP=$(grep -o '"ip": *"[^"]*"' "${DEPLOY_JSON}" | head -1 | sed 's/.*"\(.*\)"/\1/')
    SVR_USER=$(grep -o '"user": *"[^"]*"' "${DEPLOY_JSON}" | head -1 | sed 's/.*"\(.*\)"/\1/')
    if [[ -n "$SVR_IP" ]]; then
      # MySQL 已独立部署到 192.168.1.55（非应用服务器本地），远程 dump 需指向数据库服务器
      ssh "${SVR_USER}@${SVR_IP}" "mysqldump -h${DB_HOST} -u${DB_USER} -p'${DB_PASS}' \
        --single-transaction --routines --triggers --events \
        --add-drop-table --default-character-set=utf8mb4 \
        '${DB_NAME}' | gzip" > "${DB_FILE}" 2>/dev/null
    else
      echo "  ✗ 无法连接服务器，数据库备份失败"
      DO_DB=false
    fi
  fi

  if $DO_DB; then
    DB_SIZE=$(du -h "${DB_FILE}" | cut -f1)
    echo "  ✓ ${DB_FILE}"
    echo "    大小: ${DB_SIZE}"
  fi
else
  echo ""
  echo "[2/3] 跳过数据库 (加 --db 启用)"
fi

# ====================================================================
# Step 3: Git 提交 & 推送
# ====================================================================
echo ""
echo "[3/3] Git 提交到远程仓库..."

cd "${SCRIPT_DIR}"

# 检查是否有远程仓库
REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")
if [[ -z "$REMOTE_URL" ]]; then
  echo "  ⚠ 未配置 git remote，跳过 git 操作"
else
  # 检查备份文件大小：超过 95MB 跳过 git push（GitHub 限制 100MB）
  FILE_SIZE_MB=$(du -m "${CODE_FILE}" | cut -f1)
  if [[ "$FILE_SIZE_MB" -gt 95 ]]; then
    echo "  ⚠ 备份文件 ${FILE_SIZE_MB}MB，超过 GitHub 100MB 限制，跳过 git push"
    echo ""
    echo "========================================"
    echo "  备份完成! (仅本地)"
    echo "  文件夹: $(basename "${BACKUP_DIR}")"
    echo "  代码:   $(basename "${CODE_FILE}")  (${CODE_SIZE})"
    echo "  位置:   ${BACKUP_DIR}"
    echo "========================================"
    exit 0
  fi

  git add backup/ 2>/dev/null || true

  if git diff --cached --quiet 2>/dev/null; then
    echo "  ℹ 无变更，跳过 commit"
  else
    COMMIT_MSG="backup: ${TIMESTAMP}"
    $DO_DB && COMMIT_MSG="${COMMIT_MSG} (code+db)" || COMMIT_MSG="${COMMIT_MSG} (code)"
    git commit -m "${COMMIT_MSG}" 2>/dev/null
    echo "  ✓ git commit: ${COMMIT_MSG}"

    # 推送到远程
    if git push 2>/dev/null; then
      echo "  ✓ git push → ${REMOTE_URL}"
    else
      echo "  ⚠ git push 失败，请手动推送"
    fi
  fi
fi

# ====================================================================
# Step 4: 清理旧备份文件夹
# ====================================================================
echo ""
echo "---"
# 按文件夹名排序，保留最近 KEEP_COUNT 个
OLD_DIRS=$(find "${BACKUP_ROOT}" -maxdepth 1 -type d -name "${PROJECT_NAME}-backup-*" | sort | head -n -${KEEP_COUNT})
if [[ -n "$OLD_DIRS" ]]; then
  echo "$OLD_DIRS" | while read -r d; do
    rm -rf "$d"
    echo "  🗑 已删除旧备份: $(basename "$d")"
  done
  # 清理后也 git 一下
  git add backup/ 2>/dev/null || true
  if ! git diff --cached --quiet 2>/dev/null; then
    git commit -m "backup: cleanup old backups" 2>/dev/null || true
    git push 2>/dev/null || true
  fi
else
  echo "  保留最近 ${KEEP_COUNT} 个备份，无需清理"
fi

# ====================================================================
# 完成
# ====================================================================
echo ""
echo "========================================"
echo "  备份完成!"
echo ""
echo "  文件夹: $(basename "${BACKUP_DIR}")"
echo "  代码:   $(basename "${CODE_FILE}")  (${CODE_SIZE})"
$DO_DB && echo "  数据库: ${DB_NAME}-backup-${TIMESTAMP}.sql.gz  (${DB_SIZE})" || true
echo "  位置:   ${BACKUP_DIR}"
echo "  远程:   ${REMOTE_URL:-无}"
echo "========================================"
echo ""
