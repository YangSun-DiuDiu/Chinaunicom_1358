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
.h5-page { min-height:100vh; background:linear-gradient(135deg, #0A1530 0%, #1A2A52 60%, #5645D4 100%); padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.result-card { margin-top:20px; background:#fff; border:1px solid rgba(26,174,57,0.4);
  border-radius:12px; padding:16px; text-align:center; box-shadow: rgba(15,15,15,0.08) 0px 4px 12px 0px; }
.result-title { font-size:16px; color:#1AAE39; font-weight:700; margin-bottom:8px; }
.h5-page >>> .el-input__inner, .h5-page >>> .el-textarea__inner { background:#fff; border-color:#C8C4BE; color:#37352F; }
.h5-page >>> .el-form-item__label { color:#5D5B54; }
.h5-page >>> .el-button--primary { background:#5645D4; border-color:#5645D4; }
</style>
