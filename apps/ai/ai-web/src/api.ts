import axios from 'axios'
import {clearSession,storedSession} from './auth'
export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1', timeout: 125000 })
api.interceptors.request.use(config=>{const token=storedSession()?.accessToken;if(token)config.headers.Authorization=`Bearer ${token}`;return config})
api.interceptors.response.use(response=>response,error=>{if(error.response?.status===401){clearSession();window.dispatchEvent(new Event('platform-auth-expired'))}return Promise.reject(error)})
export type Provider = { id:number; name:string; baseUrl:string; hasApiKey:boolean; enabled:boolean; sortOrder:number; updatedAt:string }
export type Model = { id:number; providerId:number; providerName:string; code:string; displayName:string; enabled:boolean; inputPricePerMillion:number; outputPricePerMillion:number; sortOrder:number; free:boolean; canonicalSlug?:string; remoteCreatedAt?:string; expirationDate?:string; knowledgeCutoff?:string }
export type Stats = { totalCalls:number; successfulCalls:number; successRate:number; inputTokens:number; outputTokens:number; averageDurationMs:number; estimatedCost:number }
