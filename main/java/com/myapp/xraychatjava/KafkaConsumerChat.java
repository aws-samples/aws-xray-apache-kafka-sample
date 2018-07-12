/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.myapp.xraychatjava;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Subsegment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

public class KafkaConsumerChat implements Runnable {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final KafkaConsumer<String,String> consumer;
    public static final String KafkaTopic = "xraychat";
    public static HashMap<String,String> chatRecords = new HashMap<>();
    
    //Since the Kafka Consumer runs on a separate thread you require the global recorder and trace entity
    //The global recorder and trace entity ensure that X-Ray records the subsegment
    //X-Ray recorder
    private final AWSXRayRecorder recorder = AWSXRay.getGlobalRecorder();
    
    //trace entity 
    private Entity traceEn;
     
     public KafkaConsumerChat(Entity traceEnPassed){
        Properties props = new Properties();
        props.put("bootstrap.servers", "<your kafka ip address and port>");//add your Kafka instance IP and port address
        props.put("group.id", "test");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "com.myapp.xraychatjava.MessageDeserializer");
        this.traceEn = traceEnPassed;
        this.consumer = new KafkaConsumer<>(props);
        this.closed.set(false);
     }

    @Override
     public void run() {
         //sets the trace entity to the recorder to ensure subsegment that is running on a different thread is added to the trace
         this.recorder.setTraceEntity(this.traceEn);
         //begin subsegment in the trace recorder to add the subsegment to the Kafka Consumer segment
         Subsegment kafkaChatSS = this.recorder.beginSubsegment("KafkaChatSubSegment");
         
         try {
            kafkaChatSS.putAnnotation("parentID",this.traceEn.getId());
            
            consumer.subscribe(Arrays.asList(KafkaTopic));
            while (!closed.get()) {
                 ConsumerRecords<String,String> records = consumer.poll(10000);
                 
                if(records.isEmpty()){
                    System.out.printf("Records in KafkaConsumer is empty");
                    kafkaChatSS.putAnnotation("Empty",true);
                }
                else{
                    System.out.printf("Records in KafkaConsumer is NOT empty");
                    kafkaChatSS.putAnnotation("Empty",false);
                    for (ConsumerRecord<String, String> record : records){
                        kafkaChatSS.putAnnotation("message",true);
                        System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                        chatRecords.put(record.key(), record.value());
                    }
                }
             }
         } catch (WakeupException e) {
             // Ignore exception if closing
             if (!closed.get()){
                 System.out.printf("WakeupException on KafkaConsumerChat: "+e.getMessage());
                 //add exception to subsegment
                 kafkaChatSS.addException(e);
             }
         } catch (Exception ee){
             System.out.printf("Other exception on KafkaConsumerChat: "+ee.getMessage());
             //add exception to subsegment
             kafkaChatSS.addException(ee);
         } finally {
             consumer.close();
             //close subsegment
             this.recorder.endSubsegment();
         }
     }

     // Shutdown hook which can be called from a separate thread
     public void shutdown() {
         closed.set(true);
         consumer.wakeup();
         chatRecords.clear();
     }
 }