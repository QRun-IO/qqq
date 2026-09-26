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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIHasAdditionalProperties;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionResponseV1 implements ManageSessionOutputInterface, ToSchema
{
   @OpenAPIDescription("Unique identifier of the session.  Required to be returned on subsequent requests in the sessionUUID Cookie, to prove authentication.")
   private String uuid;

   @OpenAPIDescription("Optional object with application-defined values.")
   @OpenAPIHasAdditionalProperties()
   private Map<String, Serializable> values;



   /*******************************************************************************
    ** Getter for uuid
    *******************************************************************************/
   public String getUuid()
   {
      return (this.uuid);
   }



   /*******************************************************************************
    ** Setter for uuid
    *******************************************************************************/
   public void setUuid(String uuid)
   {
      this.uuid = uuid;
   }



   /*******************************************************************************
    ** Fluent setter for uuid
    *******************************************************************************/
   public ManageSessionResponseV1 withUuid(String uuid)
   {
      this.uuid = uuid;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public ManageSessionResponseV1 withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }

}
