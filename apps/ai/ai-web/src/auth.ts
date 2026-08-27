export type UserSession={id:number;username:string;displayName:string;enabled:boolean;roles:string[];permissions:string[]}
export type Session={accessToken:string;expiresAt:string;user:UserSession}
const SESSION_KEY='platform-session',USER_KEY='platform-user'
export const storedSession=():Session|undefined=>{try{const raw=localStorage.getItem(SESSION_KEY);if(!raw)return;const s=JSON.parse(raw) as Session;if(new Date(s.expiresAt)<=new Date()){localStorage.removeItem(SESSION_KEY);return}return s}catch{return}}
export const storedUser=():UserSession|undefined=>{try{const raw=localStorage.getItem(USER_KEY);return raw?JSON.parse(raw):storedSession()?.user}catch{return}}
export const saveSession=(s:Session)=>{localStorage.setItem(SESSION_KEY,JSON.stringify(s));localStorage.setItem(USER_KEY,JSON.stringify(s.user))}
export const saveUser=(u:UserSession)=>localStorage.setItem(USER_KEY,JSON.stringify(u))
export const clearSession=()=>{localStorage.removeItem(SESSION_KEY);localStorage.removeItem(USER_KEY)}
export const can=(permission:string)=>storedUser()?.permissions.includes(permission)??false
async function call<T>(url:string,options:RequestInit={}):Promise<T>{const token=storedSession()?.accessToken;const r=await fetch(url,{...options,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{ }),...(options.headers||{})}});if(r.status===401){clearSession();window.dispatchEvent(new Event('platform-auth-expired'))}if(!r.ok){let detail=`请求失败 (${r.status})`;try{detail=(await r.json()).detail||detail}catch{}throw new Error(detail)}return r.json()}
export const login=(username:string,password:string)=>call<Session>('/platform-api/auth/login',{method:'POST',body:JSON.stringify({username,password})})
export const me=()=>call<UserSession>('/platform-api/auth/me')
export const logout=()=>call('/platform-api/auth/logout',{method:'POST'})
