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
      <el-form-item label="访客电话" prop="visitorPhone">
        <el-input
          v-model="queryParams.visitorPhone"
          placeholder="请输入访客电话"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          style="width: 200px"
        >
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="来访中" value="VISITING" />
          <el-option label="已完成" value="COMPLETED" />
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
        ></el-date-picker>
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
          v-hasPermi="['visitor:appointment:add']"
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
          v-hasPermi="['visitor:appointment:edit']"
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
          v-hasPermi="['visitor:appointment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-close"
          size="mini"
          :disabled="single"
          @click="handleCancel"
          v-hasPermi="['visitor:appointment:cancel']"
        >取消预约</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-check"
          size="mini"
          :disabled="single"
          @click="handleComplete"
          v-hasPermi="['visitor:appointment:complete']"
        >完成来访</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['visitor:appointment:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-qr-code"
          size="mini"
          @click="showQrCode"
        >登记二维码</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="appointmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="通行码" align="center" key="passCode" prop="passCode" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.passCode" type="primary" size="small">{{ scope.row.passCode }}</el-tag>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="访客姓名" align="center" key="visitorName" prop="visitorName" v-if="columns.visitorName.visible" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="访客电话" align="center" key="visitorPhone" prop="visitorPhone" v-if="columns.visitorPhone.visible" width="120" />
      <el-table-column label="访客单位" align="center" key="visitorCompany" prop="visitorCompany" v-if="columns.visitorCompany.visible" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="被访人" align="center" key="hostName" prop="hostName" v-if="columns.hostName.visible" width="120" />
      <el-table-column label="被访部门" align="center" key="hostDept" prop="hostDept" v-if="columns.hostDept.visible" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="来访时间" align="center" key="visitTime" v-if="columns.visitTime.visible" width="140">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.visitTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" key="status" v-if="columns.status.visible" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status==='PENDING'" type="warning" size="small">待审批</el-tag>
          <el-tag v-else-if="scope.row.status==='APPROVED'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="scope.row.status==='REJECTED'" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else type="info" size="small">{{scope.row.status}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="短信" align="center" key="smsSent" width="70">
        <template slot-scope="scope">
          <span v-if="scope.row.smsSent==='Y'" style="color:#67c23a">&#10003;</span>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['visitor:appointment:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['visitor:appointment:remove']"
          >删除</el-button>
          <el-button
            v-if="scope.row.status==='APPROVED' && scope.row.passCode"
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="openPassPage(scope.row)"
          >通行码</el-button>
          <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['visitor:appointment:cancel', 'visitor:appointment:complete']">
            <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="handleCancel" icon="el-icon-close" v-hasPermi="['visitor:appointment:cancel']">取消预约</el-dropdown-item>
              <el-dropdown-item command="handleComplete" icon="el-icon-check" v-hasPermi="['visitor:appointment:complete']">完成来访</el-dropdown-item>
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

    <!-- 二维码弹窗 -->
    <qr-code ref="qrcode" />

    <!-- 添加或修改预约对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="访客姓名" prop="visitorName">
              <el-input v-model="form.visitorName" placeholder="请输入访客姓名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="访客电话" prop="visitorPhone">
              <el-input v-model="form.visitorPhone" placeholder="请输入访客电话" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="visitorIdCard">
              <el-input v-model="form.visitorIdCard" placeholder="请输入身份证号" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="访客单位" prop="visitorCompany">
              <el-input v-model="form.visitorCompany" placeholder="请输入访客单位" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="来访事由" prop="visitReason">
              <el-input v-model="form.visitReason" type="textarea" placeholder="请输入来访事由" maxlength="200" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否开车">
              <el-radio-group v-model="form.hasCar" @change="onHasCarChange">
                <el-radio label="0">否</el-radio>
                <el-radio label="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.hasCar === '1'">
            <el-form-item label="车牌号" prop="carPlate">
              <el-input v-model="form.carPlate" placeholder="请输入车牌号" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否携带物资">
              <el-radio-group v-model="form.hasGoods" @change="onHasGoodsChange">
                <el-radio label="0">否</el-radio>
                <el-radio label="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.hasGoods === '1'">
            <el-form-item label="物资说明" prop="goodsDesc">
              <el-input v-model="form.goodsDesc" placeholder="请输入携带物资说明" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="被访人" prop="hostName">
              <el-input v-model="form.hostName" placeholder="请输入被访人姓名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="被访部门" prop="hostDept">
              <el-input v-model="form.hostDept" placeholder="请输入被访部门" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="被访人电话" prop="hostPhone">
              <el-input v-model="form.hostPhone" placeholder="请输入被访人电话" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来访时间" prop="visitTime">
              <el-date-picker
                v-model="form.visitTime"
                type="datetime"
                placeholder="请选择来访时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%"
              ></el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAppointment, getAppointment, delAppointment, addAppointment, updateAppointment, cancelAppointment, completeAppointment } from "@/api/visitor/visitor"
