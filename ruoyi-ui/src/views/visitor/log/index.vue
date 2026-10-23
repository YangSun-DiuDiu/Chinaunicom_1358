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
          type="primary"
          icon="el-icon-camera"
          size="mini"
          @click="openVerify()"
          v-hasPermi="['visitor:log:exit']"
        >二维码核验</el-button>
      </el-col>
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
      <el-table-column label="状态" align="center" key="status" v-if="columns.status.visible" width="100">
        <template slot-scope="scope">
          <el-tag v-if="statusText(scope.row) === '待审批'" type="warning" size="small">待审批</el-tag>
          <el-tag v-else-if="statusText(scope.row) === '已通过'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="statusText(scope.row) === '已拒绝'" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else-if="statusText(scope.row) === '已取消'" type="info" size="small">已取消</el-tag>
          <el-tag v-else-if="statusText(scope.row) === '已到访'" type="success" size="small">已到访</el-tag>
          <el-tag v-else-if="statusText(scope.row) === '已完成'" type="info" size="small">已完成</el-tag>
          <el-tag v-else type="info" size="small">{{ statusText(scope.row) }}</el-tag>
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

    <!-- 二维码核验弹窗（复用 H5 保安核验逻辑：摄像头扫码 / 手动输入通行码） -->
    <el-dialog title="访客二维码核验" :visible.sync="verifyVisible" width="420px" :close-on-click-modal="false" @closed="stopScan">
      <div v-if="scanning" style="text-align:center;">
        <div ref="scanner" class="verify-scanner"></div>
        <p style="color:#999;font-size:13px;margin:8px 0;">将访客通行码对准框内扫码</p>
        <el-button size="small" type="info" plain @click="stopScan">关闭扫码</el-button>
      </div>
      <div v-else>
        <el-button type="primary" size="medium" icon="el-icon-camera-solid" style="width:100%" @click="startScan">打开摄像头扫码</el-button>
        <div style="text-align:center;color:#999;padding:12px 0;font-size:13px;">或</div>
        <el-input v-model="code" placeholder="手动输入通行码（扫码枪扫描后自动核验）" size="medium" clearable @keyup.enter.native="verifyManual">
          <el-button slot="append" @click="verifyManual" :loading="loading">核验</el-button>
        </el-input>
      </div>
      <div v-if="result" class="verify-result" :class="result.valid?'valid':'invalid'">
        <div class="verify-title">{{ result.valid ? '✅ 通行有效 · 已到访' : '❌ 无效通行码' }}</div>
        <div class="verify-grid" v-if="result.valid">
          <div class="verify-row"><span>访客</span><b>{{ result.visitorName }}</b></div>
          <div class="verify-row"><span>电话</span><b>{{ result.visitorPhone }}</b></div>
          <div class="verify-row"><span>被访人</span><b>{{ result.hostName }}</b></div>
          <div class="verify-row" v-if="result.hostDept"><span>部门</span><b>{{ result.hostDept }}</b></div>
          <div class="verify-row"><span>通行码</span><b>{{ result.passCode }}</b></div>
        </div>
        <div class="verify-time">{{ resultTime }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listVisitorLog, visitorExit } from "@/api/visitor/visitor"
