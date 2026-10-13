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

package com.kingsrook.qqq.backend.core.model.metadata.collaboration;


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordReferenceTableMetaData extends QSupplementalTableMetaData
{
   private String tableNameField;
   private String recordIdField;
   private RecordFeature feature;
   private boolean mirroredLocks;
   private String authorField;
   public static final String NAME = "recordReference";



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getType()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordReferenceTableMetaData of(QTableMetaData table)
   {
      return (table == null ? null : (RecordReferenceTableMetaData) table.getSupplementalMetaData(NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordReferenceTableMetaData ofOrWithNew(QTableMetaData table)
   {
      return (ofOrWithNew(table, NAME, RecordReferenceTableMetaData::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance instance, QTableMetaData table, QInstanceValidator validator)
   {
      validator.assertCondition(tableNameField != null && table.getFields().containsKey(tableNameField), table.getName() + ": recordReference tableNameField must exist");
      validator.assertCondition(recordIdField != null && table.getFields().containsKey(recordIdField), table.getName() + ": recordReference recordIdField must exist");
      validator.assertCondition(authorField == null || table.getFields().containsKey(authorField), table.getName() + ": recordReference authorField must exist");
      validator.assertCondition(feature != null, table.getName() + ": recordReference feature is required");
   }



   /*******************************************************************************
    ** All fields are immutable values.
    *******************************************************************************/
   protected QSupplementalTableMetaData finishClone(QSupplementalTableMetaData clone)
   {
      return (clone);
   }



   /*******************************************************************************
    ** Getter for tableNameField
    *******************************************************************************/
   public String getTableNameField()
   {
      return (this.tableNameField);
   }



   /*******************************************************************************
    ** Setter for tableNameField
    *******************************************************************************/
   public void setTableNameField(String tableNameField)
   {
      this.tableNameField = tableNameField;
   }



   /*******************************************************************************
    ** Fluent setter for tableNameField
    *******************************************************************************/
   public RecordReferenceTableMetaData withTableNameField(String tableNameField)
   {
      setTableNameField(tableNameField);
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordIdField
    *******************************************************************************/
   public String getRecordIdField()
   {
      return (this.recordIdField);
   }



   /*******************************************************************************
    ** Setter for recordIdField
    *******************************************************************************/
   public void setRecordIdField(String recordIdField)
   {
      this.recordIdField = recordIdField;
   }



   /*******************************************************************************
    ** Fluent setter for recordIdField
    *******************************************************************************/
   public RecordReferenceTableMetaData withRecordIdField(String recordIdField)
   {
      setRecordIdField(recordIdField);
      return (this);
   }



   /*******************************************************************************
    ** Getter for feature
    *******************************************************************************/
   public RecordFeature getFeature()
   {
      return (this.feature);
   }



   /*******************************************************************************
    ** Setter for feature
    *******************************************************************************/
   public void setFeature(RecordFeature feature)
   {
      this.feature = feature;
   }



   /*******************************************************************************
    ** Fluent setter for feature
    *******************************************************************************/
   public RecordReferenceTableMetaData withFeature(RecordFeature feature)
   {
      setFeature(feature);
      return (this);
   }



   /*******************************************************************************
    ** Getter for mirroredLocks
    *******************************************************************************/
   public boolean getMirroredLocks()
   {
      return (this.mirroredLocks);
   }



   /*******************************************************************************
    ** Setter for mirroredLocks
    *******************************************************************************/
   public void setMirroredLocks(boolean mirroredLocks)
   {
      this.mirroredLocks = mirroredLocks;
   }



   /*******************************************************************************
    ** Fluent setter for mirroredLocks
    *******************************************************************************/
   public RecordReferenceTableMetaData withMirroredLocks(boolean mirroredLocks)
   {
      setMirroredLocks(mirroredLocks);
      return (this);
   }



   /*******************************************************************************
    ** Getter for authorField
    *******************************************************************************/
   public String getAuthorField()
   {
      return (this.authorField);
   }



   /*******************************************************************************
    ** Setter for authorField
    *******************************************************************************/
   public void setAuthorField(String authorField)
   {
      this.authorField = authorField;
   }



   /*******************************************************************************
    ** Fluent setter for authorField
    *******************************************************************************/
   public RecordReferenceTableMetaData withAuthorField(String authorField)
   {
      setAuthorField(authorField);
      return (this);
   }

}
