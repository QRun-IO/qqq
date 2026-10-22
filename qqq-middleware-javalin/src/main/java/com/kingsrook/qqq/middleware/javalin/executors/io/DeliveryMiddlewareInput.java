/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.io.Serializable;
import java.util.Map;


/*******************************************************************************
 ** Typed delivery operation input.
 *******************************************************************************/
public class DeliveryMiddlewareInput extends AbstractMiddlewareInput
{
   /*******************************************************************************
    ** Requested operation.
    *******************************************************************************/
   public enum Operation
   {
      LIST, TARGETS, OUTPUT
   }

   private Operation operation;
   private String name;
   private String logId;
   private Map<String, Serializable> chosen = Map.of();



   /*******************************************************************************
    ** Get operation.
    *******************************************************************************/
   public Operation getOperation()
   {
      return (operation);
   }



   /*******************************************************************************
    ** With operation.
    *******************************************************************************/
   public DeliveryMiddlewareInput withOperation(Operation operation)
   {
      this.operation = operation;
      return (this);
   }



   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** With name.
    *******************************************************************************/
   public DeliveryMiddlewareInput withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Get logId.
    *******************************************************************************/
   public String getLogId()
   {
      return (logId);
   }



   /*******************************************************************************
    ** With logId.
    *******************************************************************************/
   public DeliveryMiddlewareInput withLogId(String logId)
   {
      this.logId = logId;
      return (this);
   }



   /*******************************************************************************
    ** Get chosen.
    *******************************************************************************/
   public Map<String, Serializable> getChosen()
   {
      return (chosen);
   }



   /*******************************************************************************
    ** With chosen.
    *******************************************************************************/
   public DeliveryMiddlewareInput withChosen(Map<String, Serializable> chosen)
   {
      this.chosen = chosen;
      return (this);
   }
}
