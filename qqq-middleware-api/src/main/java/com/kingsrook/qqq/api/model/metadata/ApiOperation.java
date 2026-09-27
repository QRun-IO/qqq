/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata;


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;


/*******************************************************************************
 **
 *******************************************************************************/
public enum ApiOperation
{
   QUERY_BY_QUERY_STRING(Capability.TABLE_QUERY),
   GET(Capability.TABLE_GET),
   INSERT(Capability.TABLE_INSERT),
   UPDATE(Capability.TABLE_UPDATE),
   DELETE(Capability.TABLE_DELETE),
   BULK_INSERT(Capability.TABLE_INSERT),
   BULK_UPDATE(Capability.TABLE_UPDATE),
   BULK_DELETE(Capability.TABLE_DELETE);


   private final Capability capability;



   /*******************************************************************************
    **
    *******************************************************************************/
   ApiOperation(Capability capability)
   {
      this.capability = capability;
   }



   /*******************************************************************************
    ** Getter for capability
    **
    *******************************************************************************/
   public Capability getCapability()
   {
      return capability;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean isOperationEnabled(List<EnabledOperationsProvider> enabledOperationsProviders)
   {
      /////////////////////////////
      // by default, assume yes. //
      /////////////////////////////
      boolean result = true;

      for(EnabledOperationsProvider enabledOperationsProvider : enabledOperationsProviders)
      {
         Boolean answerAtThisLevel = enabledOperationsProvider.getAnswer(this);
         if(answerAtThisLevel != null)
         {
            result = answerAtThisLevel;
         }
      }

      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public interface EnabledOperationsProvider
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      Set<ApiOperation> getEnabledOperations();

      /*******************************************************************************
       **
       *******************************************************************************/
      Set<ApiOperation> getDisabledOperations();

      /*******************************************************************************
       **
       *******************************************************************************/
      default Boolean getAnswer(ApiOperation operation)
      {
         Boolean answer = null;
         if(getEnabledOperations() != null && getEnabledOperations().contains(operation))
         {
            answer = true;
         }
         if(getDisabledOperations() != null && getDisabledOperations().contains(operation))
         {
            answer = false;
         }
         return (answer);
      }
   }
}
