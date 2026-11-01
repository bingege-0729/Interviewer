/**
 * 通用 API 响应结构
 */
export interface ApiResponse<T = unknown> {
  code: number
  data: T
  message?: string
}

/**
 * 分页响应结构（匹配后端 CommonUtil.convertToPageMap 输出）
 */
export interface PaginationResponse<T = unknown> {
  code: number
  data: {
    currentData: T[]
    totalRecord: number
    totalPage: number
  }
  message?: string
}

/**
 * 分页请求参数
 */
export interface PaginationParams {
  page: number
  size: number
}
