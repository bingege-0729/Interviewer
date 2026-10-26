/**
 * 权益编码枚举
 */
export enum BenefitCode {
  RESUME_ANALYSE = 'RESUME_ANALYSE',
  SPECIAL_INTERVIEW = 'SPECIAL_INTERVIEW',
  COMMON_INTERVIEW = 'COMMON_INTERVIEW',
  QUESTION_BANK = 'QUESTION_BANK',
}

/**
 * 订单状态枚举
 */
export enum OrderState {
  NEW = 'NEW',
  PAY = 'PAY',
  CANCEL = 'CANCEL',
}

/**
 * 支付方式枚举
 */
export enum PayType {
  WECHAT_PAY = 'WECHAT_PAY',
  ALIPAY = 'ALIPAY',
}

/**
 * 轮播图信息
 */
export interface BannerInfo {
  id: number
  location: string | null
  name: string | null
  imgUrl: string | null
  pcLink: string | null
  text: string | null
  status: 'ON' | 'OFF' | null
  gmtCreate: string
  gmtModified: string
}

/**
 * 权益信息
 */
export interface BenefitInfo {
  id: number
  name: string
  benefitCode: string | null
  description: string | null
  unitPrice: number
  validDays: number
  isHot: boolean
  isRecommended: boolean
  status: 'ON' | 'OFF'
  sort: number
  gmtCreate: string
  gmtModified: string
}

/**
 * 套餐信息
 */
export interface PackageInfo {
  id: number
  name: string
  description: string | null
  price: number
  validDays: number
  isRecommended: boolean
  status: 'ON' | 'OFF'
  sort: number
  benefits?: BenefitInfo[]
  totalBenefitCount?: number
  gmtCreate: string
  gmtModified: string
}

/**
 * 套餐包含权益
 */
export interface PackageBenefit {
  id: number
  productPackageId: number
  benefitCode: string
  count: number
  gmtCreate: string
}

/**
 * 订单信息
 */
export interface OrderInfo {
  id: number
  accountId: number
  orderType: 'PACKAGE_ORDER' | 'BENEFIT_ORDER'
  title: string | null
  productPackageId: number | null
  benefitId: number | null
  purchaseCount: number | null
  unitPrice: number | null
  discount: number
  payAmount: number
  orderState: OrderState
  payType: PayType | null
  transactionNo: string | null
  outTradeNo: string
  notifyTime: string | null
  gmtCreate: string
  gmtModified: string
}

/**
 * 创建套餐订单参数
 */
export interface CreatePackageOrderParams {
  packageId: number
  payType: PayType
  discount?: number
}

/**
 * 创建权益订单参数
 */
export interface CreateBenefitOrderParams {
  benefitId: number
  purchaseCount: number
  payType: PayType
  discount?: number
}

/**
 * 支付二维码响应
 */
export interface PayQrCodeResult {
  outTradeNo: string
  payAmount: number
  payType: string
  codeUrl: string
}
