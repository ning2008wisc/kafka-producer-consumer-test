package io.github.ning2008wisc.kafka.test;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.TimeoutException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Demo {

	private static final String PREFIX = "primary"; //TODO: make PREFIX configurable
	private static final int joinTimeoutMs = 2000;

    static Set<Producer> producerThreads = new HashSet<>();
    
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		/*
        if (args.length != 3) {
            throw new IllegalArgumentException("Should accept 3 parameters: " +
                "[number of partitions], [number of instances], [number of records]");
        } */

        System.out.println("topics = " + args[0]);
        System.out.println("sourceBrokerList = " + args[1]);
        System.out.println("targetBrokerList = " + args[2]);
        System.out.println("numRecords = " + args[3]);
        System.out.println("numThreads = " + args[4]);
        System.out.println("sleep time = " + args[5]);
        System.out.println("mirrored topic prefix = " + args[6]);
        
        
        String[] topics = args[0].split(",");
        String sourceBrokerList = args[1];
        String targetBrokerList = args[2];
        validate(sourceBrokerList, targetBrokerList, topics);
        int numRecords = Integer.parseInt(args[3]);
        int numThreads = Integer.parseInt(args[4]);
        int sleepTimeMs = Integer.parseInt(args[5]); // sleep time between two records that are produced
        String topicPrefix = args[6];
        
        CountDownLatch prePopulateLatch = new CountDownLatch(numThreads);


        System.out.println("topicCount = " + topics.length);
        int totalRecord =  numRecords * topics.length;
        /* Stage 2: pre-populate records */
        for (int i = 0; i < numThreads; i++) {
        	Producer producerThread = new Producer(topics, sourceBrokerList, false, null, true, totalRecord, -1, prePopulateLatch, sleepTimeMs);
        	producerThreads.add(producerThread);
            producerThread.start();
        }

        if (!prePopulateLatch.await(5, TimeUnit.MINUTES)) {
            throw new TimeoutException("Timeout after 5 minutes waiting for data pre-population");
        }
        
        stopProducer();
        
        CountDownLatch consumeLatch = new CountDownLatch(numThreads);

        /* Stage 4: consume all processed messages to verify exactly once */
        String[] mirrorTopics = new String[topics.length];
        for (int i = 0; i < topics.length; i++) {
        	mirrorTopics[i] = topicPrefix + "." + topics[i];
        	System.out.println("mirrorTopics = " + mirrorTopics[i]);
        }
        System.out.println("targetBrokerList = "+ targetBrokerList);
        Consumer consumerThread = new Consumer(mirrorTopics, targetBrokerList, "Verify-consumer", Optional.empty(), true, totalRecord * numThreads, consumeLatch);
        consumerThread.start();
        
        if (!consumeLatch.await(5, TimeUnit.MINUTES)) {
            throw new TimeoutException("Timeout after 5 minutes waiting for output data consumption");
        }
        
        consumerThread.shutdown();
        consumerThread.join(joinTimeoutMs);
        
        System.out.println("All finished!");
	}
	
	private static synchronized void stopProducer() throws InterruptedException {
        for (Producer producer : producerThreads) {
            System.out.println("joining producer thread id: " + producer.getId());
            joinProducer(producer, joinTimeoutMs);
            System.out.println("joined producer thread id: " + producer.getId());
        }
	}
	
	private static void joinProducer(Producer producer, long timeoutMs) throws InterruptedException {
        try {
        	System.out.println("calling join on producer id: " + producer.getId());
            producer.join(timeoutMs);
        } catch (InterruptedException e) {
        	System.out.println("InterruptedException while joining producer id: " + producer.getId() + ". Not fatal.");
        } catch (IllegalStateException e) {
        	System.out.println("Illegal state exception while joining producer id: " + producer.getId() + ": " + e);
        } finally {
        	producer.get().close();
        	System.out.println("closing kafka producer");
        }
    }

	// TODO: verify that the topics exists on source and target cluster
	private static void validate(String sourceBrokerList, String targetBrokerList, String[] topics) {
		
	}
}
