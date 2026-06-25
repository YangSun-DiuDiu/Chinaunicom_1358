<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备名称"><el-input v-model="query.deviceName" placeholder="设备名称" clearable @keyup.enter.native="getList" style="width:180px"/></el-form-item>
      <el-form-item label="设备品牌">
        <el-select v-model="query.deviceBrand" placeholder="品牌" clearable style="width:140px">
          <el-option label="海康威视" value="HIKVISION"/><el-option label="大华" value="DAHUA"/><el-option label="其他" value="OTHER"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['smart:personAccess:add']">新增</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="设备名称" prop="device_name" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="品牌" prop="device_brand" min-width="90"><template slot-scope="{row}"><el-tag size="small">{{row.device_brand||'--'}}</el-tag></template></el-table-column>
      <el-table-column label="IP地址" prop="ip_address" min-width="130"/>
      <el-table-column label="端口" prop="port" min-width="70"/>
      <el-table-column label="位置" prop="location" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="操作" min-width="220" fixed="right">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['smart:personAccess:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-connection" @click="testDevice(row)">测试</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['smart:personAccess:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <!-- 测试结果弹窗 -->
    <el-dialog title="设备连接测试" :visible.sync="testOpen" width="550px" append-to-body>
      <div v-if="testLoading" style="text-align:center;padding:30px"><i class="el-icon-loading" style="font-size:32px"/> <p>正在检测...</p></div>
      <div v-else-if="testResult">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="设备IP">{{ testResult.ip }}</el-descriptions-item>
          <el-descriptions-item label="端口">{{ testResult.port }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ testResult.brand }}</el-descriptions-item>
          <el-descriptions-item label="TCP连通">
            <el-tag :type="testResult.tcpConnect?'success':'danger'">{{ testResult.tcpConnect?'已连通':'无法连接' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="延迟" v-if="testResult.latency">{{ testResult.latency }}</el-descriptions-item>
          <el-descriptions-item label="错误" v-if="testResult.error"><span style="color:red;font-size:12px">{{ testResult.error }}</span></el-descriptions-item>
          <el-descriptions-item label="SDK类型" :span="2" v-if="testResult.sdkType">{{ testResult.sdkType }}</el-descriptions-item>
          <el-descriptions-item label="HTTP接口" :span="2" v-if="testResult.httpApi"><a :href="'http://'+testResult.ip+(testResult.port===80?'':':'+testResult.port)" target="_blank" style="color:#1890ff">{{ testResult.httpApi }}</a></el-descriptions-item>
          <el-descriptions-item label="SDK状态" :span="2" v-if="testResult.sdkStatus"><span style="color:#e6a23c">{{ testResult.sdkStatus }}</span></el-descriptions-item>
          <el-descriptions-item label="开发建议" :span="2" v-if="testResult.suggestion"><span style="color:#67c23a">{{ testResult.suggestion }}</span></el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item label="设备名称" prop="device_name"><el-input v-model="form.device_name" maxlength="100"/></el-form-item>
        <el-form-item label="设备品牌" prop="device_brand">
          <el-select v-model="form.device_brand" style="width:100%"><el-option label="海康威视" value="HIKVISION"/><el-option label="大华" value="DAHUA"/><el-option label="其他" value="OTHER"/></el-select>
        </el-form-item>
        <el-form-item label="IP地址" prop="ip_address"><el-input v-model="form.ip_address" maxlength="50"/></el-form-item>
        <el-form-item label="端口" prop="port"><el-input-number v-model="form.port" :min="1" :max="65535" style="width:100%"/></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" maxlength="100"/></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" maxlength="100" type="password"/></el-form-item>
        <el-form-item label="安装位置"><el-input v-model="form.location" maxlength="200"/></el-form-item>
      </el-form>
      <div slot="footer"><el-button type="primary" @click="submit">确定</el-button><el-button @click="open=false">取消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/smart/access/person-device'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      testOpen: false, testLoading: false, testResult: null,
      query: { pageNum: 1, pageSize: 10, deviceName: undefined, deviceBrand: undefined },
      form: { device_name:'', device_brand:'HIKVISION', ip_address:'', port:80, username:'admin', password:'', location:'' },
      rules: {
        device_name: [{ required: true, message: '设备名称不能为空', trigger: 'blur' }],
        device_brand: [{ required: true, message: '请选择品牌', trigger: 'change' }],
        ip_address: [{ required: true, message: 'IP地址不能为空', trigger: 'blur' }],
        port: [{ required: true, message: '端口不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; request({ url: BASE+'/list', method: 'get', params: this.query }).then(r => { this.list = r.rows; this.total = r.total; this.loading = false }) },
    reset() { this.query = { pageNum:1, pageSize:10, deviceName:undefined, deviceBrand:undefined }; this.getList() },
    handleAdd() { this.form = { device_name:'', device_brand:'HIKVISION', ip_address:'', port:80, username:'admin', password:'', location:'' }; this.title='新增人员通行设备'; this.open = true },
    handleEdit(row) { this.form = { ...row }; this.title='修改人员通行设备'; this.open = true },
    handleDel(row) { this.$confirm('确认删除?').then(() => request({ url: BASE+'/'+row.device_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })).catch(() => {}) },
    submit() { this.$refs.form.validate(v => { if(!v) return; const m = this.form.device_id ? 'put' : 'post'; request({ url: BASE, method: m, data: this.form }).then(() => { this.$message.success('操作成功'); this.open = false; this.getList() }) }) },
    testDevice(row) {
      this.testOpen = true; this.testLoading = true; this.testResult = null
      request({ url: '/smart/device-test/connect', method: 'post', data: { ipAddress: row.ip_address, port: row.port || 80, deviceBrand: row.device_brand } }).then(res => {
        this.testResult = res.data || res; this.testLoading = false
      }).catch(() => { this.testLoading = false; this.$message.error('测试失败') })
    }
  }
}
</script>
