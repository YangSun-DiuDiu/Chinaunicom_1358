<template>
  <div class="app-container">
    <el-form :model="query" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="会议室">
        <el-select v-model="query.roomId" placeholder="选择会议室" clearable style="width:180px">
          <el-option v-for="r in roomOptions" :key="r.roomId" :label="r.roomName" :value="r.roomId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:130px">
          <el-option label="待审批" value="PENDING"/>
          <el-option label="已通过" value="APPROVED"/>
          <el-option label="已拒绝" value="REJECTED"/>
          <el-option label="已取消" value="CANCELLED"/>
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="dateRange" style="width:240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['meeting:booking:add']">新增预约</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="会议标题" prop="title" min-width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="会议室" prop="roomName" min-width="120"/>
      <el-table-column label="主持人" prop="hostName" min-width="100"/>
      <el-table-column label="开始时间" prop="startTime" min-width="150" align="center"/>
      <el-table-column label="结束时间" prop="endTime" min-width="150" align="center"/>
      <el-table-column label="参会人员" prop="attendees" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="状态" min-width="90" align="center">
        <template slot-scope="{row}">
          <el-tag v-if="row.status === 'PENDING'" type="warning" size="small">待审批</el-tag>
          <el-tag v-else-if="row.status === 'APPROVED'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="row.status === 'REJECTED'" type="danger" size="small">已拒绝</el-tag>
          <el-tag v-else-if="row.status === 'CANCELLED'" type="info" size="small">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="150" fixed="right" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(row)" v-hasPermi="['meeting:booking:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDel(row)" v-hasPermi="['meeting:booking:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="会议室" prop="roomId">
          <el-select v-model="form.roomId" placeholder="选择会议室" style="width:100%">
            <el-option v-for="r in roomOptions" :key="r.roomId" :label="r.roomName" :value="r.roomId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="时间选择" v-if="form.roomId">
          <div class="timeline-bar">
            <div class="timeline-label">08:00</div>
            <div
              v-for="b in generateTimeBlocks()"
              :key="b.time"
              class="timeline-slot"
              :class="{
                'slot-occupied': b.occupied,
                'slot-free': !b.occupied,
                'slot-start': startSlot === b.time,
                'slot-range': startSlot && endSlot && b.time > startSlot && b.time < endSlot
              }"
              :title="b.time + (b.occupied ? ' (已占用)' : ' (空闲)')"
              @click="!b.occupied && selectSlot(b.time)"
            />
            <div class="timeline-label">20:00</div>
          </div>
          <div class="timeline-legend">
            <span class="legend-free">空闲</span>
            <span class="legend-occupied">已占用</span>
            <span class="legend-selected" v-if="startSlot">已选: {{ startSlot }}{{ endSlot ? ' - ' + endSlot : '' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="会议标题" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="请输入会议标题"/>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择开始时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择结束时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="主持人" prop="hostName">
          <el-input v-model="form.hostName" maxlength="50" placeholder="请输入主持人姓名"/>
        </el-form-item>
        <el-form-item label="主持人电话" prop="hostPhone">
          <el-input v-model="form.hostPhone" maxlength="20" placeholder="请输入主持人电话"/>
        </el-form-item>
        <el-form-item label="参会人员" prop="attendees">
          <el-input v-model="form.attendees" maxlength="500" placeholder="请输入参会人员" type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submit">确定</el-button>
        <el-button @click="open=false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/utils/request'
const BASE = '/meeting/booking'
export default {
  data() {
    return {
      loading: false, showSearch: true, total: 0, list: [], open: false, title: '',
      roomOptions: [], dateRange: [],
      query: { pageNum: 1, pageSize: 10, roomId: undefined, status: undefined, startTime: undefined, endTime: undefined, createBy: this.$store.getters.name },
      form: { bookingId: undefined, roomId: undefined, title: '', startTime: '', endTime: '', hostName: '', hostPhone: '', attendees: '' },
      slots: [], slotDate: '', startSlot: null, endSlot: null,
      rules: {
        roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
        title: [{ required: true, message: '会议标题不能为空', trigger: 'blur' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
        hostName: [{ required: true, message: '主持人姓名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList(); this.getRoomOptions()
  },
  watch: {
    'form.roomId'(val) {
      if (val && this.open) {
        this.fetchSlots()
      } else {
        this.slots = []; this.startSlot = null; this.endSlot = null
      }
    },
    'form.startTime'() {
      if (this.form.roomId && this.open) {
        this.fetchSlots()
      }
    }
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.query }
      if (this.dateRange && this.dateRange.length === 2) {
        params.startTime = this.dateRange[0]; params.endTime = this.dateRange[1]
      } else {
        params.startTime = undefined; params.endTime = undefined
      }
      request({ url: BASE + '/list', method: 'get', params }).then(r => {
        this.list = r.rows; this.total = r.total; this.loading = false
      })
    },
    getRoomOptions() {
      request({ url: '/meeting/room/list', method: 'get', params: { pageNum: 1, pageSize: 999 } }).then(r => {
        this.roomOptions = r.rows || []
      })
    },
    reset() {
      this.dateRange = []
      this.query = { pageNum: 1, pageSize: 10, roomId: undefined, status: undefined, startTime: undefined, endTime: undefined, createBy: this.$store.getters.name }
      this.getList()
    },
    handleAdd() {
      this.form = { bookingId: undefined, roomId: undefined, title: '', startTime: '', endTime: '', hostName: '', hostPhone: '', attendees: '' }
      this.title = '新增会议预约'; this.open = true
    },
    handleEdit(row) {
      this.form = {
        bookingId: row.bookingId, roomId: row.roomId, title: row.title,
        startTime: row.startTime, endTime: row.endTime,
        hostName: row.hostName, hostPhone: row.hostPhone, attendees: row.attendees
      }
      this.title = '修改会议预约'; this.open = true
    },
    handleDel(row) {
      this.$confirm('确认删除会议预约「' + row.title + '」?', '提示', { type: 'warning' }).then(() =>
        request({ url: BASE + '/' + row.bookingId, method: 'delete' }).then(() => { this.$message.success('删除成功'); this.getList() })
      ).catch(() => {})
    },
    fetchSlots() {
      const roomId = this.form.roomId
      if (!roomId) return
      const date = this.form.startTime ? this.form.startTime.substring(0, 10) : this.todayStr()
      this.slotDate = date
      request({ url: BASE + '/slots/' + roomId, method: 'get', params: { date } }).then(r => {
        this.slots = r.data || r.rows || []
        this.startSlot = null; this.endSlot = null
      }).catch(() => { this.slots = [] })
    },
    todayStr() {
      const d = new Date()
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return y + '-' + m + '-' + day
    },
    generateTimeBlocks() {
      const blocks = []
      for (let h = 8; h < 20; h++) {
        for (let m = 0; m < 60; m += 30) {
          const time = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0')
          const occupied = this.isSlotOccupied(time)
          blocks.push({ time, occupied })
        }
      }
      return blocks
    },
    isSlotOccupied(time) {
      if (!this.slots || this.slots.length === 0) return false
      const date = this.slotDate
      const checkTime = date + ' ' + time + ':00'
      return this.slots.some(s => {
        const slotStart = s.startTime || s.start
        const slotEnd = s.endTime || s.end
        return slotStart && slotEnd && checkTime >= slotStart && checkTime < slotEnd
      })
    },
    selectSlot(time) {
      const date = this.slotDate || this.todayStr()
      const fullTime = date + ' ' + time + ':00'
      if (!this.startSlot || (this.startSlot && this.endSlot)) {
        this.startSlot = time
        this.endSlot = null
        this.form.startTime = fullTime
        this.form.endTime = ''
      } else {
        if (time <= this.startSlot) {
          this.startSlot = time
          this.form.startTime = fullTime
          this.form.endTime = ''
          this.endSlot = null
        } else {
          this.endSlot = time
          this.form.endTime = fullTime
        }
      }
    },
    submit() {
      this.$refs.form.validate(v => {
        if (!v) return
        const method = this.form.bookingId ? 'put' : 'post'
        request({ url: BASE, method, data: this.form }).then(() => {
          this.$message.success('操作成功'); this.open = false; this.getList()
        })
      })
    }
  }
}
</script>
<style scoped>
.timeline-bar {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}
.timeline-label {
  font-size: 11px;
  color: #909399;
  min-width: 36px;
  text-align: center;
}
.timeline-slot {
  width: 18px;
  height: 24px;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s;
}
.timeline-slot.slot-free {
  background: #e1f3d8;
  border: 1px solid #b3e19d;
}
.timeline-slot.slot-free:hover {
  background: #c6e9b5;
  transform: scale(1.15);
}
.timeline-slot.slot-occupied {
  background: #fde2e2;
  border: 1px solid #f5b7b7;
  cursor: not-allowed;
}
.timeline-slot.slot-start {
  background: #409eff;
  border-color: #337ecc;
}
.timeline-slot.slot-range {
  background: #a0cfff;
  border-color: #79bbff;
}
.timeline-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
}
.legend-free::before,
.legend-occupied::before,
.legend-selected::before {
  content: '';
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}
.legend-free::before {
  background: #e1f3d8;
  border: 1px solid #b3e19d;
}
.legend-occupied::before {
  background: #fde2e2;
  border: 1px solid #f5b7b7;
}
.legend-selected {
  color: #409eff;
  font-weight: 500;
}
.legend-selected::before {
  background: #409eff;
}
</style>
