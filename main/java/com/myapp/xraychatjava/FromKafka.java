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
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FromKafka extends HttpServlet {
    
    public static String parentIdOfConsumer = "";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet FromKafka</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet FromKafka at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AWSXRayRecorder xrayRecorder = AWSXRay.getGlobalRecorder();
        
        //begin segment for Kafka Consumer
        Segment kafkaConsumerSS = xrayRecorder.beginSegment("KafkaConsumerSegment");
        kafkaConsumerSS.putAnnotation("parentID",xrayRecorder.getTraceEntity().getId());
        
        //set the trace entity for Kafka Consumer on a different thread
        KafkaConsumerChat chat = new KafkaConsumerChat(xrayRecorder.getTraceEntity());
        
        Thread chatThread = new Thread(chat);
        chatThread.start();
        
        int i = 0;
        while(KafkaConsumerChat.chatRecords.isEmpty()){
            try {
                //Thread sleep for Kafka Consumer to retrieve messages from the Kafka queue
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                System.out.printf("Exception while chatThread.sleep(): "+e.getMessage());
                kafkaConsumerSS.addException(e);
            }
        }
        
        HashMap<String,String> chatRecords = KafkaConsumerChat.chatRecords;
        
        //process the chat records and add the X-Ray traceId and parentId to this segment
        processRequestWithChatRecords(request,response,chatRecords,xrayRecorder);
        chat.shutdown();
        
        //close segment
        xrayRecorder.endSegment();
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private void processRequestWithChatRecords(HttpServletRequest request, HttpServletResponse response, HashMap<String, String> chatRecords,AWSXRayRecorder xrayRecorder) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet FromKafka</title>");            
            out.println("</head>");
            out.println("<body>");
            if(chatRecords.isEmpty()){
                out.println("<h1>Empty chat records</h1>");
            }else{
                for (Map.Entry entry : chatRecords.entrySet())
                {
                    Message receivedMessage = (Message)entry.getValue();
                    
                    //displays the key, message and trace information
                    out.println("<h3>"+entry.getKey()+":"+receivedMessage.getMessageText()+":"+receivedMessage.getTraceInformation()+"</h3>");
                    
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
                }
            }

            out.println("</body>");
            out.println("</html>");
        }
    }

}