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
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptExecutionMode;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Every native write entry point that accepts the restore flag - insert with
 ** explicit ids, replace (insert, update and delete halves), and delete - keeps the
 ** execution marker guard, with the existing isolation authority as the only way to
 ** write an isolated script. The bulk-load and ETL load steps write through
 ** InsertAction/UpdateAction, which is where the guard sits.
 *******************************************************************************/
class ScriptRestoreEntryPointCoverageTest extends BaseTest
{
   private Integer scriptTypeId;



   /*******************************************************************************
    ** Script table and type; the session may restore.
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
    ** Stored marker, read as the system user.
    *******************************************************************************/
   private ScriptExecutionMode storedMode(Integer id) throws QException
   {
      QRecord stored = ScriptExecutionIsolation.getScriptRecordAsSystem(id, null);
      return (stored == null ? null : ScriptExecutionMode.fromValue(stored.getValue("executionMode")));
   }



   /*******************************************************************************
    ** An isolated script made through the authority API.
    *******************************************************************************/
   private Integer isolated() throws QException
   {
      return (ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("iso").withScriptTypeId(scriptTypeId), null).getValueInteger("id"));
   }



   /*******************************************************************************
    ** A replace of script rows.
    *******************************************************************************/
   private ReplaceInput replaceInput()
   {
      ReplaceInput input = new ReplaceInput();
      input.setTableName(Script.TABLE_NAME);
      return (input);
   }



   /*******************************************************************************
    ** A flagged replace on an existing isolated script cannot downgrade it.
    *******************************************************************************/
   @Test
   void replaceCannotDowngradeIsolatedScript() throws QException
   {
      Integer id = isolated();
      for(String mode : new String[] { null, "", "LEGACY" })
      {
         try
         {
            new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("name")).withPerformDeletes(false).withFlag(new RestoreWriteFlag())
               .withRecords(List.of(new Script().withName("iso").withScriptTypeId(scriptTypeId).withExecutionMode(mode).toQRecord())));
         }
         catch(QException expected)
         {
            // refusing is correct
         }
         assertEquals(ScriptExecutionMode.ISOLATED, storedMode(id), "replace must not downgrade (mode=" + mode + ")");
      }
   }



   /*******************************************************************************
    ** A flagged replace, or insert, cannot create an isolated script with an
    ** explicit id outside the authority.
    *******************************************************************************/
   @Test
   void explicitIdIsolatedInsertOutsideAuthorityIsRefused() throws QException
   {
      try
      {
         new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("name")).withPerformDeletes(false).withFlag(new RestoreWriteFlag())
            .withRecords(List.of(new Script().withId(9001).withName("viaReplace").withScriptTypeId(scriptTypeId).withExecutionMode("ISOLATED").toQRecord())));
      }
      catch(QException expected)
      {
         // refusing is correct
      }
      assertNull(storedMode(9001), "replace must not create an isolated script outside the authority");

      try
      {
         new InsertAction().execute(new InsertInput(Script.TABLE_NAME).withFlag(new RestoreWriteFlag())
            .withRecord(new Script().withId(9002).withName("viaInsert").withScriptTypeId(scriptTypeId).withExecutionMode("ISOLATED").toQRecord()));
      }
      catch(QException expected)
      {
         // refusing is correct
      }
      assertNull(storedMode(9002), "insert must not create an isolated script outside the authority");
   }



   /*******************************************************************************
    ** The delete half of a flagged replace, and a flagged delete, never remove an
    ** isolated script (so there is no delete-then-recreate route around the marker).
    *******************************************************************************/
   @Test
   void deleteNeverRemovesIsolatedScriptEvenWithTheFlag() throws QException
   {
      Integer id = isolated();
      try
      {
         new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("name")).withPerformDeletes(true).withFlag(new RestoreWriteFlag())
            .withRecords(List.of(new Script().withName("other").withScriptTypeId(scriptTypeId).toQRecord())));
      }
      catch(QException expected)
      {
         // refusing is correct
      }
      assertEquals(ScriptExecutionMode.ISOLATED, storedMode(id), "the delete half of a replace must not remove an isolated script");

      try
      {
         new DeleteAction().execute(new DeleteInput(Script.TABLE_NAME).withPrimaryKey(id).withFlag(new RestoreWriteFlag()));
      }
      catch(QException expected)
      {
         // refusing is correct
      }
      assertEquals(ScriptExecutionMode.ISOLATED, storedMode(id), "a flagged delete must not remove an isolated script");
      assertEquals(1, new QueryAction().execute(new QueryInput(Script.TABLE_NAME).withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id)))).getRecords().size());
   }



   /*******************************************************************************
    ** Positive controls: under the authority a flagged replace restores an isolated
    ** script with its explicit id; legacy rows replace and delete as before.
    *******************************************************************************/
   @Test
   void authorityStillRestoresAndLegacyStillWorks() throws QException
   {
      ScriptExecutionIsolation.runAsIsolationAuthority("restore test", () -> new ReplaceAction().execute(replaceInput()
         .withKey(new UniqueKey("name")).withPerformDeletes(false).withFlag(new RestoreWriteFlag())
         .withRecords(List.of(new Script().withId(9003).withName("restored").withScriptTypeId(scriptTypeId).withExecutionMode("ISOLATED").withCurrentScriptRevisionId(5).toQRecord()))));
      assertEquals(ScriptExecutionMode.ISOLATED, storedMode(9003));
      assertEquals(5, ScriptExecutionIsolation.getScriptRecordAsSystem(9003, null).getValueInteger("currentScriptRevisionId"));

      Integer legacy = new InsertAction().execute(new InsertInput(Script.TABLE_NAME).withRecord(new Script().withName("leg").withScriptTypeId(scriptTypeId).toQRecord())).getRecords().get(0).getValueInteger("id");
      new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("name")).withPerformDeletes(false).withFlag(new RestoreWriteFlag())
         .withRecords(List.of(new Script().withName("leg").withScriptTypeId(scriptTypeId).withMaxBatchSize(7).toQRecord())));
      assertNotNull(ScriptExecutionIsolation.getScriptRecordAsSystem(legacy, null));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(Script.TABLE_NAME).withPrimaryKey(legacy).withFlag(new RestoreWriteFlag())).getDeletedRecordCount());
   }
}
