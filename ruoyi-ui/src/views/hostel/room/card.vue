<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button plain size="mini" @click="$router.push('/hostel/room')">📋 表格</el-button>
        <el-button plain size="mini" @click="$router.push('/hostel/room/card')">🎴 卡片</el-button>
      </el-col>
    </el-row>
    <el-form :model="query" size="small" :inline="true" class="filter-bar" label-width="60px">
      <el-form-item label="公寓">
        <el-select v-model="query.apartmentId" placeholder="全部" clearable style="width:160px" @change="getCardList">
          <el-option v-for="a in apartments" :key="a.apartmentId" :label="a.apartmentName" :value="a.apartmentId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="楼栋">
        <el-input v-model="query.building" placeholder="楼栋" clearable style="width:140px" @keyup.enter.native="getCardList"/>
      </el-form-item>
      <el-form-item label="楼层">
        <el-input v-model="query.floor" placeholder="楼层" clearable style="width:140px" @keyup.enter.native="getCardList"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px" @change="getCardList">
          <el-option label="空闲" value="0"/>
          <el-option label="已租" value="1"/>
          <el-option label="维修中" value="2"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getCardList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <div class="card-grid">
      <div v-for="room in cardList" :key="room.roomId" class="room-card" :class="cardClass(room.status)" @click="openDetail(room)">
        <div class="card-header">{{ room.roomCode }}</div>
        <div class="card-body">
          <p>{{ room.unitType || '-' }} | {{ room.area != null ? room.area + 'm²' : '-' }}</p>
          <p>{{ room.building || '-' }}-{{ room.floor || '-' }}</p>
          <p>{{ tenantNames(room) || '暂无租客' }}</p>
          <p>床位: {{ room.params.actualCount || 0 }}/{{ room.tenantCount || 0 }} | {{ room.doorType === 'ONLINE' ? '联网锁' : 'NFC锁' }}</p>
        </div>
        <div class="card-footer">{{ room.rentStart || '-' }} ~ {{ room.rentEnd || '-' }}</div>
      </div>
    </div>
    <!-- 详情弹窗 -->
    <el-dialog :visible.sync="detailOpen" width="80%" top="5vh" :close-on-click-modal="false" append-to-body>
      <el-tabs v-if="detailOpen" v-model="activeTab">
        <el-tab-pane label="基础信息" name="info">
          <el-form ref="detailForm" :model="detailForm" label-width="100px" size="small" style="max-width:600px">
            <el-form-item label="房间编号">
              <el-input v-model="detailForm.roomCode" maxlength="50"/>
            </el-form-item>
            <el-form-item label="所属公寓">
              <el-select v-model="detailForm.apartmentId" style="width:100%">
                <el-option v-for="a in apartments" :key="a.apartmentId" :label="a.apartmentName" :value="a.apartmentId"/>
              </el-select>
            </el-form-item>
            <el-form-item label="楼栋">
              <el-input v-model="detailForm.building" maxlength="50"/>
            </el-form-item>
            <el-form-item label="楼层">
              <el-input v-model="detailForm.floor" maxlength="50"/>
            </el-form-item>
            <el-form-item label="户型">
              <el-input v-model="detailForm.unitType" maxlength="50"/>
            </el-form-item>
            <el-form-item label="面积(m²)">
              <el-input-number v-model="detailForm.area" :min="0" :precision="2" style="width:100%"/>
            </el-form-item>
            <el-form-item label="租期开始">
              <el-date-picker v-model="detailForm.rentStart" type="date" value-format="yyyy-MM-dd" style="width:100%"/>
            </el-form-item>
            <el-form-item label="租期结束">
              <el-date-picker v-model="detailForm.rentEnd" type="date" value-format="yyyy-MM-dd" style="width:100%"/>
            </el-form-item>
            <el-form-item label="租客人数">
              <el-input-number v-model="detailForm.tenantCount" :min="0" :max="9999" style="width:100%"/>
            </el-form-item>
            <el-form-item label="门锁类型">
              <el-select v-model="detailForm.doorType" style="width:100%">
                <el-option label="联网锁" value="ONLINE"/>
                <el-option label="NFC锁" value="NFC"/>
              </el-select>
            </el-form-item>
            <el-form-item label="设备状态">
              <el-select v-model="detailForm.deviceStatus" style="width:100%">
                <el-option label="已绑定" value="BOUND"/>
                <el-option label="未绑定" value="UNBOUND"/>
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="detailForm.status">
                <el-radio label="0">空闲</el-radio>
                <el-radio label="1">已租</el-radio>
                <el-radio label="2">维修中</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveDetail">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="租客管理" name="tenant">
          <div v-if="detailForm.status === '0' || detailForm.status === 'GREEN'" style="margin-bottom:12px">
            <el-button type="primary" size="small" icon="el-icon-plus" @click="$router.push('/hostel/tenant/checkin')" v-hasPermi="['hostel:tenant:checkin']">办理入住</el-button>
          </div>
          <el-table :data="tenantList" v-loading="tenantLoading" size="small" max-height="400">
            <el-table-column label="姓名" prop="tenantName" min-width="80"/>
            <el-table-column label="手机号" prop="phone" min-width="110"/>
            <el-table-column label="租期开始" prop="rentStart" min-width="100" align="center"/>
            <el-table-column label="租期结束" prop="rentEnd" min-width="100" align="center"/>
            <el-table-column label="状态" min-width="70" align="center">
              <template slot-scope="{row}">
                <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">{{ row.status === 'NORMAL' ? '在租' : '已退租' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="80" align="center" v-if="detailForm.status === '1' || detailForm.status === 'BLUE'">
              <template slot-scope="{row}">
                <el-button size="mini" type="text" icon="el-icon-delete" @click="doCheckOut(row)" v-if="row.status === 'NORMAL'" v-hasPermi="['hostel:tenant:checkin']">退租</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!tenantLoading && tenantList.length === 0" class="tab-placeholder">暂无租客数据</div>
        </el-tab-pane>
        <el-tab-pane label="设备绑定" name="device"><div class="tab-placeholder">功能开发中</div></el-tab-pane>
        <el-tab-pane label="账单" name="bill"><div class="tab-placeholder">功能开发中</div></el-tab-pane>
        <el-tab-pane label="能耗分析" name="energy"><div class="tab-placeholder">功能开发中</div></el-tab-pane>
        <el-tab-pane label="报修" name="repair"><div class="tab-placeholder">功能开发中</div></el-tab-pane>
      </el-tabs>
      <div slot="footer" style="text-align:right">
        <el-button @click="detailOpen=false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/hostel/room'
