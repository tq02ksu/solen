FROM openjdk:8-alpine
LABEL maintainer=tq02ksu@gmail.com
WORKDIR /app

COPY . /app/
RUN ./mvnw clean install -DskipTests -U

FROM openjdk:8-alpine
LABEL maintainer=tq02ksu@gmail.com
# https://spring.io/guides/topicals/spring-boot-docker/

WORKDIR /app
RUN apk add libc6-compat
RUN mkdir -p lib bin log; \
    { echo '#!/bin/sh' && echo 'exec java $JAVA_OPTS -jar lib/app.jar ${@}'; } > bin/run.sh; \
    chmod 755 bin/run.sh
COPY --from=0 /app/target/*.jar lib/app.jar

EXPOSE 8080
ENTRYPOINT bin/run.sh