import QrCode from '@/components/QrCode'

export default {
  name: "VisitorAppointment",
  components: { QrCode },
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
      // 预约表格数据
      appointmentList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        visitorName: undefined,
        visitorPhone: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 列信息
      columns: {
        visitorName: { label: '访客姓名', visible: true },
        visitorPhone: { label: '访客电话', visible: true },
        visitorCompany: { label: '访客单位', visible: true },
        hostName: { label: '被访人', visible: true },
        hostDept: { label: '被访部门', visible: true },
        visitTime: { label: '来访时间', visible: true },
        status: { label: '状态', visible: true }
      },
      // 表单校验
      rules: {
        visitorName: [
          { required: true, message: "访客姓名不能为空", trigger: "blur" }
        ],
        visitorPhone: [
          { required: true, message: "访客电话不能为空", trigger: "blur" },
          { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }
        ],
        visitorIdCard: [
          { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: "请输入正确的身份证号", trigger: "blur" }
        ],
        visitReason: [
          { required: true, message: "来访事由不能为空", trigger: "blur" }
        ],
        hostName: [
          { required: true, message: "被访人不能为空", trigger: "blur" }
        ],
        hostPhone: [
          { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }
        ],
        visitTime: [
          { required: true, message: "来访时间不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询预约列表 */
    getList() {
      this.loading = true
      listAppointment(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.appointmentList = response.rows
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
        appointmentId: undefined,
        visitorName: undefined,
        visitorPhone: undefined,
        visitorIdCard: undefined,
        visitorCompany: undefined,
        visitReason: undefined,
        hasCar: '0',
        carPlate: undefined,
        hasGoods: '0',
        goodsDesc: undefined,
        hostName: undefined,
        hostDept: undefined,
        hostPhone: undefined,
        visitTime: undefined
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
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.appointmentId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    // 更多操作触发
    /** 打开访客通行码页面 */
    openPassPage(row) {
      window.open('/pass/' + row.passCode, '_blank')
    },
    handleCommand(command, row) {
      switch (command) {
        case "handleCancel":
          this.handleCancel(row)
          break
        case "handleComplete":
          this.handleComplete(row)
          break
        default:
          break
      }
    },
    onHasCarChange(val) { if (val === '0') this.form.carPlate = '' },
    onHasGoodsChange(val) { if (val === '0') this.form.goodsDesc = '' },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加预约"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const appointmentId = row.appointmentId || this.ids
      getAppointment(appointmentId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改预约"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.appointmentId != undefined) {
            updateAppointment(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addAppointment(this.form).then(() => {
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
      const appointmentIds = row.appointmentId || this.ids.join(",")
      this.$modal.confirm('是否确认删除预约编号为"' + appointmentIds + '"的数据项？').then(function() {
        return delAppointment(appointmentIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 取消预约按钮操作 */
    handleCancel(row) {
      const appointmentId = row.appointmentId || this.ids.join(",")
      this.$modal.confirm('是否确认取消预约编号为"' + appointmentId + '"的预约？').then(function() {
        return cancelAppointment(appointmentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("取消成功")
      }).catch(() => {})
    },
    /** 完成来访按钮操作 */
    handleComplete(row) {
      const appointmentId = row.appointmentId || this.ids.join(",")
      this.$modal.confirm('是否确认完成预约编号为"' + appointmentId + '"的来访？').then(function() {
        return completeAppointment(appointmentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("完成来访成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('visitor/appointment/export', {
        ...this.addDateRange(this.queryParams, this.dateRange)
      }, `appointment_${new Date().getTime()}.xlsx`)
    },
    /** 显示登记二维码 */
    showQrCode() {
      this.$refs.qrcode.show()
    }
  }
}
</script>
