FROM node:20-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8102

COPY package.json ./
RUN npm install --omit=dev

COPY src ./src

EXPOSE 8102

CMD ["node", "src/server.js"]
