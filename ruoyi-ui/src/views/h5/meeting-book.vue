<template>
  <div class="h5-page">
    <div class="h5-nav"><el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button></div>
    <h3>预约会议</h3>
    <el-form ref="form" :model="form" label-position="top">
      <el-form-item label="会议主题" prop="title"><el-input v-model="form.title" placeholder="会议主题" /></el-form-item>
      <el-form-item label="主持人" prop="hostName"><el-input v-model="form.hostName" placeholder="主持人姓名" /></el-form-item>
      <el-form-item label="联系电话" prop="hostPhone"><el-input v-model="form.hostPhone" placeholder="联系电话" /></el-form-item>
      <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" placeholder="选择" style="width:100%" /></el-form-item>
      <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" placeholder="选择" style="width:100%" /></el-form-item>
      <el-button type="primary" size="large" long :loading="loading" @click="submit">提交预约</el-button>
    </el-form>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  data() {
    return {
      form: { title:'', hostName:'', hostPhone:'', startTime:null, endTime:null,
        roomId: this.$route.query.roomId ? Number(this.$route.query.roomId) : null, status:'PENDING' },
      loading: false
    }
  },
  methods: {
    submit() {
      this.loading = true
      request({ url:'/meeting/booking', method:'post', data:this.form }).then(() => {
        this.$message.success('预约成功')
        this.$router.back()
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.h5-page { min-height:100vh; background:#0a1a2e; color:#bcc9d4; padding:16px; }
.h5-nav { margin-bottom:8px; }
.h5-page h3 { color:#fff; margin:0 0 20px; }
.h5-page >>> .el-input__inner, .h5-page >>> .el-textarea__inner { background:rgba(0,0,0,0.3); border-color:rgba(0,180,255,0.2); color:#fff; }
.h5-page >>> .el-form-item__label { color:#8899aa; }
</style>
