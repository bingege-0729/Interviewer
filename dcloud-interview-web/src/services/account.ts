import { http } from '@/utils/http'
import { endpoints } from '@/config/api'
import type { ApiResponse, AccountInfo, AccountBenefit, LoginParams, LoginResult, SendCodeParams, WechatQrcodeResult, WechatLoginResult } from '@/types'

export class AccountApiService {
  /**
   * 生成图形验证码图片URL
   * @param identifier 手机号或邮箱
   * @param type 验证码类型
   */
  static getCaptchaUrl(identifier: string, type: string): string {
    const baseUrl = import.meta.env.VITE_API_BASE_URL
    return `${baseUrl}${endpoints.account.captcha}?identifier=${encodeURIComponent(identifier)}&type=${type}&t=${Date.now()}`
  }

  /** 发送短信验证码 */
  static async sendCode(params: SendCodeParams): Promise<ApiResponse<null>> {
    const response = await http.getAxiosInstance().post(endpoints.account.sendCode, params)
    return response.data
  }

  /** 手机号+验证码登录 */
  static async login(params: LoginParams): Promise<ApiResponse<LoginResult>> {
    const response = await http.getAxiosInstance().post(endpoints.account.login, params)
    return response.data
  }

  /** 退出登录 */
  static async logout(): Promise<ApiResponse<null>> {
    const response = await http.getAxiosInstance().post(endpoints.account.logout)
    return response.data
  }

  /** 获取用户详情 */
  static async getDetail(): Promise<ApiResponse<AccountInfo>> {
    const response = await http.getAxiosInstance().get(endpoints.account.detail)
    return response.data
  }

  /** 上传头像 */
  static async uploadAvatar(file: File): Promise<ApiResponse<string>> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await http.getAxiosInstance().post(endpoints.account.avatar, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  }

  /** 更新用户信息 */
  static async update(data: Partial<AccountInfo>): Promise<ApiResponse<null>> {
    const response = await http.getAxiosInstance().post(endpoints.account.update, data)
    return response.data
  }

  /** 获取用户权益列表 */
  static async getBenefits(): Promise<ApiResponse<AccountBenefit[]>> {
    const response = await http.getAxiosInstance().get(endpoints.account.benefits)
    return response.data
  }

  /** 分页查询用户权益列表 */
  static async getBenefitPage(params: { page: number; size: number }): Promise<ApiResponse<any>> {
    const response = await http.getAxiosInstance().post(endpoints.account.benefitPage, params)
    return response.data
  }

  /** 生成微信登录二维码 */
  static async getWechatQrcode(): Promise<ApiResponse<WechatQrcodeResult>> {
    const response = await http.getAxiosInstance().get(endpoints.wechat.qrcode)
    return response.data
  }

  /** 查询微信扫码登录结果 */
  static async getWechatLoginResult(sceneId: string): Promise<ApiResponse<WechatLoginResult>> {
    const response = await http.getAxiosInstance().get(endpoints.wechat.loginResult, {
      params: { sceneId },
    })
    return response.data
  }
}

export default AccountApiService
