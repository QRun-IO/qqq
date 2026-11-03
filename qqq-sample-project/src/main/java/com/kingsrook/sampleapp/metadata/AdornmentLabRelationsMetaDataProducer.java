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

package com.kingsrook.sampleapp.metadata;


import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** The Adornment Lab's related tables and joins (QRun-IO/qqq#1350):
 ** - adornmentLabTag: a composite-key association - each tag is joined to its
 **   lab row on BOTH id and labKey, so a tag whose key does not match its
 **   parent's is not one of the parent's tags;
 ** - adornmentLabLocker: a readable table whose "secrets" association is to
 **   adornmentLabSecret, which needs a permission the mock user does not have,
 **   so clients can see how a denied child read looks (kept off adornmentLab,
 **   whose own associations would otherwise be denied with it);
 ** - adornmentLab to carrier: the many-to-one join behind the "Carrier"
 **   exposed join.
 *******************************************************************************/
public class AdornmentLabRelationsMetaDataProducer extends MetaDataProducer<MetaDataProducerMultiOutput>
{
   public static final String TABLE_TAG    = "adornmentLabTag";
   public static final String TABLE_LOCKER = "adornmentLabLocker";
   public static final String TABLE_SECRET = "adornmentLabSecret";

   public static final String JOIN_TAG     = "adornmentLabJoinAdornmentLabTag";
   public static final String JOIN_SECRET  = "adornmentLabLockerJoinAdornmentLabSecret";
   public static final String JOIN_CARRIER = "adornmentLabJoinCarrier";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance)
   {
      QTableMetaData tag = rdbmsTable(new QTableMetaData()
         .withName(TABLE_TAG).withLabel("Adornment Lab Tag")
         .withPrimaryKeyField("id").withRecordLabelFields("tag")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("adornmentLabId", QFieldType.INTEGER).withIsRequired(true))
         .withField(new QFieldMetaData("labKey", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("tag", QFieldType.STRING).withIsRequired(true).withMaxLength(80)));

      QTableMetaData secret = rdbmsTable(new QTableMetaData()
         .withName(TABLE_SECRET).withLabel("Adornment Lab Secret")
         .withPrimaryKeyField("id").withRecordLabelFields("id")
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("adornmentLabLockerId", QFieldType.INTEGER).withIsRequired(true))
         .withField(new QFieldMetaData("secret", QFieldType.STRING)));

      QTableMetaData locker = rdbmsTable(new QTableMetaData()
         .withName(TABLE_LOCKER).withLabel("Adornment Lab Locker")
         .withPrimaryKeyField("id").withRecordLabelFields("name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(80))
         .withAssociation(new Association().withName("secrets").withAssociatedTableName(TABLE_SECRET).withJoinName(JOIN_SECRET)));

      return (new MetaDataProducerMultiOutput()
         .with(tag)
         .with(secret)
         .with(locker)
         .with(new QJoinMetaData().withName(JOIN_TAG).withType(JoinType.ONE_TO_MANY)
            .withLeftTable(AdornmentLabTableMetaDataProducer.NAME).withRightTable(TABLE_TAG)
            .withJoinOn(new JoinOn("id", "adornmentLabId"))
            .withJoinOn(new JoinOn("labKey", "labKey")))
         .with(new QJoinMetaData().withName(JOIN_SECRET).withType(JoinType.ONE_TO_MANY)
            .withLeftTable(TABLE_LOCKER).withRightTable(TABLE_SECRET)
            .withJoinOn(new JoinOn("id", "adornmentLabLockerId")))
         .with(new QJoinMetaData().withName(JOIN_CARRIER).withType(JoinType.MANY_TO_ONE)
            .withLeftTable(AdornmentLabTableMetaDataProducer.NAME).withRightTable(SampleMetaDataProvider.TABLE_NAME_CARRIER)
            .withJoinOn(new JoinOn("carrierId", "id"))));
   }



   /*******************************************************************************
    ** a table in the sample's RDBMS backend, with snake_case table and columns
    *******************************************************************************/
   private static QTableMetaData rdbmsTable(QTableMetaData table)
   {
      table.withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
      QInstanceEnricher.setInferredFieldBackendNames(table);
      return (table);
   }
}
