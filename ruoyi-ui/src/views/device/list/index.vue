<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="queryParams.deviceName"
          placeholder="请输入设备名称"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select
          v-model="queryParams.deviceType"
          placeholder="请选择设备类型"
          clearable
          style="width: 200px"
        >
          <el-option label="网络设备" value="NETWORK" />
          <el-option label="监控设备" value="MONITOR" />
          <el-option label="硬盘录像机" value="NVR" />
          <el-option label="其他设备" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 200px"
        >
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['device:device:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['device:device:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['device:device:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['device:device:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备名称" align="center" prop="deviceName" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <a class="link-type" style="cursor:pointer" @click="handleView(scope.row)">{{ scope.row.deviceName }}</a>
        </template>
      </el-table-column>
      <el-table-column label="设备类型" align="center" prop="deviceType" width="120">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.deviceType === 'NETWORK'" type="primary">{{ scope.row.deviceType }}</el-tag>
          <el-tag v-else-if="scope.row.deviceType === 'MONITOR'" type="warning">{{ scope.row.deviceType }}</el-tag>
          <el-tag v-else>{{ scope.row.deviceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="IP地址" align="center" prop="ipAddress" width="150" />
      <el-table-column label="型号" align="center" prop="model" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="位置" align="center" prop="location" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === 'ONLINE'" type="success">{{ scope.row.status }}</el-tag>
          <el-tag v-else-if="scope.row.status === 'OFFLINE'" type="danger">{{ scope.row.status }}</el-tag>
          <el-tag v-else type="info">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="responsible" width="120" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['device:device:edit']"
          >修改</el-button>
          <el-button
            v-if="scope.row.status === 'OFFLINE'"
            size="mini"
            type="text"
            icon="el-icon-s-tools"
            style="color: #e6a23c"
            @click="handleRepair(scope.row)"
            v-hasPermi="['device:device:repair']"
          >报修</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['device:device:remove']"
          >删除</el-button>
          <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)">
            <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="handleViewPorts" icon="el-icon-connection">查看端口</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改设备对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="请选择设备类型" style="width: 100%">
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
              <el-input v-model="form.ipAddress" placeholder="请输入IP地址" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model="form.model" placeholder="请输入型号" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入位置" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="巡检周期" prop="pingInterval">
              <el-select v-model="form.pingInterval" placeholder="请选择巡检周期" style="width: 100%">
                <el-option label="1分钟" :value="1" />
                <el-option label="2分钟" :value="2" />
                <el-option label="3分钟" :value="3" />
                <el-option label="4分钟" :value="4" />
                <el-option label="5分钟" :value="5" />
                <el-option label="6分钟" :value="6" />
                <el-option label="7分钟" :value="7" />
                <el-option label="8分钟" :value="8" />
                <el-option label="9分钟" :value="9" />
                <el-option label="10分钟" :value="10" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="负责人" prop="responsible">
              <el-input v-model="form.responsible" placeholder="请输入负责人" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="responsiblePhone">
              <el-input v-model="form.responsiblePhone" placeholder="请输入联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="SNMP团体名" prop="snmpCommunity">
              <el-input v-model="form.snmpCommunity" placeholder="SNMP读团体名/社区名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SNMP端口" prop="snmpPort">
              <el-input-number v-model="form.snmpPort" :min="1" :max="65535" placeholder="默认161" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="SNMP版本" prop="snmpVersion">
              <el-select v-model="form.snmpVersion" placeholder="请选择SNMP版本" style="width: 100%">
                <el-option label="v2c (社区认证)" value="v2c" />
                <el-option label="v3 (用户认证)" value="v3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 设备端口对话框 -->
    <el-dialog :title="portDialogTitle" :visible.sync="portOpen" width="800px" append-to-body :close-on-click-modal="false">
      <el-table v-loading="portLoading" :data="portList">
        <el-table-column label="端口名称" align="center" prop="portName" />
        <el-table-column label="端口号" align="center" prop="portNumber" />
        <el-table-column label="协议" align="center" prop="protocol" />
        <el-table-column label="状态" align="center" prop="status">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === '0'" type="success">启用</el-tag>
            <el-tag v-else type="danger">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      </el-table>
      <pagination
        v-show="portTotal > 0"
        :total="portTotal"
        :page.sync="portQueryParams.pageNum"
        :limit.sync="portQueryParams.pageSize"
        @pagination="getPortList"
      />
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
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 设备表格数据
      deviceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deviceName: undefined,
        deviceType: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
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
    /** 查询设备列表 */
    getList() {
      this.loading = true
      listDevice(this.queryParams).then(response => {
        this.deviceList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
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
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case "handleViewPorts":
          this.handleViewPorts(row)
          break
        default:
          break
      }
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加设备"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const deviceId = row.deviceId || this.ids
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
      const deviceId = row.deviceId
      getDevice(deviceId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "查看设备"
        // 只读模式下禁用表单验证和提交
        this.disableSubmit = true
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
