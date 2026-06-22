import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import Docs from './Docs.jsx'
import './App.css'

// (1) Lightweight path check (the app has no router): serve the interactive
// API docs at /api-docs, otherwise render the typeahead app. Existing routes
// and app behaviour are unchanged.
const path = window.location.pathname.replace(/\/+$/, '')
const isDocs = path === '/api-docs'

// (2) React 18's new root API — replaces the old ReactDOM.render()
// (3) StrictMode helps catch bugs during development (double-renders
//     intentionally to surface side-effect issues) — has no effect in production
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {isDocs ? <Docs /> : <App />}
  </React.StrictMode>,
)
