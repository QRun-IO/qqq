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

package com.kingsrook.sampleapp;


import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsUtil;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryModuleBackendVariantSetting;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Capability metadata and owned per-session variants in the sample instance.
 *******************************************************************************/
class SampleBackendCapabilityTest
{
   private QInstance instance;
   private QBackendMetaData variantBackend;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.fullReset();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      variantBackend = new QBackendMetaData().withName("ownedVariantBackend").withBackendType(MemoryBackendModule.class).withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig().withOptionsTableName("person").withVariantTypeKey("ownedVariant")
            .withBackendSettingSourceFieldNameMap(Map.of(MemoryModuleBackendVariantSetting.PRIMARY_KEY, "id")));
      instance.addBackend(variantBackend);
      for(String name : List.of("ownedVariantRows", "ownedOtherRows"))
      {
         instance.addTable(new QTableMetaData().withName(name).withBackendName(variantBackend.getName()).withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("name", QFieldType.STRING)));
      }
      new QInstanceValidator().revalidate(instance);
      QContext.init(instance, new QSession().withBackendVariants(Map.of("ownedVariant", 1)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      MemoryRecordStore.fullReset();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Every declared flag follows the documented backend/table precedence.
    *******************************************************************************/
   @Test
   void testAllCapabilityDefaultsAndOverrides()
   {
      QTableMetaData table = instance.getTable("person");
      QBackendMetaData backend = instance.getBackendForTable("person");
      assertEquals(EnumSet.allOf(Capability.class), union(Capability.allReadCapabilities(), Capability.allWriteCapabilities()));
      for(Capability capability : Capability.values())
      {
         backend.setDisabledCapabilities(EnumSet.of(Capability.QUERY_STATS));
         table.setEnabledCapabilities(EnumSet.noneOf(Capability.class));
         table.setDisabledCapabilities(EnumSet.noneOf(Capability.class));
         assertEquals(capability != Capability.QUERY_STATS, table.isCapabilityEnabled(backend, capability));
         backend.withoutCapabilities(Set.of(capability));
         assertFalse(table.isCapabilityEnabled(backend, capability));
         assertFalse(frontend(table, backend).getCapabilities().contains(capability.name()));
         table.withCapability(capability);
         assertTrue(table.isCapabilityEnabled(backend, capability));
         assertTrue(frontend(table, backend).getCapabilities().contains(capability.name()));
         table.setEnabledCapabilities(EnumSet.noneOf(Capability.class));
         backend.withCapability(capability);
         table.withoutCapability(capability);
         assertFalse(table.isCapabilityEnabled(backend, capability));
         table.withCapability(capability);
         assertTrue(table.isCapabilityEnabled(backend, capability));
      }
   }



   /*******************************************************************************
    ** A declaration cannot supply a missing backend implementation.
    *******************************************************************************/
   @Test
   void testBackendDispatchUnknownAndUnsupportedOperations() throws Exception
   {
      QBackendModuleDispatcher dispatcher = new QBackendModuleDispatcher();
      assertInstanceOf(RDBMSBackendModule.class, dispatcher.getQBackendModule(instance.getBackendForTable("person")));
      assertEquals(5, CountAction.execute("person", null));
      assertInstanceOf(MemoryBackendModule.class, dispatcher.getQBackendModule(variantBackend));
      assertThrows(QModuleDispatchException.class, () -> dispatcher.getQBackendModule("ownedUnregisteredBackend"));
      QBackendModuleDispatcher.registerBackendModule(new UnsupportedBackend());
      QBackendMetaData unsupported = new QBackendMetaData().withName("ownedUnsupported").withBackendType(UnsupportedBackend.class);
      instance.addBackend(unsupported);
      instance.getTable("ownedOtherRows").setBackendName(unsupported.getName());
      assertThrows(IllegalStateException.class, () -> dispatcher.getQBackendModule(unsupported).getQueryInterface());
      assertThrows(IllegalStateException.class, () -> new QueryAction().execute(new QueryInput("ownedOtherRows")));
      assertEquals(5, CountAction.execute("person", null));
   }



   /*******************************************************************************
    ** Same table/key in two variants and another table retain independent values.
    *******************************************************************************/
   @Test
   void testVariantAndTableIsolationMissingSelectionAndUnknownOption() throws Exception
   {
      insert("ownedVariantRows", "One");
      insert("ownedOtherRows", "Other table");
      assertEquals("Avery", BackendVariantsUtil.getVariantRecord(variantBackend).getValueString("firstName"));
      select(2);
      assertEquals(0, CountAction.execute("ownedVariantRows", null));
      assertEquals(0, CountAction.execute("ownedOtherRows", null));
      insert("ownedVariantRows", "Two");
      assertEquals("Two", GetAction.execute("ownedVariantRows", 1).getValueString("name"));
      select(1);
      assertEquals("One", GetAction.execute("ownedVariantRows", 1).getValueString("name"));
      assertEquals("Other table", GetAction.execute("ownedOtherRows", 1).getValueString("name"));
      for(Map<String, Serializable> selection : List.<Map<String, Serializable>>of(Map.of(), Map.of("ownedVariant", 999)))
      {
         QContext.setQSession(new QSession().withBackendVariants(selection));
         assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput("ownedVariantRows")));
         assertThrows(QException.class, () -> insert("ownedVariantRows", "Rejected"));
      }
      select(1);
      assertEquals(List.of("One"), QueryAction.execute("ownedVariantRows", null).stream().map(row -> row.getValueString("name")).toList());
      select(2);
      assertEquals(List.of("Two"), QueryAction.execute("ownedVariantRows", null).stream().map(row -> row.getValueString("name")).toList());
   }



   /*******************************************************************************
    ** Both supported callback types resolve records; misses and exceptions fail.
    *******************************************************************************/
   @Test
   void testCustomVariantLookupAndFailures() throws Exception
   {
      variantBackend.getBackendVariantsConfig().setVariantRecordLookupFunction(new QCodeReference(FunctionVariant.class));
      assertEquals("Function variant", BackendVariantsUtil.getVariantRecord(variantBackend).getValueString("firstName"));
      insert("ownedVariantRows", "Function data");
      select(999);
      assertThrows(QException.class, () -> BackendVariantsUtil.getVariantRecord(variantBackend));
      select(1);
      variantBackend.getBackendVariantsConfig().setVariantRecordLookupFunction(new QCodeReference(NativeVariant.class));
      assertEquals("Function data", GetAction.execute("ownedVariantRows", 1).getValueString("name"));
      select(-1);
      assertThrows(QException.class, () -> QueryAction.execute("ownedVariantRows", null));
      select(1);
      variantBackend.getBackendVariantsConfig().setVariantRecordLookupFunction(null);
      assertEquals("Function data", GetAction.execute("ownedVariantRows", 1).getValueString("name"));
   }



   /*******************************************************************************
    ** Invalid variant settings fail validation before executing a data operation.
    *******************************************************************************/
   @Test
   void testVariantConfigurationValidation()
   {
      BackendVariantsConfig config = variantBackend.getBackendVariantsConfig();
      config.setOptionsTableName("missingOptions");
      assertThrows(QException.class, () -> new QInstanceValidator().revalidate(instance));
      config.setOptionsTableName("person");
      config.setBackendSettingSourceFieldNameMap(Map.of(MemoryModuleBackendVariantSetting.PRIMARY_KEY, "missingField"));
      assertThrows(QException.class, () -> new QInstanceValidator().revalidate(instance));
      config.setBackendSettingSourceFieldNameMap(Map.of(MemoryModuleBackendVariantSetting.PRIMARY_KEY, "id"));
      variantBackend.setUsesVariants(false);
      assertThrows(QException.class, () -> new QInstanceValidator().revalidate(instance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<Capability> union(Set<Capability> reads, Set<Capability> writes)
   {
      Set<Capability> all = EnumSet.copyOf(reads);
      all.addAll(writes);
      return all;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QFrontendTableMetaData frontend(QTableMetaData table, QBackendMetaData backend)
   {
      return new QFrontendTableMetaData(new AbstractActionInput(), backend, table, false, false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void select(Integer variant)
   {
      QContext.setQSession(new QSession().withBackendVariants(Map.of("ownedVariant", variant)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void insert(String table, String name) throws Exception
   {
      QRecord row = new InsertAction().executeForRecord(new InsertInput(table).withRecord(new QRecord().withValue("id", 1).withValue("name", name)));
      assertTrue(row.getErrors().isEmpty(), row.getErrorsAsString());
   }



   /*******************************************************************************
    ** Owned registration with no table operations implemented.
    *******************************************************************************/
   public static class UnsupportedBackend implements QBackendModuleInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getBackendType()
      {
         return "ownedUnsupportedType";
      }
   }



   /*******************************************************************************
    ** Ordinary Java function with an explicit missing option.
    *******************************************************************************/
   public static class FunctionVariant implements Function<Serializable, QRecord>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QRecord apply(Serializable id)
      {
         return Integer.valueOf(1).equals(id) ? new QRecord().withTableName("person").withValue("id", id).withValue("firstName", "Function variant") : null;
      }
   }



   /*******************************************************************************
    ** Native lookup through the exception-capable function contract.
    *******************************************************************************/
   public static class NativeVariant implements UnsafeFunction<Serializable, QRecord, QException>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QRecord apply(Serializable id) throws QException
      {
         if(Integer.valueOf(-1).equals(id))
         {
            throw new QException("Owned lookup failure");
         }
         return GetAction.execute("person", id);
      }
   }
}
