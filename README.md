# ChordFileSystem


## compile and run:
mvn clean package
docker compose -f docker/docker-compose.yml up -d --build

## execute command CLI on a container:
docker exec -it docker-node5-1 bash
java -cp /app/build/ChordFileSystem-1.0-SNAPSHOT.jar it.baddy.uni.commands.ManualCommand


locale:
java -jar target/ChordFileSystem-1.0-SNAPSHOT.jar