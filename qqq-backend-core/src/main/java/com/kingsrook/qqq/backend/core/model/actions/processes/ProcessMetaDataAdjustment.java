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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Object that stores adjustments that a process wants to make, at run-time,
 ** to its meta-data.
 **
 ** e.g., changing the steps; updating fields (e.g., changing an inline PVS,
 ** or an isRequired attribute)
 *******************************************************************************/
public class ProcessMetaDataAdjustment
{
   private static final QLogger LOG = QLogger.getLogger(ProcessMetaDataAdjustment.class);

   private List<QFrontendStepMetaData> updatedFrontendStepList = null;
   private Map<String, QFieldMetaData> updatedFields           = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessMetaDataAdjustment withUpdatedField(QFieldMetaData field)
   {
      if(updatedFields == null)
      {
         updatedFields = new LinkedHashMap<>();
      }

      if(!StringUtils.hasContent(field.getName()))
      {
         LOG.warn("Missing name on field in withUpdatedField - no update will happen.");
      }
      else
      {
         if(updatedFields.containsKey(field.getName()))
         {
            LOG.info("UpdatedFields map already contained a field with this name - overwriting it.", logPair("fieldName", field.getName()));
         }

         updatedFields.put(field.getName(), field);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for updatedFrontendStepList
    *******************************************************************************/
   public List<QFrontendStepMetaData> getUpdatedFrontendStepList()
   {
      return (this.updatedFrontendStepList);
   }



   /*******************************************************************************
    ** Setter for updatedFrontendStepList
    *******************************************************************************/
   public void setUpdatedFrontendStepList(List<QFrontendStepMetaData> updatedFrontendStepList)
   {
      this.updatedFrontendStepList = updatedFrontendStepList;
   }



   /*******************************************************************************
    ** Fluent setter for updatedFrontendStepList
    *******************************************************************************/
   public ProcessMetaDataAdjustment withUpdatedFrontendStepList(List<QFrontendStepMetaData> updatedFrontendStepList)
   {
      this.updatedFrontendStepList = updatedFrontendStepList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for updatedFields
    *******************************************************************************/
   public Map<String, QFieldMetaData> getUpdatedFields()
   {
      return (this.updatedFields);
   }



   /*******************************************************************************
    ** Setter for updatedFields
    *******************************************************************************/
   public void setUpdatedFields(Map<String, QFieldMetaData> updatedFields)
   {
      this.updatedFields = updatedFields;
   }



   /*******************************************************************************
    ** Fluent setter for updatedFields
    *******************************************************************************/
   public ProcessMetaDataAdjustment withUpdatedFields(Map<String, QFieldMetaData> updatedFields)
   {
      this.updatedFields = updatedFields;
      return (this);
   }

}
