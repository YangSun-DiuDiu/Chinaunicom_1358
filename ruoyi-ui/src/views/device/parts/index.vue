<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="配件名称"><el-input v-model="query.partName" placeholder="配件名称" style="width:180px" @keyup.enter.native="getList"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
      <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增配件</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-s-data" size="mini" @click="usageDialog=true">使用记录</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="配件名称" prop="part_name" min-width="140"/>
      <el-table-column label="编码" prop="part_code" width="120"/><el-table-column label="型号" prop="part_model" width="120"/>
      <el-table-column label="单位" prop="unit" width="60"/><el-table-column label="库存数量" prop="quantity" width="100"><template slot-scope="{row}"><el-tag :type="row.quantity <= row.alert_quantity ? 'danger':'success'">{{row.quantity}}</el-tag></template></el-table-column>
      <el-table-column label="预警数量" prop="alert_quantity" width="100"/><el-table-column label="单价" prop="price" width="80"/>
      <el-table-column label="操作" width="150"><template slot-scope="{row}"><el-button size="mini" type="text" @click="handleEdit(row)">修改</el-button><el-button size="mini" type="text" @click="handleDel(row)">删除</el-button></template></el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px"><el-form :model="form" label-width="90px" size="small">
      <el-form-item label="配件名称"><el-input v-model="form.part_name"/></el-form-item>
      <el-form-item label="编码"><el-input v-model="form.part_code"/></el-form-item>
      <el-form-item label="型号"><el-input v-model="form.part_model"/></el-form-item>
      <el-form-item label="单位"><el-select v-model="form.unit"><el-option label="个" value="个"/><el-option label="米" value="米"/><el-option label="卷" value="卷"/><el-option label="套" value="套"/></el-select></el-form-item>
      <el-form-item label="库存数量"><el-input-number v-model="form.quantity" :min="0"/></el-form-item>
      <el-form-item label="预警数量"><el-input-number v-model="form.alert_quantity" :min="0"/></el-form-item>
      <el-form-item label="单价"><el-input-number v-model="form.price" :min="0" :precision="2"/></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea"/></el-form-item>
    </el-form><div slot="footer"><el-button type="primary" @click="submit">确定</el-button></div></el-dialog>
    <!-- 使用记录 -->
    <el-dialog title="配件使用记录" :visible.sync="usageDialog" width="900px"><el-table :data="usageList" size="small"><el-table-column label="工单编号" prop="repair_no" width="120"/><el-table-column label="配件名称" prop="part_name"/><el-table-column label="数量" prop="quantity" width="80"/><el-table-column label="使用人" prop="used_by" width="100"/><el-table-column label="时间" prop="used_time" width="160"/></el-table><pagination :total="usageTotal" :page.sync="usagePage" @pagination="loadUsage"/></el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const B = '/device/parts'
export default { data() { return { loading:false, showSearch:true, total:0, list:[], open:false, title:'', usageDialog:false, usageList:[], usageTotal:0, usagePage:1, query:{ pageNum:1, pageSize:10, partName:undefined }, form:{ part_name:'', part_code:'', part_model:'', unit:'个', quantity:0, alert_quantity:5, price:0, remark:'' } } }, created() { this.getList() }, methods: { getList() { this.loading=true; request({url:B+'/list',method:'get',params:this.query}).then(r=>{this.list=r.rows;this.total=r.total;this.loading=false}) }, reset() { this.query={ pageNum:1, pageSize:10 }; this.getList() }, handleAdd() { this.form={ part_name:'', part_code:'', part_model:'', unit:'个', quantity:0, alert_quantity:5, price:0, remark:'' }; this.title='新增配件'; this.open=true }, handleEdit(r) { this.form={...r}; this.title='修改配件'; this.open=true }, handleDel(r) { this.$confirm('确认删除?').then(()=>request({url:B+'/'+r.part_id,method:'delete'}).then(()=>{this.$message.success('删除成功');this.getList()})).catch(()=>{}) }, submit() { const m=this.form.part_id?'put':'post'; request({url:B,method:m,data:this.form}).then(()=>{this.$message.success('操作成功');this.open=false;this.getList()}) }, loadUsage() { request({url:B+'/usage/list',params:{ pageNum:this.usagePage, pageSize:10 }}).then(r=>{this.usageList=r.rows;this.usageTotal=r.total}) } }, watch: { usageDialog(v) { if(v) this.loadUsage() } } }
</script>
