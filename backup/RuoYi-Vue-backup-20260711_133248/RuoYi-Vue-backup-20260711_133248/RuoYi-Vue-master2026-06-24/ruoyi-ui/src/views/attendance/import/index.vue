<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ================ Tab1: 回调接口文档 ================ -->
      <el-tab-pane label="回调接口文档" name="docs">
        <el-row :gutter="15">
          <!-- 钉钉文档 -->
          <el-col :span="12">
            <el-card header="钉钉考勤回调 (DingTalk)" shadow="hover">
              <el-alert title="配置入口" type="success" :closable="false" style="margin-bottom: 12px">
                钉钉开放平台 (open.dingtalk.com) → 创建应用 → 事件订阅
              </el-alert>

              <el-collapse v-model="dingtalkCollapse">
                <el-collapse-item title="📋 1. 回调URL配置" name="1">
                  <el-table :data="[{key:'回调URL',val:dingtalkCallbackUrl},{key:'请求方式',val:'GET + POST'},{key:'Token',val:'(自定义随机字符串)'},{key:'AESKey',val:'(43位随机字符串)'}]" size="mini">
                    <el-table-column prop="key" label="配置项" width="120" />
                    <el-table-column prop="val" label="值">
                      <template slot-scope="scope">
                        <el-input v-if="scope.row.key==='回调URL'" v-model="scope.row.val" size="mini" readonly>
                          <el-button slot="append" @click="copyText(scope.row.val)">复制</el-button>
                        </el-input>
                        <span v-else>{{ scope.row.val }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-collapse-item>

                <el-collapse-item title="📋 2. GET — URL验证" name="2">
                  <el-alert title="钉钉配置回调URL时发送GET请求验证" type="info" :closable="false" style="margin-bottom:10px" />
                  <p><b>请求参数：</b></p>
                  <el-table :data="[{name:'signature',desc:'签名，SHA256(timestamp+Token+nonce)'},{name:'timestamp',desc:'时间戳(秒)'},{name:'nonce',desc:'随机字符串'}]" size="mini">
                    <el-table-column prop="name" label="参数" width="100" />
                    <el-table-column prop="desc" label="说明" />
                  </el-table>
                  <p style="margin-top:10px"><b>处理流程：</b>验证签名 → 返回AES加密的 success 消息</p>
                  <p><b>返回：</b><code>{加密后的JSON字符串}</code></p>
                </el-collapse-item>

                <el-collapse-item title="📋 3. POST — 事件回调 (考勤打卡)" name="3">
                  <p><b>请求头：</b></p>
                  <el-table :data="[{h:'Content-Type',v:'application/json'},{h:'dingtalk-sign',v:'签名'},{h:'dingtalk-sig-time',v:'时间戳'},{h:'dingtalk-nonce',v:'随机数'}]" size="mini">
                    <el-table-column prop="h" label="Header" width="160" />
                    <el-table-column prop="v" label="值" />
                  </el-table>
                  <p style="margin-top:10px"><b>订阅事件类型：</b></p>
                  <el-tag type="success" style="margin:2px">attendance_check_record (员工打卡)</el-tag>
                  <el-tag type="warning" style="margin:2px">attendance_leave_record (员工请假)</el-tag>
                  <el-tag type="info" style="margin:2px">attendance_overtime_record (员工加班)</el-tag>
                  <p style="margin-top:10px"><b>请求体示例：</b></p>
                  <pre style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;font-size:12px;overflow-x:auto;max-height:300px">{
  "EventType": "attendance_check_record",
  "Data": {
    "userId": "manager001",
    "userName": "张三",
    "checkType": "OnDuty",         <span style="color:#6a9955">// OnDuty=上班 OffDuty=下班</span>
    "timeResult": "Normal",        <span style="color:#6a9955">// Normal/Early/Late/Absenteeism</span>
    "locationResult": "浙江省杭州市",
    "baseCheckTime": 1705651200000,
    "userCheckTime": 1705651200000,
    "groupId": "group1",
    "groupName": "固定班制"
  }
}</pre>
                  <p style="margin-top:10px"><b>返回格式（必须）：</b></p>
                  <pre style="background:#1e1e1e;color:#d4d4d4;padding:8px;border-radius:4px;font-size:12px">{"msg":"success","code":0}</pre>
                </el-collapse-item>

                <el-collapse-item title="📋 4. 数据映射规则" name="4">
                  <el-table :data="[{ding:'Normal',sys:'NORMAL(正常)'},{ding:'Early',sys:'EARLY(早退)'},{ding:'Late',sys:'LATE(迟到)'},{ding:'Absenteeism',sys:'ABSENT(缺勤)'},{ding:'NotSigned',sys:'ABSENT(缺勤)'}]" size="mini">
                    <el-table-column prop="ding" label="钉钉timeResult" width="150" />
                    <el-table-column prop="sys" label="系统状态" />
                  </el-table>
                  <p style="margin-top:10px;color:#909399">
                    打卡类型: OnDuty→签到时间 / OffDuty→签退时间<br/>
                    同一天多次打卡会自动更新已有记录
                  </p>
                </el-collapse-item>
              </el-collapse>
            </el-card>
          </el-col>

          <!-- 企业微信文档 -->
          <el-col :span="12">
            <el-card header="企业微信考勤回调 (WeCom)" shadow="hover">
              <el-alert title="配置入口" type="success" :closable="false" style="margin-bottom: 12px">
                企业微信管理后台 (work.weixin.qq.com) → 应用管理 → 自建应用 → 接收消息
              </el-alert>

              <el-collapse v-model="wecomCollapse">
                <el-collapse-item title="📋 1. 回调URL配置" name="1">
                  <el-table :data="[{key:'回调URL',val:wecomCallbackUrl},{key:'请求方式',val:'GET + POST'},{key:'Token',val:'(自定义字符串)'},{key:'EncodingAESKey',val:'(43位随机字符串)'}]" size="mini">
                    <el-table-column prop="key" label="配置项" width="140" />
                    <el-table-column prop="val" label="值">
                      <template slot-scope="scope">
                        <el-input v-if="scope.row.key==='回调URL'" v-model="scope.row.val" size="mini" readonly>
                          <el-button slot="append" @click="copyText(scope.row.val)">复制</el-button>
                        </el-input>
                        <span v-else>{{ scope.row.val }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-collapse-item>

                <el-collapse-item title="📋 2. GET — URL验证" name="2">
                  <el-alert title="企业微信配置回调URL时发送GET请求验证" type="info" :closable="false" style="margin-bottom:10px" />
                  <p><b>请求参数：</b></p>
                  <el-table :data="[{name:'msg_signature',desc:'消息签名，SHA1(token,timestamp,nonce,echostr)'},{name:'timestamp',desc:'时间戳'},{name:'nonce',desc:'随机数'},{name:'echostr',desc:'加密的随机字符串'}]" size="mini">
                    <el-table-column prop="name" label="参数" width="140" />
                    <el-table-column prop="desc" label="说明" />
                  </el-table>
                  <p style="margin-top:10px"><b>处理流程：</b>验证签名 → AES解密echostr → 返回明文</p>
                  <p><b>返回：</b><code>解密后的明文字符串</code></p>
                </el-collapse-item>

                <el-collapse-item title="📋 3. POST — 事件回调 (考勤打卡)" name="3">
                  <p><b>请求体格式：</b>XML (加密)</p>
                  <p><b>原始XML：</b></p>
                  <pre style="background:#1e1e1e;color:#d4d4d4;padding:8px;border-radius:4px;font-size:12px">&lt;xml&gt;
  &lt;ToUserName&gt;企业CorpID&lt;/ToUserName&gt;
  &lt;AgentID&gt;应用AgentID&lt;/AgentID&gt;
  &lt;Encrypt&gt;加密的消息体&lt;/Encrypt&gt;
&lt;/xml&gt;</pre>
                  <p style="margin-top:10px"><b>解密后的事件消息：</b></p>
                  <pre style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;font-size:12px;overflow-x:auto;max-height:300px">&lt;xml&gt;
  &lt;MsgType&gt;event&lt;/MsgType&gt;
  &lt;Event&gt;attendance_check_notify&lt;/Event&gt;
  &lt;UserId&gt;ZhangSan&lt;/UserId&gt;
  &lt;CheckinTime&gt;1705651200&lt;/CheckinTime&gt;  <span style="color:#6a9955">// Unix时间戳(秒)</span>
  &lt;CheckinType&gt;上班打卡&lt;/CheckinType&gt;
  &lt;LocationDetail&gt;浙江省杭州市&lt;/LocationDetail&gt;
  &lt;DeviceInfo&gt;WiFi-MAC:xx:xx&lt;/DeviceInfo&gt;
&lt;/xml&gt;</pre>
                  <p style="margin-top:10px"><b>返回：</b><code>200 OK (空响应)</code></p>
                </el-collapse-item>

                <el-collapse-item title="📋 4. 数据映射规则" name="4">
                  <el-table :data="[{wecom:'上班打卡',sys:'签到时间'},{wecom:'下班打卡',sys:'签退时间'},{wecom:'外出打卡',sys:'签退时间+位置'}]" size="mini">
                    <el-table-column prop="wecom" label="企业微信CheckinType" width="150" />
                    <el-table-column prop="sys" label="系统字段" />
                  </el-table>
                  <p style="margin-top:10px;color:#909399">
                    使用 external_user_id 字段关联企业微信UserId<br/>
                    同一天多次打卡会自动更新已有记录
                  </p>
                </el-collapse-item>
              </el-collapse>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ================ Tab2: 数据导入 ================ -->
      <el-tab-pane label="数据导入" name="import">
        <el-card><div slot="header"><span>考勤数据导入</span></div>
          <el-row :gutter="20">
            <el-col :span="12">
              <h4>Excel 导入</h4>
              <el-alert title="说明" type="info" :closable="false" style="margin:16px 0">
                <p>1. 从钉钉/企业微信管理后台导出考勤明细Excel</p>
                <p>2. 使用下方上传功能导入系统</p>
                <p>3. 系统会自动匹配用户名和部门</p>
              </el-alert>
              <el-upload :action="uploadUrl" :headers="headers" :on-success="onUpload" accept=".xlsx,.xls" drag style="margin-top:20px">
                <i class="el-icon-upload" style="font-size:48px;color:#ccc"></i>
                <div class="el-upload__text">将Excel文件拖到此处，或<em>点击上传</em></div>
              </el-upload>
            </el-col>
            <el-col :span="12">
              <h4>手动录入</h4>
              <el-form :model="form" size="small" label-width="80px" style="margin-top:16px">
                <el-form-item label="用户名"><el-input v-model="form.user_name" /></el-form-item>
                <el-form-item label="部门"><el-input v-model="form.dept_name" /></el-form-item>
                <el-form-item label="日期"><el-date-picker v-model="form.attendance_date" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item>
                <el-form-item label="签到时间"><el-time-picker v-model="form.check_in_time" value-format="HH:mm:ss" style="width:100%" /></el-form-item>
                <el-form-item label="签退时间"><el-time-picker v-model="form.check_out_time" value-format="HH:mm:ss" style="width:100%" /></el-form-item>
                <el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="正常" value="NORMAL" /><el-option label="迟到" value="LATE" /><el-option label="早退" value="EARLY" /><el-option label="缺勤" value="ABSENT" /><el-option label="加班" value="OVERTIME" /></el-select></el-form-item>
                <el-form-item label="来源"><el-select v-model="form.source" style="width:100%"><el-option label="手动" value="MANUAL" /><el-option label="钉钉" value="DINGTALK" /><el-option label="企业微信" value="WECHAT" /></el-select></el-form-item>
                <el-form-item><el-button type="primary" @click="manualAdd">添加记录</el-button></el-form-item>
              </el-form>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <!-- ================ Tab3: WebSocket 实时推送 ================ -->
      <el-tab-pane label="实时推送 (WebSocket)" name="ws">
        <el-row :gutter="15">
          <!-- WebSocket状态 -->
          <el-col :span="12">
            <el-card header="连接状态" shadow="hover">
              <div style="text-align:center;padding:20px">
                <div style="font-size:48px">
                  <i :class="wsConnected ? 'el-icon-link' : 'el-icon-unlink'" :style="{color: wsConnected ? '#67c23a' : '#f56c6c'}" />
                </div>
                <div style="font-size:18px;margin-top:10px">
                  <el-tag :type="wsConnected ? 'success' : 'danger'" size="medium">
                    {{ wsConnected ? '已连接' : '未连接' }}
                  </el-tag>
                </div>
                <div style="color:#909399;margin-top:10px;font-size:13px">
                  <div v-if="wsConnected">在线客户端: {{ wsOnlineCount }}</div>
                  <div style="margin-top:5px">
                    连接地址: <code>{{ wsUrl }}</code>
                    <el-button type="text" size="mini" @click="copyText(wsUrl)">复制</el-button>
                  </div>
                </div>
                <div style="margin-top:15px">
                  <el-button v-if="!wsConnected" type="primary" @click="wsConnect" :loading="wsConnecting">连接WebSocket</el-button>
                  <el-button v-else type="danger" @click="wsDisconnect">断开连接</el-button>
                  <el-button @click="wsLogs = []" size="small">清空日志</el-button>
                </div>
              </div>
            </el-card>

            <!-- WebSocket使用说明 -->
            <el-card header="WebSocket 接入说明" shadow="hover" style="margin-top:15px">
              <el-collapse>
                <el-collapse-item title="📋 JavaScript 接入示例" name="js">
                  <pre style="background:#1e1e1e;color:#d4d4d4;padding:12px;border-radius:4px;font-size:11px;overflow-x:auto;max-height:400px"><span style="color:#6a9955">// 创建WebSocket连接</span>
const ws = new WebSocket('{{ wsUrl }}');

<span style="color:#6a9955">// 连接成功</span>
ws.onopen = () => console.log('考勤WebSocket已连接');

<span style="color:#6a9955">// 接收考勤实时推送</span>
ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  <span style="color:#569cd6">switch</span> (msg.type) {
    <span style="color:#569cd6">case</span> 'ATTENDANCE_UPDATE':
      console.log('新考勤数据:', msg.source, msg.data);
      <span style="color:#569cd6">break</span>;
    <span style="color:#569cd6">case</span> 'RAW_MESSAGE':
      console.log('原始消息:', msg.rawData);
      <span style="color:#569cd6">break</span>;
    <span style="color:#569cd6">case</span> 'WELCOME':
      console.log('连接成功, 在线数:', msg.onlineCount);
      <span style="color:#569cd6">break</span>;
  }
};

