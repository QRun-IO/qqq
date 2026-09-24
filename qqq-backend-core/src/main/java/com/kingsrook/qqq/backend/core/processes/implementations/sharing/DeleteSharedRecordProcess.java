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


import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
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
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** DeleteSharedRecord:  {tableName; recordId; shareId;}
 *******************************************************************************/
public class DeleteSharedRecordProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "deleteSharedRecord";



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
      Integer shareId        = runBackendStepInput.getValueInteger("shareId");

      Objects.requireNonNull(tableName, "Missing required input: tableName");
      Objects.requireNonNull(recordIdString, "Missing required input: recordId");
      Objects.requireNonNull(shareId, "Missing required input: shareId");

      try
      {
         SharedRecordProcessUtils.AssetTableAndRecord assetTableAndRecord = SharedRecordProcessUtils.getAssetTableAndRecord(tableName, recordIdString);

         ShareableTableMetaData shareableTableMetaData = assetTableAndRecord.shareableTableMetaData();

         SharedRecordProcessUtils.assertRecordOwnership(assetTableAndRecord, "delete shares of");
         SharedRecordProcessUtils.assertShareBelongsToAsset(assetTableAndRecord, shareId, "delete");

         ///////////////////
         // do the delete //
         ///////////////////
         DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(shareableTableMetaData.getSharedRecordTableName()).withPrimaryKeys(List.of(shareId)));

         //////////////////////
         // check for errors //
         //////////////////////
         if(CollectionUtils.nullSafeHasContents(deleteOutput.getRecordsWithErrors()))
         {
            throw (new QException("Error deleting shared record: " + deleteOutput.getRecordsWithErrors().get(0).getErrors().get(0).getMessage()));
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error deleting shared record", e));
      }
   }

}
