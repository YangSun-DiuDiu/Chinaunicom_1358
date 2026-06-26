<template>
  <div class="app-container dashboard-container">
    <!-- 模式切换提示 -->
    <div class="dashboard-mode-badge">
      <span class="mode-label">当前驾驶舱模式：</span>
      <el-tag :type="dashboardMode === 'device' ? 'primary' : 'success'" size="small">
        {{ dashboardMode === 'device' ? '设备驾驶舱' : '访客驾驶舱' }}
      </el-tag>
    </div>

    <!-- ==================== DEVICE MODE ==================== -->
    <template v-if="dashboardMode === 'device'">
      <!-- 顶部统计卡片 -->
      <el-row :gutter="20" class="stat-card-row">
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-online">
            <div class="stat-card-icon">
              <svg-icon icon-class="monitor" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">在线设备</div>
              <count-to :start-val="0" :end-val="deviceData.onlineCount" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-offline">
            <div class="stat-card-icon">
              <svg-icon icon-class="guide" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">离线设备</div>
              <count-to :start-val="0" :end-val="deviceData.offlineCount" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-total">
            <div class="stat-card-icon">
              <svg-icon icon-class="switching" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">设备总数</div>
              <count-to :start-val="0" :end-val="deviceData.totalDevices" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-rate">
            <div class="stat-card-icon">
              <svg-icon icon-class="chart" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">在线率</div>
              <count-to :start-val="0" :end-val="deviceData.onlineRate" :duration="2000" :suffix="'%'" :decimals="1" class="stat-card-value" />
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 中部：设备类型分布饼图 + 最近告警列表 -->
      <el-row :gutter="20" class="middle-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <div slot="header" class="card-header">
              <span>设备类型分布</span>
            </div>
            <div ref="devicePieChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="table-card">
            <div slot="header" class="card-header">
              <span>最近告警记录</span>
              <el-button type="text" size="mini" @click="goToAlerts">查看更多</el-button>
            </div>
            <el-table
              :data="deviceData.recentAlerts"
              v-loading="deviceLoading"
              size="small"
              max-height="300"
              style="width: 100%"
            >
              <el-table-column label="设备名称" align="center" prop="deviceName" :show-overflow-tooltip="true" />
              <el-table-column label="告警类型" align="center" prop="alertType" width="100">
                <template slot-scope="scope">
                  <el-tag :type="alertTypeTag(scope.row.alertType)" size="mini">
                    {{ scope.row.alertType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="告警内容" align="center" prop="alertContent" :show-overflow-tooltip="true" />
              <el-table-column label="时间" align="center" width="160">
                <template slot-scope="scope">
                  <span>{{ parseTime(scope.row.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 底部：最新心跳记录 -->
      <el-row :gutter="20" class="bottom-row">
        <el-col :span="24">
          <el-card shadow="never" class="table-card">
            <div slot="header" class="card-header">
              <span>最新心跳记录</span>
              <el-button type="text" size="mini" @click="goToHeartbeat">查看更多</el-button>
            </div>
            <el-table
              :data="deviceData.heartbeatRecords"
              v-loading="deviceLoading"
              size="small"
              max-height="300"
              style="width: 100%"
            >
              <el-table-column label="设备名称" align="center" prop="deviceName" :show-overflow-tooltip="true" />
              <el-table-column label="IP地址" align="center" prop="ipAddr" width="140" />
              <el-table-column label="状态" align="center" prop="status" width="90">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status === 'ONLINE' ? 'success' : 'danger'" size="mini">
                    {{ scope.row.status === 'ONLINE' ? '在线' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="响应时间" align="center" prop="responseTime" width="100">
                <template slot-scope="scope">
                  <span>{{ scope.row.responseTime }}ms</span>
                </template>
              </el-table-column>
              <el-table-column label="心跳时间" align="center" width="160">
                <template slot-scope="scope">
                  <span>{{ parseTime(scope.row.heartbeatTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- ==================== VISITOR MODE ==================== -->
    <template v-if="dashboardMode === 'visitor'">
      <!-- 顶部统计卡片 -->
      <el-row :gutter="20" class="stat-card-row">
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-visitors">
            <div class="stat-card-icon">
              <svg-icon icon-class="peoples" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">今日访客</div>
              <count-to :start-val="0" :end-val="visitorData.todayVisitors" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-pending">
            <div class="stat-card-icon">
              <svg-icon icon-class="message" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">待审批预约</div>
              <count-to :start-val="0" :end-val="visitorData.pendingApprovals" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-appointments">
            <div class="stat-card-icon">
              <svg-icon icon-class="date" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">预约总数</div>
              <count-to :start-val="0" :end-val="visitorData.totalAppointments" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <div class="stat-card stat-card-entries">
            <div class="stat-card-icon">
              <svg-icon icon-class="logininfor" />
            </div>
            <div class="stat-card-body">
              <div class="stat-card-label">今日入园</div>
              <count-to :start-val="0" :end-val="visitorData.todayEntries" :duration="2000" class="stat-card-value" />
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 中部：访客状态分布柱状图 + 最新来访列表 -->
      <el-row :gutter="20" class="middle-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <div slot="header" class="card-header">
              <span>访客状态分布</span>
            </div>
            <div ref="visitorBarChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="table-card">
            <div slot="header" class="card-header">
              <span>最新来访记录</span>
              <el-button type="text" size="mini" @click="goToVisitLog">查看更多</el-button>
            </div>
            <el-table
              :data="visitorData.latestVisits"
              v-loading="visitorLoading"
              size="small"
              max-height="300"
              style="width: 100%"
            >
              <el-table-column label="访客姓名" align="center" prop="visitorName" width="100" />
              <el-table-column label="访客电话" align="center" prop="visitorPhone" width="130" />
              <el-table-column label="被访人" align="center" prop="hostName" width="100" />
              <el-table-column label="来访事由" align="center" prop="visitReason" :show-overflow-tooltip="true" />
              <el-table-column label="来访时间" align="center" width="160">
                <template slot-scope="scope">
                  <span>{{ parseTime(scope.row.visitTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 底部：待审批清单 -->
      <el-row :gutter="20" class="bottom-row">
        <el-col :span="24">
          <el-card shadow="never" class="table-card">
            <div slot="header" class="card-header">
              <span>待审批预约</span>
              <el-button type="text" size="mini" @click="goToApproval">查看更多</el-button>
            </div>
            <el-table
              :data="visitorData.pendingApprovalList"
              v-loading="visitorLoading"
              size="small"
              max-height="300"
              style="width: 100%"
            >
              <el-table-column label="访客姓名" align="center" prop="visitorName" width="100" />
              <el-table-column label="访客电话" align="center" prop="visitorPhone" width="130" />
              <el-table-column label="访客单位" align="center" prop="visitorCompany" :show-overflow-tooltip="true" />
              <el-table-column label="被访人" align="center" prop="hostName" width="100" />
              <el-table-column label="被访部门" align="center" prop="hostDept" :show-overflow-tooltip="true" width="120" />
              <el-table-column label="状态" align="center" prop="status" width="90">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status === 'PENDING' ? 'warning' : 'info'" size="mini">
                    {{ scope.row.status === 'PENDING' ? '待审批' : scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="预约时间" align="center" width="160">
                <template slot-scope="scope">
                  <span>{{ parseTime(scope.row.createTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import CountTo from 'vue-count-to'
import { getDeviceDashboard, getVisitorDashboard } from '@/api/dashboard/dashboard'

export default {
  name: 'Index',
  components: {
    CountTo
  },
  data() {
    return {
      // 驾驶舱模式: 'device' | 'visitor'
      dashboardMode: 'device',
      // 设备数据
      deviceLoading: false,
      deviceData: {
        onlineCount: 0,
        offlineCount: 0,
        totalDevices: 0,
        onlineRate: 0,
        recentAlerts: [],
        heartbeatRecords: [],
        deviceTypeDistribution: []
      },
      // 访客数据
      visitorLoading: false,
      visitorData: {
        todayVisitors: 0,
        pendingApprovals: 0,
        totalAppointments: 0,
        todayEntries: 0,
        latestVisits: [],
        pendingApprovalList: [],
        visitorStatusDistribution: []
      },
      // ECharts实例
      devicePieChartInstance: null,
      visitorBarChartInstance: null
    }
  },
  created() {
    this.loadDashboardMode()
  },
  mounted() {
    this.$nextTick(() => {
      this.loadData()
    })
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.disposeCharts()
  },
  methods: {
    /** 从配置中读取驾驶舱模式 */
    loadDashboardMode() {
      // 尝试从vuex或后端配置读取dashboard模式
      const mode = this.$store.state.settings.dashboardMode
      if (mode && (mode === 'device' || mode === 'visitor')) {
        this.dashboardMode = mode
      }
      // 也可通过后端配置接口获取
      if (this.getConfigKey) {
        this.getConfigKey('sys.dashboard.mode').then(res => {
          if (res && res.msg && res.data && (res.data === 'device' || res.data === 'visitor')) {
            this.dashboardMode = res.data
          }
        }).catch(() => {})
      }
    },

    /** 加载数据 */
    loadData() {
      if (this.dashboardMode === 'device') {
        this.loadDeviceData()
      } else {
        this.loadVisitorData()
      }
    },

    /** 加载设备驾驶舱数据 */
    loadDeviceData() {
      this.deviceLoading = true
      getDeviceDashboard().then(response => {
        const data = response.data || response
        this.deviceData = {
          onlineCount: data.onlineCount || 0,
          offlineCount: data.offlineCount || 0,
          totalDevices: data.totalCount || 0,
          onlineRate: data.onlineRate || 0,
          recentAlerts: (data.recentAlerts || []).map(this.mapAlertRow),
          heartbeatRecords: (data.recentHeartbeats || []).map(this.mapHeartbeatRow),
          deviceTypeDistribution: (data.deviceTypeStats || []).map(t => ({ name: this.deviceTypeName(t.type), value: t.count }))
        }
        this.deviceLoading = false
        this.$nextTick(() => {
          this.initDevicePieChart()
        })
      }).catch(() => {
        this.deviceLoading = false
      })
    },

    /** 加载访客驾驶舱数据 */
    loadVisitorData() {
      this.visitorLoading = true
      getVisitorDashboard().then(response => {
        const data = response.data || response
        this.visitorData = {
          todayVisitors: data.todayVisitors || 0,
          pendingApprovals: data.pendingApprovals || 0,
          totalAppointments: data.totalAppointments || 0,
          todayEntries: data.todayEntryCount || 0,
          latestVisits: data.recentVisits || [],
          pendingApprovalList: data.pendingApprovals ? [] : data.recentVisits || [],
          visitorStatusDistribution: (data.visitStats || []).map(s => ({ name: this.visitorStatusName(s.status), value: s.count }))
        }
        this.visitorLoading = false
        this.$nextTick(() => {
          this.initVisitorBarChart()
        })
      }).catch(() => {
        this.visitorLoading = false
      })
    },

    /** 初始化设备类型分布饼图 */
    initDevicePieChart() {
      if (!this.$refs.devicePieChart) return
      if (this.devicePieChartInstance) {
        this.devicePieChartInstance.dispose()
      }
      this.devicePieChartInstance = echarts.init(this.$refs.devicePieChart, 'macarons')

      const distData = this.deviceData.deviceTypeDistribution
      const names = distData.map(item => item.name || item.deviceType || '')
      const values = distData.map(item => item.value || item.count || 0)
      const chartData = names.map((name, index) => ({
        name: name,
        value: values[index]
      }))

      this.devicePieChartInstance.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          top: 'center'
        },
        series: [
          {
            name: '设备类型',
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['60%', '50%'],
            data: chartData.length > 0 ? chartData : [
              { value: 0, name: '暂无数据' }
            ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            },
            label: {
              formatter: '{b}: {c}'
            }
          }
        ]
      })
    },

    /** 初始化访客状态分布柱状图 */
    initVisitorBarChart() {
      if (!this.$refs.visitorBarChart) return
      if (this.visitorBarChartInstance) {
        this.visitorBarChartInstance.dispose()
      }
      this.visitorBarChartInstance = echarts.init(this.$refs.visitorBarChart, 'macarons')

      const distData = this.visitorData.visitorStatusDistribution
      const categories = distData.map(item => item.name || item.status || '')
      const values = distData.map(item => item.value || item.count || 0)

      this.visitorBarChartInstance.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          top: 10,
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: categories.length > 0 ? categories : ['暂无数据'],
            axisTick: {
              alignWithLabel: true
            }
          }
        ],
        yAxis: [
          {
            type: 'value',
            axisTick: {
              show: false
            }
          }
        ],
        series: [
          {
            name: '数量',
            type: 'bar',
            barWidth: '50%',
            data: values.length > 0 ? values : [0],
            itemStyle: {
              color: '#409EFF'
            },
            animationDuration: 2000
          }
        ]
      })
    },

    /** 窗口大小改变时重绘图表 */
    handleResize() {
      if (this.devicePieChartInstance) {
        this.devicePieChartInstance.resize()
      }
      if (this.visitorBarChartInstance) {
        this.visitorBarChartInstance.resize()
      }
    },

    /** 销毁图表实例 */
    disposeCharts() {
      if (this.devicePieChartInstance) {
        this.devicePieChartInstance.dispose()
        this.devicePieChartInstance = null
      }
      if (this.visitorBarChartInstance) {
        this.visitorBarChartInstance.dispose()
        this.visitorBarChartInstance = null
      }
    },

    /** 映射告警记录字段 */
    mapAlertRow(row) {
      return {
        deviceName: row.deviceName,
        alertType: row.changeType === 'ONLINE' ? '恢复' : '离线',
        alertContent: row.oldStatus + ' → ' + row.newStatus,
        createTime: row.changeTime || row.createTime
      }
    },

    /** 映射心跳记录字段 */
    mapHeartbeatRow(row) {
      return {
        deviceName: row.deviceName,
        ipAddr: row.ipAddress,
        status: row.pingResult === 'SUCCESS' ? 'ONLINE' : 'OFFLINE',
        responseTime: row.pingLatency,
        heartbeatTime: row.detectTime || row.createTime
      }
    },

    /** 设备类型中文映射 */
    deviceTypeName(type) {
      const map = { NETWORK: '网络设备', MONITOR: '监控设备', NVR: '硬盘录像机', OTHER: '其他设备' }
      return map[type] || type || '未知'
    },

    /** 访客状态中文映射 */
    visitorStatusName(status) {
      const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', VISITING: '访客中', COMPLETED: '已完成' }
      return map[status] || status || '未知'
    },

    /** 告警类型对应tag样式 */
    alertTypeTag(type) {
      const map = {
        '离线': 'danger',
        'OFFLINE': 'danger',
        '高延迟': 'warning',
        'HIGH_LATENCY': 'warning',
        '故障': 'danger',
        'FAULT': 'danger',
        '恢复': 'success',
        'RECOVERY': 'success'
      }
      return map[type] || 'info'
    },

    /** 跳转相关页面 */
    goToAlerts() {
      this.$router.push('/device/heartbeat')
    },
    goToHeartbeat() {
      this.$router.push('/device/heartbeat')
    },
    goToVisitLog() {
      this.$router.push('/visitor/log')
    },
    goToApproval() {
      this.$router.push('/visitor/approval')
    }
  }
}
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 10px 0;

  .dashboard-mode-badge {
    margin-bottom: 16px;
    padding: 0 5px;

    .mode-label {
      font-size: 13px;
      color: #666;
      margin-right: 8px;
    }
  }

  // 统计卡片行
  .stat-card-row {
    margin-bottom: 20px;
  }

  // 统计卡片
  .stat-card {
    display: flex;
    align-items: center;
    height: 108px;
    background: #fff;
    border-radius: 6px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    padding: 20px;
    margin-bottom: 16px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    }

    .stat-card-icon {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      flex-shrink: 0;

      .svg-icon {
        font-size: 28px;
        color: #fff;
      }
    }

    .stat-card-body {
      flex: 1;
      min-width: 0;

      .stat-card-label {
        font-size: 14px;
        color: #999;
        margin-bottom: 8px;
        white-space: nowrap;
      }

      .stat-card-value {
        font-size: 28px;
        font-weight: 700;
        color: #333;
        line-height: 1;
      }
    }

    // 各卡片配色
    &.stat-card-online .stat-card-icon {
      background: linear-gradient(135deg, #67C23A, #85CE61);
    }
    &.stat-card-online .stat-card-value {
      color: #67C23A;
    }

    &.stat-card-offline .stat-card-icon {
      background: linear-gradient(135deg, #F56C6C, #F89898);
    }
    &.stat-card-offline .stat-card-value {
      color: #F56C6C;
    }

    &.stat-card-total .stat-card-icon {
      background: linear-gradient(135deg, #409EFF, #79BBFF);
    }
    &.stat-card-total .stat-card-value {
      color: #409EFF;
    }

    &.stat-card-rate .stat-card-icon {
      background: linear-gradient(135deg, #E6A23C, #EEBB5C);
    }
    &.stat-card-rate .stat-card-value {
      color: #E6A23C;
    }

    &.stat-card-visitors .stat-card-icon {
      background: linear-gradient(135deg, #40c9c6, #67dbd9);
    }
    &.stat-card-visitors .stat-card-value {
      color: #40c9c6;
    }

    &.stat-card-pending .stat-card-icon {
      background: linear-gradient(135deg, #36a3f7, #69b9f9);
    }
    &.stat-card-pending .stat-card-value {
      color: #36a3f7;
    }

    &.stat-card-appointments .stat-card-icon {
      background: linear-gradient(135deg, #f4516c, #f77b8e);
    }
    &.stat-card-appointments .stat-card-value {
      color: #f4516c;
    }

    &.stat-card-entries .stat-card-icon {
      background: linear-gradient(135deg, #34bfa3, #5fd4bf);
    }
    &.stat-card-entries .stat-card-value {
      color: #34bfa3;
    }
  }

  // 中部行
  .middle-row {
    margin-bottom: 20px;
  }

  // 底部行
  .bottom-row {
    margin-bottom: 0;
  }

  // 图表卡片
  .chart-card {
    .chart-container {
      width: 100%;
      height: 300px;
    }
  }

  // 表格卡片
  .table-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  // 卡片通用头部
  .card-header {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }
}

// 移动端适配
@media (max-width: 768px) {
  .dashboard-container {
    .stat-card {
      height: 80px;
      padding: 12px;

      .stat-card-icon {
        width: 40px;
        height: 40px;

        .svg-icon {
          font-size: 20px;
        }
      }

      .stat-card-body {
        .stat-card-label {
          font-size: 12px;
          margin-bottom: 4px;
        }
        .stat-card-value {
          font-size: 22px;
        }
      }
    }

    .chart-card .chart-container {
      height: 240px;
    }
  }
}
</style>
