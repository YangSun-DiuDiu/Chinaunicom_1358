export default {
  data() {
    return {
      open: false,
      title: '',
      form: {}
    }
  },
  methods: {
    reset() {
      this.form = this.$options.data().form || {}
      this.resetForm && this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增'
    },
    handleUpdate(row) {
      this.reset()
      const id = row[this.idField || 'id'] || this.ids[0]
      this.getApi(id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const api = this.form[this.idField || 'id']
            ? this.updateApi(this.form)
            : this.addApi(this.form)
          api.then(() => {
            this.$modal.msgSuccess('操作成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    cancel() {
      this.open = false
      this.reset()
    }
  }
}
