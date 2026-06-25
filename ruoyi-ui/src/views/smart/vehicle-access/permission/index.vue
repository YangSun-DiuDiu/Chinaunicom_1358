<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="query" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="授权目标" prop="target_name">
        <el-input v-model="query.target_name" placeholder="请输入名称" clearable style="width:180px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="车牌号" prop="vehicle_plate">
        <el-input v-model="query.vehicle_plate" placeholder="请输入车牌号" clearable style="width:140px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="授权类型" prop="target_type">
        <el-select v-model="query.target_type" placeholder="全部" clearable style="width:110px">
          <el-option label="部门" value="DEPT"/><el-option label="个人" value="PERSON"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
          <el-option label="启用" value="ENABLED"/><el-option label="禁用" value="DISABLED"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd">新增授权</el-button>
      <el-button icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
    </el-row>

    <!-- 数据表格 - 列宽平均分配 -->
    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column label="授权类型" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.target_type==='DEPT'?'success':''" size="small" effect="plain">
            {{ row.target_type==='DEPT'?'部门':'个人' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="授权目标" prop="target_name" align="center" show-overflow-tooltip/>
      <el-table-column label="车牌号" align="center">
        <template slot-scope="{row}">
          <el-tag v-if="row.vehicle_plate" size="small" effect="dark">{{ row.vehicle_plate }}</el-tag>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="通行设备" align="center">
        <template slot-scope="{row}">
          <el-tooltip v-if="row.device_ids" :content="row.device_ids" placement="top">
            <div class="device-tags">
              <el-tag v-for="(did, i) in (row.device_ids||'').split(',').slice(0,4)" :key="i" size="mini" type="info" effect="plain">{{ getDeviceName(did.trim()) }}</el-tag>
              <el-tag v-if="(row.device_ids||'').split(',').length>4" size="mini" effect="plain">+{{ (row.device_ids||'').split(',').length - 4 }}</el-tag>
            </div>
          </el-tooltip>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="生效开始" align="center">
        <template slot-scope="{row}"><span v-if="row.start_time">{{ parseTime(row.start_time) }}</span><span v-else style="color:#c0c4cc">-</span></template>
      </el-table-column>
      <el-table-column label="生效结束" align="center">
        <template slot-scope="{row}"><span v-if="row.end_time">{{ parseTime(row.end_time) }}</span><span v-else style="color:#c0c4cc">-</span></template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.status==='ENABLED'?'success':'danger'" size="small" effect="dark">
            {{ row.status==='ENABLED'?'启用':'禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" align="center" show-overflow-tooltip/>
      <el-table-column label="操作" align="center" width="80">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click="handleDel(row)"/>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>

    <!-- 新增授权弹窗 -->
    <el-dialog title="新增车辆通行权限" :visible.sync="open" width="700px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item label="授权类型" prop="target_type">
          <el-radio-group v-model="form.target_type" @change="onTypeChange">
            <el-radio label="DEPT">按部门</el-radio>
            <el-radio label="PERSON">按个人</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.target_type==='DEPT'" label="选择部门" prop="target_id">
          <el-select v-model="form.target_id" filterable placeholder="请选择部门" style="width:100%" @change="onDeptSelect">
            <el-option v-for="d in deptList" :key="d.deptId" :label="d.deptName" :value="d.deptId"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.target_type==='DEPT' && form.target_name" label="已选部门">
          <el-tag type="success" effect="dark">{{ form.target_name }}</el-tag>
          <span style="color:#909399;font-size:12px;margin-left:8px">该部门所有车辆自动获得通行权限</span>
        </el-form-item>
        <el-form-item v-if="form.target_type==='PERSON'" label="选择人员" prop="target_id">
          <el-select v-model="form.target_id" filterable placeholder="请选择人员" style="width:100%" @change="onPersonSelect">
            <el-option v-for="u in userList" :key="u.userId" :label="u.userName + ' (' + (u.dept?u.dept.deptName:'') + ')'" :value="u.userId"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.target_type==='PERSON'" label="车牌号">
          <el-input v-model="form.vehicle_plate" placeholder="请输入车牌号(选填)" maxlength="20" clearable/>
        </el-form-item>
        <el-form-item label="通行设备" prop="selectedDevices">
          <el-transfer v-model="form.selectedDevices" :data="deviceList" :titles="['可选设备','已选设备']" filterable style="text-align:left"/>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="dateRange" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="选填" maxlength="200" :rows="2"/>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit" :loading="submitting">确 定</el-button>
        <el-button @click="open=false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'

export default {
  name: 'VehicleAccessPermission',
  data() {
    return {
      loading: false, total: 0, list: [], open: false, submitting: false, dateRange: [],
      deptList: [], userList: [], deviceList: [], deviceMap: {},
      query: { pageNum: 1, pageSize: 10, target_name: undefined, target_type: undefined, vehicle_plate: undefined, status: undefined },
      form: { target_type: 'DEPT', target_id: null, target_name: '', vehicle_plate: '', selectedDevices: [], remark: '' },
      rules: {
        target_type: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
        target_id: [{ required: true, message: '请选择部门或人员', trigger: 'change' }],
        selectedDevices: [{ type: 'array', required: true, message: '请选择通行设备', trigger: 'change' }]
      }
    }
  },
  created() { this.getList(); this.loadDeptAndUser(); this.loadDeviceList() },
  methods: {
    parseTime,
    getList() {
      this.loading = true
      const params = { ...this.query }
      if (!params.target_name) delete params.target_name
      if (!params.target_type) delete params.target_type
      if (!params.vehicle_plate) delete params.vehicle_plate
      if (!params.status) delete params.status
      request({ url: '/smart/access/permission/vehicle/list', method: 'get', params }).then(r => {
        this.list = r.rows || []; this.total = r.total || 0; this.loading = false
      }).catch(() => { this.loading = false })
    },
    resetQuery() {
      this.query = { pageNum: 1, pageSize: 10, target_name: undefined, target_type: undefined, vehicle_plate: undefined, status: undefined }
      this.getList()
    },
    loadDeptAndUser() {
      request({ url: '/system/dept/list' }).then(r => { this.deptList = r.data || [] })
      request({ url: '/system/user/list', params: { pageSize: 500 } }).then(r => { this.userList = r.rows || [] })
    },
    loadDeviceList() {
      request({ url: '/smart/access/vehicle-device/list', params: { pageSize: 200 } }).then(r => {
        const rows = r.rows || []
        this.deviceMap = {}
        rows.forEach(d => { this.deviceMap[String(d.device_id)] = d.device_name })
        this.deviceList = rows.map(d => ({ key: d.device_id, label: d.device_name + ' (' + d.location + ')' }))
      })
    },
    getDeviceName(id) {
      return this.deviceMap[id] || ('设备#' + id)
    },
    onTypeChange() { this.form.target_id = null; this.form.target_name = '' },
    onDeptSelect(val) { const d = this.deptList.find(d => d.deptId === val); this.form.target_name = d ? d.deptName : '' },
    onPersonSelect(val) { const u = this.userList.find(u => u.userId === val); this.form.target_name = u ? u.userName : '' },
    openAdd() { this.form = { target_type: 'DEPT', target_id: null, target_name: '', vehicle_plate: '', selectedDevices: [], remark: '' }; this.dateRange = []; this.open = true; this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() }) },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        this.submitting = true
        request({ url: '/smart/access/permission/vehicle', method: 'post', data: {
          target_type: this.form.target_type, target_id: this.form.target_id,
          target_name: this.form.target_name, vehicle_plate: this.form.vehicle_plate || null,
          device_ids: (this.form.selectedDevices || []).join(','),
          start_time: this.dateRange[0] || null, end_time: this.dateRange[1] || null,
          status: 'ENABLED', remark: this.form.remark
        } }).then(() => { this.$message.success('授权成功'); this.open = false; this.getList() }).finally(() => { this.submitting = false })
      })
    },
    handleDel(row) {
      const name = row.vehicle_plate ? (row.target_name + '(' + row.vehicle_plate + ')') : row.target_name
      this.$confirm('确认取消对【' + name + '】的车辆通行授权吗？', '提示', { type: 'warning' }).then(() => {
        request({ url: '/smart/access/permission/vehicle/' + row.perm_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      }).catch(() => {})
    }
  }
}
</script>
<style scoped>
.device-tags { display: flex; flex-wrap: wrap; justify-content: center; gap: 2px; }
.el-transfer { display: flex; flex-direction: row; justify-content: center; flex-wrap: nowrap; }
.el-transfer .el-transfer-panel { flex: 0 0 auto; width: 220px; }
.el-transfer .el-transfer__buttons { flex: 0 0 auto; padding: 0 12px; display: flex; flex-direction: column; justify-content: center; }
</style>
