<template>
  <div class="device-list-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="queryParams.deviceName" placeholder="请输入设备名称" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="queryParams.deviceType" placeholder="请选择设备类型" clearable style="width: 200px">
          <el-option label="网络设备" value="NETWORK" />
          <el-option label="监控设备" value="MONITOR" />
          <el-option label="硬盘录像机" value="NVR" />
          <el-option label="其他设备" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="在线" value="ONLINE" />
          <el-option label="离线" value="OFFLINE" />
          <el-option label="未知" value="UNKNOWN" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 统计概览 -->
    <el-row :gutter="16" class="device-stats" v-if="stats.total > 0">
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-total" @click="resetQuery">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">设备总数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-online">
          <div class="stat-num">{{ stats.online }}</div>
          <div class="stat-label">在线设备</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-offline">
          <div class="stat-num">{{ stats.offline }}</div>
          <div class="stat-label">离线设备</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-unknown">
          <div class="stat-num">{{ stats.unknown }}</div>
          <div class="stat-label">状态未知</div>
        </div>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['device:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['device:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['device:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['device:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-data-line" size="mini" @click="openGrafanaOverview">监控大盘</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备名称" align="center" prop="deviceName" width="200" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <a class="link-type" @click="handleView(scope.row)">{{ scope.row.deviceName }}</a>
        </template>
      </el-table-column>
      <el-table-column label="设备类型" align="center" prop="deviceType" width="200">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.deviceType === 'NETWORK'" type="primary" size="small">网络设备</el-tag>
          <el-tag v-else-if="scope.row.deviceType === 'MONITOR'" type="warning" size="small">监控设备</el-tag>
          <el-tag v-else-if="scope.row.deviceType === 'NVR'" type="success" size="small">硬盘录像机</el-tag>
          <el-tag v-else size="small" type="info">{{ scope.row.deviceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="IP地址" align="center" prop="ipAddress" width="200" />
      <el-table-column label="型号" align="center" prop="model" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="位置" align="center" prop="location" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="200">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === 'ONLINE'" type="success" size="small">在线</el-tag>
          <el-tag v-else-if="scope.row.status === 'OFFLINE'" type="danger" size="small">离线</el-tag>
          <el-tag v-else type="info" size="small">未知</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="responsible" width="200" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="200">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="500"  fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['device:edit']">修改</el-button>
          <el-button v-if="scope.row.status === 'OFFLINE'" size="mini" type="text" icon="el-icon-s-tools" style="color: #e6a23c" @click="handleRepair(scope.row)" v-hasPermi="['device:repair']">报修</el-button>
          <el-button size="mini" type="text" icon="el-icon-connection" @click="handleViewPorts(scope.row)">端口</el-button>
          <el-button size="mini" type="text" icon="el-icon-monitor" style="color: #00a8cc" @click="openGrafana(scope.row)">监控</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['device:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加/修改/查看设备对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="isViewMode ? {} : rules" label-width="90px" size="small">
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" maxlength="50" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="请选择设备类型" style="width: 100%" :disabled="isViewMode">
                <el-option label="网络设备" value="NETWORK" />
                <el-option label="监控设备" value="MONITOR" />
                <el-option label="硬盘录像机" value="NVR" />
                <el-option label="其他设备" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="IP地址" prop="ipAddress">
              <el-input v-model="form.ipAddress" placeholder="请输入IP地址" maxlength="50" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model="form.model" placeholder="请输入型号" maxlength="50" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入位置" maxlength="100" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="巡检周期" prop="pingInterval">
              <el-select v-model="form.pingInterval" placeholder="请选择巡检周期" style="width: 100%" :disabled="isViewMode">
                <el-option label="1分钟" :value="1" />
                <el-option label="2分钟" :value="2" />
                <el-option label="3分钟" :value="3" />
                <el-option label="5分钟" :value="5" />
                <el-option label="10分钟" :value="10" />
                <el-option label="15分钟" :value="15" />
                <el-option label="30分钟" :value="30" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="负责人" prop="responsible">
              <el-input v-model="form.responsible" placeholder="请输入负责人" maxlength="30" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="responsiblePhone">
              <el-input v-model="form.responsiblePhone" placeholder="请输入联系电话" maxlength="20" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="SNMP团体名" prop="snmpCommunity">
              <el-input v-model="form.snmpCommunity" placeholder="SNMP读团体名/社区名" maxlength="30" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SNMP端口" prop="snmpPort">
              <el-input-number v-model="form.snmpPort" :min="1" :max="65535" placeholder="默认161" controls-position="right" style="width: 100%" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="SNMP版本" prop="snmpVersion">
              <el-select v-model="form.snmpVersion" placeholder="请选择SNMP版本" style="width: 100%" :disabled="isViewMode">
                <el-option label="v2c (社区认证)" value="v2c" />
                <el-option label="v3 (用户认证)" value="v3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备图标" prop="icon">
              <el-select v-model="form.icon" placeholder="请选择设备图标" clearable style="width: 100%" :disabled="isViewMode">
                <el-option v-for="opt in iconOptions" :key="opt.value" :label="opt.label" :value="opt.value">
                  <span style="display: inline-flex; align-items: center; gap: 6px;">
                    <img :src="require('@/assets/topo-icons/' + opt.value + '.svg')" alt="" style="width: 20px; height: 20px;" />
                    <span>{{ opt.label }}</span>
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父设备" prop="parentId">
              <el-select v-model="form.parentId" placeholder="无（作为顶级设备）" clearable filterable style="width: 100%" :disabled="isViewMode">
                <el-option v-for="opt in parentDeviceOptions" :key="opt.deviceId" :label="opt.deviceName" :value="opt.deviceId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="线缆类型" prop="cableType">
              <el-select v-model="form.cableType" placeholder="请选择连接线缆类型" clearable style="width: 100%" :disabled="isViewMode">
                <el-option v-for="opt in cableOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="!isViewMode" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">{{ isViewMode ? '关 闭' : '取 消' }}</el-button>
      </div>
    </el-dialog>

    <!-- 设备端口对话框 -->
    <el-dialog :title="portDialogTitle" :visible.sync="portOpen" width="800px" append-to-body :close-on-click-modal="false">
      <el-table v-loading="portLoading" :data="portList" stripe>
        <el-table-column label="端口名称" align="center" prop="portName" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="端口号" align="center" prop="portNumber" width="100" />
        <el-table-column label="协议" align="center" prop="protocol" width="100" />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === '0'" type="success" size="small">启用</el-tag>
            <el-tag v-else type="danger" size="small">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述" align="center" prop="description" min-width="160" :show-overflow-tooltip="true" />
      </el-table>
      <pagination v-show="portTotal > 0" :total="portTotal" :page.sync="portQueryParams.pageNum" :limit.sync="portQueryParams.pageSize" @pagination="getPortList" />
    </el-dialog>
  </div>
</template>

<script>
import { listDevice, getDevice, delDevice, addDevice, updateDevice, exportDevice, listDevicePort } from "@/api/device/device"
import { createRepair } from "@/api/device/repair"

export default {
  name: "Device",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      deviceList: [],
      title: "",
      open: false,
      isViewMode: false,
      stats: { total: 0, online: 0, offline: 0, unknown: 0 },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deviceName: undefined,
        deviceType: undefined,
        status: undefined
      },
      form: {},
      rules: {
        deviceName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ],
        deviceType: [
          { required: true, message: "设备类型不能为空", trigger: "change" }
        ],
        ipAddress: [
          { pattern: /^(\d{1,3}\.){3}\d{1,3}$/, message: "请输入正确的IP地址", trigger: "blur" }
        ],
        pingInterval: [
          { required: true, message: "巡检周期不能为空", trigger: "change" }
        ]
      },
      // 拓扑图标选项（对应 src/assets/topo-icons/ 下 SVG 文件）
      iconOptions: [
        { value: "router", label: "路由器" },
        { value: "switch", label: "交换机" },
        { value: "firewall", label: "防火墙" },
        { value: "ap", label: "无线AP" },
        { value: "server", label: "服务器" },
        { value: "camera", label: "摄像头" },
        { value: "nvr", label: "硬盘录像机" },
        { value: "other", label: "其他" }
      ],
      // 拓扑线缆类型（连接父设备与子设备的线缆样式）
      cableOptions: [
        { value: "fiber", label: "光纤" },
        { value: "ethernet", label: "双绞线" },
        { value: "wireless", label: "无线" },
        { value: "coaxial", label: "同轴" }
      ],
      // 父设备候选列表
      parentDeviceOptions: [],
      // 端口弹窗
      portOpen: false,
      portDialogTitle: "",
      portLoading: false,
      portList: [],
      portTotal: 0,
      currentDeviceId: undefined,
      portQueryParams: {
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 跳转单设备 Grafana 监控看板（新标签页） */
    openGrafana(row) {
      if (!row.ipAddress) {
        this.$modal.msgWarning('该设备无IP地址，无法查看监控')
        return
      }
      const url = `http://115.159.194.149:3000/d/switch-categraf-huawei-h3c/b203be1?orgId=1&from=now-6h&to=now&timezone=browser&var-instance=${row.ipAddress}&refresh=10s`
      window.open(url, '_blank')
    },
    /** 跳转监控大盘 Grafana 总览看板（新标签页） */
    openGrafanaOverview() {
      window.open('http://115.159.194.149:3000/d/switch-categraf-huawei-h3c/b203be1?orgId=1&from=now-6h&to=now&timezone=browser&var-instance=192.168.1.32&refresh=10s', '_blank')
    },
    /** 查询设备列表 */
    getList() {
      this.loading = true
      listDevice(this.queryParams).then(response => {
        this.deviceList = response.rows
        this.total = response.total
        this.loading = false
        this.loadStats()
      })
    },
    /** 加载统计 */
    loadStats() {
      listDevice({ pageNum: 1, pageSize: 1 }).then(res => {
        this.stats.total = res.total || 0
      })
      listDevice({ pageNum: 1, pageSize: 1, status: 'ONLINE' }).then(res => {
        this.stats.online = res.total || 0
      })
      listDevice({ pageNum: 1, pageSize: 1, status: 'OFFLINE' }).then(res => {
        this.stats.offline = res.total || 0
      })
      listDevice({ pageNum: 1, pageSize: 1, status: 'UNKNOWN' }).then(res => {
        this.stats.unknown = res.total || 0
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.isViewMode = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        deviceId: undefined,
        deviceName: undefined,
        deviceType: undefined,
        ipAddress: undefined,
        model: undefined,
        location: undefined,
        pingInterval: 5,
        responsible: undefined,
        responsiblePhone: undefined,
        snmpCommunity: undefined,
        snmpPort: 161,
        snmpVersion: 'v2c',
        parentId: undefined,
        icon: undefined,
        cableType: undefined,
        remark: undefined
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.deviceId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 加载父设备候选列表（排除自身，避免自引用成环） */
    loadParentDevices(excludeId) {
      listDevice({ pageNum: 1, pageSize: 1000 }).then(response => {
        let list = response.rows || []
        if (excludeId !== undefined && excludeId !== null) {
          list = list.filter(item => item.deviceId !== excludeId)
        }
        this.parentDeviceOptions = list
      })
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.isViewMode = false
      this.loadParentDevices()
      this.open = true
      this.title = "添加设备"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      this.isViewMode = false
      const deviceId = row.deviceId || this.ids
      this.loadParentDevices(deviceId)
      getDevice(deviceId).then(response => {
        this.form = response.data
        // 设备状态由定时任务同步，不从表单提交
        delete this.form.status
        this.open = true
        this.title = "修改设备"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 设备状态由定时任务同步，不通过表单提交
          delete this.form.status
          if (this.form.deviceId != undefined) {
            updateDevice(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDevice(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const deviceIds = row.deviceId || this.ids.join(",")
      this.$modal.confirm('是否确认删除设备编号为"' + deviceIds + '"的数据项？').then(function() {
        return delDevice(deviceIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('device/export', {
        ...this.queryParams
      }, `device_${new Date().getTime()}.xlsx`)
    },
    /** 查看设备详情 */
    handleView(row) {
      this.reset()
      this.isViewMode = true
      const deviceId = row.deviceId
      getDevice(deviceId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "查看设备"
      })
    },
    /** 报修-创建维修工单并发送短信 */
    handleRepair(row) {
      this.$modal.confirm('确认为设备"' + row.deviceName + '"创建维修工单吗？系统将自动发送维修通知短信。').then(() => {
        return createRepair(row.deviceId)
      }).then(() => {
        this.$modal.msgSuccess("维修工单已创建，短信已发送")
      }).catch(() => {})
    },
    /** 查看端口 */
    handleViewPorts(row) {
      this.currentDeviceId = row.deviceId
      this.portDialogTitle = row.deviceName + " - 端口列表"
      this.portQueryParams.pageNum = 1
      this.portOpen = true
      this.getPortList()
    },
    /** 查询端口列表 */
    getPortList() {
      this.portLoading = true
      listDevicePort({
        deviceId: this.currentDeviceId,
        pageNum: this.portQueryParams.pageNum,
        pageSize: this.portQueryParams.pageSize
      }).then(response => {
        this.portList = response.rows
        this.portTotal = response.total
        this.portLoading = false
      })
    }
  }
}
</script>

<style scoped>
.device-list-container {
  padding: 0;
}

/* 统计卡片 */
.device-stats {
  margin-bottom: 12px;
}

.stat-card {
  padding: 16px 20px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-total .stat-num { color: #409eff; }
.stat-online .stat-num { color: #67c23a; }
.stat-offline .stat-num { color: #f56c6c; }
.stat-unknown .stat-num { color: #909399; }
</style>
