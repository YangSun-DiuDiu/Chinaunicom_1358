<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 选择租客 -->
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span><strong>选择租客</strong></span>
            <el-input v-model="tenantSearch" size="small" placeholder="搜索姓名/手机号" clearable style="width:200px;float:right;margin-top:-4px" @input="filterTenants"/>
          </div>
          <el-table :data="filteredTenants" height="400" highlight-current-row @current-change="onTenantSelect" v-loading="tenantLoading">
            <el-table-column label="姓名" prop="tenantName" min-width="80"/>
            <el-table-column label="手机号" prop="phone" min-width="110"/>
            <el-table-column label="性别" min-width="50" align="center">
              <template slot-scope="{row}">{{ row.gender === 'M' ? '男' : row.gender === 'F' ? '女' : row.gender }}</template>
            </el-table-column>
            <el-table-column label="状态" min-width="70" align="center">
              <template slot-scope="{row}">
                <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">{{ row.status === 'NORMAL' ? '在租' : '待入住' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="selectedTenant" style="margin-top:12px;padding:10px;background:#f0f9eb;border-radius:4px">
            <span>已选租客: <strong>{{ selectedTenant.tenantName }}</strong> | {{ selectedTenant.phone }}</span>
          </div>
        </el-card>
      </el-col>
      <!-- 选择房间 + 租期 -->
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span><strong>选择空置房间</strong></span>
          </div>
          <el-table :data="vacantRooms" height="300" highlight-current-row @current-change="onRoomSelect" v-loading="roomLoading">
            <el-table-column label="房间编号" prop="roomCode" min-width="110"/>
            <el-table-column label="楼栋" prop="building" min-width="70"/>
            <el-table-column label="楼层" prop="floor" min-width="60"/>
            <el-table-column label="户型" prop="unitType" min-width="80"/>
            <el-table-column label="面积(m²)" prop="area" min-width="80" align="center"/>
          </el-table>
          <div v-if="selectedRoom" style="margin-top:12px;padding:10px;background:#f0f9eb;border-radius:4px">
            <span>已选房间: <strong>{{ selectedRoom.roomCode }}</strong> | {{ selectedRoom.building }}-{{ selectedRoom.floor }} | {{ selectedRoom.unitType }}</span>
          </div>
          <el-divider/>
          <el-form ref="dateForm" :model="dateForm" :rules="dateRules" label-width="80px" size="small">
            <el-form-item label="租期开始" prop="rentStart" style="margin-bottom:14px">
              <el-date-picker v-model="dateForm.rentStart" type="date" value-format="timestamp" placeholder="选择租期开始日" style="width:100%"/>
            </el-form-item>
            <el-form-item label="租期结束" prop="rentEnd" style="margin-bottom:14px">
              <el-date-picker v-model="dateForm.rentEnd" type="date" value-format="timestamp" placeholder="选择租期结束日" style="width:100%"/>
            </el-form-item>
          </el-form>
          <div style="text-align:center;margin-top:10px">
            <el-button type="primary" size="medium" @click="doCheckIn" :disabled="!selectedTenant || !selectedRoom" v-hasPermi="['hostel:tenant:checkin']">确认办理入住</el-button>
            <el-button size="medium" @click="$router.push('/hostel/tenant')">返回列表</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script>
import request from '@/utils/request'
export default {
  data() {
    return {
      tenantSearch: '',
      tenantLoading: false,
      roomLoading: false,
      tenants: [],
      vacantRooms: [],
      selectedTenant: null,
      selectedRoom: null,
      dateForm: { rentStart: undefined, rentEnd: undefined },
      dateRules: {
        rentStart: [{ required: true, message: '请选择租期开始日', trigger: 'change' }],
        rentEnd: [{ required: true, message: '请选择租期结束日', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredTenants() {
      if (!this.tenantSearch) return this.tenants
      const kw = this.tenantSearch.toLowerCase()
      return this.tenants.filter(t =>
        (t.tenantName && t.tenantName.toLowerCase().includes(kw)) ||
        (t.phone && t.phone.includes(kw))
      )
    }
  },
  created() {
    this.loadTenants()
    this.loadVacantRooms()
  },
  methods: {
    loadTenants() {
      this.tenantLoading = true
      request({ url: '/hostel/tenant/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } }).then(r => {
        this.tenants = r.rows || []
        this.tenantLoading = false
      })
    },
    loadVacantRooms() {
      this.roomLoading = true
      request({ url: '/hostel/room/list', method: 'get', params: { pageNum: 1, pageSize: 1000, status: '0' } }).then(r => {
        this.vacantRooms = (r.rows || []).filter(room => room.status === '0' || room.status === 'GREEN')
        this.roomLoading = false
      })
    },
    filterTenants() {
      // computed handles filtering
    },
    onTenantSelect(row) {
      this.selectedTenant = row
    },
    onRoomSelect(row) {
      this.selectedRoom = row
    },
    doCheckIn() {
      this.$refs.dateForm.validate(v => {
        if (!v) return
        this.$confirm('确认办理「' + this.selectedTenant.tenantName + '」入住「' + this.selectedRoom.roomCode + '」?', '提示', { type: 'info' }).then(() => {
          const body = {
            tenantId: this.selectedTenant.tenantId,
            roomId: this.selectedRoom.roomId,
            rentStart: this.dateForm.rentStart,
            rentEnd: this.dateForm.rentEnd
          }
          request({ url: '/hostel/tenant/check-in', method: 'post', data: body }).then(() => {
            this.$message.success('办理入住成功')
            this.$router.push('/hostel/tenant')
          })
        }).catch(() => {})
      })
    }
  }
}
</script>
