/**
 * Markdown 渲染工具
 * 支持：Markdown、LaTeX 公式、代码高亮
 */

import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * 配置 marked 渲染器
 */
const renderer = new marked.Renderer()

// 自定义代码块渲染（支持语法高亮）
renderer.code = (code, language) => {
  const validLang = language || 'plaintext'
  const highlighted = highlightCode(code, validLang)
  return `
    <div class="code-block">
      <div class="code-header">
        <span class="code-lang">${validLang}</span>
        <button class="code-copy" onclick="copyCode(this)">复制</button>
      </div>
      <pre><code class="language-${validLang}">${highlighted}</code></pre>
    </div>
  `
}

// 自定义行内代码渲染
renderer.codespan = (code) => {
  return `<code class="inline-code">${code}</code>`
}

// 自定义表格渲染
renderer.table = (header, body) => {
  return `
    <div class="table-wrapper">
      <table class="markdown-table">
        <thead>${header}</thead>
        <tbody>${body}</tbody>
      </table>
    </div>
  `
}

// 自定义链接渲染（添加安全属性）
renderer.link = (href, title, text) => {
  const titleAttr = title ? ` title="${title}"` : ''
  return `<a href="${href}"${titleAttr} target="_blank" rel="noopener noreferrer">${text}</a>`
}

// 配置 marked 选项
marked.setOptions({
  renderer: renderer,
  gfm: true, // 启用 GitHub Flavored Markdown
  breaks: true, // 支持换行
  pedantic: false,
  sanitize: false, // 我们使用 DOMPurify 进行清理
  smartLists: true,
  smartypants: true
})

/**
 * 简单的代码高亮实现（不依赖 highlight.js）
 * 如果需要更强大的高亮，可以安装 highlight.js
 */
function highlightCode(code, language) {
  // 基础的 HTML 转义
  const escaped = code
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')

  // 简单的语法高亮（可以根据需要扩展）
  if (language === 'javascript' || language === 'js') {
    return highlightJavaScript(escaped)
  } else if (language === 'python') {
    return highlightPython(escaped)
  } else if (language === 'java') {
    return highlightJava(escaped)
  } else if (language === 'sql') {
    return highlightSQL(escaped)
  }

  return escaped
}

/**
 * JavaScript 语法高亮
 */
function highlightJavaScript(code) {
  const keywords = ['const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while', 'class', 'import', 'export', 'async', 'await', 'try', 'catch']
  const builtins = ['console', 'document', 'window', 'Array', 'Object', 'String', 'Number']

  let highlighted = code

  // 高亮关键字
  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b(${keyword})\\b`, 'g')
    highlighted = highlighted.replace(regex, '<span class="keyword">$1</span>')
  })

  // 高亮内置对象
  builtins.forEach(builtin => {
    const regex = new RegExp(`\\b(${builtin})\\b`, 'g')
    highlighted = highlighted.replace(regex, '<span class="builtin">$1</span>')
  })

  // 高亮字符串
  highlighted = highlighted.replace(/(['"`])(.*?)\1/g, '<span class="string">$1$2$1</span>')

  // 高亮数字
  highlighted = highlighted.replace(/\b(\d+)\b/g, '<span class="number">$1</span>')

  // 高亮注释
  highlighted = highlighted.replace(/(\/\/.*$)/gm, '<span class="comment">$1</span>')

  return highlighted
}

/**
 * Python 语法高亮
 */
function highlightPython(code) {
  const keywords = ['def', 'class', 'import', 'from', 'return', 'if', 'elif', 'else', 'for', 'while', 'try', 'except', 'with', 'as', 'lambda', 'yield']

  let highlighted = code

  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b(${keyword})\\b`, 'g')
    highlighted = highlighted.replace(regex, '<span class="keyword">$1</span>')
  })

  highlighted = highlighted.replace(/(['"`])(.*?)\1/g, '<span class="string">$1$2$1</span>')
  highlighted = highlighted.replace(/\b(\d+)\b/g, '<span class="number">$1</span>')
  highlighted = highlighted.replace(/(#.*$)/gm, '<span class="comment">$1</span>')

  return highlighted
}

/**
 * Java 语法高亮
 */
function highlightJava(code) {
  const keywords = ['public', 'private', 'protected', 'class', 'interface', 'extends', 'implements', 'return', 'if', 'else', 'for', 'while', 'new', 'void', 'int', 'String', 'boolean']

  let highlighted = code

  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b(${keyword})\\b`, 'g')
    highlighted = highlighted.replace(regex, '<span class="keyword">$1</span>')
  })

  highlighted = highlighted.replace(/(".*?")/g, '<span class="string">$1</span>')
  highlighted = highlighted.replace(/\b(\d+)\b/g, '<span class="number">$1</span>')
  highlighted = highlighted.replace(/(\/\/.*$)/gm, '<span class="comment">$1</span>')

  return highlighted
}

/**
 * SQL 语法高亮
 */
function highlightSQL(code) {
  const keywords = ['SELECT', 'FROM', 'WHERE', 'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'TABLE', 'INDEX', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'ON', 'AND', 'OR', 'ORDER', 'BY', 'GROUP', 'HAVING']

  let highlighted = code

  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b(${keyword})\\b`, 'gi')
    highlighted = highlighted.replace(regex, '<span class="keyword">$1</span>')
  })

  highlighted = highlighted.replace(/('.*?')/g, '<span class="string">$1</span>')
  highlighted = highlighted.replace(/\b(\d+)\b/g, '<span class="number">$1</span>')
  highlighted = highlighted.replace(/(--.*$)/gm, '<span class="comment">$1</span>')

  return highlighted
}

