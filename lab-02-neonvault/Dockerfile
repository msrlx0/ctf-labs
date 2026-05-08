FROM node:20-alpine

WORKDIR /usr/src/neonvault

COPY package.json ./
RUN npm install --omit=dev

COPY . .
RUN mkdir -p uploads

EXPOSE 8092

CMD ["npm", "start"]
