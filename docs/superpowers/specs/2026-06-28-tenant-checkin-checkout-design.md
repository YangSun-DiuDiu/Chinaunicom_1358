# 租客档案 + 入住退租 — 设计方案

> **日期**: 2026-06-28 | **状态**: 已审核 | **子项目**: 2a

---

## 1. 数据库

### tenant_info

| 列 | 类型 | 说明 |
|----|------|------|
| tenant_id | BIGINT PK | 主键 |
| tenant_name | VARCHAR(64) | 姓名 |
| id_card | VARCHAR(18) | 身份证号 |
| phone | VARCHAR(20) | 手机号 |
| gender | CHAR(1) | M/F |
| room_id | BIGINT | FK→room_info，入住后绑定 |
| check_in_date | DATE | 入住日期 |
| check_out_date | DATE | 退租日期 |
| rent_start | DATE | 租期开始 |
| rent_end | DATE | 租期结束 |
| status | VARCHAR(20) DEFAULT 'NORMAL' | NORMAL/CHECKED_OUT |
| openid | VARCHAR(64) | 微信公众号openid |
| push_enabled | CHAR(1) DEFAULT '1' | 推送开关 |
| dept_id | BIGINT | 所属组织 |
| +BaseEntity | | 审计字段 |

## 2. 后端

### TenantController (`/hostel/tenant`)

- `GET /list` — 分页列表（tenant_name/phone/room_id/status 筛选）
- `GET /{id}` — 详情
- `POST` — 新增租客（不含入住）
- `PUT` — 修改
- `DELETE /{ids}` — 删除
- `POST /check-in` — **入住办理** body: `{tenantId, roomId, rentStart, rentEnd}` — 更新 room_info.status=BLUE, tenant_count+1
- `POST /check-out/{tenantId}` — **退租结算** — 更新 room_info tenant_count-1，房间退完变GREEN
- `PUT /renew/{tenantId}` — **租期续费** body: `{rentEnd}` — 延长租期

### RoomController 修改

`GET /{id}/detail` — 返回数据中新增 `tenantList` 字段（当前房间租客列表）

### 权限

`hostel:tenant:list/query/add/edit/remove/checkin/checkout/renew`

## 3. 前端

| 页面 | 路由 | 说明 |
|------|------|------|
| 租客档案 | `/hostel/tenant` | 表格 CRUD + 搜索 |
| 入住办理 | `/hostel/tenant/checkin` | 选租客+选空置房间+租期+提交 |
| 卡片弹窗 Tab | card.vue | "租客管理"从空壳改为真实数据 |

## 4. 卡片联动规则

| 操作 | 卡片变化 |
|------|---------|
| 入住 | status GREEN→BLUE, tenant_count+1 |
| 退租(最后一个) | status→GREEN, tenant_count归零 |
| 退租(还有其他人) | tenant_count-1，状态不变 |

## 5. 不做的事

- 费率配置、分摊规则、扣费（子项目 2b）
- 微信公众号 openid 真实对接
- NFC 卡/门锁分配

## 6. 验证标准

- [ ] 租客 CRUD 正常
- [ ] 入住办理 → 房间变蓝 + 卡片同步
- [ ] 退租结算 → 房间变绿 + 卡片同步
- [ ] 租期续费正常
- [ ] 卡片弹窗"租客管理"Tab 显示真实数据
- [ ] 编译通过 + 部署成功
