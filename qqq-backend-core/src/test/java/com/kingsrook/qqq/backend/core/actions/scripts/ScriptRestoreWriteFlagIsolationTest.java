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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptExecutionMode;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A restore-mode write (RestoreWriteFlag, permission backups.restore) skips table
 ** customizers by design. It must not be a way to move the server-controlled
 ** script execution marker: ISOLATED never returns to LEGACY, and a script cannot
 ** be created ISOLATED outside the isolation authority.
 *******************************************************************************/
class ScriptRestoreWriteFlagIsolationTest extends BaseTest
{
   private Integer scriptTypeId;



   /*******************************************************************************
    ** Script table and script type, as in the existing isolation tests.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      new ScriptsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      scriptTypeId = new InsertAction().execute(new InsertInput(ScriptType.TABLE_NAME).withRecord(new ScriptType().withName("T").toQRecord())).getRecords().get(0).getValueInteger("id");
      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      QContext.setQSession(new QSession().withPermission(RestoreWriteFlag.REQUIRED_PERMISSION));
   }



   /*******************************************************************************
    ** Read the stored marker as the system user.
    *******************************************************************************/
   private ScriptExecutionMode storedMode(Integer id) throws QException
   {
      return (ScriptExecutionMode.fromValue(ScriptExecutionIsolation.getScriptRecordAsSystem(id, null).getValue("executionMode")));
   }



