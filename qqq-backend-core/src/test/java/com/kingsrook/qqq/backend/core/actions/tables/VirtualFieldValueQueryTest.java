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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.actions.reporting.BufferedRecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.query.VirtualFieldValueProviderInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise provider execution through real memory queries and host customization.
 *******************************************************************************/
class VirtualFieldValueQueryTest extends BaseTest
{
   private static int calls;
   private static int rows;
   private static final List<Integer> batchSizes = new ArrayList<>();

   /***************************************************************************
    ** Use two rows to distinguish a batch callback from per-record execution.
    ***************************************************************************/
   @BeforeEach
   void prepare() throws QException
   {
      calls = 0;
      rows = 0;
      batchSizes.clear();
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(HostCustomizer.class))
         .withVirtualField(new QVirtualFieldMetaData("provided", QFieldType.STRING).withIsQuerySelectable(true)
            .withValueProvider(new QCodeReference(Provider.class)));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecords(List.of(new QRecord().withValue("id", 1).withValue("firstName", "A"), new QRecord().withValue("id", 2).withValue("firstName", "B"))));
   }

   /***************************************************************************
    ** The provider sees the host's result once for the entire batch.
    ***************************************************************************/
   @Test
   void fillsAfterHostCustomizer() throws QException
   {
      var records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getRecords();
      assertEquals(2, records.size());
      assertEquals(1, calls);
      assertEquals(2, rows);
      assertTrue(records.stream().allMatch(record -> "host-value".equals(record.getValueString("provided"))));
   }

   /***************************************************************************
    ** Filter picker metadata can coexist with provider-produced labels.
    ***************************************************************************/
   @Test
   void labelProviderRetainsPickerWithoutTranslatingOutput() throws QException
   {
      var source = new com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource()
         .withName("providerPicker").withType(com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue<>("host-value", "Translated")));
      QContext.getQInstance().addPossibleValueSource(source);
      var field = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getVirtualField("provided");
      field.withPossibleValueSourceName("providerPicker");
      assertTrue(field.getShouldTranslatePossibleValues());
      var input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withShouldTranslatePossibleValues(true).withShouldGenerateDisplayValues(true);
      assertEquals("Translated", new QueryAction().execute(input).getRecords().getFirst().getDisplayValue("provided"));
      field.withShouldTranslatePossibleValues(false);
      var record = new QueryAction().execute(input).getRecords().getFirst();
      assertEquals("host-value", record.getValueString("provided"));
      assertEquals("host-value", record.getDisplayValue("provided"));
      assertEquals("providerPicker", field.getPossibleValueSourceName());
      assertTrue(!field.clone().getShouldTranslatePossibleValues());
   }

   /***************************************************************************
    ** Explicit selection excludes unrelated provider work and output.
    ***************************************************************************/
   @Test
   void omittedFieldDoesNotInvokeProvider() throws QException
   {
      var records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withFieldNamesToInclude(Set.of("id", "firstName"))).getRecords();
      assertEquals(0, calls);
      assertTrue(records.stream().noneMatch(record -> record.getValues().containsKey("provided")));
   }

   /***************************************************************************
    ** Hidden metadata does not trigger a lookup whose result would be discarded.
    ***************************************************************************/
   @Test
   void hiddenFieldDoesNotInvokeProvider() throws QException
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getVirtualFields().get("provided").setIsHidden(true);
      var records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getRecords();
      assertEquals(0, calls);
      assertTrue(records.stream().noneMatch(record -> record.getValues().containsKey("provided")));
   }

   /***************************************************************************
    ** A session-hidden field must never invoke its provider, even if metadata is visible.
    ***************************************************************************/
   @Test
   void sessionHiddenFieldDoesNotInvokeProvider() throws QException
   {
      restrictProvidedField(FieldAccessTier.HIDDEN);
      var records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withInputSource(QInputSource.USER)).getRecords();
      assertEquals(2, records.size());
      assertEquals(0, calls);
      assertTrue(records.stream().noneMatch(record -> record.getValues().containsKey("provided")));
   }

   /***************************************************************************
    ** Final field policy must remove the provider's raw value from masked output.
    ***************************************************************************/
   @Test
   void providerValuesAreMasked() throws QException
   {
      restrictProvidedField(FieldAccessTier.MASKED);
      var records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withInputSource(QInputSource.USER)).getRecords();
      assertEquals(2, records.size());
      assertEquals(1, calls);
      assertTrue(records.stream().allMatch(record -> !"host-value".equals(record.getValueString("provided"))));
   }

   /***************************************************************************
    ** Restrict only the contributed field so normal record access stays intact.
    ***************************************************************************/
   private void restrictProvidedField(FieldAccessTier tier)
   {
      FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance((table, field) ->
         "provided".equals(field.getName()) ? new FieldAccessDecision(tier, FieldMaskStyle.FULL, null) : FieldAccessDecision.FULL);
   }

   /***************************************************************************
    ** Provider-only projections retain operational identity without returning it.
    ***************************************************************************/
   @Test
   void providerOnlyProjectionDoesNotExposeIdentity() throws QException
   {
      var table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getVirtualFields().get("provided").setValueProvider(new QCodeReference(IdentityProvider.class));
      table.getCustomizers().clear();
      Set<String> selection = Set.of("provided");
      var input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFieldNamesToInclude(selection);
      var records = new QueryAction().execute(input).getRecords();
      assertEquals(selection, input.getFieldNamesToInclude());
      assertEquals(2, records.size());
      for(QRecord record : records)
      {
         assertEquals(Set.of("provided"), record.getValues().keySet());
         assertTrue(record.getValueString("provided").startsWith("record-"));
         assertTrue(!record.getDisplayValues().containsKey("id"));
      }
   }

   /***************************************************************************
    ** Stand-in for tag/EAV lookup, which needs each source record's identity.
    ***************************************************************************/
   public static class IdentityProvider implements VirtualFieldValueProviderInterface
   {
      /***********************************************************************
       ** Missing identity must fail rather than return an apparently empty value.
       ***********************************************************************/
      @Override
      public void fill(QTableMetaData table, String fieldName, List<QRecord> records, QueryInput input)
      {
         calls++;
         rows += records.size();
         batchSizes.add(records.size());
         for(QRecord record : records)
         {
            org.junit.jupiter.api.Assertions.assertNotNull(record.getValue(table.getPrimaryKeyField()));
            record.setValue(fieldName, "record-" + record.getValue(table.getPrimaryKeyField()));
         }
      }
   }

   /***************************************************************************
    ** Do not silently apply backend DISTINCT to identities instead of derived values.
    ***************************************************************************/
   @Test
   void distinctProviderValuesFailExplicitly()
   {
      var input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withFieldNamesToInclude(Set.of("provided")).withSelectDistinct(true);
      var failure = assertThrows(QException.class, () -> new QueryAction().execute(input));
      assertTrue(failure.getMessage().contains("DISTINCT"));
      assertEquals(Set.of("provided"), input.getFieldNamesToInclude());
      assertEquals(0, calls);
   }

   /***************************************************************************
    ** GET through a query must preserve field policy while invoking providers once.
    ***************************************************************************/
   @Test
   void getUsesProviderOnce() throws QException
   {
      var record = new GetAction().execute(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(1)).getRecord();
      assertEquals("host-value", record.getValueString("provided"));
      assertEquals(1, calls);
      assertEquals(1, rows);
   }

   /***************************************************************************
    ** Parent GET authorization must not cause a nested query to load hidden values.
    ***************************************************************************/
   @Test
   void getHiddenFieldDoesNotInvokeProvider() throws QException
   {
      restrictProvidedField(FieldAccessTier.HIDDEN);
      var record = new GetAction().execute(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(1)
         .withInputSource(QInputSource.USER)).getRecord();
      assertTrue(!record.getValues().containsKey("provided"));
      assertEquals(0, calls);
   }

   /***************************************************************************
    ** A backend with a native GET also runs the field provider after customization.
    ***************************************************************************/
   @ParameterizedTest
   @EnumSource(value = FieldAccessTier.class, names = { "FULL", "HIDDEN", "MASKED" })
   void nativeGetUsesProviderOnce(FieldAccessTier tier) throws QException
   {
      com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher.registerBackendModule(new NativeGetModule());
      QContext.getQInstance().getBackend(QContext.getQInstance().getTable("person").getBackendName()).setBackendType("virtual-field-native-get");
      QContext.getQInstance().getTable("person")
         .withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(HostCustomizer.class))
         .withVirtualField(new QVirtualFieldMetaData("provided", QFieldType.STRING).withIsQuerySelectable(true)
            .withValueProvider(new QCodeReference(Provider.class)));
      restrictProvidedField(tier);
      var record = new GetAction().execute(new GetInput("person").withPrimaryKey(1).withInputSource(QInputSource.USER)
         .withShouldGenerateDisplayValues(true)).getRecord();
      assertEquals(tier == FieldAccessTier.HIDDEN ? 0 : 1, calls);
      if(tier == FieldAccessTier.FULL)
      {
         assertEquals("host-value", record.getValueString("provided"));
         assertEquals("host-value", record.getDisplayValue("provided"));
      }
      else
      {
         assertTrue(!"host-value".equals(record.getValueString("provided")));
         assertTrue(!"host-value".equals(record.getDisplayValue("provided")));
         if(tier == FieldAccessTier.HIDDEN)
         {
            assertTrue(!record.getValues().containsKey("provided"));
            assertTrue(!record.getDisplayValues().containsKey("provided"));
         }
      }
   }

   /***************************************************************************
    ** Native GET providers can use a hidden identity without exposing it afterward.
    ***************************************************************************/
   @Test
   void nativeGetKeepsHiddenIdentityPrivate() throws QException
   {
      com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher.registerBackendModule(new NativeGetModule());
      var table = QContext.getQInstance().getTable("person");
      QContext.getQInstance().getBackend(table.getBackendName()).setBackendType("virtual-field-native-get");
      table.getField("id").setIsHidden(true);
      table.withVirtualField(new QVirtualFieldMetaData("provided", QFieldType.STRING).withIsQuerySelectable(true)
         .withValueProvider(new QCodeReference(IdentityProvider.class)));
      var record = new GetAction().execute(new GetInput("person").withPrimaryKey(1).withInputSource(QInputSource.USER)
         .withShouldGenerateDisplayValues(true)).getRecord();
      assertEquals("record-1", record.getValueString("provided"));
      assertEquals(1, calls);
      assertTrue(!record.getValues().containsKey("id"));
      assertTrue(!record.getDisplayValues().containsKey("id"));
   }

   /***************************************************************************
    ** The internal GET bridge is unavailable without an already-authorized action.
    ***************************************************************************/
   @Test
   void nestedGetBridgeRequiresAuthorization()
   {
      assertThrows(QException.class, () -> new QueryAction().executeForGet(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY), true));
      assertEquals(0, calls);
   }

   /***************************************************************************
    ** A plain consumer pipe still receives bounded provider batches, never one call per row.
    ***************************************************************************/
   @ParameterizedTest
   @ValueSource(ints = { 0, 64 })
   void streamedProjectionUsesBoundedBatches(int bufferSize) throws QException
   {
      var table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getVirtualFields().get("provided").setValueProvider(new QCodeReference(IdentityProvider.class));
      table.getCustomizers().clear();
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(
         java.util.stream.IntStream.rangeClosed(3, 205).mapToObj(id -> new QRecord().withValue("id", id)).toList()));
      RecordPipe pipe = bufferSize == 0 ? new RecordPipe() : new BufferedRecordPipe(bufferSize);
      var input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFieldNamesToInclude(Set.of("provided"));
      input.setRecordPipe(pipe);
      new QueryAction().execute(input);
      var delivered = pipe.consumeAvailableRecords();
      assertEquals(205, delivered.size());
      assertEquals(205, pipe.getTotalRecordCount());
      assertEquals(205, rows);
      assertEquals(bufferSize == 0 ? 3 : 4, calls);
      assertEquals(bufferSize == 0 ? List.of(100, 100, 5) : List.of(64, 64, 64, 13), batchSizes);
      assertSame(pipe, input.getRecordPipe());
      assertEquals(Set.of("provided"), input.getFieldNamesToInclude());
      assertTrue(delivered.stream().allMatch(record -> record.getValues().keySet().equals(Set.of("provided"))));
   }

   /***************************************************************************
    ** An empty result must not issue an unnecessary provider lookup.
    ***************************************************************************/
   @Test
   void emptyQuerySkipsProvider() throws QException
   {
      var input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(
         new com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter(
            new com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria("id",
               com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, -1)));
      assertTrue(new QueryAction().execute(input).getRecords().isEmpty());
      assertEquals(0, calls);
   }

   /***************************************************************************
    ** A provider failure must not publish the partially filled batch to its consumer.
    ***************************************************************************/
   @Test
   void failedProviderDoesNotPublishBatch()
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getVirtualFields().get("provided")
         .setValueProvider(new QCodeReference(FailingProvider.class));
      RecordPipe pipe = new RecordPipe();
      var input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.setRecordPipe(pipe);
      assertThrows(QException.class, () -> new QueryAction().execute(input));
      assertTrue(pipe.consumeAvailableRecords().isEmpty());
      assertEquals(0, pipe.getTotalRecordCount());
   }

   /***************************************************************************
    ** Fail after mutating one record to verify publication happens after provider success.
    ***************************************************************************/
   public static class FailingProvider implements VirtualFieldValueProviderInterface
   {
      /***********************************************************************
       ** Simulate a failed batched lookup.
       ***********************************************************************/
      @Override
      public void fill(QTableMetaData table, String fieldName, List<QRecord> records, QueryInput input) throws QException
      {
         records.get(0).setValue(fieldName, "partial");
         throw new QException("Provider fixture failed");
      }
   }

   /***************************************************************************
    ** Aliased left joins use child identity and skip absent children.
    ***************************************************************************/
   @Test
   void joinedProviderUsesChildIdentity() throws QException
   {
      var input = joinedInput();
      var records = new QueryAction().execute(input).getRecords();
      assertEquals(2, records.size());
      assertEquals(1, calls);
      assertEquals(1, rows);
      assertEquals(1, records.stream().filter(record -> "record-10".equals(record.getValueString("child.provided"))).count());
      assertEquals(1, records.stream().filter(record -> record.getValue("child.provided") == null).count());
      assertTrue(records.stream().allMatch(record -> record.getValues().keySet().equals(Set.of("child.provided"))));
      assertEquals(Set.of("child.provided"), input.getFieldNamesToInclude());
   }

   /***************************************************************************
    ** Joined aliases retain the virtual field's output-translation policy.
    ***************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void joinedProviderTranslationUsesAlias(boolean translate) throws QException
   {
      var input = joinedInput().withShouldTranslatePossibleValues(true).withShouldGenerateDisplayValues(true);
      var instance = QContext.getQInstance();
      instance.addPossibleValueSource(new com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource()
         .withName("joinedProviderPicker").withType(com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue<>("record-10", "Translated child"))));
      instance.getTable("providerChild").getVirtualField("provided").withPossibleValueSourceName("joinedProviderPicker")
         .withShouldTranslatePossibleValues(translate);
      var records = new QueryAction().execute(input).getRecords();
      assertEquals(2, records.size());
      assertEquals(1, calls);
      var matching = records.stream().filter(record -> record.getValue("child.provided") != null).findFirst().orElseThrow();
      assertEquals("record-10", matching.getValueString("child.provided"));
      assertEquals(translate ? "Translated child" : "record-10", matching.getDisplayValue("child.provided"));
      assertTrue(records.stream().allMatch(record -> record.getValues().keySet().equals(Set.of("child.provided"))));
      assertTrue(records.stream().noneMatch(record -> record.getDisplayValues().containsKey("child.id")));
   }

   /***************************************************************************
    ** Joined field policy suppresses lookups or masks output under its alias.
    ***************************************************************************/
   @ParameterizedTest
   @EnumSource(value = FieldAccessTier.class, names = { "HIDDEN", "MASKED" })
   void joinedProviderRespectsFieldPolicy(FieldAccessTier tier) throws QException
   {
      var input = joinedInput().withInputSource(QInputSource.USER);
      restrictProvidedField(tier);
      if(tier == FieldAccessTier.HIDDEN)
      {
         input.setFieldNamesToInclude(null);
      }
      var records = new QueryAction().execute(input).getRecords();
      assertEquals(2, records.size());
      assertEquals(tier == FieldAccessTier.HIDDEN ? 0 : 1, calls);
      assertTrue(records.stream().noneMatch(record -> "record-10".equals(record.getValueString("child.provided"))));
      if(tier == FieldAccessTier.HIDDEN)
      {
         assertTrue(records.stream().noneMatch(record -> record.getValues().containsKey("child.provided")));
      }
   }

   /***************************************************************************
    ** One matching child and one unmatched parent exercise left-join semantics.
    ***************************************************************************/
   private QueryInput joinedInput() throws QException
   {
      var instance = QContext.getQInstance();
      var parent = instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      parent.getCustomizers().clear();
      instance.addTable(new QTableMetaData().withName("providerChild").withBackendName(parent.getBackendName())
         .withPrimaryKeyField("id")
         .withField(new com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData("parentId", QFieldType.INTEGER))
         .withVirtualField(new QVirtualFieldMetaData("provided", QFieldType.STRING).withIsQuerySelectable(true)
            .withValueProvider(new QCodeReference(IdentityProvider.class))));
      instance.addJoin(new com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData().withName("providerChildJoin")
         .withLeftTable(parent.getName()).withRightTable("providerChild")
         .withType(com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType.ONE_TO_MANY)
         .withJoinOn(new com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn("id", "parentId")));
      new InsertAction().execute(new InsertInput("providerChild")
         .withRecord(new QRecord().withValue("id", 10).withValue("parentId", 1)));
      var input = new QueryInput(parent.getName()).withFieldNamesToInclude(Set.of("child.provided"))
         .withQueryJoin(new com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin("providerChild")
            .withSelect(true).withAlias("child").withType(com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin.Type.LEFT));
      return (input);
   }

   /***************************************************************************
    ** Host customization remains in its original slot.
    ***************************************************************************/
   public static class HostCustomizer implements TableCustomizerInterface
   {
      /***********************************************************************
       ** Supply the value consumed by the subsequent field provider.
       ***********************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         records.forEach(record -> record.setValue("firstName", "host-value"));
         return (records);
      }
   }

   /***************************************************************************
    ** Count actual provider calls and use the host-customized field value.
    ***************************************************************************/
   public static class Provider implements VirtualFieldValueProviderInterface
   {
      /***********************************************************************
       ** Fill one field for a whole batch.
       ***********************************************************************/
      @Override
      public void fill(QTableMetaData table, String fieldName, List<QRecord> records, QueryInput input)
      {
         calls++;
         rows += records.size();
         records.forEach(record -> record.setValue(fieldName, record.getValue("firstName")));
      }
   }

   /***************************************************************************
    ** Dedicated native GET backend avoids accidentally testing the query fallback.
    ***************************************************************************/
   public static class NativeGetModule extends com.kingsrook.qqq.backend.core.modules.backend.implementations.mock.MockBackendModule
   {
      /***********************************************************************
       ** Do not replace the shared mock backend registration.
       ***********************************************************************/
      @Override
      public String getBackendType()
      {
         return ("virtual-field-native-get");
      }

      /***********************************************************************
       ** Return a native record without calling QueryAction.
       ***********************************************************************/
      @Override
      public com.kingsrook.qqq.backend.core.actions.interfaces.GetInterface getGetInterface()
      {
         return (input -> new com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput()
            .withRecord(new QRecord().withValue("id", input.getPrimaryKey()).withValue("firstName", "native")));
      }
   }
}
