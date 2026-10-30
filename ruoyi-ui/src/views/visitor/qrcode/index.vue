<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable style="width: 200px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
          v-hasPermi="['visitor:qrcode:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-download" size="mini" @click="handleInit"
          v-hasPermi="['visitor:qrcode:add']">批量初始化</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['visitor:qrcode:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="qrcodeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="部门名称" align="center" key="deptName" prop="deptName" width="280"
        :show-overflow-tooltip="true" />
      <el-table-column label="登记码" align="center" key="registerCode" prop="registerCode" width="230">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.registerCode" size="small">{{ scope.row.registerCode }}</el-tag>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="登记地址" align="center" key="registerUrl" width="600">
        <template slot-scope="scope">
          <span style="color:#5645D4">{{ getRegisterUrl(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" key="enabled" width="110">
        <template slot-scope="scope">
          <el-switch :value="scope.row.enabled === '1'" @change="handleEnabledChange(scope.row, $event)"
            v-hasPermi="['visitor:qrcode:edit']"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" key="createTime" width="200">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width" >
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-qr-code" @click="showQrCode(scope.row)">二维码</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['visitor:qrcode:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['visitor:qrcode:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 二维码弹窗 -->
    <qr-code ref="qrcode" />

    <!-- 添加或修改配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="部门" prop="deptId">
          <treeselect v-model="form.deptId" :options="enabledDeptOptions" :show-count="true"
            :disabled="form.id != undefined" style="width: 100%" placeholder="请选择部门" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDeptQrcode, getDeptQrcode, addDeptQrcode, updateDeptQrcode, delDeptQrcode, initDeptQrcode } from "@/api/visitor/visitor"
import { deptTreeSelect } from "@/api/system/dept"
import QrCode from '@/components/QrCode'
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "VisitorDeptQrcode",
  components: { QrCode, Treeselect },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 配置表格数据
      qrcodeList: [],
      // 部门树选项
      deptOptions: [],
      // 启用部门树选项（表单选择用，过滤停用部门）
      enabledDeptOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        deptName: undefined,
        enabled: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        deptId: [
          { required: true, message: "请选择部门", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询配置列表 */
    getList() {
      this.loading = true
      listDeptQrcode(this.queryParams).then(response => {
        this.qrcodeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        deptId: undefined,
        enabled: '1'
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 拼接部门登记二维码地址（统一走 h5 入口，扫码后二选一） */
    getRegisterUrl(row) {
      return window.location.origin + '/h5/entry'
    },
    /** 显示二维码（传完整登记地址，确保二维码编码完整 URL） */
    showQrCode(row) {
      this.$refs.qrcode.show(window.location.origin + '/h5/entry')
    },
    /** 启用/停用切换 */
    handleEnabledChange(row, val) {
      const data = { id: row.id, enabled: val ? '1' : '0' }
      updateDeptQrcode(data).then(() => {
        row.enabled = data.enabled
        this.$modal.msgSuccess(val ? '已启用' : '已停用')
      }).catch(() => {
        // 失败时回滚开关显示状态
        this.$nextTick(() => this.getList())
      })
    },
    /** 批量初始化 */
    handleInit() {
      this.$modal.confirm('将为本园区尚未配置登记二维码的部门批量生成二维码，是否继续？').then(() => {
        return initDeptQrcode()
      }).then(res => {
        this.$modal.msgSuccess('批量初始化成功，本次生成 ' + (res.data || 0) + ' 个')
        this.getList()
      }).catch(() => { })
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.loadDeptTree()
      this.open = true
      this.title = "添加部门登记二维码"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getDeptQrcode(id).then(response => {
        this.form = response.data
        this.form.enabled = this.form.enabled === '1' ? '1' : '0'
        this.loadDeptTree()
        this.open = true
        this.title = "修改部门登记二维码"
      })
    },
    /** 加载部门树选项 */
    loadDeptTree() {
      if (this.deptOptions.length > 0) return
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
        this.enabledDeptOptions = this.filterDisabledDept(JSON.parse(JSON.stringify(response.data)))
      })
    },
    /** 过滤停用部门（表单部门树只展示启用部门） */
    filterDisabledDept(deptList) {
      return deptList.filter(dept => {
        if (dept.disabled) {
          return false
        }
        if (dept.children && dept.children.length) {
          dept.children = this.filterDisabledDept(dept.children)
        }
        return true
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != undefined) {
            updateDeptQrcode(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDeptQrcode(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids.join(",")
      this.$modal.confirm('是否确认删除选中的部门登记二维码配置？删除后该部门二维码将失效。').then(function () {
        return delDeptQrcode(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => { })
    }
  }
}
</script>
