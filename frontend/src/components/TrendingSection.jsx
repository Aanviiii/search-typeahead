import { useState, useEffect } from 'react'
import { getTrending } from '../api/typeaheadApi'

export default function TrendingSection({ refreshTrigger }) {
  const [trending, setTrending] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    async function fetchTrending() {
      setIsLoading(true)
      setError(null)
      try {
        const data = await getTrending()
        setTrending(data.trending || [])
      } catch (err) {
        setError('Could not load trending searches')
      } finally {
        setIsLoading(false)
      }
    }
    fetchTrending()

    // (1) Re-fetch every 15 seconds — picks up batch-flushed score changes
    // without requiring a manual page reload
    const interval = setInterval(fetchTrending, 15000)
    return () => clearInterval(interval)

    // (2) refreshTrigger lets the parent force an immediate refresh
    // right after the user submits a search, for a more responsive demo
  }, [refreshTrigger])

  if (isLoading && trending.length === 0) {
    return <div className="trending-section loading">Loading trending searches...</div>
  }

  if (error) {
    return <div className="trending-section error">{error}</div>
  }

  return (
    <div className="trending-section">
      <h3>🔥 Trending Searches</h3>
      <ol className="trending-list">
        {trending.map((item) => (
          <li key={item.query} className="trending-item">
            <span className="trending-query">{item.query}</span>
            <span className="trending-score">
              score: {item.score.toFixed(0)}
            </span>
          </li>
        ))}
      </ol>
    </div>
  )
}