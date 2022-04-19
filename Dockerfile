FROM node:current-alpine3.15 as FrontendBuilder
COPY ./Frontend /Frontend
WORKDIR /Frontend
RUN npm install
RUN npm run build

FROM openjdk:11.0-jdk-slim-buster as BackendBuilder
COPY ./Backend ./Backend
WORKDIR /Backend
COPY --from=FrontendBuilder /Frontend/build /Backend/src/main/resources/static
RUN chmod +x gradlew
RUN ./gradlew --no-daemon --no-watch-fs build
WORKDIR /Backend/build/libs
RUN mv "$(ls -I '*plain*')" app.jar

FROM openjdk:11.0-jdk-slim-buster as run
WORKDIR /app
COPY --from=BackendBuilder /Backend/build/libs/app.jar .
EXPOSE 8080
CMD [ "java", "-jar", "app.jar" ]
