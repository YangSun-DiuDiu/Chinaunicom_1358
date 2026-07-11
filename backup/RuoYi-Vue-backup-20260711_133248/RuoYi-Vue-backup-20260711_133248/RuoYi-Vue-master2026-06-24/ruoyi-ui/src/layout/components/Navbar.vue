<template>
  <div class="navbar" :class="'nav' + navType">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb v-if="navType == 1" id="breadcrumb-container" class="breadcrumb-container" />
    <top-nav v-if="navType == 2" id="topmenu-container" class="topmenu-container" />
    <template v-if="navType == 3">
      <logo v-show="showLogo" :collapse="false"></logo>
      <top-bar id="topbar-container" class="topbar-container" />
    </template>
    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <el-dropdown v-if="isAdmin" @command="handleSwitchDept" class="right-menu-item hover-effect dept-dropdown" trigger="click">
          <span class="dept-dropdown-trigger">
            <svg-icon icon-class="tree" />
            <span class="dept-name">{{ currentDeptName || '选择组织' }}</span>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="" icon="el-icon-refresh">清除组织上下文</el-dropdown-item>
            <el-dropdown-item divided v-for="dept in deptOptions" :key="dept.id" :command="String(dept.id)" :style="{ paddingLeft: (dept.level * 16 + 20) + 'px' }">
              {{ dept.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>

        <search id="header-search" class="right-menu-item" />

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>

      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="hover">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <span class="user-nickname"> {{ nickName }} </span>
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>个人中心</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setLayout" v-if="setting">
            <span>布局设置</span>
          </el-dropdown-item>
          <el-dropdown-item @click.native="lockScreen">
            <span>锁定屏幕</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from './TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import { switchDeptContext, clearDeptContext, deptTreeSelect } from '@/api/system/dept'

export default {
  components: {
    Breadcrumb,
    Logo,
    TopNav,
    TopBar,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search
  },
  data() {
    return {
      deptOptions: [],
      currentDeptName: ''
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'nickName',
      'roles'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      }
    },
    navType: {
      get() {
        return this.$store.state.settings.navType
      }
    },
    showLogo: {
      get() {
        return this.$store.state.settings.sidebarLogo
      }
    },
    isAdmin() {
      return this.roles.includes('admin')
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    setLayout(event) {
      this.$emit('setLayout')
    },
    lockScreen() {
      const currentPath = this.$route.fullPath
      this.$store.dispatch('lock/lockScreen', currentPath).then(() => {
        this.$router.push('/lock')
      })
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index'
        })
      }).catch(() => {})
    },
    /** 加载部门树下拉选项 */
    loadDeptOptions() {
      deptTreeSelect().then(response => {
        this.deptOptions = this.flattenDeptTree(response.data)
      })
    },
    /** 将部门树扁平化，记录层级用于缩进 */
    flattenDeptTree(tree, level) {
      if (!level) { level = 0 }
      let result = []
      if (Array.isArray(tree)) {
        tree.forEach(item => {
          result.push({ id: item.id, label: item.label, level: level })
          if (item.children && item.children.length > 0) {
            result = result.concat(this.flattenDeptTree(item.children, level + 1))
          }
        })
      }
      return result
    },
    /** 切换部门上下文 */
    handleSwitchDept(deptId) {
      if (deptId === '' || deptId === undefined || deptId === null) {
        clearDeptContext().then(() => {
          this.currentDeptName = ''
          this.$modal.msgSuccess('已清除组织上下文')
          this.refreshPage()
        })
      } else {
        switchDeptContext(deptId).then(() => {
          const dept = this.deptOptions.find(d => String(d.id) === String(deptId))
          if (dept) {
            this.currentDeptName = dept.label
          }
          this.$modal.msgSuccess('已切换组织上下文')
          this.refreshPage()
        })
      }
    },
    /** 刷新当前页面 */
    refreshPage() {
      this.$router.go(0)
    }
  },
  mounted() {
    if (this.isAdmin) {
      this.loadDeptOptions()
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar.nav3 {
  .hamburger-container {
    display: none !important;
  }
}

.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  align-items: center;
  // padding: 0 8px;
  box-sizing: border-box;

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-right: 8px;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    flex-shrink: 0;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .right-menu {
    height: 100%;
    line-height: 50px;
    display: flex;
    align-items: center;
    margin-left: auto;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025)
        }
      }
    }

    .dept-dropdown {
      font-size: 14px;

      .dept-dropdown-trigger {
        display: flex;
        align-items: center;
        gap: 4px;
        cursor: pointer;
        height: 100%;

        .dept-name {
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .avatar-container {
      margin-right: 0px;
      padding-right: 0px;

      .avatar-wrapper {
        margin-top: 10px;
        right: 8px;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 30px;
          height: 30px;
          border-radius: 50%;
        }

        .user-nickname{
          position: relative;
          bottom: 10px;
          left: 2px;
          font-size: 14px;
          font-weight: bold;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }
  }
}
</style>
