/**
 * API 端点配置
 * 所有后端接口地址集中管理
 */
export const endpoints = {
  /** 账户模块 → account-service */
  account: {
    captcha: '/api/v1/account/captcha',
    sendCode: '/api/v1/account/send_check_code',
    login: '/api/v1/account/login',
    logout: '/api/v1/account/logout',
    detail: '/api/v1/account/detail',
    avatar: '/api/v1/account/avatar',
    update: '/api/v1/account/update',
    benefits: '/api/v1/account/benefit/list',
    benefitPage: '/api/v1/account/benefit/page',
  },

  /** 微信模块 → account-service */
  wechat: {
    qrcode: '/api/v1/wechat/qrcode',
    loginResult: '/api/v1/wechat/login/result',
  },

  /** 面试模块 → ai-service */
  interview: {
    create: '/api/v1/interview/create',
    page: '/api/v1/interview/page',
    status: '/api/v1/interview/status',
    answer: '/api/v1/interview/answer',
    finish: '/api/v1/interview/finish',
    view: '/api/v1/interview/view',
    delete: '/api/v1/interview/delete',
    // 兼容旧引用
    list: '/api/v1/interview/page',
    detail: '/api/v1/interview/view',
    start: '/api/v1/interview/start',
    end: '/api/v1/interview/finish',
    rounds: '/api/v1/interview/round/list',
    roundDetail: '/api/v1/interview/round/detail',
  },

  /** 题目模块 → ai-service */
  question: {
    list: '/api/v1/question/list',
    detail: '/api/v1/question/detail',
    submit: '/api/v1/question/submit',
  },

  /** AI对话模块 → ai-service */
  chat: {
    stream: '/api/v1/chat/stream',
    history: '/api/v1/chat/history',
    sessions: '/api/v1/chat/sessions',
    createSession: '/api/v1/chat/session/create',
  },

  /** 简历模块 → ai-service */
  resume: {
    upload: '/api/v1/resume/upload',
    list: '/api/v1/resume/list',
    detail: '/api/v1/resume/detail',
    delete: '/api/v1/resume/delete',
    preview: '/api/v1/resume/preview',
    analyse: '/api/v1/resume/analyse',
  },

  /** 轮播图模块 → product-service */
  banner: {
    list: '/api/v1/banner/list',
  },

  /** 商品模块 → product-service */
  product: {
    packages: '/api/v1/product/packages',
    benefits: '/api/v1/product/benefits',
  },

  /** 订单模块 → product-service */
  order: {
    createBenefit: '/api/v1/order/benefit/create',
    createPackage: '/api/v1/order/package/create',
    status: '/api/v1/order/status',
    page: '/api/v1/order/page',
  },
} as const

/**
 * 构建带路径参数的 URL
 * @param url 接口地址模板
 * @param params 路径参数
 */
export function buildURL(url: string, params: Record<string, string | number>): string {
  let result = url
  Object.entries(params).forEach(([key, value]) => {
    result = result.replace(`:${key}`, String(value))
  })
  return result
}

export default endpoints
