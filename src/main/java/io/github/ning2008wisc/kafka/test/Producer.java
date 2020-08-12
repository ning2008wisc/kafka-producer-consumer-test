package io.github.ning2008wisc.kafka.test;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Producer extends Thread {
    private final KafkaProducer<Integer, String> producer;
    private final String[] topics;
    private final Boolean isAsync;
    private int numRecords;
    private final CountDownLatch latch;
    private volatile boolean running = true;
    private int sleepTimeMs = 0;

    public Producer(final String[] topics,
    		        final String brokerList,
                    final Boolean isAsync,
                    final String transactionalId,
                    final boolean enableIdempotency,
                    final int numRecords,
                    final int transactionTimeoutMs,
                    final CountDownLatch latch,
                    final int sleepTimeMs) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "DemoProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        if (transactionTimeoutMs > 0) {
            props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, transactionTimeoutMs);
        }
        if (transactionalId != null) {
            props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);
        }
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotency);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        producer = new KafkaProducer<>(props);
        this.topics = topics;
        this.isAsync = isAsync;
        this.numRecords = numRecords;
        this.latch = latch;
        this.sleepTimeMs = sleepTimeMs;
    }

    KafkaProducer<Integer, String> get() {
        return producer;
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run() {
        int messageKey = 0;
        int recordsSent = 0;
        System.out.println("numRecords = " + numRecords);
        while (recordsSent < numRecords) {
            String messageStr = "Message_" + messageKey;
            long startTime = System.currentTimeMillis();
            for (String topic : topics) {
            	if (isAsync) { // Send asynchronously
            		producer.send(new ProducerRecord<>(topic,
            				messageKey,
            				messageStr), new DemoCallBack(startTime, messageKey, messageStr));
            	} else { // Send synchronously
            		try {
            			RecordMetadata meta = producer.send(new ProducerRecord<>(topic,
            					messageKey,
            					messageStr)).get();
            			System.out.println("sent message : to topic " + meta.topic() + " to partition " + meta.partition() + ", (" + messageKey + ", " + messageStr + ") at offset " + meta.offset());
            			//System.out.println("Sent message: (" + messageKey + ", " + messageStr + ")");
            		} catch (InterruptedException | ExecutionException e) {
            			e.printStackTrace();
            		}
            	}
            	if (sleepTimeMs > 0) {
            		try {
            			Thread.sleep(sleepTimeMs);
            		} catch (InterruptedException e) {
            			e.printStackTrace();
            		}
            	}
            }
            messageKey += 2;
            recordsSent += 1 * topics.length;
        }
        System.out.println("Producer sent " + numRecords + " records successfully");
        latch.countDown();
    }
}

class DemoCallBack implements Callback {

    private final long startTime;
    private final int key;
    private final String message;

    public DemoCallBack(long startTime, int key, String message) {
        this.startTime = startTime;
        this.key = key;
        this.message = message;
    }

    /**
     * A callback method the user can implement to provide asynchronous handling of request completion. This method will
     * be called when the record sent to the server has been acknowledged. When exception is not null in the callback,
     * metadata will contain the special -1 value for all fields except for topicPartition, which will be valid.
     *
     * @param metadata  The metadata for the record that was sent (i.e. the partition and offset). Null if an error
     *                  occurred.
     * @param exception The exception thrown during processing of this record. Null if no error occurred.
     */
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (metadata != null) {
            System.out.println(
                "message(" + key + ", " + message + ") sent to partition(" + metadata.partition() +
                    "), " +
                    "offset(" + metadata.offset() + ") in " + elapsedTime + " ms");
        } else {
            exception.printStackTrace();
        }
    }
}
