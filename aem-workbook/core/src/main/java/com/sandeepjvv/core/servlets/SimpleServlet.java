/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.sandeepjvv.core.servlets;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

import org.apache.http.client.HttpClient;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(service=Servlet.class,
           property={
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.resourceTypes="+ "aem-workbook/components/structure/page",
                   "sling.servlet.extensions=" + "txt"
           })
@ServiceDescription("Simple Demo Servlet")
public class SimpleServlet extends SlingSafeMethodsServlet {

     private static final long serialVersionUID = 814031998439878299L;
    private static final Logger LOG = LoggerFactory.getLogger(ValidateSiteMap.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

      String url = request.getParameter("url");
        int start = Integer. parseInt(request.getParameter("start") != null ? request.getParameter("start") : "0");
        int end = Integer. parseInt(request.getParameter("end") != null ? request.getParameter("end") : "0");
        JSONArray arrayData = getSiteMapData(url, start, end);
        response.setContentType("application/json");
        if (arrayData.length() > 0) {
            response.getWriter().print(arrayData);
        } else {
            response.getWriter().print("Please check the SiteMap URL");
        }
    }

    private JSONArray getSiteMapData(String url, int start, int end) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        String apiOutput = StringUtils.EMPTY;
        JSONArray urlList = new JSONArray();
        try {
            HttpGet getRequest = new HttpGet(url);
            HttpResponse getResponse = httpClient.execute(getRequest);
            // Set the API media type in http accept header
            getRequest.addHeader("accept", "application/xml");
            // Send the request; It will immediately return the response in HttpResponse
            getResponse = httpClient.execute(getRequest);
            // verify the valid error code first
            int statusCode = getResponse.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                LOG.info(" The Status Code for the XML Sitemap URL :: {} is :: {}", url, statusCode);
            } else {
                LOG.info(" The Status Code for the XML Sitemap URL :: {} is :: {}", url, statusCode);
                // Now pull back the response object
                HttpEntity httpEntity = getResponse.getEntity();
                // an instance of factory that gives a document builder
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                // an instance of builder to parse the specified xml file
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(httpEntity.getContent());
                doc.getDocumentElement().normalize();
                // Lets see what we got from API
                NodeList nodeList = doc.getElementsByTagName("url");
                // nodeList is not iterable, so we are using for loop
                if(end > nodeList.getLength()){
                    end = nodeList.getLength();
                }
                for (int itr = start; itr < end; itr++) {
                    Node node = nodeList.item(itr);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) node;
                        JSONObject obj = new JSONObject();
                        obj.put("URL", eElement.getElementsByTagName("loc").item(0).getTextContent());
                        obj.put("Response", getHttpUrlConnection(eElement));
                        urlList.put(obj);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Exception at ValidateSiteMap  :: {}", e);
        } finally {
            // Important: Close the connect
            httpClient.getConnectionManager().shutdown();
            LOG.info("HTTP Client Connection Shutdown");
        }
        return urlList;
    }

    private String getHttpUrlConnection(Element eElement) throws IOException {
        String responceCode = StringUtils.EMPTY;
        try {
            URL pageUrl = new URL(eElement.getElementsByTagName("loc").item(0).getTextContent());
            HttpURLConnection connection = (HttpURLConnection) pageUrl.openConnection();
            responceCode = Integer.toString(connection.getResponseCode());
            connection.disconnect();
        } catch (Exception e) {
            LOG.error("Exception at ValidateSiteMap  :: {}", e);
            responceCode = e.toString();
        }
        return responceCode;
    }
}
