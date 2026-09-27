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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Meta Data Producer for RedirectState table
 *******************************************************************************/
public class RedirectStateMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
   public static final String TABLE_NAME = "redirectState";

   private final String backendName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RedirectStateMetaDataProducer(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData produce(QInstance qInstance) throws QException
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(TABLE_NAME)
         .withBackendName(backendName)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("state")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("state"))
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))

         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("state", QFieldType.STRING).withIsEditable(false).withMaxLength(45).withBehavior(ValueTooLongBehavior.ERROR))
         .withField(new QFieldMetaData("redirectUri", QFieldType.STRING).withIsEditable(false).withMaxLength(4096).withBehavior(ValueTooLongBehavior.ERROR))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false));

      return tableMetaData;
   }

}
