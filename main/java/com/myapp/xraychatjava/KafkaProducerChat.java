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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;

public class KafkaProducerChat {
    public static final String KafkaTopic = "xraychat";
    
    public static void main(Message message){
        Subsegment kafkaProducerPropertiesSubsegment = AWSXRay.beginSubsegment("KafkaProducerProperties");
        Properties props = new Properties();
        props.put("bootstrap.servers", "<your kafka ip address and port>");//add your Kafka instance IP and port address
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "com.myapp.xraychatjava.MessageSerializer");
        props.put("interceptor.classes","com.myapp.xraychatjava.KafkaProducerInterceptor");
        kafkaProducerPropertiesSubsegment.end();
        
        //subsegment for the Kafka Producer
        Subsegment kafkaProducerSendSubsegment = AWSXRay.beginSubsegment("KafkaProducerSend");
        try (Producer<String, Message> producer = new KafkaProducer<>(props)) {
            String toUsername = message.getToUserName();
            
            producer.send(new ProducerRecord<>(KafkaTopic, toUsername, message));
        }
        catch(Exception e){
            System.out.printf("Exception while getting KafkaProducer: %s",e.getMessage());
            //add exception to the subsegment
            kafkaProducerSendSubsegment.addException(e);
        }
        finally{
            //end subsegment
            kafkaProducerSendSubsegment.end();
        }
    }
}