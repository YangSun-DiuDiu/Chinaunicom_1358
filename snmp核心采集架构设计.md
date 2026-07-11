基于 RuoYi-Vue 前后端分离框架开发独立 SNMP 网络设备管理模块，**采用 Categraf 独立采集器替代后端 SNMP4J 采集逻辑**，Categraf 内置 Go 原生 SNMP 协议栈，不依赖系统 snmpwalk 命令，支持单设备独立 snmp.toml 配置文件管理，完整落地双视图、双驾驶舱功能，遵循网络工程师、系统分析师、架构师三方规范：

### 一、核心采集架构设计

1. Categraf 采集层：

   - 独立部署 Categraf，读取 `conf/input.snmp/` 下全部 *.toml 配置，每个文件对应一台设备专属采集规则；
   - 目录规范：通用 OID 模板命名 template_xxx.toml，单设备配置命名 dev_IP 下划线.toml；
   - 支持 template_files 引用公共模板，差异化配置仅存放设备私有 OID、SNMPv3 认证、专属采集周期；
   - 内置 SNMP Trap 监听，指标标准化附加 device_ip、device_type 标签，直接 Remote Write 写入独立 Docker 部署 Prometheus 时序库；
   - 修改配置后发送 SIGHUP 信号热重载，无需重启采集进程。

2. 配置文件上传与落地方案（核心需求）：

   - 若依前端创建设备页面提供两种配置模式：通用模板快速生成、自定义 toml 文件上传；
   - 前端上传文件先存入后端中转目录 upload/categraf_snmp/temp，后端做文件后缀、大小、toml 语法安全校验；
   - 校验通过自动复制文件至 Categraf 插件目录 /opt/categraf/conf/input.snmp/；
   - snmp_device 设备表存储配置文件名、完整 toml 文本、文件 MD5 用于备份与一致性校验；
   - 删除设备时同步删除对应 toml 配置文件，移入 bak 备份目录；

3. 业务层（module-snmp）职责：

   

   移除所有 SNMP 采集代码，仅负责设备资产 CRUD、SNMP 配置文件上传部署、定时同步 Prometheus 实时指标至 Redis/MySQL、封装 Prometheus 查询接口、统一告警入库推送。

### 二、Maven 依赖变更

1. 删除 snmp4j、actuator、micrometer-prometheus 埋点相关依赖；
2. 新增 Prometheus HTTP 客户端，用于后端查询时序曲线；
3. 保留 ipaddress 网段工具、若依原生 Redis、Quartz、文件上传工具。

### 三、网络工程师视角

1. Categraf 原生 GETBULK 批量采集整机、端口、硬件指标，无需外部 snmpwalk；
2. 分设备类型提供通用 OID 模板（交换机 / 防火墙 / 摄像头），单设备专属文件仅覆盖私有 MIB、特殊认证；
3. 支持 SNMP v1/v2c/v3 完整加密认证，Trap UDP162 端口接收设备主动告警；
4. 采集频率分层：整机 5min、端口流量 30s，单设备可独立自定义周期。

### 四、系统分析师数据库与流程

1. snmp_device 表新增 snmp_config_file、config_content、config_md5 字段，绑定专属采集配置；
2. 业务流程：新增设备→上传 / 生成 toml→后端部署至 Categraf 目录→入库备份→SIGHUP 重载采集；删除设备→删除对应 toml 文件；
3. 双视图展示：
   - 表格视图：读取 Redis 缓存最新标准化指标，按设备类型动态展示参数列；
   - 驾驶舱视图：Grafana iframe 弹窗嵌入，设备列表点击按钮 URL 拼接 var-device_ip 传参筛选曲线；Grafana 开启匿名免登、允许页面嵌入。
4. 定时任务增加配置一致性校验：对比磁盘 toml 文件 MD5 与数据库，不一致产生系统告警。

### 五、架构师整合规范

1. 新增 SnmpConfigFileService 配置文件管理服务，实现上传、部署、删除、重载、备份全套能力；
2. 三层隔离架构：Categraf 采集层、Prometheus 时序存储层、RuoYi 业务服务层；
3. 扩展预留：分布式多 Categraf 采集器配置同步能力、配置中心对接方案；
4. 性能优化：Redis 缓存实时指标，全局驾驶舱聚合数据缓存，Categraf 内置采集并发限流；
5. 配套交付：Categraf 完整 SNMP 多文件配置示例、文件上传 Vue 页面、后端配置管理代码、Prometheus docker-compose、Grafana 变量仪表盘配置、Linux 目录权限与重载脚本。
6. Categraf 采用 **Prometheus Remote Write V1 标准协议** 直接推送 SNMP 指标至 Prometheus，接口地址固定 `http://prometheus:9090/api/v1/write`；
7. Prometheus 启动必须增加参数 `--web.enable-remote-write-receiver` 开启 V1 协议接收端口，否则无法接收 Categraf 写入数据；
8. 废弃 PushGateway、Java micrometer 埋点、actuator 暴露指标等中间链路，简化架构；
9. Categraf 采集时统一附加 `device_ip`、`device_type` 全局标签，供 Grafana URL 变量 `var-device_ip` 筛选单设备时序曲线；
10. Categraf 配置文件 `conf/config.toml` 统一配置 [[writers]] 远程写入地址，所有 input.snmp 下设备采集数据自动复用该输出通道；
11. Remote Write V2 为实验协议，项目全程仅使用稳定 V1 协议，不做兼容适配。

### 六、约束与风险规避

1. 严格控制 Categraf 配置目录读写权限，后端进程仅允许新增 / 删除自身管理的 dev_xxx.toml，禁止修改通用 template 模板；
2. 上传文件增加安全过滤，拦截恶意路径、非法 SNMP 参数；
3. 所有时序数据流：网络设备 → Categraf → Prometheus → Grafana / 若依 ECharts；
4. 设备细粒度权限由若依控制，Grafana 仅提供只读匿名查看；
5. 定时清理 Prometheus 过期时序数据，防止磁盘溢出。