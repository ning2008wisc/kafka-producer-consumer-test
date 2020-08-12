package io.github.ning2008wisc.kafka.test;

import kafka.utils.ShutdownableThread;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class Consumer extends ShutdownableThread {
    private final KafkaConsumer<Integer, String> consumer;
    private final String[] topics;
    private final String groupId;
    private final int numMessageToConsume;
    private int messageRemaining;
    private final CountDownLatch latch;

    public Consumer(final String[] topics,
    		        final String brokerList,
                    final String groupId,
                    final Optional<String> instanceId,
                    final boolean readCommitted,
                    final int numMessageToConsume,
                    final CountDownLatch latch) {
        super("KafkaConsumerExample", false);
        this.groupId = groupId;
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        instanceId.ifPresent(id -> props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, id));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        if (readCommitted) {
            props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
        this.topics = topics;
        this.numMessageToConsume = numMessageToConsume;
        this.messageRemaining = numMessageToConsume;
        this.latch = latch;
    }

    KafkaConsumer<Integer, String> get() {
        return consumer;
    }

    @Override
    public void doWork() {
        consumer.subscribe(Arrays.asList(topics));
        ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<Integer, String> record : records) {
            System.out.println(groupId + " received message : from topic " + record.topic() + " from partition " + record.partition() + ", (" + record.key() + ", " + record.value() + ") at offset " + record.offset());
        }
        consumer.commitSync();
        messageRemaining -= records.count();
        if (messageRemaining <= 0) {
            System.out.println(groupId + " finished reading " + numMessageToConsume + " messages");
            latch.countDown();
        }
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }
}