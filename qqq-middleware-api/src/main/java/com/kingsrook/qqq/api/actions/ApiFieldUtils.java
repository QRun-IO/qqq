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

package com.kingsrook.qqq.api.actions;


import com.kingsrook.qqq.api.model.APIVersionRange;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** utility methods for working with fields
 **
 *******************************************************************************/
public class ApiFieldUtils
{


   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean isIncluded(String apiName, QFieldMetaData field)
   {
      ApiFieldMetaData apiFieldMetaData = getApiFieldMetaData(apiName, field);
      if(apiFieldMetaData != null && BooleanUtils.isTrue(apiFieldMetaData.getIsExcluded()))
      {
         return (false);
      }

      return (true);
   }



   /*******************************************************************************
    * determine if a virtual field should be included in the api.
    *
    * <p>Note that the logic here is stricter than for normal fields. a virtual field
    * must explicitly have api field meta data applied to it to be in the api.</p>
    *******************************************************************************/
   public static boolean isVirtualFieldIncluded(String apiName, QVirtualFieldMetaData field)
   {
      ApiFieldMetaData apiFieldMetaData = getApiFieldMetaData(apiName, field);
      if(apiFieldMetaData != null && !BooleanUtils.isTrue(apiFieldMetaData.getIsExcluded()))
      {
         return (true);
      }

      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange getApiVersionRangeForRemovedField(String apiName, QFieldMetaData field)
   {
      ApiFieldMetaData apiFieldMetaData = getApiFieldMetaData(apiName, field);
      if(apiFieldMetaData != null && apiFieldMetaData.getInitialVersion() != null)
      {
         if(StringUtils.hasContent(apiFieldMetaData.getFinalVersion()))
         {
            return (APIVersionRange.betweenAndIncluding(apiFieldMetaData.getInitialVersion(), apiFieldMetaData.getFinalVersion()));
         }
         else
         {
            throw (new IllegalStateException("RemovedApiFieldMetaData for field [" + field.getName() + "] did not specify a finalVersion."));
         }
      }
      else
      {
         throw (new IllegalStateException("RemovedApiFieldMetaData for field [" + field.getName() + "] did not specify an initialVersion."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange getApiVersionRange(String apiName, QFieldMetaData field)
   {
      ApiFieldMetaData apiFieldMetaData = getApiFieldMetaData(apiName, field);
      if(apiFieldMetaData != null && apiFieldMetaData.getInitialVersion() != null)
      {
         return (APIVersionRange.afterAndIncluding(apiFieldMetaData.getInitialVersion()));
      }

      return (APIVersionRange.none());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ApiFieldMetaData getApiFieldMetaData(String apiName, QFieldMetaData field)
   {
      return ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
   }
}
