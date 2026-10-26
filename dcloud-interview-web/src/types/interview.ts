/**
 * 面试状态枚举
 */
export enum InterviewStatus {
  GENERATING = 'GENERATING',
  GENERATE_QA = 'GENERATE_QA',
  IN_PROGRESS = 'IN_PROGRESS',
  EVALUATING = 'EVALUATING',
  COMPLETED = 'COMPLETED',
  FAILED_PARSE_RESUME = 'FAILED_PARSE_RESUME',
  FAILED_CREATE_INTERVIEW = 'FAILED_CREATE_INTERVIEW',
  FAILED_GENERATE_QA = 'FAILED_GENERATE_QA',
}

/**
 * 面试类型枚举
 */
export enum InterviewType {
  COMMON_INTERVIEW = 'COMMON_INTERVIEW',
  SPECIAL_INTERVIEW = 'SPECIAL_INTERVIEW',
  RESUME_ANALYSE = 'RESUME_ANALYSE',
}

/**
 * 面试信息
 */
export interface InterviewInfo {
  id: number
  title: string | null
  description: string | null
  type: string | null
  extendContent: string | null
  profile: string | null
  accountId: number | null
  resumeId: number | null
  status: InterviewStatus | null
  overallScore: number | null
  summary: string | null
  strength: string | null
  improvement: string | null
  suggestion: string | null
  technicalSkills: number | null
  logicalThinking: number | null
  communication: number | null
  problemSolving: number | null
  passedQuestion: number | null
  excellentQuestion: number | null
  gmtCreate: string
  gmtModified: string
}

/**
 * 面试轮次信息
 */
export interface InterviewRound {
  id: number
  interviewId: number
  accountId: number | null
  roundNumber: number | null
  roundType: string
  status: 'IN_PROGRESS' | 'EVALUATING' | 'COMPLETED' | null
  description: string | null
  totalQuestion: number
  answeredQuestions: number
  overallScore: number | null
  summary: string | null
  strength: string | null
  improvement: string | null
  suggestion: string | null
  gmtModified: string
  gmtCreate: string
}

/**
 * 题目难度枚举
 */
export enum QuestionDifficulty {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
}

/**
 * 题目类型枚举
 */
export enum QuestionType {
  CODING = 'CODING',
  SHORT_ANSWER = 'SHORT_ANSWER',
}

/**
 * 面试题目
 */
export interface QuestionInfo {
  id: number
  interviewId: number | null
  accountId: number | null
  interviewRoundId: number | null
  status: string | null
  userAnswer: string | null
  evaluation: string | null
  content: string | null
  type: QuestionType | null
  difficulty: QuestionDifficulty | null
  category: string | null
  standardAnswer: string | null
  keyPoints: string | null
  maxScore: number
  timeLimit: number | null
  userScore: number | null
  gmtCreate: string
  gmtModified: string
}

/**
 * AI对话记录
 */
export interface AiChatRecord {
  id: number
  accountId: number
  sessionId: number
  role: 'user' | 'assistant' | 'system' | null
  content: string
  sequence: number | null
  gmtCreate: string
}

/**
 * 简历信息
 */
export interface ResumeInfo {
  id: number
  accountId: number | null
  filename: string | null
  filePath: string | null
  fileType: string | null
  content: string | null
  skillTags: string | null
  evaluation: string | null
  status: string | null
  gmtCreate: string
  gmtModified: string
}

/**
 * 创建面试参数
 */
export interface CreateInterviewParams {
  expectedSalary: string
  workYears: string
  targetCity: string
  targetPosition: string
  interviewType: InterviewType
  specialContent?: string
  resumeId: number
}

/**
 * 提交答案参数
 */
export interface SubmitAnswerParams {
  questionId: number
  interviewId: number
  answer: string
}

/**
 * 面试详情视图响应（/api/v1/interview/view 返回结构）
 */
export interface InterviewViewData {
  interview: InterviewInfo
  rounds: InterviewRoundDetail[]
}

/**
 * 面试轮次详情（含题目列表）
 */
export interface InterviewRoundDetail {
  id: number
  roundNumber: number | null
  roundType: string
  description: string | null
  status: string | null
  totalQuestion: number
  answeredQuestions: number
  overallScore: number | null
  summary: string | null
  strength: string | null
  improvement: string | null
  suggestion: string | null
  questions: QuestionInfo[]
}

/**
 * 用户求职画像（profile JSON）
 */
export interface InterviewProfile {
  expectedSalary: string
  workYears: string
  targetCity: string
  targetPosition: string
  interviewType: string
  specialContent: string
  resumeId: number
}
