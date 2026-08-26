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
| 28d. 线缆加长 | ✅ complete | 用户反馈线缆偏短,层级间距40→64px(+60%):connector 28→44、padding-top 12→20、子竖线12→20、水平间距56→64。commit 3f83728 |
| 28e. 问题复盘 | ✅ complete | findings.md补5个调试坑:ssh -f退出杀进程/Redis过期playwright stale/playwright-cli污染dist EBUSY/DeptScope影响非admin拓扑可见/线缆几何调参 |
| 29. 文档中心模块 | ✅ complete | DocSyncLog实体+Mapper+DocServiceImpl扫描引擎(500行)+DocController(5接口);DocTree树形导航+DocReader Markdown渲染+Code Atlas设计体系;Quartz每60s检测commit变化自动同步;SecurityConfig+permission.js+router白名单匿名访问;13模块228文档自动生成;部署1.94.26.126验证200 |
| 30. 文档中心部署 | ✅ complete | SQL执行(sys_doc_sync_log+菜单+定时任务);marked@4.3.0依赖修复;本地maven+npm构建;JAR 106MB+前端dist上传;docs/目录(255.md)同步到服务器;后端重启验证HTTP 200 |
| 31. Vue页面文件命名区分 | ✅ complete | 所有Vue页面都是index.vue→目录树无法区分;DocServiceImpl.buildTreeNode加父目录前缀(list/index.vue);scanVueFiles骨架标题同步改造;mvn编译+scp JAR+重启;生产/doc/tree HTTP 200验证 |
| 32. 文档中心维护Skill | ✅ complete | .claude/skills/zhihuiyuanqu-doc-center/SKILL.md:5操作场景+Vue命名规范+3类文档模板+6注意事项+快速参考表 |
| 33. 设备管理文档补全 | ✅ complete | DeviceMapper.md+DeviceMapper.xml.md+device.js.md+Device.md(4份);dept_id过滤机制+12条SQL+13 API;tar+paramiko上传生产验证230行 |
| 34. 设备父设备/拓扑状态修复 | ✅ complete | 父设备无法修改/删除修复(DeviceMapper.xml无条件更新parent_id+前端空串归一null);修改后状态无法同步到拓扑(keep-alive缓存→topology页activated()重拉);loadParentDevices排除自身+全部后代防循环;部署1.94.26.126验证106→NULL→106 |
| 35. zhaojg部门数据隔离越权修复 | ✅ complete | 根因:DeptScopeInterceptor白名单按SQL文本contains判定,iot_device LEFT JOIN sys_dept被误判系统表跳过过滤→非管理员可见全部设备。修复:白名单改主表判定(extractMainTable)+非管理员强制追加"本部门+子部门"find_in_set过滤+删除WHERE含dept_id即跳过后门;DeviceMapper.xml保留deptId条件(路线甲);编译部署重启;端到端验证:zhaojg(dept 101)仅见2台(106/114),admin见12台 |
| 35a. 账号密码规范入档 | ✅ complete | 所有业务账号密码统一admin123(含admin/zhaojg/各测试账号);CLAUDE.md新增"系统登录账号"章节+长期记忆account-password-convention.md+dept-scope-isolation.md |
| 36. 拓扑跨部门父设备修复 | ✅ complete | 根因:admin给zhaojg根设备106设父设备100(不在zhaojg可见范围),部门隔离下buildTopologyTree从根遍历不到106→拓扑图空白。修复:DeviceController.buildTopologyTree父设备不在可见集合时提升为根节点;部署验证zhaojg见106(+子114),admin树不变 |
| 37. 会议室组织架构树 | ✅ complete | 会议室管理页加TreePanel组织树(与sys_dept实时同步):Controller新增/meeting/room/deptTree;列表SQL加LEFT JOIN sys_dept+deptId过滤(find_in_set含子部门)+deptName列;meeting_room移出DeptScopeInterceptor白名单(非管理员仅见本部门+子部门);DeptFillAspect自动归属当前部门(编辑不改dept_id,update SQL不加);存量dept_id已全填充(0行NULL,无需回填);部署验证admin树/过滤/zhaojg隔离/simpleList全通过 |
| 37a. 组织树渲染Bug修复 | ✅ complete | 浏览器验证发现index.vue漏`components:{TreePanel}`注册→<tree-panel>当未知元素渲染为空(树不显示+resetQuery $refs报错);补注册后前端重构建+部署,浏览器复验树渲染/节点过滤/重置全通过 |
| 38. 会议室使用状态总览 | ✅ complete | 一页展示所有会议室当前状态(空闲/即将/使用中)+预约记录;MeetingRoomController新增/meeting/room/status聚合接口(三态计算从MeetingBoardController抽到IMeetingRoomService复用+selectTodayApprovedList避免N+1);前端status/index.vue(统计卡片+三态渲染+预约弹窗时间线+30s自动刷新);router静态路由status;sys_menu插2180;全量部署+端到端验证(无token 401/admin 3室/zhaojg仅room3/IN_USE三态) |
| 38a. 状态墙大屏美化+网格铺满 | ✅ complete | 投影电视大屏:指挥中心墙重写(深蓝渐变#0a1733→#0e2a63+青色#00d4ff,去操作列整卡可点,实时大时钟,时间轴预约色块+"现在"指针);网格按会议室数量选cols×rows铺满整屏(minmax(0,1fr)等分);>16分页+20s自动轮播;墙固定calc(100vh-50px)恰好铺满app-main;浏览器验证11卡4×3/16卡4×4/自动轮播全通过 |
| 39. 访客部门登记二维码 | ✅ complete | 每部门独立登记二维码,扫码仅可预约该部门(含子部门)人员:核心表visitor_dept_qrcode(dept_id唯一+register_code唯一,URL用16位code而非deptId防暴露);后端VisitorDeptQrcode全套CRUD+/visitor/deptQrcode/*(init批量初始化仅admin)+VisitorH5Controller改(新增/info/{code},/hosts带code查真实员工,/submit校验部门子树ancestors);registerWalkin三参重载+validateDeptScope;前端qrcode/index.vue管理页+QrCode组件show(url)+h5/register.vue从?code=读码;旧码无code退回原历史被访人模式;部署生产:SQL建表+菜单2181~2185、jar+dist、15部门批量初始化、H5 info/hosts/非法码全链路验证通过 |
| 39a. 二维码页修复+侧边栏改名 | ✅ complete | ①新增弹窗无法选部门根因:`el-tree-select`是Element Plus(Vue3)组件,本项目Vue2+Element UI 2.15.14无此组件→渲染为空。改`@riophae/vue-treeselect`(对齐system/user写法)+enabledDeptOptions过滤停用部门 ②侧边栏「部门登记二维码」→「二维码」(仅router meta.title,DB与页内标题用户指定不动)③部署踩坑:`VisitorDeptQrcodeMapper.xml`引用remark列但建表漏建→init报Unknown column 'v.remark',生产ALTER补齐。前端重构建(app.c458e0aa.js)+部署验证200 |
