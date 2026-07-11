<template>
    <div :class="['sidebar-theme-wrapper', {'has-logo':showLogo}, settings.sideTheme]" :style="{ backgroundColor: settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
        <logo v-if="showLogo" :collapse="isCollapse" />
        <el-scrollbar :class="settings.sideTheme" wrap-class="scrollbar-wrapper">
            <el-menu
                :default-active="activeMenu"
                :collapse="isCollapse"
                :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
                :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
                :unique-opened="true"
                :active-text-color="settings.theme"
                :collapse-transition="false"
                mode="vertical"
            >
                <sidebar-item
                    v-for="(route, index) in filteredRouters"
                    :key="route.path  + index"
                    :item="route"
                    :base-path="route.path"
                />
            </el-menu>
        </el-scrollbar>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex"
import Logo from "./Logo"
import SidebarItem from "./SidebarItem"
import variables from "@/assets/styles/variables.scss"

export default {
    components: { SidebarItem, Logo },
    computed: {
        ...mapState(["settings"]),
        ...mapGetters(["sidebarRouters", "sidebar"]),
        // 模块门户过滤：进入模块后只显示该模块的子菜单
        filteredRouters() {
            const parentId = this.currentParentMenuId
            if (!parentId) {
                // 首页或无标记：显示完整菜单树
                return this.sidebarRouters
            }
            return this.sidebarRouters.filter(route => {
                // 保留隐藏路由（不影响侧边栏）
                if (route.hidden) return true
                // 直接匹配 parentMenuId
                if (route.meta && route.meta.parentMenuId === parentId) return true
                // 子路由中有匹配的 parentMenuId
                if (route.children) {
                    return route.children.some(c => c.meta && c.meta.parentMenuId === parentId)
                }
                return false
            })
        },
        // 从当前路由的 matched 链中查找 parentMenuId（父路由携带）
        currentParentMenuId() {
            const matched = this.$route.matched || []
            for (const m of matched) {
                if (m.meta && m.meta.parentMenuId) {
                    return m.meta.parentMenuId
                }
            }
            return null
        },
        activeMenu() {
            const route = this.$route
            const { meta, path } = route
            // if set path, the sidebar will highlight the path you set
            if (meta.activeMenu) {
                return meta.activeMenu
            }
            return path
        },
        showLogo() {
            return this.$store.state.settings.sidebarLogo
        },
        variables() {
            return variables
        },
        isCollapse() {
            return !this.sidebar.opened
        }
    }
}
</script>
