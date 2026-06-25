<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="访客姓名" prop="visitorName">
        <el-input
          v-model="queryParams.visitorName"
          placeholder="请输入访客姓名"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="登记类型" prop="registerType">
        <el-select
          v-model="queryParams.registerType"
          placeholder="请选择登记类型"
          clearable
          style="width: 200px"
        >
          <el-option label="预约来访" value="APPOINTMENT" />
          <el-option label="现场登记" value="WALKIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="来访时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['visitor:log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns" />
    </el-row>

    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column label="通行码" align="center" prop="passCode" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.passCode" type="primary" size="small">{{ scope.row.passCode }}</el-tag>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="访客姓名" align="center" key="visitorName" prop="visitorName" v-if="columns.visitorName.visible" :show-overflow-tooltip="true" min-width="100" />
      <el-table-column label="访客电话" align="center" key="visitorPhone" prop="visitorPhone" v-if="columns.visitorPhone.visible" width="130" />
      <el-table-column label="访客单位" align="center" key="visitorCompany" prop="visitorCompany" v-if="columns.visitorCompany.visible" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="被访人" align="center" key="hostName" prop="hostName" v-if="columns.hostName.visible" width="100" />
      <el-table-column label="被访部门" align="center" key="hostDept" prop="hostDept" v-if="columns.hostDept.visible" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="登记类型" align="center" key="registerType" v-if="columns.registerType.visible" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.registerType === 'APPOINTMENT'" type="primary" size="small">预约来访</el-tag>
          <el-tag v-else-if="scope.row.registerType === 'WALKIN'" type="info" size="small">现场登记</el-tag>
          <span v-else>{{ scope.row.registerType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="进入时间" align="center" key="entryTime" v-if="columns.entryTime.visible" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.entryTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="离开时间" align="center" key="exitTime" v-if="columns.exitTime.visible" width="160">
        <template slot-scope="scope">
          <span v-if="scope.row.exitTime">{{ parseTime(scope.row.exitTime) }}</span>
          <span v-else style="color: #999;">--</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width" v-if="columns.operation.visible">
        <template slot-scope="scope">
          <el-button
            v-if="!scope.row.exitTime"
            size="mini"
            type="warning"
            plain
            icon="el-icon-switch-button"
            @click="handleExit(scope.row)"
            v-hasPermi="['visitor:log:exit']"
          >离开</el-button>
          <span v-else style="color: #67C23A;">已离开</span>
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
  </div>
</template>

<script>
import { listVisitorLog, visitorExit } from "@/api/visitor/visitor"

export default {
  name: "VisitorLog",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 来访记录表格数据
      logList: [],
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        visitorName: undefined,
        registerType: undefined
      },
      // 列信息
      columns: {
        visitorName: { label: '访客姓名', visible: true },
        visitorPhone: { label: '访客电话', visible: true },
        visitorCompany: { label: '访客单位', visible: true },
        hostName: { label: '被访人', visible: true },
        hostDept: { label: '被访部门', visible: true },
        registerType: { label: '登记类型', visible: true },
        entryTime: { label: '进入时间', visible: true },
        exitTime: { label: '离开时间', visible: true },
        operation: { label: '操作', visible: true }
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询来访记录列表 */
    getList() {
      this.loading = true
      listVisitorLog(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.logList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange() {
      // 预留选择功能
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('visitor/log/export', {
        ...this.queryParams
      }, `visitor_log_${new Date().getTime()}.xlsx`)
    },
    /** 记录离开操作 */
    handleExit(row) {
      this.$modal.confirm('确认记录访客"' + row.visitorName + '"的离开时间吗？').then(() => {
        return visitorExit(row.logId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("离开记录成功")
      }).catch(() => {})
    }
  }
}
</script>
