/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptExecutionMode;


/*******************************************************************************
 ** Pre-insert / pre-update customizer for the Script table, which keeps the
 ** executionMode marker server-controlled (see ScriptExecutionIsolation).
 **
 ** Through any generic write (the table's own insert/update actions, the
 ** frontend, the API, bulk load, ...):
 ** - a Script can't be inserted as ISOLATED;
 ** - a Script's executionMode can't be changed - not set, and not cleared;
 ** - an ISOLATED Script's currentScriptRevisionId can't be changed;
 ** - an ISOLATED Script can't be deleted (that would orphan its revisions);
 ** - a Script can't be inserted under an explicit id that revisions reference.
 **
 ** Inside ScriptExecutionIsolation.runAsIsolationAuthority, a Script can be
 ** inserted ISOLATED (with no current revision), a LEGACY one can be marked
 ** ISOLATED, and an ISOLATED one can be activated - but ISOLATED can never go
 ** back to LEGACY.
 **
 ** ScriptsMetaDataProvider adds this to the script table it defines; an app that
 ** defines its own script table must add it too (and the executionMode field).
 *******************************************************************************/
public class ScriptTableCustomizer implements TableCustomizerInterface
{
   private static final String FIELD_CURRENT_REVISION_ID = "currentScriptRevisionId";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      for(QRecord record : records)
      {
         /////////////////////////////////////////////////////////////////////////////////////////
         // a script re-inserted under an explicit id that revisions still reference would      //
         // silently adopt (as unmarked, LEGACY) the revisions of a deleted isolated project    //
         /////////////////////////////////////////////////////////////////////////////////////////
         Integer explicitId = record.getValueInteger("id");
         if(explicitId != null && ScriptExecutionIsolation.hasRevisionsAsSystem(explicitId, insertInput.getTransaction()))
         {
            throw (new QPermissionDeniedException("A script cannot be inserted with id " + explicitId + ", as script revisions already reference that id."));
         }

         Object rawMode = record.getValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME);
         if(ScriptExecutionMode.fromValue(rawMode) == ScriptExecutionMode.ISOLATED)
         {
            if(!ScriptExecutionIsolation.isIsolationAuthorityActive())
            {
               throw (new QPermissionDeniedException("A script's execution mode is server-controlled, and cannot be set by this operation."));
            }

            //////////////////////////////////////////////////////////////////////////////////////////
            // a restore replays a state that was valid when backed up, so - and only inside the     //
            // isolation authority - its isolated script may arrive with its revision pointer.       //
            // otherwise, a new isolated script has no current revision (activate one afterwards).   //
            //////////////////////////////////////////////////////////////////////////////////////////
            boolean restoreReplay = RestoreWriteFlag.from(insertInput.getFlags()) != null;
            if(record.getValue(FIELD_CURRENT_REVISION_ID) != null && !restoreReplay)
            {
               throw (new QPermissionDeniedException("A new isolated script cannot have a current script revision."));
            }
         }
      }

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      boolean authority = ScriptExecutionIsolation.isIsolationAuthorityActive();

      for(QRecord record : records)
      {
         boolean hasMode     = record.getValues().containsKey(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME);
         boolean hasRevision = record.getValues().containsKey(FIELD_CURRENT_REVISION_ID);
         if(!hasMode && !hasRevision)
         {
            continue;
         }

         QRecord oldRecord = findOldRecord(updateInput, record, oldRecordList);
         if(oldRecord == null)
         {
            ///////////////////////////////////////////////////////////////////////////////
            // not an existing script - nothing to protect (the update itself will deal) //
            ///////////////////////////////////////////////////////////////////////////////
            continue;
         }

         ScriptExecutionMode oldMode = ScriptExecutionMode.fromValue(oldRecord.getValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME));
         if(hasMode)
         {
            ScriptExecutionMode newMode = ScriptExecutionMode.fromValue(record.getValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME));
            Object oldRaw = oldRecord.getValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME);
            Object newRaw = record.getValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME);

            ///////////////////////////////////////////////////////////////////////////////////
            // a null/blank echo on a legacy script is normalized back to the stored value   //
            // (a form posting "" for an unmarked script) - not a change.  Any raw change on //
            // an isolated script (even to a value that reads as legacy) stays forbidden.    //
            ///////////////////////////////////////////////////////////////////////////////////
            if(oldMode == ScriptExecutionMode.LEGACY && newMode == ScriptExecutionMode.LEGACY && (newRaw == null || newRaw.toString().isBlank()))
            {
               record.setValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME, oldRaw);
               continue;
            }

            boolean rawChanged = !Objects.equals(oldRaw, newRaw);
            if(rawChanged && (!authority || oldMode == ScriptExecutionMode.ISOLATED && newMode != ScriptExecutionMode.ISOLATED))
            {
               throw (new QPermissionDeniedException("A script's execution mode is server-controlled, and cannot be changed by this operation."));
            }
         }

         if(hasRevision && !authority && oldMode == ScriptExecutionMode.ISOLATED
            && !Objects.equals(record.getValueInteger(FIELD_CURRENT_REVISION_ID), oldRecord.getValueInteger(FIELD_CURRENT_REVISION_ID)))
         {
            throw (new QPermissionDeniedException("The current revision of an isolated script cannot be changed by this operation."));
         }
      }

      return (records);
   }



   /*******************************************************************************
    ** An ISOLATED script cannot be deleted through a generic delete (or by anyone):
    ** deleting it would orphan its revisions, whose owner could then no longer be
    ** resolved - and, were the id re-used, they would be adopted by a LEGACY script.
    *******************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      for(QRecord record : records)
      {
         Integer id = record.getValueInteger("id");
         if(id == null)
         {
            continue;
         }

         QRecord stored = ScriptExecutionIsolation.getScriptRecordAsSystem(id, deleteInput.getTransaction());
         if(stored != null && ScriptExecutionMode.fromValue(stored.getValue(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME)) == ScriptExecutionMode.ISOLATED)
         {
            throw (new QPermissionDeniedException("An isolated script cannot be deleted."));
         }
      }

      return (records);
   }



   /*******************************************************************************
    ** Find the stored version of the record being updated - from the old records
    ** the update action supplied (if any), else by reading it as the system user.
    *******************************************************************************/
   private QRecord findOldRecord(UpdateInput updateInput, QRecord record, Optional<List<QRecord>> oldRecordList) throws QException
   {
      Integer id = record.getValueInteger("id");
      if(id == null)
      {
         return (null);
      }

      if(oldRecordList != null && oldRecordList.isPresent())
      {
         for(QRecord oldRecord : oldRecordList.get())
         {
            if(Objects.equals(id, oldRecord.getValueInteger("id")))
            {
               return (oldRecord);
            }
         }
      }

      return (ScriptExecutionIsolation.getScriptRecordAsSystem(id, updateInput.getTransaction()));
   }

}
