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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.lambda.model.QLambdaRequest;
import com.kingsrook.qqq.lambda.model.QLambdaResponse;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 ** Abstract base class for any and all QQQ lambda handlers.
 **
 ** This class provides the method `handleRequest(InputStream, OutputStream, Context)`,
 ** which is what gets invoked by AWS Lambda.  In there, we parse the data from
 ** the inputStream to build a QLambdaRequest - which is then passed to:
 **
 ** `handleRequest(QLambdaRequest)` - which would be meant for implementing in a
 ** subclass.
 **
 *******************************************************************************/
public abstract class QAbstractLambdaHandler implements RequestStreamHandler
{
   private static QLogger LOG; // = QLogger.getLogger(QBasicLambdaHandler.class);

   protected Context context;

   @SuppressWarnings("checkstyle:MemberName")
   public static final QLambdaResponse GENERIC_SERVER_ERROR = new QLambdaResponse(500, "Internal Server Error");
   public static final QLambdaResponse OK                   = new QLambdaResponse(200);



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QAbstractLambdaHandler()
   {
      ///////////////////////////////////////////////////////////////////////
      // tell log4j to use a config that won't fail when running in lambda //
      ///////////////////////////////////////////////////////////////////////
      Configurator.initialize(null, "qqq-lambda-log4j2.xml");
   }



   /*******************************************************************************
    ** Entrypoint from AWS Lambda.
    *******************************************************************************/
   public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
   {
      this.context = context;

      String requestId = "unknown";
      try
      {
         String input = IOUtils.toString(inputStream, "UTF-8");
         log("Full Input: " + input);

         /////////////////////////////////////////////////////////////////////////////////////////
         // parse the Lambda input as json - then pull parts out of it that we know to look for //
         /////////////////////////////////////////////////////////////////////////////////////////
         JSONObject inputJsonObject;
         try
         {
            inputJsonObject = JsonUtils.toJSONObject(input);
         }
         catch(JSONException je)
         {
            writeResponse(outputStream, requestId, new QLambdaResponse(400, "Unable to parse input as JSON: " + je.getMessage()));
            return;
         }

         QLambdaRequest request = new QLambdaRequest(inputJsonObject);
         requestId = request.getRequestContext().optString("requestId");

         /////////////////////////////////////////////////////////////////////////////////////
         // pass the request downstream, to get back a response we can write back to Lambda //
         /////////////////////////////////////////////////////////////////////////////////////
         QLambdaResponse response = handleRequest(request);
         writeResponse(outputStream, requestId, response);
      }
      catch(QUserFacingException ufe)
      {
         writeResponse(outputStream, requestId, new QLambdaResponse(400, "Error handing request: " + ufe.getMessage()));
      }
      catch(Exception e)
      {
         writeResponse(outputStream, requestId, new QLambdaResponse(500, "Uncaught error handing request: " + e.getMessage()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected abstract QLambdaResponse handleRequest(QLambdaRequest request) throws QException;



   /*******************************************************************************
    ** Meant to be overridden by subclasses, to provide functionality, if needed.
    *******************************************************************************/
   protected QLambdaResponse handleJsonRequest(QLambdaRequest request, JSONObject bodyJsonObject) throws QException
   {
      log(this.getClass().getSimpleName() + " did not override handleJsonRequest - so noop and return 200.");
      return (OK);
   }



   /*******************************************************************************
    ** Write to the cloudwatch logs.
    *******************************************************************************/
   protected void log(String message)
   {
      if(message == null)
      {
         return;
      }

      if(this.context != null && this.context.getLogger() != null)
      {
         context.getLogger().log(message + (message.endsWith("\n") ? "" : "\n"));
      }
      else
      {
         initLOG();
         LOG.info(message);
      }
   }



   /*******************************************************************************
    ** Write to the cloudwatch logs.
    *******************************************************************************/
   protected void log(String message, Throwable t)
   {
      log(message);
      log(t);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void log(Throwable t)
   {
      if(t == null)
      {
         return;
      }

      if(this.context != null && this.context.getLogger() != null)
      {
         StringWriter sw = new StringWriter();
         PrintWriter  pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         log(sw + "\n");
      }
      else
      {
         initLOG();
         LOG.warn("Exception", t);
      }
   }



   /*******************************************************************************
    ** So, when running inside lambda, we cannot
    *******************************************************************************/
   private void initLOG()
   {
      if(LOG == null)
      {
         LOG = QLogger.getLogger(QAbstractLambdaHandler.class);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void writeResponse(OutputStream outputStream, String requestId, QLambdaResponse response) throws IOException
   {
      QLambdaResponse.Body body = response.getBody();
      body.setRequestId(requestId);

      outputStream.write(JsonUtils.toJson(body).getBytes(StandardCharsets.UTF_8));
   }

}
