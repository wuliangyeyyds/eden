# 构建阶段
#FROM m.daocloud.io/docker.io/library/node:16 AS builder
FROM 79ef49c1615f431398ff867142fc29ba.mirror.swr.myhuaweicloud.com/library/node:16 AS builder
WORKDIR /app
LABEL authors="cloudby"

COPY package.json package-lock.json ./
RUN npm install

COPY . .
RUN npm run build

# 运行阶段
FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]