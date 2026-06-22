import SwaggerUI from 'swagger-ui-react'
import 'swagger-ui-react/swagger-ui.css'

// (1) Interactive API documentation. Renders Swagger UI for the hand-written
// OpenAPI spec at /openapi.yaml (served as a static asset by Vite in dev and
// by nginx in the Docker build). "Try it out" calls hit the /api server entry,
// which the frontend proxy forwards to the backend.
export default function Docs() {
  return <SwaggerUI url="/openapi.yaml" />
}
