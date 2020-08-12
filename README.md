Kafka Producer Consumer Test
============
This codebase aims to test MirrorMaker 2 ["Exactly once" feature](https://cwiki.apache.org/confluence/display/KAFKA/KIP-656%3A+MirrorMaker2+Exactly-once+Semantics) to make sure the data can be mirrored from source to target cluster exactly-once, even under failure cases. 

[Exactly-once Example from Kafka Github](https://github.com/apache/kafka/blob/2.5/examples/src/main/java/kafka/examples/KafkaExactlyOnceDemo.java) shows the exactly-once semantics (EOS) by creating producers to send pre-defined number of records and consumers to receive them from the same cluster, then compare if the numbers are equal. 

In the MirrorMaker case, there will be one source cluster and one target cluster, instead one cluster, but the producer and consumer setup are same, so the codebase in this repo mostly reuse the above community efforts and best practices.

There are still some diff from the open-source:

- deploy-able to k8s
- multi-thread
- multiple topics
- sleep time between two records that are produced

How to build and make docker image
-----------------

- `mvn package` build a jar-with-dependencies under /target

- `docker build -t kafka-mm-test ./` build a docker image (named kafka-mm-test) from Dockerfile

- `docker image tag kafka-mm-test:latest <entity_name>/kafka-mm-test:latest` tag the local docker image

- `docker push <entity_name>/kafka-mm-test:latest` push the local image to dockerhub

example yaml for Docker
-----------------

```
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: kafka--test
  name: kafka-test
  namespace: kafka
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka-test
  template:
    metadata:
      labels:
        app: kafka-test
    spec:
      containers:
      - image: ning2008wisc/kafka-test:0.15
        imagePullPolicy: IfNotPresent
        name: kafka-test
        ports:
        - containerPort: 8080
        env:
        - name: TOPICS
          value: foo,bar
        - name: SOURCE_BROKER_LIST
          value: kafka:9092
        - name: TARGET_BROKER_LIST
          value: kafka2:9092
        - name: NUM_THREAD
          value: "1"
        - name: NUM_RECORD
          value: "10"
        - name: SLEEP_TIME_MS
          value: "1000"
```