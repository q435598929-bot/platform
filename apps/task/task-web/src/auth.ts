export type UserSession={id:number;username:string;displayName:string;enabled:boolean;roles:string[];permissions:string[]}
export type Session={accessToken:string;expiresAt:string;user:UserSession}
const KEY='platform-session'
export const storedSession=():Session|undefined=>{try{const raw=localStorage.getItem(KEY);if(!raw)return;const value=JSON.parse(raw) as Session;if(new Date(value.expiresAt)<=new Date()){localStorage.removeItem(KEY);return}return value}catch{return}}
export const saveSession=(session:Session)=>localStorage.setItem(KEY,JSON.stringify(session))
export const clearSession=()=>localStorage.removeItem(KEY)
export async function authorizedRequest<T>(url:string,options:RequestInit={}):Promise<T>{const token=storedSession()?.accessToken;const headers=new Headers(options.headers);if(!(options.body instanceof FormData))headers.set('Content-Type','application/json');if(token)headers.set('Authorization',`Bearer ${token}`);const response=await fetch(url,{...options,headers});if(response.status===401){clearSession();window.dispatchEvent(new Event('platform-auth-expired'))}if(!response.ok){let detail=`请求失败 (${response.status})`;try{detail=(await response.json()).detail||detail}catch{}throw new Error(detail)}if(response.status===204)return undefined as T;return response.json()}
export const login=(username:string,password:string)=>authorizedRequest<Session>('/platform-api/auth/login',{method:'POST',body:JSON.stringify({username,password})})
export const me=()=>authorizedRequest<UserSession>('/platform-api/auth/me')
export const logout=()=>authorizedRequest('/platform-api/auth/logout',{method:'POST'})
