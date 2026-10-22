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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataClassifications;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.audits.AuditValueProtectionMetaData;
import com.kingsrook.qqq.backend.core.actions.audits.AuditValueProtectorInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Classification-based protection for noninteractive output, independent of user tier.
 *******************************************************************************/
public final class ClassifiedValues
{
   /*******************************************************************************
    ** Static utility.
    *******************************************************************************/
   private ClassifiedValues()
   {
   }



   /*******************************************************************************
    ** The optional application classification and token provider.
    *******************************************************************************/
   public static AuditValueProtectorInterface protector()
   {
      AuditValueProtectionMetaData metadata = AuditValueProtectionMetaData.of(QContext.getQInstance());
      return metadata == null ? null : metadata.getProtectorInstance();
   }



   /*******************************************************************************
    ** Whether this field must be protected in durable output.
    *******************************************************************************/
   public static boolean isClassified(QTableMetaData table, String fieldName)
   {
      QFieldMetaData field = table == null || table.getFields() == null ? null : table.getFields().get(fieldName);
      AuditValueProtectorInterface protector = protector();
      DataClassification classification = protector == null || field == null ? AiDataClassifications.ofField(table, fieldName) : protector.classificationOf(table, field);
      return classification == DataClassification.PII || classification == DataClassification.PHI;
   }



   /*******************************************************************************
    ** Diagnostics use full masks: even an email domain can contain classified data.
    *******************************************************************************/
   public static String diagnostic(QTableMetaData table, String fieldName, Object value)
   {
      QFieldMetaData field = table == null || table.getFields() == null ? null : table.getFields().get(fieldName);
      return isClassified(table, fieldName) || (field != null && field.getType() != null && field.getType().needsMasked()) ? FieldValueMasker.mask(ValueUtils.getValueAsString(value), null, FieldMaskStyle.FULL) : ValueUtils.getValueAsString(value);
   }



   /*******************************************************************************
    ** A value-only log snapshot excludes labels, backend details and arbitrary errors.
    *******************************************************************************/
   public static String recordForLog(QRecord record)
   {
      QTableMetaData table = QContext.getQInstance() == null || record.getTableName() == null ? null : QContext.getQInstance().getTable(record.getTableName());
      Map<String, Serializable> values = new LinkedHashMap<>();
      record.getValues().forEach((name, value) ->
      {
         QTableMetaData owner = table;
         String fieldName = name;
         if(name.contains(".") && QContext.getQInstance() != null)
         {
            owner = QContext.getQInstance().getTable(name.substring(0, name.indexOf('.')));
            fieldName = name.substring(name.indexOf('.') + 1);
         }
         values.put(name, owner == null ? FieldValueMasker.mask(ValueUtils.getValueAsString(value), null, FieldMaskStyle.FULL) : diagnostic(owner, fieldName, value));
      });
      return JsonUtils.toJson(values);
   }
}
