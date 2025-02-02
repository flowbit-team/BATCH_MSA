FROM openjdk:11
COPY ./build/libs/batchservice-0.0.1-SNAPSHOT.jar application.jar
ENV TZ=Asia/Seoul


ENTRYPOINT ["java", "-jar", "-Dspring.config.name=bootstrap", "-DSPRING_PROFILE=prod", "-DCONFIG_SERVER_URL=http://flowbit-config:8888/", "/application.jar"]
