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
        { id: 2004, name: '访客管理',   path: '/visitor/appointment', pattern: 'p-visitor',    sub: '预约 · 审批 · 登记 · 记录' },
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

/* ====== 2个几何抽象图案（纯CSS，零图片） ====== */
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

/* ====== 响应式 ====== */
@media (max-width:900px) { .portal-grid { grid-template-columns:repeat(2, 1fr); } }
@media (max-width:600px) { .portal-grid { grid-template-columns:1fr; } .portal { padding:20px 14px; } }
</style>
