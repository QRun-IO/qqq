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


import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
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
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** EditSharedRecord:  {tableName; recordId; shareId; scopeId;}
 *******************************************************************************/
public class EditSharedRecordProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "editSharedRecord";



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
                  .withField(new QFieldMetaData("scopeId", QFieldType.STRING).withPossibleValueSourceName(ShareScopePossibleValueMetaDataProducer.NAME))
                  .withField(new QFieldMetaData("shareId", QFieldType.INTEGER))
               )
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String  tableName      = runBackendStepInput.getValueString("tableName");
      String  recordIdString = runBackendStepInput.getValueString("recordId");
      String  scopeId        = runBackendStepInput.getValueString("scopeId");
      Integer shareId        = runBackendStepInput.getValueInteger("shareId");

      Objects.requireNonNull(tableName, "Missing required input: tableName");
      Objects.requireNonNull(recordIdString, "Missing required input: recordId");
      Objects.requireNonNull(scopeId, "Missing required input: scopeId");
      Objects.requireNonNull(shareId, "Missing required input: shareId");

      try
      {
         SharedRecordProcessUtils.AssetTableAndRecord assetTableAndRecord = SharedRecordProcessUtils.getAssetTableAndRecord(tableName, recordIdString);

         ShareableTableMetaData shareableTableMetaData = assetTableAndRecord.shareableTableMetaData();
         QTableMetaData         shareTable             = QContext.getQInstance().getTable(shareableTableMetaData.getSharedRecordTableName());

         SharedRecordProcessUtils.assertRecordOwnership(assetTableAndRecord, "edit shares of");
         SharedRecordProcessUtils.assertShareBelongsToAsset(assetTableAndRecord, shareId, "update");
         ShareScope shareScope = SharedRecordProcessUtils.validateScopeId(scopeId);

         ///////////////////
         // do the update //
         ///////////////////
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(shareableTableMetaData.getSharedRecordTableName()).withRecord(new QRecord()
            .withValue(shareTable.getPrimaryKeyField(), shareId)
            .withValue(shareableTableMetaData.getAssetIdFieldName(), assetTableAndRecord.recordId())
            .withValue(shareableTableMetaData.getScopeFieldName(), shareScope.getPossibleValueId())));

         //////////////////////
         // check for errors //
         //////////////////////
         if(CollectionUtils.nullSafeHasContents(updateOutput.getRecords().get(0).getErrors()))
         {
            throw (new QException("Error editing shared record: " + updateOutput.getRecords().get(0).getErrors().get(0).getMessage()));
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error editing shared record", e));
      }
   }

}
