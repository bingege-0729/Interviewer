/**
 * 用户账号信息
 */
export interface AccountInfo {
  id: number
  username: string
  email: string | null
  phone: string | null
  headImg: string | null
  status: 'ON' | 'OFF'
  role: 'COMMON' | 'ADMIN'
  openid: string | null
  unionid: string | null
  gmtCreate: string
  gmtModified: string
}

/**
 * 用户权益信息
 */
export interface AccountBenefit {
  id: number
  accountId: number
  benefitCode: string
  productOrderId: number | null
  remainingCount: number
  totalCount: number
  startTime: string
  endTime: string
  benefitId: number | null
  orderType: 'PACKAGE_ORDER' | 'BENEFIT_ORDER' | null
  gmtCreate: string
  gmtModified: string
}

/**
 * 登录参数
 */
export interface LoginParams {
  identifier: string
  checkCode: string
  type: string
}

/**
 * 登录响应
 */
export interface LoginResult {
  success: boolean
  token: string
  accountDTO: AccountInfo
  errorMessage: string | null
  qrCodeStatus: string | null
  sceneId: string | null
}

/**
 * 发送验证码参数
 */
export interface SendCodeParams {
  identifier: string
  captcha: string
  type: string
}

/**
 * 微信二维码生成响应
 */
export interface WechatQrcodeResult {
  sceneId: string
  qrCodeUrl: string
  expiresTime: number
  status: WechatScanStatus
  createTime: number
}

/**
 * 微信扫码状态枚举
 */
export type WechatScanStatus = 'WAITING' | 'SCANNED' | 'CONFIRMED' | 'EXPIRED'

/**
 * 微信扫码登录结果
 */
export interface WechatLoginResult {
  success: boolean
  token: string | null
  accountDTO: AccountInfo | null
  errorMessage: string | null
  qrCodeStatus: WechatScanStatus
  sceneId: string | null
}
