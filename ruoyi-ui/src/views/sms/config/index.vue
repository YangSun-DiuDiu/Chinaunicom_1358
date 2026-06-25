<template>
  <div class="app-container">
    <!-- 阿里云短信基础配置 -->
    <el-card>
      <div slot="header"><span>阿里云短信接口配置</span></div>
      <el-form :model="form" :rules="rules" ref="configForm" label-width="140px" size="small">
        <el-form-item label="AccessKey ID" prop="accessKeyId">
          <el-input v-model="form.accessKeyId" placeholder="请输入阿里云 AccessKey ID" maxlength="100" show-password />
        </el-form-item>
        <el-form-item label="AccessKey Secret" prop="accessKeySecret">
          <el-input v-model="form.accessKeySecret" placeholder="请输入阿里云 AccessKey Secret" maxlength="100" type="password" show-password />
        </el-form-item>
        <el-form-item label="短信签名" prop="signName">
          <el-input v-model="form.signName" placeholder="如: 智慧园区" maxlength="20" />
          <span style="color:#909399;font-size:12px">需在阿里云短信控制台已审核通过的签名名称</span>
        </el-form-item>
        <el-form-item label="区域ID" prop="regionId">
          <el-select v-model="form.regionId" placeholder="请选择区域" style="width: 280px">
            <el-option label="华东1 (杭州) cn-hangzhou" value="cn-hangzhou" />
            <el-option label="华东2 (上海) cn-shanghai" value="cn-shanghai" />
            <el-option label="华北1 (青岛) cn-qingdao" value="cn-qingdao" />
            <el-option label="华北2 (北京) cn-beijing" value="cn-beijing" />
            <el-option label="华南1 (深圳) cn-shenzhen" value="cn-shenzhen" />
            <el-option label="中国香港 cn-hongkong" value="cn-hongkong" />
          </el-select>
        </el-form-item>
        <el-form-item label="短信开关">
          <el-switch v-model="form.smsEnabled" active-text="开启" inactive-text="关闭" />
        </el-form-item>
        <el-form-item label="API接入域名">
          <el-input v-model="form.smsEndpoint" placeholder="默认: dysmsapi.aliyuncs.com" maxlength="100" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="savingConfig">保存配置</el-button>
          <el-button @click="testSend" :loading="testing">测试发送</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 短信模板配置 -->
    <el-card style="margin-top:15px">
      <div slot="header">
        <span>短信模板配置</span>
        <span style="color:#909399;font-size:12px;margin-left:10px">请填写阿里云短信控制台中已审核通过的模板CODE</span>
      </div>
      <el-form :model="tpl" label-width="160px" size="small">
        <el-form-item label="设备离线告警模板CODE">
          <el-input v-model="tpl.deviceOffline" placeholder="如: SMS_123456789" maxlength="30" style="width: 320px" />
          <span style="color:#909399;font-size:12px;margin-left:8px">
            模板变量: ${deviceName}, ${ipAddress}, ${time}
          </span>
        </el-form-item>
        <el-form-item label="设备上线通知模板CODE">
          <el-input v-model="tpl.deviceOnline" placeholder="如: SMS_123456789" maxlength="30" style="width: 320px" />
          <span style="color:#909399;font-size:12px;margin-left:8px">
            模板变量: ${deviceName}, ${ipAddress}, ${time}
          </span>
        </el-form-item>
        <el-form-item label="设备报修通知模板CODE">
          <el-input v-model="tpl.repair" placeholder="如: SMS_123456789" maxlength="30" style="width: 320px" />
          <span style="color:#909399;font-size:12px;margin-left:8px">
            模板变量: ${deviceName}, ${responsible}, ${phone}
          </span>
        </el-form-item>
        <el-form-item label="访客审批通知模板CODE">
          <el-input v-model="tpl.visitorApproval" placeholder="如: SMS_123456789" maxlength="30" style="width: 320px" />
          <span style="color:#909399;font-size:12px;margin-left:8px">
            模板变量: ${visitorName}, ${hostName}, ${visitTime}, ${passCode}
          </span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveTemplates" :loading="savingTpl">保存模板</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 测试发送弹窗 -->
    <el-dialog title="测试短信发送" :visible.sync="testDialogOpen" width="400px" append-to-body>
      <el-form :model="testForm" :rules="testRules" ref="testForm" label-width="100px" size="small">
        <el-form-item label="手机号码" prop="phoneNumber">
          <el-input v-model="testForm.phoneNumber" placeholder="请输入测试手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="confirmTestSend">确认发送</el-button>
        <el-button @click="testDialogOpen = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { updateConfig } from '@/api/system/config'

