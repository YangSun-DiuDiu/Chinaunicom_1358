<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公寓名称">
        <el-input v-model="query.apartmentName" placeholder="公寓名称" clearable style="width:180px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['hostel:apartment:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="公寓名称" prop="apartmentName" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="地址" prop="address" min-width="200" :show-overflow-tooltip="true"/>
      <el-table-column label="联系电话" prop="contactPhone" min-width="120"/>
      <el-table-column label="创建时间" prop="createTime" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="120" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['hostel:apartment:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['hostel:apartment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="公寓名称" prop="apartmentName">
          <el-input v-model="form.apartmentName" maxlength="100" placeholder="请输入公寓名称"/>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" maxlength="200" placeholder="请输入地址"/>
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="50" placeholder="请输入联系电话"/>
        </el-form-item>
        <el-form-item label="区域描述" prop="areaDesc">
          <el-input v-model="form.areaDesc" maxlength="500" placeholder="请输入区域描述" type="textarea" :rows="3"/>
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
const BASE = '/hostel/apartment'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      query: { pageNum: 1, pageSize: 10, apartmentName: undefined },
      form: { apartmentId: undefined, apartmentName: '', address: '', contactPhone: '', areaDesc: '' },
      rules: {
        apartmentName: [{ required: true, message: '公寓名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, apartmentName: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { apartmentId: undefined, apartmentName: '', address: '', contactPhone: '', areaDesc: '' }
      this.title = '新增公寓'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改公寓'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除公寓「' + row.apartmentName + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.apartmentId, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.apartmentId ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
