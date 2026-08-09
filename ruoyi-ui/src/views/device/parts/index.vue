<template>
  <div class="parts-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="parts-stats">
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-total">
          <div class="stat-num">{{ list.length }}</div>
          <div class="stat-label">配件种类</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-alert">
          <div class="stat-num">{{ lowStockCount }}</div>
          <div class="stat-label">库存预警</div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索 -->
    <el-form :model="query" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="配件名称">
        <el-input v-model="query.partName" placeholder="配件名称" style="width:180px" @keyup.enter.native="getList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增配件</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-s-data" size="mini" @click="usageDialog = true">使用记录</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list" stripe size="small">
      <el-table-column label="配件名称" prop="part_name" width="200" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="编码" prop="part_code" width="200" align="center" />
      <el-table-column label="型号" prop="part_model" width="200" align="center" />
      <el-table-column label="单位" prop="unit" width="60" align="center" />
      <el-table-column label="库存数量" prop="quantity" width="200" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.quantity <= row.alert_quantity ? 'danger' : 'success'" size="small">
            {{ row.quantity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预警数量" prop="alert_quantity" width="200" align="center" />
      <el-table-column label="单价" prop="price" width="200" align="center" />
      <el-table-column label="备注" prop="remark" min-width="200" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList" />

    <!-- 新增/修改弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body :close-on-click-modal="false">
      <el-form :model="form" label-width="90px" size="small">
        <el-form-item label="配件名称">
          <el-input v-model="form.part_name" placeholder="请输入配件名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.part_code" placeholder="请输入编码" maxlength="30" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.part_model" placeholder="请输入型号" maxlength="30" />
        </el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.unit" style="width:100%">
            <el-option label="个" value="个" />
            <el-option label="米" value="米" />
            <el-option label="卷" value="卷" />
            <el-option label="套" value="套" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存数量">
          <el-input-number v-model="form.quantity" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="预警数量">
          <el-input-number v-model="form.alert_quantity" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit">确定</el-button>
        <el-button @click="open = false">取消</el-button>
      </div>
    </el-dialog>

    <!-- 使用记录弹窗 -->
    <el-dialog title="配件使用记录" :visible.sync="usageDialog" width="900px" append-to-body>
      <el-table :data="usageList" size="small" stripe>
        <el-table-column label="工单编号" prop="repair_no" width="170" align="center" />
        <el-table-column label="配件名称" prop="part_name" min-width="170" align="center" />
        <el-table-column label="数量" prop="quantity" width="170" align="center" />
        <el-table-column label="使用人" prop="used_by" width="170" align="center" />
        <el-table-column label="时间" prop="used_time" width="170" align="center" />
      </el-table>
      <pagination :total="usageTotal" :page.sync="usagePage" @pagination="loadUsage" />
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

const BASE = '/device/parts'

export default {
  name: 'DeviceParts',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      open: false,
      title: '',
      usageDialog: false,
      usageList: [],
      usageTotal: 0,
      usagePage: 1,
      query: { pageNum: 1, pageSize: 10, partName: undefined },
      form: { part_name: '', part_code: '', part_model: '', unit: '个', quantity: 0, alert_quantity: 5, price: 0, remark: '' }
    }
  },
  computed: {
    lowStockCount() {
      return this.list.filter(item => item.quantity <= item.alert_quantity).length
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      request({ url: BASE + '/list', method: 'get', params: this.query }).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10 }
      this.getList()
    },
    handleAdd() {
      this.form = { part_name: '', part_code: '', part_model: '', unit: '个', quantity: 0, alert_quantity: 5, price: 0, remark: '' }
      this.title = '新增配件'
      this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.title = '修改配件'
      this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除该配件吗？').then(() => {
        request({ url: BASE + '/' + row.part_id, method: 'delete' }).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    submit() {
      const method = this.form.part_id ? 'put' : 'post'
      request({ url: BASE, method, data: this.form }).then(() => {
        this.$message.success('操作成功')
        this.open = false
        this.getList()
      })
    },
    loadUsage() {
      request({ url: BASE + '/usage/list', params: { pageNum: this.usagePage, pageSize: 10 } }).then(res => {
        this.usageList = res.rows
        this.usageTotal = res.total
      })
    }
  },
  watch: {
    usageDialog(val) {
      if (val) this.loadUsage()
    }
  }
}
</script>

<style scoped>
.parts-container {
  padding: 0;
}

.parts-stats {
  margin-bottom: 16px;
}

.stat-card {
  padding: 16px 20px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: transform 0.15s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-total .stat-num { color: #409eff; }
.stat-alert .stat-num { color: #f56c6c; }
</style>
