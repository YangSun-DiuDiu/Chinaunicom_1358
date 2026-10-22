<template>
  <div class="app-container notion-profile">
    <div class="profile-layout">
      <!-- 左：个人信息卡 -->
      <div class="profile-card profile-summary">
        <div class="profile-head">
          <userAvatar />
          <div class="profile-nickname">{{ user.nickName || user.userName }}</div>
          <div class="profile-sub" v-if="user.dept">{{ user.dept.deptName }}<template v-if="postGroup"> · {{ postGroup }}</template></div>
          <div class="profile-tags">
            <span class="profile-tag" v-if="roleGroup">{{ roleGroup }}</span>
            <span class="profile-tag">{{ user.phonenumber || '未绑定手机' }}</span>
          </div>
        </div>
        <el-divider />
        <div class="profile-fields">
          <div class="pf-row">
            <span class="pf-label">用户名称</span>
            <span class="pf-value">{{ user.userName }}</span>
          </div>
          <div class="pf-row">
            <span class="pf-label">手机号码</span>
            <span class="pf-value">{{ user.phonenumber || '-' }}</span>
          </div>
          <div class="pf-row">
            <span class="pf-label">用户邮箱</span>
            <span class="pf-value">{{ user.email || '-' }}</span>
          </div>
          <div class="pf-row">
            <span class="pf-label">所属部门</span>
            <span class="pf-value" v-if="user.dept">{{ user.dept.deptName }}</span>
            <span class="pf-value" v-else>-</span>
          </div>
          <div class="pf-row">
            <span class="pf-label">所属角色</span>
            <span class="pf-value">{{ roleGroup || '-' }}</span>
          </div>
          <div class="pf-row">
            <span class="pf-label">创建日期</span>
            <span class="pf-value">{{ user.createTime || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 右：编辑卡 -->
      <div class="profile-card profile-edit">
        <div class="profile-card-title">个人资料</div>
        <el-tabs v-model="selectedTab" class="profile-tabs">
          <el-tab-pane label="基本资料" name="userinfo">
            <userInfo :user="user" />
          </el-tab-pane>
          <el-tab-pane label="修改密码" name="resetPwd">
            <resetPwd />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script>
import userAvatar from "./userAvatar"
import userInfo from "./userInfo"
import resetPwd from "./resetPwd"
import { getUserProfile } from "@/api/system/user"

export default {
  name: "Profile",
  components: { userAvatar, userInfo, resetPwd },
  data() {
    return {
      user: {},
      roleGroup: {},
      postGroup: {},
      selectedTab: "userinfo"
    }
  },
  created() {
    const activeTab = this.$route.params && this.$route.params.activeTab
    if (activeTab) {
      this.selectedTab = activeTab
    }
    this.getUser()
  },
  methods: {
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data
        this.roleGroup = response.roleGroup
        this.postGroup = response.postGroup
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.notion-profile {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px;
}

.profile-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;

  .profile-summary {
    width: 320px;
    flex-shrink: 0;
  }

  .profile-edit {
    flex: 1;
    min-width: 0;
  }
}

.profile-card {
  background: #ffffff;
  border: 1px solid #e5e3df;
  border-radius: 12px;
  padding: 24px;
}

.profile-head {
  text-align: center;
}

.profile-nickname {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 600;
  color: #37352f;
  line-height: 1.4;
}

.profile-sub {
  margin-top: 2px;
  font-size: 13px;
  color: #787671;
  line-height: 1.5;
}

.profile-tags {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-tag {
  display: inline-block;
  background: #e6e0f5;
  color: #391c57;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  border-radius: 6px;
  padding: 3px 8px;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-fields {
  .pf-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #ede9e4;

    &:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }
  }

  .pf-label {
    font-size: 13px;
    color: #787671;
    flex-shrink: 0;
  }

  .pf-value {
    font-size: 14px;
    color: #37352f;
    font-weight: 500;
    text-align: right;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.profile-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #37352f;
  padding-bottom: 16px;
}

.profile-tabs {
  ::v-deep .el-tabs__header {
    margin-bottom: 20px;
    border-bottom: 1px solid #ede9e4;
  }

  ::v-deep .el-tabs__nav-wrap::after {
    height: 1px;
    background-color: #ede9e4;
  }

  ::v-deep .el-tabs__item {
    height: 40px;
    line-height: 40px;
    font-size: 14px;
    color: #787671;
    padding: 0 20px;

    &:hover {
      color: #37352f;
    }

    &.is-active {
      color: #37352f;
      font-weight: 600;
    }
  }

  ::v-deep .el-tabs__active-bar {
    height: 2px;
    background-color: #5645d4;
  }
}
</style>
