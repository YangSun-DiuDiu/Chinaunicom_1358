# 会话日志

| 日期 | 内容 | 提交 |
|------|------|------|
| 06-25 | 架构文档化 | 10篇 doc/ |
| 06-25 | 代码精简+多租户 | 15 Tasks, ~18 commits |
| 06-25 | 部署上线 | 端口80/8090 |
| 06-25~26 | Bug修复 | 字典/配件/维修/上传/短信 |
| 06-26 | 会议管理 | Domain+Mapper+Controller+前端6页 |
| 06-26 | 会议改进 | 审批拆分/时间轴/平板/权限补全 |
| 06-26 | 科技风主题 | Element变量/Layout/登录页/首页 |
| 06-26 | 权限全量修复 | 100+条菜单补全, 32/32测试通过 |
| 06-26 | 拦截器JOIN修复 | 自动检测别名 |
| 06-26 | 会议室白名单 | meeting_room全员可见 |
| 06-28 | 公寓房源+卡片 | 2表4页, 4色状态 |
| 06-28 | 租客档案+入住退租 | tenant_info, check-in/out/renew |
| 06-29 | 短信中台全量重建 | 五表+SmsUtil+三驱动+6前端+11调用点 |
| 06-29 | 短信参数/签名/去重/repairNo | 变量JSON, 空格修复, 心跳去双SMS |
| 06-29 | 访客模块5优先重构 | 短信去重+VISITING+逻辑删除+审批过滤+层次下沉 |
| 06-29~30 | 访客审批不可见根因修复 | 5层根因逐一破解，5次编译部署，最终全通 |
| 07-01 | DeptFillAspect修复 | 跳过SysDept避免主键冲突 Duplicate entry 100 |
| 07-01 | 驾驶舱重构(17) | 4模块+深蓝底+ECharts修复 |
| 07-01 | 审批短信(18) | 3个biz_code:visitor/meeting通知 |
| 07-01 | H5轻应用(19) | 登录+首页7卡+访客/保安/会议/设备 |
| 07-01 | 权限审计修复(20) | DB补3缺失+9H5菜单+H5Controller@PreAuthorize |
| 07-02 | 菜单结构修正(20a) | 公寓/智能化扁平化+考勤按钮归位+权限对照表doc |
| 07-03 | 备份+需求暂缓 | 代码备份+DB备份+getRouter精简方案记录 |
| 07-10 | 部署Skill通用化(22) | deploy.json参数化、4部署模式、多服务器、git变更检测 |
| 07-10 | 设备管理页面打磨(11) | 8页面重排: list/fault/heartbeat/repair/parts/snmp/topology/workload，统计卡片+Tab分区 |
| 07-10 | Redis修复(23) | 1.94.26.126 corrupt dump.rdb清理并启动，配置切回本地Redis |
| 07-11 | 驾驶舱v2工业风(25) | HMI配色+8KPI单行88px+Grid比例布局+串行API；设备饼图API 499排查中 |
| 07-11 | 模块导航门户(26) | 移除驾驶舱→10卡CSS几何门户+侧边栏parentMenuId过滤+首页全屏化+返回首页入口+LED信号灯；8任务完成，权限审计进行中 |
| 07-11 | 门户收尾+审计+清理(26✅) | 权限审计107/130三方一致(post/register为死代码已清)+DB补11条空menu_type为F；阶段25驾驶舱作废(被门户取代)；9任务全通部署验证 |
| 07-12 | 权限注解语法修复 | VisitorLogController hasAnyPermi多参数→逗号分隔(修复现场登记500)；全项目247个@PreAuthorize审核仅1处问题；新增zhihuiyuanqu-permission-check Skill |
| 07-12 | 监控体系改造(27完成) | 核心坑:DeepSeek content首元素thinking块;Prometheus地址修正;AlertManager部署(115:9093)+告警链路端到端验证通过(labels.target) |
| 07-13 | 监控收尾+短信修复 | sendRepairSms参数修复(离线不再双发device_repair,只发offline_alert,手动报修才发repair);DeptScopeInterceptor日志噪音分析(webhook无登录态);方案C定期对账回退;设备心跳任务保持现状(iot_device已停,3类智能化保留) |
| 07-14 | Webhook调试日志+payload验证 | AlertWebhookServiceImpl日志移至try内+防NPE+截断2000字符;部署验证AlertManager payload格式(labels.target) |
| 07-22 | 访客审批逻辑修复 | dept_id改回被访人部门(非创建者);assignApproverAndDept强制匹配hostName→sys_user,失败抛ServiceException;visitor_log补dept_id两处(approveAppointment+registerWalkin);3次部署到1.94.26.126 |
| 08-09 | 设备拓扑整体升级(28) | 8个SVG图标(assets/topo-icons)+4类线缆库;iot_device加icon/cable_type;buildTopologyTree字段对齐修复拓扑页空白;拓扑页SVG图标+线缆着色;设备表单加图标/父设备/线缆3下拉;补财务部(11)/工程部(13)device:topology:query权限;备份commit 5eae970;部署1.94.26.126拓扑接口验证200 |
| 08-09 | 拓扑页渲染层修复(28a) | 真正根因:TopologyNode内联字符串template在runtime-only Vue(无编译器,module/main=vue.runtime.*,无vue$ alias)下不编译→渲染为`<!--function...-->`空注释占位→节点区空白;生产NODE_ENV静默编译警告(console 0错误误导)。修复:拆独立SFC TopologyNode.vue(vue-loader预编译render)+index.vue改import。生产部署验证:节点卡片/图标(camera.svg)/线缆cable-default均渲染,console 0错误 |
| 08-09 | 拓扑过滤+架构图连线(28b) | 后端DeviceController.topology过滤只显示参与关联设备(parent_id非空或被引用为父),隐藏孤立设备:生产12台中仅华为防火墙USG6565[1](→测试设备-权限验证)保留,10台测试/演示设备隐藏;TopologyNode.vue连线改架构图直角折线(层级40px/水平56px/padding12px/线缆着色fiber橙),admin验证通过 |
| 08-09 | 拓扑视觉升级(28c) | 工程蓝图设计:画布点阵背景`radial-gradient(#D2D7E0 1px) 20px`+`#EEF1F6`底色;工具栏透明化(按钮透明底悬停显色);卡片升级(12px圆角+双层阴影+hover#3B82F6光晕+上移3px);状态色统一palette(Online#10B981/Offline#EF4444/Unknown#9CA3AF,10px圆点+外发光);文字色#152433/#697888;连接线#98A6B6;线缆Fiber#D97706/Ethernet#3B82F6/Coaxial#9CA3AF/Wireless#10B981;零JS/零第三方。生产验证全部几何+色彩一致,console 0错误 |

## 当前状态

- **服务器**: 1.94.26.126:80/8090
- **Redis**: 1.94.26.126:6379 (已修复)
- **Git**: master
- **待处理**: 阶段11(样式优化剩余), 阶段13(公寓预付费后续)
