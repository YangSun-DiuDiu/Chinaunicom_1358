<template>
  <div class="app-container tree-sidebar-manage-wrap">
    <tree-panel title="组织机构" :tree-data="deptOptions" search-placeholder="请输入部门名称" storage-key="dept-sidebar-width" :defaultExpandAll="true" @node-click="handleNodeClick" @refresh="getDeptTree" ref="deptTreeRef" />
    <div class="tree-sidebar-content">
      <div class="content-inner">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="用户名称" prop="userName">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
              <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:user:add']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:user:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:user:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport" v-hasPermi="['system:user:import']">导入</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:user:export']">导出</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-refresh" size="mini" @click="openSyncDialog" v-hasPermi="['system:user:import']">组织同步</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns.userId.visible" min-width="70" />
          <el-table-column label="用户名称" align="center" key="userName" v-if="columns.userName.visible" min-width="100" :show-overflow-tooltip="true">
            <template slot-scope="scope">
              <span>{{ scope.row.userName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="工号" align="center" key="jobNumber" prop="jobNumber" v-if="columns.jobNumber.visible" min-width="80" />
          <el-table-column label="昵称" align="center" key="nickName" prop="nickName" v-if="columns.nickName.visible" min-width="80" :show-overflow-tooltip="true" />
          <el-table-column label="别名" align="center" key="aliasName" prop="aliasName" v-if="columns.aliasName.visible" min-width="80" />
          <el-table-column label="外部来源" align="center" key="externalSource" v-if="columns.externalSource.visible" min-width="90">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.externalSource === 'DINGTALK'" type="primary" size="mini">钉钉</el-tag>
              <el-tag v-else-if="scope.row.externalSource === 'WECHAT'" type="success" size="mini">企微</el-tag>
              <span v-else style="color:#909399">手动</span>
            </template>
          </el-table-column>
          <el-table-column label="部门" align="center" key="deptName" v-if="columns.deptName.visible" min-width="100" :show-overflow-tooltip="true">
            <template slot-scope="scope"><span>{{ scope.row.dept ? scope.row.dept.deptName : scope.row.deptName || '' }}</span></template>
          </el-table-column>
          <el-table-column label="职位" align="center" key="position" prop="position" v-if="columns.position.visible" min-width="100" :show-overflow-tooltip="true" />
          <el-table-column label="手机号" align="center" key="phonenumber" prop="phonenumber" v-if="columns.phonenumber.visible" min-width="110" />
          <el-table-column label="座机" align="center" key="telephone" prop="telephone" v-if="columns.telephone.visible" min-width="110" />
          <el-table-column label="邮箱" align="center" key="email" prop="email" v-if="columns.email.visible" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="企业邮箱" align="center" key="orgEmail" prop="orgEmail" v-if="columns.orgEmail.visible" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="工作地点" align="center" key="workPlace" prop="workPlace" v-if="columns.workPlace.visible" min-width="100" />
          <el-table-column label="地址" align="center" key="address" prop="address" v-if="columns.address.visible" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column label="入职日期" align="center" key="hireDate" prop="hireDate" v-if="columns.hireDate.visible" min-width="100" />
          <el-table-column label="状态" align="center" key="status" v-if="columns.status.visible" min-width="70">
            <template slot-scope="scope">
              <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)"></el-switch>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns.createTime.visible" min-width="140">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="照片" align="center" min-width="80">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.photo" type="success" size="small">已上传</el-tag>
              <el-tag v-else type="info" size="small">未上传</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" min-width="180" class-name="small-padding fixed-width">
            <template slot-scope="scope" v-if="scope.row.userId !== 1">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:user:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:user:remove']">删除</el-button>
              <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:user:resetPwd', 'system:user:edit']">
                <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="handleResetPwd" icon="el-icon-key" v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>
                  <el-dropdown-item command="handleAuthRole" icon="el-icon-circle-check" v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </div>
    </div>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <treeselect v-model="form.deptId" :options="enabledDeptOptions" :show-count="true" placeholder="请选择归属部门" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password" :rules="pwdValidator">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工号" prop="jobNumber">
              <el-input v-model="form.jobNumber" placeholder="请输入工号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="别名" prop="aliasName">
              <el-input v-model="form.aliasName" placeholder="请输入别名" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择性别">
                <el-option v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择岗位">
                <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" :disabled="item.status == 1" ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" :disabled="item.status == 1"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="人员照片">
              <el-upload :action="photoUploadUrl" :headers="photoHeaders" :on-success="onPhotoSuccess" :before-upload="onPhotoBefore" :show-file-list="false" accept="image/*" style="display:inline-block;margin-right:10px">
                <el-button size="small" type="primary" icon="el-icon-upload2">上传照片</el-button>
              </el-upload>
              <img v-if="form.photo" :src="form.photo" style="width:60px;height:60px;border-radius:4px;border:1px solid #ddd;vertical-align:middle;object-fit:cover" />
              <span v-if="form.photo" style="margin-left:8px;font-size:12px;color:#67c23a">已上传</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 用户详情抽屉 -->
    <user-view-drawer ref="userViewRef" />
    <!-- 用户导入对话框 -->
    <excel-import-dialog ref="importUserRef" title="用户导入" action="/system/user/importData" template-action="/system/user/importTemplate" template-file-name="user_template" update-support-label="是否更新已经存在的用户数据" @success="getList" />

    <!-- 组织架构同步弹窗 -->
    <el-dialog title="组织架构同步（钉钉/企业微信）" :visible.sync="showSyncDialog" width="900px" :close-on-click-modal="false">
      <el-tabs v-model="syncTab">
        <el-tab-pane label="钉钉 (DingTalk)" name="dingtalk">
          <!-- API密钥配置 -->
          <el-card header="钉钉API配置" shadow="hover" style="margin-bottom:12px">
            <el-form :model="apiConfig" label-width="90px" size="small">
              <el-form-item label="API地址">
                <el-input v-model="apiConfig['dingtalk.api_url']" placeholder="默认: https://oapi.dingtalk.com" style="width:100%" />
              </el-form-item>
              <el-form-item label="AppKey">
                <el-input v-model="apiConfig['dingtalk.app_key']" placeholder="钉钉应用AppKey" />
              </el-form-item>
              <el-form-item label="AppSecret">
                <el-input v-model="apiConfig['dingtalk.app_secret']" placeholder="钉钉应用AppSecret" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="mini" @click="saveApiConfig" :loading="configSaving">保存配置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          <el-alert title="数据格式说明" type="info" :closable="false" style="margin-bottom:12px">
            <p><b>方式一（推荐）：</b>填写上方AppKey/AppSecret后，点击"API一键同步"自动拉取钉钉组织架构。</p>
            <p><b>方式二：</b>粘贴钉钉导出的JSON数据进行手动导入。</p>
          </el-alert>
          <el-button type="primary" icon="el-icon-cloud-upload" @click="apiSyncDingtalk" :loading="syncApiLoading" style="margin-bottom:12px" size="small">
            API一键同步
          </el-button>
          <el-collapse style="margin-bottom:12px">
            <el-collapse-item title="📋 钉钉JSON格式示例（点击展开）" name="1">
              <pre style="background:#1e1e1e;color:#d4d4d4;padding:10px;border-radius:4px;font-size:11px;max-height:300px;overflow:auto">{
  "departments": [
    { "dept_id": 1, "name": "总公司", "parent_id": 0, "dept_code": "HQ" },
    { "dept_id": 2, "name": "技术部", "parent_id": 1, "dept_code": "TECH" }
  ],
  "users": [
    { "userid": "zhangsan", "name": "张三", "dept_id_list": [2],
      "job_number": "EMP001", "mobile": "13800001111",
      "email": "zs@example.com", "org_email": "zhangsan@company.com",
      "tel": "010-12345678", "title": "高级工程师",
      "work_place": "杭州", "hire_date": "2024-01-15" },
    { "userid": "lisi", "name": "李四", "dept_id_list": [2],
      "job_number": "EMP002", "mobile": "13900002222",
      "email": "ls@example.com", "title": "前端开发",
      "work_place": "北京", "hire_date": "2023-06-01" }
  ]
}</pre>
            </el-collapse-item>
          </el-collapse>
          <el-input v-model="syncDataDingtalk" type="textarea" :rows="10" placeholder="粘贴钉钉组织架构JSON数据..." />
          <div style="margin-top:10px;text-align:right">
            <el-button @click="previewSync('DINGTALK')" :loading="syncPreviewing">预览</el-button>
            <el-button type="primary" @click="doOrgSync('DINGTALK')" :loading="syncImporting">导入钉钉组织架构</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="企业微信 (WeCom)" name="wecom">
          <!-- API密钥配置 -->
          <el-card header="企业微信API配置" shadow="hover" style="margin-bottom:12px">
            <el-form :model="apiConfig" label-width="90px" size="small">
              <el-form-item label="API地址">
                <el-input v-model="apiConfig['wecom.api_url']" placeholder="默认: https://qyapi.weixin.qq.com/cgi-bin" style="width:100%" />
              </el-form-item>
              <el-form-item label="企业CorpID">
                <el-input v-model="apiConfig['wecom.corp_id']" placeholder="企业微信CorpID" />
              </el-form-item>
              <el-form-item label="应用Secret">
                <el-input v-model="apiConfig['wecom.secret']" placeholder="自建应用Secret" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="mini" @click="saveApiConfig" :loading="configSaving">保存配置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          <el-alert title="数据格式说明" type="info" :closable="false" style="margin-bottom:12px">
            <p><b>方式一（推荐）：</b>填写上方CorpID/Secret后，点击"API一键同步"自动拉取企业微信组织架构。</p>
            <p><b>方式二：</b>粘贴企业微信导出的JSON数据进行手动导入。</p>
          </el-alert>
          <el-button type="success" icon="el-icon-cloud-upload" @click="apiSyncWecom" :loading="syncApiLoading" style="margin-bottom:12px" size="small">
            API一键同步
          </el-button>
          <el-collapse style="margin-bottom:12px">
            <el-collapse-item title="📋 企业微信JSON格式示例（点击展开）" name="1">
              <pre style="background:#1e1e1e;color:#d4d4d4;padding:10px;border-radius:4px;font-size:11px;max-height:300px;overflow:auto">{
  "departments": [
    { "id": 1, "name": "总公司", "parentid": 0, "order": 1, "dept_code": "HQ" },
    { "id": 2, "name": "技术部", "parentid": 1, "order": 2, "dept_code": "TECH" }
  ],
  "users": [
    { "userid": "zhangsan", "name": "张三", "department": [2],
      "mobile": "13800001111", "email": "zs@example.com",
      "telephone": "010-87654321", "alias": "ZhangSan",
      "position": "高级工程师", "gender": "1",
      "address": "浙江省杭州市", "avatar": "https://example.com/avatar.jpg" },
    { "userid": "lisi", "name": "李四", "department": [2],
      "mobile": "13900002222", "email": "ls@example.com",
      "telephone": "021-66668888", "alias": "LiSi",
      "position": "前端开发", "gender": "2",
      "address": "北京市朝阳区" }
  ]
}</pre>
            </el-collapse-item>
          </el-collapse>
          <el-input v-model="syncDataWecom" type="textarea" :rows="10" placeholder="粘贴企业微信组织架构JSON数据..." />
          <div style="margin-top:10px;text-align:right">
            <el-button @click="previewSync('WECHAT')" :loading="syncPreviewing">预览</el-button>
            <el-button type="primary" @click="doOrgSync('WECHAT')" :loading="syncImporting">导入企业微信组织架构</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
      <!-- 预览结果 -->
      <div v-if="syncPreviewResult" style="margin-top:15px">
        <el-alert :title="'预览: ' + syncPreviewResult.deptCount + '个部门, ' + syncPreviewResult.userCount + '个用户'" type="success" :closable="false" />
      </div>
      <!-- 导入结果 -->
      <div v-if="syncResult" style="margin-top:15px">
        <el-descriptions :column="5" border size="mini">
          <el-descriptions-item label="部门导入">{{ syncResult.deptImported || 0 }}</el-descriptions-item>
          <el-descriptions-item label="部门跳过">{{ syncResult.deptSkipped || 0 }}</el-descriptions-item>
          <el-descriptions-item label="用户导入">{{ syncResult.userImported || 0 }}</el-descriptions-item>
          <el-descriptions-item label="用户跳过">{{ syncResult.userSkipped || 0 }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ syncResult.costMs }}ms</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listUser, getUser, delUser, addUser, updateUser, resetUserPwd, changeUserStatus, deptTreeSelect } from "@/api/system/user"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"
