<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="姓名">
        <el-input v-model="query.tenantName" placeholder="租客姓名" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
          <el-option label="在租" value="NORMAL"/>
          <el-option label="已退租" value="CHECKED_OUT"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['hostel:tenant:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain size="mini" @click="$router.push('/hostel/tenant/checkin')" v-hasPermi="['hostel:tenant:checkin']">办理入住</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="姓名" prop="tenantName" min-width="100"/>
      <el-table-column label="身份证号" prop="idCard" min-width="160"/>
      <el-table-column label="手机号" prop="phone" min-width="120"/>
      <el-table-column label="性别" min-width="60" align="center">
        <template slot-scope="{row}">{{ row.gender === 'M' ? '男' : row.gender === 'F' ? '女' : row.gender }}</template>
      </el-table-column>
      <el-table-column label="房间" min-width="130">
        <template slot-scope="{row}">{{ roomCodeOf(row.roomId) }}</template>
      </el-table-column>
      <el-table-column label="状态" min-width="80" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.status === 'NORMAL' ? 'success' : 'info'" size="small">{{ row.status === 'NORMAL' ? '在租' : '已退租' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="租期开始" prop="rentStart" min-width="110" align="center"/>
      <el-table-column label="租期结束" prop="rentEnd" min-width="110" align="center"/>
      <el-table-column label="创建时间" prop="createTime" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="160" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['hostel:tenant:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['hostel:tenant:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="580px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="姓名" prop="tenantName">
          <el-input v-model="form.tenantName" maxlength="64" placeholder="请输入姓名"/>
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" maxlength="18" placeholder="请输入身份证号"/>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="20" placeholder="请输入手机号"/>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择性别" style="width:100%">
            <el-option label="男" value="M"/>
            <el-option label="女" value="F"/>
          </el-select>
        </el-form-item>
        <el-form-item label="租期开始" prop="rentStart">
          <el-date-picker v-model="form.rentStart" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%"/>
        </el-form-item>
        <el-form-item label="租期结束" prop="rentEnd">
          <el-date-picker v-model="form.rentEnd" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="NORMAL">在租</el-radio>
            <el-radio label="CHECKED_OUT">已退租</el-radio>
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
const BASE = '/hostel/tenant'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      rooms: [],
      query: { pageNum: 1, pageSize: 10, tenantName: undefined, phone: undefined, status: undefined },
      form: { tenantId: undefined, tenantName: '', idCard: '', phone: '', gender: 'M', rentStart: undefined, rentEnd: undefined, status: 'NORMAL' },
      rules: {
        tenantName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getRooms(); this.getList() },
  methods: {
    getRooms() {
      request({ url: '/hostel/room/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } }).then(r => {
        this.rooms = r.rows || []
      })
    },
    roomCodeOf(id) {
      const r = this.rooms.find(x => x.roomId === id)
      return r ? r.roomCode : ''
    },
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, tenantName: undefined, phone: undefined, status: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { tenantId: undefined, tenantName: '', idCard: '', phone: '', gender: 'M', roomId: undefined, rentStart: undefined, rentEnd: undefined, status: 'NORMAL' }
      this.title = '新增租客'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改租客'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除租客「' + row.tenantName + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.tenantId, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.tenantId ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
