export default function SuggestionDropdown({ suggestions, highlightedIndex, onSelect }) {
  return (
    <ul className="suggestion-dropdown" role="listbox">
      {suggestions.map((suggestion, index) => (
        <li
          key={suggestion}
          // (1) Highlight the keyboard-selected item
          className={`suggestion-item ${index === highlightedIndex ? 'highlighted' : ''}`}
          // (2) onMouseDown (not onClick) fires BEFORE the input's onBlur,
          // so clicking a suggestion works even though the input loses focus
          onMouseDown={() => onSelect(suggestion)}
          role="option"
          aria-selected={index === highlightedIndex}
        >
          {suggestion}
        </li>
      ))}
    </ul>
  )
}