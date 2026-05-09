<template>
  <div class="course-mgmt-page">
    <section class="page-header">
      <div><h2>📚 课程管理</h2><p>管理平台课程资源，支持导入、编辑和发布课程到门户首页。</p></div>
      <el-button type="primary" @click="openCreateDialog">+ 新建课程</el-button>
    </section>

    <!-- Course Cards Grid -->
    <div class="course-grid">
      <article v-for="(course, idx) in courses" :key="course.id || idx" class="course-card panel lift-card">
        <div class="card-cover" :style="{ background: getCourseGradient(course) }">
          <span class="cover-tag">{{ course.tag || '课程' }}</span>
        </div>
        <div class="card-body">
          <small>{{ course.category || '未分类' }}</small>
          <h3>{{ course.title }}</h3>
          <p>{{ course.description || course.desc || '' }}</p>
          <div class="card-meta">
            <span :class="['status', course.status === '已发布' || course.status === '已上线' ? 'active' : 'pending']">{{ course.status }}</span>
            <strong>{{ course.price }}</strong>
          </div>
          <div class="card-actions">
            <el-button size="small" @click="openEditDialog(course)" link>编辑</el-button>
            <el-popconfirm title="确定删除此课程吗？" @confirm="handleDelete(course)">
              <el-button size="small" type="danger" link>删除</el-button>
            </el-popconfirm>
          </div>
        </div>
      </article>

      <div v-if="courses.length === 0 && !loading" class="empty-state">暂无课程数据，点击"新建课程"开始添加</div>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '添加课程'" width="600px" destroy-on-close :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" label-width="100px" label-position="top" ref="formRef">
        <el-form-item label="课程名称" prop="title"><el-input v-model="form.title" placeholder="如：人工智能导论"/></el-form-item>
        <el-form-item label="分类标签" prop="category"><el-input v-model="form.category" placeholder="如：人工智能"/></el-form-item>
        <el-form-item label="标签标识" prop="tag"><el-input v-model="form.tag" placeholder="如：AI" maxlength="6"/></el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="课程简介..."/></el-form-item>
        <el-form-item label="价格" prop="price"><el-input v-model="form.price" placeholder="免费 / ¥299"/></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status"><el-option label="已发布" value="已发布"/><el-option label="草稿" value="草稿"/><el-option label="下架" value="下架"/></el-select>
        </el-form-item>
        <el-form-item label="封面图URL（可选）">
          <el-input v-model="form.coverImage" placeholder="https://..."/>
        </el-form-item>
        <el-form-item label="目标受众"><el-input v-model="form.targetAudience" placeholder="适合人群描述..."/></el-form-item>
        <el-form-item label="章节（可选）"><el-input v-model="chaptersInput" type="textarea" :rows="4" placeholder="用逗号分隔各章节标题"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiAdminListCourses, apiAdminCreateCourse, apiAdminUpdateCourse, apiAdminDeleteCourse } from '../../api'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const gradientInput = ref('')
const chaptersInput = ref('')

const form = reactive({ id: null, title: '', category: '', tag: '', description: '', price: '免费', status: '已发布', coverImage: '', targetAudience: '' })
const rules = { title: [{ required: true, message: '请输入课程名称', trigger: 'blur' }] }

// Courses from backend
const courses = ref([])

const loadCourses = async () => {
  loading.value = true
  try {
    const res = await apiAdminListCourses()
    courses.value = res.data || []
  } catch (e) {
    console.warn('Load courses failed:', e)
  } finally {
    loading.value = false
  }
}

