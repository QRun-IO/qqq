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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Object made available to scripts for access to qqq api (e.g., query, insert,
 ** etc, plus object constructors).
 **
 ** Before scripts knew about the API, this class made sense and was used.
 ** But, when scripts gained knowledge of the API, then it felt like this class could
 ** be deleted... but, what about, a QQQ deployment without the API module...
 ** In that case, we might still want this class... think about it.
 **
 ** And/Or - it turns out - sometimes using QQQ directly is "better" (?) than using
 ** an api - so - this object may be available for other use cases (e.g., getting
 ** a record's backendDetails (e.g., for full json from a source backend api)).
 *******************************************************************************/
public class QqqScriptUtils implements Serializable
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryInput newQueryInput()
   {
      return (new QueryInput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QQueryFilter newQueryFilter()
   {
      return (new QQueryFilter());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFilterCriteria newFilterCriteria()
   {
      return (new QFilterCriteria());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFilterOrderBy newFilterOrderBy()
   {
      return (new QFilterOrderBy());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord newRecord()
   {
      return (new QRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> query(String tableName, QQueryFilter filter) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
      return (new QueryAction().execute(queryInput).getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> query(QueryInput queryInput) throws QException
   {
      PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
      return (new QueryAction().execute(queryInput).getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void insert(String tableName, QRecord record) throws QException
   {
      insert(tableName, List.of(record));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void insert(String tableName, List<QRecord> recordList) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(tableName);
      insertInput.setRecords(recordList);
      PermissionsHelper.checkTablePermissionThrowing(insertInput, TablePermissionSubType.INSERT);
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void update(String tableName, QRecord record) throws QException
   {
      update(tableName, List.of(record));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void update(String tableName, List<QRecord> recordList) throws QException
   {
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(tableName);
      updateInput.setRecords(recordList);
      PermissionsHelper.checkTablePermissionThrowing(updateInput, TablePermissionSubType.EDIT);
      new UpdateAction().execute(updateInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void delete(String tableName, Serializable primaryKey) throws QException
   {
      delete(tableName, List.of(primaryKey));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void delete(String tableName, QRecord record) throws QException
   {
      delete(tableName, List.of(record));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void delete(String tableName, List<?> recordOrPrimaryKeyList) throws QException
   {
      QTableMetaData table       = QContext.getQInstance().getTable(tableName);
      DeleteInput    deleteInput = new DeleteInput();
      deleteInput.setTableName(tableName);
      List<Serializable> primaryKeyList = new ArrayList<>();
      for(Object o : recordOrPrimaryKeyList)
      {
         if(o instanceof QRecord qRecord)
         {
            primaryKeyList.add(qRecord.getValue(table.getPrimaryKeyField()));
         }
         else
         {
            primaryKeyList.add((Serializable) o);
         }
      }
      deleteInput.setPrimaryKeys(primaryKeyList);
      PermissionsHelper.checkTablePermissionThrowing(deleteInput, TablePermissionSubType.DELETE);
      new DeleteAction().execute(deleteInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void delete(String tableName, QQueryFilter filter) throws QException
   {
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(tableName);
      deleteInput.setQueryFilter(filter);
      PermissionsHelper.checkTablePermissionThrowing(deleteInput, TablePermissionSubType.DELETE);
      new DeleteAction().execute(deleteInput);
   }

}
