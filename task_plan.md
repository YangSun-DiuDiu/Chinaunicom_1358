# 任务计划

## 项目：智慧园区管理系统

| 阶段 | 状态 | 说明 |
|------|------|------|
| 1. 架构文档化 | ✅ complete | 探索6模块，输出10篇文档到doc/ |
| 2. 代码精简+多租户 | ✅ complete | 15 Tasks，删除死代码，抽取基类，DeptScopeInterceptor |
| 3. 部署上线 | ✅ complete | 端口80/8090，Nginx+JAR部署到1.94.26.126 |
| 4. Bug修复 | ✅ complete | 字典404、配件SQL、维修反馈、上传预览、短信模板 |
| 5. 会议管理模块 | ✅ complete | 会议室/预约/审批/管理/平板展示 |
| 6. 会议预约改进 | ✅ complete | 预约审批拆分、时间轴、平板地址、数据隔离 |
| 7. 科技风主题 | ✅ complete | 配色#00D4FF、侧边栏毛玻璃、去若依化 |
| 8. 权限全量修复 | ✅ complete | 补全100+条菜单权限，Controller/路由/DB三方一致 |
| 9. 拦截器JOIN修复 | ✅ complete | 自动检测表别名，解决dept_id歧义 |
| 10. 会议室白名单 | ✅ complete | meeting_room加入白名单，全员可见 |
| 11. 样式优化 | pending | 待确认方案 |
| 12. 公寓房源+卡片 | ✅ complete | 2张表、4页前端、4色状态(GREEN/CYAN/BLUE/GRAY)、弹窗Tab |
| 12a. 租客档案+入住退租 | ✅ complete | tenant_info表、CRUD、入住/退租/续费、CYAN逻辑修复 |
| 13. 公寓预付费-后续 | pending | 费率/分摊/扣费(2b)、物联网设备(3)、微信支付(4)、AI能耗(5)、监控看板(6) |
| 14. 短信中台重建 | ✅ complete | 五表+三驱动+SmsUtil+6页前端+11调用点改造 |
| 14a. 短信参数/修复 | ✅ complete | 变量JSON替换content-string、签名空格修复、心跳双SMS去除、repairNo补全 |
| 15. 访客模块重构 | ✅ complete | 5优先：短信去重、VISITING状态、逻辑删除、审批过滤、Controller下沉Service |
| 15a. 访客审批不可见修复 | ✅ complete | dept_id/approver_id自动设置、MyBatis类型别名修复、DeptScope穿透 |
| 16. DeptFillAspect修复 | ✅ complete | 跳过SysDept避免deptId(主键)被覆盖为admin的deptId |
| 17. 驾驶舱重构 | ✅ complete | 4模块统一cockpit、深蓝科技风、2新API+前端重写 |
| 18. 审批通知短信 | ✅ complete | 访客预约/会议预约通知审批人(3个biz_code) |
| 19. H5轻应用 | ✅ complete | 8页面(登录/首页/访客/保安/会议/设备/报修)+QR扫码 |
