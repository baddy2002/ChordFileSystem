FROM eclipse-temurin:21-jre

WORKDIR /app

ENV LANGUAGE='en_US:en'
ENV JAVA_APP_JAR="/app/build/ChordFileSystem-1.0-SNAPSHOT.jar"

COPY target/*.jar /app/build/

EXPOSE 1099

CMD ["sh", "-c", "java \
  -Djava.rmi.server.hostname=${NODE_HOSTNAME} \
  -Dnode.id=${NODE_ID} \
  -Dnode.port=${NODE_PORT} \
  -jar ${JAVA_APP_JAR}"]
