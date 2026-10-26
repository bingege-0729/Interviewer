import { http } from '@/utils/http'
import { endpoints } from '@/config/api'
import type {
  ApiResponse,
  PaginationResponse,
  InterviewInfo,
  InterviewRound,
  InterviewViewData,
  QuestionInfo,
  CreateInterviewParams,
  SubmitAnswerParams,
  AiChatRecord,
  ResumeInfo,
} from '@/types'

export class InterviewApiService {
  /** 创建面试 */
  static async create(params: CreateInterviewParams): Promise<ApiResponse<InterviewInfo>> {
    const response = await http.getAxiosInstance().post(endpoints.interview.create, params)
    return response.data
  }

  /** 分页查询面试列表 */
  static async getList(params?: { page: number; size: number }): Promise<PaginationResponse<InterviewInfo>> {
    const response = await http.getAxiosInstance().post(endpoints.interview.page, params)
    return response.data
  }

  /** 获取面试详情（含轮次和题目） */
  static async getDetail(interviewId: number): Promise<ApiResponse<InterviewViewData>> {
    const response = await http.getAxiosInstance().get(endpoints.interview.view, {
      params: { interviewId },
    })
    return response.data
  }

  /** 开始面试 */
  static async start(interviewId: number): Promise<ApiResponse<null>> {
    const response = await http.getAxiosInstance().post(endpoints.interview.start, { interviewId })
    return response.data
  }

  /** 结束面试 */
  static async end(interviewId: number): Promise<ApiResponse<null>> {
    const response = await http.getAxiosInstance().post(endpoints.interview.end, { interviewId })
    return response.data
  }

  /** 结束面试 */
  static async finish(interviewId: number): Promise<ApiResponse<null>> {
    const response = await http.getAxiosInstance().post(endpoints.interview.finish, { interviewId })
    return response.data
  }

  /** 获取面试轮次列表 */
  static async getRounds(interviewId: number): Promise<ApiResponse<InterviewRound[]>> {
    const response = await http.getAxiosInstance().get(endpoints.interview.rounds, {
      params: { interviewId },
    })
    return response.data
  }

  /** 获取轮次详情 */
  static async getRoundDetail(roundId: number): Promise<ApiResponse<InterviewRound>> {
    const response = await http.getAxiosInstance().get(endpoints.interview.roundDetail, {
      params: { roundId },
    })
    return response.data
  }
}

export class QuestionApiService {
  /** 获取题目列表 */
  static async getList(roundId: number): Promise<ApiResponse<QuestionInfo[]>> {
    const response = await http.getAxiosInstance().get(endpoints.question.list, {
      params: { roundId },
    })
    return response.data
  }

  /** 获取题目详情 */
  static async getDetail(questionId: number): Promise<ApiResponse<QuestionInfo>> {
    const response = await http.getAxiosInstance().get(endpoints.question.detail, {
      params: { questionId },
    })
    return response.data
  }

  /** 提交答案 */
  static async submit(params: SubmitAnswerParams): Promise<ApiResponse<QuestionInfo>> {
    const response = await http.getAxiosInstance().post(endpoints.interview.answer, params)
    return response.data
  }
}

export class ChatApiService {
  /** 发送流式对话消息（已迁移到 AiChatApiService.streamChat） */
  static async send(sessionId: number, content: string): Promise<ApiResponse<AiChatRecord>> {
    const response = await http.getAxiosInstance().post(endpoints.chat.stream, {
      sessionId,
      message: content,
    })
    return response.data
  }

  /** 获取对话历史 */
  static async getHistory(sessionId: number): Promise<ApiResponse<AiChatRecord[]>> {
    const response = await http.getAxiosInstance().get(endpoints.chat.history, {
      params: { sessionId },
    })
    return response.data
  }
}

export class ResumeApiService {
  /** 上传简历 */
  static async upload(file: File): Promise<ApiResponse<ResumeInfo>> {
    return http.upload(endpoints.resume.upload, file)
  }

  /** 获取简历列表 */
  static async getList(): Promise<ApiResponse<ResumeInfo[]>> {
    const response = await http.getAxiosInstance().get(endpoints.resume.list)
    return response.data
  }

  /** 获取简历详情 */
  static async getDetail(id: number): Promise<ApiResponse<ResumeInfo>> {
    const response = await http.getAxiosInstance().get(endpoints.resume.detail, {
      params: { id },
    })
    return response.data
  }

  /** 删除简历 */
  static async delete(id: number): Promise<ApiResponse<boolean>> {
    const response = await http.getAxiosInstance().delete(endpoints.resume.delete, {
      params: { id },
    })
    return response.data
  }

  /** 预览简历 */
  static async preview(id: number): Promise<ApiResponse<string>> {
    const response = await http.getAxiosInstance().get(endpoints.resume.preview, {
      params: { id },
    })
    return response.data
  }

  /** 简历分析 */
  static async analyse(id: number): Promise<ApiResponse<ResumeInfo>> {
    const response = await http.getAxiosInstance().get(endpoints.resume.analyse, {
      params: { id },
    })
    return response.data
  }
}

export default InterviewApiService
