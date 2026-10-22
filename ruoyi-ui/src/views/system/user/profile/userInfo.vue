<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="88px" class="notion-form">
    <el-form-item label="用户昵称" prop="nickName">
      <el-input v-model="form.nickName" maxlength="30" placeholder="请输入昵称" />
    </el-form-item>
    <el-form-item label="手机号码" prop="phonenumber">
      <el-input v-model="form.phonenumber" maxlength="11" placeholder="请输入手机号码" />
    </el-form-item>
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="form.email" maxlength="50" placeholder="请输入邮箱地址" />
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="form.sex" class="notion-radio">
        <el-radio label="0">男</el-radio>
        <el-radio label="1">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">保存修改</el-button>
      <el-button plain @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateUserProfile } from "@/api/system/user"

export default {
  props: {
    user: {
      type: Object
    }
  },
  data() {
    return {
      form: {},
      // 表单校验
      rules: {
        nickName: [
          { required: true, message: "用户昵称不能为空", trigger: "blur" }
        ],
        email: [
          { required: true, message: "邮箱地址不能为空", trigger: "blur" },
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        phonenumber: [
          { required: true, message: "手机号码不能为空", trigger: "blur" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ]
      }
    }
  },
  watch: {
    user: {
      handler(user) {
        if (user) {
          this.form = { nickName: user.nickName, phonenumber: user.phonenumber, email: user.email, sex: user.sex }
        }
      },
      immediate: true
    }
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateUserProfile(this.form).then(() => {
            this.$modal.msgSuccess("修改成功")
            this.user.phonenumber = this.form.phonenumber
            this.user.email = this.form.email
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

  .notion-radio {
    ::v-deep .el-radio__label {
      color: #37352f;
      font-size: 14px;
    }
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
