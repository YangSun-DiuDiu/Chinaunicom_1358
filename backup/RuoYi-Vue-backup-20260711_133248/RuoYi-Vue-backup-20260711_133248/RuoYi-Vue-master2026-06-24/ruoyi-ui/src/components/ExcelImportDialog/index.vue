<template>
  <el-dialog title="导入" :visible.sync="visible" width="500px" append-to-body>
    <el-upload
      ref="upload"
      :limit="1"
      accept=".xlsx, .xls"
      :headers="headers"
      :action="action"
      :disabled="uploading"
      :on-progress="handleFileUploadProgress"
      :on-success="handleFileSuccess"
      :auto-upload="false"
      drag
    >
      <i class="el-icon-upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
    </el-upload>
    <div slot="footer">
      <el-button type="primary" @click="submitFileForm">确 定</el-button>
      <el-button @click="visible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  name: 'ExcelImportDialog',
  props: {
    uploadAction: { type: String, default: '' }
  },
  data() {
    return {
      visible: false,
      uploading: false,
      headers: { Authorization: 'Bearer ' + getToken() },
      action: this.uploadAction
    }
  },
  methods: {
    show() { this.visible = true },
    submitFileForm() { this.$refs.upload.submit() },
    handleFileUploadProgress() { this.uploading = true },
    handleFileSuccess(response) {
      this.uploading = false
      this.$refs.upload.clearFiles()
      this.$emit('success', response)
      this.visible = false
    }
  }
}
</script>
