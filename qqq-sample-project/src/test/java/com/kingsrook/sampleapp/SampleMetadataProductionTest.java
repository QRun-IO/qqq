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


import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.loaders.MetaDataLoaderHelper;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.fixturemetadata.InvalidAnnotatedEntity;
import com.kingsrook.sampleapp.fixturemetadata.LabChild;
import com.kingsrook.sampleapp.fixturemetadata.LabParent;
import com.kingsrook.sampleapp.fixturemetadata.LabStatus;
import com.kingsrook.sampleapp.fixturemetadata.LabTablesMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Sample-owned metadata production and entity conversion using disposable data.
 *******************************************************************************/
public class SampleMetadataProductionTest
{
   private static final String JOIN_NAME = QJoinMetaData.makeInferredJoinName(LabParent.TABLE_NAME, LabChild.TABLE_NAME);
   private QInstance instance;



   /*******************************************************************************
    ** Keep the fixture package outside the application's normal metadata scan.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      MemoryRecordStore.getInstance().reset();
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addBackend(new QBackendMetaData().withName("metadataLab").withBackendType(MemoryBackendModule.class));
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(instance, LabParent.class.getPackageName(), (context, table) ->
      {
         table.setBackendName("metadataLab");
         if(table.getName().equals(LabParent.TABLE_NAME))
         {
            table.withAssociation(new Association().withName("children").withAssociatedTableName(LabChild.TABLE_NAME).withJoinName(JOIN_NAME));
         }
         return table;
      });
      new QInstanceValidator().revalidate(instance);
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      MemoryRecordStore.getInstance().reset();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Generated tables must be usable by actions, not just present in a registry.
    *******************************************************************************/
   @Test
   void testScannedAnnotationsAndMultipleOutputs() throws Exception
   {
      QTableMetaData parent = instance.getTable(LabParent.TABLE_NAME);
      assertEquals("id", parent.getPrimaryKeyField());
      assertEquals("Parent Name", parent.getField("name").getLabel());
      assertTrue(parent.getField("name").getIsRequired());
      assertEquals(60, parent.getField("name").getMaxLength());
      assertFalse(parent.getField("id").getIsEditable());
      assertEquals(QFieldType.INTEGER, parent.getField("status").getType());
      assertEquals("LabStatus", parent.getField("status").getPossibleValueSourceName());
      assertNotNull(instance.getPossibleValueSource(LabParent.TABLE_NAME));
      assertEquals(List.of(new QPossibleValue<>(1, "Open"), new QPossibleValue<>(2, "Closed")),
         instance.getPossibleValueSource(LabStatus.class.getSimpleName()).getEnumValues());
      var join = instance.getJoin(JOIN_NAME);
      assertEquals(LabParent.TABLE_NAME, join.getLeftTable());
      assertEquals(LabChild.TABLE_NAME, join.getRightTable());
      assertEquals("id", join.getJoinOns().get(0).getLeftField());
      assertEquals("parentId", join.getJoinOns().get(0).getRightField());
      var widget = instance.getWidget(JOIN_NAME);
      assertEquals("Lab Children", widget.getLabel());
      assertEquals(JOIN_NAME, widget.getDefaultValues().get("joinName"));
      assertEquals(5, widget.getDefaultValues().get("maxRows"));
      assertNull(instance.getTable(InvalidAnnotatedEntity.TABLE_NAME));

      for(String name : List.of(LabParent.TABLE_NAME, LabTablesMetaDataProducer.NAME + "First", LabTablesMetaDataProducer.NAME + "Second"))
      {
         QRecord inserted = new InsertAction().executeForRecord(new InsertInput().withTableName(name)
            .withRecord(new QRecord().withValue("name", "Produced " + name)));
         assertTrue(inserted.getErrors().isEmpty());
         assertEquals("Produced " + name, GetAction.execute(name, inserted.getValue("id")).getValueString("name"));
      }
   }



