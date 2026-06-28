<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="业务编码">
        <el-input v-model="query.biz_code" placeholder="业务编码" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="业务名称">
        <el-input v-model="query.biz_name" placeholder="业务名称" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="启用" value="0"/>
          <el-option label="禁用" value="1"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sms:biz:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list" border stripe size="small">
      <el-table-column label="业务编码" prop="biz_code" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="业务名称" prop="biz_name" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="签名模板" prop="st_id" min-width="140" align="center">
        <template slot-scope="{row}">
          {{ stMap[row.st_id] || row.st_id }}
        </template>
      </el-table-column>
      <el-table-column label="备用渠道" prop="backup_channel_id" min-width="120" align="center">
        <template slot-scope="{row}">
          {{ row.backup_channel_id ? (channelMap[row.backup_channel_id] || row.backup_channel_id) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="分钟上限" prop="minute_limit" min-width="90" align="center"/>
      <el-table-column label="日上限" prop="day_limit" min-width="90" align="center"/>
      <el-table-column label="状态" min-width="80" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="create_time" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="120" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['sms:biz:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['sms:biz:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <el-form-item label="业务编码" prop="biz_code">
          <el-input v-model="form.biz_code" maxlength="50" placeholder="全局唯一业务编码" :disabled="!!form.biz_id"/>
        </el-form-item>
        <el-form-item label="业务名称" prop="biz_name">
          <el-input v-model="form.biz_name" maxlength="100" placeholder="如: 设备维修通知"/>
        </el-form-item>
        <el-form-item label="签名模板" prop="st_id">
          <el-select v-model="form.st_id" placeholder="请选择签名模板" style="width:100%">
            <el-option v-for="st in stOptions" :key="st.st_id" :label="st.sign_name + ' / ' + st.template_code" :value="st.st_id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备用渠道" prop="backup_channel_id">
          <el-select v-model="form.backup_channel_id" placeholder="可选备用渠道" clearable style="width:100%">
            <el-option v-for="ch in channelOptions" :key="ch.channel_id" :label="ch.channel_name" :value="ch.channel_id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="分钟上限" prop="minute_limit">
          <el-input-number v-model="form.minute_limit" :min="1" :max="9999" style="width:100%" placeholder="每分钟发送上限"/>
        </el-form-item>
        <el-form-item label="日上限" prop="day_limit">
          <el-input-number v-model="form.day_limit" :min="1" :max="99999" style="width:100%" placeholder="每日发送上限"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" maxlength="500" placeholder="请输入备注" type="textarea" :rows="2"/>
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
const BASE = '/sms/biz'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      channelOptions: [], channelMap: {}, stOptions: [], stMap: {},
      query: { pageNum: 1, pageSize: 10, biz_code: undefined, biz_name: undefined, status: undefined },
      form: { biz_id: undefined, biz_code: '', biz_name: '', st_id: '', backup_channel_id: null, minute_limit: 5, day_limit: 100, status: '0', remark: '' },
      rules: {
        biz_code: [{ required: true, message: '业务编码不能为空', trigger: 'blur' }],
        biz_name: [{ required: true, message: '业务名称不能为空', trigger: 'blur' }],
        st_id: [{ required: true, message: '请选择签名模板', trigger: 'change' }]
      }
    }
  },
  created() { this.loadRefs(); this.getList() },
  methods: {
    loadRefs() {
      request({ url: '/sms/channel/list', method: 'get', params: { pageNum: 1, pageSize: 999 } }).then(r => {
        this.channelOptions = r.rows || []
        this.channelOptions.forEach(ch => { this.channelMap[ch.channel_id] = ch.channel_name })
      }).catch(() => {})
      request({ url: '/sms/signtemplate/list', method: 'get', params: { pageNum: 1, pageSize: 999 } }).then(r => {
        this.stOptions = r.rows || []
        this.stOptions.forEach(st => { this.stMap[st.st_id] = st.sign_name + ' / ' + st.template_code })
      }).catch(() => {})
    },
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      }).catch(() => { this.loading = false })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, biz_code: undefined, biz_name: undefined, status: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { biz_id: undefined, biz_code: '', biz_name: '', st_id: '', backup_channel_id: null, minute_limit: 5, day_limit: 100, status: '0', remark: '' }
      this.title = '新增业务绑定'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改业务绑定'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除业务绑定「' + row.biz_name + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.biz_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.biz_id ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
