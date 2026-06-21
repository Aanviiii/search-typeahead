import { useState } from 'react'
import SearchBox from './components/SearchBox'
import TrendingSection from './components/TrendingSection'
import StatusMessage from './components/StatusMessage'
import { postSearch } from './api/typeaheadApi'

export default function App() {
  // (1) Holds the dummy search response message
  const [searchStatus, setSearchStatus] = useState(null)
  const [searchStatusType, setSearchStatusType] = useState('info')
  // (2) Bumped after every search to trigger TrendingSection to refresh
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // (3) Called by SearchBox when user submits (Enter, button click, or suggestion click)
  async function handleSearchSubmit(query) {
    try {
      const data = await postSearch(query)
      // (4) Display the exact backend response — required by Section 9: 
      // "Display of the dummy search response"
      setSearchStatus(`"${query}" → ${data.message}`)
      setSearchStatusType('success')
      // (5) Trigger trending section to refresh shortly after,
      // demonstrating eventual consistency once batch flush completes
      setRefreshTrigger((prev) => prev + 1)
    } catch (err) {
      setSearchStatus('Failed to submit search. Is the backend running?')
      setSearchStatusType('error')
    }
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>🔍 Search Typeahead System</h1>
        <p className="app-subtitle">
          HLD Project — Java Spring Boot + Consistent Hashing Cache + Trie
        </p>
      </header>

      <main className="app-main">
        <SearchBox onSearchSubmit={handleSearchSubmit} />

        <StatusMessage message={searchStatus} type={searchStatusType} />

        <TrendingSection refreshTrigger={refreshTrigger} />
      </main>

      <footer className="app-footer">
        <p>Backend: Spring Boot 3.2 · Java 21 · SQLite · Consistent Hashing Cache</p>
      </footer>
    </div>
  )
}