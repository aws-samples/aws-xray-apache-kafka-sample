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

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceID;
import java.util.Map;
import java.util.Scanner;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class KafkaProducerInterceptor implements ProducerInterceptor  {
    public static final String KafkaTopic = "xraychat";
    
    @Override
    public ProducerRecord onSend(ProducerRecord record) {
        //intercept the message
        AWSXRayRecorder xrayRecorder = AWSXRayRecorderBuilder.defaultRecorder();
        Subsegment kafkaProducerInterceptorSS = xrayRecorder.beginSubsegment("KafkaProducerInterceptorSS");
        
        //get the parentId and traceId
        String parentId = xrayRecorder.getCurrentSegment().getId();
        TraceID traceId = xrayRecorder.getCurrentSegment().getTraceId();
        
        //format the trace information prior to saving
        String traceInformation = traceId.toString()+"::"+parentId;
        
        //create a new message object with the trace information
        Message interceptedMessage = (Message)record.value();
        
        //now add the message with trace information
        String changedMessage = "intercepted-and-changed-"+interceptedMessage.getMessageText();
        Message messageWithTraceInformation = new Message(interceptedMessage.getToUserName(),changedMessage,traceInformation);
        
        //send the changed intercepted message
        ProducerRecord newRecord = new ProducerRecord<>(KafkaTopic, record.key(), messageWithTraceInformation);
        kafkaProducerInterceptorSS.end();
        
        return newRecord;
    }

    @Override
    public void onAcknowledgement(RecordMetadata rm, Exception e) {
        if(rm != null){
            System.out.printf("KafkaProducerInterceptor onAcknowledgement recordMetadata topic: %s",rm.topic());
        }
        if(e != null){
            System.out.printf("KafkaProducerInterceptor onAcknowledgement exception called: %s",e.getMessage());
        }
    }

    @Override
    public void close() {
        System.out.printf("KafkaProducerInterceptor close() called");
    }

    @Override
    public void configure(Map<String, ?> map) {
        System.out.printf("KafkaProducerInterceptor configure() called");
    }
    
}