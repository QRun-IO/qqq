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

package com.kingsrook.qqq.backend.core.model.metadata.automation;


/*******************************************************************************
 ** Enum to define the possible automation provider types
 **
 *******************************************************************************/
public enum QAutomationProviderType
{
   POLLING("polling"),
   SYNCHRONOUS("synchronous"),
   ASYNCHRONOUS("asynchronous"),
   MQ("mq"),
   AMAZON_SQS("sqs");

   private final String name;



   /*******************************************************************************
    ** enum constructor
    *******************************************************************************/
   QAutomationProviderType(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return this.name;
   }

}
