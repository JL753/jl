<template>
  <div class="report-shell panel">
    <aside class="side-nav">
      <div class="nav-item" @click="router.push('/student/assistant')">智能问答</div>
      <div class="nav-item" @click="router.push('/student/path')">资源推荐</div>
      <div class="nav-item active">图片生成</div>
    </aside>

    <section class="image-stage">
      <div class="page-head">
        <div>
          <p class="eyebrow">IMAGE LAB</p>
          <h3>AI 图片生成</h3>
          <p>根据学习场景快速生成配图、海报和课堂插图，并输出学习评估摘要。</p>
        </div>
        <span class="soft-tag">学生端</span>
      </div>
      <div class="chat-scroll">
        <div class="msg ai-msg">
          <img class="msg-icon" src="https://dummyimage.com/42x42/eaf4ff/4c8dff.png&text=AI" alt="ai" />
          <div class="bubble">你好，请描述您生成的图片！</div>
        </div>
        <div class="generated-box">
          <div class="generated-image" :style="imageStyle">{{ prompt }}</div>
          <div class="tips-box">
            <strong>智能评估摘要</strong>
            <p>{{ assessmentObj?.diagnosis || '可根据学生学习内容生成配图、课件插图或课堂海报。' }}</p>
            <p class="opt">{{ assessmentObj?.optimizeSuggestion }}</p>
          </div>
        </div>
      </div>
      <div class="compose-row">
        <el-input v-model="prompt" placeholder="请输入您上课的场景或图片需求" @keyup.enter="generateImage" />
        <el-button circle type="primary" @click="generateImage">➜</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiEvaluate } from '../../api'

const router = useRouter()
const prompt = ref('请给我生成关于人工智能的配图')
const assessmentObj = ref(null)
const imageStyle = computed(() => ({ background: `linear-gradient(135deg, rgba(248,208,131,.86), rgba(239,157,123,.78), rgba(138,184,255,.8)), url(https://dummyimage.com/960x540/18253d/a8dfff&text=${encodeURIComponent(prompt.value)}) center/cover` }))

const generateImage = async () => {
  const res = await apiEvaluate({ finishedTasks: 6, practiceCount: 30, wrongCount: 6, studyMinutes: 280, resourceUseCount: 15 })
  assessmentObj.value = res.data
}

onMounted(generateImage)
</script>

<style scoped>
.report-shell { min-height: 760px; display: grid; grid-template-columns: 190px 1fr; overflow: hidden; }
.side-nav { padding: 18px 12px; border-right: 1px solid #eef3f8; background: linear-gradient(180deg, #ffffff, #fffdf0); }
.nav-item { padding: 12px 14px; border-radius: 12px; color: #6d7f96; cursor: pointer; margin-bottom: 8px; }
.nav-item.active, .nav-item:hover { background: #eef6ff; color: #4c8dff; }
.image-stage { padding: 18px; display: grid; grid-template-rows: auto 1fr auto; background: linear-gradient(180deg, #fffefc, #fffce8); }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 12px; }
.page-head h3 { margin: 0 0 6px; }
.page-head p:last-child { margin: 0; color: #95a4b8; }
.chat-scroll { overflow: auto; }
.msg { display: flex; gap: 10px; margin-bottom: 18px; }
.msg-icon { width: 42px; height: 42px; border-radius: 50%; }
.bubble { padding: 12px 14px; border-radius: 18px; background: white; border: 1px solid #eef3f8; }
.generated-box { width: min(760px, 100%); background: white; border: 1px solid #eef3f8; border-radius: 18px; padding: 20px; }
.generated-image { height: 260px; border-radius: 16px; display: grid; place-items: center; color: white; font-size: 28px; font-weight: 700; margin-bottom: 14px; background-size: cover; background-position: center; }
.tips-box p { color: #657a91; line-height: 1.9; }
.opt { color: #405a79; font-weight: 600; }
.compose-row { display: grid; grid-template-columns: 1fr 46px; gap: 10px; }
@media (max-width: 1200px) { .report-shell { grid-template-columns: 1fr; } .side-nav { display: none; } }
</style>
