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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataClassifications;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Classification of the fields actually rendered by a producer.
 *******************************************************************************/
public record DeliveryClassification(DataClassification classification, List<String> restrictedFieldLabels)
{
   /*******************************************************************************
    ** Resolve metadata sensitivity and current field-access restrictions together.
    *******************************************************************************/
   public static DeliveryClassification of(QTableMetaData table, Collection<String> fieldNames)
   {
      DataClassification highest = null;
      List<String> labels = new ArrayList<>();
      for(String name : fieldNames)
      {
         QFieldMetaData field = table.getField(name);
         if(field == null)
         {
            throw new IllegalArgumentException("Unknown delivery field: " + name);
         }
         DataClassification classification = AiDataClassifications.ofField(table, name);
         highest = DataClassification.max(highest, classification);
         if((classification != null && classification.isAtLeast(DataClassification.CONFIDENTIAL)) || FieldAccessHelper.getTier(table, name) != FieldAccessTier.FULL)
         {
            labels.add(field.getLabel() == null ? name : field.getLabel());
         }
      }
      return (new DeliveryClassification(highest, List.copyOf(labels)));
   }
}
