<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button></div>
    <h3>👤 访客自助登记</h3>
    <el-form ref="form" :model="form" :rules="rules" label-position="top">
      <el-form-item label="姓名" prop="visitorName"><el-input v-model="form.visitorName" placeholder="请输入姓名" /></el-form-item>
      <el-form-item label="电话" prop="visitorPhone"><el-input v-model="form.visitorPhone" placeholder="请输入电话" /></el-form-item>
      <el-form-item label="被访人" prop="hostName"><el-input v-model="form.hostName" placeholder="请输入被访人姓名" /></el-form-item>
      <el-form-item label="来访事由"><el-input v-model="form.visitReason" placeholder="请输入来访事由" type="textarea" :rows="2" /></el-form-item>
      <el-button type="primary" size="large" long :loading="loading" @click="submit">提交登记</el-button>
    </el-form>
    <div class="result-card" v-if="result">
      <div class="result-title">✅ 登记成功</div>
      <div>通行码：<b>{{ result.passCode }}</b></div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  data() {
    return {
      form: { visitorName:'', visitorPhone:'', hostName:'', visitReason:'', visitTime: null },
      rules: {
        visitorName: [{ required:true, message:'必填', trigger:'blur' }],
        visitorPhone: [{ required:true, message:'必填', trigger:'blur' }],
        hostName: [{ required:true, message:'必填', trigger:'blur' }]
      },
      loading: false, result: null
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        request({ url:'/visitor/h5/submit', method:'post', data:this.form }).then(res => {
          this.result = res.data
          this.$message.success('登记成功')
        }).finally(() => { this.loading = false })
      })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.result-card { margin-top:20px; background:rgba(0,230,118,0.1); border:1px solid rgba(0,230,118,0.3);
  border-radius:8px; padding:16px; text-align:center; }
.result-title { font-size:16px; color:#00e676; margin-bottom:8px; }
.h5-page >>> .el-input__inner, .h5-page >>> .el-textarea__inner { background:rgba(0,0,0,0.3); border-color:rgba(0,180,255,0.2); color:#fff; }
.h5-page >>> .el-form-item__label { color:#8899aa; }
</style>
