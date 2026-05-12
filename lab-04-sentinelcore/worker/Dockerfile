FROM node:20-alpine

WORKDIR /worker

COPY package.json .
RUN npm install --omit=dev

COPY . .

CMD ["npm", "start"]
