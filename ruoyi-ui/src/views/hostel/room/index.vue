<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公寓">
        <el-select v-model="query.apartmentId" placeholder="全部" clearable style="width:160px">
          <el-option v-for="a in apartments" :key="a.apartmentId" :label="a.apartmentName" :value="a.apartmentId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="楼栋">
        <el-input v-model="query.building" placeholder="楼栋" clearable style="width:140px"/>
      </el-form-item>
      <el-form-item label="房间编号">
        <el-input v-model="query.roomCode" placeholder="房间编号" clearable style="width:160px"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option label="空闲" value="0"/>
          <el-option label="已租" value="1"/>
          <el-option label="维修中" value="2"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['hostel:room:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain size="mini" @click="$router.push('/hostel/room')">📋 表格</el-button>
        <el-button plain size="mini" @click="$router.push('/hostel/room/card')">🎴 卡片</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="房间编号" prop="roomCode" min-width="120"/>
      <el-table-column label="所属公寓" min-width="140">
        <template slot-scope="{row}">{{ aptName(row.apartmentId) }}</template>
      </el-table-column>
      <el-table-column label="楼栋" prop="building" min-width="80"/>
      <el-table-column label="楼层" prop="floor" min-width="70"/>
      <el-table-column label="户型" prop="unitType" min-width="90"/>
      <el-table-column label="面积(m²)" prop="area" min-width="90" align="center"/>
      <el-table-column label="租客人数" prop="tenantCount" min-width="80" align="center"/>
      <el-table-column label="门锁类型" min-width="90">
        <template slot-scope="{row}">{{ row.doorType === 'ONLINE' ? '联网锁' : row.doorType === 'NFC' ? 'NFC锁' : row.doorType }}</template>
      </el-table-column>
      <el-table-column label="设备状态" min-width="100">
        <template slot-scope="{row}">{{ row.deviceStatus === 'BOUND' ? '已绑定' : '未绑定' }}</template>
      </el-table-column>
      <el-table-column label="状态" min-width="80" align="center">
        <template slot-scope="{row}">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="120" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['hostel:room:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['hostel:room:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="580px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="房间编号" prop="roomCode">
          <el-input v-model="form.roomCode" maxlength="50" placeholder="请输入房间编号"/>
        </el-form-item>
        <el-form-item label="所属公寓" prop="apartmentId">
          <el-select v-model="form.apartmentId" placeholder="请选择公寓" style="width:100%">
            <el-option v-for="a in apartments" :key="a.apartmentId" :label="a.apartmentName" :value="a.apartmentId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋" prop="building">
          <el-input v-model="form.building" maxlength="50" placeholder="请输入楼栋"/>
        </el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input v-model="form.floor" maxlength="50" placeholder="请输入楼层"/>
        </el-form-item>
        <el-form-item label="户型" prop="unitType">
          <el-input v-model="form.unitType" maxlength="50" placeholder="请输入户型"/>
        </el-form-item>
        <el-form-item label="面积(m²)" prop="area">
          <el-input-number v-model="form.area" :min="0" :precision="2" style="width:100%" placeholder="请输入面积"/>
        </el-form-item>
        <el-form-item label="租期开始" prop="rentStart">
          <el-date-picker v-model="form.rentStart" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%"/>
        </el-form-item>
        <el-form-item label="租期结束" prop="rentEnd">
          <el-date-picker v-model="form.rentEnd" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%"/>
        </el-form-item>
        <el-form-item label="租客人数" prop="tenantCount">
          <el-input-number v-model="form.tenantCount" :min="0" :max="9999" style="width:100%" placeholder="请输入租客人数"/>
        </el-form-item>
        <el-form-item label="门锁类型" prop="doorType">
          <el-select v-model="form.doorType" placeholder="请选择" style="width:100%">
            <el-option label="联网锁" value="ONLINE"/>
            <el-option label="NFC锁" value="NFC"/>
          </el-select>
        </el-form-item>
        <el-form-item label="设备状态" prop="deviceStatus">
          <el-select v-model="form.deviceStatus" placeholder="请选择" style="width:100%">
            <el-option label="已绑定" value="BOUND"/>
            <el-option label="未绑定" value="UNBOUND"/>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">空闲</el-radio>
            <el-radio label="1">已租</el-radio>
            <el-radio label="2">维修中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit">确定</el-button>
        <el-button @click="open=false">取消</el-button>
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
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      apartments: [],
      query: { pageNum: 1, pageSize: 10, apartmentId: undefined, building: undefined, roomCode: undefined, status: undefined },
      form: { roomId: undefined, roomCode: '', apartmentId: undefined, building: '', floor: '', unitType: '', area: undefined, rentStart: undefined, rentEnd: undefined, tenantCount: 0, doorType: 'ONLINE', deviceStatus: 'BOUND', status: '0' },
      rules: {
        roomCode: [{ required: true, message: '房间编号不能为空', trigger: 'blur' }],
        apartmentId: [{ required: true, message: '请选择公寓', trigger: 'change' }]
      }
    }
  },
  created() { this.getApartments(); this.getList() },
  methods: {
    getApartments() {
      request({ url: '/hostel/apartment/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } }).then(r => {
        this.apartments = r.rows || []
      })
    },
    aptName(id) {
      const a = this.apartments.find(x => x.apartmentId === id)
      return a ? a.apartmentName : ''
    },
    statusText(v) { return v === '0' ? '空闲' : v === '1' ? '已租' : v === '2' ? '维修中' : v },
    statusTag(v) { return v === '0' ? 'success' : v === '1' ? '' : v === '2' ? 'danger' : '' },
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, apartmentId: undefined, building: undefined, roomCode: undefined, status: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { roomId: undefined, roomCode: '', apartmentId: undefined, building: '', floor: '', unitType: '', area: undefined, rentStart: undefined, rentEnd: undefined, tenantCount: 0, doorType: 'ONLINE', deviceStatus: 'BOUND', status: '0' }
      this.title = '新增房间'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改房间'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除房间「' + row.roomCode + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.roomId, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.roomId ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
