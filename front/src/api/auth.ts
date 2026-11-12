import { get, post } from './request'

export interface UserInfo {
  id: number
  account: string
  nickname: string
  phone?: string
  avatar?: string
}

export interface LoginResult {
  token: string
  user?: UserInfo
}

export interface Captcha {
  captchaId: string
  image: string
}

export interface AuthPayload {
  account: string
  password: string
  captchaId: string
  captchaCode: string
}

/** 获取图形验证码 */
export const getCaptcha = () => get<Captcha>('/auth/captcha')

/** 账号 + 密码 + 图形验证码注册 */
export const register = (data: AuthPayload) => post<LoginResult>('/auth/register', data)

/** 账号 + 密码 + 图形验证码登录 */
export const login = (data: AuthPayload) => post<LoginResult>('/auth/login', data)

export const logout = () => post('/auth/logout')

export const me = () => get<UserInfo>('/auth/me')
