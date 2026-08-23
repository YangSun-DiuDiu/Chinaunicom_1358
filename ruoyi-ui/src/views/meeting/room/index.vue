<template>
  <div class="app-container tree-sidebar-manage-wrap">
    <tree-panel title="组织架构" :tree-data="deptOptions" search-placeholder="请输入部门名称"
                storage-key="meeting-room-dept-sidebar-width" :defaultExpandAll="true"
                @node-click="handleDeptNodeClick" @refresh="getDeptTree" ref="deptTreeRef" />
    <div class="tree-sidebar-content">
      <div class="content-inner">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="会议室名称">
        <el-input v-model="query.roomName" placeholder="会议室名称" clearable style="width:180px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
          <el-option label="启用" value="0"/>
          <el-option label="停用" value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['meeting:room:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会议室名称" prop="roomName" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="位置" prop="location" min-width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="容量" prop="capacity" min-width="80" align="center"/>
      <el-table-column label="设备" prop="equipment" min-width="180" :show-overflow-tooltip="true"/>
      <el-table-column label="所属部门" prop="deptName" min-width="120" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="80" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="140" align="center"/>
      <el-table-column label="平板地址" min-width="300">
        <template slot-scope="{row}">
          <el-input :value="'http://1.94.26.126:80/meeting/board?roomId='+row.roomId" size="mini" readonly style="width:240px"/>
          <el-button size="mini" icon="el-icon-document-copy" style="margin-left:4px" @click="copyBoardUrl(row)">复制</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="120" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['meeting:room:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['meeting:room:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="会议室名称" prop="roomName">
          <el-input v-model="form.roomName" maxlength="100" placeholder="请输入会议室名称"/>
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" maxlength="200" placeholder="请输入位置"/>
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="9999" style="width:100%" placeholder="请输入容量"/>
        </el-form-item>
        <el-form-item label="设备" prop="equipment">
          <el-input v-model="form.equipment" maxlength="500" placeholder="请输入设备信息" type="textarea" :rows="3"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit">确定</el-button>
        <el-button @click="open=false">取消</el-button>
      </div>
    </el-dialog>
      </div>
    </div>
  </div>
</template>
<script>
import request from '@/utils/request'
import TreePanel from "@/components/TreePanel"
import { deptTreeSelect } from "@/api/meeting/room"
const BASE = '/meeting/room'
export default {
  components: { TreePanel },
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      deptOptions: [],
      query: { pageNum: 1, pageSize: 10, roomName: undefined, status: undefined, deptId: undefined },
      form: { roomId: undefined, roomName: '', location: '', capacity: 10, equipment: '', status: '0' },
      rules: {
        roomName: [{ required: true, message: '会议室名称不能为空', trigger: 'blur' }],
        location: [{ required: true, message: '位置不能为空', trigger: 'blur' }],
        capacity: [{ required: true, message: '容量不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList(); this.getDeptTree() },
  methods: {
    /** 查询部门树（组织架构，与系统管理-部门管理数据同步） */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
      })
    },
    /** 部门节点单击事件：按部门（含子部门）过滤会议室 */
    handleDeptNodeClick(data) {
      this.query.deptId = data.id
      this.getList()
    },
    /** 重置查询：清空部门过滤并取消树选中 */
    resetQuery() {
      this.query = { pageNum: 1, pageSize: 10, roomName: undefined, status: undefined, deptId: undefined }
      if (this.$refs.deptTreeRef) { this.$refs.deptTreeRef.setCurrentKey(null) }
      this.getList()
    },
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      })
    },
    handleAdd() {
      this.form = { roomId: undefined, roomName: '', location: '', capacity: 10, equipment: '', status: '0' }
      this.title = '新增会议室'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改会议室'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除会议室「' + row.roomName + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.roomId, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    copyBoardUrl(row) {
      const url = 'http://1.94.26.126:80/meeting/board?roomId=' + row.roomId
      navigator.clipboard.writeText(url).then(() => this.$message.success('已复制'))
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
