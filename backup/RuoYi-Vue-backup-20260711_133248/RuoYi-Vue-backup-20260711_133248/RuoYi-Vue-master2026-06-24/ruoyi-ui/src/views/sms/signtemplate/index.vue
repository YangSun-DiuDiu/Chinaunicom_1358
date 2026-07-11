<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="签名">
        <el-input v-model="query.sign_name" placeholder="签名" clearable style="width:160px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="模板CODE">
        <el-input v-model="query.template_code" placeholder="模板CODE" clearable style="width:180px" @keyup.enter.native="getList"/>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sms:signtemplate:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list" border stripe size="small">
      <el-table-column label="渠道" prop="channel_id" min-width="120" align="center">
        <template slot-scope="{row}">
          {{ channelMap[row.channel_id] || row.channel_id }}
        </template>
      </el-table-column>
      <el-table-column label="签名" prop="sign_name" min-width="120" :show-overflow-tooltip="true"/>
      <el-table-column label="模板CODE" prop="template_code" min-width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="参数映射" prop="template_param_mapping" min-width="200" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="80" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="create_time" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="120" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['sms:signtemplate:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['sms:signtemplate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <el-form-item label="关联渠道" prop="channel_id">
          <el-select v-model="form.channel_id" placeholder="请选择渠道" style="width:100%">
            <el-option v-for="ch in channelOptions" :key="ch.channel_id" :label="ch.channel_name" :value="ch.channel_id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="签名" prop="sign_name">
          <el-input v-model="form.sign_name" maxlength="100" placeholder="如: 智慧园区"/>
        </el-form-item>
        <el-form-item label="模板CODE" prop="template_code">
          <el-input v-model="form.template_code" maxlength="100" placeholder="如: SMS_123456789"/>
        </el-form-item>
        <el-form-item label="参数映射" prop="template_param_mapping">
          <el-input v-model="form.template_param_mapping" maxlength="1000" placeholder="JSON参数映射规则" type="textarea" :rows="3"/>
        </el-form-item>
        <el-form-item label="用途说明" prop="template_desc">
          <el-input v-model="form.template_desc" maxlength="500" placeholder="模板用途描述"/>
        </el-form-item>
        <el-form-item label="模板正文" prop="template_content">
          <el-input v-model="form.template_content" maxlength="500" placeholder="如: 您的验证码${code}，有效期5分钟" type="textarea" :rows="2"/>
        </el-form-item>
        <el-form-item label="参数说明" prop="template_param_desc">
          <el-input v-model="form.template_param_desc" maxlength="500" placeholder="如: code:验证码, device_name:设备名"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">禁用</el-radio>
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
const BASE = '/sms/signtemplate'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      channelOptions: [], channelMap: {},
      query: { pageNum: 1, pageSize: 10, sign_name: undefined, template_code: undefined, status: undefined },
      form: { st_id: undefined, channel_id: '', sign_name: '', template_code: '', template_content: '', template_param_desc: '', template_param_mapping: '', template_desc: '', status: '0' },
      rules: {
        channel_id: [{ required: true, message: '请选择渠道', trigger: 'change' }],
        sign_name: [{ required: true, message: '签名不能为空', trigger: 'blur' }],
        template_code: [{ required: true, message: '模板CODE不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.loadChannels(); this.getList() },
  methods: {
    loadChannels() {
      request({ url: '/sms/channel/list', method: 'get', params: { pageNum: 1, pageSize: 999 } }).then(r => {
        this.channelOptions = r.rows || []
        this.channelOptions.forEach(ch => { this.channelMap[ch.channel_id] = ch.channel_name })
      }).catch(() => {})
    },
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      }).catch(() => { this.loading = false })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, sign_name: undefined, template_code: undefined, status: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { st_id: undefined, channel_id: '', sign_name: '', template_code: '', template_param_mapping: '', template_desc: '', status: '0' }
      this.title = '新增签名模板'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改签名模板'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除签名模板「' + row.sign_name + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.st_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.st_id ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
