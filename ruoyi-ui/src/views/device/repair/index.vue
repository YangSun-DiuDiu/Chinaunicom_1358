<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="queryParams.deviceName" placeholder="设备名称" clearable style="width:180px" @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="工单状态" clearable style="width:150px">
          <el-option label="待处理" value="PENDING"/><el-option label="已派发" value="ASSIGNED"/>
          <el-option label="已接收" value="ACCEPTED"/><el-option label="已拒绝" value="REJECTED"/>
          <el-option label="已完成" value="COMPLETED"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-s-data" size="mini" @click="$router.push('/device/repair-workload')">工作量统计</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="loading" :data="repairList">
      <el-table-column label="工单编号" align="center" prop="repairNo" width="110"/>
      <el-table-column label="设备名称" align="center" prop="deviceName" :show-overflow-tooltip="true" min-width="110"/>
      <el-table-column label="设备IP" align="center" prop="deviceIp" width="140"/>
      <el-table-column label="责任人" align="center" prop="currentResponsible" width="100"/>
      <el-table-column label="联系人电话" align="center" prop="currentPhone" width="120"/>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status==='PENDING'" type="warning" size="small">待处理</el-tag>
          <el-tag v-else-if="scope.row.status==='ASSIGNED'" type="info" size="small">已派发</el-tag>
          <el-tag v-else-if="scope.row.status==='ACCEPTED'" type="primary" size="small">已接收</el-tag>
          <el-tag v-else-if="scope.row.status==='REJECTED'" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else-if="scope.row.status==='COMPLETED'" type="success" size="small">已完成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="维修结果" align="center" prop="repairResult" :show-overflow-tooltip="true" min-width="140"/>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template slot-scope="scope">
          <el-button v-if="scope.row.status==='PENDING'||scope.row.status==='ACCEPTED'" size="mini" type="primary" @click="handleTransfer(scope.row)">转派</el-button>
          <el-button v-if="scope.row.status==='ASSIGNED'" size="mini" type="success" @click="handleAccept(scope.row)">接收</el-button>
          <el-button v-if="scope.row.status==='ASSIGNED'" size="mini" type="danger" @click="handleReject(scope.row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 转派弹窗 -->
    <el-dialog title="转派工单" :visible.sync="transferOpen" width="400px" append-to-body>
      <el-form ref="transferForm" :model="transferForm" :rules="transferRules" label-width="90px" size="small">
        <el-form-item label="被转派人" prop="transferTo"><el-input v-model="transferForm.transferTo" placeholder="请输入被转派人姓名" maxlength="30"/></el-form-item>
        <el-form-item label="联系电话" prop="transferToPhone"><el-input v-model="transferForm.transferToPhone" placeholder="请输入被转派人电话" maxlength="11"/></el-form-item>
        <el-form-item label="转派原因"><el-input v-model="transferForm.reason" type="textarea" placeholder="选填" :rows="2"/></el-form-item>
      </el-form>
      <div slot="footer"><el-button type="primary" @click="confirmTransfer">确认转派</el-button><el-button @click="transferOpen=false">取消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'DeviceRepair',
  data() {
    return {
      loading: true, showSearch: true, total: 0, repairList: [],
      transferOpen: false, currentRepairId: null,
      queryParams: { pageNum:1, pageSize:10, deviceName:undefined, status:undefined },
      transferForm: { transferTo:'', transferToPhone:'', reason:'' },
      transferRules: {
        transferTo: [{ required: true, message: '被转派人不能为空', trigger: 'blur' }],
        transferToPhone: [{ required: true, message: '联系电话不能为空', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      request({ url: '/device/repair/list', method: 'get', params: this.queryParams }).then(res => {
        this.repairList = res.rows; this.total = res.total; this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleTransfer(row) {
      this.currentRepairId = row.repairId
      this.transferForm = { transferTo:'', transferToPhone:'', reason:'' }
      this.transferOpen = true
    },
    confirmTransfer() {
      this.$refs.transferForm.validate(valid => {
        if (!valid) return
        request({ url: '/device/repair/transfer/' + this.currentRepairId, method: 'post', data: this.transferForm }).then(() => {
          this.$message.success('转派成功'); this.transferOpen = false; this.getList()
        }).catch(() => {})
      })
    },
    handleAccept(row) {
      this.$confirm('确认接收该维修工单吗？').then(() => {
        request({ url: '/device/repair/accept/' + row.repairId, method: 'post' }).then(() => { this.$message.success('已接收'); this.getList() })
      }).catch(() => {})
    },
    handleReject(row) {
      this.$prompt('请输入拒绝原因', '拒绝工单', { confirmButtonText:'确定', cancelButtonText:'取消' }).then(({ value }) => {
        request({ url: '/device/repair/reject/' + row.repairId, method: 'post', data: { reason: value || '' } }).then(() => { this.$message.success('已拒绝'); this.getList() })
      }).catch(() => {})
    }
  }
}
</script>
