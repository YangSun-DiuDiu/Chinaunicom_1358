# 会议预约改进 — 设计方案

> **日期**: 2026-06-26
> **状态**: 已审核

---

## 1. 预约 + 审批拆分为两页

### 1.1 页面拆分

| 页面 | 路由 | 权限 | 功能 |
|------|------|------|------|
| 会议预约 | `/meeting/booking` | `meeting:booking:list` | 新建预约 + 查看自己的预约 + 取消 |
| 会议审批 | `/meeting/approval` | `meeting:approval:list` | 审批列表 + 通过/拒绝 |

### 1.2 菜单

删除 `sys_menu` 中 menu_id=2033（旧会议预约），新增：
- menu_id=2035: "会议预约" (`meeting/booking/index`, `meeting:booking:list`)
- menu_id=2036: "会议审批" (`meeting/approval/index`, `meeting:approval:list`)

admin 角色授权两个新菜单。

### 1.3 前端

- 预约页：`getList` 自动过滤 `create_by` 为当前用户名（或通过参数 `my=1` 后端过滤）
- 审批页：新文件 `ruoyi-ui/src/views/meeting/approval/index.vue`，列表默认 status=PENDING，操作列：通过/拒绝按钮

---

## 2. 预约时间全景图

### 2.1 后端新接口

`GET /meeting/booking/slots/{roomId}?date=2026-06-26`
→ 返回 `[{startTime, endTime, title, status}]`（仅 APPROVED 和 PENDING）

### 2.2 前端

新建预约弹窗中：
- 选择会议室后自动加载 slots
- 渲染时间轴（08:00-18:00），已占用区间用色块标注
- 用户拖动或点击选择空闲区间
- startTime/endTime 从选择的区间自动填充

---

## 3. 会议管理数据修复

- 默认查询今日全部预约（不限状态）
- 增加日期选择器切换日期
- 增加会议室下拉筛选

---

## 4. 会议室管理 → 平板地址列

在 `room/index.vue` 表格新增列：

```html
<el-table-column label="平板地址" min-width="260">
  <template slot-scope="{row}">
    <el-input :value="boardUrl(row)" size="mini" readonly style="width:200px" />
    <el-button size="mini" icon="el-icon-document-copy" @click="copyUrl(row)" style="margin-left:4px">复制</el-button>
  </template>
</el-table-column>
```

```javascript
boardUrl(row) { return `http://1.94.26.126:80/meeting/board?roomId=${row.roomId}` },
copyUrl(row) {
  navigator.clipboard.writeText(this.boardUrl(row)).then(() => this.$message.success('已复制'))
}
```

---

## 5. 不做的事

- 不修改平板展示页
- 不修改冲突检测后端逻辑
- 不新增数据库表

---

## 6. 验证标准

- [ ] 预约页仅显示当前用户的预约
- [ ] 审批页独立显示 PENDING 预约，可审批
- [ ] 时间轴正确展示已占用时段
- [ ] 会议管理有数据
- [ ] 会议室表格显示平板地址 + 可复制
