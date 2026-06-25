<template>
  <el-dialog title="访客登记二维码" :visible.sync="visible" width="420px" append-to-body :close-on-click-modal="false">
    <div style="text-align:center;padding:20px">
      <div style="margin-bottom:16px;font-size:14px;color:#666">访客扫码自助登记</div>
      <img :src="qrUrl" alt="二维码" style="width:240px;height:240px;border:1px solid #eee;padding:8px" />
      <div style="margin-top:16px;font-size:13px;color:#999">或访问链接：</div>
      <div style="margin-top:8px;display:flex;align-items:center;justify-content:center;gap:8px">
        <el-input :value="registerUrl" readonly size="small" style="width:280px" />
        <el-button size="small" type="primary" @click="copyUrl">复制</el-button>
      </div>
      <div style="margin-top:12px">
        <el-button size="small" type="success" @click="downloadQr">下载二维码</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'QrCode',
  data() {
    const base = window.location.origin
    return {
      visible: false,
      registerUrl: base + '/h5-register',
      qrUrl: 'https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=' + encodeURIComponent(base + '/h5-register')
    }
  },
  methods: {
    show() {
      this.visible = true
    },
    copyUrl() {
      const input = document.createElement('input')
      input.value = this.registerUrl
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$message.success('链接已复制到剪贴板')
    },
    downloadQr() {
      const a = document.createElement('a')
      a.href = this.qrUrl
      a.download = '访客登记二维码.png'
      a.click()
    }
  }
}
</script>
