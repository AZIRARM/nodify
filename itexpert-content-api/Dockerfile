FROM eclipse-temurin:21

RUN echo "Set Workspace"
RUN mkdir -p /app
ENV APP_HOME="/app"

RUN echo "Create folders : bin"
RUN mkdir -p ${APP_HOME}/bin
RUN echo "Create folders : config"
RUN mkdir -p ${APP_HOME}/config
RUN echo "Create folders : logs"
RUN mkdir -p ${APP_HOME}/logs

RUN echo "Copy app"
COPY ./target/*.jar ${APP_HOME}/bin/app.jar

RUN echo "Copy entrypoint"
COPY ./entrypoint.sh /

RUN chmod +x /entrypoint.sh

EXPOSE 9080

ENTRYPOINT ["/entrypoint.sh"]

HEALTHCHECK CMD curl --fail http://localhost:9080/health || exit 1