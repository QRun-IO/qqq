/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.sharing;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
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
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareScopePossibleValueMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** InsertSharedRecord:  {tableName; recordId; audienceType; audienceId; scopeId;}
 *******************************************************************************/
public class InsertSharedRecordProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "insertSharedRecord";



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
                  .withField(new QFieldMetaData("audienceType", QFieldType.STRING)) // todo take a PVS name as param?
                  .withField(new QFieldMetaData("audienceId", QFieldType.STRING))
                  .withField(new QFieldMetaData("scopeId", QFieldType.STRING).withPossibleValueSourceName(ShareScopePossibleValueMetaDataProducer.NAME))
               )
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String tableName        = runBackendStepInput.getValueString("tableName");
      String recordIdString   = runBackendStepInput.getValueString("recordId");
      String audienceType     = runBackendStepInput.getValueString("audienceType");
      String audienceIdString = runBackendStepInput.getValueString("audienceId");
      String scopeId          = runBackendStepInput.getValueString("scopeId");

      Objects.requireNonNull(tableName, "Missing required input: tableName");
      Objects.requireNonNull(recordIdString, "Missing required input: recordId");
      Objects.requireNonNull(audienceType, "Missing required input: audienceType");
      Objects.requireNonNull(audienceIdString, "Missing required input: audienceId");
      Objects.requireNonNull(scopeId, "Missing required input: scopeId");

      String assetTableLabel = tableName;
      try
      {
         SharedRecordProcessUtils.AssetTableAndRecord assetTableAndRecord = SharedRecordProcessUtils.getAssetTableAndRecord(tableName, recordIdString);

         ShareableTableMetaData shareableTableMetaData = assetTableAndRecord.shareableTableMetaData();
         Serializable           recordId               = assetTableAndRecord.recordId();
         assetTableLabel = assetTableAndRecord.table().getLabel();

         SharedRecordProcessUtils.assertRecordOwnership(assetTableAndRecord, "share");

         ////////////////////////////////
         // validate the audience type //
         ////////////////////////////////
         ShareableAudienceType shareableAudienceType = shareableTableMetaData.getAudienceTypes().get(audienceType);
         if(shareableAudienceType == null)
         {
            throw (new QException("[" + audienceType + "] is not a recognized audience type for sharing records from the " + tableName + " table.  Allowed values are: " + shareableTableMetaData.getAudienceTypes().keySet()));
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////
         // if we know the audience source-table, then fetch & validate security-wise the audience id //
         ///////////////////////////////////////////////////////////////////////////////////////////////
         Serializable audienceId         = audienceIdString;
         String       audienceTableLabel = "audience";
         if(StringUtils.hasContent(shareableAudienceType.getSourceTableName()))
         {
            QTableMetaData audienceTable = QContext.getQInstance().getTable(shareableAudienceType.getSourceTableName());
            audienceTableLabel = audienceTable.getLabel();

            GetInput getInput = new GetInput(audienceTable.getName());
            if(StringUtils.hasContent(shareableAudienceType.getSourceTableKeyFieldName()))
            {
               audienceId = ValueUtils.getValueAsFieldType(audienceTable.getField(shareableAudienceType.getSourceTableKeyFieldName()).getType(), audienceIdString);
               getInput.withUniqueKey(Map.of(shareableAudienceType.getSourceTableKeyFieldName(), audienceId));
            }
            else
            {
               audienceId = ValueUtils.getValueAsFieldType(audienceTable.getField(audienceTable.getPrimaryKeyField()).getType(), audienceIdString);
               getInput.withPrimaryKey(audienceId);
            }

            QRecord audienceRecord = new GetAction().executeForRecord(getInput);
            if(audienceRecord == null)
            {
               throw (new QException("A record could not be found for audience type " + audienceType + ", audience id: " + audienceIdString));
            }
         }

         ////////////////////////////////
         // validate input share scope //
         ////////////////////////////////
         ShareScope shareScope = SharedRecordProcessUtils.validateScopeId(scopeId);

         ///////////////////
         // do the insert //
         ///////////////////
         InsertOutput insertOutput = new InsertAction().execute(new InsertInput(shareableTableMetaData.getSharedRecordTableName()).withRecord(new QRecord()
            .withValue(shareableTableMetaData.getAssetIdFieldName(), recordId)
            .withValue(shareableTableMetaData.getScopeFieldName(), shareScope.getPossibleValueId())
            .withValue(shareableAudienceType.getFieldName(), audienceId)));

         //////////////////////
         // check for errors //
         //////////////////////
         if(CollectionUtils.nullSafeHasContents(insertOutput.getRecords().get(0).getErrors()))
         {
            QErrorMessage errorMessage = insertOutput.getRecords().get(0).getErrors().get(0);
            if(errorMessage instanceof DuplicateKeyBadInputStatusMessage)
            {
               throw (new QUserFacingException("This " + assetTableLabel + " has already been shared with this " + audienceTableLabel));
            }
            else if(errorMessage instanceof BadInputStatusMessage)
            {
               throw (new QUserFacingException(errorMessage.getMessage()));
            }
            throw (new QException("Error sharing " + assetTableLabel + ": " + errorMessage.getMessage()));
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error sharing " + assetTableLabel, e));
      }
   }

}
