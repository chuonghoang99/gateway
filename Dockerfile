FROM openjdk:11.0.16-jre-buster

RUN apt-get update && apt-get install -y curl

ARG build_version="1.0"
ARG release_date=""
ARG app_name=gateway
ARG PROFILE=""

ENV PROFILE="${PROFILE}" \
    DB_URL="" \
    DB_USER="" \
    DB_PASSWORD=""

RUN mkdir -p /deployments/afstech/
WORKDIR /deployments/afstech/
#COPY --from=builder /app/target /deployments/
COPY cache/${app_name}-${build_version}.jar /deployments/
ADD https://raw.githubusercontent.com/fabric8io-images/run-java-sh/master/fish-pepper/run-java-sh/fp-files/run-java.sh /deployments/run-java.sh
RUN chmod +x /deployments/run-java.sh
CMD /deployments/run-java.sh --spring.profiles.active=${PROFILE} \
    --spring.application.name=${APPLICATION_NAME} \
    --spring.datasource.url=${DB_URL} \
    --spring.datasource.username=${DB_USER} \
    --spring.datasource.password=${DB_PASSWORD}
