/**
 * HTTP 请求封装：统一处理 /api 前缀、JSON 序列化与响应包装。
 */
import type { ApiEnvelope } from './types'

/**
 * 发起接口请求并解包统一响应。
 *
 * @param path  接口路径（以 /api 开头）
 * @param method HTTP 方法
 * @param body  请求体（可选，自动转 JSON）
 * @returns 解包后的业务数据
 * @throws 业务失败时抛出携带后端提示信息的异常
 */
export async function request<T>(path: string, method: string, body?: unknown): Promise<T> {
  // 组装请求：JSON 头 + 可选请求体
  const response = await fetch(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  // 解析统一响应结构
  const envelope = (await response.json()) as ApiEnvelope<T>
  // 业务失败：code 非 0 时把后端提示抛给调用方展示
  if (envelope.code !== 0) {
    throw new Error(envelope.message)
  }
  return envelope.data
}
