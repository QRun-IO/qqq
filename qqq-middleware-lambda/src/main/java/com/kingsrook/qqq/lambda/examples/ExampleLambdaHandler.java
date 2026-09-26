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

package com.kingsrook.qqq.lambda.examples;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.lambda.QBaseCustomLambdaHandler;
import com.kingsrook.qqq.lambda.model.QLambdaRequest;
import com.kingsrook.qqq.lambda.model.QLambdaResponse;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class ExampleLambdaHandler extends QBaseCustomLambdaHandler
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected QLambdaResponse handleJsonRequest(QLambdaRequest request, JSONObject bodyJsonObject) throws QException
   {
      log("In subclass: " + getClass().getSimpleName());
      return (OK);
   }

}
