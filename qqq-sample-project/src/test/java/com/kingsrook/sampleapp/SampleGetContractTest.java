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

package com.kingsrook.sampleapp;


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldFilterBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Get examples use the sample's declared tables and independently stored SQL.
 *******************************************************************************/
class SampleGetContractTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPrimaryKeysAndMissingRecords() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      QRecord record = new GetAction().execute(new GetInput("person").withPrimaryKey(1)).getRecord();
      assertEquals(1, record.getValueInteger("id"));
      assertEquals("Avery", record.getValueString("firstName"));
      assertEquals("avery@example.invalid", record.getValueString("email"));
      assertEquals(record.getValues(), GetAction.execute("person", 1).getValues());
      assertEquals(record.getValues(), new GetAction().executeForRecord(new GetInput("person").withPrimaryKey("1")).getValues());
      assertNull(GetAction.execute("person", 99999));
      assertNull(new GetAction().execute(new GetInput("person").withPrimaryKey(99999)).getRecord());
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeclaredUniqueKeysAndNormalization() throws Exception
   {
      QRecord inserted = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Get example").withValue("normalizedKey", " mixed-key "))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty());
      assertEquals(List.of(List.of("Get example", "MIXED-KEY")), rows("SELECT name,normalized_key FROM field_lab"));
      assertEquals(inserted.getValue("id"), GetAction.execute("fieldLab", Map.of("name", "Get example")).getValue("id"));
      assertEquals(inserted.getValue("id"), GetAction.execute("fieldLab", Map.of("normalizedKey", " mixed-KEY ")).getValue("id"));
      assertNull(GetAction.execute("fieldLab", Map.of("name", "Missing example")));
      instance.getTable("carrier").withUniqueKey(new UniqueKey("company_code", "service_level"));
      assertEquals(1, GetAction.execute("carrier", Map.of("company_code", "UPS", "service_level", "G")).getValueInteger("id"));
      assertNull(GetAction.execute("carrier", Map.of("company_code", "UPS", "service_level", "MISSING")));
      assertEquals(List.of(List.of("Get example", "MIXED-KEY")), rows("SELECT name,normalized_key FROM field_lab"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullableCompositeUniqueKeyAndMissingComponent() throws Exception
   {
      instance.getTable("fieldLab").withUniqueKey(new UniqueKey("textValue", "decimalValue"));
      QRecord decoy = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Nonnull Get key").withValue("textValue", "nullable-key").withValue("decimalValue", new BigDecimal("9.50")))).getRecords().get(0);
      assertTrue(decoy.getErrors().isEmpty());
      QRecord inserted = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Nullable Get key").withValue("textValue", "nullable-key").withValue("decimalValue", null))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty());
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      Map<String, Serializable> key = new HashMap<>();
      key.put("textValue", "nullable-key");
      key.put("decimalValue", null);
      assertEquals(inserted.getValue("id"), GetAction.execute("fieldLab", key).getValue("id"));
      assertTrue(key.containsKey("decimalValue"));
      assertNull(key.get("decimalValue"));
      assertEquals(decoy.getValue("id"), GetAction.execute("fieldLab", Map.of("textValue", "nullable-key", "decimalValue", new BigDecimal("9.50"))).getValue("id"));
      assertNull(GetAction.execute("fieldLab", Map.of("textValue", "nullable-key", "decimalValue", 0)));
      assertThrows(QException.class, () -> GetAction.execute("fieldLab", Map.of("textValue", "nullable-key")));
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidSelectorsCannotReturnAnArbitraryRecord() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      instance.getTable("carrier").withUniqueKey(new UniqueKey("company_code", "service_level"));
      for(GetInput input : List.of(new GetInput("person"), new GetInput("missing").withPrimaryKey(1),
         new GetInput("person").withPrimaryKey("invalid-number"),
         new GetInput("person").withPrimaryKey(1).withUniqueKey(Map.of("id", 2)),
         new GetInput("person").withUniqueKey(Map.of()),
         new GetInput("person").withUniqueKey(Map.of("missing", "value")),
         new GetInput("carrier").withUniqueKey(Map.of("company_code", "UPS")),
         new GetInput("carrier").withUniqueKey(Map.of("company_code", "UPS", "service_level", "G", "name", "UPS Ground"))))
      {
         assertThrows(QException.class, () -> new GetAction().execute(input));
      }
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDisplayAndPossibleValueOptions() throws Exception
   {
      QRecord raw = GetAction.execute("person", 1);
      assertFalse(raw.getDisplayValues().containsKey("annualSalary"));
      assertEquals(new BigDecimal("75003.50"), raw.getValue("annualSalary"));
      QRecord formatted = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1).withShouldGenerateDisplayValues(true));
      assertEquals("$75,003.50", formatted.getDisplayValue("annualSalary"));
      assertEquals("1,001", formatted.getDisplayValue("daysWorked"));
      assertEquals(raw.getValues(), formatted.getValues());
      QRecord pet = new GetAction().executeForRecord(new GetInput("pet").withPrimaryKey(1).withShouldTranslatePossibleValues(true));
      assertEquals("Avery Sample", pet.getDisplayValue("personId"));
      assertEquals("Dog", pet.getDisplayValue("speciesId"));
      assertEquals(1, pet.getValueInteger("personId"));
      assertEquals(1, pet.getValueInteger("speciesId"));
      assertFalse(GetAction.execute("pet", 1).getDisplayValues().containsKey("speciesId"));
   }



   /*******************************************************************************
    ** Missing execution context must reject even an otherwise valid selector.
    *******************************************************************************/
   @Test
   void testMissingSessionAndContextRejectWithoutMutation() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      GetInput input = new GetInput("person").withPrimaryKey(1);
      try
      {
         QContext.setQSession(null);
         assertEquals("QSession was not set in QContext.", assertThrows(QException.class, () -> new GetAction().execute(input)).getMessage());
         QContext.clear();
         assertEquals("QInstance was not set in QContext.", assertThrows(QException.class, () -> new GetAction().execute(input)).getMessage());
      }
      finally
      {
         QContext.init(instance, new QSession());
      }
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
      assertEquals("Avery", GetAction.execute("person", 1).getValueString("firstName"));
   }



   /*******************************************************************************
    ** Read options must reach actual children and grandchildren independently.
    *******************************************************************************/
   @Test
   void testReadOptionsReachChildrenAndGrandchildren() throws Exception
   {
      List<List<String>> beforePets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> beforeNotes = rows("SELECT * FROM pet_note ORDER BY id");
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      instance.getTable("pet").getField("id").setDisplayFormat("Pet %d");
      instance.getTable("petNote").getField("id").setDisplayFormat("Note %d");
      instance.getTable("petNote").getField("note").setIsHeavy(true);
      instance.getTable("petNote").getField("note").setIsHidden(true);

      QRecord defaults = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1).withIncludeAssociations(true));
      List<QRecord> defaultPets = defaults.getAssociatedRecords().get("pets");
      assertEquals(List.of(1, 2, 3, 4), defaultPets.stream().map(record -> record.getValueInteger("id")).sorted().toList());
      QRecord defaultPet = defaultPets.stream().filter(record -> record.getValueInteger("id").equals(1)).findFirst().orElseThrow();
      assertEquals("************", defaultPet.getValueString("name"));
      assertFalse(defaultPet.getDisplayValues().containsKey("id"));
      assertFalse(defaultPet.getDisplayValues().containsKey("speciesId"));
      List<QRecord> defaultNotes = defaultPet.getAssociatedRecords().get("notes");
      assertEquals(List.of(1), defaultNotes.stream().map(record -> record.getValueInteger("id")).toList());
      assertFalse(defaultNotes.get(0).getValues().containsKey("note"));
      assertFalse(defaultNotes.get(0).getDisplayValues().containsKey("id"));

      GetInput completeInput = new GetInput("person").withPrimaryKey(1).withIncludeAssociations(true)
         .withShouldFetchHeavyFields(true).withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)
         .withShouldGenerateDisplayValues(true).withShouldTranslatePossibleValues(true);
      QRecord complete = new GetAction().executeForRecord(completeInput);
      List<QRecord> completePets = complete.getAssociatedRecords().get("pets");
      assertEquals(List.of(1, 2, 3, 4), completePets.stream().map(record -> record.getValueInteger("id")).sorted().toList());
      QRecord completePet = completePets.stream().filter(record -> record.getValueInteger("id").equals(1)).findFirst().orElseThrow();
      assertEquals("Charlie", completePet.getValueString("name"));
      assertEquals("Pet 1", completePet.getDisplayValue("id"));
      assertEquals("Dog", completePet.getDisplayValue("speciesId"));
      List<QRecord> completeNotes = completePet.getAssociatedRecords().get("notes");
      assertEquals(List.of(1), completeNotes.stream().map(record -> record.getValueInteger("id")).toList());
      assertEquals("Target note", completeNotes.get(0).getValueString("note"));
      assertEquals("Note 1", completeNotes.get(0).getDisplayValue("id"));

      QRecord light = new GetAction().executeForRecord(completeInput.withShouldFetchHeavyFields(false));
      List<QRecord> lightPets = light.getAssociatedRecords().get("pets");
      assertEquals(List.of(1, 2, 3, 4), lightPets.stream().map(record -> record.getValueInteger("id")).sorted().toList());
      QRecord lightPet = lightPets.stream().filter(record -> record.getValueInteger("id").equals(1)).findFirst().orElseThrow();
      List<QRecord> lightNotes = lightPet.getAssociatedRecords().get("notes");
      assertEquals(List.of(1), lightNotes.stream().map(record -> record.getValueInteger("id")).toList());
      assertFalse(lightNotes.get(0).getValues().containsKey("note"));
      assertEquals(beforePets, rows("SELECT * FROM pet ORDER BY id"));
      assertEquals(beforeNotes, rows("SELECT * FROM pet_note ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHeavyHiddenAndPasswordOptionsLeaveStorageUntouched() throws Exception
   {
      instance.getTable("fieldLab").getField("htmlValue").setIsHidden(true);
      instance.getTable("fieldLab").getField("textValue").setIsHeavy(true);
      instance.getTable("fieldLab").getField("blobValue").setIsHeavy(true);
      byte[] bytes = new byte[] { 0, 1, -1, -128, 65, 0 };
      QRecord inserted = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Get privacy").withValue("textValue", "Heavy text").withValue("htmlValue", "Private HTML")
         .withValue("passwordValue", "synthetic-password").withValue("blobValue", bytes))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty());
      Integer id = inserted.getValueInteger("id");
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      QRecord defaults = GetAction.execute("fieldLab", id);
      assertEquals("Heavy text", defaults.getValueString("textValue"));
      assertArrayEquals(bytes, (byte[]) defaults.getValue("blobValue"));
      assertFalse(defaults.getValues().containsKey("htmlValue"));
      assertEquals("************", defaults.getValue("passwordValue"));
      QRecord light = new GetAction().executeForRecord(new GetInput("fieldLab").withPrimaryKey(id).withShouldFetchHeavyFields(false));
      assertFalse(light.getValues().containsKey("textValue"));
      assertFalse(light.getValues().containsKey("blobValue"));
      assertFalse(light.getValues().containsKey("htmlValue"));
      QRecord complete = new GetAction().executeForRecord(new GetInput("fieldLab").withPrimaryKey(id)
         .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false));
      assertEquals("Private HTML", complete.getValueString("htmlValue"));
      assertEquals("synthetic-password", complete.getValueString("passwordValue"));
      assertArrayEquals(bytes, (byte[]) complete.getValue("blobValue"));
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNamedAssociationSelectionAndEmptyGroups() throws Exception
   {
      assertTrue(GetAction.execute("person", 1).getAssociatedRecords().isEmpty());
      QRecord none = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1)
         .withIncludeAssociations(true).withAssociationNamesToInclude(Set.of()));
      assertTrue(none.getAssociatedRecords().isEmpty());
      QRecord direct = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1)
         .withIncludeAssociations(true).withAssociationNamesToInclude(Set.of("pets")));
      assertEquals(Set.of("pets"), direct.getAssociatedRecords().keySet());
      assertEquals(List.of(1, 2, 3, 4), direct.getAssociatedRecords().get("pets").stream().map(record -> record.getValueInteger("id")).sorted().toList());
      assertTrue(direct.getAssociatedRecords().get("pets").stream().allMatch(record -> record.getAssociatedRecords().isEmpty()));
      QRecord expanded = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1)
         .withIncludeAssociations(true).withAssociationNamesToInclude(Set.of("pets", "pets.notes")));
      assertEquals(Set.of("pets"), expanded.getAssociatedRecords().keySet());
      List<QRecord> pets = expanded.getAssociatedRecords().get("pets");
      assertEquals(List.of(1, 2, 3, 4), pets.stream().map(record -> record.getValueInteger("id")).sorted().toList());
      for(QRecord pet : pets)
      {
         assertEquals(Set.of("notes"), pet.getAssociatedRecords().keySet());
         if(pet.getValueInteger("id").equals(1))
         {
            assertEquals(List.of("Target note"), pet.getAssociatedRecords().get("notes").stream().map(record -> record.getValueString("note")).toList());
         }
         else
         {
            assertTrue(pet.getAssociatedRecords().get("notes").isEmpty());
         }
      }
      assertTrue(new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(4).withIncludeAssociations(true)).getAssociatedRecords().get("pets").isEmpty());
      assertEquals(List.of(List.of("1", "Target note"), List.of("5", "Other parent note")), rows("SELECT pet_id,note FROM pet_note ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRowSecurityAppliesToPrimaryAndUniqueKeys() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      instance.addSecurityKeyType(new QSecurityKeyType().withName("getPerson"));
      instance.getTable("person").withUniqueKey(new UniqueKey("email"))
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("getPerson").withFieldName("firstName"));
      QContext.setQSession(new QSession().withSecurityKeyValue("getPerson", "Avery"));
      assertEquals(1, GetAction.execute("person", 1).getValueInteger("id"));
      assertEquals(1, GetAction.execute("person", Map.of("email", "avery@example.invalid")).getValueInteger("id"));
      assertNull(GetAction.execute("person", 2));
      assertNull(GetAction.execute("person", Map.of("email", "blair@example.invalid")));
      QContext.setQSession(new QSession());
      assertNull(GetAction.execute("person", 1));
      assertNull(GetAction.execute("person", Map.of("email", "avery@example.invalid")));
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExplicitJoinReturnsOnlyTheRequestedRelation() throws Exception
   {
      GetInput input = new GetInput("pet").withPrimaryKey(1).withInputSource(QInputSource.USER)
         .withQueryJoin(new QueryJoin("person").withSelect(true));
      QRecord record = new GetAction().executeForRecord(input);
      assertEquals("Charlie", record.getValueString("name"));
      assertEquals("Avery", record.getValueString("person.firstName"));
      assertEquals(1, record.getValueInteger("person.id"));
      assertTrue(record.getAssociatedRecords().isEmpty());
      instance.addSecurityKeyType(new QSecurityKeyType().withName("getOwner"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("getOwner").withFieldName("firstName"));
      QContext.setQSession(new QSession().withSecurityKeyValue("getOwner", "Blair"));
      assertNull(new GetAction().executeForRecord(input));
      assertEquals(List.of(List.of("Charlie", "Avery")), rows("SELECT p.name,o.first_name FROM pet p JOIN person o ON p.person_id=o.id WHERE p.id=1"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetUsesCallerTransactionWithoutCommittingOrClosingIt() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend())))
      {
         try(Statement statement = transaction.getConnection().createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE person SET first_name='Uncommitted Get' WHERE id=1"));
         }
         QRecord inside = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1).withTransaction(transaction));
         assertEquals("Uncommitted Get", inside.getValueString("firstName"));
         assertEquals("Avery", GetAction.execute("person", 1).getValueString("firstName"));
         assertFalse(transaction.getConnection().isClosed());
         assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
         transaction.rollback();
      }
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
      assertEquals("Avery", GetAction.execute("person", 1).getValueString("firstName"));
   }



   /*******************************************************************************
    ** A field-aware converter must receive the unique-key component's metadata.
    *******************************************************************************/
   @Test
   void testUniqueKeyBehaviorUsesItsOwnFieldType() throws Exception
   {
      QRecord decoy = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Integer Get decoy").withValue("decimalValue", new BigDecimal("9")))).getRecords().get(0);
      assertTrue(decoy.getErrors().isEmpty());
      QRecord inserted = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Decimal Get key").withValue("decimalValue", new BigDecimal("9.50")))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty());
      instance.getTable("fieldLab").withUniqueKey(new UniqueKey("decimalValue"));
      instance.getTable("fieldLab").getField("decimalValue").withBehavior(NativeFieldTypeFilter.CONVERT);
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      QRecord found = GetAction.execute("fieldLab", Map.of("decimalValue", "9.50"));
      assertNotNull(found);
      assertEquals(inserted.getValue("id"), found.getValue("id"));
      assertEquals(0, new BigDecimal("9.50").compareTo(found.getValueBigDecimal("decimalValue")));
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Filter-only example using the field type declared by the application.
    *******************************************************************************/
   private enum NativeFieldTypeFilter implements FieldFilterBehavior<NativeFieldTypeFilter>
   {
      NONE,
      CONVERT;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public NativeFieldTypeFilter getDefault()
      {
         return NONE;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable applyToFilterCriteriaValue(Serializable value, QInstance instance, QTableMetaData table, QFieldMetaData field)
      {
         if(this == CONVERT)
         {
            assertEquals("decimalValue", field.getName());
            assertEquals(QFieldType.DECIMAL, field.getType());
         }
         return this == NONE ? value : ValueUtils.getValueAsFieldType(field.getType(), value);
      }



      /*******************************************************************************
       ** This example only normalizes lookup operands, not stored or returned data.
       *******************************************************************************/
      @Override
      public void apply(ValueBehaviorApplier.Action action, List<QRecord> records, QInstance instance, QTableMetaData table, QFieldMetaData field)
      {
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> rows(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery(sql))
      {
         List<List<String>> rows = new ArrayList<>();
         while(result.next())
         {
            List<String> values = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               values.add(result.getString(column));
            }
            rows.add(values);
         }
         return rows;
      }
   }
}
