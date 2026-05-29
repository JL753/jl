# Unity AI 虚拟人前端移植 + Java 后端适配

## 注意事项

- 源项目 LLM 使用百度 ERNIE 3.5，目标项目使用讯飞星火大模型。system prompt 需要从百度格式适配到讯飞格式，导航指令集根据目标项目路由重新定义。
- 讯飞 STT/TTS 具体 API 接入方式（SDK vs REST API）在实现阶段调研确认。
- Unity Build 文件从源项目 `src/assets/static/Unity/Build/` 复制，不做修改。

## 概述

将 ai-assistant-teaching-website 中的 Unity WebGL 虚拟人"小慧"（含语音交互、导航指令）移植到 softwacecup 学生端，前端保留 Unity 3D 角色和对话面板，后端复用现有 Java/讯飞服务。

## 移植策略

**渐进增强（A 方案）**：保留现有 AICompanionView（纯文本 AI 对话），新增 AICompanionImmersive（Unity 虚拟人 + 对话），两个页面共存。

- 侧边栏新增"沉浸伴学"入口
- AIFloatingBall 悬浮球对话中新增跳转链接
- 情绪检测功能暂缓移植

## 新建文件

| 文件 | 说明 |
|---|---|
| `src/views/student/AICompanionImmersive.vue` | 左 Unity 虚拟人 + 右对话面板 |
| `public/unity/` 目录及其中 Unity Build 文件 | .data / .framework.js / .loader.js / .wasm |
| `public/unity/recorder.wav.min.js` | 浏览器端 WAV 录音库 |
| `public/unity/unity.recorder.js` | Unity 录音通信桥接 |

## 修改文件

| 文件 | 改动 |
|---|---|
| `src/layout/StudentLayout.vue` | 侧边栏新增"沉浸伴学"菜单项 |
| `src/components/AIFloatingBall.vue` | 对话中新增"打开沉浸伴学"跳转链接 |
| `src/router/index.js` | 新增 `/student/companion-immersive` 路由 |
| `src/api/index.js` | 新增 apiAgentChatStream、apiSTT、apiTTS |
| `backend/.../controller/AgentController.java` | 新增 /api/agent/chat-stream、/api/agent/stt、/api/agent/tts |
| 讯飞 STT/TTS 依赖 | pom.xml 新增讯飞 SDK 依赖（如需要） |

## 不动的文件

- `AICompanionView.vue` — 保留现有文本对话页面
- `AIDialogBar.vue` — 保留课程页 AI 对话栏

## 前端组件设计

### AICompanionImmersive.vue

页面布局：左侧 Unity WebGL 容器（3D 虚拟人），右侧对话面板（消息列表 + 输入区）。

Unity WebGL 初始化逻辑移植自 UnityComponent.vue：
- 动态加载 Unity loader.js
- 配置 canvas 尺寸
- 初始化 Recorder 录音实例
- 暴露 window.handleUnityTransmission 全局函数

对话面板复用目标项目现有设计语言（玻璃态、暗色主题、SCSS 变量），功能包括：
- 消息气泡（用户/AI，支持 Markdown 渲染）
- 流式加载指示器
- 语音输入按钮（调用 RecorderIns）
- 文字输入框 + 发送按钮
- 清空对话按钮

### Unity ↔ Vue 通信桥接

沿用源项目模式：

- **Unity → JS**：Unity C# 通过 `Application.ExternalCall("handleUnityTransmission", str)` 调用全局函数
- **JS → Unity**：Vue 通过 `UnityIns.SendMessage('ChatManager', 'MethodName', params)` 调用 Unity 方法

handleUnityTransmission 处理的消息类型：

| 前缀 | 含义 | 处理 |
|---|---|---|
| `<网站指令>` | 导航指令 | 解析目标页面，执行 router.push() |
| `<用户输入>` | STT 识别结果 | Toast 提示 |
| `<LLM回复>` | LLM 结构化回复 | 解析 表情\|动作\|回复文本\|指令，显示回复，触发 TTS |
| `<聊天完成>` | 对话结束 | 结束 loading 状态 |
| `<请求失败>` | 错误 | Toast 错误提示 |

### 结构化 LLM 回复格式

沿用源项目格式：

```
表情：开心|动作：右手放胸前|回复文本：你好！我是虚拟教学助手小慧|指令：无
```

四个字段用 `|` 分隔，无内容用 `无` 代替。前端解析后：
- 表情/动作 → Unity 动画同步（UnityIns.SendMessage）
- 回复文本 → 对话区 Markdown 渲染
- 指令 → 执行页面导航

## 后端设计

### AgentController 新增端点

**POST /api/agent/chat-stream**

流式 LLM 对话，SSE 响应。请求体：

```json
{ "question": "用户输入文本", "history": [{"role":"user","content":"..."}] }
```

SSE 事件流：每收到讯飞 LLM 一个 token，发送 `data: {"delta":"...","finish":false}`，结束发送 `data: {"delta":"","finish":true}`。

后端构建 system prompt，要求 LLM 按结构化格式（表情|动作|回复文本|指令）回复，并支持以下导航指令：打开课程平台、打开学习分析、打开我的考试、打开问答广场、打开个人资料、打开学习首页。

**POST /api/agent/stt**

语音识别。请求：multipart/form-data，字段 `file`（WAV 音频）。响应：

```json
{ "text": "识别结果文本" }
```

调用讯飞语音转文本 API（需确认具体 API 名称和接入方式）。

**POST /api/agent/tts**

语音合成。请求体：

```json
{ "text": "需要合成语音的文本" }
```

响应：audio/wav 二进制流。调用讯飞文本转语音 API。

## 错误处理与降级

### 前端降级

| 场景 | 处理 |
|---|---|
| Unity WebGL 加载超时 (>30s) | 显示加载失败 + 重试按钮，对话面板切换全宽纯文本模式 |
| 麦克风权限被拒 | Toast 提示开启权限，文字输入不受影响 |
| STT 识别失败/为空 | Toast "语音识别失败，请用文字输入" |
| LLM 请求超时 | 对话区显示"小慧暂时不在线，请稍后重试" |
| TTS 合成失败 | Unity 只播放表情动作，不播语音（静默降级） |
| Unity canvas 崩溃 | 隐藏 Unity 容器，对话面板全宽 |

### 后端错误码

| 场景 | HTTP 状态码 |
|---|---|
| 讯飞 API 超时 (10s) | 504 |
| 讯飞 API 配额耗尽 | 429 + `{"error":"quota_exhausted"}` |
| 音频文件过大 (>10MB) | 413 |

## 路由设计

```javascript
// router/index.js 新增
{
  path: '/student/companion-immersive',
  component: () => import('@/views/student/AICompanionImmersive.vue'),
  meta: { title: '沉浸伴学', layout: 'StudentLayout' }
}
```

## 侧边栏菜单

StudentLayout.vue 侧边栏在 AICompanionView（AI 伴学）下方新增菜单项：
- 图标：与 AI 伴学相同或相近
- 文案："沉浸伴学"
- 路由：`/student/companion-immersive`

## 验收清单

- [ ] Unity WebGL 虚拟人加载并渲染正常
- [ ] 麦克风录音 → STT 识别 → 结果显示
- [ ] 文字输入 → SSE 流式返回 → Markdown 渲染
- [ ] 结构化回复解析正确（表情/动作/文本/指令拆分）
- [ ] 导航指令触发页面跳转
- [ ] TTS 语音合成并在 Unity 中播放
- [ ] Unity 不可用时自动降级为纯文本全宽模式
- [ ] 现有 AICompanionView 功能不受影响
- [ ] AIFloatingBall 原有功能正常
