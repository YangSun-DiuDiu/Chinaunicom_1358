export default {
  data() {
    return {
      queryParams: {},
      list: [],
      total: 0,
      loading: false,
      ids: [],
      dateRange: []
    }
  },
  methods: {
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.queryParams = {}
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item[this.idField || 'id'])
    },
    handleDelete(row) {
      const ids = row[this.idField || 'id'] || this.ids
      this.$modal.confirm('确认删除？').then(() => {
        return this.deleteApi(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      })
    },
    handleExport() {
      this.$modal.confirm('确认导出？').then(() => {
        return this.exportApi(this.queryParams)
      }).then((res) => {
        this.$download.name(res.msg)
      })
    }
  }
}
