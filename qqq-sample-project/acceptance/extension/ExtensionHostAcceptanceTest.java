/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.qrunio.acceptance.extension;

import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** Exercises a separately built generated QBit in a host with actual DML. */
class ExtensionHostAcceptanceTest
{
   private QInstance instance;


   @BeforeEach
   void setup()
   {
      MemoryRecordStore.getInstance().reset();
      instance = new QInstance();
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData()
         .withName("mock").withType(QAuthenticationType.MOCK));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.addTable(table("order", true));
      instance.addTable(table("unrelated", true));
      QContext.init(instance, new QSystemUserSession());
   }


   @AfterEach
   void cleanup()
   {
      QContext.clear();
      MemoryRecordStore.getInstance().reset();
   }


   @Test
   void generatedExtensionChangesOnlyConfiguredHostTable() throws Exception
   {
      produce(true);
      assertEquals(1, instance.getQBits().size());
      assertNotNull(instance.getTable("order").getCustomizers());
      assertNull(instance.getTable("unrelated").getCustomizers());

      insert("order", "one", "alpha");
      insert("unrelated", "two", "beta");
      assertEquals("extended:alpha", GetAction.execute("order", "one").getValueString("name"));
      assertEquals("beta", GetAction.execute("unrelated", "two").getValueString("name"));
   }


   @Test
   void disabledConfigurationLeavesHostBehaviorUnchanged() throws Exception
   {
      produce(false);
      assertNull(instance.getTable("order").getCustomizers());
      insert("order", "one", "alpha");
      assertEquals("alpha", GetAction.execute("order", "one").getValueString("name"));
   }


   @Test
   void missingTargetAndMissingRequiredFieldFailBeforeRegistration() throws Exception
   {
      assertThrows(QBitConfigValidationException.class, () ->
         new OrderDeskExtensionQBitProducer().withConfig(new OrderDeskExtensionQBitConfig()
            .withTargetTableName("absent")).produce(instance, "proof"));
      assertThrows(QBitConfigValidationException.class, () ->
         new OrderDeskExtensionQBitProducer().withConfig(new OrderDeskExtensionQBitConfig())
            .produce(instance, "proof"));
      instance.addTable(table("noName", false));
      assertThrows(QBitConfigValidationException.class, () ->
         new OrderDeskExtensionQBitProducer().withConfig(new OrderDeskExtensionQBitConfig()
            .withTargetTableName("noName")).produce(instance, "proof"));
      assertTrue(instance.getQBits().isEmpty());
      assertNull(instance.getTable("order").getCustomizers());
      assertNull(instance.getTable("unrelated").getCustomizers());
   }


   private void produce(boolean enabled) throws Exception
   {
      new OrderDeskExtensionQBitProducer().withConfig(new OrderDeskExtensionQBitConfig()
         .withTargetTableName("order").withEnableFeatureX(enabled)).produce(instance, "proof");
   }


   private void insert(String tableName, String id, String name) throws Exception
   {
      new InsertAction().execute(new InsertInput(tableName)
         .withRecord(new QRecord().withValue("id", id).withValue("name", name)));
   }


   private QTableMetaData table(String name, boolean withName)
   {
      QTableMetaData table = new QTableMetaData().withName(name).withLabel(name)
         .withBackendName("memory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.STRING));
      if(withName)
      {
         table.withField(new QFieldMetaData("name", QFieldType.STRING));
      }
      return table;
   }
}
