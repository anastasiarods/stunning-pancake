FROM adoptopenjdk/openjdk11:alpine-jre
COPY images.jar /srv/images.jar
CMD ["java", "-jar", "/srv/images.jar"]
