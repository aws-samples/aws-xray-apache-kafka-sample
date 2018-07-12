## AWS X-Ray Apache Kafka Sample

Trace end-to-end performance of messaging applications built on Apache Kafka using AWS X-Ray.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.


## Get AWS X-Ray working with Apache Kafka

At a high-level, you have to do three things:
1.	Update the data object that is streamed to have a variable to store the trace ID and segment ID.
2.	Create a class that implements the Kafka ProducerInterceptor interface (link: https://kafka.apache.org/0110/javadoc/org/apache/kafka/clients/producer/ProducerInterceptor.html). Override the onSend(ProducerRecord<K,V> record)  method to store the trace ID and segment ID to the variable in the data object.
3.	Set the trace ID and segment ID to the current segment in the Kafka Consumer.

The following steps walk you through tasks in detail:

### a.	Update the Kafka Producer implementation code
1.	Add a String variable to your data object (message) that will be published to the Kafka Topic to store the trace ID and segment ID.

```java
//add this variable to have traceId and segmentId in the message object
    private String traceInformation;

//getter
public String getTraceInformation(){
        return this.traceInformation;
    }

//setter
    private void setTraceInformation(String traceInfo){
        this.traceInformation = traceInfo;
    }

```

2.	Create a class implementing the Kafka ProducerInterceptor interface (link: https://kafka.apache.org/0110/javadoc/org/apache/kafka/clients/producer/ProducerInterceptor.html).

3.	Override the onSend(ProducerRecord<K,V> record)  method to store the segment ID and trace ID to the variable in the data object.

```java
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
```
4.	These data can be published to the Kafka Topic from the Producer to be retrieved from the Consumer.

### b.	Update the Kafka Consumer implementation code
Because the same request can’t both publish the data to the Kafka Topic and retrieve the data from it, the Kafka Consumer will be a separate segment in the X-Ray trace.

1.	Create a new X-Ray segment on your Kafka Consumer.
```java
AWSXRayRecorder xrayRecorder = AWSXRay.getGlobalRecorder();
Segment kafkaConsumerSS = xrayRecorder.beginSegment("KafkaConsumerSegment");
```

2.	Follow these steps only if the Kafka Consumer is running on a separate thread on the consumer side.
a. Set the trace entity variable on the Kafka Consumer.
```java
KafkaConsumerChat chat = new KafkaConsumerChat(xrayRecorder.getTraceEntity());
```
b. Set the trace entity to the X-Ray global recorder in the Kafka Consumer
```java
this.recorder.setTraceEntity(this.traceEn);
```

3.	Get the trace ID and segment ID that were set in the Message object and set them to the current segment created on the Kafka Consumer. Notice that the trace ID and segment ID were set in a single String variable with a delimiter. You can instead have two String variables, with one representing the trace ID while the other represents the segment ID.

```java
//now get the trace information to set it to the current trace segment
                    String traceIdValue;
                    String parentValue;

                    //get the traceId and parentId that were in the delimited string
                    try (Scanner traceidandparentscanner = new Scanner(receivedMessage.getTraceInformation()).useDelimiter("::")) {
                        traceIdValue = traceidandparentscanner.next();
                        parentValue = traceidandparentscanner.next();

                        traceidandparentscanner.close();
                    }

                    //get the traceId in the X-Ray format
                    TraceID traceIdFromString = TraceID.fromString(traceIdValue);

                    //set the traceId and parentId to the current Kafka Consumer segment
                    xrayRecorder.getCurrentSegment().setTraceId(traceIdFromString);
                    xrayRecorder.getCurrentSegment().setParentId(parentValue);
```