   /*******************************************************************************
    ** Serialize authored table metadata, load it again and use its fields in actions.
    *******************************************************************************/
   @Test
   void testConfigurationJsonRoundTrip() throws Exception
   {
      QTableMetaData original = instance.getTable(LabTablesMetaDataProducer.NAME + "First");
      JSONObject json = new JSONObject(JsonUtils.toJson(original))
         .put("class", "QTableMetaData").put("version", "1.0").put("name", "metadataLabReloaded");
      QTableMetaData loaded;
      try(var input = new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)))
      {
         loaded = assertInstanceOf(QTableMetaData.class, MetaDataLoaderHelper.readMetaDataFile(instance, input, "metadata-lab.json"));
      }
      assertEquals(original.getPrimaryKeyField(), loaded.getPrimaryKeyField());
      assertEquals(original.getBackendName(), loaded.getBackendName());
      assertEquals(original.getFields().keySet(), loaded.getFields().keySet());
      for(String field : original.getFields().keySet())
      {
         assertEquals(original.getField(field).getType(), loaded.getField(field).getType());
         assertEquals(original.getField(field).getLabel(), loaded.getField(field).getLabel());
      }
      instance.addTable(loaded);
      new QInstanceValidator().revalidate(instance);
      QRecord inserted = new InsertAction().executeForRecord(new InsertInput().withTableName(loaded.getName())
         .withRecord(new QRecord().withValue("name", "Loaded configuration")));
      assertTrue(inserted.getErrors().isEmpty());
      assertEquals("Loaded configuration", GetAction.execute(loaded.getName(), inserted.getValue("id")).getValueString("name"));
   }



   /*******************************************************************************
    ** Association records convert in both directions alongside scalar fields.
    *******************************************************************************/
   @Test
   void testEntityAssociationRoundTripAndChangedFields() throws Exception
   {
      QRecord source = new QRecord().withTableName(LabParent.TABLE_NAME)
         .withValue("id", 7).withValue("name", "Original").withValue("status", 1)
         .withAssociatedRecord("children", new QRecord().withValue("id", 8).withValue("parentId", 7).withValue("note", "Child note"));
      LabParent entity = QRecordEntity.fromQRecord(LabParent.class, source);
      assertEquals(7, entity.getId());
      assertEquals("Original", entity.getName());
      assertEquals(1, entity.getStatus());
      assertEquals(1, entity.getChildren().size());
      assertEquals("Child note", entity.getChildren().get(0).getNote());
      assertTrue(entity.toQRecordOnlyChangedFields(false).getValues().isEmpty());
      entity.withName("Changed");
      assertEquals(Map.of("id", 7, "name", "Changed"), entity.toQRecordOnlyChangedFields(true).getValues());
      QRecord result = entity.toQRecord();
      assertEquals(LabParent.TABLE_NAME, result.getTableName());
      assertEquals("Changed", result.getValueString("name"));
      assertEquals(source.getAssociatedRecords().get("children").get(0).getValues(), result.getAssociatedRecords().get("children").get(0).getValues());
      assertEquals("Original", source.getValueString("name"));
      entity.withChildren(List.of());
      assertTrue(entity.toQRecord().getAssociatedRecords().get("children").isEmpty());
   }



   /*******************************************************************************
    ** Typed accessors preserve value types independently of display/backend details.
    *******************************************************************************/
   @Test
   void testTypedValuesDisplayAndBackendDetails() throws Exception
   {
      byte[] bytes = new byte[] { 0, 1, -1 };
      QRecord record = new QRecord().withValue("string", 42).withValue("integer", "17")
         .withValue("long", Long.toString(Long.MAX_VALUE)).withValue("decimal", "12345678901234567890.12345")
         .withValue("boolean", "true").withValue("date", "2026-09-23").withValue("time", "12:34:56")
         .withValue("instant", "2026-09-23T12:34:56Z").withValue("bytes", bytes)
         .withDisplayValue("integer", "Seventeen").withBackendDetail("source", "sample fixture");
      assertEquals("42", record.getValueString("string"));
      assertEquals(17, record.getValueInteger("integer"));
      assertEquals(Long.MAX_VALUE, record.getValueLong("long"));
      assertEquals(new BigDecimal("12345678901234567890.12345"), record.getValueBigDecimal("decimal"));
      assertTrue(record.getValueBoolean("boolean"));
      assertEquals(LocalDate.of(2026, 9, 23), record.getValueLocalDate("date"));
      assertEquals(LocalTime.of(12, 34, 56), record.getValueLocalTime("time"));
      assertEquals(Instant.parse("2026-09-23T12:34:56Z"), record.getValueInstant("instant"));
      assertArrayEquals(bytes, record.getValueByteArray("bytes"));
      assertEquals("Seventeen", record.getDisplayValue("integer"));
      assertEquals("sample fixture", record.getBackendDetail("source"));
      assertNull(record.getValue("source"));
      record.setValue("integer", 18);
      assertEquals(18, record.getValueInteger("integer"));
      assertEquals("Seventeen", record.getDisplayValue("integer"));
   }



   /*******************************************************************************
    ** Missing and explicit null values remain nullable in records and entities.
    *******************************************************************************/
   @Test
   void testNullableAndPrefixedEntityValues() throws Exception
   {
      QRecord empty = new QRecord().withValue("name", null);
      assertNull(empty.getValueInteger("missing"));
      assertNull(empty.getValueString("name"));
      LabParent entity = QRecordEntity.fromQRecord(LabParent.class, empty);
      assertNull(entity.getId());
      assertNull(entity.getName());
      assertNull(entity.getStatus());
      assertNull(entity.getChildren());
      assertTrue(entity.toQRecord().getValues().containsKey("name"));
      assertNull(entity.toQRecord().getValue("name"));
      QRecord joined = new QRecord().withValue("id", 99).withValue("parent.id", "7").withValue("parent.name", "Joined");
      LabParent parent = QRecordEntity.fromQRecord(LabParent.class, joined, "parent.");
      assertEquals(7, parent.getId());
      assertEquals("Joined", parent.getName());
      assertNull(parent.getStatus());
   }



   /*******************************************************************************
    ** Invalid coercions and a missing public no-argument constructor are rejected.
    *******************************************************************************/
   @Test
   void testInvalidCoercionAndEntityMapping()
   {
      QRecord invalid = new QRecord().withValue("id", "not-an-integer");
      assertThrows(QValueException.class, () -> invalid.getValueInteger("id"));
      assertThrows(QException.class, () -> QRecordEntity.fromQRecord(LabParent.class, invalid));
      assertThrows(QException.class, () -> QRecordEntity.fromQRecord(NoDefaultConstructor.class, new QRecord()));
      assertEquals("not-an-integer", invalid.getValueString("id"));
   }



   /*******************************************************************************
    ** Deliberately invalid entity construction contract.
    *******************************************************************************/
   public static class NoDefaultConstructor extends QRecordEntity
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      public NoDefaultConstructor(String unused)
      {
      }
   }
}
