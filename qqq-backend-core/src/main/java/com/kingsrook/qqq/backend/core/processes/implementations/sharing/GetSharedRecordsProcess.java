/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.sharing;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** GetSharedRecords:  {tableName; recordId;} => [{id; audienceType; audienceId; audienceLabel; scopeId}]
 *******************************************************************************/
public class GetSharedRecordsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "getSharedRecords";

   private static final QLogger LOG = QLogger.getLogger(GetSharedRecordsProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withIcon(new QIcon().withName("share"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)) // todo confirm or protect
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(getClass()))
               .withInputData(new QFunctionInputMetaData()
                  .withField(new QFieldMetaData("tableName", QFieldType.STRING).withPossibleValueSourceName(TablesPossibleValueSourceMetaDataProvider.NAME)) // todo - actually only a subset of this...
                  .withField(new QFieldMetaData("recordId", QFieldType.STRING))
               )
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String tableName      = runBackendStepInput.getValueString("tableName");
      String recordIdString = runBackendStepInput.getValueString("recordId");

      Objects.requireNonNull(tableName, "Missing required input: tableName");
      Objects.requireNonNull(recordIdString, "Missing required input: recordId");

      try
      {
         SharedRecordProcessUtils.AssetTableAndRecord assetTableAndRecord = SharedRecordProcessUtils.getAssetTableAndRecord(tableName, recordIdString);

         ShareableTableMetaData shareableTableMetaData = assetTableAndRecord.shareableTableMetaData();
         QTableMetaData         shareTable             = QContext.getQInstance().getTable(shareableTableMetaData.getSharedRecordTableName());
         Serializable           recordId               = assetTableAndRecord.recordId();

         /////////////////////////////////////
         // query for shares on this record //
         /////////////////////////////////////
         QueryInput queryInput = new QueryInput(shareTable.getName());
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria(shareableTableMetaData.getAssetIdFieldName(), QCriteriaOperator.EQUALS, recordId))
            .withOrderBy(new QFilterOrderBy(shareTable.getPrimaryKeyField()))
         );
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // iterate results, building QRecords to output - note - we'll need to collect ids, then look them up in audience-source tables //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         ArrayList<QRecord>                resultList  = new ArrayList<>();
         ListingHash<String, Serializable> audienceIds = new ListingHash<>();
         for(QRecord record : queryOutput.getRecords())
         {
            QRecord outputRecord = new QRecord();
            outputRecord.setValue("shareId", record.getValue(shareTable.getPrimaryKeyField()));
            outputRecord.setValue("scopeId", record.getValue(shareableTableMetaData.getScopeFieldName()));

            boolean foundAudienceType = false;
            for(ShareableAudienceType audienceType : shareableTableMetaData.getAudienceTypes().values())
            {
               Serializable audienceId = record.getValue(audienceType.getFieldName());
               if(audienceId != null)
               {
                  outputRecord.setValue("audienceType", audienceType.getName());
                  outputRecord.setValue("audienceId", audienceId);
                  audienceIds.add(audienceType.getName(), audienceId);
                  foundAudienceType = true;
                  break;
               }
            }

            if(!foundAudienceType)
            {
               LOG.warn("Failed to find what audience type to use for a shared record",
                  logPair("sharedTableName", shareTable.getName()),
                  logPair("id", record.getValue(shareTable.getPrimaryKeyField())),
                  logPair("recordId", record.getValue(shareableTableMetaData.getAssetIdFieldName())));
               continue;
            }

            resultList.add(outputRecord);
         }

         /////////////////////////////////
         // look up the audience labels //
         /////////////////////////////////
         Map<String, Map<Serializable, String>> audienceLabels          = new HashMap<>();
         Set<String>                            audienceTypesWithLabels = new HashSet<>();
         for(Map.Entry<String, List<Serializable>> entry : audienceIds.entrySet())
         {
            String             audienceType = entry.getKey();
            List<Serializable> ids          = entry.getValue();
            if(CollectionUtils.nullSafeHasContents(ids))
            {
               ShareableAudienceType shareableAudienceType = shareableTableMetaData.getAudienceTypes().get(audienceType);
               if(StringUtils.hasContent(shareableAudienceType.getSourceTableName()))
               {
                  audienceTypesWithLabels.add(audienceType);

                  String keyField = shareableAudienceType.getSourceTableKeyFieldName();
                  if(!StringUtils.hasContent(keyField))
                  {
                     keyField = QContext.getQInstance().getTable(shareableAudienceType.getSourceTableName()).getPrimaryKeyField();
                  }

                  QueryInput audienceQueryInput = new QueryInput(shareableAudienceType.getSourceTableName());
                  audienceQueryInput.setFilter(new QQueryFilter(new QFilterCriteria(keyField, QCriteriaOperator.IN, ids)));
                  audienceQueryInput.setShouldGenerateDisplayValues(true); // to get record labels
                  QueryOutput audienceQueryOutput = new QueryAction().execute(audienceQueryInput);
                  for(QRecord audienceRecord : audienceQueryOutput.getRecords())
                  {
                     audienceLabels.computeIfAbsent(audienceType, k -> new HashMap<>());
                     audienceLabels.get(audienceType).put(audienceRecord.getValue(keyField), audienceRecord.getRecordLabel());
                  }
               }
            }
         }

         ////////////////////////////////////////////
         // put those labels on the output records //
         ////////////////////////////////////////////
         for(QRecord outputRecord : resultList)
         {
            String                    audienceType = outputRecord.getValueString("audienceType");
            Map<Serializable, String> typeLabels   = audienceLabels.getOrDefault(audienceType, Collections.emptyMap());
            Serializable              audienceId   = outputRecord.getValue("audienceId");
            String                    label        = typeLabels.get(audienceId);
            if(StringUtils.hasContent(label))
            {
               outputRecord.setValue("audienceLabel", label);
            }
            else
            {
               if(audienceTypesWithLabels.contains(audienceType))
               {
                  outputRecord.setValue("audienceLabel", "Unknown " + audienceType + " (id=" + audienceId + ")");
               }
               else
               {
                  outputRecord.setValue("audienceLabel", audienceType + " " + audienceId);
               }
            }
         }

         ////////////////////////////
         // sort results by labels //
         ////////////////////////////
         resultList.sort(Comparator.comparing(r -> r.getValueString("audienceLabel")));

         runBackendStepOutput.addValue("resultList", resultList);
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error getting shared records.", e));
      }
   }

}
