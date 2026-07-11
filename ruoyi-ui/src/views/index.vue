<template>
  <div class="portal">
    <div class="portal-bg"></div>
    <div class="portal-header">
      <h2>SMART PARK</h2>
      <p>智慧园区 · 综合管理平台</p>
    </div>
    <div class="portal-grid">
      <div
        v-for="card in cards"
        :key="card.id"
        class="portal-card"
        @click="enterModule(card.path)"
      >
        <div :class="['card-pattern', card.pattern]"></div>
        <div class="card-title">{{ card.name }}</div>
        <div class="card-sub">{{ card.sub }}</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PortalIndex',
  data() {
    return {
      cards: [
        { id: 1,    name: '系统管理',   path: '/system/user',         pattern: 'p-admin',      sub: '菜单 · 角色 · 用户 · 部门 · 岗位 · 参数' },
        { id: 2000, name: '设备管理',   path: '/device/list',         pattern: 'p-device',     sub: 'IoT设备 · 拓扑 · 心跳 · 故障 · SNMP · 维修' },
        { id: 2009, name: '智能化管理', path: '/smart/person-access-device', pattern: 'p-smart', sub: '门禁 · 车行 · 视频监控' },
        { id: 2004, name: '访客管理',   path: '/visitor/appointment', pattern: 'p-visitor',    sub: '预约 · 审批 · 登记 · 记录' },
        { id: 2021, name: '考勤管理',   path: '/attendance/import',   pattern: 'p-attendance', sub: '数据对接 · 考勤统计' },
        { id: 2,    name: '系统监控',   path: '/monitor/operlog',     pattern: 'p-monitor',    sub: '操作日志 · 登录日志 · 数据监控 · 缓存' },
        { id: 2132, name: '短信中台',   path: '/sms/channel',         pattern: 'p-sms',        sub: '渠道 · 模板 · 业务 · 黑名单 · 日志' },
        { id: 2031, name: '会议管理',   path: '/meeting/room',        pattern: 'p-meeting',    sub: '会议室 · 预约 · 管理 · 审批' },
        { id: 2113, name: '公寓管理',   path: '/hostel/apartment',    pattern: 'p-hostel',     sub: '房源 · 房间 · 卡片 · 租客' },
        { id: 2100, name: '通知公告',   path: '/system/notice',       pattern: 'p-notice',     sub: '公告发布 · 消息推送' },
      ]
    }
  },
  methods: {
    enterModule(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
/* ====== 门户容器 ====== */
.portal {
  position: relative; margin: -20px;
  min-height: calc(100vh - 84px);
  background: linear-gradient(135deg, #0a1628 0%, #162240 50%, #0d1f3c 100%);
  padding: 36px 30px; overflow: auto;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

/* 暗纹网格背景 */
.portal-bg {
  position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image:
    linear-gradient(rgba(0,212,255,0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,212,255,0.025) 1px, transparent 1px);
  background-size: 30px 30px;
}

.portal-header { position:relative; z-index:1; text-align:center; margin-bottom:28px; }
.portal-header h2 { font-size:22px; font-weight:300; color:rgba(255,255,255,0.5); letter-spacing:6px; margin:0 0 6px; }
.portal-header p { font-size:13px; color:rgba(255,255,255,0.3); letter-spacing:3px; margin:0; }

/* ====== 3列卡片网格 ====== */
.portal-grid {
  position:relative; z-index:1;
  display:grid;
  grid-template-columns:repeat(3, 1fr);
  gap:18px; max-width:1050px; margin:0 auto;
}

/* ====== 卡片 ====== */
.portal-card {
  position:relative; height:170px; border-radius:12px;
  cursor:pointer; overflow:hidden;
  display:flex; flex-direction:column; justify-content:flex-end; padding:20px;
  border:1px solid rgba(255,255,255,0.06);
  background:rgba(20,28,40,0.6);
  transition:all 0.35s cubic-bezier(0.4,0,0.2,1);
}
.portal-card:hover { transform:translateY(-4px); border-color:rgba(0,212,255,0.3); }
.portal-card:hover .card-pattern { opacity:1; }

.card-title {
  position:relative; z-index:1; font-size:17px; font-weight:600; color:#fff;
  letter-spacing:1px; margin-bottom:4px;
}
.card-sub {
  position:relative; z-index:1; font-size:11px; color:rgba(255,255,255,0.45);
  letter-spacing:0.5px;
}

/* ====== 10个几何抽象图案（纯CSS，零图片） ====== */
.card-pattern { position:absolute; inset:0; opacity:0.5; transition:opacity 0.35s; }

/* 系统管理：嵌套方块 */
.p-admin {
  background:
    repeating-conic-gradient(rgba(84,112,198,0.06) 0% 2%, transparent 2% 8%) 50% 30% / 80px 80px,
    linear-gradient(135deg, rgba(84,112,198,0.1) 0%, rgba(40,60,130,0.03) 100%);
}
.p-admin::before {
  content:''; position:absolute; top:20%; left:30%; right:50%; bottom:30%;
  border:1.5px solid rgba(84,112,198,0.15); border-radius:3px;
  box-shadow:8px 8px 0 rgba(84,112,198,0.08), 16px 16px 0 rgba(84,112,198,0.04);
}

/* 设备管理：电路板网格 */
.p-device {
  background:
    linear-gradient(0deg, transparent 48%, rgba(0,200,100,0.08) 48%, rgba(0,200,100,0.08) 49%, transparent 49%),
    linear-gradient(90deg, transparent 48%, rgba(0,200,100,0.1) 48%, rgba(0,200,100,0.1) 49%, transparent 49%),
    linear-gradient(135deg, rgba(0,200,100,0.12) 0%, rgba(0,160,80,0.04) 100%);
  background-size:30px 30px, 60px 60px, 100% 100%;
}
.p-device::before {
  content:''; position:absolute; top:15%; left:55%; right:25%; bottom:20%;
  border:1px solid rgba(0,200,100,0.08); border-radius:4px;
}
.p-device::after {
  content:''; position:absolute; top:35%; left:30%;
  width:50px; height:1px; background:rgba(0,200,100,0.2);
  box-shadow:0 25px 0 rgba(0,200,100,0.1), 0 50px 0 rgba(0,200,100,0.06);
}

/* 智能化管理：雷达扫描 */
.p-smart {
  background:
    conic-gradient(from 0deg, rgba(255,145,0,0.12) 0deg, transparent 30deg),
    radial-gradient(circle at 70% 30%, rgba(255,145,0,0.08) 0%, transparent 50%),
    linear-gradient(135deg, rgba(255,145,0,0.08) 0%, rgba(180,100,0,0.02) 100%);
}
.p-smart::after {
  content:''; position:absolute; top:20%; right:15%;
  width:60px; height:60px; border-radius:50%;
  border:1px solid rgba(255,145,0,0.2);
  box-shadow:0 0 20px rgba(255,145,0,0.05);
}

/* 访客管理：闸机竖线 */
.p-visitor {
  background:
    linear-gradient(125deg, transparent 60%, rgba(64,158,255,0.08) 60%, rgba(64,158,255,0.08) 62%, transparent 62%),
    linear-gradient(135deg, rgba(64,158,255,0.1) 0%, rgba(30,100,200,0.04) 100%);
}
.p-visitor::before {
  content:''; position:absolute; top:25%; bottom:25%; right:25%;
  width:3px; background:rgba(64,158,255,0.15);
  box-shadow:-15px 0 0 rgba(64,158,255,0.1), -30px 0 0 rgba(64,158,255,0.06), -45px 0 0 rgba(64,158,255,0.03);
}

/* 考勤管理：日历方格 */
.p-attendance {
  background:
    repeating-linear-gradient(0deg, transparent, transparent 13px, rgba(240,160,0,0.04) 13px, rgba(240,160,0,0.04) 14px),
    repeating-linear-gradient(90deg, transparent, transparent 40px, rgba(240,160,0,0.04) 40px, rgba(240,160,0,0.04) 41px),
    linear-gradient(135deg, rgba(240,160,0,0.07) 0%, rgba(180,120,0,0.02) 100%);
}

/* 系统监控：心电图波形 */
.p-monitor {
  background:linear-gradient(135deg, rgba(232,69,60,0.06) 0%, rgba(30,30,40,0) 100%);
}
.p-monitor::after {
  content:''; position:absolute; top:20%; left:15%; right:15%; bottom:60%;
  border-bottom:1px solid rgba(232,69,60,0.15);
  clip-path:polygon(
    0% 100%, 8% 80%, 12% 100%, 16% 30%, 20% 90%, 25% 100%,
    30% 75%, 35% 100%, 40% 20%, 45% 90%, 50% 100%,
    55% 85%, 60% 100%, 70% 40%, 75% 90%, 80% 100%,
    85% 75%, 90% 100%, 95% 85%, 100% 100%
  );
}

/* 短信中台：波纹扩散 */
.p-sms {
  background:
    radial-gradient(circle at 65% 28%, rgba(24,144,255,0.15) 0%, transparent 10%),
    radial-gradient(circle at 65% 28%, rgba(24,144,255,0.08) 10%, transparent 20%),
    radial-gradient(circle at 65% 28%, rgba(24,144,255,0.04) 20%, transparent 30%),
    linear-gradient(135deg, rgba(24,144,255,0.08) 0%, rgba(10,60,130,0.02) 100%);
}

/* 会议管理：环形会议桌 */
.p-meeting {
  background:
    radial-gradient(ellipse 55% 60% at 55% 40%, transparent 30%, rgba(0,180,130,0.06) 30%, rgba(0,180,130,0.06) 32%, transparent 32%),
    radial-gradient(ellipse 40% 45% at 55% 40%, transparent 28%, rgba(0,180,130,0.04) 28%, rgba(0,180,130,0.04) 30%, transparent 30%),
    linear-gradient(135deg, rgba(0,180,130,0.07) 0%, rgba(0,100,80,0.02) 100%);
}

/* 公寓管理：建筑窗户网格 */
.p-hostel {
  background:
    repeating-linear-gradient(0deg, transparent, transparent 14px, rgba(160,120,200,0.04) 14px, rgba(160,120,200,0.04) 15px),
    repeating-linear-gradient(90deg, transparent, transparent 28px, rgba(160,120,200,0.04) 28px, rgba(160,120,200,0.04) 29px),
    linear-gradient(135deg, rgba(160,120,200,0.07) 0%, rgba(80,40,140,0.02) 100%);
}
.p-hostel::before {
  content:''; position:absolute; top:15%; left:15%; right:55%; bottom:30%;
  border:1px solid rgba(160,120,200,0.1);
  box-shadow:40px 0 0 rgba(160,120,200,0.05), inset 0 20px 0 rgba(160,120,200,0.04);
}

/* 通知公告：公告板 */
.p-notice {
  background:
    repeating-linear-gradient(0deg, transparent, transparent 18px, rgba(250,200,88,0.03) 18px, rgba(250,200,88,0.03) 19px),
    linear-gradient(135deg, rgba(250,200,88,0.08) 0%, rgba(180,140,40,0.02) 100%);
}
.p-notice::before {
  content:''; position:absolute; top:18%; left:50%; right:25%; bottom:35%;
  border:1px solid rgba(250,200,88,0.1); border-radius:3px;
  background:linear-gradient(180deg, rgba(250,200,88,0.06) 0%, transparent 100%);
}

/* ====== 响应式 ====== */
@media (max-width:900px) { .portal-grid { grid-template-columns:repeat(2, 1fr); } }
@media (max-width:600px) { .portal-grid { grid-template-columns:1fr; } .portal { padding:20px 14px; } }
</style>
