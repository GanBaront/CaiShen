import { get, post } from './request'

export const submitFeedback = (data: { content: string; contact?: string }) => post('/feedback', data)
export const myFeedback = () => get('/feedback')
