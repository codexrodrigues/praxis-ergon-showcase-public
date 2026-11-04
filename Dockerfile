FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn/local-repo/io /root/.m2/repository/io
COPY pom.xml .
COPY src ./src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
# Render's per-instance SSH access requires this directory in the image.
# Keep it empty: public-key admission remains managed by Render.
RUN install -d -m 0700 /root/.ssh
ENV PORT=8080
# Let the container memory limit govern metaspace instead of imposing a second,
# undersized ceiling. Exit-on-OOM lets Render replace a failed process promptly
# instead of leaving an HTTP port backed by a degraded JVM.
ENV JAVA_OPTS="-XX:+UseSerialGC -Xms96m -Xmx220m -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=15 -XX:-ShrinkHeapInSteps -XX:ReservedCodeCacheSize=24m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss256k -XX:MaxDirectMemorySize=10m -XX:+ExitOnOutOfMemoryError -Dserver.tomcat.threads.max=15 -Dserver.tomcat.threads.min-spare=2 -Dspring.jmx.enabled=false -Dspring.datasource.hikari.maximum-pool-size=2 -Dspring.datasource.hikari.minimum-idle=1 -Dfile.encoding=UTF-8"
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
