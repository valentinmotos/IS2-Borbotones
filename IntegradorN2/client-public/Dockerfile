# Imagen del sitio promocional (E0-09). Se construye con contexto en IntegradorN2/
# (los clientes son workspaces de npm y usan packages/), ver docker-compose.yml.

# ---- Compilación ----
FROM node:22-alpine AS build
WORKDIR /app
# Primero solo los package.json: npm ci queda en caché mientras no cambien las dependencias
COPY package.json package-lock.json ./
COPY client-admin/package.json client-admin/
COPY client-public/package.json client-public/
COPY packages/http/package.json packages/http/
RUN npm ci --no-audit --no-fund
COPY packages packages
COPY client-public client-public
# Las variables VITE_* son públicas: quedan dentro del build. Nunca poner secretos.
ARG VITE_API_URL=/api
ARG VITE_ADMIN_URL=http://localhost:5173
ENV VITE_API_URL=$VITE_API_URL VITE_ADMIN_URL=$VITE_ADMIN_URL
RUN npm run build -w client-public

# ---- Ejecución ----
FROM nginx:1.27-alpine
COPY docker/nginx-cliente.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/client-public/dist /usr/share/nginx/html
EXPOSE 80
