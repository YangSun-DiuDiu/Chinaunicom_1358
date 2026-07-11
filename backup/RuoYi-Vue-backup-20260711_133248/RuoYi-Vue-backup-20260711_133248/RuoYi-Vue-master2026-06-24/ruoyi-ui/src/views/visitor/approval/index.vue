<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="getList"
        >刷新</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="appointmentList">
      <el-table-column label="通行码" align="center" key="passCode" prop="passCode" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.passCode" type="primary" size="small">{{ scope.row.passCode }}</el-tag>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="访客姓名" align="center" key="visitorName" prop="visitorName" :show-overflow-tooltip="true" />
      <el-table-column label="访客电话" align="center" key="visitorPhone" prop="visitorPhone" width="130" />
      <el-table-column label="访客单位" align="center" key="visitorCompany" prop="visitorCompany" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="来访事由" align="center" key="visitReason" prop="visitReason" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="被访人" align="center" key="hostName" prop="hostName" width="100" />
      <el-table-column label="被访部门" align="center" key="hostDept" prop="hostDept" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="来访时间" align="center" key="visitTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.visitTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" key="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="success"
            icon="el-icon-check"
            @click="handleApprove(scope.row)"
          >通过</el-button>
          <el-button
            size="mini"
            type="danger"
            icon="el-icon-close"
            @click="handleReject(scope.row)"
          >拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 审批通过对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="approveOpen" width="500px" append-to-body>
      <el-form ref="approveForm" :model="approveFormData" :rules="remarkRules" label-width="80px">
        <el-form-item label="访客姓名">
          <el-input v-model="currentRow.visitorName" disabled />
        </el-form-item>
        <el-form-item label="审批备注" prop="remark">
          <el-input
            v-model="approveFormData.remark"
            type="textarea"
            placeholder="请输入审批备注（可选）"
            maxlength="200"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitApprove">确 定</el-button>
        <el-button @click="approveOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 拒绝对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="rejectOpen" width="500px" append-to-body>
      <el-form ref="rejectForm" :model="rejectFormData" :rules="remarkRequiredRules" label-width="80px">
        <el-form-item label="访客姓名">
          <el-input v-model="currentRow.visitorName" disabled />
        </el-form-item>
        <el-form-item label="拒绝理由" prop="remark">
          <el-input
            v-model="rejectFormData.remark"
            type="textarea"
            placeholder="请输入拒绝理由"
            maxlength="200"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitReject">确 定</el-button>
        <el-button @click="rejectOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { pendingAppointment, approveAppointment } from "@/api/visitor/visitor"

export default {
  name: "VisitorApproval",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 待审批列表
      appointmentList: [],
      // 当前操作行数据
      currentRow: {},
      // 审批通过弹窗
      approveOpen: false,
      approveFormData: {
        remark: ""
      },
      // 拒绝弹窗
      rejectOpen: false,
      rejectFormData: {
        remark: ""
      },
      // 弹窗标题
      dialogTitle: "",
      // 审批备注校验（可选）
      remarkRules: {
        remark: [
          { max: 200, message: "备注长度不能超过200个字符", trigger: "blur" }
        ]
      },
      // 拒绝理由校验（必填）
      remarkRequiredRules: {
        remark: [
          { required: true, message: "拒绝理由不能为空", trigger: "blur" },
          { max: 200, message: "理由长度不能超过200个字符", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询待审批列表 */
    getList() {
      this.loading = true
      pendingAppointment().then(response => {
        this.appointmentList = response.rows || response.data || []
        this.loading = false
      })
    },
    /** 通过按钮操作 */
    handleApprove(row) {
      this.currentRow = row
      this.approveFormData.remark = ""
      this.dialogTitle = "审批通过 - " + row.visitorName
      this.approveOpen = true
      this.$nextTick(() => {
        this.$refs.approveForm && this.$refs.approveForm.clearValidate()
      })
    },
    /** 拒绝按钮操作 */
    handleReject(row) {
      this.currentRow = row
      this.rejectFormData.remark = ""
      this.dialogTitle = "拒绝预约 - " + row.visitorName
      this.rejectOpen = true
      this.$nextTick(() => {
        this.$refs.rejectForm && this.$refs.rejectForm.clearValidate()
      })
    },
    /** 提交审批通过 */
    submitApprove() {
      this.$refs.approveForm.validate(valid => {
        if (valid) {
          const data = {
            appointmentId: this.currentRow.appointmentId,
            status: "APPROVED",
            remark: this.approveFormData.remark
          }
          approveAppointment(data).then(() => {
            this.$modal.msgSuccess("审批通过")
            this.approveOpen = false
            this.getList()
          })
        }
      })
    },
    /** 提交拒绝 */
    submitReject() {
      this.$refs.rejectForm.validate(valid => {
        if (valid) {
          const data = {
            appointmentId: this.currentRow.appointmentId,
            status: "REJECTED",
            remark: this.rejectFormData.remark
          }
          approveAppointment(data).then(() => {
            this.$modal.msgSuccess("已拒绝")
            this.rejectOpen = false
            this.getList()
          })
        }
      })
    }
  }
}
</script>
