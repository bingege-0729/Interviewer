import { http } from '@/utils/http'
import { endpoints } from '@/config/api'
import type {
  ApiResponse,
  BannerInfo,
  BenefitInfo,
  PackageInfo,
  OrderInfo,
  CreatePackageOrderParams,
  CreateBenefitOrderParams,
  PayQrCodeResult,
  PaginationResponse,
} from '@/types'

export class BannerApiService {
  /** 获取轮播图列表 */
  static async getList(): Promise<ApiResponse<BannerInfo[]>> {
    const response = await http.getAxiosInstance().get(endpoints.banner.list)
    return response.data
  }
}

export class ProductApiService {
  /** 获取套餐列表 */
  static async getPackages(): Promise<ApiResponse<PackageInfo[]>> {
    const response = await http.getAxiosInstance().get(endpoints.product.packages)
    return response.data
  }

  /** 获取权益列表 */
  static async getBenefits(): Promise<ApiResponse<BenefitInfo[]>> {
    const response = await http.getAxiosInstance().get(endpoints.product.benefits)
    return response.data
  }
}

export class OrderApiService {
  /** 创建权益订单 */
  static async createBenefitOrder(params: CreateBenefitOrderParams): Promise<ApiResponse<PayQrCodeResult>> {
    const response = await http.getAxiosInstance().post(endpoints.order.createBenefit, params)
    return response.data
  }

  /** 创建套餐订单 */
  static async createPackageOrder(params: CreatePackageOrderParams): Promise<ApiResponse<PayQrCodeResult>> {
    const response = await http.getAxiosInstance().post(endpoints.order.createPackage, params)
    return response.data
  }

  /** 分页查询订单列表 */
  static async page(params?: { page: number; size: number }): Promise<PaginationResponse<OrderInfo>> {
    const response = await http.getAxiosInstance().post(endpoints.order.page, params)
    return response.data
  }

  /** 查询订单支付状态 */
  static async queryPayStatus(outTradeNo: string): Promise<ApiResponse<OrderInfo>> {
    const response = await http.getAxiosInstance().get(endpoints.order.status, {
      params: { outTradeNo },
    })
    return response.data
  }
}

export default ProductApiService
