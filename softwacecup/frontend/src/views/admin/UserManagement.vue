<template>
  <div class="user-mgmt-page">
    <section class="page-header">
      <div><h2>👥 用户管理</h2><p>管理平台所有教师、学生和管理员账号，支持创建、编辑、删除和角色切换。</p></div>
      <el-button type="primary" @click="openCreateDialog">+ 新建用户</el-button>
    </section>

    <section class="filter-bar">
      <el-input v-model="searchText" placeholder="搜索用户名/显示名..." clearable prefix-icon="Search" />
      <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width:160px;">
        <el-option label="全部" value="" /><el-option label="学生" value="student" /><el-option label="教师" value="teacher" /><el-option label="管理员" value="admin" />
      </el-select>
      <span class="result-count">共 {{ filteredUsers.length }} 条记录</span>
    </section>

    <section class="table-section">
      <el-table :data="filteredUsers" stripe style="width:100%" v-loading="loading" empty-text="暂无用户数据">
        <el-table-column prop="id" label="ID" width="70" sortable />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="displayName" label="显示名称" width="140" />
        <el-table-column prop="role" label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'teacher' ? '' : 'primary'" size="small">{{ roleMap[row.role] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="avatarUrl" label="头像" width="90">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl || undefined">{{ (row.displayName || '?')[0] }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)" link>编辑</el-button>
            <el-popconfirm title="确定删除此用户吗？" @confirm="handleDelete(row)">
              <el-button size="small" type="danger" link>删除</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="560px" destroy-on-close :close-on-click-modal="false">
      <el-form :model="form" :rules="formRules" label-width="100px" label-position="top" ref="formRef">
        <el-form-item label="账号" prop="username"><el-input v-model="form.username" placeholder="登录账号（唯一）" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password placeholder="登录密码" /></el-form-item>
        <el-form-item label="显示名称" prop="displayName"><el-input v-model="form.displayName" placeholder="显示名称" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%"><el-option label="学生" value="student" /><el-option label="教师" value="teacher" /><el-option label="管理员" value="admin" /></el-select>
        </el-form-item>
        <el-form-item label="头像URL（可选)"><el-input v-model="form.avatarUrl" placeholder="头像图片URL" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建用户' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { apiAdminListUsers, apiAdminCreateUser, apiAdminUpdateUser, apiAdminDeleteUser } from '../../api'

const loading = ref(false)
const searchText = ref('')
const roleFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const users = ref([])

const roleMap = { student: '学生', teacher: '教师', admin: '管理员' }

const form = reactive({
  id: null, username: '', password: '', displayName: '', role: 'student', avatarUrl: ''
})

const formRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const filteredUsers = computed(() => users.value.filter((u) => {
  const matchSearch = !searchText.value || u.username.includes(searchText.value) || u.displayName.includes(searchText.value)
  const matchRole = !roleFilter.value || u.role === roleFilter.value
  return matchSearch && matchRole
}))

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await apiAdminListUsers({ keyword: undefined, role: undefined })
    users.value = data || []
  } catch (e) {
    console.warn('Load users failed:', e)
    users.value = []
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, username: '', password: '123456', displayName: '', role: 'student', avatarUrl: '' })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      const { id, ...payload } = form
      await apiAdminUpdateUser(id, payload)
      const idx = users.value.findIndex((u) => u.id === id)
      if (idx >= 0) Object.assign(users.value[idx], form)
      ElMessage.success('用户已更新')
    } else {
      const data = await apiAdminCreateUser({ username: form.username, password: form.password, role: form.role, displayName: form.displayName })
      users.value.push(data || { ...form, id: Date.now() })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (e) {
    ElMessage.error(isEdit.value ? `更新失败: ${e.message || ''}` : `创建失败: ${e.message || ''}`)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await apiAdminDeleteUser(row.id)
    users.value = users.value.filter((u) => u.id !== row.id)
    ElMessage.success('用户已删除')
  } catch (e) {
    ElMessage.error(`删除失败: ${e.message || ''}`)
  }
}

onMounted(loadUsers)
</script>

<style scoped lang="scss">
.user-mgmt-page {
  display: grid;
  gap: $spacing-lg;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;

  h2 {
    margin: 0;
    font-size: $font-size-xxl;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: $font-size-base;
    max-width: 560px;
  }
}

.filter-bar {
  display: flex;
  gap: $spacing-sm;
  align-items: center;
  padding: $spacing-sm $spacing-lg;
  background: $bg-white;
  border-radius: $radius-medium;
  border: 1px solid $border-extra-light;
  box-shadow: $shadow-light;
}

.result-count {
  margin-left: auto;
  font-size: $font-size-sm;
  color: $text-secondary;
  font-weight: $font-weight-medium;
  padding: $spacing-xs $spacing-md;
  border-radius: $radius-round;
  background: $bg-lighter;
}

.table-section {
  background: $bg-white;
  border-radius: $radius-medium;
  border: 1px solid $border-extra-light;
  overflow: hidden;
  padding: 4px;
  box-shadow: $shadow-card;
}
</style>
