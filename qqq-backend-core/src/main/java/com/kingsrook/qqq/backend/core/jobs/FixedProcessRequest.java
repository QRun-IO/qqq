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


package com.kingsrook.qqq.backend.core.jobs;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.BoundedIntentData;


/*******************************************************************************
 ** Fixed host process request, with a server-owned operation key and immutable
 ** bounded JSON parameters. It cannot select planners, upload lifecycle effects,
 ** frontend steps, Java code references, or a different transaction.
 *******************************************************************************/
public final class FixedProcessRequest
{
   private final String processName;
   private final String operationKey;
   private final Map<String, Serializable> values;



   /*******************************************************************************
    ** Capture the complete request before beginning transactional persistence.
    *******************************************************************************/
   public FixedProcessRequest(String processName, String operationKey, Map<String, ? extends Serializable> values) throws QException
   {
      if(processName == null || processName.isBlank() || processName.length() > 250
         || operationKey == null || !operationKey.matches("[A-Za-z0-9._:-]{1,128}") || values == null)
      {
         throw (new QException("Fixed process enlistment requires a process, server operation key and JSON parameters"));
      }
      this.processName = processName;
      this.operationKey = operationKey;
      this.values = BoundedIntentData.copyParameters(values);
   }



   /*******************************************************************************
    ** Name of the explicitly opted-in host process.
    *******************************************************************************/
   public String getProcessName()
   {
      return (processName);
   }



   /*******************************************************************************
    ** Server operation identity; not merely the source primary key.
    *******************************************************************************/
   public String getOperationKey()
   {
      return (operationKey);
   }



   /*******************************************************************************
    ** Immutable deep copy of the requested JSON parameters.
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (values);
   }
}
