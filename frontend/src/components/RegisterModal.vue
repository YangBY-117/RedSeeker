<template>
  <el-dialog v-model="visible" title="用户注册" width="400px" :close-on-click-modal="false">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" autocomplete="off" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" autocomplete="off" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirm">
        <el-input v-model="form.confirm" type="password" autocomplete="off" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleRegister">注册</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
const visible = ref(false)
const formRef = ref()
const form = ref({ username: '', password: '', confirm: '' })
const rules = {
  username: [ { required: true, message: '请输入用户名', trigger: 'blur' } ],
  password: [ { required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' } ],
  confirm: [ { required: true, message: '请确认密码', trigger: 'blur' }, { validator: (rule, value, callback) => { if (value !== form.value.password) callback(new Error('两次密码不一致')); else callback(); }, trigger: 'blur' } ]
}

function handleRegister() {
  formRef.value.validate((valid) => {
    if (!valid) return
    // TODO: 调用后端注册API
    // 注册成功后关闭弹窗
    visible.value = false
  })
}

// 可通过事件或props控制 visible
</script>

<style scoped>
.el-dialog {
  border-radius: 12px;
}
</style>
