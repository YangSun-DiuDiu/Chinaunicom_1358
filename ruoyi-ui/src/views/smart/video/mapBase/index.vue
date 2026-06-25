<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="query" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="底图名称" prop="map_name">
        <el-input v-model="query.map_name" placeholder="请输入名称" clearable style="width:200px" @keyup.enter.native="getList"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd">新增底图</el-button>
      <el-button icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column label="底图名称" prop="map_name" align="center" show-overflow-tooltip/>
      <el-table-column label="底图图片" align="center">
        <template slot-scope="{row}">
          <el-image v-if="row.map_image" :src="baseUrl + row.map_image" style="width:120px;height:68px;border-radius:4px" fit="cover" :preview-src-list="[baseUrl + row.map_image]"/>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="底图尺寸" align="center">
        <template slot-scope="{row}">
          <span v-if="row.map_width && row.map_height">{{ row.map_width }} × {{ row.map_height }}</span>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="create_by" align="center"/>
      <el-table-column label="创建时间" align="center">
        <template slot-scope="{row}">{{ parseTime(row.create_time) }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" align="center" show-overflow-tooltip/>
      <el-table-column label="操作" align="center" width="120">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="openEdit(row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click="handleDel(row)"/>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>

    <!-- 新增/修改弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" size="small">
        <el-form-item label="底图名称" prop="map_name">
          <el-input v-model="form.map_name" placeholder="请输入底图名称" maxlength="100"/>
        </el-form-item>
        <el-form-item label="底图图片" prop="map_image">
          <el-upload class="map-upload" :action="uploadUrl" :headers="headers" :file-list="fileList" list-type="picture-card" :limit="1" :on-success="onUploadSuccess" :on-remove="onUploadRemove" :before-upload="beforeUpload" accept="image/*">
            <i class="el-icon-plus"/>
          </el-upload>
          <span style="color:#909399;font-size:12px">建议上传与设备点位尺寸匹配的平面图</span>
        </el-form-item>
        <el-form-item label="底图尺寸">
          <el-row :gutter="10">
            <el-col :span="11"><el-input v-model.number="form.map_width" placeholder="宽度(px)"><template slot="prepend">宽</template></el-input></el-col>
            <el-col :span="2" style="text-align:center;line-height:36px">×</el-col>
            <el-col :span="11"><el-input v-model.number="form.map_height" placeholder="高度(px)"><template slot="prepend">高</template></el-input></el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="选填" maxlength="200" :rows="2"/>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit" :loading="submitting">确 定</el-button>
        <el-button @click="open=false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'
import { getToken } from '@/utils/auth'

export default {
  name: 'MapBase',
  data() {
    return {
      loading: false, total: 0, list: [], open: false, submitting: false, title: '',
      baseUrl: process.env.VUE_APP_BASE_API,
      uploadUrl: process.env.VUE_APP_BASE_API + '/common/upload',
      headers: { Authorization: 'Bearer ' + getToken() },
      fileList: [],
      query: { pageNum: 1, pageSize: 10, map_name: undefined },
      form: { map_id: undefined, map_name: '', map_image: '', map_width: 1920, map_height: 1080, remark: '' },
      rules: {
        map_name: [{ required: true, message: '底图名称不能为空', trigger: 'blur' }],
        map_image: [{ required: true, message: '请上传底图图片', trigger: 'change' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    parseTime,
    getList() {
      this.loading = true
      const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize }
      if (this.query.map_name) params.map_name = this.query.map_name
      request({ url: '/smart/access/map/list', method: 'get', params }).then(r => {
        this.list = r.rows || []; this.total = r.total || 0; this.loading = false
      }).catch(() => { this.loading = false })
    },
    resetQuery() { this.query = { pageNum: 1, pageSize: 10, map_name: undefined }; this.getList() },
    openAdd() {
      this.title = '新增底图'
      this.form = { map_id: undefined, map_name: '', map_image: '', map_width: 1920, map_height: 1080, remark: '' }
      this.fileList = []
      this.open = true
      this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() })
    },
    openEdit(row) {
      this.title = '修改底图'
      this.form = { ...row, map_id: row.map_id }
      this.fileList = row.map_image ? [{ name: '底图', url: this.baseUrl + row.map_image }] : []
      this.open = true
      this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() })
    },
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      if (!isImage) { this.$message.error('只能上传图片文件'); return false }
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isLt5M) { this.$message.error('图片大小不能超过5MB'); return false }
      return true
    },
    onUploadSuccess(res) {
      if (res.code === 200) {
        this.form.map_image = res.fileName || res.url || ''
        this.$message.success('上传成功')
      } else {
        this.$message.error(res.msg || '上传失败')
      }
    },
    onUploadRemove() { this.form.map_image = '' },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        this.submitting = true
        const data = { map_name: this.form.map_name, map_image: this.form.map_image, map_width: this.form.map_width, map_height: this.form.map_height, remark: this.form.remark }
        const method = this.form.map_id ? 'put' : 'post'
        if (this.form.map_id) data.map_id = this.form.map_id
        request({ url: '/smart/access/map', method, data }).then(() => {
          this.$message.success(this.form.map_id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        }).finally(() => { this.submitting = false })
      })
    },
    handleDel(row) {
      this.$confirm('确认删除底图【' + row.map_name + '】吗？', '提示', { type: 'warning' }).then(() => {
        request({ url: '/smart/access/map/' + row.map_id, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      }).catch(() => {})
    }
  }
}
</script>
<style scoped>
.map-upload .el-upload--picture-card { width: 148px; height: 148px; line-height: 148px; }
</style>
