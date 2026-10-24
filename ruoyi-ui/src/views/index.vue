<template>
  <div class="portal">
    <div class="portal-header">
      <h2>智慧园区 · 综合管理平台</h2>
      <p class="portal-sub">SMART PARK MANAGEMENT SYSTEM</p>
      <div class="portal-accent"></div>
    </div>
    <div class="portal-grid">
      <div
        v-for="card in cards"
        :key="card.id"
        class="portal-card"
        @click="enterModule(card.path)"
      >
        <div :class="['card-pattern', card.pattern]"></div>
        <div class="card-icon">{{ card.icon }}</div>
        <div class="card-title">{{ card.name }}</div>
        <div class="card-sub">{{ card.sub }}</div>
        <div class="card-enter">进入模块 →</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PortalIndex',
  computed: {
    // 门户卡片按当前用户权限动态过滤：无权限的模块不显示，访客管理跳转到该用户有权限的首个菜单
    // 超级管理员（admin 角色或通配权限 *:*:*）直接显示全部卡片
    cards() {
      const perms = this.$store.getters.permissions || []
      const roles = this.$store.getters.roles || []
      const isAdmin = roles.includes('admin') || perms.includes('*:*:*')
      const hasAny = (arr) => isAdmin || arr.some((p) => perms.includes(p))
      const list = []

      // 系统管理：任一系统管理列表权限即显示（超管恒显示）
      if (hasAny(['system:user:list', 'system:role:list', 'system:dept:list'])) {
        list.push({ id: 1, name: '系统管理', path: '/system/user', pattern: 'p-admin', icon: '⚙', sub: '菜单 · 角色 · 用户 · 部门 · 岗位 · 参数' })
      }

      // 访客管理：任一访客权限即显示，入口优先预约→来访记录→现场登记→二维码（超管默认预约）
      if (hasAny(['visitor:appointment:list', 'visitor:log:list', 'visitor:register:add', 'visitor:qrcode:list', 'visitor:approval:list'])) {
        const path = isAdmin || perms.includes('visitor:appointment:list')
          ? '/visitor/appointment'
          : perms.includes('visitor:log:list')
            ? '/visitor/log'
            : perms.includes('visitor:register:add')
              ? '/visitor/register'
              : '/visitor/deptQrcode'
        list.push({ id: 2004, name: '访客管理', path, pattern: 'p-visitor', icon: '🏢', sub: '预约 · 审批 · 登记 · 记录' })
      }
      return list
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
/* ====== 门户容器 - Notion 浅色 ====== */
.portal {
  position: relative; margin: -20px;
  min-height: calc(100vh - 84px);
  background: #F6F5F4;
  padding: 48px 30px; overflow: auto;
  font-family: -apple-system, BlinkMacSystemFont, 'Inter', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.portal-header { text-align: center; margin-bottom: 40px; }
.portal-header h2 {
  font-size: 24px; font-weight: 600; color: #37352F;
  letter-spacing: 1px; margin: 0 0 8px;
}
.portal-sub { font-size: 12px; color: #787671; letter-spacing: 3px; margin: 0 0 12px; }
.portal-accent {
  width: 48px; height: 4px; margin: 0 auto;
  background: linear-gradient(90deg, #5645D4, #8B7CF6);
  border-radius: 9999px;
}

/* ====== 卡片网格 ====== */
.portal-grid {
  display:grid;
  grid-template-columns:repeat(3, 1fr);
  gap:20px; max-width:1050px; margin:0 auto;
}

/* ====== 卡片 - Notion card-base ====== */
.portal-card {
  position:relative; height:180px; border-radius:12px;
  cursor:pointer; overflow:hidden;
  display:flex; flex-direction:column; justify-content:flex-end; padding:24px;
  border:1px solid #E5E3DF;
  background:#FFFFFF;
  box-shadow: rgba(15, 15, 15, 0.04) 0px 1px 2px 0px;
  transition:all 0.3s ease;
}
.portal-card:hover {
  transform:translateY(-3px);
  box-shadow: rgba(15, 15, 15, 0.08) 0px 8px 24px 0px;
  border-color: #5645D4;
}

.card-icon {
  position:absolute; top:20px; right:20px;
  font-size:34px; opacity:0.85; z-index:1;
}

.card-title {
  position:relative; z-index:1; font-size:17px; font-weight:600; color:#37352F;
  letter-spacing:0.5px; margin-bottom:4px;
}
.card-sub {
  position:relative; z-index:1; font-size:12px; color:#787671;
  letter-spacing:0.3px;
}
.card-enter {
  position:relative; z-index:1; margin-top:12px;
  font-size:13px; font-weight:500; color:#5645D4;
  opacity:0; transform:translateY(4px);
  transition:all 0.3s ease;
}
.portal-card:hover .card-enter { opacity:1; transform:translateY(0); }

/* ====== 卡片背景图案（Pastel 浅色调） ====== */
.card-pattern { position:absolute; inset:0; opacity:0.6; }

/* 系统管理：浅紫嵌套方块 */
.p-admin {
  background:
    repeating-conic-gradient(rgba(86,69,212,0.04) 0% 2%, transparent 2% 8%) 50% 30% / 80px 80px,
    linear-gradient(135deg, rgba(86,69,212,0.05) 0%, rgba(230,224,245,0.4) 100%);
}
.p-admin::before {
  content:''; position:absolute; top:20%; left:30%; right:50%; bottom:30%;
  border:1.5px solid rgba(86,69,212,0.15); border-radius:3px;
  box-shadow:8px 8px 0 rgba(86,69,212,0.08), 16px 16px 0 rgba(86,69,212,0.04);
}

/* 访客管理：浅蓝闸机竖线 */
.p-visitor {
  background:
    linear-gradient(125deg, transparent 60%, rgba(0,117,222,0.06) 60%, rgba(0,117,222,0.06) 62%, transparent 62%),
    linear-gradient(135deg, rgba(0,117,222,0.06) 0%, rgba(220,236,250,0.5) 100%);
}
.p-visitor::before {
  content:''; position:absolute; top:25%; bottom:25%; right:25%;
  width:3px; background:rgba(0,117,222,0.15);
  box-shadow:-15px 0 0 rgba(0,117,222,0.10), -30px 0 0 rgba(0,117,222,0.06), -45px 0 0 rgba(0,117,222,0.03);
}

/* ====== 响应式 ====== */
@media (max-width:900px) { .portal-grid { grid-template-columns:repeat(2, 1fr); } }
@media (max-width:600px) { .portal-grid { grid-template-columns:1fr; } .portal { padding:24px 14px; } }
</style>
