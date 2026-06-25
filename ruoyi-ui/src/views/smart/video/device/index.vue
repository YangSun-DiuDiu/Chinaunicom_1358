<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="设备名称"><el-input v-model="query.deviceName" placeholder="设备名称" style="width:180px" @keyup.enter.native="getList"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="设备名称" prop="device_name" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="品牌" prop="device_brand" min-width="100"><template slot-scope="{row}"><el-tag size="small">{{row.device_brand||'--'}}</el-tag></template></el-table-column>
      <el-table-column label="IP地址" prop="ip_address" min-width="130"/>
      <el-table-column label="RTSP端口" prop="rtsp_port" min-width="90"/>
      <el-table-column label="码流" prop="stream_type" min-width="80"/>
      <el-table-column label="位置" prop="location" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="90">
        <template slot-scope="{row}">
          <el-tag v-if="row.status==='ONLINE'" type="success" size="small">在线</el-tag>
          <el-tag v-else-if="row.status==='OFFLINE'" type="danger" size="small">离线</el-tag>
          <el-tag v-else type="info" size="small">{{row.status||'未知'}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="240"><template slot-scope="{row}">
        <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)">修改</el-button>
        <el-button size="mini" type="text" icon="el-icon-connection" @click="pingDevice(row)">在线测试</el-button>
        <el-button size="mini" type="text" icon="el-icon-video-play" @click="preview(row)">预览</el-button>
        <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)">删除</el-button>
      </template></el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form :model="form" label-width="100px" size="small">
        <el-form-item label="设备名称"><el-input v-model="form.device_name"/></el-form-item>
        <el-form-item label="品牌"><el-select v-model="form.device_brand" style="width:100%"><el-option label="海康威视" value="HIKVISION"/><el-option label="大华" value="DAHUA"/><el-option label="其他" value="OTHER"/></el-select></el-form-item>
        <el-form-item label="IP地址"><el-input v-model="form.ip_address"/></el-form-item>
        <el-form-item label="RTSP端口"><el-input-number v-model="form.rtsp_port" :min="1"/></el-form-item>
        <el-form-item label="RTSP地址"><el-input v-model="form.rtsp_url" placeholder="如: rtsp://ip:554/Streaming/Channels/101"/></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username"/></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password"/></el-form-item>
        <el-form-item label="安装位置"><el-input v-model="form.location"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea"/></el-form-item>
      </el-form>
      <div slot="footer"><el-button type="primary" @click="submit">确定</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const B = '/smart/access/video-device'
export default { data() { return { loading:false, showSearch:true, total:0, list:[], open:false, title:'', query:{ pageNum:1, pageSize:10, deviceName:undefined }, form:{ device_name:'', device_brand:'HIKVISION', ip_address:'', rtsp_port:554, rtsp_url:'', username:'admin', password:'', location:'' } } }, created() { this.getList() }, methods: { getList() { this.loading=true; request({url:B+'/list',method:'get',params:this.query}).then(r=>{this.list=r.rows;this.total=r.total;this.loading=false}) }, reset() { this.query={ pageNum:1, pageSize:10 }; this.getList() }, handleAdd() { this.form={ device_name:'', device_brand:'HIKVISION', ip_address:'', rtsp_port:554, rtsp_url:'', username:'admin', password:'', location:'' }; this.title='新增视频设备'; this.open=true }, handleEdit(r) { this.form={...r}; this.title='修改视频设备'; this.open=true }, handleDel(r) { this.$confirm('确认删除?').then(()=>request({url:B+'/'+r.device_id,method:'delete'}).then(()=>{this.$message.success('删除成功');this.getList()})).catch(()=>{}) }, submit() { const m=this.form.device_id?'put':'post'; request({url:B,method:m,data:this.form}).then(()=>{this.$message.success('操作成功');this.open=false;this.getList()}) }, preview(row) { const url = row.rtsp_url || 'rtsp://'+row.ip_address+':'+(row.rtsp_port||554)+'/Streaming/Channels/'+(row.channel||1)+'01'; this.$alert('RTSP地址: '+url, '预览提示', { confirmButtonText: '知道了' }) }, pingDevice(row) { const load = this.$loading({ text: 'Ping检测 '+row.ip_address+'...', target: '.app-container' }); request({ url: B+'/ping/'+row.device_id, method: 'post' }).then(res => { load.close(); const d = res.data || res; this.$alert((d.reachable?'在线':'离线')+' | '+d.ip+' | '+(d.latency||''), '检测结果', { type: d.reachable?'success':'error' }).then(()=>{ this.getList() }) }).catch(() => { load.close() }) } } }
</script>