export default {
  name: 'SmsConfig',
  data() {
    const validatePhone = (rule, value, callback) => {
      if (!value) { callback(new Error('请输入手机号码')); return }
      if (!/^1[3-9]\d{9}$/.test(value)) { callback(new Error('请输入正确的手机号码')); return }
      callback()
    }
    return {
      savingConfig: false,
      savingTpl: false,
      testing: false,
      testDialogOpen: false,
      form: {
        accessKeyId: '',
        accessKeySecret: '',
        signName: '',
        regionId: 'cn-hangzhou',
        smsEnabled: true,
        smsEndpoint: 'dysmsapi.aliyuncs.com'
      },
      tpl: {
        deviceOffline: '',
        deviceOnline: '',
        repair: '',
        visitorApproval: ''
      },
      testForm: {
        phoneNumber: ''
      },
      rules: {
        signName: [
          { max: 20, message: '签名最长20个字符', trigger: 'blur' }
        ]
      },
      testRules: {
        phoneNumber: [
          { required: true, validator: validatePhone, trigger: 'blur' }
        ]
      }
    }
  },
  created() { this.loadConfig() },
  methods: {
    /** 加载全部配置 */
    loadConfig() {
      // 基础配置 (sms.config 是JSON对象，其他是独立key)
      request({ url: '/system/config/configKey/sms.config', method: 'get' }).then(res => {
        const cfg = this.parseJson(res.msg)
        if (cfg) {
          this.form.accessKeyId = cfg.accessKeyId || ''
          this.form.accessKeySecret = cfg.accessKeySecret || ''
          this.form.signName = cfg.signName || ''
          this.form.regionId = cfg.regionId || 'cn-hangzhou'
          this.form.smsEndpoint = cfg.smsEndpoint || 'dysmsapi.aliyuncs.com'
        }
      }).catch(() => {})
      // 短信开关
      request({ url: '/system/config/configKey/sms.enabled', method: 'get' }).then(res => {
        this.form.smsEnabled = res.msg !== 'false'
      }).catch(() => {})
      // 模板
      const tplKeys = ['sms.tpl.deviceOffline', 'sms.tpl.deviceOnline', 'sms.tpl.repair', 'sms.tpl.visitorApproval']
      tplKeys.forEach(k => {
        request({ url: '/system/config/configKey/' + k, method: 'get' }).then(res => {
          const v = res.msg
          if (k === 'sms.tpl.deviceOffline') this.tpl.deviceOffline = v || ''
          else if (k === 'sms.tpl.deviceOnline') this.tpl.deviceOnline = v || ''
          else if (k === 'sms.tpl.repair') this.tpl.repair = v || ''
          else if (k === 'sms.tpl.visitorApproval') this.tpl.visitorApproval = v || ''
        }).catch(() => {})
      })
    },
    parseJson(str) {
      if (!str || str === 'null' || str === 'undefined') return null
      try { return JSON.parse(str) } catch (e) { return null }
    },
    /** 保存基础配置 */
    saveConfig() {
      this.savingConfig = true
      const cfgJson = JSON.stringify({
        accessKeyId: this.form.accessKeyId,
        accessKeySecret: this.form.accessKeySecret,
        signName: this.form.signName,
        regionId: this.form.regionId,
        smsEndpoint: this.form.smsEndpoint
      })
      updateConfig({ configKey: 'sms.config', configValue: cfgJson }).then(() => {
        return updateConfig({ configKey: 'sms.enabled', configValue: this.form.smsEnabled ? 'true' : 'false' })
      }).then(() => {
        this.$message.success('短信配置保存成功')
        this.savingConfig = false
      }).catch(() => { this.savingConfig = false })
    },
    /** 保存模板 */
    saveTemplates() {
      this.savingTpl = true
      const items = [
        { key: 'sms.tpl.deviceOffline', value: this.tpl.deviceOffline },
        { key: 'sms.tpl.deviceOnline', value: this.tpl.deviceOnline },
        { key: 'sms.tpl.repair', value: this.tpl.repair },
        { key: 'sms.tpl.visitorApproval', value: this.tpl.visitorApproval }
      ]
      Promise.all(items.map(item => updateConfig({ configKey: item.key, configValue: item.value })))
        .then(() => { this.$message.success('模板保存成功') })
        .catch(() => {})
        .finally(() => { this.savingTpl = false })
    },
    /** 测试发送 - 弹窗 */
    testSend() {
      this.testForm.phoneNumber = ''
      this.testDialogOpen = true
    },
    /** 确认测试发送 */
    confirmTestSend() {
      this.$refs.testForm.validate(valid => {
        if (!valid) return
        this.testing = true
        this.testDialogOpen = false
        request({
          url: '/sms/config/testSend',
          method: 'post',
          data: { phoneNumber: this.testForm.phoneNumber }
        }).then(res => {
          this.$message.success('测试短信发送成功，请查看短信日志')
        }).catch(() => {
          this.$message.error('测试短信发送失败')
        }).finally(() => { this.testing = false })
      })
    }
  }
}
</script>
