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


import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Typed realtime UI API payload. Qbits publish their own event names through the same bus.
 *******************************************************************************/
public class RealtimeSubscriptionRequestV1 implements ToSchema
{
   private String connectionId;
   private List<String> add;
   private List<String> remove;



   /*******************************************************************************
    ** Explicit schema for clients in any language.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Map<String, Schema> properties = new TreeMap<>();
      properties.put("connectionId", new Schema().withType(Type.STRING));
      properties.put("add", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)));
      properties.put("remove", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)));
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }




   /*******************************************************************************
    ** Getter for connectionId
    *******************************************************************************/
   public String getConnectionId()
   {
      return (connectionId);
   }




   /*******************************************************************************
    ** Setter for connectionId
    *******************************************************************************/
   public void setConnectionId(String connectionId)
   {
      this.connectionId = connectionId;
   }




   /*******************************************************************************
    ** Fluent setter for connectionId
    *******************************************************************************/
   public RealtimeSubscriptionRequestV1 withConnectionId(String connectionId)
   {
      setConnectionId(connectionId);
      return (this);
   }




   /*******************************************************************************
    ** Getter for add
    *******************************************************************************/
   public List<String> getAdd()
   {
      return (add);
   }




   /*******************************************************************************
    ** Setter for add
    *******************************************************************************/
   public void setAdd(List<String> add)
   {
      this.add = add;
   }




   /*******************************************************************************
    ** Fluent setter for add
    *******************************************************************************/
   public RealtimeSubscriptionRequestV1 withAdd(List<String> add)
   {
      setAdd(add);
      return (this);
   }




   /*******************************************************************************
    ** Getter for remove
    *******************************************************************************/
   public List<String> getRemove()
   {
      return (remove);
   }




   /*******************************************************************************
    ** Setter for remove
    *******************************************************************************/
   public void setRemove(List<String> remove)
   {
      this.remove = remove;
   }




   /*******************************************************************************
    ** Fluent setter for remove
    *******************************************************************************/
   public RealtimeSubscriptionRequestV1 withRemove(List<String> remove)
   {
      setRemove(remove);
      return (this);
   }
}

