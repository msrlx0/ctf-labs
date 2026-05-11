FROM node:20-alpine

RUN apk add --no-cache iputils bind-tools busybox-extras tar bash

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY . .

EXPOSE 3000

CMD ["npm", "start"]
