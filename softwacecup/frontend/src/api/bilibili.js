import http from './http'

export const apiBilibiliParse = (url) => http.post('/bilibili/parse', { url })
export const apiBilibiliSubtitles = (bvid) => http.post('/bilibili/subtitles', { bvid })
export const apiBilibiliSearch = (keyword, page = 1, pageSize = 10) => http.post('/bilibili/search', { keyword, page, pageSize })
export const apiBilibiliPlaylist = (url) => http.post('/bilibili/playlist', { url })
export const apiBilibiliImport = (bvids, autoGenerate = true) => http.post('/bilibili/import', { bvids, autoGenerate })
export const apiBilibiliImportPlaylist = (bvids, courseName, autoGenerate = true) => http.post('/bilibili/import-playlist', { bvids, courseName, autoGenerate })
export const apiMyImports = () => http.get('/lessons/my-imports')
