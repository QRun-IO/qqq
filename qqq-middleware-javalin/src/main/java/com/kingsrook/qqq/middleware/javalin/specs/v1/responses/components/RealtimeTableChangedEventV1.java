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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Typed realtime UI API payload. Qbits publish their own event names through the same bus.
 *******************************************************************************/
public class RealtimeTableChangedEventV1 implements ToSchema
{
   private String table;



   /*******************************************************************************
    ** Explicit schema for clients in any language.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Map<String, Schema> properties = new TreeMap<>();
      properties.put("table", new Schema().withType(Type.STRING));
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }




   /*******************************************************************************
    ** Getter for table
    *******************************************************************************/
   public String getTable()
   {
      return (table);
   }




   /*******************************************************************************
    ** Setter for table
    *******************************************************************************/
   public void setTable(String table)
   {
      this.table = table;
   }




   /*******************************************************************************
    ** Fluent setter for table
    *******************************************************************************/
   public RealtimeTableChangedEventV1 withTable(String table)
   {
      setTable(table);
      return (this);
   }
}

