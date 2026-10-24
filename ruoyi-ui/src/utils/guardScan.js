/**
 * 保安核验 · 摄像头扫码能力（单一来源）
 *
 * 从 guard.vue / log/index.vue 两处重复实现中收敛：
 *   - preflightCamera()        摄像头预检（environment → user 降级），绕过 html5-qrcode 异常包装拿真实错误名
 *   - reportCameraError(e)     上报真实错误到服务器日志（应用内浏览器错误丢失时靠它远程诊断）
 *   - getCameraErrorMessage(e) 错误对象 → 中文提示文案（页面/弹窗共用同一映射）
 *   - startScanner()           Html5Qrcode 构造 + environment→user 降级启动（扫码回调唯一入口）
 *   - stopScanner()            停止并释放扫码器
 *
 * 注意：Html5Qrcode 构造参数是容器元素的 **id**（内部 getElementById，找不到会抛错），
 * 使用方模板必须写 <div id="scanner" ref="scanner">（本模块不负责 DOM）。
 */
import { Html5Qrcode } from 'html5-qrcode'

/** 预检摄像头：优先后置，失败降级前置；返回媒体流（成功后调用方负责 stop） */
export async function preflightCamera() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    const err = new Error('浏览器未提供摄像头接口（mediaDevices 不可用）')
    err.name = 'NoMediaDevices'
    throw err
  }
  try {
    return await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
  } catch (err1) {
    try {
      return await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
    } catch (err2) {
      reportCameraError(err2)
      throw err2
    }
  }
}

/** 上报真实错误到服务器日志（/visitor/h5/camlog），用于远程诊断 */
export function reportCameraError(e) {
  try {
    const d = {
      name: (e && e.name) || '',
      message: (e && e.message) || '',
      stack: ((e && e.stack) || '').slice(0, 500),
      ua: navigator.userAgent
    }
    fetch('/prod-api/visitor/h5/camlog', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(d)
    }).catch(() => {})
  } catch (ignore) { /* 上报失败不影响主流程 */ }
}

/** 错误对象 → 中文提示文案（唯一映射来源） */
export function getCameraErrorMessage(e) {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    return '当前环境不支持摄像头调用（浏览器未提供摄像头接口）：请改用系统浏览器（Chrome/Safari）打开本站'
  }
  const name = (e && e.name) || ''
  const msg = (e && e.message) || ''
  const msgMap = {
    NoMediaDevices: '当前环境不支持摄像头调用（浏览器未提供摄像头接口）：请改用系统浏览器（Chrome/Safari）打开本站',
    NotAllowedError: '浏览器拒绝了摄像头权限：请点击地址栏右侧摄像头图标，将本站权限设为「允许」后重试；或在手机设置中允许浏览器使用相机',
    NotFoundError: '未检测到可用摄像头：请确认设备已接入摄像头，或改用手动输入通行码',
    NotReadableError: '摄像头正被其他应用占用：请关闭其他占用相机的应用（如微信、钉钉）后重试',
    NotSupportedError: '当前环境不支持摄像头调用：请使用手机系统浏览器（Chrome/Safari）打开本站重试',
    SecurityError: '非安全环境无法调用摄像头：请通过 https:// 地址访问本站',
    TypeError: '浏览器拒绝摄像头调用（可能是应用内浏览器限制）：请改用系统浏览器（Chrome/Safari）打开'
  }
  if (msgMap[name]) {
    return msgMap[name]
  }
  if (msg && /not found|element/i.test(msg)) {
    return '页面组件未就绪：请刷新页面后重试打开摄像头'
  }
  return '无法打开摄像头（' + (name || msg || '未知错误') + '）'
}

/**
 * 启动扫码器（优先后置摄像头，失败自动降级前置）
 * @param {string} elementId 扫码容器元素 id（必须真实存在）
 * @param {Object} options { onDecoded(code), onFail(err) }
 * @returns {Promise<Html5Qrcode>} 已启动的扫码器实例
 */
export async function startScanner(elementId, { onDecoded, onFail }) {
  const scanner = new Html5Qrcode(elementId)
  const onSuccess = (decodedText) => {
    onDecoded && onDecoded(decodedText)
  }
  try {
    await scanner.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 250, height: 250 } },
      onSuccess,
      () => {} // ignore 单帧解码失败
    )
  } catch (err1) {
    try {
      await scanner.start(
        { facingMode: 'user' },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        onSuccess,
        () => {}
      )
    } catch (err2) {
      try { scanner.clear() } catch (e) {}
      onFail && onFail(err2)
      return null
    }
  }
  return scanner
}

/** 停止并清理扫码器 */
export async function stopScanner(scanner) {
  if (scanner) {
    try { await scanner.stop() } catch (e) {}
    try { scanner.clear() } catch (e) {}
  }
  return null
}

/** 释放媒体流 */
export function releaseStream(stream) {
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
  }
}
