FROM eclipse-temurin:21-jdk
COPY ./target/scrapper.jar scrapper.jar
ENTRYPOINT ["java","-jar","/scrapper.jar"]
