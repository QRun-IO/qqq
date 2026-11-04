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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptExecutionMode;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.StoreScriptRevisionProcessStep;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.TestScriptProcessStep;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the Script execution-isolation marker - ScriptExecutionIsolation,
 ** ScriptTableCustomizer, and the legacy paths that must refuse marked scripts.
 **
 ** No hostile script is run here.  Where a marked script must be refused, the
 ** "engine" is a counting java function - so a test can prove the refusal came
 ** before the engine boundary (the function never ran).
 *******************************************************************************/
class ScriptExecutionIsolationTest extends BaseTest
{
   private Integer scriptTypeId;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      CountingFunction.COUNT.set(0);

      QInstance qInstance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(ScriptType.TABLE_NAME);
      insertInput.setRecords(List.of(new ScriptType().withName("TestScriptType").withTestScriptInterfaceName(RecordScriptTestInterface.class.getName()).toQRecord()));
      scriptTypeId = new InsertAction().execute(insertInput).getRecords().get(0).getValueInteger("id");

      TestUtils.insertDefaultShapes(qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExecutionModeFromValue()
   {
      assertEquals(ScriptExecutionMode.LEGACY, ScriptExecutionMode.fromValue(null));
      assertEquals(ScriptExecutionMode.LEGACY, ScriptExecutionMode.fromValue(""));
      assertEquals(ScriptExecutionMode.LEGACY, ScriptExecutionMode.fromValue("LEGACY"));
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionMode.fromValue("ISOLATED"));

      /////////////////////////////////////////////////////////////////////////////////
      // anything unrecognized (including a differently-cased legacy) fails closed //
      /////////////////////////////////////////////////////////////////////////////////
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionMode.fromValue("legacy"));
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionMode.fromValue("SOMETHING_ELSE"));
   }



   /*******************************************************************************
    ** existing (unmarked) scripts are legacy, and keep working.
    *******************************************************************************/
   @Test
   void testUnmarkedScriptIsLegacy() throws QException
   {
      Integer scriptId = insertLegacyScript();
      assertNull(getScript(scriptId).getValue("executionMode"));
      assertEquals(ScriptExecutionMode.LEGACY, ScriptExecutionIsolation.getExecutionMode(scriptId, null));
      ScriptExecutionIsolation.requireLegacyExecutionAllowed(scriptId);

      ///////////////////////////////////////////////////////////////////////////////////////
      // a null id has no project to be marked - so is fine.  an id that doesn't resolve   //
      // is NOT fine (fail closed)                                                         //
      ///////////////////////////////////////////////////////////////////////////////////////
      ScriptExecutionIsolation.requireLegacyExecutionAllowed((Integer) null);
      assertThatThrownBy(() -> ScriptExecutionIsolation.requireLegacyExecutionAllowed(-999)).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** deleting an isolated script (which would orphan its revisions) is refused;
    ** deleting a legacy one is not.
    *******************************************************************************/
   @Test
   void testIsolatedScriptCannotBeDeleted() throws QException
   {
      Integer isolatedId = insertIsolatedScript();
      Integer legacyId   = insertLegacyScript();

      assertThatThrownBy(() -> deleteScript(isolatedId)).isInstanceOf(QPermissionDeniedException.class);
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionIsolation.getExecutionMode(isolatedId, null));

      deleteScript(legacyId);
      assertNull(getScript(legacyId));
   }



   /*******************************************************************************
    ** a revision whose script row is gone (deleted behind our back) is an orphan:
    ** it must not run as legacy - nor must any id that can't be resolved.  The
    ** counting function proves the engine is never reached.
    *******************************************************************************/
   @Test
   void testOrphanedAndUnresolvableIdentitiesFailClosed() throws QException
   {
      Integer isolatedId       = insertIsolatedScript();
      Integer isolatedRevision = insertRevision(isolatedId, 1);
      removeScriptRowDirectly(isolatedId);

      QCodeReference counting = new QCodeReference(CountingFunction.class);

      /////////////////////////////////////////////////////////////////////////////
      // orphan revision: by revision id, with a lying (or no) script id         //
      /////////////////////////////////////////////////////////////////////////////
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptRevisionId(isolatedRevision));
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(isolatedId));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptRevisionId(isolatedRevision)));

      ////////////////////////////////////////////////////////////////////////////////////////
      // ids that never existed (or aren't visible, e.g., uncommitted in another tx): the   //
      // foreign ids fall through to nothing                                                //
      ////////////////////////////////////////////////////////////////////////////////////////
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(-999));
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptRevisionId(-999));
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(insertLegacyScript()).withScriptRevisionId(-999));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptRevisionRecord(new ScriptRevision().withScriptId(-999).toQRecord())));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptRevisionRecord(new ScriptRevision().withId(-999).withScriptId(insertLegacyScript()).toQRecord())));

      assertEquals(0, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** a script re-inserted under the id of a deleted isolated project (whose
    ** revisions remain) is refused - so those revisions can't be adopted as legacy.
    *******************************************************************************/
   @Test
   void testExplicitIdReinsertionRefused() throws QException
   {
      Integer isolatedId       = insertIsolatedScript();
      Integer isolatedRevision = insertRevision(isolatedId, 1);
      removeScriptRowDirectly(isolatedId);

      assertThatThrownBy(() -> insertGeneric(new Script().withId(isolatedId).withName("squatter").withScriptTypeId(scriptTypeId).toQRecord()))
         .isInstanceOf(QPermissionDeniedException.class);
      assertNull(getScript(isolatedId));

      assertRefused(new ExecuteCodeInput().withCodeReference(new QCodeReference(CountingFunction.class)).withScriptRevisionId(isolatedRevision));
      assertEquals(0, CountingFunction.COUNT.get());

      /////////////////////////////////////////////////////////////////////
      // an explicit id that nothing references is still fine            //
      /////////////////////////////////////////////////////////////////////
      insertGeneric(new Script().withId(4242).withName("fine").withScriptTypeId(scriptTypeId).toQRecord());
      assertEquals(ScriptExecutionMode.LEGACY, ScriptExecutionIsolation.getExecutionMode(4242, null));
   }



   /*******************************************************************************
    ** with no instance in the context a supplied id can't be resolved (refused);
    ** with no stored id at all, the standalone trusted execution still runs.
    *******************************************************************************/
   @Test
   void testNoInstanceInContext() throws QException
   {
      QContext.clear();

      QCodeReference counting = new QCodeReference(CountingFunction.class);
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(1));
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptRevisionId(1));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptId(1)));
      assertEquals(0, CountingFunction.COUNT.get());

      new ExecuteCodeAction().run(new ExecuteCodeInput().withCodeReference(counting), new ExecuteCodeOutput());
      assertEquals(1, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** an instance whose script table has no executionMode field can't report the
    ** marker - so a supplied id is refused, rather than assumed legacy.
    *******************************************************************************/
   @Test
   void testScriptTableWithoutMarkerFieldFailsClosed() throws QException
   {
      Integer legacyId = insertLegacyScript();
      QContext.getQInstance().getTable(Script.TABLE_NAME).getFields().remove(ScriptExecutionIsolation.EXECUTION_MODE_FIELD_NAME);

      QCodeReference counting = new QCodeReference(CountingFunction.class);
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(legacyId));
      assertEquals(0, CountingFunction.COUNT.get());

      new ExecuteCodeAction().run(new ExecuteCodeInput().withCodeReference(counting), new ExecuteCodeOutput());
      assertEquals(1, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** an id that is not visible to the caller's own transaction (e.g., a script
    ** inserted but uncommitted elsewhere) can't be resolved either - denied.  (The
    ** memory backend has no transaction isolation, so "not visible" is modelled by
    ** an id with no stored row; the code path is the same single resolve-or-deny.)
    *******************************************************************************/
   @Test
   void testIdNotVisibleToTransactionIsDenied() throws QException
   {
      try(QBackendTransaction transaction = new QBackendTransaction())
      {
         assertThatThrownBy(() -> ScriptExecutionIsolation.requireLegacyExecutionAllowed(98765, transaction)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> ScriptExecutionIsolation.getExecutionMode(98765, transaction)).isInstanceOf(QPermissionDeniedException.class);
      }
   }



   /*******************************************************************************
    ** the script-origin scope belongs to its thread, and close is idempotent.
    *******************************************************************************/
   @Test
   void testScriptOriginScopeOwnerAndIdempotentClose() throws Exception
   {
      ScriptExecutionIsolation.ScriptOriginScope outer = ScriptExecutionIsolation.openScriptOriginScope();
      ScriptExecutionIsolation.ScriptOriginScope inner = ScriptExecutionIsolation.openScriptOriginScope();

      /////////////////////////////////////////////////////////////////////////////
      // another thread can't close it - and it stays open                       //
      /////////////////////////////////////////////////////////////////////////////
      Throwable[] thrown = new Throwable[1];
      Thread      other  = new Thread(() ->
      {
         try
         {
            inner.close();
         }
         catch(Throwable t)
         {
            thrown[0] = t;
         }
      });
      other.start();
      other.join();
      assertThat(thrown[0]).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> ScriptExecutionIsolation.runAsIsolationAuthority("still in scope", () -> "nope")).isInstanceOf(IllegalStateException.class);

      ////////////////////////////////////////////////////////////////////////////////
      // closing the inner twice must not unwind the outer scope                    //
      ////////////////////////////////////////////////////////////////////////////////
      inner.close();
      inner.close();
      assertThatThrownBy(() -> ScriptExecutionIsolation.runAsIsolationAuthority("outer still open", () -> "nope")).isInstanceOf(IllegalStateException.class);

      outer.close();
      outer.close();
      assertTrue(ScriptExecutionIsolation.runAsIsolationAuthority("closed", () -> true));
   }



   /*******************************************************************************
    ** a null/blank echo of the marker on a legacy script is normalized (not a
    ** change); a raw change on an isolated script - even a blank - is forbidden.
    *******************************************************************************/
   @Test
   void testBlankMarkerEchoNormalizedOnLegacyOnly() throws QException
   {
      Integer legacyId   = insertLegacyScript();
      Integer isolatedId = insertIsolatedScript();

      updateGeneric(new QRecord().withValue("id", legacyId).withValue("name", "n1").withValue("executionMode", ""));
      assertEquals("n1", getScript(legacyId).getValueString("name"));
      assertEquals(ScriptExecutionMode.LEGACY, ScriptExecutionIsolation.getExecutionMode(legacyId, null));
      assertThat(getScript(legacyId).getValueString("executionMode")).isNullOrEmpty();

      for(Serializable raw : new Serializable[] { null, "", " ", "LEGACY" })
      {
         assertThatThrownBy(() -> updateGeneric(new QRecord().withValue("id", isolatedId).withValue("executionMode", raw))).isInstanceOf(QPermissionDeniedException.class);
      }
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionIsolation.getExecutionMode(isolatedId, null));
   }



   /*******************************************************************************
    ** the typed api creates the project marked, in the one insert.
    *******************************************************************************/
   @Test
   void testInsertIsolatedScript() throws QException
   {
      Integer scriptId = insertIsolatedScript();
      assertEquals("ISOLATED", getScript(scriptId).getValueString("executionMode"));
      assertNull(getScript(scriptId).getValue("currentScriptRevisionId"));
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionIsolation.getExecutionMode(scriptId, null));

      /////////////////////////////////////////////////////////////
      // authority is scoped to the call - and not left open     //
      /////////////////////////////////////////////////////////////
      assertFalse(ScriptExecutionIsolation.isIsolationAuthorityActive());

      /////////////////////////////////////////////////////////////////////////
      // a caller can't sneak in an active revision with a new isolated script //
      /////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("x").withScriptTypeId(scriptTypeId).withCurrentScriptRevisionId(1), null))
         .isInstanceOf(QException.class);
   }



   /*******************************************************************************
    ** a generic insert can't mark a script (the caller isn't the authority) - even
    ** with the value in any case/shape that fromValue reads as isolated.
    *******************************************************************************/
   @Test
   void testGenericInsertCannotMark()
   {
      for(String value : List.of("ISOLATED", "isolated", "SOMETHING_ELSE"))
      {
         assertThatThrownBy(() -> insertGeneric(new Script().withName("x").withScriptTypeId(scriptTypeId).withExecutionMode(value).toQRecord()))
            .isInstanceOf(QPermissionDeniedException.class);
      }
      assertThat(TestUtilsQuery.scripts()).isEmpty();
   }



   /*******************************************************************************
    ** generic updates can neither clear, nor set, nor change the marker.
    *******************************************************************************/
   @Test
   void testGenericUpdateCannotClearOrSetMarker() throws QException
   {
      Integer isolatedId = insertIsolatedScript();
      Integer legacyId   = insertLegacyScript();

      for(Serializable clearValue : new Serializable[] { null, "", "LEGACY", "legacy" })
      {
         assertThatThrownBy(() -> updateGeneric(new QRecord().withValue("id", isolatedId).withValue("executionMode", clearValue)))
            .isInstanceOf(QPermissionDeniedException.class);
      }
      assertEquals("ISOLATED", getScript(isolatedId).getValueString("executionMode"));

      assertThatThrownBy(() -> updateGeneric(new QRecord().withValue("id", legacyId).withValue("executionMode", "ISOLATED")))
         .isInstanceOf(QPermissionDeniedException.class);
      assertNull(getScript(legacyId).getValue("executionMode"));

      ////////////////////////////////////////////////////////////////////////////////////////
      // but a normal edit - even one that echoes back the stored marker - is just fine //
      ////////////////////////////////////////////////////////////////////////////////////////
      updateGeneric(new QRecord().withValue("id", isolatedId).withValue("name", "renamed").withValue("executionMode", "ISOLATED"));
      assertEquals("renamed", getScript(isolatedId).getValueString("name"));
      updateGeneric(new QRecord().withValue("id", legacyId).withValue("name", "renamed").withValue("executionMode", null));
      assertEquals("renamed", getScript(legacyId).getValueString("name"));
   }



   /*******************************************************************************
    ** the customizer must not trust the caller-visible old-record list: if the
    ** stored script is absent from it (e.g., hidden from the caller by a lock or
    ** permission), the stored marker is still read, as the system user, and an
    ** isolated script is still protected.  Only a script that is not stored at all
    ** is passed through.
    *******************************************************************************/
   @Test
   void testInvisibleOldRecordStillProtectsIsolatedScript() throws QException
   {
      Integer isolatedId = insertIsolatedScript();
      Integer legacyId   = insertLegacyScript();
      Integer revisionId = insertRevision(isolatedId, 1);

      UpdateInput updateInput = new UpdateInput(Script.TABLE_NAME);
      ScriptTableCustomizer customizer = new ScriptTableCustomizer();

      for(Optional<List<QRecord>> hiddenOldRecords : List.of(Optional.<List<QRecord>>empty(), Optional.of(List.<QRecord>of()), Optional.of(List.of(new QRecord().withValue("id", legacyId)))))
      {
         assertThatThrownBy(() -> customizer.preUpdate(updateInput, List.of(new QRecord().withValue("id", isolatedId).withValue("executionMode", "LEGACY")), false, hiddenOldRecords))
            .isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> customizer.preUpdate(updateInput, List.of(new QRecord().withValue("id", isolatedId).withValue("currentScriptRevisionId", revisionId)), false, hiddenOldRecords))
            .isInstanceOf(QPermissionDeniedException.class);
      }

      //////////////////////////////////////////////////////////////////////////////////
      // a legacy script with a hidden old-record list is still just a legacy script  //
      //////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> customizer.preUpdate(updateInput, List.of(new QRecord().withValue("id", legacyId).withValue("executionMode", "ISOLATED")), false, Optional.empty()))
         .isInstanceOf(QPermissionDeniedException.class);

      /////////////////////////////////////////////////////////////////////////////
      // a script that is not stored at all has nothing to protect               //
      /////////////////////////////////////////////////////////////////////////////
      assertEquals(1, customizer.preUpdate(updateInput, List.of(new QRecord().withValue("id", 987654).withValue("executionMode", "LEGACY")), false, Optional.empty()).size());
      assertEquals("ISOLATED", getScript(isolatedId).getValueString("executionMode"));
   }



   /*******************************************************************************
    ** generic updates can't activate a revision of an isolated script - but can
    ** (as ever) for a legacy one.
    *******************************************************************************/
   @Test
   void testGenericUpdateCannotActivateIsolatedRevision() throws QException
   {
      Integer isolatedId       = insertIsolatedScript();
      Integer isolatedRevision = insertRevision(isolatedId, 1);
      Integer legacyId         = insertLegacyScript();
      Integer legacyRevision   = insertRevision(legacyId, 1);

      assertThatThrownBy(() -> updateGeneric(new QRecord().withValue("id", isolatedId).withValue("currentScriptRevisionId", isolatedRevision)))
         .isInstanceOf(QPermissionDeniedException.class);
      assertNull(getScript(isolatedId).getValue("currentScriptRevisionId"));

      updateGeneric(new QRecord().withValue("id", legacyId).withValue("currentScriptRevisionId", legacyRevision));
      assertEquals(legacyRevision, getScript(legacyId).getValueInteger("currentScriptRevisionId"));

      //////////////////////////////////////////////////////////////////////////////////////
      // only the typed api (the authority) can activate - and only the script's own revision //
      //////////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> ScriptExecutionIsolation.activateIsolatedRevision(isolatedId, legacyRevision, null)).isInstanceOf(QException.class);
      assertThatThrownBy(() -> ScriptExecutionIsolation.activateIsolatedRevision(legacyId, legacyRevision, null)).isInstanceOf(QException.class);
      assertNull(getScript(isolatedId).getValue("currentScriptRevisionId"));

      ScriptExecutionIsolation.activateIsolatedRevision(isolatedId, isolatedRevision, null);
      assertEquals(isolatedRevision, getScript(isolatedId).getValueInteger("currentScriptRevisionId"));
      assertEquals("ISOLATED", getScript(isolatedId).getValueString("executionMode"));

      ///////////////////////////////////////////////////////////////////////////////////
      // and now that there is a current revision, generic writes still can't move it //
      ///////////////////////////////////////////////////////////////////////////////////
      Integer secondRevision = insertRevision(isolatedId, 2);
      assertThatThrownBy(() -> updateGeneric(new QRecord().withValue("id", isolatedId).withValue("currentScriptRevisionId", secondRevision)))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> updateGeneric(new QRecord().withValue("id", isolatedId).withValue("currentScriptRevisionId", null)))
         .isInstanceOf(QPermissionDeniedException.class);
      assertEquals(isolatedRevision, getScript(isolatedId).getValueInteger("currentScriptRevisionId"));
   }



   /*******************************************************************************
    ** isolated is forever: not even the authority can take the marker off - though
    ** it can mark a legacy script.
    *******************************************************************************/
   @Test
   void testMarkerIsMonotonic() throws QException
   {
      Integer isolatedId = insertIsolatedScript();
      Integer legacyId   = insertLegacyScript();

      assertThatThrownBy(() -> ScriptExecutionIsolation.runAsIsolationAuthority("test downgrade", () ->
         updateGeneric(new QRecord().withValue("id", isolatedId).withValue("executionMode", "LEGACY"))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> ScriptExecutionIsolation.runAsIsolationAuthority("test downgrade", () ->
         updateGeneric(new QRecord().withValue("id", isolatedId).withValue("executionMode", null))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertEquals("ISOLATED", getScript(isolatedId).getValueString("executionMode"));

      ScriptExecutionIsolation.runAsIsolationAuthority("test upgrade", () -> updateGeneric(new QRecord().withValue("id", legacyId).withValue("executionMode", "ISOLATED")));
      assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionIsolation.getExecutionMode(legacyId, null));
   }



   /*******************************************************************************
    ** script-origin code (a legacy script's thread, or an isolated engine's host
    ** bridge) can never be the authority; and the authority needs a reason.
    *******************************************************************************/
   @Test
   void testAuthorityRefusedFromScriptOrigin()
   {
      try(ScriptExecutionIsolation.ScriptOriginScope ignored = ScriptExecutionIsolation.openScriptOriginScope())
      {
         assertThatThrownBy(() -> ScriptExecutionIsolation.runAsIsolationAuthority("from a script", () -> "nope")).isInstanceOf(IllegalStateException.class);
         assertThatThrownBy(() -> ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("x").withScriptTypeId(scriptTypeId), null)).isInstanceOf(IllegalStateException.class);
      }

      assertThatThrownBy(() -> ScriptExecutionIsolation.runAsIsolationAuthority(" ", () -> "nope")).isInstanceOf(IllegalArgumentException.class);
      assertThat(TestUtilsQuery.scripts()).isEmpty();

      /////////////////////////////////////////////////////////////////////////////////////
      // the scope nests, and then the authority is available again - and only inside it //
      /////////////////////////////////////////////////////////////////////////////////////
      assertFalse(ScriptExecutionIsolation.isIsolationAuthorityActive());
      assertTrue(ScriptExecutionIsolation.runAsIsolationAuthority("check", ScriptExecutionIsolation::isIsolationAuthorityActive));
      assertFalse(ScriptExecutionIsolation.isIsolationAuthorityActive());
   }



   /*******************************************************************************
    ** the one door - ExecuteCodeAction.run - refuses a marked script however the
    ** caller names it, and before the engine; and still runs everything else.
    *******************************************************************************/
   @Test
   void testExecuteCodeActionRefusesMarkedScript() throws QException
   {
      Integer isolatedId       = insertIsolatedScript();
      Integer isolatedRevision = insertRevision(isolatedId, 1);
      Integer legacyId         = insertLegacyScript();
      Integer legacyRevision   = insertRevision(legacyId, 1);

      QCodeReference counting = new QCodeReference(CountingFunction.class);

      ///////////////////////////////////////////////////////////////////////////
      // by script id, or revision id, on the input (not any caller-given flag) //
      ///////////////////////////////////////////////////////////////////////////
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(isolatedId));
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptRevisionId(isolatedRevision));

      ///////////////////////////////////////////////////////////////////////////////
      // lying about the script id, but naming the isolated script's revision id   //
      ///////////////////////////////////////////////////////////////////////////////
      assertRefused(new ExecuteCodeInput().withCodeReference(counting).withScriptId(legacyId).withScriptRevisionId(isolatedRevision));

      ///////////////////////////////////////////////////////////////////////
      // by each of the ways that an ad hoc code reference can name a script //
      ///////////////////////////////////////////////////////////////////////
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptId(isolatedId)));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptRevisionId(isolatedRevision)));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptRevisionRecord(new ScriptRevision().withScriptId(isolatedId).toQRecord())));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptRevisionRecord(new ScriptRevision().withId(isolatedRevision).withScriptId(legacyId).toQRecord())));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptId(legacyId).withScriptRevisionId(isolatedRevision)));
      assertRefused(adHoc(new AdHocScriptCodeReference().withScriptId(legacyId).withScriptRevisionRecord(new ScriptRevision().withScriptId(isolatedId).toQRecord())));

      ///////////////////////////////////////////////////////////////////////////////////
      // and the engine boundary was never reached by any of those                     //
      ///////////////////////////////////////////////////////////////////////////////////
      assertEquals(0, CountingFunction.COUNT.get());

      ///////////////////////////////////////////////////////////////////
      // legitimate legacy calls - trusted java code, legacy scripts   //
      ///////////////////////////////////////////////////////////////////
      new ExecuteCodeAction().run(new ExecuteCodeInput().withCodeReference(counting), new ExecuteCodeOutput());
      new ExecuteCodeAction().run(new ExecuteCodeInput().withCodeReference(counting).withScriptId(legacyId).withScriptRevisionId(legacyRevision), new ExecuteCodeOutput());
      assertEquals(2, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** the setup that builds execute input from a stored script revision refuses
    ** a marked one - before loading any of its files.
    *******************************************************************************/
   @Test
   void testSetupExecuteCodeInputRefusesMarkedScript() throws QException
   {
      Integer isolatedId       = insertIsolatedScript();
      Integer isolatedRevision = insertRevision(isolatedId, 1);
      Integer legacyId         = insertLegacyScript();

      assertThatThrownBy(() -> ExecuteCodeAction.setupExecuteCodeInput(new RunAdHocRecordScriptInput(), new ScriptRevision().withId(isolatedRevision).withScriptId(isolatedId)))
         .isInstanceOf(QPermissionDeniedException.class);

      /////////////////////////////////////////////////////////////////////////////////
      // a revision record that lies about its script is caught by its revision id   //
      /////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> ExecuteCodeAction.setupExecuteCodeInput(new RunAdHocRecordScriptInput(), new ScriptRevision().withId(isolatedRevision).withScriptId(legacyId)))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** the legacy testScript process refuses a marked script (however the source
    ** is given) before the engine; and still tests a legacy script, to the engine.
    *******************************************************************************/
   @Test
   void testTestScriptProcessStep() throws QException
   {
      Integer isolatedId = insertIsolatedScript();
      Integer legacyId   = insertLegacyScript();

      RunBackendStepOutput output = runTestScript(isolatedId);
      assertThat((Exception) output.getValue("exception")).isInstanceOf(QPermissionDeniedException.class);

      /////////////////////////////////////////////////////////////////////////////////////
      // a legacy script gets all the way to the engine - which (no javascript module on //
      // the classpath in core) is the class-not-found the existing step test expects    //
      /////////////////////////////////////////////////////////////////////////////////////
      output = runTestScript(legacyId);
      assertThat((Exception) output.getValue("exception")).hasRootCauseInstanceOf(ClassNotFoundException.class);
   }



   /*******************************************************************************
    ** the legacy storeScriptRevision process refuses a marked script, and so can't
    ** add a revision or activate one.
    *******************************************************************************/
   @Test
   void testStoreScriptRevisionProcessStep() throws QException
   {
      Integer isolatedId = insertIsolatedScript();
      Integer legacyId   = insertLegacyScript();

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("scriptId", isolatedId);
      input.addValue("fileNames", "script");
      input.addValue("fileContents:script", "not run - just stored");
      assertThatThrownBy(() -> new StoreScriptRevisionProcessStep().run(input, new RunBackendStepOutput())).isInstanceOf(QPermissionDeniedException.class);

      assertThat(TestUtilsQuery.revisions()).isEmpty();
      assertNull(getScript(isolatedId).getValue("currentScriptRevisionId"));

      input.addValue("scriptId", legacyId);
      new StoreScriptRevisionProcessStep().run(input, new RunBackendStepOutput());
      assertThat(TestUtilsQuery.revisions()).hasSize(1);
      assertEquals(1, getScript(legacyId).getValueInteger("currentScriptRevisionId"));
   }



   /*******************************************************************************
    ** the legacy routes are explicitly permissioned - never the host's default,
    ** not-protected.
    *******************************************************************************/
   @Test
   void testLegacyProcessesRequirePermission() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      for(String processName : List.of(ScriptsMetaDataProvider.TEST_SCRIPT_PROCESS_NAME, ScriptsMetaDataProvider.STORE_SCRIPT_REVISION_PROCESS_NAME))
      {
         assertEquals(PermissionLevel.HAS_ACCESS_PERMISSION, qInstance.getProcess(processName).getPermissionRules().getLevel());

         RunProcessInput processInput = new RunProcessInput().withProcessName(processName);
         QContext.setQSession(new QSession());
         assertFalse(PermissionsHelper.hasProcessPermission(processInput, processName));
         assertThatThrownBy(() -> PermissionsHelper.checkProcessPermissionThrowing(processInput, processName)).isInstanceOf(QPermissionDeniedException.class);

         QContext.setQSession(new QSession().withPermissions(processName + ".hasAccess"));
         assertTrue(PermissionsHelper.hasProcessPermission(processInput, processName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRefused(ExecuteCodeInput input)
   {
      assertThatThrownBy(() -> new ExecuteCodeAction().run(input, new ExecuteCodeOutput())).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** build an input with an ad hoc reference that would (if it weren't refused)
    ** run the counting function.
    *******************************************************************************/
   private ExecuteCodeInput adHoc(AdHocScriptCodeReference reference)
   {
      reference.setName(CountingFunction.class.getName());
      reference.setCodeType(QCodeType.JAVA);
      return (new ExecuteCodeInput().withCodeReference(reference));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepOutput runTestScript(Integer scriptId) throws QException
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("scriptId", scriptId);
      input.addValue("recordPrimaryKeyList", "1");
      input.addValue("fileNames", "script.js");
      input.addValue("fileContents:script.js", "inert text - never evaluated");

      RunBackendStepOutput output = new RunBackendStepOutput();
      new TestScriptProcessStep().run(input, output);
      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertLegacyScript() throws QException
   {
      return (insertGeneric(new Script().withName("legacy").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_SHAPE).toQRecord()).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertIsolatedScript() throws QException
   {
      return (ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("isolated").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_SHAPE), null).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertRevision(Integer scriptId, int sequenceNo) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(ScriptRevision.TABLE_NAME);
      insertInput.setRecords(List.of(new ScriptRevision().withScriptId(scriptId).withSequenceNo(sequenceNo).toQRecord()));
      return (new InsertAction().execute(insertInput).getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertGeneric(QRecord record) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(Script.TABLE_NAME);
      insertInput.setRecords(List.of(record));
      return (new InsertAction().execute(insertInput).getRecords().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Object updateGeneric(QRecord record) throws QException
   {
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(Script.TABLE_NAME);
      updateInput.setRecords(List.of(record));
      new UpdateAction().execute(updateInput);
      return (null);
   }



   /*******************************************************************************
    ** generic delete of a script, throwing if it is refused.
    *******************************************************************************/
   private void deleteScript(Integer scriptId) throws QException
   {
      DeleteInput deleteInput = new DeleteInput(Script.TABLE_NAME).withPrimaryKeys(List.of(scriptId));
      DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
      for(QRecord record : deleteOutput.getRecordsWithErrors())
      {
         throw (new QPermissionDeniedException(record.getErrors().get(0).getMessage()));
      }
   }



   /*******************************************************************************
    ** delete the stored script row straight out of the backend - modelling an
    ** out-of-band delete (sql, an older build) that leaves its revisions orphaned.
    *******************************************************************************/
   private void removeScriptRowDirectly(Integer scriptId) throws QException
   {
      MemoryRecordStore.getInstance().delete(new DeleteInput(Script.TABLE_NAME).withPrimaryKeys(List.of(scriptId)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord getScript(Integer scriptId) throws QException
   {
      return (new GetAction().executeForRecord(new GetInput(Script.TABLE_NAME).withPrimaryKey(scriptId)));
   }



   /*******************************************************************************
    ** query helpers that don't throw checked exceptions (for use in assertions)
    *******************************************************************************/
   private static class TestUtilsQuery
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      static List<QRecord> scripts()
      {
         try
         {
            return (TestUtils.queryTable(Script.TABLE_NAME));
         }
         catch(QException e)
         {
            throw (new IllegalStateException(e));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      static List<QRecord> revisions()
      {
         try
         {
            return (TestUtils.queryTable(ScriptRevision.TABLE_NAME));
         }
         catch(QException e)
         {
            throw (new IllegalStateException(e));
         }
      }
   }



   /*******************************************************************************
    ** stands in for an engine: counts the times it is (wrongly or rightly) reached.
    *******************************************************************************/
   public static class CountingFunction implements Function<Map<String, Object>, Serializable>
   {
      static final AtomicInteger COUNT = new AtomicInteger(0);



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable apply(Map<String, Object> context)
      {
         return (COUNT.incrementAndGet());
      }
   }

}
