/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.lambda;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.Context;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.lambda.model.QLambdaRequest;
import com.kingsrook.qqq.lambda.model.QLambdaResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.lambda.AnotherHandler
 *******************************************************************************/
class QBaseCustomLambdaHandlerTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccess() throws IOException
   {
      String     inputString  = getSuccessInputString();
      String     outputString = runHandleRequest(inputString);
      JSONObject outputJson   = JsonUtils.toJSONObject(outputString);
      assertTrue(outputJson.has("requestId"));
      assertFalse(outputJson.has("errorMessage"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonJsonInput() throws IOException
   {
      String     inputString  = getNonJsonInputString();
      String     outputString = runHandleRequest(inputString);
      JSONObject outputJson   = JsonUtils.toJSONObject(outputString);
      assertTrue(outputJson.has("errorMessage"));
      assertThat(outputJson.getString("errorMessage")).contains("Unable to parse input as JSON");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonJsonBody() throws IOException
   {
      String     inputString  = getNonJsonBodyString();
      String     outputString = runHandleRequest(inputString);
      JSONObject outputJson   = JsonUtils.toJSONObject(outputString);
      assertTrue(outputJson.has("errorMessage"));
      assertThat(outputJson.getString("errorMessage")).contains("Unable to parse request body as JSON");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonJsonContentType() throws IOException
   {
      String     inputString  = getNonJsonContentTypeString();
      String     outputString = runHandleRequest(inputString);
      JSONObject outputJson   = JsonUtils.toJSONObject(outputString);
      assertTrue(outputJson.has("errorMessage"));
      assertThat(outputJson.getString("errorMessage")).contains("Unsupported content-type:");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoBodyInputStringNonJsonContentType() throws IOException
   {
      String     inputString  = getNoBodyInputString();
      String     outputString = runHandleRequest(inputString);
      JSONObject outputJson   = JsonUtils.toJSONObject(outputString);
      assertTrue(outputJson.has("errorMessage"));
      assertThat(outputJson.getString("errorMessage")).contains("Unsupported content-type:");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLogException() throws IOException
   {
      InputStream           inputStream  = new ByteArrayInputStream(getSuccessInputString().getBytes());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Context               context      = new MockContext();

      new QBaseCustomLambdaHandler()
      {
         @Override
         protected QLambdaResponse handleJsonRequest(QLambdaRequest request, JSONObject bodyJsonObject) throws QException
         {
            log(new QException("Test Exception"));
            return (OK);
         }
      }.handleRequest(inputStream, outputStream, context);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String runHandleRequest(String inputString) throws IOException
   {
      InputStream           inputStream  = new ByteArrayInputStream(inputString.getBytes());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Context               context      = new MockContext();
      new QBaseCustomLambdaHandler().handleRequest(inputStream, outputStream, context);
      String outputString = outputStream.toString(StandardCharsets.UTF_8);
      System.out.println(outputString);
      return outputString;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testResponseJson()
   {
      String json = JsonUtils.toPrettyJson(new QLambdaResponse(400, "Testing an error"));
      System.out.println(json);

      json = JsonUtils.toPrettyJson(new QLambdaResponse(200, false, Map.of("Content-Type", "application/json", "X-my-header", "1234"),
         new QLambdaResponse.Body(UUID.randomUUID().toString(), null)));
      System.out.println(json);

      QLambdaResponse qLambdaResponse = new QLambdaResponse(201);
      qLambdaResponse.getBody().setRequestId(UUID.randomUUID().toString());
      json = JsonUtils.toPrettyJson(qLambdaResponse);
      System.out.println(json);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHandleRequest() throws QException
   {
      QLambdaResponse response = new QBaseCustomLambdaHandler().handleJsonRequest(new QLambdaRequest(new JSONObject(), "/", "", ""), new JSONObject());
      assertEquals(200, response.getStatusCode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getTemplateInputString()
   {
      return ("""
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getNonJsonContentTypeString()
   {
      return ("""
         {
           "version": "2.0",
           "routeKey": "$default",
           "rawPath": "/",
           "rawQueryString": "",
           "headers": {
             "content-length": "21",
             "x-amzn-tls-cipher-suite": "ECDHE-RSA-AES128-GCM-SHA256",
             "x-amzn-tls-version": "TLSv1.2",
             "x-amzn-trace-id": "Root=1-63483527-78c1f53c4710848602d961f0",
             "x-forwarded-proto": "https",
             "host": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm.lambda-url.us-east-1.on.aws",
             "x-forwarded-port": "443",
             "content-type": "text/xml",
             "x-forwarded-for": "24.217.225.229",
             "accept": "*/*",
             "user-agent": "curl/7.79.1"
           },
           "requestContext": {
             "accountId": "anonymous",
             "apiId": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm",
             "domainName": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm.lambda-url.us-east-1.on.aws",
             "domainPrefix": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm",
             "http": {
               "method": "POST",
               "path": "/",
               "protocol": "HTTP/1.1",
               "sourceIp": "24.217.225.229",
               "userAgent": "curl/7.79.1"
             },
             "requestId": "4c11e825-79a3-4583-ba6e-438fc5d9ec91",
             "routeKey": "$default",
             "stage": "$default",
             "time": "13/Oct/2022:15:56:23 +0000",
             "timeEpoch": 1665676583934
           },
           "body": "<?xml ...>",
           "isBase64Encoded": false
         }
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getNonJsonBodyString()
   {
      return ("""
         {
           "version": "2.0",
           "routeKey": "$default",
           "rawPath": "/",
           "rawQueryString": "",
           "headers": {
             "content-length": "21",
             "x-amzn-tls-cipher-suite": "ECDHE-RSA-AES128-GCM-SHA256",
             "x-amzn-tls-version": "TLSv1.2",
             "x-amzn-trace-id": "Root=1-63483527-78c1f53c4710848602d961f0",
             "x-forwarded-proto": "https",
             "host": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm.lambda-url.us-east-1.on.aws",
             "x-forwarded-port": "443",
             "content-type": "application/json",
             "x-forwarded-for": "24.217.225.229",
             "accept": "*/*",
             "user-agent": "curl/7.79.1"
           },
           "requestContext": {
             "accountId": "anonymous",
             "apiId": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm",
             "domainName": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm.lambda-url.us-east-1.on.aws",
             "domainPrefix": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm",
             "http": {
               "method": "POST",
               "path": "/",
               "protocol": "HTTP/1.1",
               "sourceIp": "24.217.225.229",
               "userAgent": "curl/7.79.1"
             },
             "requestId": "4c11e825-79a3-4583-ba6e-438fc5d9ec91",
             "routeKey": "$default",
             "stage": "$default",
             "time": "13/Oct/2022:15:56:23 +0000",
             "timeEpoch": 1665676583934
           },
           "body": "not json.",
           "isBase64Encoded": false
         }
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getNonJsonInputString()
   {
      return ("""
         not json.
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getSuccessInputString()
   {
      return ("""
         {
           "version": "2.0",
           "routeKey": "$default",
           "rawPath": "/",
           "rawQueryString": "",
           "headers": {
             "content-length": "21",
             "x-amzn-tls-cipher-suite": "ECDHE-RSA-AES128-GCM-SHA256",
             "x-amzn-tls-version": "TLSv1.2",
             "x-amzn-trace-id": "Root=1-63483527-78c1f53c4710848602d961f0",
             "x-forwarded-proto": "https",
             "host": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm.lambda-url.us-east-1.on.aws",
             "x-forwarded-port": "443",
             "content-type": "application/json",
             "x-forwarded-for": "24.217.225.229",
             "accept": "*/*",
             "user-agent": "curl/7.79.1"
           },
           "requestContext": {
             "accountId": "anonymous",
             "apiId": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm",
             "domainName": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm.lambda-url.us-east-1.on.aws",
             "domainPrefix": "ge74ifwcbvykmlp7rmqd2a7a4e0hhkmm",
             "http": {
               "method": "POST",
               "path": "/",
               "protocol": "HTTP/1.1",
               "sourceIp": "24.217.225.229",
               "userAgent": "curl/7.79.1"
             },
             "requestId": "4c11e825-79a3-4583-ba6e-438fc5d9ec91",
             "routeKey": "$default",
             "stage": "$default",
             "time": "13/Oct/2022:15:56:23 +0000",
             "timeEpoch": 1665676583934
           },
           "body": "{\\n  \\"test\\": \\"event\\"\\n}",
           "isBase64Encoded": false
         }
         """);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String getNoBodyInputString()
   {
      return ("""
         {
             "version": "2.0",
             "routeKey": "$default",
             "rawPath": "/",
             "rawQueryString": "",
             "headers": {
                 "x-amzn-tls-cipher-suite": "ECDHE-RSA-AES128-GCM-SHA256",
                 "x-amzn-tls-version": "TLSv1.2",
                 "x-amzn-trace-id": "Root=1-6349b9a1-4d3785872159571e302a3015",
                 "x-forwarded-proto": "https",
                 "host": "p6kox4fzsfajxvsmyc222cu5ta0dlpwp.lambda-url.us-east-1.on.aws",
                 "x-forwarded-port": "443",
                 "x-forwarded-for": "24.217.225.229",
                 "accept": "*/*",
                 "user-agent": "curl/7.79.1"
             },
             "requestContext": {
                 "accountId": "anonymous",
                 "apiId": "p6kox4fzsfajxvsmyc222cu5ta0dlpwp",
                 "domainName": "p6kox4fzsfajxvsmyc222cu5ta0dlpwp.lambda-url.us-east-1.on.aws",
                 "domainPrefix": "p6kox4fzsfajxvsmyc222cu5ta0dlpwp",
                 "http": {
                     "method": "GET",
                     "path": "/",
                     "protocol": "HTTP/1.1",
                     "sourceIp": "24.217.225.229",
                     "userAgent": "curl/7.79.1"
                 },
                 "requestId": "82b80656-5f05-46eb-bc2a-7b8328d095d4",
                 "routeKey": "$default",
                 "stage": "$default",
                 "time": "14/Oct/2022:19:33:53 +0000",
                 "timeEpoch": 1665776033574
             },
             "isBase64Encoded": false
         }
         """);
   }
}