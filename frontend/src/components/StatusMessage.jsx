export default function StatusMessage({ message, type = 'info' }) {
  if (!message) return null
  return <div className={`status-banner ${type}`}>{message}</div>
}