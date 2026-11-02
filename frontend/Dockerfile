# 1단계: React 빌드 (VITE_API_BASE_URL을 비워 두면 '/api' — Nginx가 같은 주소에서 백엔드로 넘긴다)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2단계: Nginx가 정적 파일을 주고 /api·/ws-chat은 백엔드로 프록시
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
