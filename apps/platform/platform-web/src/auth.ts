export type UserSession={id:number;username:string;displayName:string;enabled:boolean;roles:string[];permissions:string[]}
export type Session={accessToken:string;expiresAt:string;user:UserSession}
const KEY='platform-session'
export const storedSession=():Session|undefined=>{try{const value=localStorage.getItem(KEY);if(!value)return;const session=JSON.parse(value) as Session;if(new Date(session.expiresAt)<=new Date()){localStorage.removeItem(KEY);return}return session}catch{return}}
export const saveSession=(session:Session)=>localStorage.setItem(KEY,JSON.stringify(session))
export const clearSession=()=>localStorage.removeItem(KEY)
export const can=(permission:string)=>storedSession()?.user.permissions.includes(permission)??false
export async function request<T>(url:string,options:RequestInit={}):Promise<T>{
  const token=storedSession()?.accessToken
  const response=await fetch(url,{...options,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{ }),...(options.headers||{})}})
  if(response.status===401){clearSession();window.dispatchEvent(new Event('platform-auth-expired'))}
  if(!response.ok){let detail=`请求失败 (${response.status})`;try{detail=(await response.json()).detail||detail}catch{}throw new Error(detail)}
  if(response.status===204)return undefined as T
  return response.json()
}
export const login=(username:string,password:string)=>request<Session>('/api/auth/login',{method:'POST',body:JSON.stringify({username,password})})
export const logout=()=>request<{loggedOut:boolean}>('/api/auth/logout',{method:'POST'})
