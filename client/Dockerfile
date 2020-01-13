FROM node:alpine as build 
WORKDIR /app
COPY package*.json ./
RUN npm install 
COPY . .
RUN npm run build

FROM nginx 
EXPOSE 8080
COPY nginx/ /etc/nginx/conf.d/
COPY --from=build /app/build /usr/share/nginx/html
