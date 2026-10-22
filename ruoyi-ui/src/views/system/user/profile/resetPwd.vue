<template>
  <el-form ref="form" :model="user" :rules="formRules" label-width="88px" class="notion-form">
    <el-form-item label="旧密码" prop="oldPassword">
      <el-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" show-password/>
    </el-form-item>
    <el-form-item label="新密码" prop="newPassword" :rules="infoPwdValidator">
      <el-input v-model="user.newPassword" placeholder="请输入新密码" type="password" show-password/>
    </el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password/>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">确认修改</el-button>
      <el-button plain @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserPwd } from "@/api/system/user"
import passwordRule from "@/utils/passwordRule"

export default {
  mixins: [passwordRule],
  data() {
    return {
      user: {
        oldPassword: undefined,
        newPassword: undefined,
        confirmPassword: undefined
      }
    }
  },
  computed: {
    formRules() {
      return {
        oldPassword: [
          { required: true, message: "旧密码不能为空", trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, message: "确认密码不能为空", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (this.user.newPassword !== value) {
                callback(new Error("两次输入的密码不一致"))
              } else {
                callback()
              }
            }, trigger: "blur"
          }
        ]
      }
    }
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateUserPwd(this.user.oldPassword, this.user.newPassword).then(() => {
            this.$modal.msgSuccess("修改成功")
          })
        }
      })
    },
    close() {
      this.$tab.closePage()
    }
  }
}
</script>

<style lang="scss" scoped>
.notion-form {
  max-width: 480px;

  ::v-deep .el-form-item {
    margin-bottom: 22px;
  }

  ::v-deep .el-form-item__label {
    font-size: 13px;
    color: #5d5b54;
    line-height: 40px;
  }

  ::v-deep .el-input__inner {
    height: 40px;
    line-height: 40px;
    border-radius: 8px;
    border-color: #c8c4be;
    color: #37352f;

    &:hover {
      border-color: #a4a097;
    }

    &:focus {
      border-color: #5645d4;
    }
  }

  ::v-deep .el-input__inner::placeholder {
    color: #bbb8b1;
  }

  ::v-deep .el-button--primary {
    border-radius: 8px;
  }

  ::v-deep .el-button--default {
    border-radius: 8px;
    border-color: #c8c4be;
    color: #37352f;

    &:hover {
      border-color: #5645d4;
      color: #5645d4;
    }
  }
}
</style>
