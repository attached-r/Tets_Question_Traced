/**
 * 生成提交编号：REQ-yyyyMMdd-6位随机
 * 编号是幂等键，必须由提交端生成且足够随机，避免两个人撞号。
 */
export function genRequestId(): string {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  const date = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`

  const bytes = new Uint8Array(3)
  crypto.getRandomValues(bytes)
  const random = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('').toUpperCase()

  return `REQ-${date}-${random}`
}