/**
 * 处理 LaTeX 公式
 * 支持行内公式 $...$ 和块级公式 $$...$$
 */
function processLatex(text) {
  // 处理块级公式 $$...$$
  text = text.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula) => {
    return `<div class="latex-block" data-formula="${encodeURIComponent(formula)}">${renderLatex(formula, true)}</div>`
  })

  // 处理行内公式 $...$
  text = text.replace(/\$([^\$\n]+?)\$/g, (match, formula) => {
    return `<span class="latex-inline" data-formula="${encodeURIComponent(formula)}">${renderLatex(formula, false)}</span>`
  })

  return text
}

/**
 * 渲染 LaTeX 公式（简化版）
 * 注意：这是一个简化实现，实际项目中建议使用 KaTeX 或 MathJax
 */
function renderLatex(formula, isBlock) {
  // 这里返回原始公式，实际使用时应该集成 KaTeX
  // 如果安装了 katex，可以使用：
  // import katex from 'katex'
  // return katex.renderToString(formula, { displayMode: isBlock, throwOnError: false })

  // 临时方案：返回格式化的公式文本
  const className = isBlock ? 'latex-formula-block' : 'latex-formula-inline'
  return `<span class="${className}">${formula}</span>`
}

/**
 * 主渲染函数：将 Markdown 文本转换为 HTML
 * @param {string} markdown - Markdown 文本
 * @param {boolean} enableLatex - 是否启用 LaTeX 渲染
 * @returns {string} 安全的 HTML 字符串
 */
export function renderMarkdown(markdown, enableLatex = true) {
  if (!markdown) return ''

  try {
    // 1. 如果启用 LaTeX，先处理公式（在 Markdown 渲染之前）
    let processed = markdown
    if (enableLatex) {
      processed = processLatex(processed)
    }

    // 2. 使用 marked 渲染 Markdown
    let html = marked.parse(processed)

    // 3. 使用 DOMPurify 清理 HTML（防止 XSS 攻击）
    const clean = DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'p', 'br', 'hr',
        'strong', 'em', 'u', 's', 'del',
        'a', 'img',
        'ul', 'ol', 'li',
        'blockquote', 'pre', 'code',
        'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'div', 'span'
      ],
      ALLOWED_ATTR: [
        'href', 'title', 'target', 'rel',
        'src', 'alt', 'width', 'height',
        'class', 'id',
        'data-formula'
      ]
    })

    return clean
  } catch (error) {
    console.error('Markdown 渲染失败:', error)
    return `<p class="error">渲染失败: ${error.message}</p>`
  }
}

/**
 * 流式渲染：逐步渲染 Markdown（用于 SSE 流式输出）
 * @param {string} partialMarkdown - 部分 Markdown 文本
 * @param {boolean} enableLatex - 是否启用 LaTeX 渲染
 * @returns {string} 安全的 HTML 字符串
 */
export function renderMarkdownStream(partialMarkdown, enableLatex = true) {
  // 流式渲染时，可能会收到不完整的 Markdown
  // 需要特殊处理，避免渲染错误

  try {
    // 检查是否有未闭合的代码块
    const codeBlockCount = (partialMarkdown.match(/```/g) || []).length
    const hasUnclosedCodeBlock = codeBlockCount % 2 !== 0

    // 如果有未闭合的代码块，暂时闭合它
    let processed = partialMarkdown
    if (hasUnclosedCodeBlock) {
      processed += '\n```'
    }

    return renderMarkdown(processed, enableLatex)
  } catch (error) {
    console.error('流式 Markdown 渲染失败:', error)
    return partialMarkdown // 降级：返回原始文本
  }
}

/**
 * 提取纯文本（去除 Markdown 格式）
 * @param {string} markdown - Markdown 文本
 * @returns {string} 纯文本
 */
export function extractPlainText(markdown) {
  if (!markdown) return ''

  // 移除 Markdown 语法
  let text = markdown
    .replace(/```[\s\S]*?```/g, '') // 移除代码块
    .replace(/`[^`]+`/g, '') // 移除行内代码
    .replace(/\$\$[\s\S]+?\$\$/g, '') // 移除块级公式
    .replace(/\$[^\$\n]+?\$/g, '') // 移除行内公式
    .replace(/!\[.*?\]\(.*?\)/g, '') // 移除图片
    .replace(/\[([^\]]+)\]\(.*?\)/g, '$1') // 移除链接，保留文本
    .replace(/[#*_~`]/g, '') // 移除格式符号
    .replace(/\n+/g, ' ') // 合并换行
    .trim()

  return text
}

/**
 * 全局函数：复制代码（供 HTML 中的按钮调用）
 */
if (typeof window !== 'undefined') {
  window.copyCode = function(button) {
    const codeBlock = button.closest('.code-block')
    const code = codeBlock.querySelector('code').textContent

    navigator.clipboard.writeText(code).then(() => {
      const originalText = button.textContent
      button.textContent = '已复制'
      button.style.color = '#10b981'

      setTimeout(() => {
        button.textContent = originalText
        button.style.color = ''
      }, 2000)
    }).catch(err => {
      console.error('复制失败:', err)
      alert('复制失败，请手动复制')
    })
  }
}

export default {
  renderMarkdown,
  renderMarkdownStream,
  extractPlainText
}
