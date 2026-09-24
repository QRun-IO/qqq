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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Read privacy uses the canonical sample tables and independent native snapshots.
 ** Field Lab has no associations: its association mode tests the buffered delivery
 ** path only. A separate Person/Pet/PetNote case exercises actual nested reads.
 *******************************************************************************/
class SampleQueryPrivacyTest
{
   private static final String MASK = "************";

   private QInstance instance;

   /*******************************************************************************
    **
    *******************************************************************************/
   private enum Delivery
   {
      DIRECT, PLAIN_PIPE, ASSOCIATION_BUFFER
   }



   /*******************************************************************************
    ** Seed native values so read assertions do not depend on INSERT normalization.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         assertEquals(2, statement.executeUpdate("INSERT INTO field_lab (name,text_value,html_value,password_value) VALUES "
            + "('First source','original-one','private-one','synthetic-one'),"
            + "('Second source','original-two','private-two','synthetic-two')"));
      }
      hideField("fieldLab", "htmlValue");
      assertEquals(List.of(List.of("1", "synthetic-one", "private-one"), List.of("2", "synthetic-two", "private-two")),
         rows("SELECT id,password_value,html_value FROM field_lab ORDER BY id"));
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
   void testDirectReplacementRecordsArePublishedAndSanitized() throws Exception
   {
      assertClonedRecords(Delivery.DIRECT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPlainPipeReplacementRecordsArePublishedAndSanitized() throws Exception
   {
      assertClonedRecords(Delivery.PLAIN_PIPE);
   }



   /*******************************************************************************
    ** Ordinary native delivery preserves source rows and the caller's context.
    *******************************************************************************/
   @Test
   void testPlainPipeDeliversNativeRowsWithoutChangingSource() throws Exception
   {
      List<List<String>> nativeBefore = rows("SELECT * FROM field_lab ORDER BY id");
      List<List<String>> unrelatedBefore = rows("SELECT * FROM person ORDER BY id");
      String metadataBefore = JsonUtils.toJson(instance.getTables());
      String sessionBefore = JsonUtils.toJson(QContext.getQSession());
      QueryInput input = fieldLabInput();
      input.setIncludeAssociations(false);
      String filterBefore = JsonUtils.toJson(input.getFilter());
      List<QRecord> records = query(input, Delivery.PLAIN_PIPE, 2);
      assertAll(
         () -> assertEquals(List.of(1, 2), ids(records, "id")),
         () -> assertEquals(nativeBefore, rows("SELECT * FROM field_lab ORDER BY id")),
         () -> assertEquals(unrelatedBefore, rows("SELECT * FROM person ORDER BY id")),
         () -> assertEquals(metadataBefore, JsonUtils.toJson(instance.getTables())),
         () -> assertEquals(filterBefore, JsonUtils.toJson(input.getFilter())),
         () -> assertEquals(sessionBefore, JsonUtils.toJson(QContext.getQSession())));
   }



   /*******************************************************************************
    ** Field Lab has no nested model; this specifically checks the wrapper's tail.
    *******************************************************************************/
   @Test
   void testBufferedWrapperReplacementRecordsArePublishedAndSanitized() throws Exception
   {
      assertClonedRecords(Delivery.ASSOCIATION_BUFFER);
   }



   /*******************************************************************************
    ** A returned view can share storage with the input list.
    *******************************************************************************/
   @Test
   void testSubListReplacementRetainsRowsInEveryDeliveryMode()
   {
      customize("fieldLab", SubListRecords.class);
      assertAll(Arrays.stream(Delivery.values()).map(mode -> (Executable) () ->
      {
         List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
         List<QRecord> records = query(fieldLabInput(), mode);
         assertEquals(List.of(1, 2), ids(records, "id"), mode.name());
         assertFieldLabPrivacy(records);
         assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      }));
   }



   /*******************************************************************************
    ** Direct and buffered customizers may suppress every result.
    *******************************************************************************/
   @Test
   void testEmptyReplacementInDirectAndBufferedDelivery()
   {
      customize("fieldLab", EmptyRecords.class);
      assertAll(List.of(Delivery.DIRECT, Delivery.ASSOCIATION_BUFFER).stream().map(mode -> (Executable) () ->
      {
         List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
         assertEquals(List.of(), query(fieldLabInput(), mode), mode.name());
         assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      }));
   }



