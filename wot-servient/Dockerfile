FROM node:22-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY server.js dashboard.html ./
COPY tds/ ./tds/
EXPOSE 1180 5000
CMD ["node", "server.js"]
