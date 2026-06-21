import { useState, useEffect, useRef } from 'react'
import { useDebounce } from '../hooks/useDebounce'
import { getSuggestions } from '../api/typeaheadApi'
import SuggestionDropdown from './SuggestionDropdown'

export default function SearchBox({ onSearchSubmit }) {
  // (1) The raw input value — updates on every keystroke
  const [inputValue, setInputValue] = useState('')
  // (2) Suggestions returned from the backend
  const [suggestions, setSuggestions] = useState([])
  // (3) Loading state — true while waiting for the API response
  const [isLoading, setIsLoading] = useState(false)
  // (4) Error state — holds an error message if the request fails
  const [error, setError] = useState(null)
  // (5) Whether the dropdown is visible
  const [showDropdown, setShowDropdown] = useState(false)
  // (6) Currently highlighted suggestion index (for keyboard nav)
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  // (7) Whether the last response was a cache hit (shown for demo/debug)
  const [cacheHit, setCacheHit] = useState(null)

  // (8) Debounce the input — only fires 300ms after typing stops
  const debouncedValue = useDebounce(inputValue, 300)

  // (9) Ref to detect clicks outside the search box (to close dropdown)
  const containerRef = useRef(null)

  // ─────────────────────────────────────────────
  // FETCH SUGGESTIONS WHEN DEBOUNCED VALUE CHANGES
  // ─────────────────────────────────────────────
  useEffect(() => {
    // (10) Don't fire a request for empty input
    if (!debouncedValue || debouncedValue.trim() === '') {
      setSuggestions([])
      setShowDropdown(false)
      return
    }

    // (11) Track whether this effect is still "current" —
    // prevents a slow earlier request from overwriting a faster later one
    let isCancelled = false

    async function fetchSuggestions() {
      setIsLoading(true)
      setError(null)
      try {
        const data = await getSuggestions(debouncedValue)
        // (12) Only update state if this effect hasn't been superseded
        if (!isCancelled) {
          setSuggestions(data.suggestions || [])
          setCacheHit(data.cacheHit)
          setShowDropdown(true)
          setHighlightedIndex(-1)
        }
      } catch (err) {
        if (!isCancelled) {
          setError('Failed to fetch suggestions. Is the backend running?')
          setSuggestions([])
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false)
        }
      }
    }

    fetchSuggestions()

    // (13) Cleanup: mark this effect's request as stale if a newer one starts
    return () => {
      isCancelled = true
    }
  }, [debouncedValue])

  // ─────────────────────────────────────────────
  // CLOSE DROPDOWN ON OUTSIDE CLICK
  // ─────────────────────────────────────────────
  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // ─────────────────────────────────────────────
  // KEYBOARD NAVIGATION
  // ─────────────────────────────────────────────
  function handleKeyDown(e) {
    if (!showDropdown || suggestions.length === 0) {
      if (e.key === 'Enter') {
        handleSubmit(inputValue)
      }
      return
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightedIndex((prev) =>
        prev < suggestions.length - 1 ? prev + 1 : prev
      )
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : -1))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      const chosen =
        highlightedIndex >= 0 ? suggestions[highlightedIndex] : inputValue
      handleSubmit(chosen)
    } else if (e.key === 'Escape') {
      setShowDropdown(false)
    }
  }

  // ─────────────────────────────────────────────
  // SUBMIT (search button, Enter key, or suggestion click)
  // ─────────────────────────────────────────────
  function handleSubmit(query) {
    if (!query || query.trim() === '') return
    setInputValue(query)
    setShowDropdown(false)
    onSearchSubmit(query) // (14) Bubble up to parent (App.jsx) to call POST /search
  }

  function handleSuggestionClick(suggestion) {
    handleSubmit(suggestion)
  }

  return (
    <div className="search-box-container" ref={containerRef}>
      <div className="search-input-row">
        <input
          type="text"
          className="search-input"
          placeholder="Search for products, brands..."
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => suggestions.length > 0 && setShowDropdown(true)}
        />
        <button
          className="search-button"
          onClick={() => handleSubmit(inputValue)}
        >
          Search
        </button>
      </div>

      {/* (15) Loading indicator */}
      {isLoading && <div className="status-message loading">Loading...</div>}

      {/* (16) Error message */}
      {error && <div className="status-message error">{error}</div>}

      {/* (17) Cache hit indicator — useful for your demo/viva */}
      {!isLoading && !error && cacheHit !== null && debouncedValue && (
        <div className={`cache-indicator ${cacheHit ? 'hit' : 'miss'}`}>
          {cacheHit ? '⚡ Cache HIT' : '🔍 Cache MISS (Trie lookup)'}
        </div>
      )}

      {/* (18) Suggestion dropdown */}
      {showDropdown && suggestions.length > 0 && (
        <SuggestionDropdown
          suggestions={suggestions}
          highlightedIndex={highlightedIndex}
          onSelect={handleSuggestionClick}
        />
      )}

      {/* (19) "No matches" message */}
      {showDropdown && !isLoading && suggestions.length === 0 && debouncedValue && (
        <div className="status-message no-results">
          No suggestions found for "{debouncedValue}"
        </div>
      )}
    </div>
  )
}