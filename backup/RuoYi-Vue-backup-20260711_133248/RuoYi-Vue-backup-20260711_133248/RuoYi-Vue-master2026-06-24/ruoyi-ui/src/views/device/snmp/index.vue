<template>
  <div class="snmp-container">
    <!-- ========== 顶部工具栏 ========== -->
    <div class="snmp-toolbar">
      <div class="toolbar-left">
        <el-select v-model="selectedDeviceId" placeholder="请选择网络设备" @change="loadSnmpInfo" filterable size="small"
          style="width: 340px">
          <el-option v-for="d in deviceList" :key="d.deviceId" :label="d.deviceName + ' (' + d.ipAddress + ')'"
            :value="d.deviceId" />
        </el-select>
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="loadSnmpInfo">
          刷新
        </el-button>
        <el-tag v-if="snmpMode" :type="snmpMode.indexOf('snmpwalk') >= 0 ? 'success' : 'warning'" size="small"
          effect="plain">
          <i :class="snmpMode.indexOf('snmpwalk') >= 0 ? 'el-icon-check' : 'el-icon-warning'"></i>
          {{ snmpMode }}
        </el-tag>
        <span v-else class="snmp-mode-pending">SNMP模式检测中…</span>
      </div>
      <div class="toolbar-right">
        <el-button v-if="selectedDeviceId" type="warning" size="small" icon="el-icon-data-line"
          @click="grafanaOpen = true">
          Grafana 驾驶舱
        </el-button>
      </div>
    </div>

    <!-- ========== 设备选择后展示 ========== -->
    <template v-if="selectedDeviceId">
      <!-- ========== 统计概览卡片 ========== -->
      <el-row :gutter="16" class="snmp-stats">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card" v-loading="loading">
            <div class="stat-label">系统名称</div>
            <div class="stat-value" :title="snmpInfo.sysName">{{ snmpInfo.sysName || '—' }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card" v-loading="loading">
            <div class="stat-label">运行时间</div>
            <div class="stat-value">{{ snmpInfo.sysUpTime || '—' }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-label">端口数量</div>
            <div class="stat-value">{{ portList.length || '—' }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card" v-loading="loadingNvr">
            <div class="stat-label">存储使用</div>
            <div class="stat-value" v-if="currentDevice && currentDevice.deviceType === 'NVR' && nvrInfo.totalSizeGB">
              {{ nvrInfo.totalUsedGB || 0 }} / {{ nvrInfo.totalSizeGB }} GB
            </div>
            <div class="stat-value" v-else>—</div>
          </div>
        </el-col>
      </el-row>

      <!-- ========== 错误提示 ========== -->
      <el-alert v-if="snmpError" :title="snmpError" type="warning" :closable="false" show-icon
        style="margin-bottom: 12px" />

      <!-- ========== Tab 功能区 ========== -->
      <el-tabs v-model="activeTab" type="border-card" class="snmp-tabs">
        <!-- 端口监控 -->
        <el-tab-pane label="端口监控" name="ports">
          <div v-if="portError" class="empty-state">
            <i class="el-icon-warning"></i>
            <p>{{ portError }}</p>
          </div>
          <el-table v-else :data="portList" size="small" v-loading="loadingPort" max-height="420" stripe>
            <el-table-column prop="name" label="端口名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="90" />
            <el-table-column prop="speed" label="速率" width="100" />
            <el-table-column prop="status" label="状态" width="90">
              <template slot-scope="scope">
                <el-tag :type="scope.row.status === 'up' ? 'success' : 'danger'" size="mini">
                  {{ scope.row.status === 'up' ? '启用' : scope.row.status === 'down' ? '停用' : scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!portError && portList.length === 0 && !loadingPort" class="empty-state">
            <i class="el-icon-info"></i>
            <p>暂无端口数据</p>
          </div>
        </el-tab-pane>

        <!-- MIB 查询 -->
        <el-tab-pane label="MIB 查询" name="mib">
          <div class="mib-query-bar">
            <el-select v-model="selectedMib" placeholder="请选择MIB对象" @change="onMibChange" size="small"
              style="width: 320px">
              <el-option-group label="System 系统组 (1.3.6.1.2.1.1)">
                <el-option label="sysDescr 系统描述" :value="'1.3.6.1.2.1.1.1.0'" />
                <el-option label="sysObjectID 设备OID" :value="'1.3.6.1.2.1.1.2.0'" />
                <el-option label="sysUpTime 运行时间" :value="'1.3.6.1.2.1.1.3.0'" />
                <el-option label="sysContact 联系人" :value="'1.3.6.1.2.1.1.4.0'" />
                <el-option label="sysName 系统名称" :value="'1.3.6.1.2.1.1.5.0'" />
                <el-option label="sysLocation 位置" :value="'1.3.6.1.2.1.1.6.0'" />
              </el-option-group>
              <el-option-group label="Interfaces 接口组 (1.3.6.1.2.1.2)">
                <el-option label="ifNumber 接口数量" :value="'1.3.6.1.2.1.2.1.0'" />
                <el-option label="ifDescr 接口描述" :value="'1.3.6.1.2.1.2.2.1.2'" />
                <el-option label="ifType 接口类型" :value="'1.3.6.1.2.1.2.2.1.3'" />
                <el-option label="ifSpeed 接口速率" :value="'1.3.6.1.2.1.2.2.1.5'" />
                <el-option label="ifStatus 接口状态" :value="'1.3.6.1.2.1.2.2.1.8'" />
              </el-option-group>
              <el-option-group label="IP 协议组 (1.3.6.1.2.1.4)">
                <el-option label="ipForwarding 转发" :value="'1.3.6.1.2.1.4.1.0'" />
                <el-option label="ipAddrTable 地址表" :value="'1.3.6.1.2.1.4.20'" />
              </el-option-group>
              <el-option-group label="ICMP / TCP / UDP (1.3.6.1.2.1.5-7)">
                <el-option label="icmpInMsgs ICMP输入" :value="'1.3.6.1.2.1.5.1.0'" />
                <el-option label="tcpConnTable TCP连接" :value="'1.3.6.1.2.1.6.13'" />
                <el-option label="udpTable UDP监听" :value="'1.3.6.1.2.1.7.5'" />
              </el-option-group>
              <el-option-group label="SNMP 统计 (1.3.6.1.2.1.11)">
                <el-option label="snmpInPkts SNMP包数" :value="'1.3.6.1.2.1.11.1.0'" />
              </el-option-group>
              <el-option-group label="Host Resources (1.3.6.1.2.1.25)">
                <el-option label="hrStorageTable 存储表" :value="'1.3.6.1.2.1.25.2.3'" />
              </el-option-group>
              <el-option-group label="">
                <el-option label="—— 自定义OID ——" :value="'__custom__'" />
              </el-option-group>
            </el-select>

            <el-input v-if="showCustomOid" v-model="customOid" placeholder="输入自定义OID，如 1.3.6.1.2.1.1.5.0" size="small"
              style="width: 260px; margin-left: 8px" />

            <el-button type="primary" size="small" icon="el-icon-search" @click="executeCustom" :loading="loadingCustom"
              style="margin-left: 8px">
              GET
            </el-button>
            <el-button size="small" icon="el-icon-menu" @click="executeWalk" :loading="loadingWalk">
              Walk 遍历
            </el-button>
          </div>

          <!-- 查询结果 -->
          <div v-if="customResult !== null" class="mib-result">
            <div class="mib-result-header">
              <el-tag type="info" size="small">OID: {{ currentOid }}</el-tag>
            </div>
            <div v-if="typeof customResult === 'string'" class="mib-result-single">
              <span class="mib-result-label">值:</span>
              <code>{{ customResult }}</code>
            </div>
            <el-table v-else :data="walkTableData" size="small" max-height="380" stripe style="margin-top: 8px">
              <el-table-column prop="oidName" label="OID名称" width="160" show-overflow-tooltip />
              <el-table-column prop="oid" label="OID" min-width="200" show-overflow-tooltip />
              <el-table-column prop="valueDesc" label="值（描述）" min-width="160" show-overflow-tooltip />
            </el-table>
          </div>
          <div v-else class="mib-placeholder">
            <i class="el-icon-upload2"></i>
            <p>选择 MIB 对象后点击 GET 或 Walk 遍历查询</p>
          </div>
        </el-tab-pane>

        <!-- 配置管理 -->
        <el-tab-pane label="配置管理" name="config">
          <el-row :gutter="24">
            <el-col :span="12">
              <div class="config-section">
                <h4 class="config-title">上传 TOML 配置文件</h4>
                <el-upload ref="upload" :action="uploadUrl" :data="uploadData" :headers="uploadHeaders"
                  :before-upload="beforeConfigUpload" :on-success="onConfigSuccess" :auto-upload="true" accept=".toml"
                  :show-file-list="false">
                  <el-button size="small" type="primary" icon="el-icon-upload2">选择并上传 .toml 文件</el-button>
                </el-upload>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="config-section">
                <h4 class="config-title">使用预置模板</h4>
                <div class="config-template-row">
                  <el-select v-model="templateType" placeholder="选择模板" size="small" style="width: 150px">
                    <el-option label="交换机模板" value="switch" />
                    <el-option label="防火墙模板" value="firewall" />
                    <el-option label="摄像头模板" value="camera" />
                  </el-select>
                  <el-button size="small" type="success" icon="el-icon-document" @click="deployTemplate"
                    :loading="deploying" style="margin-left: 10px">
                    生成并部署
                  </el-button>
                </div>
              </div>
            </el-col>
          </el-row>
          <!-- 当前部署状态 -->
          <div v-if="currentDevice && currentDevice.snmpConfigFile" class="config-deployed">
            <i class="el-icon-circle-check"></i>
            已部署配置文件: <strong>{{ currentDevice.snmpConfigFile }}</strong>
            <span class="config-md5">MD5: {{ currentDevice.configMd5 ? currentDevice.configMd5.substring(0, 8) : '—'
              }}</span>
          </div>
        </el-tab-pane>

        <!-- 设备详情 -->
        <el-tab-pane label="设备详情" name="detail">
          <el-row :gutter="24">
            <el-col :span="12">
              <h4 class="config-title">设备基本信息</h4>
              <el-descriptions :column="1" border size="small" v-loading="loading">
                <el-descriptions-item label="系统名称">{{ snmpInfo.sysName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="系统描述" :contentStyle="{ 'word-break': 'break-all' }">{{ snmpInfo.sysDescr ||
                  '—'
                  }}</el-descriptions-item>
                <el-descriptions-item label="运行时间">{{ snmpInfo.sysUpTime || '—' }}</el-descriptions-item>
                <el-descriptions-item label="系统联系人">{{ snmpInfo.sysContact || '—' }}</el-descriptions-item>
                <el-descriptions-item label="系统位置">{{ snmpInfo.sysLocation || '—' }}</el-descriptions-item>
              </el-descriptions>
            </el-col>
            <el-col :span="12" v-if="currentDevice && currentDevice.deviceType === 'NVR'">
              <h4 class="config-title">存储使用情况（NVR）</h4>
              <div v-loading="loadingNvr">
                <el-row :gutter="16" style="margin-bottom: 16px">
                  <el-col :span="12">
                    <div class="nvr-stat">
                      <span class="nvr-stat-value">{{ nvrInfo.totalSizeGB || '—' }}</span>
                      <span class="nvr-stat-unit">GB</span>
                      <div class="nvr-stat-label">总容量</div>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="nvr-stat">
                      <span class="nvr-stat-value">{{ nvrInfo.totalUsedGB || '—' }}</span>
                      <span class="nvr-stat-unit">GB</span>
                      <div class="nvr-stat-label">已用</div>
                    </div>
                  </el-col>
                </el-row>
                <el-table :data="nvrInfo.storageList" size="small"
                  v-if="nvrInfo.storageList && nvrInfo.storageList.length" stripe>
                  <el-table-column prop="description" label="存储卷" />
                  <el-table-column prop="totalKB" label="总大小 (KB)" />
                  <el-table-column prop="usedKB" label="已用 (KB)" />
                </el-table>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- ========== 未选择设备时的占位 ========== -->
    <div v-else class="no-device">
      <i class="el-icon-monitor"></i>
      <p>请在上方选择一个网络设备以查看 SNMP 监控信息</p>
    </div>

    <!-- ========== Grafana 驾驶舱弹窗 ========== -->
    <el-dialog title="SNMP 驾驶舱" :visible.sync="grafanaOpen" width="90%" top="5vh" :close-on-click-modal="false"
      append-to-body>
      <iframe :src="grafanaUrl" style="width: 100%; height: 75vh; border: none" />
    </el-dialog>
  </div>
</template>

<script>
import { listDevice } from '@/api/device/device'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

export default {
  name: 'DeviceSnmp',
  data() {
    return {
      deviceList: [],
      selectedDeviceId: null,
      currentDevice: null,
      activeTab: 'ports',
      loading: false,
      loadingPort: false,
      loadingNvr: false,
      loadingCustom: false,
      loadingWalk: false,
      snmpInfo: {},
      portList: [],
      nvrInfo: {},
      selectedMib: '1.3.6.1.2.1.1.5.0',
      showCustomOid: false,
      customOid: '',
      customResult: null,
      snmpError: '',
      portError: '',
      snmpMode: '',
      uploading: false,
      deploying: false,
      templateType: 'switch',
      grafanaOpen: false
    }
  },
  computed: {
    currentOid() {
      return this.selectedMib === '__custom__' ? this.customOid : this.selectedMib
    },
    walkTableData() {
      if (!this.customResult || typeof this.customResult === 'string') return []
      const baseOid = this.selectedMib === '__custom__' ? this.customOid : this.selectedMib
      return Object.entries(this.customResult).map(([oid, value]) => ({
        oid,
        oidName: this.resolveOidName(oid, baseOid),
        valueDesc: this.resolveValue(oid, value)
      }))
    },
    uploadUrl() {
      return process.env.VUE_APP_BASE_API + '/device/snmp/config/upload'
    },
    uploadData() {
      return { deviceId: this.selectedDeviceId, templateType: this.templateType }
    },
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    },
    grafanaUrl() {
      return 'http://192.168.1.60:3000/d-solo/snmp-dashboard?orgId=1&refresh=30s&var-device_ip=' +
        (this.currentDevice ? this.currentDevice.ipAddress : '') + '&theme=light&kiosk'
    }
  },
  created() {
    this.loadDevices()
    this.loadSnmpStatus()
  },
  methods: {
    // ── 配置上传 ──
    beforeConfigUpload(file) {
      const isToml = file.name.endsWith('.toml')
      if (!isToml) {
        this.$message.error('仅支持 .toml 文件')
        return false
      }
      return true
    },
    onConfigSuccess(res) {
      if (res.code === 200) {
        this.$message.success('配置文件已部署')
        this.loadDevices()
      } else {
        this.$message.error(res.msg || '部署失败')
      }
    },
    deployTemplate() {
      this.deploying = true
      this.$axios.post('/device/snmp/config/upload', null, {
        params: { deviceId: this.selectedDeviceId, templateType: this.templateType }
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('模板配置已生成并部署')
          this.loadDevices()
        } else {
          this.$message.error(res.msg || '部署失败')
        }
      }).finally(() => {
        this.deploying = false
      })
    },

    // ── 数据加载 ──
    loadDevices() {
      listDevice({ pageNum: 1, pageSize: 100 }).then(res => {
        this.deviceList = res.rows || []
      })
    },
    loadSnmpStatus() {
      request({ url: '/device/snmp/status', method: 'get' }).then(res => {
        if (res.code === 200 && res.data) {
          this.snmpMode = res.data.mode || ''
        }
      }).catch(() => {
        this.snmpMode = 'SNMP状态未知'
      })
    },
    loadSnmpInfo() {
      if (!this.selectedDeviceId) return
      this.currentDevice = this.deviceList.find(d => d.deviceId === this.selectedDeviceId)
      this.loading = true
      this.loadingPort = true
      this.customResult = null
      this.snmpError = ''
      this.portError = ''

      request({ url: '/device/snmp/info/' + this.selectedDeviceId, method: 'get' }).then(res => {
        if (res.code === 200) this.snmpInfo = res.data || res
        else this.snmpError = res.msg || '请求失败'
        this.loading = false
      }).catch(() => {
        this.snmpError = 'SNMP 请求超时 (8s)，请检查设备 SNMP 配置或网络连通性'
        this.loading = false
      })

      request({ url: '/device/snmp/ports/' + this.selectedDeviceId, method: 'get' }).then(res => {
        if (res.code === 200) this.portList = res.data || []
        else this.portError = res.msg || '查询失败'
        this.loadingPort = false
      }).catch(() => {
        this.portError = '端口查询超时'
        this.loadingPort = false
      })

      if (this.currentDevice && this.currentDevice.deviceType === 'NVR') {
        this.loadingNvr = true
        request({ url: '/device/snmp/nvrStorage/' + this.selectedDeviceId, method: 'get' }).then(res => {
          this.nvrInfo = res.data || res
          this.loadingNvr = false
        }).catch(() => {
          this.loadingNvr = false
        })
      }
    },

    // ── MIB 查询 ──
    onMibChange(val) {
      this.showCustomOid = val === '__custom__'
      this.customResult = null
    },
    executeCustom() {
      const oid = this.currentOid
      if (!oid) return
      this.loadingCustom = true
      request({
        url: '/device/snmp/get/' + this.selectedDeviceId,
        method: 'get',
        params: { oid }
      }).then(res => {
        if (res.code === 200) {
          const data = res.data || res
          this.customResult = data.value || '(null)'
        }
        this.loadingCustom = false
      }).catch(() => {
        this.loadingCustom = false
      })
    },
    executeWalk() {
      const oid = this.currentOid
      if (!oid) return
      this.loadingWalk = true
      request({
        url: '/device/snmp/walk/' + this.selectedDeviceId,
        method: 'get',
        params: { oid }
      }).then(res => {
        if (res.code === 200) this.customResult = res.data || {}
        this.loadingWalk = false
      }).catch(() => {
        this.loadingWalk = false
      })
    },

    // ── 工具方法（保持原逻辑不变）──
    oidNameMap: {
      '1.3.6.1.2.1.1.1.0': 'sysDescr', '1.3.6.1.2.1.1.2.0': 'sysObjectID',
      '1.3.6.1.2.1.1.3.0': 'sysUpTime', '1.3.6.1.2.1.1.4.0': 'sysContact',
      '1.3.6.1.2.1.1.5.0': 'sysName', '1.3.6.1.2.1.1.6.0': 'sysLocation',
      '1.3.6.1.2.1.2.1.0': 'ifNumber',
      '1.3.6.1.2.1.2.2.1.1': 'ifIndex', '1.3.6.1.2.1.2.2.1.2': 'ifDescr',
      '1.3.6.1.2.1.2.2.1.3': 'ifType', '1.3.6.1.2.1.2.2.1.4': 'ifMtu',
      '1.3.6.1.2.1.2.2.1.5': 'ifSpeed', '1.3.6.1.2.1.2.2.1.6': 'ifPhysAddress',
      '1.3.6.1.2.1.2.2.1.7': 'ifAdminStatus', '1.3.6.1.2.1.2.2.1.8': 'ifOperStatus',
      '1.3.6.1.2.1.4.1.0': 'ipForwarding', '1.3.6.1.2.1.4.20': 'ipAddrTable',
      '1.3.6.1.2.1.5.1.0': 'icmpInMsgs', '1.3.6.1.2.1.6.13': 'tcpConnTable',
      '1.3.6.1.2.1.7.5': 'udpTable', '1.3.6.1.2.1.11.1.0': 'snmpInPkts',
      '1.3.6.1.2.1.25.2.3': 'hrStorageTable',
      '1.3.6.1.2.1.25.2.3.1.1': 'hrStorageIndex',
      '1.3.6.1.2.1.25.2.3.1.2': 'hrStorageType',
      '1.3.6.1.2.1.25.2.3.1.3': 'hrStorageDescr',
      '1.3.6.1.2.1.25.2.3.1.4': 'hrStorageAllocationUnits',
      '1.3.6.1.2.1.25.2.3.1.5': 'hrStorageSize',
      '1.3.6.1.2.1.25.2.3.1.6': 'hrStorageUsed',
      '1.3.6.1.2.1.1': 'system', '1.3.6.1.2.1.2': 'interfaces',
      '1.3.6.1.2.1.4': 'ip', '1.3.6.1.2.1.25': 'hostResources'
    },
    ifTypeNames: {
      1: 'other', 2: 'regular1822', 3: 'hdh1822', 4: 'ddnX25', 5: 'rfc877x25',
      6: 'ethernetCsmacd(以太网)', 7: 'iso88023Csmacd', 8: 'iso88024TokenBus',
      9: 'iso88025TokenRing', 10: 'iso88026Man', 11: 'starLan', 12: 'proteon10Mbit',
      13: 'proteon80Mbit', 14: 'hyperchannel', 15: 'fddi', 16: 'lapb', 17: 'sdlc',
      18: 'ds1', 19: 'e1', 20: 'basicISDN', 21: 'primaryISDN', 22: 'propPointToPointSerial',
      23: 'ppp', 24: 'softwareLoopback(回环)', 25: 'eon', 26: 'ethernet3Mbit',
      27: 'nsip', 28: 'slip', 29: 'ultra', 30: 'ds3', 31: 'sip', 32: 'frameRelay',
      33: 'rs232', 34: 'para', 35: 'arcnet', 36: 'arcnetPlus', 37: 'atm',
      38: 'miox25', 39: 'sonet', 40: 'x25ple', 41: 'iso88022llc', 42: 'localTalk',
      43: 'smdsDxi', 44: 'frameRelayService', 45: 'v35', 46: 'hssi', 47: 'hippi',
      48: 'modem', 49: 'aal5', 50: 'sonetPath', 51: 'sonetVT', 52: 'smdsIcip',
      53: 'propVirtual(虚拟接口)', 54: 'propMultiplexor', 55: 'ieee80212', 56: 'fibreChannel',
      57: 'hippiInterface', 58: 'frameRelayInterconnect', 59: 'aflane8023',
      60: 'aflane8025', 61: 'cctEmul', 62: 'fastEther(快速以太网)', 63: 'isdn',
      64: 'v11', 65: 'v36', 66: 'g703at64k', 67: 'g703at2mb', 68: 'qllc',
      69: 'fastEtherFX', 70: 'channel', 71: 'ieee80211', 72: 'ibm370parChan',
      73: 'escon', 74: 'dlsw', 75: 'isdns', 76: 'isdnu', 77: 'lapd',
      78: 'ipSwitch', 79: 'rsrb', 80: 'atmLogical', 81: 'ds0', 82: 'ds0Bundle',
      83: 'bsc', 84: 'async', 85: 'cnr', 86: 'iso88025Dtr', 87: 'eplrs',
      88: 'arap', 89: 'propCnls', 90: 'hostPad', 91: 'termPad', 92: 'frameRelayMPI',
      93: 'x213', 94: 'adsl', 95: 'radsl', 96: 'sdsl', 97: 'vdsl',
      98: 'iso88025CRFPInt', 99: 'myrinet', 100: 'voiceEM', 101: 'voiceFXO',
      102: 'voiceFXS', 103: 'voiceEncap', 104: 'voiceOverIp', 105: 'atmDxi',
      106: 'atmFuni', 107: 'atmIma', 108: 'pppMultilinkBundle', 109: 'ipOverCdlc',
      110: 'ipOverClaw', 111: 'stackToStack', 112: 'virtualIpAddress',
      113: 'mpc', 114: 'ipOverAtm', 115: 'iso88025Fiber', 116: 'tdlc',
      117: 'gigabitEthernet(千兆以太网)', 118: 'hdlc', 119: 'lapf',
      120: 'v37', 121: 'x25mlp', 122: 'x25huntGroup', 123: 'transpHdlc',
      124: 'interleave', 125: 'fast', 126: 'ip', 127: 'docsCableMaclayer',
      128: 'docsCableDownstream', 129: 'docsCableUpstream', 130: 'a12Mpp',
      131: 'tunnel(隧道)', 132: 'coffee', 133: 'ces', 134: 'atmSubInterface',
      135: 'l2vlan(VLAN)', 136: 'l3ipvlan', 137: 'l3ipxvlan', 138: 'digitalPowerline',
      139: 'mediaMailOverIp', 140: 'dtm', 141: 'dcn', 142: 'ipForward',
      143: 'msdsl', 144: 'ieee1394', 145: 'if-gsn', 146: 'dvbRccMacLayer',
      147: 'dvbRccDownstream', 148: 'dvbRccUpstream', 149: 'atmVirtual',
      150: 'mplsTunnel', 151: 'srp', 152: 'voiceOverAtm', 153: 'voiceOverFrameRelay',
      154: 'idsl', 155: 'compositeLink', 156: 'ss7SigLink', 157: 'propWirelessP2P',
      158: 'frForward', 159: 'rfc1483', 160: 'usb', 161: 'ieee8023adLag',
      162: 'bgpPolicyAccounting', 163: 'frf16MfrBundle', 164: 'h323Gatekeeper',
      165: 'h323Proxy', 166: 'mpls', 167: 'mfSigLink', 168: 'hdsl2',
      169: 'shdsl', 170: 'ds1FDL', 171: 'pos', 172: 'dvbAsiIn',
      173: 'dvbAsiOut', 174: 'plc', 175: 'nfas', 176: 'tr008',
      177: 'gr303RDT', 178: 'gr303IDT', 179: 'isup', 180: 'propDocsWirelessMaclayer',
      181: 'propDocsWirelessDownstream', 182: 'propDocsWirelessUpstream',
      183: 'hiperlan2', 184: 'propBWAp2Mp', 185: 'sonetOverheadChannel',
      186: 'digitalWrapperOverheadChannel', 187: 'aal2', 188: 'radioMAC',
      189: 'atmRadio', 190: 'imt', 191: 'mvl', 192: 'reachDSL',
      193: 'frDlciEndPt', 194: 'atmVciEndPt', 195: 'opticalChannel',
      196: 'opticalTransport', 197: 'propAtm', 198: 'voiceOverCable',
      199: 'infiniband', 200: 'teLink', 201: 'q2931', 202: 'virtualTg',
      203: 'sipTg', 204: 'sipSig', 205: 'docsCableUpstreamChannel',
      206: 'econet', 207: 'pon155', 208: 'pon622', 209: 'bridge(桥接)',
      210: 'linegroup', 211: 'voiceEMFGD', 212: 'voiceFGDEANA',
      213: 'voiceDID', 214: 'mpegTransport', 215: 'sixToFour',
      216: 'gtp', 217: 'pdnEtherLoop1', 218: 'pdnEtherLoop2',
      219: 'opticalChannelGroup', 220: 'homepna', 221: 'gfp',
      222: 'ciscoISLvlan', 223: 'actelisMetaLOOP', 224: 'fcipLink',
      225: 'rpr', 226: 'qam', 227: 'lmp', 228: 'cblVectaStar',
      229: 'docsCableMCmtsDownstream', 230: 'adsl2', 231: 'macSecControlledIF',
      232: 'macSecUncontrolledIF', 233: 'aviciOpticalEther',
      234: 'atmbond', 235: 'voiceFGDOS', 236: 'mocaVersion1',
      237: 'ieee80216WMAN', 238: 'adsl2plus', 239: 'dvbRcsMacLayer',
      240: 'dvbTdm', 241: 'dvbRcsTdma', 242: 'x86Laps',
      243: 'wwanPP', 244: 'wwanPP2', 245: 'voiceEBS',
      246: 'ifPwType', 247: 'ilan', 248: 'pip', 249: 'aluELP',
      250: 'gpon', 251: 'vdsl2', 252: 'capwapDot11Profile',
      253: 'capwapDot11Bss', 254: 'capwapWtpVirtualRadio',
      255: 'bits', 256: 'docsCableUpstreamRfPort',
      257: 'cableDownstreamRfPort', 258: 'vmwareVirtualNic',
      259: 'ieee802154', 260: 'otnOdu', 261: 'otnOtu',
      262: 'ifVfiType', 263: 'g9981', 264: 'g9982', 265: 'g9983',
      266: 'aluEpon', 267: 'aluEponOnu', 268: 'aluEponPhysicalUni',
      269: 'aluEponLogicalLink', 270: 'aluGponOnu', 271: 'aluGponPhysicalUni',
      272: 'aluGponLogicalLink', 273: 'ieee8021AE2964'
    },
    resolveOidName(oid, baseOid) {
      if (this.oidNameMap[oid]) return this.oidNameMap[oid]
      const suffix = oid.substring(oid.lastIndexOf('.') + 1)
      const baseName = this.oidNameMap[baseOid] || baseOid.substring(baseOid.lastIndexOf('.') + 1)
      const prefix = oid.substring(0, oid.lastIndexOf('.'))
      const prefixName = this.oidNameMap[prefix]
      if (prefixName) return prefixName + '.' + suffix
      return baseName + '.' + suffix
    },
    resolveValue(oid, value) {
      if (!value) return '-'
      if (oid.indexOf('1.3.6.1.2.1.2.2.1.3') === 0 || oid.indexOf('.1.3.6.1.2.1.2.2.1.3') === 0) {
        const typeName = this.ifTypeNames[parseInt(value)] || ('Type-' + value)
        return value + ' (' + typeName + ')'
      }
      if (oid.indexOf('1.3.6.1.2.1.2.2.1.8') === 0 || oid.indexOf('.1.3.6.1.2.1.2.2.1.8') === 0 ||
        oid.indexOf('1.3.6.1.2.1.2.2.1.7') === 0 || oid.indexOf('.1.3.6.1.2.1.2.2.1.7') === 0) {
        const statusMap = { 1: 'up(启用)', 2: 'down(停用)', 3: 'testing(测试)' }
        return value + ' (' + (statusMap[parseInt(value)] || value) + ')'
      }
      if (oid.indexOf('1.3.6.1.2.1.2.2.1.5') === 0 || oid.indexOf('.1.3.6.1.2.1.2.2.1.5') === 0) {
        const bps = parseInt(value)
        if (bps >= 1e9) return value + ' (' + (bps / 1e9).toFixed(1) + ' Gbps)'
        if (bps >= 1e6) return value + ' (' + Math.round(bps / 1e6) + ' Mbps)'
        if (bps >= 1e3) return value + ' (' + Math.round(bps / 1e3) + ' Kbps)'
        return value + ' (' + bps + ' bps)'
      }
      if (oid.indexOf('1.3.6.1.2.1.1.3.0') === 0 || oid.indexOf('.1.3.6.1.2.1.1.3.0') === 0) {
        try {
          const match = value.match(/\((\d+)\)/)
          if (match) {
            const sec = parseInt(match[1]) / 100
            const d = Math.floor(sec / 86400)
            const h = Math.floor((sec % 86400) / 3600)
            const m = Math.floor((sec % 3600) / 60)
            return value + ' (' + d + '天' + h + '时' + m + '分)'
          }
        } catch (e) { /* ignore */ }
      }
      return value
    }
  }
}
</script>

<style scoped>
/* ── 容器 ── */
.snmp-container {
  padding: 0;
}

/* ── 顶部工具栏 ── */
.snmp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.snmp-mode-pending {
  color: #909399;
  font-size: 12px;
}

/* ── 统计概览卡片 ── */
.snmp-stats {
  margin-bottom: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 4px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  min-height: 64px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Tab 面板 ── */
.snmp-tabs {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.snmp-tabs>>>.el-tabs__content {
  padding: 16px 20px;
}

/* ── 空状态、占位 ── */
.empty-state,
.mib-placeholder,
.no-device {
  text-align: center;
  padding: 48px 0;
  color: #c0c4cc;
}

.empty-state i,
.mib-placeholder i,
.no-device i {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.empty-state p,
.mib-placeholder p,
.no-device p {
  font-size: 14px;
  margin: 0;
  color: #909399;
}

/* ── MIB 查询 ── */
.mib-query-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
  margin-bottom: 12px;
}

.mib-result {
  background: #fafafa;
  border-radius: 4px;
  padding: 12px 16px;
}

.mib-result-header {
  margin-bottom: 8px;
}

.mib-result-single {
  font-size: 14px;
  padding: 8px 0;
}

.mib-result-label {
  color: #909399;
  margin-right: 8px;
}

.mib-result-single code {
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

/* ── 配置管理 ── */
.config-section {
  background: #fafafa;
  border-radius: 4px;
  padding: 16px 20px;
  min-height: 80px;
}

.config-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.config-template-row {
  display: flex;
  align-items: center;
}

.config-deployed {
  margin-top: 16px;
  padding: 10px 16px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  font-size: 13px;
  color: #67c23a;
}

.config-deployed i {
  margin-right: 6px;
}

.config-md5 {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

/* ── NVR 存储统计 ── */
.nvr-stat {
  text-align: center;
  background: #fafafa;
  border-radius: 4px;
  padding: 16px;
}

.nvr-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.nvr-stat-unit {
  font-size: 13px;
  color: #909399;
  margin-left: 2px;
}

.nvr-stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .snmp-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-left {
    width: 100%;
  }

  .toolbar-left .el-select {
    width: 100% !important;
  }

  .stat-card {
    margin-bottom: 8px;
  }
}
</style>
