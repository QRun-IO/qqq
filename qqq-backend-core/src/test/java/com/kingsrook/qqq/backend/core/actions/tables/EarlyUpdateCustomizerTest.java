/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface.PreUpdatePhase;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Additive early update phase keeps native authorization and validation intact.
 *******************************************************************************/
class EarlyUpdateCustomizerTest extends BaseTest
{
   private static final String TABLE = "earlyUpdate";
   private QTableMetaData table;

   /***************************************************************************
    ** Independent memory fixture with real required/length/unique metadata.
    ***************************************************************************/
   @BeforeEach
   void prepare() throws Exception
   {
      table = new QTableMetaData().withName(TABLE).withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsRequired(true).withMaxLength(4).withBehavior(ValueTooLongBehavior.ERROR))
         .withField(new QFieldMetaData("countryId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("stateId", QFieldType.INTEGER))
         .withUniqueKey(new UniqueKey("code"));
      QContext.getQInstance().addTable(table);
      new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "one"),
         new QRecord().withValue("id", 2).withValue("code", "two"))));
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(Early.class));
      Early.patch = record -> record.setValue("code", "good");
      Early.calls = 0;
      Early.badResult = false;
      Early.control = input ->
      {
      };
      Early.priorCodes = new ArrayList<>();
      Early.preview = false;
      Late.calls = 0;
      Late.seen = new ArrayList<>();
   }

   /***************************************************************************
    ** Execute the actual user update path.
    ***************************************************************************/
   private QRecord update() throws Exception
   {
      return (new UpdateAction().execute(new UpdateInput().withTableName(TABLE).withInputSource(QInputSource.USER)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "sent")))).getRecords().getFirst());
   }

   /***************************************************************************
    ** All ordinary native validators see the patched value.
    ***************************************************************************/
   @Test
   void requiredLengthAndUniqueChecksFollowEarlyPatch() throws Exception
   {
      for(String invalid : new String[] { null, "", "too-long", "two" })
      {
         Early.patch = record -> record.setValue("code", invalid);
         assertFalse(update().getErrors().isEmpty());
      }
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
      Early.patch = record -> record.setValue("code", "good");
      assertTrue(update().getErrors().isEmpty());
      assertEquals("good", GetAction.execute(TABLE, 1).getValueString("code"));
   }

   /***************************************************************************
    ** Native field behaviors run once after early callbacks, before default hosts.
    ***************************************************************************/
   @Test
   void nativeBehaviorAndDefaultInstanceOrderingArePreserved() throws Exception
   {
      table.getField("code").setBehaviors(java.util.Set.of(ValueTooLongBehavior.TRUNCATE));
      QContext.getQInstance().withTableCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(Late.class));
      Early.patch = record -> record.setValue("code", "patched");
      assertTrue(update().getErrors().isEmpty());
      assertEquals(1, Early.calls);
      assertEquals(1, Late.calls);
      assertEquals(List.of("patc"), Late.seen);
      assertEquals("patc", GetAction.execute(TABLE, 1).getValueString("code"));
   }

   /***************************************************************************
    ** Required validation also stays before an existing late table callback.
    ***************************************************************************/
   @Test
   void defaultTableHostKeepsLatePhase() throws Exception
   {
      table.getCustomizers().put(TableCustomizers.PRE_UPDATE_RECORD.getRole(), new QCodeReference(Late.class));
      QRecord row = new UpdateAction().execute(new UpdateInput().withTableName(TABLE).withInputSource(QInputSource.USER)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "")))).getRecords().getFirst();
      assertFalse(row.getErrors().isEmpty());
      assertEquals(1, Late.calls);
      assertEquals(0, Early.calls);
   }

   /***************************************************************************
    ** Missing/denied selected records never reach the early callback.
    ***************************************************************************/
   @Test
   void deniedStoredRowsNeverReachEarlyCode() throws Exception
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(List.of(new QRecord().withValue("id", 1).withValue("storeId", 1).withValue("orderNo", "original"))));
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER).withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(Early.class));
      QContext.getQSession().withSecurityKeyValues(java.util.Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, List.of(2)));
      QRecord denied = new UpdateAction().execute(new UpdateInput().withTableName(TestUtils.TABLE_NAME_ORDER)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("orderNo", "changed")))).getRecords().getFirst();
      assertFalse(denied.getErrors().isEmpty());
      assertEquals(0, Early.calls);
   }

   /***************************************************************************
    ** Early callbacks cannot retarget a row, replace it, or discard an error row.
    ***************************************************************************/
   @Test
   void earlyCallbackCannotChangeSelectedIdentities() throws Exception
   {
      Early.patch = record -> record.setValue("id", 2);
      assertThrows(QException.class, this::update);
      Early.patch = record -> record.setValue("code", "good");
      Early.badResult = true;
      assertThrows(QException.class, this::update);
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
      assertEquals("two", GetAction.execute(TABLE, 2).getValueString("code"));
   }

   /***************************************************************************
    ** Explicit early scheduling requires an authorized stored snapshot.
    ***************************************************************************/
   @Test
   void missingPrefetchRefreshesAuthoritativeStoredRows() throws Exception
   {
      new UpdateAction().performValidations(new UpdateInput().withTableName(TABLE).withInputSource(QInputSource.USER)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "sent"))),
         Optional.of(List.of(new QRecord().withValue("id", 1).withValue("code", "FAKE"))), true);
      assertEquals(1, Early.calls);
      assertEquals(List.of("one"), Early.priorCodes);
      assertTrue(Early.preview);
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
   }


   /***************************************************************************
    ** Native contingent possible-value validation follows an early patch.
    ***************************************************************************/
   @Test
   void contingentValuesSeePatchedDependencies() throws Exception
   {
      QContext.getQInstance().addPossibleValueSource(new QPossibleValueSource().withName("earlyStates").withType(QPossibleValueSourceType.CUSTOM)
         .withCustomCodeReference(new QCodeReference(States.class)));
      table.getField("stateId").withPossibleValueSourceName("earlyStates").withPossibleValueSourceDependsOn(List.of("countryId"));
      Early.patch = record -> record.withValue("countryId", 2).withValue("stateId", 1);
      assertFalse(update().getErrors().isEmpty());
      Early.patch = record -> record.withValue("countryId", 1).withValue("stateId", 1);
      assertTrue(update().getErrors().isEmpty());
   }

   /***************************************************************************
    ** Controlled early callback; it never executes user source.
    ***************************************************************************/
   public static class Early implements TableCustomizerInterface
   {
      static Consumer<QRecord> patch;
      static int calls;
      static boolean badResult;
      static Consumer<UpdateInput> control;
      static List<String> priorCodes;
      static boolean preview;

      /***********************************************************************
       ** Explicitly opt in to the additive scheduling phase.
       ***********************************************************************/
      @Override
      public PreUpdatePhase whenToRunPreUpdate(UpdateInput input, boolean preview)
      {
         return (PreUpdatePhase.BEFORE_FIELD_VALIDATIONS);
      }

      /***********************************************************************
       ** Apply the trusted fixture patch to authorized values only.
       ***********************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> prior)
      {
         calls++;
         Early.preview = preview;
         prior.orElse(List.of()).forEach(record -> priorCodes.add(record.getValueString("code")));
         control.accept(input);
         records.forEach(patch);
         return (badResult ? List.of() : records);
      }
   }

   /***************************************************************************
    ** Existing hosts omit the phase method and remain after native validation.
    ***************************************************************************/
   public static class Late implements TableCustomizerInterface
   {
      static int calls;
      static List<String> seen;

      /***********************************************************************
       ** Record exactly what native field behaviors and validation produced.
       ***********************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> prior)
      {
         calls++;
         records.forEach(record -> seen.add(record.getValueString("code")));
         return (records);
      }
   }

   /***************************************************************************
    ** Fixture possible values only permit a state in country one.
    ***************************************************************************/
   public static class States implements QCustomPossibleValueProvider<Integer>
   {
      /***********************************************************************
       ** Resolve a stable fixture label.
       ***********************************************************************/
      public QPossibleValue<Integer> getPossibleValue(Serializable id)
      {
         return (new QPossibleValue<>((Integer) id, "State"));
      }

      /***********************************************************************
       ** Read the actual dependency supplied by native write validation.
       ***********************************************************************/
      public List<QPossibleValue<Integer>> search(SearchPossibleValueSourceInput input)
      {
         return (Integer.valueOf(1).equals(input.getOtherValues().get("countryId")) ? input.getIdList().stream().map(this::getPossibleValue).toList() : List.of());
      }
   }

   /***************************************************************************
    ** Patching a record-reference parent cannot bypass native parent visibility.
    ***************************************************************************/
   @Test
   void referenceSecurityStillRunsAfterEarlyPatch() throws Exception
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("storeId", 1), new QRecord().withValue("id", 2).withValue("storeId", 2))));
      QContext.getQSession().withSecurityKeyValues(java.util.Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, List.of(1)));
      RecordFeaturesMetaData.ofOrWithNew(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER)).withEnabled(RecordFeature.COMMENTS, true);
      QTableMetaData reference = new QTableMetaData().withName("earlyReference").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("tableName", QFieldType.STRING))
         .withField(new QFieldMetaData("recordId", QFieldType.STRING)).withField(new QFieldMetaData("body", QFieldType.STRING))
         .withSupplementalMetaData(new RecordReferenceTableMetaData().withTableNameField("tableName").withRecordIdField("recordId").withFeature(RecordFeature.COMMENTS));
      QContext.getQInstance().addTable(reference);
      new com.kingsrook.qqq.backend.core.instances.QInstanceEnricher(QContext.getQInstance()).enrich();
      com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataOverlay.refresh(QContext.getQInstance());
      QRecord original = new InsertAction().execute(new InsertInput("earlyReference").withRecords(List.of(new QRecord().withValue("tableName", TestUtils.TABLE_NAME_ORDER)
         .withValue("recordId", "1").withValue("body", "original")))).getRecords().getFirst();
      assertTrue(original.getErrors().isEmpty(), original.getErrors().toString());
      reference.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(Early.class));
      Early.patch = record -> record.setValue("recordId", "2");
      QRecord result = new UpdateAction().execute(new UpdateInput().withTableName("earlyReference").withRecords(List.of(new QRecord()
         .withValue("id", original.getValue("id")).withValue("body", "changed")))).getRecords().getFirst();
      assertEquals(1, Early.calls);
      assertFalse(result.getErrors().isEmpty());
      assertEquals("1", GetAction.execute("earlyReference", original.getValue("id")).getValueString("recordId"));
   }

   /***************************************************************************
    ** Added field values still face current field write authorization.
    ***************************************************************************/
   @Test
   void earlyPatchCannotAddDeniedField() throws Exception
   {
      com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance())
         .withResolverInstance((metadata, field) -> new com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision(
            field.getName().equals("stateId") ? com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier.MASKED
               : com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier.FULL, null, "fixture", false));
      Early.patch = record -> record.setValue("stateId", 42);
      assertThrows(QException.class, this::update);
      assertEquals(1, Early.calls);
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
   }

   /***************************************************************************
    ** A callback sees defensive request/metadata copies, not live control flags.
    ***************************************************************************/
   @Test
   void earlyCallbackCannotAlterValidationOrAuditControls() throws Exception
   {
      Early.control = input ->
      {
         input.setInputSource(QInputSource.SYSTEM);
         input.setOmitDmlAudit(true);
         input.setOmitRecordChangeEvents(true);
         input.setFlags(new java.util.HashSet<>());
         input.getTable().getField("code").setIsRequired(false);
      };
      Early.patch = record -> record.setValue("code", null);
      UpdateInput input = new UpdateInput().withTableName(TABLE).withInputSource(QInputSource.USER)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "sent")));
      assertFalse(new UpdateAction().execute(input).getRecords().getFirst().getErrors().isEmpty());
      assertEquals(QInputSource.USER, input.getInputSource());
      assertFalse(input.getOmitDmlAudit());
      assertFalse(input.getOmitRecordChangeEvents());
      assertTrue(table.getField("code").getIsRequired());
   }

   /***************************************************************************
    ** Association deletion or insertion cannot be smuggled into an early patch.
    ***************************************************************************/
   @Test
   void earlyCallbackCannotAddAssociationChanges() throws Exception
   {
      Early.patch = record -> record.setAssociatedRecords(java.util.Map.of("children", List.of()));
      assertThrows(QException.class, this::update);
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
   }

   /***************************************************************************
    ** Duplicate input object references are independent callback candidates.
    ***************************************************************************/
   @Test
   void duplicateInputReferenceDoesNotFailIdentityGate() throws Exception
   {
      table.setUniqueKeys(List.of());
      QRecord input = new QRecord().withValue("id", 1).withValue("code", "sent");
      var result = new UpdateAction().execute(new UpdateInput().withTableName(TABLE).withInputSource(QInputSource.USER).withRecords(List.of(input, input)));
      assertTrue(result.getRecords().stream().allMatch(record -> record.getErrors().isEmpty()));
      assertEquals(1, Early.calls);
   }

   /***************************************************************************
    ** A mixed batch exposes only authorized values to the early callback.
    ***************************************************************************/
   @Test
   void missingRowsStayOutsideCallbackAndReceiveOneError() throws Exception
   {
      var result = new UpdateAction().execute(new UpdateInput().withTableName(TABLE).withInputSource(QInputSource.USER)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "sent"), new QRecord().withValue("id", 999).withValue("code", "sent"))));
      assertEquals(1, Early.calls);
      assertEquals(List.of("one"), Early.priorCodes);
      assertEquals(1, result.getRecords().get(1).getErrors().size());
   }

   /***************************************************************************
    ** Bulk-load update previews route early callbacks through authorized native
    ** validation once, using stored values rather than merged preview records.
    ***************************************************************************/
   @Test
   void bulkUpdatePreviewRunsEarlyHookOnceWithStoredPrior() throws Exception
   {
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput();
      var output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      input.setTableName(TABLE);
      input.setStepName(com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.addValue("isBulkEdit", true);
      input.addValue("keyFields", "id");
      input.setRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "sent")));
      var step = new com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertTransformStep();
      step.preRun(input, output);
      step.runOnePage(input, output);
      assertEquals(1, Early.calls);
      assertEquals(List.of("one"), Early.priorCodes);
      assertTrue(Early.preview);
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
   }

   /***************************************************************************
    ** Legacy bulk preview invokes its table callback once and still invokes the
    ** independent instance callback plus all native field validations.
    ***************************************************************************/
   @Test
   void legacyBulkTableCallbackIsNotRepeatedByValidation() throws Exception
   {
      table.getCustomizers().put(TableCustomizers.PRE_UPDATE_RECORD.getRole(), new QCodeReference(Late.class));
      QContext.getQInstance().withTableCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(Late.class));
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput();
      var output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      input.setTableName(TABLE);
      input.setStepName(com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.addValue("isBulkEdit", true);
      input.addValue("keyFields", "id");
      input.setRecords(List.of(new QRecord().withValue("id", 1).withValue("code", "")));
      var step = new com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertTransformStep();
      step.preRun(input, output);
      step.runOnePage(input, output);
      assertEquals(2, Late.calls);
      assertTrue(output.getRecords().isEmpty());
      assertEquals("one", GetAction.execute(TABLE, 1).getValueString("code"));
   }

   /***************************************************************************
    ** Instance early callbacks run before legacy table callbacks, once each.
    ***************************************************************************/
   @Test
   void instanceEarlyPhasePrecedesDefaultTablePhase() throws Exception
   {
      table.getCustomizers().put(TableCustomizers.PRE_UPDATE_RECORD.getRole(), new QCodeReference(Late.class));
      QContext.getQInstance().withTableCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(Early.class));
      assertTrue(update().getErrors().isEmpty());
      assertEquals(1, Early.calls);
      assertEquals(1, Late.calls);
      assertEquals(List.of("good"), Late.seen);
   }
}
