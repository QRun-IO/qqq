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

package com.kingsrook.qqq.backend.core.model.helpcontent;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Meta-data provider for table & PVS's for defining help-content for other
 ** meta-data objects within a QQQ app
 *******************************************************************************/
public class HelpContentMetaDataProvider
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      defineHelpContentTable(instance, backendName, backendDetailEnricher);
      instance.addPossibleValueSource(QPossibleValueSource.newForEnum(HelpContentFormat.NAME, HelpContentFormat.values()));
      instance.addPossibleValueSource(QPossibleValueSource.newForEnum(HelpContentRole.NAME, HelpContentRole.values()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineHelpContentTable(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(HelpContent.TABLE_NAME)
         .withBackendName(backendName)
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("key", "role")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("key", "role"))
         .withFieldsFromEntity(HelpContent.class)
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "key", "role")))
         .withSection(new QFieldSection("content", new QIcon("dataset"), Tier.T2, List.of("format", "content")))
         .withSection(new QFieldSection("dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(HelpContentPostInsertCustomizer.class))
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(HelpContentPostUpdateCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(HelpContentPreUpdateCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(HelpContentPreDeleteCustomizer.class));

      table.getField("format").withFieldAdornment(AdornmentType.Size.SMALL.toAdornment());
      table.getField("key").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      table.getField("content").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      table.getField("content").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("html")));
      table.getField("content").withGridColumns(12);

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      instance.addTable(table);
   }

}
