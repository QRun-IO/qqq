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


import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Table settings for deduplicating file rows loaded by a durable job.
 *******************************************************************************/
public class DurableTableMetaData extends QSupplementalTableMetaData
{
   public static final String TYPE = "durable";

   private String jobRowKeyField;



   /*******************************************************************************
    ** The durable settings on a table, or null.
    *******************************************************************************/
   public static DurableTableMetaData of(QTableMetaData table)
   {
      return (table == null ? null : QSupplementalTableMetaData.of(table, TYPE));
   }



   /*******************************************************************************
    ** Getter for type.
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (TYPE);
   }



   /*******************************************************************************
    ** A declared row key must be a string protected by its own unique key.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QTableMetaData table, QInstanceValidator validator)
   {
      if(jobRowKeyField == null)
      {
         return;
      }

      QFieldMetaData field  = table.getFields().get(jobRowKeyField);
      String         prefix = "Table " + table.getName() + " durable jobRowKeyField " + jobRowKeyField;
      validator.assertCondition(field != null, prefix + " does not exist on the table.");
      validator.assertCondition(field == null || field.getType() == QFieldType.STRING, prefix + " must be STRING.");
      validator.assertCondition(CollectionUtils.nonNullList(table.getUniqueKeys()).stream().anyMatch(key -> List.of(jobRowKeyField).equals(key.getFieldNames())),
         prefix + " requires a unique key consisting of exactly that field.");
   }



   /*******************************************************************************
    ** Stable across chunk attempts, using the record's first original file row.
    *******************************************************************************/
   public static String rowKey(Integer jobId, Integer fileRowNumber)
   {
      return (jobId + ":" + fileRowNumber);
   }



   /*******************************************************************************
    ** Getter for jobRowKeyField.
    *******************************************************************************/
   public String getJobRowKeyField()
   {
      return (this.jobRowKeyField);
   }



   /*******************************************************************************
    ** Fluent setter for jobRowKeyField.
    *******************************************************************************/
   public DurableTableMetaData withJobRowKeyField(String jobRowKeyField)
   {
      this.jobRowKeyField = jobRowKeyField;
      return (this);
   }



   /*******************************************************************************
    ** The only setting is an immutable string, already copied by clone.
    *******************************************************************************/
   @Override
   protected QSupplementalTableMetaData finishClone(QSupplementalTableMetaData abstractClone)
   {
      return (abstractClone);
   }
}
