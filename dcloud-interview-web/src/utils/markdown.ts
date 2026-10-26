import { marked } from 'marked'

/**
 * Markdown渲染工具
 * 支持将Markdown文本转换为安全的HTML
 * 支持内联支付二维码卡片渲染
 */

// 配置marked选项
marked.setOptions({
  breaks: true, // 支持换行符转<br>
  gfm: true,    // 启用GitHub Flavored Markdown
})

/**
 * 支付二维码标记正则
 * 匹配格式: [PAY_QRCODE:{"outTradeNo":"...","codeUrl":"...","payAmount":...}]
 */
const PAY_QRCODE_REGEX = /\[PAY_QRCODE:(\{[^}]+\})\]/g

/**
 * 将Markdown文本渲染为HTML
 * @param content Markdown文本内容
 * @returns 渲染后的HTML字符串
 */
export function renderMarkdown(content: string): string {
  if (!content) return ''
  try {
    // 先处理支付二维码标记（在markdown渲染前提取，避免被转义）
    const paymentBlocks: Array<{ placeholder: string; html: string }> = []
    let processedContent = content.replace(PAY_QRCODE_REGEX, (_match, jsonStr) => {
      // 使用不会被Markdown解析器解释的占位符（避免__被解析为加粗）
      const placeholder = `PAYBLOCK${paymentBlocks.length}HOLDER`
      try {
        const payInfo = JSON.parse(jsonStr)
        const html = renderPaymentCard(payInfo)
        paymentBlocks.push({ placeholder, html })
      } catch {
        paymentBlocks.push({ placeholder, html: `<p>支付信息解析失败</p>` })
      }
      return placeholder
    })

    // 渲染markdown
    let html = marked.parse(processedContent) as string

    // 还原支付卡片HTML（处理被<p>包裹和未包裹两种情况）
    for (const block of paymentBlocks) {
      html = html.replace(`<p>${block.placeholder}</p>`, block.html)
      html = html.replace(block.placeholder, block.html)
    }

    return html
  } catch (e) {
    console.error('Markdown渲染失败:', e)
    return escapeHtml(content)
  }
}

/**
 * 渲染支付卡片HTML
 * 生成包含data属性的支付卡片，前端Vue组件会后续填充QR码和轮询逻辑
 */
function renderPaymentCard(payInfo: { outTradeNo?: string; codeUrl?: string; payAmount?: number; payType?: string }): string {
  const outTradeNo = payInfo.outTradeNo || ''
  const codeUrl = payInfo.codeUrl || ''
  const payAmount = payInfo.payAmount ? (payInfo.payAmount / 100).toFixed(2) : '0.00'

  return `
<div class="payment-card" data-out-trade-no="${escapeHtml(outTradeNo)}" data-code-url="${escapeHtml(codeUrl)}" data-pay-amount="${payAmount}">
  <div class="payment-card-inner">
    <div class="payment-qr-area">
      <canvas class="payment-qr-canvas" width="200" height="200"></canvas>
    </div>
    <div class="payment-info-area">
      <p class="payment-amount">¥${payAmount}</p>
      <p class="payment-hint">请使用微信扫一扫</p>
      <p class="payment-order">订单号：${escapeHtml(outTradeNo)}</p>
    </div>
    <div class="payment-status" data-status="pending">
      <span class="payment-status-icon">⏳</span>
      <span class="payment-status-text">等待支付中...</span>
    </div>
  </div>
</div>`
}

/**
 * 转义HTML特殊字符（用于纯文本内容）
 * @param text 纯文本
 * @returns 转义后的文本
 */
export function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  }
  return text.replace(/[&<>"']/g, (m) => map[m])
}

/**
 * 根据内容类型渲染消息内容
 * @param content 消息内容
 * @param contentType 内容类型
 * @returns 渲染后的HTML
 */
export function renderMessageContent(content: string, contentType: string): string {
  switch (contentType) {
    case 'MARKDOWN':
      return renderMarkdown(content)
    case 'IMAGE':
      return `<img src="${escapeHtml(content)}" alt="AI生成图片" class="max-w-full rounded-lg" loading="lazy" />`
    case 'TEXT':
    default:
      // 纯文本保留换行
      return escapeHtml(content).replace(/\n/g, '<br>')
  }
}