<span style="color:#6a9955">// 心跳保持</span>
setInterval(() => ws.send(JSON.stringify({type:'PING'})), 30000);

<span style="color:#6a9955">// 断开重连</span>
ws.onclose = () => setTimeout(() => connectWS(), 5000);</pre>
                </el-collapse-item>
                <el-collapse-item title="📋 消息格式说明" name="format">
                  <el-table :data="[{type:'WELCOME',desc:'连接成功欢迎消息'},{type:'ATTENDANCE_UPDATE',desc:'考勤数据更新推送(钉钉/企业微信解析后)'},{type:'RAW_MESSAGE',desc:'原始回调消息推送(未解析)'},{type:'PONG',desc:'心跳响应'}]" size="mini">
                    <el-table-column prop="type" label="type" width="180" />
                    <el-table-column prop="desc" label="说明" />
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </el-card>
          </el-col>

          <!-- 实时日志 -->
          <el-col :span="12">
            <el-card header="实时消息日志" shadow="hover" style="max-height:500px">
              <div style="max-height:450px;overflow-y:auto" ref="logContainer">
                <div v-if="wsLogs.length === 0" style="text-align:center;color:#909399;padding:40px">
                  <i class="el-icon-info" style="font-size:48px" />
                  <p>暂无消息，请连接WebSocket后等待数据推送</p>
                </div>
                <div v-for="(log, idx) in wsLogs" :key="idx" style="border-bottom:1px solid #ebeef5;padding:8px 0">
                  <div style="display:flex;justify-content:space-between;align-items:center">
                    <el-tag :type="log.source === 'DINGTALK' ? 'primary' : 'success'" size="mini" effect="plain">
                      {{ log.source }}
                    </el-tag>
                    <el-tag :type="log.type === 'ATTENDANCE_UPDATE' ? 'success' : 'info'" size="mini" effect="plain">
                      {{ log.type }}
                    </el-tag>
                    <span style="color:#909399;font-size:11px">{{ log.timestamp }}</span>
                  </div>
                  <div style="margin-top:4px;font-size:12px">
                    <div v-if="log.data">
                      <span v-if="log.data.userName">用户: <b>{{ log.data.userName }}</b></span>
                      <span v-if="log.data.checkType"> 类型: {{ log.data.checkType }}</span>
                      <span v-if="log.data.timeResult"> 结果: {{ log.data.timeResult }}</span>
                    </div>
                    <div v-if="log.rawData" style="color:#909399;word-break:break-all;max-height:60px;overflow:hidden">
                      {{ log.rawData.substring(0, 200) }}
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ================ Tab4: 应用管理 ================ -->
      <el-tab-pane label="应用管理" name="apps">
        <el-row :gutter="15" style="margin-bottom:12px">
          <el-col :span="4">
            <el-button type="primary" icon="el-icon-plus" @click="showCreateDialog = true">创建应用条目</el-button>
          </el-col>
          <el-col :span="4">
            <el-select v-model="appFilterPlatform" placeholder="平台筛选" clearable size="small" @change="loadApps">
              <el-option label="钉钉" value="DINGTALK" />
              <el-option label="企业微信" value="WECHAT" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="appFilterStatus" placeholder="状态筛选" clearable size="small" @change="loadApps">
              <el-option label="在线" value="ONLINE" />
              <el-option label="离线" value="OFFLINE" />
              <el-option label="异常" value="ERROR" />
              <el-option label="未知" value="UNKNOWN" />
            </el-select>
          </el-col>
          <el-col :span="12" style="text-align:right">
            <el-button icon="el-icon-refresh" size="small" @click="loadApps">刷新列表</el-button>
          </el-col>
        </el-row>

        <el-table :data="appList" v-loading="appLoading" border>
          <el-table-column prop="app_name" label="应用名称" width="160" />
          <el-table-column label="平台" width="90">
            <template slot-scope="{row}">
              <el-tag :type="row.platform === 'DINGTALK' ? 'primary' : 'success'" size="small">
                {{ row.platform === 'DINGTALK' ? '钉钉' : '企业微信' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回调地址" min-width="240">
            <template slot-scope="{row}">
              <el-input v-model="row.callback_url" size="mini" readonly>
                <el-button slot="append" @click="copyText(row.callback_url)">复制</el-button>
              </el-input>
            </template>
          </el-table-column>
          <el-table-column prop="token" label="Token" width="160">
            <template slot-scope="{row}">
              <el-tooltip :content="row.token" placement="top">
                <span style="font-family:monospace;font-size:11px">{{ row.token ? row.token.substring(0, 16) + '...' : '' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="aes_key" label="AESKey" width="160">
            <template slot-scope="{row}">
              <el-tooltip :content="row.aes_key" placement="top">
                <span style="font-family:monospace;font-size:11px">{{ row.aes_key ? row.aes_key.substring(0, 16) + '...' : '' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template slot-scope="{row}">
              <el-tag :type="appStatusTag(row.status)" size="small" effect="plain">
                <i :class="appStatusIcon(row.status)" style="margin-right:4px"></i>
                {{ appStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近测试" width="160">
            <template slot-scope="{row}">
              <div v-if="row.last_test_time" style="font-size:11px">
                <div>{{ row.last_test_time }}</div>
                <div style="color:#909399;word-break:break-all">{{ row.last_test_result || '' }}</div>
              </div>
              <span v-else style="color:#909399">未测试</span>
            </template>
          </el-table-column>
          <el-table-column label="最近回调" width="160">
            <template slot-scope="{row}">
              <div v-if="row.last_callback_time" style="font-size:11px">
                <div>{{ row.last_callback_time }}</div>
                <div style="color:#909399">累计: {{ row.total_callbacks || 0 }}次</div>
              </div>
              <span v-else style="color:#909399">未收到回调</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template slot-scope="{row}">
              <el-button type="success" size="mini" @click="testApp(row)" :loading="row._testing">测试</el-button>
              <el-button type="warning" size="mini" @click="simulateApp(row)" :loading="row._simulating">模拟</el-button>
              <el-button type="info" size="mini" @click="regenerateKeys(row)">重置</el-button>
              <el-button type="danger" size="mini" @click="deleteApp(row)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="appTotal" :page.sync="appPage.pageNum" :limit.sync="appPage.pageSize" @pagination="loadApps" />
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 创建应用弹窗 ============ -->
    <el-dialog title="创建应用条目" :visible.sync="showCreateDialog" width="520px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="100px" size="small">
        <el-form-item label="应用名称" required>
          <el-input v-model="createForm.appName" placeholder="例如：园区考勤-钉钉应用" />
        </el-form-item>
        <el-form-item label="平台" required>
          <el-radio-group v-model="createForm.platform">
            <el-radio label="DINGTALK">钉钉 (DingTalk)</el-radio>
            <el-radio label="WECHAT">企业微信 (WeCom)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.platform === 'WECHAT'" label="AgentId(选填)">
          <el-input v-model="createForm.agentId" placeholder="企业微信应用AgentId" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-alert type="info" :closable="false" style="margin-bottom:10px">
          <p>系统将<b>自动生成</b>以下全部密钥参数，无需手动填写：</p>
          <p>• {{ createForm.platform === 'WECHAT' ? 'CorpID前缀' : 'AppKey' }}（如 {{ createForm.platform === 'WECHAT' ? 'ww_' : 'ding_' }}xxxxxxxxxxxx）</p>
          <p>• AppSecret（32位强随机字符串）</p>
          <p>• Token（8-16位验证Token）</p>
          <p>• AESKey（43位Base64安全字符）</p>
          <p>• 回调地址（自动拼接当前域名）</p>
        </el-alert>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createApp" :loading="appCreating">创建应用</el-button>
      </div>
    </el-dialog>

    <!-- ============ 查看密钥弹窗 ============ -->
    <el-dialog title="应用密钥详情" :visible.sync="showKeysDialog" width="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="应用名称">{{ keysData.appName }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ keysData.platform === 'DINGTALK' ? '钉钉' : '企业微信' }}</el-descriptions-item>
        <el-descriptions-item :label="keysData.platform === 'WECHAT' ? 'CorpID' : 'AppKey'">
          <el-input v-model="keysData.appKey" readonly size="mini">
            <el-button slot="append" @click="copyText(keysData.appKey)">复制</el-button>
          </el-input>
        </el-descriptions-item>
        <el-descriptions-item label="AppSecret">
          <el-input v-model="keysData.appSecret" readonly size="mini" type="password" show-password>
            <el-button slot="append" @click="copyText(keysData.appSecret)">复制</el-button>
          </el-input>
        </el-descriptions-item>
        <el-descriptions-item label="Token">
          <el-input v-model="keysData.token" readonly size="mini">
            <el-button slot="append" @click="copyText(keysData.token)">复制</el-button>
          </el-input>
        </el-descriptions-item>
        <el-descriptions-item label="AESKey">
          <el-input v-model="keysData.aes_key" readonly size="mini">
            <el-button slot="append" @click="copyText(keysData.aes_key)">复制</el-button>
          </el-input>
        </el-descriptions-item>
        <el-descriptions-item label="回调地址">
          <el-input v-model="keysData.callback_url" readonly size="mini">
            <el-button slot="append" @click="copyText(keysData.callback_url)">复制</el-button>
          </el-input>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export default {
  name: 'AttendanceImport',
  data() {
    const host = window.location.host
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return {
      activeTab: 'docs',
      dingtalkCollapse: ['1', '2', '3'],
      wecomCollapse: ['1', '2', '3'],
      // 上传表单
      uploadUrl: process.env.VUE_APP_BASE_API + '/attendance/import',
      headers: { Authorization: 'Bearer ' + getToken() },
      form: { user_name: '', dept_name: '', attendance_date: '', check_in_time: '', check_out_time: '', status: 'NORMAL', source: 'MANUAL' },
      // WebSocket
      wsUrl: protocol + '//' + host + '/ws/attendance',
      wsConnected: false,
      wsConnecting: false,
      wsOnlineCount: 0,
      wsLogs: [],
      wsSocket: null,
      wsReconnectTimer: null,
      // 应用管理
      appList: [],
      appLoading: false,
      appTotal: 0,
      appPage: { pageNum: 1, pageSize: 10 },
      appFilterPlatform: '',
      appFilterStatus: '',
      showCreateDialog: false,
      appCreating: false,
      createForm: { appName: '', platform: 'DINGTALK', agentId: '', description: '' },
      showKeysDialog: false,
      keysData: {}
    }
  },
  computed: {
    dingtalkCallbackUrl() { return window.location.protocol + '//' + window.location.host + '/attendance/callback/dingtalk' },
    wecomCallbackUrl() { return window.location.protocol + '//' + window.location.host + '/attendance/callback/wecom' }
  },
  created() { /* 不自动连接WebSocket，等用户手动连接 */ },
  beforeDestroy() { this.wsDisconnect() },
  methods: {
    // ===== Excel上传 =====
    onUpload(res) {
      if (res.code === 200) this.$message.success('导入成功: ' + res.msg)
      else this.$message.error(res.msg || '导入失败')
    },
    manualAdd() {
      request({ url: '/attendance/add', method: 'post', data: this.form }).then(() => {
        this.$message.success('添加成功')
        Object.keys(this.form).forEach(k => this.form[k] = '')
      })
    },

    // ===== WebSocket =====
    wsConnect() {
      if (this.wsSocket) return
      this.wsConnecting = true
      try {
        this.wsSocket = new WebSocket(this.wsUrl)
        this.wsSocket.onopen = () => {
          this.wsConnected = true
          this.wsConnecting = false
          this.$message.success('WebSocket 连接成功')
          // 发送心跳
          if (this.wsReconnectTimer) clearInterval(this.wsReconnectTimer)
          this.wsReconnectTimer = setInterval(() => {
            if (this.wsSocket && this.wsSocket.readyState === WebSocket.OPEN) {
              this.wsSocket.send(JSON.stringify({ type: 'PING' }))
            }
          }, 30000)
        }
        this.wsSocket.onmessage = (event) => {
          try {
            const msg = JSON.parse(event.data)
            if (msg.type === 'WELCOME') {
              this.wsOnlineCount = msg.onlineCount || 0
            }
            // 添加到日志（最多保留100条）
            this.wsLogs.unshift({
              type: msg.type,
              source: msg.source || 'SYSTEM',
              timestamp: msg.timestamp || '',
              data: msg.data || null,
              rawData: msg.rawData || null
            })
            if (this.wsLogs.length > 100) this.wsLogs.length = 100
            // 自动滚动到底部
            this.$nextTick(() => {
              const el = this.$refs.logContainer
              if (el) el.scrollTop = 0 // 新消息在顶部
            })
          } catch (e) { /* ignore */ }
        }
        this.wsSocket.onclose = () => {
          this.wsConnected = false
          this.wsSocket = null
          if (this.wsReconnectTimer) clearInterval(this.wsReconnectTimer)
        }
        this.wsSocket.onerror = () => {
          this.wsConnecting = false
          this.$message.error('WebSocket 连接失败')
        }
      } catch (e) {
        this.wsConnecting = false
        this.$message.error('WebSocket 连接失败: ' + e.message)
      }
    },
    wsDisconnect() {
      if (this.wsReconnectTimer) { clearInterval(this.wsReconnectTimer); this.wsReconnectTimer = null }
      if (this.wsSocket) { this.wsSocket.close(); this.wsSocket = null }
      this.wsConnected = false
      this.wsConnecting = false
    },

    // ===== 应用管理 =====
    loadApps() {
      this.appLoading = true
      const params = { ...this.appPage }
      if (this.appFilterPlatform) params.platform = this.appFilterPlatform
      if (this.appFilterStatus) params.status = this.appFilterStatus
      request({ url: '/attendance/app/list', method: 'get', params }).then(res => {
        this.appList = (res.rows || []).map(r => ({
          ...r,
          _testing: false,
          _simulating: false
        }))
        this.appTotal = res.total
        this.appLoading = false
      }).catch(() => { this.appLoading = false })
    },
    createApp() {
      if (!this.createForm.appName) { this.$message.warning('请输入应用名称'); return }
      if (!this.createForm.platform) { this.$message.warning('请选择平台'); return }
      this.appCreating = true
      request({ url: '/attendance/app', method: 'post', data: this.createForm }).then(res => {
        if (res.code === 200) {
          const d = res.data
          this.$message.success('应用创建成功！')
          this.keysData = {
            appName: this.createForm.appName,
            platform: this.createForm.platform,
            appKey: d.appKey,
            appSecret: d.appSecret,
            token: d.token,
            aes_key: d.aesKey,
            callback_url: d.callbackUrl
          }
          this.showCreateDialog = false
          this.showKeysDialog = true
          // 重置表单
          this.createForm = { appName: '', platform: 'DINGTALK', agentId: '', description: '' }
          this.loadApps()
        } else {
          this.$message.error(res.msg || '创建失败')
        }
        this.appCreating = false
      }).catch(() => { this.appCreating = false })
    },
    regenerateKeys(row) {
      this.$confirm('确定重新生成 Token 和 AESKey？原有密钥将立即失效。', '提示', { type: 'warning' }).then(() => {
        request({ url: '/attendance/app/' + row.app_id + '/regenerate', method: 'post' }).then(res => {
          if (res.code === 200) {
            this.keysData = {
              appName: row.app_name,
              platform: row.platform,
              appKey: res.data.appKey || row.app_key,
              appSecret: res.data.appSecret || row.app_secret,
              token: res.data.token,
              aes_key: res.data.aesKey,
              callback_url: row.callback_url
            }
            this.showKeysDialog = true
            this.$message.success('密钥已重新生成')
            this.loadApps()
          }
        })
      }).catch(() => {})
    },
    deleteApp(row) {
      this.$confirm('确定删除应用「' + row.app_name + '」？', '提示', { type: 'warning' }).then(() => {
        request({ url: '/attendance/app/' + row.app_id, method: 'delete' }).then(() => {
          this.$message.success('已删除')
          this.loadApps()
        })
      }).catch(() => {})
    },
    testApp(row) {
      this.$set(row, '_testing', true)
      request({ url: '/attendance/app/' + row.app_id + '/test', method: 'post' }).then(res => {
        if (res.code === 200) {
          const d = res.data
          if (d.result === '可达') {
            this.$message.success('连接测试通过: HTTP ' + d.httpStatus + ', 延迟 ' + d.latency + 'ms')
          } else {
            this.$message.warning('连接测试: ' + (d.result || d.error || '未知结果'))
          }
        }
        this.$set(row, '_testing', false)
        this.loadApps()
      }).catch(() => { this.$set(row, '_testing', false); this.loadApps() })
    },
    simulateApp(row) {
      this.$set(row, '_simulating', true)
      request({ url: '/attendance/app/' + row.app_id + '/simulate', method: 'post' }).then(res => {
        if (res.code === 200) {
          this.$message.success('模拟考勤回调成功: ' + (res.data.platform || '') + ' HTTP ' + res.data.httpStatus)
        } else {
          this.$message.error(res.msg || '模拟失败')
        }
        this.$set(row, '_simulating', false)
        this.loadApps()
      }).catch(() => { this.$set(row, '_simulating', false); this.loadApps() })
    },
    appStatusTag(s) {
      const m = { ONLINE: 'success', OFFLINE: 'danger', ERROR: 'warning', UNKNOWN: 'info' }
      return m[s] || 'info'
    },
    appStatusIcon(s) {
      const m = { ONLINE: 'el-icon-check', OFFLINE: 'el-icon-close', ERROR: 'el-icon-warning', UNKNOWN: 'el-icon-question' }
      return m[s] || 'el-icon-question'
    },
    appStatusLabel(s) {
      const m = { ONLINE: '在线', OFFLINE: '离线', ERROR: '异常', UNKNOWN: '未知' }
      return m[s] || s
    },

    // ===== 工具 =====
    copyText(text) {
      const el = document.createElement('textarea')
      el.value = text
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
      this.$message.success('已复制')
    }
  }
}
</script>

<style scoped>
pre { margin: 0; }
.el-collapse { margin-top: 8px; }
</style>
