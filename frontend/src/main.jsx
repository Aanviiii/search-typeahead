import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './App.css'

// (1) React 18's new root API — replaces the old ReactDOM.render()
// (2) StrictMode helps catch bugs during development (double-renders
//     intentionally to surface side-effect issues) — has no effect in production
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)