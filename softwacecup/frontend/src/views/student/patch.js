const fs = require('fs');
let code = fs.readFileSync('AICompanionImmersive.vue', 'utf8');

// 1. Make initUnity async
code = code.replace('function initUnity() {', 'async function initUnity() {');

// 2. Add framework patching after "}" closing the mobile/desktop check, before "unityLoadTimer"
const mobileEnd = `  }

  unityLoadTimer = setTimeout(() => {`;
const patchCode = `
  // 拦截 framework.js，注入代码暴露闭包内的 WEBAudio 到全局
  try {
    const fwResp = await fetch(config.frameworkUrl)
    let fwCode = await fwResp.text()
    fwCode += '\\n;(function(){if(typeof WEBAudio!=="undefined"){window.__unityWEBAudio=WEBAudio;console.log("[小慧] WEBAudio 已暴露",Object.keys(WEBAudio))}})();\\n'
    const blob = new Blob([fwCode], { type: 'application/javascript' })
    config.frameworkUrl = URL.createObjectURL(blob)
    console.log('[小慧] framework.js 已修补，WEBAudio 将被暴露到全局')
  } catch (e) {
    console.warn('[小慧] framework.js 修补失败，口型同步将不可用:', e)
  }

  unityLoadTimer = setTimeout(() => {`;

code = code.replace(mobileEnd, patchCode);

// 3. Replace the audio detection block with simplified version
const detectStart = '// 获取 Unity Web Audio 上下文供 uLipSync 口型同步';
const startIdx = code.indexOf(detectStart);
if (startIdx >= 0) {
  // Find the catch block that wraps the detection
  const tryIdx = code.lastIndexOf('try {', startIdx);
  const catchEndMarker = "console.warn('[小慧] 获取 Unity 音频上下文异常:', e)";
  const catchEndIdx = code.indexOf(catchEndMarker, startIdx);
  if (catchEndIdx >= 0) {
    const blockEnd = code.indexOf('}', catchEndIdx) + 1;
    const newDetect = `// 捕获 Unity 音频上下文（已通过 framework 补丁暴露到 window.__unityWEBAudio）
      const w = window.__unityWEBAudio
      if (w && w.audioContext && typeof w.audioContext.createBufferSource === 'function') {
        window.__unityAudioCtx = w.audioContext
        console.log('[小慧] Unity 音频上下文已获取 (state=' + w.audioContext.state + ')，口型同步可用')
      } else {
        console.warn('[小慧] 未能获取 Unity 音频上下文 (__unityWEBAudio=' + !!w + ')')
      }`;
    // Replace from tryIdx to blockEnd
    code = code.substring(0, tryIdx) + newDetect + code.substring(blockEnd);
  }
}

// 4. Simplify speakText detection code
const speakDebugStart = '  let unityCtx = window.__unityAudioCtx\n  if (!unityCtx) {\n    try {\n      // 延迟调试日志\n      console.log(\'[小慧口型 DEBUG] typeof WEBAudio:\', typeof WEBAudio)';
const speakDebugEnd = '    } catch (e) {}\n  }';
const speakStartIdx = code.indexOf(speakDebugStart);
const speakEndIdx = code.indexOf(speakDebugEnd, speakStartIdx);
if (speakStartIdx >= 0 && speakEndIdx >= 0) {
  const newSpeakDetect = `  let unityCtx = window.__unityAudioCtx
  if (!unityCtx) {
    // 延迟检测：Unity 可能在首次用户交互后才创建 AudioContext
    const w = window.__unityWEBAudio
    if (w && w.audioContext && typeof w.audioContext.createBufferSource === 'function') {
      window.__unityAudioCtx = w.audioContext
      unityCtx = w.audioContext
      console.log('[小慧口型] 延迟获取 Unity 音频上下文成功 (state=' + unityCtx.state + ')')
    }`;
  code = code.substring(0, speakStartIdx) + newSpeakDetect + code.substring(speakEndIdx + speakDebugEnd.length);
}

fs.writeFileSync('AICompanionImmersive.vue', code);
console.log('All patches applied successfully');
