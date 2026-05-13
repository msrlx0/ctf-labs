FROM node:20-alpine

WORKDIR /internal-admin

COPY package.json .
RUN npm install --omit=dev

COPY . .

EXPOSE 8081

CMD ["npm", "start"]