   /*******************************************************************************
    ** Direct and buffered delivery publish both sanitized copies.
    *******************************************************************************/
   @Test
   void testExpandedReplacementInDirectAndBufferedDelivery()
   {
      customize("fieldLab", ExpandedRecords.class);
      assertAll(List.of(Delivery.DIRECT, Delivery.ASSOCIATION_BUFFER).stream().map(mode -> (Executable) () ->
      {
         List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
         List<QRecord> records = query(fieldLabInput(), mode);
         assertEquals(List.of(1, 1, 2, 2), ids(records, "id"), mode.name());
         assertEquals(List.of("First source A", "First source B", "Second source A", "Second source B"),
            records.stream().map(record -> record.getValueString("textValue")).toList());
         assertFieldLabPrivacy(records);
         assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      }));
   }



   /*******************************************************************************
    ** A failing callback must reach the caller and leave no producer behind.
    *******************************************************************************/
   @Test
   void testCustomizerFailurePropagatesInEveryDeliveryMode()
   {
      customize("fieldLab", FailingRecords.class);
      assertAll(Arrays.stream(Delivery.values()).map(mode -> (Executable) () ->
      {
         List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
         QException error = assertThrows(QException.class, () -> query(fieldLabInput(), mode));
         assertFailureMessage(error, "Synthetic read failure");
         assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      }));
   }



   /*******************************************************************************
    ** Null is an invalid customizer result, not a successful empty read.
    *******************************************************************************/
   @Test
   void testNullCustomizerResultFailsInEveryDeliveryMode()
   {
      customize("fieldLab", NullRecords.class);
      assertAll(Arrays.stream(Delivery.values()).map(mode -> (Executable) () ->
      {
         List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
         QException error = assertThrows(QException.class, () -> query(fieldLabInput(), mode));
         assertFailureMessage(error, "Post-query customizer returned null records");
         assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      }));
   }



