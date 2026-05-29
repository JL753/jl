import http from './http'

export const apiLogin = (payload) => http.post('/auth/login', payload)
export const apiRegister = (payload) => http.post('/auth/register', payload)
export const apiMe = () => http.get('/auth/me')
export const apiUpdateMe = (payload) => http.put('/auth/me', payload)

export const apiBuildProfile = (payload) => http.post('/profile/dialogue', payload)
export const apiGetProfile = () => http.get('/profile/mine')
