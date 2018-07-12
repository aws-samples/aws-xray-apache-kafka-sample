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
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceID;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class ToKafka extends HttpServlet {

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
            out.println("<title>Message to Kafka</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Successfully sent message to Kafka " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
            out.close();
        }catch(Exception e){
            System.out.printf("Exception while response.getWriter(): "+e.getMessage());
        }
        
    }
    
    private void processError(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// Analyze the servlet exception
		Throwable throwable = (Throwable) request
				.getAttribute("javax.servlet.error.exception");
		Integer statusCode = (Integer) request
				.getAttribute("javax.servlet.error.status_code");
		String servletName = (String) request
				.getAttribute("javax.servlet.error.servlet_name");
		if (servletName == null) {
			servletName = "Unknown";
		}
		String requestUri = (String) request
				.getAttribute("javax.servlet.error.request_uri");
		if (requestUri == null) {
			requestUri = "Unknown";
		}
		
		// Set response content type
	      response.setContentType("text/html");
	 
	      PrintWriter out = response.getWriter();
	      out.write("<html><head><title>Exception/Error Details</title></head><body>");
	      if(statusCode != 500){
	    	  out.write("<h3>Error Details</h3>");
	    	  out.write("<strong>Status Code</strong>:"+statusCode+"<br>");
	    	  out.write("<strong>Requested URI</strong>:"+requestUri);
	      }else{
	    	  out.write("<h3>Exception Details</h3>");
	    	  out.write("<ul><li>Servlet Name:"+servletName+"</li>");
	    	  out.write("<li>Exception Name:"+throwable.getClass().getName()+"</li>");
	    	  out.write("<li>Requested URI:"+requestUri+"</li>");
	    	  out.write("<li>Exception Message:"+throwable.getMessage()+"</li>");
	    	  out.write("</ul>");
	      }
	      
	      out.write("<br><br>");
	      out.write("<a href=\"sendmessage.jsp\">Send Message</a>");
	      out.write("</body></html>");
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
        
        AWSXRayRecorder xrayRecorder = AWSXRayRecorderBuilder.defaultRecorder();
        Subsegment kafkaProducerSS = xrayRecorder.beginSubsegment("KafkaProducerSubSegment");
        try{
            kafkaProducerSS.putAnnotation("tousername",request.getParameter("tousername"));
            kafkaProducerSS.putAnnotation("message",request.getParameter("message"));
        }catch(Exception ee){
            System.out.printf("Exception while kafkaProducerSS.putAnnotation(): "+ee.getMessage());
        }
        
        /*String parentId = xrayRecorder.getCurrentSegment().getId();
        TraceID traceId = xrayRecorder.getCurrentSegment().getTraceId();
        
        String traceInformation = traceId.toString()+"::"+parentId;*/
        
        try{
            //creating message with only username and message
            Message message = new Message(request.getParameter("tousername"),request.getParameter("message"));
            KafkaProducerChat.main(message);            
        }catch(RuntimeException e){
            kafkaProducerSS.addException(e);
            kafkaProducerSS.setError(true);
        }finally{
            kafkaProducerSS.end();
        }
        
        Subsegment CWSubSegment = xrayRecorder.beginSubsegment("CWSubSegment");
        
        final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();

        Dimension dimension = new Dimension()
            .withName("UNIQUE_PAGES")
            .withValue("URLS");

        MetricDatum datum = new MetricDatum()
            .withMetricName("PAGES_VISITED")
            .withUnit(StandardUnit.None)
            .withValue(123.0)
            .withDimensions(dimension);
        
        PutMetricDataRequest request1 = new PutMetricDataRequest()
        .withNamespace("SITE/TRAFFIC")
        .withMetricData(datum);
        
        PutMetricDataResult response1 = cw.putMetricData(request1);
        CWSubSegment.end();
        
        processRequest(request,response);
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
        return "Servlet for sending chat messages to Kafka";
    }// </editor-fold>

}