/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata.personalization;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 * action to execute the TableMetaDataPersonalizerInterface assigned to the
 * QInstance, e.g., to adjust a TableMetaData object to "personalize" the table
 * for the current user, before (or within) certain backend actions.
 *
 * e.g., to hide fields from a subset of users - as the query action passes the
 * table being queried through this class, so removing a field from the table
 * should effectively hide such a field's data.
 *
 * It is vitally important that the {@link QTableMetaData} returned by the execute
 * method return a clone if it makes any changes, to avoid changing the meta data
 * for the whole application!
 *******************************************************************************/
public class TableMetaDataPersonalizerAction
{

   /***************************************************************************
    * execute the personalizer, using the table in the tableActionInput param,
    * returning the table that was modified (as a clone!) or the original table
    * if no personalization applied.
    *
    * Note that the output table (e.g., w/ personalizations) will NOT be set
    * in the input `tableActionInput` - though callers may very likely want
    * to do this themselves afterwards.
    *
    * @param tableActionInput an action-input - e.g., to QueryAction or
    * TableMetaDataAction - with a table set in it (most likely through
    * tableName being set), and likely an InputSource, so the personalizer
    * implementation can choose to only apply the changes to USER-based
    * actions, if appropriate.
    *
    * @return either the original unmodified {@link QTableMetaData}, or, if
    * personaliztions were applied, a clone of that table meta data.k
    ***************************************************************************/
   public static QTableMetaData execute(AbstractTableActionInput tableActionInput) throws QException
   {
      return (execute(tableActionInput, shouldApplyFieldSecurityLocks(tableActionInput)));
   }



   /***************************************************************************
    * Variant of execute that lets the caller (e.g., a core action that has
    * already decided whether permissions are enforced for this call) say
    * whether fields denied to the session by a FieldSecurityLock should be
    * removed from the returned table (in addition to any personalizer changes).
    ***************************************************************************/
   public static QTableMetaData execute(AbstractTableActionInput tableActionInput, boolean applyFieldSecurityLocks) throws QException
   {
      QTableMetaData table = executePersonalizer(tableActionInput);
      if(applyFieldSecurityLocks && table != null)
      {
         table = FieldAccessHelper.applyToTable(table);
      }
      return (table);
   }



   /***************************************************************************
    * Field security locks are applied to the personalized table for read-style
    * inputs (query, get, count, aggregate, export, table meta data) that are
    * having permissions enforced.  Write inputs instead reject writes to denied
    * fields (see FieldAccessHelper.assertWritesAllowed), so that
    * removing fields from the table can't change insert/update semantics.
    ***************************************************************************/
   private static boolean shouldApplyFieldSecurityLocks(AbstractTableActionInput tableActionInput)
   {
      if(tableActionInput instanceof InsertInput || tableActionInput instanceof UpdateInput || tableActionInput instanceof DeleteInput || tableActionInput instanceof ReplaceInput)
      {
         return (false);
      }
      return (CorePermissionEnforcer.shouldEnforceFieldAccess(tableActionInput));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static QTableMetaData executePersonalizer(AbstractTableActionInput tableActionInput) throws QException
   {
      if(tableActionInput.getTable() == null)
      {
         ///////////////////////////////////////////////////////////////////////////
         // if input table is null (either tableName wasn't set, or it referenced //
         // a non-existing table) there's nothing to personalize, so return null  //
         ///////////////////////////////////////////////////////////////////////////
         return (null);
      }

      TableMetaDataPersonalizerInput input = new TableMetaDataPersonalizerInput();
      input.setTableName(tableActionInput.getTable().getName());
      input.setTableMetaData(tableActionInput.getTable());
      input.setInputSource(tableActionInput.getInputSource());

      TableMetaDataPersonalizerOutput output = new TableMetaDataPersonalizerOutput();
      execute(input, output);
      return (output.getTable());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static void execute(TableMetaDataPersonalizerInput input, TableMetaDataPersonalizerOutput output) throws QException
   {
      QCodeReference codeReference = QContext.getQInstance().getSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE);
      if(codeReference != null)
      {
         TableMetaDataPersonalizerInterface personalizer = QCodeLoader.getAdHoc(TableMetaDataPersonalizerInterface.class, codeReference);
         if(personalizer == null)
         {
            throw (new QException("Error initializing tableMetaDataPersonalizer"));
         }
         output.setTable(personalizer.execute(input));
      }
      else
      {
         output.setTable(input.getTable());
      }
   }

}
