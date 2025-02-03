FROM node:current-slim AS builder

WORKDIR /app

COPY package.json .
RUN npm install

COPY . .
RUN npm run build-prod

FROM nginx:alpine


ENV EXPERT_CONTENT_CORE_URL=http://expert_content_core:8080
ENV EXPERT_CONTENT_API_URL=http://expert_content_api:9080

COPY --from=builder /app/dist/content-ui/* /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/
COPY entrypoint.sh  /etc/nginx/entrypoint.sh

RUN chmod +x /etc/nginx/entrypoint.sh

ENTRYPOINT ["/etc/nginx/entrypoint.sh"]

HEALTHCHECK --start-period=15s --interval=1m --timeout=10s --retries=5 \
            CMD curl --silent --fail --request GET http://localhost/health || exit 1


