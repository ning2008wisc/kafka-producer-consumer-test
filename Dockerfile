FROM openjdk:8u212-jre-alpine

ARG num_record
ARG num_thread

ENV NUM_RECORD=$num_record
ENV NUM_THREAD=$num_thread

RUN apk add --no-cache bash
ADD target/kafka-test-1.0-SNAPSHOT-jar-with-dependencies.jar kafka-test-1.0-SNAPSHOT.jar
COPY run /home/
CMD ["/home/run"]
# ENTRYPOINT ["java", "-cp", "kafka-test-1.0-SNAPSHOT.jar", "io.github.ning2008wisc.kafka.test.Demo"]
# CMD [${NUM_RECORD}, ${NUM_THREAD}]

