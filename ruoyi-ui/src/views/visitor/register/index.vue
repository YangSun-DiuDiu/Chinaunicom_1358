<template>
  <div class="app-container">
    <!-- 现场登记表单 -->
    <el-card class="register-card">
      <div slot="header">
        <span>访客现场登记</span>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" size="small">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="访客姓名" prop="visitorName">
              <el-input v-model="form.visitorName" placeholder="请输入访客姓名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="访客电话" prop="visitorPhone">
              <el-input v-model="form.visitorPhone" placeholder="请输入访客电话" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="身份证号" prop="visitorIdCard">
              <el-input v-model="form.visitorIdCard" placeholder="请输入身份证号" maxlength="18" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="访客单位" prop="visitorCompany">
              <el-input v-model="form.visitorCompany" placeholder="请输入访客单位" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="被访人" prop="hostName">
              <el-input v-model="form.hostName" placeholder="请输入被访人姓名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="被访部门" prop="hostDept">
              <el-input v-model="form.hostDept" placeholder="请输入被访部门" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="来访事由" prop="visitReason">
              <el-input v-model="form.visitReason" type="textarea" placeholder="请输入来访事由" maxlength="200" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登记时间" prop="entryTime">
              <el-date-picker
                v-model="form.entryTime"
                type="datetime"
                placeholder="请选择登记时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" style="text-align: right;">
            <el-form-item>
              <el-button type="primary" icon="el-icon-check" @click="submitForm" :loading="submitting">提 交</el-button>
              <el-button icon="el-icon-refresh" @click="resetForm">重 置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 当日登记记录 -->
    <el-card class="list-card" style="margin-top: 16px;">
      <div slot="header">
        <span>当日登记记录</span>
        <el-button
          style="float: right; padding: 3px 0"
          type="text"
          icon="el-icon-refresh"
          @click="getList"
        >刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="logList" stripe border>
        <el-table-column label="通行码" align="center" prop="passCode" width="90">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.passCode" type="primary" size="small">{{ scope.row.passCode }}</el-tag>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="访客姓名" align="center" prop="visitorName" :show-overflow-tooltip="true" min-width="100" />
        <el-table-column label="访客电话" align="center" prop="visitorPhone" width="130" />
        <el-table-column label="身份证号" align="center" prop="visitorIdCard" width="180" />
        <el-table-column label="访客单位" align="center" prop="visitorCompany" :show-overflow-tooltip="true" min-width="140" />
        <el-table-column label="来访事由" align="center" prop="visitReason" :show-overflow-tooltip="true" min-width="140" />
        <el-table-column label="被访人" align="center" prop="hostName" width="100" />
        <el-table-column label="被访部门" align="center" prop="hostDept" :show-overflow-tooltip="true" min-width="120" />
        <el-table-column label="登记时间" align="center" width="160">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.entryTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="90">
          <template slot-scope="scope">
            <el-tag v-if="!scope.row.exitTime" type="primary" size="small">来访中</el-tag>
            <el-tag v-else type="success" size="small">已完成</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="logList.length === 0 && !loading" style="text-align: center; padding: 40px 0; color: #999;">
        暂无当日登记记录
      </div>
    </el-card>

    <!-- 登记成功弹窗 -->
    <el-dialog :title="'登记成功'" :visible.sync="successOpen" width="500px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="访客姓名">{{ lastRecord.visitorName }}</el-descriptions-item>
        <el-descriptions-item label="访客电话">{{ lastRecord.visitorPhone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ lastRecord.visitorIdCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="访客单位">{{ lastRecord.visitorCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来访事由">{{ lastRecord.visitReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被访人">{{ lastRecord.hostName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被访部门">{{ lastRecord.hostDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ parseTime(lastRecord.entryTime) }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="successOpen = false">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { registerVisitor, listVisitorLog } from "@/api/visitor/visitor"

export default {
  name: "VisitorRegister",
  data() {
    return {
      // 表单加载
      submitting: false,
      // 列表加载
      loading: true,
      // 当日登记记录
      logList: [],
      // 登记成功弹窗
      successOpen: false,
      // 最近一条登记记录
      lastRecord: {},
      // 表单参数
      form: {
        visitorName: undefined,
        visitorPhone: undefined,
        visitorIdCard: undefined,
        visitorCompany: undefined,
        visitReason: undefined,
        hostName: undefined,
        hostDept: undefined,
        entryTime: undefined
      },
      // 表单校验
      rules: {
        visitorName: [
          { required: true, message: "访客姓名不能为空", trigger: "blur" }
        ],
        visitorPhone: [
          { required: true, message: "访客电话不能为空", trigger: "blur" },
          { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号码", trigger: "blur" }
        ],
        visitorIdCard: [
          { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: "请输入正确的身份证号", trigger: "blur" }
        ],
        entryTime: [
          { required: true, message: "登记时间不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.initEntryTime()
    this.getList()
  },
  methods: {
    /** 初始化登记时间为当前时间 */
    initEntryTime() {
      const now = new Date()
      const y = now.getFullYear()
      const m = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      const hh = String(now.getHours()).padStart(2, '0')
      const mm = String(now.getMinutes()).padStart(2, '0')
      const ss = String(now.getSeconds()).padStart(2, '0')
      this.form.entryTime = `${y}-${m}-${d} ${hh}:${mm}:${ss}`
    },
    /** 查询当日登记记录 */
    getList() {
      this.loading = true
      const today = new Date()
      const y = today.getFullYear()
      const m = String(today.getMonth() + 1).padStart(2, '0')
      const d = String(today.getDate()).padStart(2, '0')
      const todayStr = `${y}-${m}-${d}`
      const params = {
        pageNum: 1,
        pageSize: 999,
        registerType: 'WALKIN',
        params: {
          beginTime: todayStr,
          endTime: todayStr
        }
      }
      listVisitorLog(params).then(response => {
        this.logList = response.rows || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    /** 重置表单 */
    resetForm() {
      this.form = {
        visitorName: undefined,
        visitorPhone: undefined,
        visitorIdCard: undefined,
        visitorCompany: undefined,
        visitReason: undefined,
        hostName: undefined,
        hostDept: undefined,
        entryTime: undefined
      }
      this.$refs.form && this.$refs.form.resetFields()
      this.initEntryTime()
    },
    /** 提交登记 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.submitting = true
          registerVisitor(this.form).then(response => {
            this.submitting = false
            this.lastRecord = response.data || this.form
            this.successOpen = true
            this.$modal.msgSuccess("登记成功")
            this.resetForm()
            this.getList()
          }).catch(() => {
            this.submitting = false
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.register-card {
  max-width: 960px;
}

.list-card {
  max-width: 1200px;
}
</style>
