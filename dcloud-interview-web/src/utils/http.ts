import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

/**
 * HTTP 工具类
 * 封装 Axios 实例，包含请求/响应拦截器
 */
class HttpClient {
  private instance: AxiosInstance

  constructor() {
    this.instance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    this.setupInterceptors()
  }

  /**
   * 配置请求/响应拦截器
   */
  private setupInterceptors(): void {
    // 请求拦截器
    this.instance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        // 从 localStorage 获取 token 并注入请求头
        const token = localStorage.getItem('token')
        if (token && config.headers) {
          config.headers.token = token
        }
        return config
      },
      (error: unknown) => {
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    this.instance.interceptors.response.use(
      (response: AxiosResponse) => {
        const { data } = response

        // 业务成功码判断
        if (data.code === 0) {
          return response
        }

        // Token 过期或未认证
        if (data.code === -2 || data.code === 401) {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
          return Promise.reject(new Error(data.message || '登录已过期，请重新登录'))
        }

        // 其他业务错误
        return Promise.reject(new Error(data.message || '请求失败'))
      },
      (error: { response?: { status?: number } }) => {
        // HTTP 状态码错误处理
        const statusMessages: Record<number, string> = {
          400: '请求参数错误',
          401: '未授权，请重新登录',
          403: '拒绝访问',
          404: '请求资源不存在',
          500: '服务器内部错误',
          502: '网关错误',
          503: '服务不可用',
          504: '网关超时',
        }

        const status = error.response?.status
        const message = status ? statusMessages[status] || `请求失败(${status})` : '网络连接异常，请检查网络'

        return Promise.reject(new Error(message))
      }
    )
  }

  /**
   * 获取 Axios 实例
   */
  getAxiosInstance(): AxiosInstance {
    return this.instance
  }

  /**
   * GET 请求
   */
  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.get(url, config)
    return response.data
  }

  /**
   * POST 请求
   */
  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.post(url, data, config)
    return response.data
  }

  /**
   * PUT 请求
   */
  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.put(url, data, config)
    return response.data
  }

  /**
   * DELETE 请求
   */
  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.delete(url, config)
    return response.data
  }

  /**
   * 文件上传
   */
  async upload<T>(url: string, file: File, fieldName = 'file'): Promise<T> {
    const formData = new FormData()
    formData.append(fieldName, file)
    const response = await this.instance.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  }
}

export const http = new HttpClient()
export default http