import TreePanel from "@/components/TreePanel"
import ExcelImportDialog from "@/components/ExcelImportDialog"
import UserViewDrawer from "./view"
import passwordRule from "@/utils/passwordRule"
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

export default {
  name: "User",
  mixins: [passwordRule],
  dicts: ['sys_normal_disable', 'sys_user_sex'],
  components: { Treeselect, TreePanel, ExcelImportDialog, UserViewDrawer },
  data() {
    const base = process.env.VUE_APP_BASE_API || '/dev-api'
    return {
      // 照片上传
      photoUploadUrl: base + '/common/upload',
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
      // 用户表格数据
      userList: null,
      // 弹出层标题
      title: "",
      // 所有部门树选项
      deptOptions: undefined,
      // 过滤掉已禁用部门树选项
      enabledDeptOptions: undefined,
      // 是否显示弹出层
      open: false,
      // 默认密码
      initPassword: undefined,
      // 日期范围
      dateRange: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 表单参数
      form: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      },
      // 列信息
      columns: {
        userId: { label: '用户编号', visible: true },
        userName: { label: '用户名称', visible: true },
        jobNumber: { label: '工号', visible: true },
        nickName: { label: '昵称', visible: true },
        aliasName: { label: '别名', visible: false },
        externalSource: { label: '外部来源', visible: true },
        deptName: { label: '部门', visible: true },
        position: { label: '职位', visible: true },
        phonenumber: { label: '手机号', visible: true },
        telephone: { label: '座机', visible: false },
        email: { label: '邮箱', visible: false },
        orgEmail: { label: '企业邮箱', visible: false },
        workPlace: { label: '工作地点', visible: false },
        address: { label: '地址', visible: false },
        hireDate: { label: '入职日期', visible: false },
        status: { label: '状态', visible: true },
        createTime: { label: '创建时间', visible: true }
      },
      // 组织同步
      showSyncDialog: false,
      syncTab: 'dingtalk',
      syncDataDingtalk: '',
      syncDataWecom: '',
      syncPreviewing: false,
      syncImporting: false,
      syncApiLoading: false,
      configSaving: false,
      apiConfig: {
        'dingtalk.api_url': '', 'dingtalk.app_key': '', 'dingtalk.app_secret': '',
        'wecom.api_url': '', 'wecom.corp_id': '', 'wecom.secret': ''
      },
      syncPreviewResult: null,
      syncResult: null,
      // 表单校验
      rules: {
        userName: [
          { required: true, message: "用户名称不能为空", trigger: "blur" },
          { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: "用户昵称不能为空", trigger: "blur" }
        ],
        email: [
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        phonenumber: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ]
      }
    }
  },
  created() {
    this.getList()
    this.getDeptTree()
    this.getConfigKey("sys.user.initPassword").then(response => {
      this.initPassword = response.msg
    })
  },
  computed: {
    photoHeaders() { return { Authorization: 'Bearer ' + (getToken() || '') } }
  },
  methods: {
    /** 查询用户列表 */
    getList() {
      this.loading = true
      listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.userList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
        this.enabledDeptOptions = this.filterDisabledDept(JSON.parse(JSON.stringify(response.data)))
      })
    },
    // 过滤禁用的部门
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
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.handleQuery()
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用"
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(function() {
        return changeUserStatus(row.userId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0"
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
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        jobNumber: undefined,
        aliasName: undefined,
        nickName: undefined,
        password: undefined,
        phonenumber: undefined,
        email: undefined,
        sex: undefined,
        status: "0",
        remark: undefined,
        postIds: [],
        roleIds: []
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
      this.dateRange = []
      this.resetForm("queryForm")
      this.queryParams.deptId = undefined
      this.$refs.deptTreeRef.setCurrentKey(null)
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case "handleResetPwd":
          this.handleResetPwd(row)
          break
        case "handleAuthRole":
          this.handleAuthRole(row)
          break
        default:
          break
      }
    },
    /** 照片上传回调 */
    onPhotoSuccess(res) {
      if (res.code === 200) {
        const url = res.data || res.url || res.fileName
        this.form.photo = url.startsWith('http') ? url : (process.env.VUE_APP_BASE_API + url)
        this.$message.success('照片上传成功')
      } else { this.$message.error(res.msg || '上传失败') }
    },
    onPhotoBefore(file) {
      const isImg = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isImg) { this.$message.error('只能上传图片文件'); return false }
      if (!isLt2M) { this.$message.error('照片大小不能超过2MB'); return false }
      return true
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      getUser().then(response => {
        this.postOptions = response.posts
        this.roleOptions = response.roles
        this.open = true
        this.title = "添加用户"
        this.form.password = this.initPassword
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const userId = row.userId || this.ids
      getUser(userId).then(response => {
        this.form = response.data
        this.postOptions = response.posts
        this.roleOptions = response.roles
        this.$set(this.form, "postIds", response.postIds)
        this.$set(this.form, "roleIds", response.roleIds)
        this.open = true
        this.title = "修改用户"
        this.form.password = ""
      })
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$prompt(`请输入「${row.userName}」的新密码`, "重置密码", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        closeOnClickModal: false,
        inputValidator: this.pwdPromptValidator
      }).then(({ value }) => {
        resetUserPwd(row.userId, value).then(() => {
          this.$modal.msgSuccess("修改成功，新密码是：" + value)
        })
      }).catch(() => {})
    },
    /** 分配角色操作 */
    handleAuthRole(row) {
      const userId = row.userId
      this.$router.push("/system/user-auth/role/" + userId)
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.userId != undefined) {
            updateUser(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addUser(this.form).then(() => {
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
      const userIds = row.userId || this.ids
      this.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function() {
        return delUser(userIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    },
    /** 详情按钮操作 */
    handleViewData(row) {
      this.$refs.userViewRef.open(row.userId)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.$refs.importUserRef.open()
    },
    // ===== 组织架构同步 =====
    openSyncDialog() {
      this.showSyncDialog = true
      this.loadApiConfig()
    },
    loadApiConfig() {
      request({ url: '/system/orgSync/apiConfig', method: 'get' }).then(res => {
        if (res.code === 200 && res.data) {
          Object.keys(this.apiConfig).forEach(k => {
            if (res.data[k] !== undefined) this.apiConfig[k] = res.data[k]
          })
        }
      })
    },
    saveApiConfig() {
      this.configSaving = true
      request({ url: '/system/orgSync/apiConfig', method: 'post', data: this.apiConfig }).then(res => {
        if (res.code === 200) {
          this.$message.success('API配置已保存')
        } else {
          this.$message.error(res.msg || '保存失败')
        }
        this.configSaving = false
      }).catch(() => { this.configSaving = false; this.$message.error('保存失败') })
    },
    apiSyncDingtalk() {
      this.syncApiLoading = true
      this.syncResult = null
      request({ url: '/system/orgSync/apiSync/dingtalk', method: 'post' }).then(res => {
        if (res.code === 200) {
          this.syncResult = res.data
          this.$message.success('钉钉API同步成功! 部门:' + res.data.deptImported + ' 用户:' + res.data.userImported)
          this.getList()
          this.getDeptTree()
        } else {
          this.$message.error(res.msg || 'API同步失败，请检查钉钉AppKey/AppSecret配置')
        }
        this.syncApiLoading = false
      }).catch(() => { this.syncApiLoading = false; this.$message.error('API同步失败，请检查钉钉AppKey/AppSecret配置') })
    },
    apiSyncWecom() {
      this.syncApiLoading = true
      this.syncResult = null
      request({ url: '/system/orgSync/apiSync/wecom', method: 'post' }).then(res => {
        if (res.code === 200) {
          this.syncResult = res.data
          this.$message.success('企业微信API同步成功! 部门:' + res.data.deptImported + ' 用户:' + res.data.userImported)
          this.getList()
          this.getDeptTree()
        } else {
          this.$message.error(res.msg || 'API同步失败，请检查企业微信CorpID/Secret配置')
        }
        this.syncApiLoading = false
      }).catch(() => { this.syncApiLoading = false; this.$message.error('API同步失败，请检查企业微信CorpID/Secret配置') })
    },
    getSyncData(platform) {
      return platform === 'WECHAT' ? this.syncDataWecom : this.syncDataDingtalk
    },
    previewSync(platform) {
      const raw = this.getSyncData(platform)
      if (!raw.trim()) { this.$message.warning('请先粘贴JSON数据'); return }
      let body
      try { body = JSON.parse(raw) } catch (e) { this.$message.error('JSON格式错误'); return }
      body.platform = platform
      this.syncPreviewing = true
      request({ url: '/system/orgSync/preview', method: 'post', data: body }).then(res => {
        if (res.code === 200) {
          this.syncPreviewResult = res.data
          this.$message.success('解析成功: ' + res.data.deptCount + '个部门, ' + res.data.userCount + '个用户')
        } else {
          this.$message.error(res.msg || '解析失败')
        }
        this.syncPreviewing = false
      }).catch(() => { this.syncPreviewing = false })
    },
    doOrgSync(platform) {
      const raw = this.getSyncData(platform)
      if (!raw.trim()) { this.$message.warning('请先粘贴JSON数据'); return }
      let body
      try { body = JSON.parse(raw) } catch (e) { this.$message.error('JSON格式错误'); return }
      this.syncImporting = true
      this.syncResult = null
      const url = platform === 'WECHAT' ? '/system/orgSync/wecom' : '/system/orgSync/dingtalk'
      request({ url, method: 'post', data: body }).then(res => {
        if (res.code === 200) {
          this.syncResult = res.data
          this.$message.success('导入成功! 部门:' + res.data.deptImported + ' 用户:' + res.data.userImported)
          this.getList()
          this.getDeptTree()
        } else {
          this.$message.error(res.msg || '导入失败')
        }
        this.syncImporting = false
      }).catch(() => { this.syncImporting = false })
    }
  }
}
</script>
