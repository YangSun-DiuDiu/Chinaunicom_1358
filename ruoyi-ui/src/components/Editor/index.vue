<template>
  <div class="editor-stub">
    <el-input
      type="textarea"
      :value="value"
      @input="$emit('input', $event)"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
    />
  </div>
</template>

<script>
export default {
  name: 'Editor',
  props: {
    value: { type: String, default: '' },
    rows: { type: Number, default: 4 },
    placeholder: { type: String, default: '请输入内容' },
    disabled: { type: Boolean, default: false }
  }
}
</script>
