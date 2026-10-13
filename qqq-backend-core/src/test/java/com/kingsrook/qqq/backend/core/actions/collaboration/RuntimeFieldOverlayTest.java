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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeTableMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class RuntimeFieldOverlayTest extends BaseTest
{
   private QInstance instance;
   private QTableMetaData original;



   /*******************************************************************************
    ** Start from validated metadata and an empty provider.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      instance = QContext.getQInstance();
      original = new QTableMetaData().withName("runtimeTest").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      instance.addTable(original);
      new QInstanceValidator().validate(instance);
      Provider.contribution = new RuntimeMetaDataContribution();
      RecordFeaturesMetaData.ofOrWithNew(instance).withRuntimeProviders(List.of(new QCodeReference(Provider.class)));
   }



   /*******************************************************************************
    ** Copy-on-write publication, sections, search, frontend metadata and removal.
    *******************************************************************************/
   @Test
   void addsAndRemovesFields() throws Exception
   {
      RuntimeTableMetaDataContribution table = new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("custom", QFieldType.STRING)))
         .withSectionPlacements(Map.of("custom", "extra")).withSearchFields(List.of("custom"));
      Provider.contribution.withTables(Map.of("runtimeTest", table));
      RuntimeMetaDataOverlay.refresh(instance);
      QTableMetaData published = instance.getTable("runtimeTest");
      assertNotSame(original, published);
      assertFalse(original.getFields().containsKey("custom"));
      assertEquals("Custom", published.getField("custom").getLabel());
      assertEquals("Custom fields", published.getSection("extra").getLabel());
      assertEquals("extra", published.getSections().get(published.getSections().size() - 1).getName());
      assertEquals(List.of("custom"), published.getSearchFields());
      QFrontendTableMetaData frontend = new QFrontendTableMetaData(new QueryInput(), instance.getBackendForTable("runtimeTest"), published, true, false);
      assertTrue(frontend.getFields().containsKey("custom"));
      assertTrue(instance.getHasBeenValidated());
      assertEquals(1, RuntimeMetaDataOverlay.getGeneration(instance));
      // Repeated refresh must replace rather than accumulate fields and sections.
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(2, instance.getTable("runtimeTest").getFields().size());
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(3, RuntimeMetaDataOverlay.getGeneration(instance));
      assertFalse(instance.getTable("runtimeTest").getFields().containsKey("custom"));
      assertNull(instance.getTable("runtimeTest").getSection("extra"));
      assertNull(instance.getTable("runtimeTest").getSearchFields());
      assertTrue(published.getFields().containsKey("custom"));
   }



   /*******************************************************************************
    ** A bad table retains its accepted generation while code fields win clashes.
    *******************************************************************************/
   @Test
   void clashesAndInvalidContributions() throws Exception
   {
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("id", QFieldType.STRING), JsonPathStorageTest.field("custom", QFieldType.STRING)))));
      RuntimeMetaDataOverlay.refresh(instance);
      QTableMetaData accepted = instance.getTable("runtimeTest");
      assertEquals(QFieldType.INTEGER, accepted.getField("id").getType());
      assertNotNull(accepted.getField("custom"));
      QFieldMetaData invalid = JsonPathStorageTest.field("bad", QFieldType.BLOB);
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution().withFields(List.of(invalid))));
      RuntimeMetaDataOverlay.refresh(instance);
      assertSame(accepted, instance.getTable("runtimeTest"));
      assertEquals(2, RuntimeMetaDataOverlay.getGeneration(instance));
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(List.of("id"), new ArrayList<>(instance.getTable("runtimeTest").getFields().keySet()));
   }



   /*******************************************************************************
    ** Virtual fields are contributed independently of physical form sections.
    *******************************************************************************/
   @Test
   void addsVirtualFieldsAndIsolatesTables() throws Exception
   {
      instance.addTable(new QTableMetaData().withName("otherRuntime").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      new QInstanceValidator().revalidate(instance);
      QTableMetaData other = instance.getTable("otherRuntime");
      Provider.contribution.withTables(Map.of(
         "runtimeTest", new RuntimeTableMetaDataContribution().withVirtualFields(List.of(new QVirtualFieldMetaData("computed", QFieldType.STRING))),
         "otherRuntime", new RuntimeTableMetaDataContribution().withFields(List.of(JsonPathStorageTest.field("bad", QFieldType.BLOB)))));
      RuntimeMetaDataOverlay.refresh(instance);
      assertNotNull(instance.getTable("runtimeTest").getVirtualField("computed"));
      assertSame(other, instance.getTable("otherRuntime"));
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertNull(instance.getTable("runtimeTest").getVirtualField("computed"));
   }



   /*******************************************************************************
    ** Provider used to simulate committed metadata changes.
    *******************************************************************************/
   public static class Provider implements RuntimeMetaDataProviderInterface
   {
      private static RuntimeMetaDataContribution contribution;



      /*******************************************************************************
       ** Return the current contribution.
       *******************************************************************************/
      @Override
      public RuntimeMetaDataContribution getContribution(QInstance instance)
      {
         return (contribution);
      }
   }
}

