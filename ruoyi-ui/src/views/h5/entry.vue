<template>
  <div class="h5-page">
    <div class="hero">
      <div class="hero-icon">▦</div>
      <h3>访客入场</h3>
      <p>欢迎来访，请选择您的到访方式</p>
    </div>

    <div class="entry-cards">
      <div class="entry-card" @click="goAppointments">
        <div class="card-icon">📋</div>
        <div class="card-title">已有预约</div>
        <div class="card-desc">查询我的预约记录<br/>并出示通行码</div>
        <div class="card-arrow">›</div>
      </div>

      <div class="entry-card alt" @click="goRegister">
        <div class="card-icon">✏️</div>
        <div class="card-title">新增预约</div>
        <div class="card-desc">填写来访信息<br/>提交预约申请</div>
        <div class="card-arrow">›</div>
      </div>
    </div>

    <div class="footer">
      <p>智慧园区管理系统</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'H5Entry',
  methods: {
    goAppointments() {
      this.$router.push('/h5/appointments')
    },
    goRegister() {
      this.$router.push('/h5/register')
    }
  }
}
</script>

<style scoped>
.h5-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0A1530 0%, #1A2A52 60%, #5645D4 100%);
  padding: 16px;
  display: flex;
  flex-direction: column;
}
.hero {
  text-align: center;
  padding: 48px 0 36px;
}
.hero-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #fff;
}
.hero h3 {
  color: #fff;
  margin: 0 0 8px;
  font-size: 22px;
}
.hero p {
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
  margin: 0;
}
.entry-cards {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 420px;
  width: 100%;
  margin: 0 auto;
}
.entry-card {
  position: relative;
  background: #fff;
  border-radius: 14px;
  padding: 22px 20px;
  cursor: pointer;
  box-shadow: rgba(15, 15, 15, 0.15) 0 4px 14px 0;
  transition: transform 0.15s ease;
}
.entry-card:active {
  transform: scale(0.98);
}
.entry-card .card-icon {
  font-size: 26px;
  margin-bottom: 8px;
}
.entry-card .card-title {
  font-size: 17px;
  font-weight: 700;
  color: #37352F;
  margin-bottom: 4px;
}
.entry-card .card-desc {
  font-size: 13px;
  color: #787671;
  line-height: 1.6;
}
.entry-card .card-arrow {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 24px;
  color: #C8C4BE;
}
.entry-card.alt {
  background: linear-gradient(135deg, #FFFFFF 0%, #F3F1FD 100%);
  border: 1px solid rgba(86, 69, 212, 0.25);
}
.entry-card.alt .card-arrow {
  color: #5645D4;
}
.footer {
  text-align: center;
  padding: 20px 0 8px;
}
.footer p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  margin: 0;
}
</style>