   /*******************************************************************************
    ** Selectable customizer-backed virtual fields use declared privacy metadata.
    *******************************************************************************/
   @Test
   void testVirtualPasswordAndHiddenValuesUseDeclaredPrivacy() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      instance.getTable("fieldLab")
         .withVirtualField(new QVirtualFieldMetaData("virtualPassword", QFieldType.PASSWORD).withIsQuerySelectable(true))
         .withVirtualField(new QVirtualFieldMetaData("virtualHidden", QFieldType.STRING).withIsQuerySelectable(true).withIsHidden(true));
      customize("fieldLab", VirtualPrivateValues.class);
      List<QRecord> defaults = query(fieldLabInput(), Delivery.DIRECT);
      assertEquals(List.of(1, 2), ids(defaults, "id"));
      for(QRecord record : defaults)
      {
         assertEquals(MASK, record.getValueString("virtualPassword"));
         assertEquals(MASK, record.getDisplayValue("virtualPassword"));
         assertAbsent(record, "virtualHidden");
      }
      List<QRecord> unmasked = query(fieldLabInput().withShouldMaskPasswords(false).withShouldOmitHiddenFields(false), Delivery.DIRECT);
      assertEquals(List.of(1, 2), ids(unmasked, "id"));
      assertEquals(List.of("synthetic-one", "synthetic-two"), unmasked.stream().map(record -> record.getValueString("virtualPassword")).toList());
      assertEquals(List.of("private-one", "private-two"), unmasked.stream().map(record -> record.getValueString("virtualHidden")).toList());
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Actual nested reads must attach children to the published replacement root.
    *******************************************************************************/
   @Test
   void testBufferedReplacementWithRealChildrenAndGrandchildren() throws Exception
   {
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> notes = rows("SELECT * FROM pet_note ORDER BY id");
      instance.getTable("person").getField("firstName").setType(QFieldType.PASSWORD);
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      hideField("person", "lastName");
      customize("person", ClonedPeople.class);
      QueryInput input = new QueryInput("person").withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)))
         .withShouldGenerateDisplayValues(true).withShouldFetchHeavyFields(true);
      List<QRecord> records = query(input, Delivery.ASSOCIATION_BUFFER);
      assertEquals(List.of(1), ids(records, "id"));
      QRecord person = records.get(0);
      assertEquals("copied@example.invalid", person.getValueString("email"));
      assertEquals(MASK, person.getValueString("firstName"));
      assertEquals(MASK, person.getDisplayValue("firstName"));
      assertAbsent(person, "lastName");
      assertEquals(Set.of("pets"), person.getAssociatedRecords().keySet());
      List<QRecord> children = person.getAssociatedRecords().get("pets");
      assertEquals(List.of(1, 2, 3, 4), ids(children, "id").stream().sorted().toList());
      for(QRecord child : children)
      {
         assertEquals(MASK, child.getValueString("name"));
         assertEquals(Set.of("notes"), child.getAssociatedRecords().keySet());
         List<QRecord> grandchildren = child.getAssociatedRecords().get("notes");
         if(child.getValueInteger("id").equals(1))
         {
            assertEquals(List.of(1), ids(grandchildren, "id"));
            assertEquals("Target note", grandchildren.get(0).getValueString("note"));
         }
         else
         {
            assertEquals(List.of(), grandchildren);
         }
      }
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"));
      assertEquals(notes, rows("SELECT * FROM pet_note ORDER BY id"));
   }



   /*******************************************************************************
    ** Selected joined fields follow the same defaults, overrides and REVEAL rule.
    *******************************************************************************/
   @Test
   void testJoinedAliasPrivacyAndExplicitOverrides() throws Exception
   {
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      assertEquals(List.of(List.of("1", "Charlie", "1"), List.of("2", "Coco", "1"), List.of("3", "Louie", "1"), List.of("4", "Barkley", "1")),
         rows("SELECT id,name,species_id FROM pet WHERE person_id=1 ORDER BY id"));
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      hideField("pet", "speciesId");

      List<QRecord> defaults = query(joinedPeopleInput(), Delivery.DIRECT);
      assertEquals(List.of(1, 2, 3, 4), ids(defaults, "animal.id"));
      for(QRecord record : defaults)
      {
         assertEquals(1, record.getValueInteger("id"));
         assertEquals(MASK, record.getValueString("animal.name"));
         assertEquals(MASK, record.getDisplayValue("animal.name"));
         assertAbsent(record, "animal.speciesId");
      }

      List<QRecord> unmasked = query(joinedPeopleInput().withShouldMaskPasswords(false).withShouldOmitHiddenFields(false), Delivery.DIRECT);
      assertEquals(List.of(1, 2, 3, 4), ids(unmasked, "animal.id"));
      assertEquals(List.of("Charlie", "Coco", "Louie", "Barkley"), unmasked.stream().map(record -> record.getValueString("animal.name")).toList());
      assertEquals(List.of(1, 1, 1, 1), ids(unmasked, "animal.speciesId"));
      assertEquals(List.of("Dog", "Dog", "Dog", "Dog"), unmasked.stream().map(record -> record.getDisplayValue("animal.speciesId")).toList());

      instance.getTable("pet").getField("name").withFieldAdornment(new FieldAdornment().withType(AdornmentType.REVEAL));
      List<QRecord> revealed = query(joinedPeopleInput(), Delivery.DIRECT);
      assertEquals(List.of("Charlie", "Coco", "Louie", "Barkley"), revealed.stream().map(record -> record.getValueString("animal.name")).toList());
      revealed.forEach(record -> assertAbsent(record, "animal.speciesId"));
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"));
   }



   /*******************************************************************************
    ** Get's joined fallback uses a unique Pet/Person row rather than an arbitrary
    ** first child from a one-to-many join.
    *******************************************************************************/
   @Test
   void testGetJoinedAliasPrivacyAndExplicitOverrides() throws Exception
   {
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      assertEquals(List.of(List.of("Charlie", "Avery", "Sample")),
         rows("SELECT p.name,o.first_name,o.last_name FROM pet p JOIN person o ON p.person_id=o.id WHERE p.id=1"));
      instance.getTable("person").getField("firstName").setType(QFieldType.PASSWORD);
      hideField("person", "lastName");

      QRecord defaults = new GetAction().executeForRecord(joinedPetInput());
      assertNotNull(defaults);
      assertEquals(1, defaults.getValueInteger("id"));
      assertEquals(1, defaults.getValueInteger("owner.id"));
      assertEquals("Charlie", defaults.getValueString("name"));
      assertEquals(MASK, defaults.getValueString("owner.firstName"));
      assertEquals(MASK, defaults.getDisplayValue("owner.firstName"));
      assertAbsent(defaults, "owner.lastName");

      QRecord unmasked = new GetAction().executeForRecord(joinedPetInput().withShouldMaskPasswords(false).withShouldOmitHiddenFields(false));
      assertEquals("Avery", unmasked.getValueString("owner.firstName"));
      assertEquals("Sample", unmasked.getValueString("owner.lastName"));
      instance.getTable("person").getField("firstName").withFieldAdornment(new FieldAdornment().withType(AdornmentType.REVEAL));
      QRecord revealed = new GetAction().executeForRecord(joinedPetInput());
      assertEquals("Avery", revealed.getValueString("owner.firstName"));
      assertAbsent(revealed, "owner.lastName");
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"));
   }



   /*******************************************************************************
    ** Privacy cannot create excluded fields or retain display-only private values.
    *******************************************************************************/
   @Test
   void testExactBaseProjectionClearsDisplayOnlyPrivateValues()
   {
      customize("fieldLab", DisplayOnlyPrivateValues.class);
      assertAll(Arrays.stream(Delivery.values()).map(mode -> (Executable) () ->
      {
         List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
         List<QRecord> records = query(fieldLabInput().withShouldGenerateDisplayValues(false).withFieldNamesToInclude(Set.of("id", "name")), mode);
         assertEquals(List.of(1, 2), ids(records, "id"));
         for(QRecord record : records)
         {
            assertEquals(Set.of("id", "name"), record.getValues().keySet());
            assertAbsent(record, "passwordValue");
            assertAbsent(record, "htmlValue");
         }
         assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      }));
   }



   /*******************************************************************************
    ** Exact joined projections neither expose hidden values nor add an excluded
    ** base or joined password. Requested passwords remain present and masked.
    *******************************************************************************/
   @Test
   void testExactJoinedProjection() throws Exception
   {
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      instance.getTable("person").getField("firstName").setType(QFieldType.PASSWORD);
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      hideField("pet", "speciesId");
      List<QRecord> keys = query(joinedPeopleInput().withFieldNamesToInclude(Set.of("id", "animal.id")), Delivery.DIRECT);
      assertEquals(List.of(1, 2, 3, 4), ids(keys, "animal.id"));
      for(QRecord record : keys)
      {
         assertEquals(Set.of("id", "animal.id"), record.getValues().keySet());
         assertAbsent(record, "firstName");
         assertAbsent(record, "animal.name");
      }
      List<QRecord> selected = query(joinedPeopleInput().withFieldNamesToInclude(Set.of("id", "animal.id", "animal.name", "animal.speciesId")), Delivery.DIRECT);
      assertEquals(List.of(1, 2, 3, 4), ids(selected, "animal.id"));
      for(QRecord record : selected)
      {
         assertEquals(Set.of("id", "animal.id", "animal.name"), record.getValues().keySet());
         assertEquals(MASK, record.getValueString("animal.name"));
         assertEquals(MASK, record.getDisplayValue("animal.name"));
         assertAbsent(record, "firstName");
         assertAbsent(record, "animal.speciesId");
      }
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPrivateRecordLabelsAcrossEveryDeliveryMode() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      instance.getTable("person").withRecordLabelFormat("%s %s").withRecordLabelFields("firstName", "lastName");
      instance.getTable("person").getField("firstName").setType(QFieldType.PASSWORD);
      hideField("person", "lastName");
      assertAll(Arrays.stream(Delivery.values()).map(mode -> (Executable) () ->
      {
         QueryInput input = new QueryInput("person").withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)))
            .withShouldGenerateDisplayValues(true);
         List<QRecord> records = query(input, mode);
         assertEquals(List.of(1), ids(records, "id"));
         assertEquals(MASK + " ", records.get(0).getRecordLabel(), mode.name());
      }));
      assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
   }



   /*******************************************************************************
    ** Explicit read overrides and public labels retain their existing behavior.
    *******************************************************************************/
   @Test
   void testRecordLabelPrivacyOverridesAndPublicControl() throws Exception
   {
      instance.getTable("person").withRecordLabelFormat("%s %s").withRecordLabelFields("firstName", "lastName");
      GetInput input = new GetInput("person").withPrimaryKey(1).withShouldGenerateDisplayValues(true);
      assertEquals("Avery Sample", new GetAction().executeForRecord(input).getRecordLabel());
      instance.getTable("person").getField("firstName").setType(QFieldType.PASSWORD);
      hideField("person", "lastName");
      assertEquals("Avery Sample", new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1)
         .withShouldGenerateDisplayValues(true).withShouldMaskPasswords(false).withShouldOmitHiddenFields(false)).getRecordLabel());
      instance.getTable("person").getField("firstName").withFieldAdornment(new FieldAdornment().withType(AdornmentType.REVEAL));
      assertEquals("Avery ", new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1).withShouldGenerateDisplayValues(true)).getRecordLabel());
   }



   /*******************************************************************************
    ** Nested labels are part of the delivered record tree and must be sanitized.
    *******************************************************************************/
   @Test
   void testPrivateLabelsOnActualAssociatedRecords() throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM pet ORDER BY id");
      instance.getTable("pet").withRecordLabelFormat("%s").withRecordLabelFields("name");
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      QRecord person = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1)
         .withIncludeAssociations(true).withShouldGenerateDisplayValues(true));
      List<QRecord> pets = person.getAssociatedRecords().get("pets");
      assertEquals(List.of(1, 2, 3, 4), ids(pets, "id").stream().sorted().toList());
      assertAll(pets.stream().map(pet -> (Executable) () -> assertEquals(MASK, pet.getRecordLabel())));
      assertEquals(before, rows("SELECT * FROM pet ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFullAssociationBufferAndTailPreserveEverySanitizedTree() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement insert = connection.prepareStatement("INSERT INTO person (id,first_name,last_name,email) VALUES (?,?,?,?)");
          Statement statement = connection.createStatement())
      {
         for(int id = 1000; id <= 1100; id++)
         {
            insert.setInt(1, id);
            insert.setString(2, "Stream " + id);
            insert.setString(3, "Private family");
            insert.setString(4, "stream-" + id + "@example.invalid");
            assertEquals(1, insert.executeUpdate());
         }
         assertEquals(2, statement.executeUpdate("INSERT INTO pet (id,name,species_id,person_id) VALUES (2000,'Private pet first',1,1000),(2100,'Private pet tail',2,1100)"));
         assertEquals(2, statement.executeUpdate("INSERT INTO pet_note (id,pet_id,note) VALUES (3000,2000,'First batch note'),(3100,2100,'Tail batch note')"));
      }
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> notes = rows("SELECT * FROM pet_note ORDER BY id");
      List<Integer> nativeIds = rows("SELECT id FROM person WHERE id>=1000 ORDER BY id").stream().map(row -> Integer.valueOf(row.get(0))).toList();
      assertEquals(101, nativeIds.size());
      BatchClonedPeople.batches.clear();
      customize("person", BatchClonedPeople.class);
      instance.getTable("person").getField("firstName").setType(QFieldType.PASSWORD);
      instance.getTable("pet").getField("name").setType(QFieldType.PASSWORD);
      hideField("person", "lastName");
      List<QRecord> records = query(new QueryInput("person")
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 1000)).withOrderBy(new QFilterOrderBy("id")))
         .withShouldGenerateDisplayValues(true), Delivery.ASSOCIATION_BUFFER, 8);
      assertEquals(nativeIds, ids(records, "id"));
      assertEquals(List.of(100, 1), BatchClonedPeople.batches);
      for(QRecord record : records)
      {
         assertEquals("copied@example.invalid", record.getValueString("email"));
         assertEquals(MASK, record.getValueString("firstName"));
         assertAbsent(record, "lastName");
         assertEquals(MASK + " ", record.getRecordLabel());
         assertEquals(Set.of("pets"), record.getAssociatedRecords().keySet());
         List<QRecord> children = record.getAssociatedRecords().get("pets");
         if(record.getValueInteger("id").equals(1000) || record.getValueInteger("id").equals(1100))
         {
            int childId = record.getValueInteger("id") + 1000;
            assertEquals(List.of(childId), ids(children, "id"));
            QRecord child = children.get(0);
            assertEquals(MASK, child.getValueString("name"));
            assertEquals(MASK, child.getRecordLabel());
            assertEquals(Set.of("notes"), child.getAssociatedRecords().keySet());
            assertEquals(List.of(childId + 1000), ids(child.getAssociatedRecords().get("notes"), "id"));
            assertEquals(childId == 2000 ? "First batch note" : "Tail batch note", child.getAssociatedRecords().get("notes").get(0).getValueString("note"));
         }
         else
         {
            assertTrue(children.isEmpty());
         }
      }
      assertEquals(people, rows("SELECT * FROM person ORDER BY id"));
      assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"));
      assertEquals(notes, rows("SELECT * FROM pet_note ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertClonedRecords(Delivery delivery) throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      customize("fieldLab", ClonedRecords.class);
      List<QRecord> records = query(fieldLabInput(), delivery);
      assertAll(
         () -> assertEquals(List.of(1, 2), ids(records, "id")),
         () -> assertEquals(List.of("copied:First source", "copied:Second source"), records.stream().map(record -> record.getValueString("textValue")).toList()),
         () -> assertFieldLabPrivacy(records),
         () -> assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertFieldLabPrivacy(List<QRecord> records)
   {
      assertAll(records.stream().map(record -> (Executable) () -> assertAll(
         () -> assertEquals(MASK, record.getValueString("passwordValue")),
         () -> assertEquals(MASK, record.getDisplayValue("passwordValue")),
         () -> assertAbsent(record, "htmlValue"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertAbsent(QRecord record, String field)
   {
      assertAll(
         () -> assertFalse(record.getValues().containsKey(field), field + " raw value"),
         () -> assertFalse(record.getDisplayValues().containsKey(field), field + " display value"));
   }



   /*******************************************************************************
    ** Native pipe execution may wrap a callback's checked failure.
    *******************************************************************************/
   private void assertFailureMessage(Throwable error, String message)
   {
      Throwable cause = error;
      while(cause.getCause() != null)
      {
         cause = cause.getCause();
      }
      assertEquals(message, cause.getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void hideField(String tableName, String fieldName)
   {
      instance.getTable(tableName).getField(fieldName).setIsHidden(true);
      instance.getTable(tableName).getSections().forEach(section -> section.setFieldNames(section.getFieldNames().stream()
         .filter(name -> !name.equals(fieldName)).toList()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void customize(String tableName, Class<? extends TableCustomizerInterface> customizer)
   {
      instance.getTable(tableName).withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(customizer));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput fieldLabInput()
   {
      return new QueryInput("fieldLab").withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))
         .withShouldGenerateDisplayValues(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput joinedPeopleInput()
   {
      return new QueryInput("person").withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
            .withOrderBy(new QFilterOrderBy("animal.id")))
         .withQueryJoin(new QueryJoin("pet").withAlias("animal").withSelect(true))
         .withShouldGenerateDisplayValues(true).withShouldTranslatePossibleValues(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private GetInput joinedPetInput()
   {
      return new GetInput("pet").withPrimaryKey(1).withQueryJoin(new QueryJoin("person").withAlias("owner").withSelect(true))
         .withShouldGenerateDisplayValues(true).withShouldTranslatePossibleValues(true);
   }



   /*******************************************************************************
    ** Consume concurrently with an owned producer: a one-slot pipe must not hide
    ** expansion loss behind a generously sized queue. Always terminate and join.
    *******************************************************************************/
   private List<QRecord> query(QueryInput input, Delivery delivery) throws Exception
   {
      return query(input, delivery, 1);
   }



   /*******************************************************************************
    ** Larger inputs retain a small queue without spending a retry interval per row.
    *******************************************************************************/
   private List<QRecord> query(QueryInput input, Delivery delivery, Integer capacity) throws Exception
   {
      if(delivery == Delivery.DIRECT)
      {
         return new QueryAction().execute(input).getRecords();
      }
      RecordPipe pipe = new RecordPipe(capacity);
      input.setRecordPipe(pipe);
      input.setIncludeAssociations(delivery == Delivery.ASSOCIATION_BUFFER);
      ExecutorService producer = Executors.newSingleThreadExecutor();
      Future<QueryOutput> future = producer.submit(() ->
      {
         QContext.init(instance, new QSession());
         try
         {
            return new QueryAction().execute(input);
         }
         finally
         {
            QContext.clear();
         }
      });
      try
      {
         List<QRecord> records = new ArrayList<>();
         long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
         while(!future.isDone() || pipe.countAvailableRecords() > 0)
         {
            records.addAll(pipe.consumeAvailableRecords());
            assertTrue(System.nanoTime() < deadline, "Query producer exceeded its bounded deadline");
            Thread.sleep(5);
         }
         QueryOutput output;
         try
         {
            output = future.get(5, TimeUnit.SECONDS);
         }
         catch(ExecutionException e)
         {
            assertTrue(records.isEmpty(), "Failing first callback published records");
            assertEquals(0, pipe.countAvailableRecords());
            if(e.getCause() instanceof QException queryException)
            {
               throw queryException;
            }
            throw e;
         }
         records.addAll(pipe.consumeAvailableRecords());
         assertThrows(IllegalStateException.class, output::getRecords);
         assertEquals(records.size(), pipe.getTotalRecordCount());
         return records;
      }
      finally
      {
         pipe.terminate();
         future.cancel(true);
         producer.shutdownNow();
         assertTrue(producer.awaitTermination(5, TimeUnit.SECONDS), "Owned query producer did not stop");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> ids(List<QRecord> records, String field)
   {
      return records.stream().map(record -> record.getValueInteger(field)).toList();
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



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ClonedRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return records.stream().map(record -> new QRecord(record).withValue("textValue", "copied:" + record.getValueString("name"))).toList();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class BatchClonedPeople extends ClonedPeople
   {
      private static final List<Integer> batches = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         batches.add(records.size());
         return super.postQuery(input, records);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SubListRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return records.subList(0, records.size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class EmptyRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return List.of();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ExpandedRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         List<QRecord> copies = new ArrayList<>();
         for(QRecord record : records)
         {
            copies.add(new QRecord(record).withValue("textValue", record.getValueString("name") + " A"));
            copies.add(new QRecord(record).withValue("textValue", record.getValueString("name") + " B"));
         }
         return copies;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailingRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records) throws QException
      {
         throw new QException("Synthetic read failure");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ClonedPeople implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return records.stream().map(record -> new QRecord(record).withValue("email", "copied@example.invalid")).toList();
      }
   }



   /*******************************************************************************
    ** Return the same list to isolate display/projection behavior from replacement.
    *******************************************************************************/
   public static class DisplayOnlyPrivateValues implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         records.forEach(record -> record.withDisplayValue("passwordValue", "display-private").withDisplayValue("htmlValue", "hidden-display"));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class NullRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return null;
      }
   }



   /*******************************************************************************
    ** Preserve list ownership here to isolate virtual-field privacy.
    *******************************************************************************/
   public static class VirtualPrivateValues implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         for(QRecord record : records)
         {
            record.withValue("virtualPassword", record.getValue("passwordValue"))
               .withValue("virtualHidden", record.getValue("htmlValue"))
               .withDisplayValue("virtualPassword", "virtual-display-secret")
               .withDisplayValue("virtualHidden", "virtual-hidden-display");
         }
         return records;
      }
   }
}
