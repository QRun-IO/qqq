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
import java.util.Arrays;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SharedRecordProcessUtils
{
   /*******************************************************************************
    **
    *******************************************************************************/
   record AssetTableAndRecord(QTableMetaData table, ShareableTableMetaData shareableTableMetaData, QRecord record, Serializable recordId) {}



   /*******************************************************************************
    **
    *******************************************************************************/
   static AssetTableAndRecord getAssetTableAndRecord(String tableName, String recordIdString) throws QException
   {
      //////////////////////////////
      // validate the asset table //
      //////////////////////////////
      QTableMetaData assetTable = QContext.getQInstance().getTable(tableName);
      if(assetTable == null)
      {
         throw (new QException("The specified tableName, " + tableName + ", was not found."));
      }

      ShareableTableMetaData shareableTableMetaData = assetTable.getShareableTableMetaData();
      if(shareableTableMetaData == null)
      {
         throw (new QException("The specified tableName, " + tableName + ", is not shareable."));
      }

      //////////////////////////////
      // look up the asset record //
      //////////////////////////////
      Serializable recordId    = ValueUtils.getValueAsFieldType(assetTable.getField(assetTable.getPrimaryKeyField()).getType(), recordIdString);
      QRecord      assetRecord = new GetAction().executeForRecord(new GetInput(tableName).withPrimaryKey(recordId));
      if(assetRecord == null)
      {
         throw (new QException("A record could not be found in table, " + tableName + ", with primary key: " + recordIdString));
      }

      return new AssetTableAndRecord(assetTable, shareableTableMetaData, assetRecord, recordId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void assertRecordOwnership(AssetTableAndRecord asset, String verbClause) throws QException
   {
      String ownerField = asset.shareableTableMetaData().getThisTableOwnerIdFieldName();
      if(StringUtils.hasContent(ownerField))
      {
         Serializable userId = QContext.getQSession().getUser().getIdReference();
         QQueryFilter filter = new QQueryFilter(
            new QFilterCriteria(asset.table().getPrimaryKeyField(), QCriteriaOperator.EQUALS, asset.recordId()),
            new QFilterCriteria(ownerField, QCriteriaOperator.EQUALS, userId));
         if(userId == null || CountAction.execute(asset.table().getName(), filter) != 1)
         {
            throw (new QException("You are not the owner of this record, so you may not " + verbClause + " it."));
         }
      }
   }



   /*******************************************************************************
    ** Check the stored relationship before either mutation. A filtered count
    ** avoids presentation customizers and hidden-field omission on returned rows.
    *******************************************************************************/
   static void assertShareBelongsToAsset(AssetTableAndRecord asset, Integer shareId, String operation) throws QException
   {
      ShareableTableMetaData sharing = asset.shareableTableMetaData();
      QTableMetaData shareTable = QContext.getQInstance().getTable(sharing.getSharedRecordTableName());
      QQueryFilter filter = new QQueryFilter(
         new QFilterCriteria(shareTable.getPrimaryKeyField(), QCriteriaOperator.EQUALS, shareId),
         new QFilterCriteria(sharing.getAssetIdFieldName(), QCriteriaOperator.EQUALS, asset.recordId()));
      if(CountAction.execute(sharing.getSharedRecordTableName(), filter) != 1)
      {
         throw new QException("Error " + ("update".equals(operation) ? "editing" : "deleting")
            + " shared record: No record was found to " + operation + " for the specified asset and share.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static ShareScope validateScopeId(String scopeId) throws QException
   {
      ////////////////////////////////
      // validate input share scope //
      ////////////////////////////////
      ShareScope shareScope = null;
      try
      {
         shareScope = ShareScope.valueOf(scopeId);
         return (shareScope);
      }
      catch(IllegalArgumentException e)
      {
         throw (new QException("[" + scopeId + "] is not a recognized value for shareScope.  Allowed values are: " + Arrays.toString(ShareScope.values())));
      }
   }

}