import { Html5Qrcode } from 'html5-qrcode'
import request from '@/utils/request'

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
      // 核验弹窗
      verifyVisible: false,
      code: '',
      loadingVerify: false,
      scanning: false,
      scanner: null,
      result: null,
      resultTime: '',
      // 列信息
      columns: {
        visitorName: { label: '访客姓名', visible: true },
        visitorPhone: { label: '访客电话', visible: true },
        visitorCompany: { label: '访客单位', visible: true },
        hostName: { label: '被访人', visible: true },
        hostDept: { label: '被访部门', visible: true },
        registerType: { label: '登记类型', visible: true },
        status: { label: '状态', visible: true },
        entryTime: { label: '进入时间', visible: true },
        exitTime: { label: '离开时间', visible: true },
        operation: { label: '操作', visible: true }
      }
    }
  },
  created() {
    this.getList()
  },
  beforeDestroy() {
    this.stopScan()
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
    },
    /** 状态文案：现场登记按进入/离开判定，预约型按预约状态映射 */
    statusText(row) {
      if (row.registerType === 'WALKIN') {
        return row.exitTime ? '已完成' : '已到访'
      }
      const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', VISITING: '已到访', COMPLETED: '已完成' }
      return map[row.status] || row.status || '-'
    },
    // ===== 二维码核验（复用 H5 保安核验逻辑） =====
    openVerify(row) {
      this.verifyVisible = true
      this.result = null
      this.code = ''
    },
    async startScan() {
      this.result = null
      try {
        // ① 先用原生 getUserMedia 探测摄像头（绕过 html5-qrcode 的异常包装，拿真实错误名）
        const stream = await this.preflightCamera()
        // 预检成功：释放探测流，交给 html5-qrcode 重新获取（权限已记住，不会再弹框）
        stream.getTracks().forEach(t => t.stop())
        // ② 渲染容器后再构造扫码器
        this.scanning = true
        await this.$nextTick()
        this.scanner = new Html5Qrcode('scanner')
        // 优先后置摄像头（environment），失败自动降级前置摄像头
        const onSuccess = (decodedText) => {
          this.code = decodedText
          this.stopScan()
          this.verifyCode(decodedText)
        }
        try {
          await this.scanner.start(
            { facingMode: 'environment' },
            { fps: 10, qrbox: { width: 250, height: 250 } },
            onSuccess,
            () => {}
          )
        } catch (err1) {
          try {
            await this.scanner.start(
              { facingMode: 'user' },
              { fps: 10, qrbox: { width: 250, height: 250 } },
              onSuccess,
              () => {}
            )
          } catch (err2) {
            this.scanning = false
            this.showCameraError(err2)
          }
        }
      } catch (e) {
        this.scanning = false
        this.showCameraError(e)
      }
    },
    async preflightCamera() {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        const err = new Error('浏览器未提供摄像头接口（mediaDevices 不可用）')
        err.name = 'NoMediaDevices'
        throw err
      }
      try {
        return await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      } catch (err1) {
        // 后置摄像头失败 → 降级前置
        try {
          return await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
        } catch (err2) {
          throw err2
        }
      }
    },
    showCameraError(e) {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        this.$message.error('当前环境不支持摄像头调用（浏览器未提供摄像头接口）：请改用系统浏览器（Chrome/Safari）打开本站')
        return
      }
      const name = (e && e.name) || ''
      const msg = (e && e.message) || ''
      const msgMap = {
        NoMediaDevices: '当前环境不支持摄像头调用（浏览器未提供摄像头接口）：请改用系统浏览器（Chrome/Safari）打开本站',
        NotAllowedError: '浏览器拒绝了摄像头权限：请点击地址栏右侧摄像头图标，将本站权限设为「允许」后重试',
        NotFoundError: '未检测到可用摄像头：请确认设备已接入摄像头，或改用手动输入通行码',
        NotReadableError: '摄像头正被其他应用占用：请关闭其他占用相机的应用后重试',
        NotSupportedError: '当前环境不支持摄像头调用：请改用系统浏览器打开本站重试',
        SecurityError: '非安全环境无法调用摄像头：请通过 https:// 地址访问本站',
        TypeError: '浏览器拒绝摄像头调用（可能是应用内浏览器限制）：请改用系统浏览器打开'
      }
      if (msgMap[name]) {
        this.$message.error(msgMap[name])
      } else if (msg && /not found|element/i.test(msg)) {
        this.$message.error('页面组件未就绪：请刷新页面后重试打开摄像头')
      } else {
        this.$message.error('无法打开摄像头（' + (name || msg || '未知错误') + '）')
      }
    },
    async stopScan() {
      if (this.scanner) {
        try { await this.scanner.stop() } catch (e) {}
        try { this.scanner.clear() } catch (e) {}
        this.scanner = null
      }
      this.scanning = false
    },
    verifyManual() {
      if (!this.code) return
      this.verifyCode(this.code.trim())
    },
    verifyCode(code) {
      this.loadingVerify = true
      this.code = code
      request({ url: '/pass/' + encodeURIComponent(code), method: 'get' }).then(res => {
        const d = res.data || res
        if (d.visitorName) {
          this.result = { valid: true, ...d }
          // 到访确认（写操作走独立 POST；GET 为纯读），核验通过自动变更状态
          request({ url: '/pass/' + encodeURIComponent(code) + '/checkin', method: 'post' }).then(res2 => {
            const d2 = res2.data || res2
            if (d2.visitorName) {
              this.result = { valid: true, ...d2 }
              this.$modal.msgSuccess('核验通过，已自动变更状态为「已到访」')
              this.getList()
            }
          }).catch(() => {})
        } else {
          this.result = { valid: false }
        }
        this.resultTime = this.parseTime(new Date())
      }).catch(() => {
        this.result = { valid: false }
        this.resultTime = this.parseTime(new Date())
      }).finally(() => { this.loadingVerify = false })
    }
  }
}
</script>

<style scoped>
.verify-scanner { width:100%; max-width:320px; margin:0 auto 12px; border-radius:12px; overflow:hidden; background:#fff; }
.verify-result { margin-top:16px; border-radius:12px; padding:20px; text-align:center; background:#fff; border:1px solid #E4E7ED; }
.verify-result.valid { border:1px solid rgba(26,174,57,0.4); }
.verify-result.invalid { border:1px solid rgba(224,49,49,0.4); }
.verify-title { font-size:18px; font-weight:700; margin-bottom:14px; }
.valid .verify-title { color:#1AAE39; }
.invalid .verify-title { color:#E03131; }
.verify-grid { text-align:left; }
.verify-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid #F0F2F5; font-size:14px; color:#303133; }
.verify-row span { color:#909399; }
.verify-time { margin-top:14px; color:#C0C4CC; font-size:12px; }
</style>
