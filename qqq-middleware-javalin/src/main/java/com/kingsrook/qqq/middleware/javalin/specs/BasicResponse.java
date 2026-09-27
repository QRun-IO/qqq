/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.Map;
import com.kingsrook.qqq.openapi.model.Example;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;


/***************************************************************************
 ** Basic version of a response from a spec/endpoint.
 ***************************************************************************/
public record BasicResponse(String contentType, HttpStatus status, String description, String schemaRefName, Map<String, Example> examples)
{

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BasicResponse(String description, String schemaRefName)
   {
      this(ContentType.APPLICATION_JSON.getMimeType(), HttpStatus.OK, description, schemaRefName, null);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BasicResponse(String description, String schemaRefName, Map<String, Example> examples)
   {
      this(ContentType.APPLICATION_JSON.getMimeType(), HttpStatus.OK, description, schemaRefName, examples);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BasicResponse(HttpStatus status, String description, String schemaRefName)
   {
      this(ContentType.APPLICATION_JSON.getMimeType(), status, description, schemaRefName, null);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BasicResponse(HttpStatus status, String description, String schemaRefName, Map<String, Example> examples)
   {
      this(ContentType.APPLICATION_JSON.getMimeType(), status, description, schemaRefName, examples);
   }

}
