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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.enumeration;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Enum-backed tables are read-only: the enricher disables the write
 ** capabilities the module cannot support, so no bulk write processes or
 ** write permissions are offered for them.
 *******************************************************************************/
class EnumerationBackendCapabilitiesTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEnumTablesAreReadOnly()
   {
      QInstance instance = TestUtils.defineInstance();
      QBackendMetaData backend = new QBackendMetaData().withName("enum").withBackendType(EnumerationBackendModule.class);
      instance.addBackend(backend);
      instance.addTable(enumTable("statesEnum"));

      new QInstanceEnricher(instance).enrich();

      QTableMetaData table = instance.getTable("statesEnum");
      assertTrue(table.isCapabilityEnabled(backend, Capability.TABLE_QUERY));
      assertTrue(table.isCapabilityEnabled(backend, Capability.TABLE_COUNT));
      assertFalse(table.isCapabilityEnabled(backend, Capability.TABLE_INSERT));
      assertFalse(table.isCapabilityEnabled(backend, Capability.TABLE_UPDATE));
      assertFalse(table.isCapabilityEnabled(backend, Capability.TABLE_DELETE));
      assertNull(instance.getProcess("statesEnum.bulkInsert"));
      assertNull(instance.getProcess("statesEnum.bulkEdit"));
      assertNull(instance.getProcess("statesEnum.bulkDelete"));
   }



   /*******************************************************************************
    ** A backend that explicitly enables a capability keeps it.
    *******************************************************************************/
   @Test
   void testExplicitlyEnabledCapabilityIsKept()
   {
      QInstance instance = TestUtils.defineInstance();
      QBackendMetaData backend = new QBackendMetaData().withName("enum").withBackendType(EnumerationBackendModule.class)
         .withCapability(Capability.TABLE_UPDATE);
      instance.addBackend(backend);
      instance.addTable(enumTable("statesEnum"));

      new QInstanceEnricher(instance).enrich();

      assertTrue(instance.getTable("statesEnum").isCapabilityEnabled(backend, Capability.TABLE_UPDATE));
      assertFalse(instance.getTable("statesEnum").isCapabilityEnabled(backend, Capability.TABLE_INSERT));
   }



   /*******************************************************************************
    ** Other modules are unaffected.
    *******************************************************************************/
   @Test
   void testOtherBackendsKeepWrites()
   {
      QInstance instance = TestUtils.defineInstance();
      new QInstanceEnricher(instance).enrich();
      QTableMetaData person = instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertTrue(person.isCapabilityEnabled(instance.getBackendForTable(person.getName()), Capability.TABLE_INSERT));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData enumTable(String name)
   {
      return new QTableMetaData()
         .withName(name)
         .withBackendName("enum")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("postalCode", QFieldType.STRING))
         .withField(new QFieldMetaData("population", QFieldType.INTEGER))
         .withBackendDetails(new EnumerationTableBackendDetails().withEnumClass(EnumerationQueryActionTest.States.class));
   }
}
