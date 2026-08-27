import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './style.css'
import './auth.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#536dfe', borderRadius: 10 } }}><App /></ConfigProvider></React.StrictMode>,
)
