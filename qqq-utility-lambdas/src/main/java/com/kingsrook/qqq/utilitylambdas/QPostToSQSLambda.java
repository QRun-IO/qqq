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

package com.kingsrook.qqq.utilitylambdas;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;


/*******************************************************************************
 ** QQQ Utility Lambda to post input data to SQS.
 **
 ** Requires environment variable:  QUEUE_URL (e.g., https://sqs.us-east-0.amazonaws.com/111122223333/my-queue-name)
 *******************************************************************************/
public class QPostToSQSLambda implements RequestStreamHandler
{
   protected Context context;



   /*******************************************************************************
    ** Entrypoint from AWS Lambda.
    *******************************************************************************/
   public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
   {
      this.context = context;

      try
      {
         String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
         log("Full Input: " + input);

         final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

         SendMessageRequest sendMessageRequest = new SendMessageRequest()
            .withQueueUrl(System.getenv("QUEUE_URL"))
            .withMessageBody(input);
         sqs.sendMessage(sendMessageRequest);

         writeResponse(outputStream, "OK");
      }
      catch(Exception e)
      {
         log(e);
         throw (new IOException(e));
         // writeResponse(outputStream, requestId, 500, "Uncaught error handing request: " + e.getMessage());
      }
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

      context.getLogger().log(message + (message.endsWith("\n") ? "" : "\n"));
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

      StringWriter sw = new StringWriter();
      PrintWriter  pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      log(sw + "\n");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void writeResponse(OutputStream outputStream, String messageBody) throws IOException
   {
      StringBuilder body = new StringBuilder("{");
      if(messageBody != null && !messageBody.equals(""))
      {
         body.append("\"body\":\"").append(messageBody.replaceAll("\"", "'")).append("\"");
      }
      body.append("}");

      outputStream.write(body.toString().getBytes(StandardCharsets.UTF_8));
   }

}
