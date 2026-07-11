<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="渠道名称">
        <el-input v-model="query.channel_name" placeholder="渠道名称" clearable style="width:180px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item label="渠道类型">
        <el-select v-model="query.channel_type" placeholder="渠道类型" clearable style="width:140px">
          <el-option label="阿里云" value="ALIYUN"/>
          <el-option label="腾讯云" value="TENCENT"/>
          <el-option label="API" value="API"/>
        </el-select>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sms:channel:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list" border stripe size="small">
      <el-table-column label="渠道名称" prop="channel_name" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="渠道类型" prop="channel_type" min-width="100" align="center">
        <template slot-scope="{row}">
          <el-tag size="small" :type="row.channel_type === 'ALIYUN' ? '' : row.channel_type === 'TENCENT' ? 'success' : 'warning'">{{ row.channel_type === 'ALIYUN' ? '阿里云' : row.channel_type === 'TENCENT' ? '腾讯云' : 'API' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AccessKey ID" prop="access_key_id" min-width="180" :show-overflow-tooltip="true"/>
      <el-table-column label="AccessKey Secret" prop="access_key_secret" min-width="140" align="center">
        <template slot-scope="{row}">
          <span v-if="row.access_key_secret">******</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="Endpoint" prop="endpoint" min-width="180" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="80" align="center">
        <template slot-scope="{row}">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="create_time" min-width="140" align="center"/>
      <el-table-column label="操作" min-width="120" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-message" @click="handleTest(row)">测试</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['sms:channel:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['sms:channel:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="130px" size="small">
        <el-form-item label="渠道名称" prop="channel_name">
          <el-input v-model="form.channel_name" maxlength="100" placeholder="请输入渠道名称"/>
        </el-form-item>
        <el-form-item label="渠道类型" prop="channel_type">
          <el-select v-model="form.channel_type" placeholder="请选择渠道类型" style="width:100%">
            <el-option label="阿里云" value="ALIYUN"/>
            <el-option label="腾讯云" value="TENCENT"/>
            <el-option label="API" value="API"/>
          </el-select>
        </el-form-item>
        <el-form-item label="AccessKey ID" prop="access_key_id">
          <el-input v-model="form.access_key_id" maxlength="200" placeholder="请输入AccessKey ID"/>
        </el-form-item>
        <el-form-item label="AccessKey Secret" prop="access_key_secret">
          <el-input v-model="form.access_key_secret" maxlength="500" placeholder="请输入AccessKey Secret" type="password" show-password/>
        </el-form-item>
        <el-form-item label="Endpoint" prop="endpoint">
          <el-input v-model="form.endpoint" maxlength="300" placeholder="请输入网关/接口地址"/>
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
    <!-- 测试发送 -->
    <el-dialog title="测试短信发送" :visible.sync="testOpen" width="450px" append-to-body>
      <el-form label-width="80px" size="small">
        <el-form-item label="渠道">{{ testChannel.channel_name }}</el-form-item>
        <el-form-item label="签名模板">
          <el-select v-model="testStId" placeholder="选择模板" style="width:100%">
            <el-option v-for="t in templates" :key="t.st_id" :label="t.sign_name + '(' + t.template_code + ')'" :value="t.st_id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="testPhone" placeholder="请输入测试手机号"/>
        </el-form-item>
        <el-form-item label="模板参数">
          <el-input v-model="testParams" placeholder='如: {"code":"123456"}' type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="testOpen=false">取消</el-button>
        <el-button type="primary" :disabled="!testPhone" @click="doTest">发送测试</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/sms/channel'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      query: { pageNum: 1, pageSize: 10, channel_name: undefined, channel_type: undefined, status: undefined },
      form: { channel_id: undefined, channel_name: '', channel_type: 'ALIYUN', access_key_id: '', access_key_secret: '', endpoint: '', status: '0', remark: '' },
      testOpen: false, testChannel: {}, testStId: undefined, testPhone: '', testParams: '', templates: [],
      rules: {
        channel_name: [{ required: true, message: '渠道名称不能为空', trigger: 'blur' }],
        channel_type: [{ required: true, message: '渠道类型不能为空', trigger: 'change' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      }).catch(() => { this.loading = false })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10, channel_name: undefined, channel_type: undefined, status: undefined }
      this.getList()
    },
    handleAdd() {
      this.form = { channel_id: undefined, channel_name: '', channel_type: 'ALIYUN', access_key_id: '', access_key_secret: '', endpoint: '', status: '0', remark: '' }
      this.title = '新增渠道'; this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }; this.title = '修改渠道'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除渠道「' + row.channel_name + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.channel_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    handleTest(row) {
      this.testChannel = row; this.testStId = undefined; this.testPhone = ''; this.testParams = ''
      // 加载可用模板列表
      request({ url: '/sms/signtemplate/list', method: 'get', params: { pageNum: 1, pageSize: 100 } }).then(r => {
        this.templates = r.rows || []; this.testOpen = true
      })
    },
    doTest() {
      const body = { phone: this.testPhone, params: this.testParams, stId: this.testStId }
      request({ url: BASE + '/test/' + this.testChannel.channel_id, method: 'post', data: body }).then(r => {
        if (r.code === 200) this.$message.success('测试短信发送成功'); else this.$message.error(r.msg || '发送失败')
      }).catch(() => { this.$message.error('发送异常') })
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.channel_id ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
