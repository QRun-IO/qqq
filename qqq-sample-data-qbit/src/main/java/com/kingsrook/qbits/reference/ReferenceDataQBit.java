/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

/* Copyright 2026 QRun-IO. */

package com.kingsrook.qbits.reference;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.json.JSONArray;

/** A generated reference-data consumer installed in the sample host. */
public final class ReferenceDataQBit implements QBitProducer
{
   private final String prefix;
   private final String backendName;
   private final ReferenceDataQBitConfig config;

   /** Select the generated table prefix and an existing host backend. */
   public ReferenceDataQBit(String prefix, String backendName)
   {
      this.config = new ReferenceDataQBitConfig(backendName, prefix);
      this.prefix = prefix;
      this.backendName = backendName;
   }

   /** Return the QQQ metadata name for this generated table. */
   public String tableName()
   {
      return config.applyPrefix("referenceCategory");
   }

   @Override
   public void produce(QInstance instance, String namespace) throws QException
   {
      config.validate(instance);
      QBitMetaData identity = new QBitMetaData().withGroupId("com.kingsrook.qbits")
         .withArtifactId("qqq-sample-data-qbit").withVersion("1.0.0")
         .withNamespace(namespace == null ? prefix : namespace).withConfig(config);
      instance.addQBit(identity);
      QTableMetaData table = new QTableMetaData().withName(tableName()).withLabel("Reference Categories")
         .withBackendName(backendName).withSourceQBitName(identity.getName())
         .withPrimaryKeyField("id").withRecordLabelFields("name")
         .withUniqueKey(new UniqueKey("code"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsRequired(true).withMaxLength(20))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(100))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN).withDefaultValue(true));
      if("rdbms".equals(instance.getBackend(backendName).getBackendType()))
      {
         table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(tableName()));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      }
      instance.addTable(table);
      instance.addPossibleValueSource(QPossibleValueSource.newForTable(tableName()));
      instance.addProcess(new QProcessMetaData().withName(prefix + "_syncReferenceCategories")
         .withLabel("Sync Reference Categories").withTableName(tableName()).withIsHidden(true)
         .withSourceQBitName(identity.getName())
         .withStep(new QBackendStepMetaData().withName("sync")
            .withCode(new QCodeReference(SyncStep.class))));
   }

   /** Read the actual host table through the QQQ query action. */
   public List<QRecord> records() throws QException
   {
      return new QueryAction().execute(new QueryInput().withTableName(tableName()).withFilter(new QQueryFilter())).getRecords();
   }

   /** Return a changelog for hosts that store this generated table in an RDBMS. */
   public String liquibaseChangelog()
   {
      return ReferenceDataLiquibaseGenerator.generate(prefix, tableName());
   }

   /** Reconcile active source rows by their natural code key. */
   public SyncResult sync(List<Source> source) throws QException
   {
      Objects.requireNonNull(source, "source");
      Set<String> seen = new HashSet<>();
      for(Source item : source)
      {
         if(item == null || item.code() == null || item.code().isBlank() || item.code().length() > 20
            || item.name() == null || item.name().isBlank() || item.name().length() > 100
            || !seen.add(item.code()))
         {
            throw new IllegalArgumentException("Invalid or duplicate reference-data source key");
         }
      }

      Map<String, QRecord> existing = new HashMap<>();
      for(QRecord record : records())
      {
         if(existing.put(record.getValueString("code"), record) != null)
         {
            throw new IllegalStateException("Duplicate stored reference-data key");
         }
      }
      int inserted = 0;
      int updated = 0;
      int unchanged = 0;
      int deactivated = 0;
      for(Source item : source)
      {
         QRecord record = existing.remove(item.code());
         if(record == null)
         {
            requireSuccess(new InsertAction().executeForRecord(new InsertInput(tableName()).withRecord(
               new QRecord().withValue("code", item.code()).withValue("name", item.name()).withValue("isActive", true))));
            inserted++;
         }
         else if(!item.name().equals(record.getValueString("name")) || !Boolean.TRUE.equals(record.getValueBoolean("isActive")))
         {
            record.setValue("name", item.name());
            record.setValue("isActive", true);
            requireSuccess(new UpdateAction().executeForRecord(new UpdateInput(tableName()).withRecord(record)));
            updated++;
         }
         else
         {
            unchanged++;
         }
      }
      for(QRecord record : existing.values())
      {
         if(Boolean.TRUE.equals(record.getValueBoolean("isActive")))
         {
            record.setValue("isActive", false);
            requireSuccess(new UpdateAction().executeForRecord(new UpdateInput(tableName()).withRecord(record)));
            deactivated++;
         }
      }
      return new SyncResult(inserted, updated, unchanged, deactivated);
   }

   /** Reject action-level validation errors as sync failures. */
   private void requireSuccess(QRecord record)
   {
      if(!record.getErrors().isEmpty())
      {
         throw new IllegalStateException("Reference-data write failed");
      }
   }

   /** One source reference row. */
   public record Source(String code, String name) {}

   /** Counts of changed and unchanged rows. */
   public record SyncResult(int inserted, int updated, int unchanged, int deactivated) {}

   /** Runs the bundled source fixture through the registered QQQ process. */
   public static class SyncStep implements BackendStep
   {
      /** Load the classpath fixture and execute the natural-key sync. */
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String tableName = input.getTableName();
         String suffix = "_referenceCategory";
         if(tableName == null || !tableName.endsWith(suffix))
         {
            throw new QException("Invalid reference-data process table");
         }
         String prefix = tableName.substring(0, tableName.length() - suffix.length());
         String backend = QContext.getQInstance().getTable(tableName).getBackendName();
         try(InputStream stream = SyncStep.class.getResourceAsStream("/data/reference-categories.json"))
         {
            if(stream == null)
            {
               throw new QException("Reference-data source is missing");
            }
            JSONArray json = new JSONArray(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            List<Source> source = new java.util.ArrayList<>();
            for(int i = 0; i < json.length(); i++)
            {
               source.add(new Source(json.getJSONObject(i).getString("code"), json.getJSONObject(i).getString("name")));
            }
            SyncResult result = new ReferenceDataQBit(prefix, backend).sync(source);
            output.addValue("inserted", result.inserted());
            output.addValue("updated", result.updated());
            output.addValue("deactivated", result.deactivated());
         }
         catch(java.io.IOException e)
         {
            throw new QException("Could not load reference-data source", e);
         }
      }
   }
}
