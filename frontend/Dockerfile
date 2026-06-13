# Multi-stage build for the fkmovies frontend (Angular 22). Builds the SPA, serves it with nginx,
# and proxies /api and /ws to the backend container (mirrors proxy.conf.json for local dev).
FROM node:22-alpine AS build
WORKDIR /app
COPY . .
RUN npm ci && npm run build

FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/fkmovies-frontend/browser /usr/share/nginx/html
EXPOSE 80
