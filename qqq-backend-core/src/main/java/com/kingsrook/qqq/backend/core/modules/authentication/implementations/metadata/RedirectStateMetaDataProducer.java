/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
         .withField(new QFieldMetaData("codeVerifier", QFieldType.STRING).withIsEditable(false).withMaxLength(128).withBehavior(ValueTooLongBehavior.ERROR))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false));

      return tableMetaData;
   }

}
