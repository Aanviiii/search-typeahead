import { useState, useEffect } from 'react'

// (1) Custom hook: delays updating a value until the user stops
// changing it for `delay` milliseconds
export function useDebounce(value, delay = 300) {
    // (2) debouncedValue only updates after the delay has passed
    // without `value` changing again
    const [debouncedValue, setDebouncedValue] = useState(value)

    useEffect(() => {
        // (3) Set a timer that will update debouncedValue after `delay` ms
        const timer = setTimeout(() => {
            setDebouncedValue(value)
        }, delay)

        // (4) CRITICAL: cleanup function
        // If `value` changes again before the timer fires (user kept typing),
        // React calls this cleanup, cancelling the PREVIOUS timer.
        // Only the timer from the LAST keystroke ever actually fires.
        return () => clearTimeout(timer)
    }, [value, delay])

    return debouncedValue
}