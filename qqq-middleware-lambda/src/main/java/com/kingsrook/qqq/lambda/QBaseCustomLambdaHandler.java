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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.lambda.model.QLambdaRequest;
import com.kingsrook.qqq.lambda.model.QLambdaResponse;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 ** QQQ base class for "Custom" lambda handlers.  e.g., completely custom code
 ** to run in your QQQ app, outside of tables or processes (for those standard
 ** use-cases, see QStandardLambdaHandler).
 **
 ** Subclasses here  can just override `handleJsonRequest`, and avoid seeing the
 ** lambda-ness of lambda.
 **
 ** Such subclasses can then have easy standalone unit tests - just testing their
 ** logic, and not the lambda-ness.
 *******************************************************************************/
public class QBaseCustomLambdaHandler extends QAbstractLambdaHandler
{

   /*******************************************************************************
    **
    *******************************************************************************/
   protected QLambdaResponse handleRequest(QLambdaRequest request) throws QException
   {
      String contentType = request.getHeaders().optString("content-type");
      if("application/json".equals(contentType))
      {
         JSONObject bodyJsonObject;
         try
         {
            bodyJsonObject = JsonUtils.toJSONObject(request.getBody());
         }
         catch(JSONException je)
         {
            return (new QLambdaResponse(400, "Unable to parse request body as JSON: " + je.getMessage()));
         }

         return (handleJsonRequest(request, bodyJsonObject));
      }
      else
      {
         return (new QLambdaResponse(400, "Unsupported content-type: " + contentType));
      }
   }



   /*******************************************************************************
    ** Meant to be overridden by subclasses, to provide functionality, if needed.
    *******************************************************************************/
   @Override
   protected QLambdaResponse handleJsonRequest(QLambdaRequest request, JSONObject bodyJsonObject) throws QException
   {
      log(this.getClass().getSimpleName() + " did not override handleJsonRequest - so noop and return 200.");
      return (OK);
   }

}
