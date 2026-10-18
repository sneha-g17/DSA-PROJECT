FROM eclipse-temurin:17-jdk-jammy AS build
RUN apt-get update && apt-get install -y --no-install-recommends g++ curl unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY . .
RUN bash build.sh

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends libstdc++6 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/transit_engine build/transit_engine
COPY --from=build /app/backend/target/oneroute-api.jar backend/target/oneroute-api.jar
COPY data data
RUN useradd --create-home planner && chown -R planner:planner /app
USER planner
EXPOSE 8081
CMD ["java", "-jar", "backend/target/oneroute-api.jar", "--server.address=0.0.0.0"]
