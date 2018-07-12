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

public class Message {
    //message text
    private String messageText;
    
    //user name of the receiver
    private String toUserName;
    
    //user name of the sender
    private String fromUserName;
    
    //add this variable to have traceId and parentId in the message object
    private String traceInformation;
    
    private Message(){
        messageText = null;
    }
    
    public Message(String toUsername,String message){
        this.setMessageText(message);
        this.setToUserName(toUsername);
    }
    
    public Message(String toUsername,String message, String traceInfo){
        this.setMessageText(message);
        this.setToUserName(toUsername);
        this.setTraceInformation(traceInfo);
    }
    
    public String getTraceInformation(){
        return this.traceInformation;
    }
    
    private void setTraceInformation(String traceInfo){
        this.traceInformation = traceInfo;
    }

    /**
     * @return the messageText
     */
    public String getMessageText() {
        return messageText;
    }

    /**
     * @param messageText the messageText to set
     */
    public final void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    /**
     * @return the toUserName
     */
    public String getToUserName() {
        return toUserName;
    }

    /**
     * @param toUserName the toUserName to set
     */
    public final void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }
    
    /**
     * @return the toUserName
     */
    public String getFromUserName() {
        return fromUserName;
    }

    /**
     * @param fromUserName the from user name to set
     */
    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }
}