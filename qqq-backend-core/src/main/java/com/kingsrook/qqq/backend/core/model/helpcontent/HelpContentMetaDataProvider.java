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
import com.kingsrook.qqq.backend.core.actions.help.HelpBundleProcessStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
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
   private boolean targetingFields = true;
   private boolean provenanceFields = true;


   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      defineHelpContentTable(instance, backendName, backendDetailEnricher);
      if(provenanceFields)
      {
         instance.addPossibleValueSource(QPossibleValueSource.newForEnum(HelpContentSource.NAME, HelpContentSource.values()));
         instance.addPossibleValueSource(QPossibleValueSource.newForEnum(HelpContentUpstreamStatus.NAME, HelpContentUpstreamStatus.values()));
         if(targetingFields)
         {
            HelpBundles.register(instance, "help/qqq-core.yaml", HelpContentMetaDataProvider.class.getClassLoader());
            HelpBundleProcessStep.defineAll(instance);
         }
      }
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
         .withSection(new QFieldSection("content", new QIcon("dataset"), Tier.T2, targetingFields ? List.of("summary", "format", "content") : List.of("format", "content")))
         .withSection(new QFieldSection("dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(HelpContentPostInsertCustomizer.class))
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(HelpContentPostUpdateCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(HelpContentPreUpdateCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(HelpContentPreDeleteCustomizer.class));

      table.getField("format").withFieldAdornment(AdornmentType.Size.SMALL.toAdornment());
      table.getField("key").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      table.getField("content").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      table.getField("content").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("markdown")));
      table.getField("content").withGridColumns(12);

      if(targetingFields)
      {
         table.getSections().add(2, new QFieldSection("targeting", new QIcon("tune"), Tier.T2, List.of("permissionName", "clientChannel")));
      }
      else
      {
         table.getFields().keySet().removeAll(List.of("summary", "permissionName", "clientChannel"));
      }

      List<String> provenance = List.of("source", "bundleId", "bundleVersion", "lastSeededHash", "upstreamStatus", "upstreamSummary", "upstreamContent", "modifiedBy");
      if(provenanceFields)
      {
         table.withSection(new QFieldSection("provenance", new QIcon("history"), Tier.T3, provenance));
         table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(HelpContentPreInsertCustomizer.class));
      }
      else
      {
         table.getFields().keySet().removeAll(provenance);
      }

      if(instance.getTable("audit") != null && instance.getTable("auditDetail") != null
         && instance.getTable("auditTable") != null && instance.getTable("auditUser") != null)
      {
         table.withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      }

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      instance.addTable(table);
   }



   /*******************************************************************************
    ** Opt out until the existing physical helpContent table has the three new columns.
    *******************************************************************************/
   public HelpContentMetaDataProvider withTargetingFields(boolean targetingFields)
   {
      this.targetingFields = targetingFields;
      return (this);
   }



   /*******************************************************************************
    ** Opt out until the physical helpContent table has provenance columns.
    *******************************************************************************/
   public HelpContentMetaDataProvider withProvenanceFields(boolean provenanceFields)
   {
      this.provenanceFields = provenanceFields;
      return (this);
   }
}