const getCourseGradient = (course) => {
  const gradients = {
    'AI': 'linear-gradient(135deg,#667eea,#764ba2)',
    'ML': 'linear-gradient(135deg,#59a9ff,#5fd8ff)',
    'DL': 'linear-gradient(135deg,#6f7fff,#9ca8ff)',
    'SE': 'linear-gradient(135deg,#18b48f,#6fd8b9)',
    'DS': 'linear-gradient(135deg,#f093fb,#f5576c)',
  }
  if (gradients[course.tag]) return gradients[course.tag]
  if (course.coverImage) return `url(${course.coverImage}) center/cover`
  return 'linear-gradient(135deg, #667eea, #764ba2)'
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, title: '', category: '', tag: '', description: '', price: '免费', status: '已发布', coverImage: '', targetAudience: '' })
  chaptersInput.value = ''
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEditDialog = (course) => {
  isEdit.value = true
  Object.assign(form, JSON.parse(JSON.stringify(course)))
  try {
    const parsed = typeof course.chaptersJson === 'string' ? JSON.parse(course.chaptersJson) : []
    chaptersInput.value = Array.isArray(parsed) ? parsed.map(ch => ch.title || ch).join(', ') : ''
  } catch { chaptersInput.value = '' }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  const chapterList = chaptersInput.value.split(/[,，]/).filter(s => s.trim()).map(s => ({ title: s.trim(), description: '' }))
  
  try {
    if (isEdit.value) {
      await apiAdminUpdateCourse(form.id, {
        title: form.title, category: form.category, description: form.description,
        price: form.price, tag: form.tag, status: form.status, coverImage: form.coverImage,
        targetAudience: form.targetAudience, chaptersJson: chapterList.length > 0 ? chapterList : undefined
      })
      ElMessage.success('课程已更新')
    } else {
      await apiAdminCreateCourse({
        title: form.title, category: form.category, description: form.description,
        price: form.price, tag: form.tag, status: form.status, coverImage: form.coverImage,
        targetAudience: form.targetAudience, chapters: chapterList.length > 0 ? chapterList : undefined
      })
      ElMessage.success('课程已创建')
    }
    dialogVisible.value = false
    await loadCourses()
  } catch (e) { ElMessage.error((isEdit.value ? '更新' : '创建') + '失败: ' + (e.message || '')) }
  finally { submitting.value = false }
}

const handleDelete = async (course) => {
  try {
    await apiAdminDeleteCourse(course.id)
    courses.value = courses.value.filter(c => c.id !== course.id)
    ElMessage.success('课程已删除')
  } catch (e) { ElMessage.error('删除失败: ' + (e.message || '')) }
}

onMounted(loadCourses)
</script>
<style scoped lang="scss">
.course-mgmt-page {
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
  }
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: $spacing-lg;
}

.course-card {
  overflow: hidden;
  border-radius: $radius-large;
  border: 1px solid $border-extra-light;
  background: $bg-white;
  transition: all $transition-slow;
}

.card-cover {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 30px;
  font-weight: $font-weight-bold;
}

.cover-tag {
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.card-body {
  padding: $spacing-lg $spacing-xl;

  small {
    font-size: $font-size-xs;
    color: $text-secondary;
    text-transform: uppercase;
    font-weight: $font-weight-semibold;
  }

  h3 {
    font-size: $font-size-xl;
    margin: 10px 0 8px;
    color: $text-primary;
    font-weight: $font-weight-bold;
  }

  p {
    color: $text-regular;
    font-size: $font-size-sm;
    line-height: 1.65;
    margin: 0 0 $spacing-md;
    min-height: 40px;
  }
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-extra-light;
}

.status {
  padding: 3px 10px;
  border-radius: $radius-round;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;

  &.active {
    background: #dcfce7;
    color: $success-color;
  }

  &.pending {
    background: #fef3c7;
    color: $warning-color;
  }
}

.card-price {
  font-size: $font-size-lg;
  color: $success-color;
  font-weight: $font-weight-bold;
}

.card-actions {
  display: flex;
  gap: $spacing-xs;
  margin-top: $spacing-sm;
}

.lift-card {
  transition: transform $transition-slow, box-shadow $transition-slow;

  &:hover {
    transform: translateY(-6px);
    box-shadow: $shadow-heavy;
  }
}

.empty-state {
  text-align: center;
  padding: 60px $spacing-lg;
  color: $text-secondary;
  font-size: $font-size-md;
}
</style>