   /*******************************************************************************
    ** An isolated script created through the authority API.
    *******************************************************************************/
   private Integer isolated() throws QException
   {
      return (ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("iso").withScriptTypeId(scriptTypeId), null).getValueInteger("id"));
   }



   /*******************************************************************************
    ** The marker cannot be cleared or changed by a restore-mode update.
    *******************************************************************************/
   @Test
   void restoreWriteCannotMoveIsolatedMarkerBackToLegacy() throws QException
   {
      Integer id = isolated();
      for(String value : new String[] { null, "", "LEGACY" })
      {
         try
         {
            new UpdateAction().execute(new UpdateInput(Script.TABLE_NAME).withRecord(new QRecord().withValue("id", id).withValue("executionMode", value)).withFlag(new RestoreWriteFlag()));
         }
         catch(QException expected)
         {
            // refusing is correct
         }
         assertEquals(ScriptExecutionMode.ISOLATED, storedMode(id), "restore-mode write must not downgrade an isolated script (value=" + value + ")");
      }
   }



   /*******************************************************************************
    ** A restore-mode insert cannot create a script that is ISOLATED outside the authority.
    *******************************************************************************/
   @Test
   void restoreWriteCannotInsertIsolatedScriptOutsideAuthority() throws QException
   {
      List<QRecord> inserted = List.of();
      try
      {
         inserted = new InsertAction().execute(new InsertInput(Script.TABLE_NAME).withFlag(new RestoreWriteFlag())
            .withRecord(new Script().withName("sneaky").withScriptTypeId(scriptTypeId).withExecutionMode("ISOLATED").toQRecord())).getRecords();
      }
      catch(QException expected)
      {
         // refusing is correct
      }
      assertTrue(inserted.stream().allMatch(record -> !CollectionUtils.nullSafeIsEmpty(record.getErrors())), "an isolated script must not be created outside the isolation authority");
   }



   /*******************************************************************************
    ** Legitimate restore-mode writes to a legacy script (no marker involved) still work.
    *******************************************************************************/
   @Test
   void restoreWriteStillUpdatesLegacyScripts() throws QException
   {
      Integer id = new InsertAction().execute(new InsertInput(Script.TABLE_NAME).withRecord(new Script().withName("leg").withScriptTypeId(scriptTypeId).toQRecord())).getRecords().get(0).getValueInteger("id");
      new UpdateAction().execute(new UpdateInput(Script.TABLE_NAME).withRecord(new QRecord().withValue("id", id).withValue("name", "renamed")).withFlag(new RestoreWriteFlag()));
      assertEquals("renamed", new GetAction().executeForRecord(new GetInput(Script.TABLE_NAME).withPrimaryKey(id)).getValueString("name"));
      assertEquals(ScriptExecutionMode.LEGACY, storedMode(id));
   }



   /*******************************************************************************
    ** The owner-managed restore path: code running inside the existing isolation
    ** authority may restore an isolated script (marker and revision pointer) with
    ** the restore flag.
    *******************************************************************************/
   @Test
   void restoreWriteUnderAuthorityCanInsertIsolatedScriptWithRevisionPointer() throws QException
   {
      QRecord inserted = ScriptExecutionIsolation.runAsIsolationAuthority("restore test", () -> new InsertAction().execute(new InsertInput(Script.TABLE_NAME).withFlag(new RestoreWriteFlag())
         .withRecord(new Script().withName("restored").withScriptTypeId(scriptTypeId).withExecutionMode("ISOLATED").withCurrentScriptRevisionId(77).toQRecord())).getRecords().get(0));
      assertTrue(CollectionUtils.nullSafeIsEmpty(inserted.getErrors()), String.valueOf(inserted.getErrors()));
      assertEquals(ScriptExecutionMode.ISOLATED, storedMode(inserted.getValueInteger("id")));
      assertEquals(77, ScriptExecutionIsolation.getScriptRecordAsSystem(inserted.getValueInteger("id"), null).getValueInteger("currentScriptRevisionId"));
   }



   /*******************************************************************************
    ** Under the authority a restore may move a legacy script to ISOLATED and move
    ** the pointer of an isolated one, but still never back to LEGACY.
    *******************************************************************************/
   @Test
   void restoreWriteUnderAuthorityMovesForwardOnly() throws QException
   {
      Integer legacyId = new InsertAction().execute(new InsertInput(Script.TABLE_NAME).withRecord(new Script().withName("leg").withScriptTypeId(scriptTypeId).toQRecord())).getRecords().get(0).getValueInteger("id");
      ScriptExecutionIsolation.runAsIsolationAuthority("restore test", () -> new UpdateAction().execute(new UpdateInput(Script.TABLE_NAME)
         .withRecord(new QRecord().withValue("id", legacyId).withValue("executionMode", "ISOLATED")).withFlag(new RestoreWriteFlag())));
      assertEquals(ScriptExecutionMode.ISOLATED, storedMode(legacyId));

      Integer id = isolated();
      ScriptExecutionIsolation.runAsIsolationAuthority("restore test", () -> new UpdateAction().execute(new UpdateInput(Script.TABLE_NAME)
         .withRecord(new QRecord().withValue("id", id).withValue("currentScriptRevisionId", 5)).withFlag(new RestoreWriteFlag())));
      assertEquals(5, ScriptExecutionIsolation.getScriptRecordAsSystem(id, null).getValueInteger("currentScriptRevisionId"));

      for(String value : new String[] { null, "", "LEGACY" })
      {
         try
         {
            ScriptExecutionIsolation.runAsIsolationAuthority("restore test", () -> new UpdateAction().execute(new UpdateInput(Script.TABLE_NAME)
               .withRecord(new QRecord().withValue("id", id).withValue("executionMode", value)).withFlag(new RestoreWriteFlag())));
         }
         catch(QException expected)
         {
            // refusing is correct
         }
         assertEquals(ScriptExecutionMode.ISOLATED, storedMode(id), "even the authority cannot downgrade (value=" + value + ")");
      }
   }



   /*******************************************************************************
    ** Outside the authority a restore cannot move the revision pointer of an isolated script.
    *******************************************************************************/
   @Test
   void restoreWriteOutsideAuthorityCannotMovePointer() throws QException
   {
      Integer id = isolated();
      try
      {
         new UpdateAction().execute(new UpdateInput(Script.TABLE_NAME).withRecord(new QRecord().withValue("id", id).withValue("currentScriptRevisionId", 9)).withFlag(new RestoreWriteFlag()));
      }
      catch(QException expected)
      {
         // refusing is correct
      }
      assertEquals(null, ScriptExecutionIsolation.getScriptRecordAsSystem(id, null).getValueInteger("currentScriptRevisionId"));
   }
}