export default {
  data() {
    return {
      cardList: [],
      apartments: [],
      query: { apartmentId: undefined, building: undefined, floor: undefined, status: undefined },
      timer: null,
      detailOpen: false,
      activeTab: 'info',
      detailForm: {},
      tenantList: [],
      tenantLoading: false
    }
  },
  created() {
    this.getApartments(); this.getCardList()
    this.timer = setInterval(() => { this.getCardList() }, 30000)
  },
  beforeDestroy() { if (this.timer) clearInterval(this.timer) },
  methods: {
    getApartments() {
      request({ url: '/hostel/apartment/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } }).then(r => {
        this.apartments = r.rows || []
      })
    },
    cardClass(status) {
      return 'card-' + (status || 'GREEN')
    },
    tenantNames(room) {
      const names = room.params && room.params.tenantNames
      return names && names.length ? names.join(', ') : ''
    },
    getCardList() {
      request({ url: BASE + '/card/list', method: 'get', params: this.query }).then(r => {
        this.cardList = r.data || []
      })
    },
    reset() {
      this.query = { apartmentId: undefined, building: undefined, floor: undefined, status: undefined }
      this.getCardList()
    },
    openDetail(room) {
      request({ url: BASE + '/' + room.roomId + '/detail', method: 'get' }).then(r => {
        this.detailForm = r.data || {}
        this.activeTab = 'info'
        this.detailOpen = true
        this.loadTenants(room.roomId)
      })
    },
    saveDetail() {
      request({ url: BASE, method: 'put', data: this.detailForm }).then(() => {
        this.$message.success('保存成功')
        this.detailOpen = false
        this.getCardList()
      })
    },
    loadTenants(roomId) {
      this.tenantLoading = true
      request({ url: '/hostel/tenant/list', method: 'get', params: { pageNum: 1, pageSize: 100, roomId: roomId } }).then(r => {
        this.tenantList = r.rows || []
        this.tenantLoading = false
      })
    },
    doCheckOut(tenant) {
      this.$confirm('确认「' + tenant.tenantName + '」退租?', '提示', { type: 'warning' }).then(() =>
        request({ url: '/hostel/tenant/check-out/' + tenant.tenantId, method: 'post' }).then(() => {
          this.$message.success('退租成功')
          this.loadTenants(this.detailForm.roomId)
          this.getCardList()
        })
      ).catch(() => {})
    }
  }
}
</script>
<style scoped>
.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.room-card {
  width: 280px;
  height: 220px;
  border-radius: 8px;
  padding: 16px;
  box-sizing: border-box;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: box-shadow 0.2s;
}
.room-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}
.card-header {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.card-body p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.card-footer {
  font-size: 12px;
  color: #909399;
  border-top: 1px solid rgba(0,0,0,0.08);
  padding-top: 8px;
}
.card-GREEN  { background: #e8f5e9; }
.card-BLUE   { background: #e3f2fd; }
.card-YELLOW { background: #fff8e1; }
.card-ORANGE { background: #fff3e0; }
.card-RED    { background: #ffebee; }
.card-GRAY   { background: #f5f5f5; }
.tab-placeholder {
  padding: 60px 0;
  text-align: center;
  color: #909399;
  font-size: 16px;
}
.filter-bar {
  margin-bottom: 16px;
}
</style>
