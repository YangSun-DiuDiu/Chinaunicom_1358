<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}" :style="{ backgroundColor: sideTheme === 'theme-dark' && navType !== 3 ? variables.menuBackground : variables.menuLightBackground }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img class="sidebar-logo-img" src="@/assets/logo/logo-icon.png" alt="智慧园区">
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img class="sidebar-logo-img" src="@/assets/logo/logo-icon.png" alt="智慧园区">
        <span class="sidebar-logo-text">智慧园区</span>
      </router-link>
    </transition>
  </div>
</template>

<script>
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    variables() {
      return variables
    },
    sideTheme() {
      return this.$store.state.settings.sideTheme
    },
    navType() {
      return this.$store.state.settings.navType
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  height: 50px;
  line-height: 50px;
  background: #2b2f3a;
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    & .sidebar-logo-img {
      width: 30px;
      height: 30px;
      border-radius: 7px;
      vertical-align: middle;
      display: inline-block;
    }

    & .sidebar-logo-text {
      display: inline-block;
      margin: 0;
      margin-left: 8px;
      font-weight: 700;
      line-height: 50px;
      font-size: 16px;
      font-family: -apple-system, BlinkMacSystemFont, 'Inter', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
      vertical-align: middle;
      background: linear-gradient(90deg, #2F5B50, #5FA08F);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  &.collapse {
    .sidebar-logo-img {
      width: 34px;
      height: 34px;
    }
  }
}
</style>
