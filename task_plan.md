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
| 11. 样式优化 | ⏳ partial | 设备管理8页面已打磨(list/fault/heartbeat/repair/parts/snmp/topology/workload)，统计卡片+Tab分区+格式规范 |
| 12. 公寓房源+卡片 | ✅ complete | 2张表、4页前端、4色状态(GREEN/CYAN/BLUE/GRAY)、弹窗Tab |
| 12a. 租客档案+入住退租 | ✅ complete | tenant_info表、CRUD、入住/退租/续费、CYAN逻辑修复 |
| 13. 公寓预付费-后续 | pending | 费率/分摊/扣费(2b)、物联网设备(3)、微信支付(4)、AI能耗(5)、监控看板(6) |
| 22. 部署Skill通用化 | ✅ complete | deploy.json参数化、4模式(全量/前端/后端/重启)、多服务器、git变更检测 |
| 23. Redis修复 | ✅ complete | 1.94.26.126 corrupt dump.rdb清理、Redis启动、配置切回本地 |
| 24. 会议门牌Android | ✅ complete | board.html独立页+Android WebView壳(Kotlin)+BootReceiver+simpleList API |
| 14. 短信中台重建 | ✅ complete | 五表+三驱动+SmsUtil+6页前端+11调用点改造 |
| 14a. 短信参数/修复 | ✅ complete | 变量JSON替换content-string、签名空格修复、心跳双SMS去除、repairNo补全 |
| 15. 访客模块重构 | ✅ complete | 5优先：短信去重、VISITING状态、逻辑删除、审批过滤、Controller下沉Service |
| 15a. 访客审批不可见修复 | ✅ complete | dept_id/approver_id自动设置、MyBatis类型别名修复、DeptScope穿透 |
| 15b. 访客审批逻辑修正 | ✅ complete | dept_id改回被访人部门;assignApproverAndDept强制匹配hostName→sys_user;visitor_log补dept_id(approveAppointment+registerWalkin) |
| 16. DeptFillAspect修复 | ✅ complete | 跳过SysDept避免deptId(主键)被覆盖为admin的deptId |
| 17. 驾驶舱重构 | ✅ complete | 4模块统一cockpit、深蓝科技风、2新API+前端重写 |
| 18. 审批通知短信 | ✅ complete | 访客预约/会议预约通知审批人(3个biz_code) |
| 19. H5轻应用 | ✅ complete | 8页面(登录/首页/访客/保安/会议/设备/报修)+QR扫码 |
| 20. 权限审计修复 | ✅ complete | 补3缺失权限+9条H5菜单+H5Controller注解 |
| 20a. 菜单结构修正 | ✅ complete | 公寓/智能化扁平化、考勤按钮归位、DB对齐前端路由 |
| 21. getRouter精简 | ✅ complete | 删Controller/Service/Mapper/XML/RouterVo僵尸代码(-397行) |
| 25. 驾驶舱v2工业风 | ⛔ 作废 | 驾驶舱已被阶段26门户取代，设备饼图API 499随驾驶舱移除失效 |
| 26. 模块导航门户 | ✅ complete | 移除驾驶舱→10卡CSS几何门户+侧边栏parentMenuId过滤+首页全屏化+返回首页入口+LED信号灯；9任务全通+死代码清理+DB补menu_type；权限审计107/130一致 |
| 27. 监控体系改造 | ✅ complete | A:AlertManager部署(115:9093)+Webhook+删Java心跳+设备列表Grafana跳转; B:net_report表+健康评分算法+DeepSeek报告(6周期)+前端页; DeepSeek解析修复(跳过thinking块)+sendRepairSms参数修复(离线不再双发device_repair); AlertManager→Java告警链路端到端验证通过(labels.target) |
| 28. 设备拓扑整体升级 | ✅ complete | 本地SVG图标集(8类:router/switch/firewall/ap/server/camera/nvr/other)+线缆库(4类:fiber/ethernet/wireless/coaxial); iot_device加icon/cable_type字段; buildTopologyTree字段对齐(id/label→deviceId/deviceName等)修复拓扑页空白; 拓扑页SVG图标渲染+按cableType线缆着色; 设备表单加图标/父设备/线缆3下拉; 补财务部(11)/工程部(13)device:topology:query权限; 部署1.94.26.126验证200 |
| 28a. 拓扑页渲染层修复 | ✅ complete | 真正根因:TopologyNode内联字符串template在runtime-only Vue(无模板编译器,module/main=vue.runtime.*,无vue$ alias)下不编译→渲染为`<!--function...-->`空注释→节点区空白;生产NODE_ENV静默编译警告(console 0错误误导)。修复:TopologyNode拆独立SFC(TopologyNode.vue, vue-loader预编译render)+index.vue改import;生产部署验证节点/图标/线缆均渲染 |
| 28b. 拓扑过滤+架构图连线 | ✅ complete | DeviceController.topology只显示参与关联设备(parent_id非空或被引用为父),隐藏孤立设备:生产12台中仅华为防火墙USG6565→测试设备-权限验证保留,10台隐藏;TopologyNode.vue连线改架构图直角折线(层级40px/水平间距56px/线缆着色);admin验证通过 |
| 28c. 拓扑视觉升级·工程蓝图 | ✅ complete | 画布点阵背景(radial-gradient 20px)+#EEF1F6底色;工具栏透明化;卡片12px圆角+双层阴影+hover#3B82F6光晕;状态色统一palette(Online#10B981/Offline#EF4444);连线#98A6B6;线缆色Fiber#D97706/Ethernet#3B82F6;零JS/零第三方。生产全属性实测一致,console 0错误 |
