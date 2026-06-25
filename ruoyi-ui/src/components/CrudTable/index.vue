<template>
  <div class="crud-table">
    <el-table
      v-loading="loading"
      :data="data"
      @selection-change="handleSelectionChange"
      border
      stripe
    >
      <el-table-column v-if="selection" type="selection" width="55" align="center" />
      <template v-for="col in columns">
        <el-table-column
          :key="col.prop"
          :label="col.label"
          :prop="col.prop"
          :width="col.width"
          :align="col.align || 'center'"
          :sortable="col.sortable"
          :formatter="col.formatter"
        >
          <template v-if="col.slot" v-slot="scope">
            <slot :name="col.slot" :row="scope.row" :index="scope.$index" />
          </template>
        </el-table-column>
      </template>
    </el-table>
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
export default {
  name: 'CrudTable',
  props: {
    columns: { type: Array, required: true },
    data: { type: Array, default: () => [] },
    total: { type: Number, default: 0 },
    loading: { type: Boolean, default: false },
    queryParams: { type: Object, default: () => ({}) },
    selection: { type: Boolean, default: true }
  },
  methods: {
    handleSelectionChange(selection) {
      this.$emit('selection-change', selection)
    },
    getList() {
      this.$emit('get-list')
    }
  }
}
</script>
