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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Common read options and relational selection use canonical sample metadata.
 ** Native SQL is the independent oracle; set-operation acceptance is H2/RDBMS
 ** evidence and does not imply support by every backend provider.
 *******************************************************************************/
class SampleQueryReadContractTest
{
   private QInstance instance;



   /*******************************************************************************
    ** Native values distinguish nonempty, empty, and null heavy-field storage.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      String[] names = { "Alpha", "Beta", "Gamma" };
      String[] texts = { "heavy payload", "", null };
      Long[] numbers = { 42L, 0L, null };
      byte[][] bytes = { { 0, 1, -1 }, {}, null };
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("INSERT INTO field_lab (name,long_value,text_value,blob_value) VALUES (?,?,?,?)"))
      {
         for(int index = 0; index < names.length; index++)
         {
            statement.setString(1, names[index]);
            statement.setObject(2, numbers[index], Types.BIGINT);
            statement.setString(3, texts[index]);
            statement.setBytes(4, bytes[index]);
            assertEquals(1, statement.executeUpdate());
         }
      }
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
    ** Query defaults omit heavy values, retain lengths, and do not format records.
    *******************************************************************************/
   @Test
   void testDefaultAndExplicitHeavyFieldReads() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      instance.getTable("fieldLab").getField("textValue").setIsHeavy(true);
      instance.getTable("fieldLab").getField("blobValue").setIsHeavy(true);
      instance.getTable("fieldLab").getField("longValue").setDisplayFormat("Long %d");
      Set<String> lightFields = new HashSet<>(instance.getTable("fieldLab").getFields().keySet());
      lightFields.removeAll(Set.of("textValue", "blobValue"));
      List<QRecord> light = query(new QueryInput("fieldLab").withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))));
      assertEquals(List.of(1, 2, 3), ids(light));
      List<List<String>> lengths = rows("SELECT id,CHAR_LENGTH(text_value),OCTET_LENGTH(blob_value) FROM field_lab ORDER BY id");
      assertEquals("13", lengths.get(0).get(1));
      assertEquals("3", lengths.get(0).get(2));
      assertEquals("0", lengths.get(1).get(1));
      assertEquals("0", lengths.get(1).get(2));
      assertNull(lengths.get(2).get(1));
      assertNull(lengths.get(2).get(2));
      for(int index = 0; index < light.size(); index++)
      {
         QRecord record = light.get(index);
         assertEquals(lightFields, record.getValues().keySet());
         assertEquals(Map.of(), record.getDisplayValues());
         assertNull(record.getRecordLabel());
         assertEquals(Map.of(), record.getAssociatedRecords());
         Map<?, ?> fieldLengths = (Map<?, ?>) record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS);
         assertNotNull(fieldLengths);
         assertEquals(Set.of("textValue", "blobValue"), fieldLengths.keySet());
         assertEquals(lengths.get(index).get(1), fieldLengths.get("textValue") == null ? null : fieldLengths.get("textValue").toString());
         assertEquals(lengths.get(index).get(2), fieldLengths.get("blobValue") == null ? null : fieldLengths.get("blobValue").toString());
      }

      List<QRecord> complete = query(new QueryInput("fieldLab").withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))
         .withShouldFetchHeavyFields(true).withShouldGenerateDisplayValues(true));
      assertEquals(rows("SELECT id,name,long_value,text_value FROM field_lab ORDER BY id"), recordRows(complete, "id", "name", "longValue", "textValue"));
      assertEquals("Long 42", complete.get(0).getDisplayValue("longValue"));
      assertEquals("Long 0", complete.get(1).getDisplayValue("longValue"));
      assertNull(complete.get(2).getDisplayValue("longValue"));
      assertEquals(List.of("Alpha", "Beta", "Gamma"), complete.stream().map(QRecord::getRecordLabel).toList());
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT id,blob_value FROM field_lab ORDER BY id"))
      {
         int index = 0;
         while(result.next())
         {
            QRecord record = complete.get(index++);
            assertEquals(result.getInt("id"), record.getValueInteger("id"));
            assertEquals(instance.getTable("fieldLab").getFields().keySet(), record.getValues().keySet());
            assertArrayEquals(result.getBytes("blob_value"), record.getValueByteArray("blobValue"));
         }
         assertEquals(3, index);
      }
      assertEquals(List.of(1, 2, 3), ids(query(new QueryInput("fieldLab"))).stream().sorted().toList());
      assertEquals(List.of(1, 2, 3), ids(query(new QueryInput("fieldLab").withFilter(new QQueryFilter()))).stream().sorted().toList());
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** Formatting and possible-value translation are independent input options.
    *******************************************************************************/
   @Test
   void testDisplayFormattingIsIndependentOfPossibleValueTranslation() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      instance.getTable("pet").getField("id").setDisplayFormat("Pet %d");
      List<QRecord> defaults = query(petsInput());
      assertEquals(List.of(1, 6), ids(defaults));
      assertEquals(rows("SELECT id,name,person_id,species_id FROM pet WHERE id IN (1,6) ORDER BY id"), recordRows(defaults, "id", "name", "personId", "speciesId"));
      for(QRecord record : defaults)
      {
         assertEquals(Map.of(), record.getDisplayValues());
         assertNull(record.getRecordLabel());
         assertEquals(Map.of(), record.getAssociatedRecords());
      }
      List<QRecord> formatted = query(petsInput().withShouldGenerateDisplayValues(true));
      assertEquals(List.of("Pet 1", "Pet 6"), formatted.stream().map(record -> record.getDisplayValue("id")).toList());
      assertEquals(List.of("Charlie", "Mae"), formatted.stream().map(QRecord::getRecordLabel).toList());
      assertEquals(List.of("1", "3"), formatted.stream().map(record -> record.getDisplayValue("personId")).toList());
      assertEquals(List.of("1", "2"), formatted.stream().map(record -> record.getDisplayValue("speciesId")).toList());
      List<QRecord> both = query(petsInput().withShouldGenerateDisplayValues(true).withShouldTranslatePossibleValues(true));
      assertEquals(List.of("Pet 1", "Pet 6"), both.stream().map(record -> record.getDisplayValue("id")).toList());
      assertEquals(List.of("Avery Sample", "Casey Sample"), both.stream().map(record -> record.getDisplayValue("personId")).toList());
      assertEquals(List.of("Dog", "Cat"), both.stream().map(record -> record.getDisplayValue("speciesId")).toList());
      assertEquals(recordRows(defaults, "id", "name", "personId", "speciesId"), recordRows(both, "id", "name", "personId", "speciesId"));
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** Null means all possible-value fields; empty means none. The enable flag wins.
    *******************************************************************************/
   @Test
   void testPossibleValueTranslationAllSubsetEmptyAndDisabled() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      assertEquals(List.of(List.of("1", "1", "1", "Avery Sample"), List.of("6", "3", "2", "Casey Sample")),
         rows("SELECT a.id,a.person_id,a.species_id,p.first_name || ' ' || p.last_name FROM pet a JOIN person p ON p.id=a.person_id WHERE a.id IN (1,6) ORDER BY a.id"));
      assertPetTranslations(query(petsInput().withShouldTranslatePossibleValues(true)), Set.of("personId", "speciesId"));
      for(Set<String> subset : List.of(Set.of("speciesId"), Set.of("personId"), Set.<String>of()))
      {
         assertPetTranslations(query(petsInput().withShouldTranslatePossibleValues(true).withFieldsToTranslatePossibleValues(subset)), subset);
      }
      assertPetTranslations(query(petsInput().withShouldTranslatePossibleValues(false).withFieldsToTranslatePossibleValues(Set.of("personId", "speciesId"))), Set.of());
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** A joined field subset uses its alias; an unqualified name cannot expand it.
    *******************************************************************************/
   @Test
   void testJoinedPossibleValueSubsetUsesQualifiedAlias() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      QueryInput input = peopleWithPets(true, QueryJoin.Type.INNER)
         .withFieldNamesToInclude(Set.of("id", "animal.id", "animal.speciesId"))
         .withShouldTranslatePossibleValues(true).withFieldsToTranslatePossibleValues(Set.of("animal.speciesId"));
      List<QRecord> translated = query(input);
      assertEquals(rows("SELECT p.id,a.id,a.species_id FROM person p JOIN pet a ON a.person_id=p.id ORDER BY p.id,a.id"), recordRows(translated, "id", "animal.id", "animal.speciesId"));
      assertEquals(List.of("Dog", "Dog", "Dog", "Dog", "Dog", "Cat"), translated.stream().map(record -> record.getDisplayValue("animal.speciesId")).toList());
      for(QRecord record : translated)
      {
         assertEquals(Set.of("animal.speciesId"), record.getDisplayValues().keySet());
      }
      List<QRecord> unqualified = query(peopleWithPets(true, QueryJoin.Type.INNER)
         .withFieldNamesToInclude(Set.of("id", "animal.id", "animal.speciesId"))
         .withShouldTranslatePossibleValues(true).withFieldsToTranslatePossibleValues(Set.of("speciesId")));
      assertEquals(recordRows(translated, "id", "animal.id", "animal.speciesId"), recordRows(unqualified, "id", "animal.id", "animal.speciesId"));
      unqualified.forEach(record -> assertEquals(Map.of(), record.getDisplayValues()));
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** LEFT keeps parents with no children; INNER returns only actual joined tuples.
    *******************************************************************************/
   @Test
   void testInnerAndLeftJoinsReturnExactNativeTuples() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      List<List<String>> inner = rows("SELECT p.id,p.first_name,a.id,a.name FROM person p INNER JOIN pet a ON a.person_id=p.id ORDER BY p.id,a.id");
      assertEquals(6, inner.size());
      assertEquals(List.of("1", "1", "1", "1", "2", "3"), inner.stream().map(row -> row.get(0)).toList());
      assertEquals(inner, selectedRows(peopleWithPets(true, QueryJoin.Type.INNER), "id", "firstName", "animal.id", "animal.name"));
      List<List<String>> left = rows("SELECT p.id,p.first_name,a.id,a.name FROM person p LEFT JOIN pet a ON a.person_id=p.id ORDER BY p.id,a.id");
      assertEquals(8, left.size());
      assertEquals(List.of("1", "1", "1", "1", "2", "3", "4", "5"), left.stream().map(row -> row.get(0)).toList());
      assertNull(left.get(6).get(2));
      assertNull(left.get(7).get(2));
      assertEquals(left, selectedRows(peopleWithPets(true, QueryJoin.Type.LEFT), "id", "firstName", "animal.id", "animal.name"));
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** An unselected join still participates in both filtering and ordering.
    *******************************************************************************/
   @Test
   void testUnselectedJoinStillFiltersAndOrdersBaseRows() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      QueryInput input = peopleWithPets(false, QueryJoin.Type.INNER).withFilter(new QQueryFilter(new QFilterCriteria("animal.id", QCriteriaOperator.IN, 2, 5, 6))
         .withOrderBy(new QFilterOrderBy("animal.name")));
      List<List<String>> expected = rows("SELECT p.id,p.first_name FROM person p JOIN pet a ON a.person_id=p.id WHERE a.id IN (2,5,6) ORDER BY a.name");
      assertEquals(List.of("1", "3", "2"), expected.stream().map(row -> row.get(0)).toList());
      assertEquals(expected, selectedRows(input, "id", "firstName"));
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** DISTINCT deduplicates the selected row, not always the base primary key.
    *******************************************************************************/
   @Test
   void testDistinctAppliesToEntireSelectedRow() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      QueryInput repeatedInput = peopleWithPets(false, QueryJoin.Type.INNER).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      List<List<String>> repeated = selectedRows(repeatedInput, "id", "firstName");
      assertEquals(rows("SELECT p.id,p.first_name FROM person p JOIN pet a ON a.person_id=p.id ORDER BY p.id"), repeated);
      assertEquals(List.of("1", "1", "1", "1", "2", "3"), repeated.stream().map(row -> row.get(0)).toList());
      QueryInput distinctInput = peopleWithPets(false, QueryJoin.Type.INNER).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))).withSelectDistinct(true);
      List<List<String>> distinct = selectedRows(distinctInput, "id", "firstName");
      assertEquals(rows("SELECT DISTINCT p.id,p.first_name FROM person p JOIN pet a ON a.person_id=p.id ORDER BY p.id"), distinct);
      assertEquals(List.of("1", "2", "3"), distinct.stream().map(row -> row.get(0)).toList());
      List<List<String>> joinedDistinct = selectedRows(peopleWithPets(true, QueryJoin.Type.INNER).withSelectDistinct(true), "id", "animal.id");
      assertEquals(rows("SELECT DISTINCT p.id,a.id FROM person p JOIN pet a ON a.person_id=p.id ORDER BY p.id,a.id"), joinedDistinct);
      assertEquals(6, joinedDistinct.size());
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** A field operand compares actual columns, not a literal field-name string.
    *******************************************************************************/
   @Test
   void testOtherFieldComparisonAndRejectedReferences() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      for(QCriteriaOperator operator : List.of(QCriteriaOperator.EQUALS, QCriteriaOperator.GREATER_THAN))
      {
         QueryInput input = peopleWithPets(true, QueryJoin.Type.INNER).withFilter(new QQueryFilter(
            new QFilterCriteria("animal.id", operator).withOtherFieldName("id"))
            .withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("animal.id")));
         String sqlOperator = operator == QCriteriaOperator.EQUALS ? "=" : ">";
         List<List<String>> expected = rows("SELECT p.id,a.id FROM person p JOIN pet a ON a.person_id=p.id WHERE a.id " + sqlOperator + " p.id ORDER BY p.id,a.id");
         assertEquals(operator == QCriteriaOperator.EQUALS ? 1 : 5, expected.size());
         assertEquals(expected, selectedRows(input, "id", "animal.id"));
      }
      assertThrows(QException.class, () -> query(peopleWithPets(true, QueryJoin.Type.INNER).withFilter(new QQueryFilter(
         new QFilterCriteria("animal.id", QCriteriaOperator.EQUALS).withOtherFieldName("missingField")))));
      assertThrows(QException.class, () -> query(peopleWithPets(true, QueryJoin.Type.INNER).withFilter(new QQueryFilter(
         new QFilterCriteria("id", QCriteriaOperator.EQUALS).withOtherFieldName("animal.missingField")))));
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** H2/RDBMS native set operations preserve overlap multiplicity and ordering.
    *******************************************************************************/
   @Test
   void testRdbmsSubFilterSetOperations() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      Map<QQueryFilter.SubFilterSetOperator, List<String>> expectedNames = Map.of(
         QQueryFilter.SubFilterSetOperator.UNION, List.of("Alpha", "Beta", "Gamma"),
         QQueryFilter.SubFilterSetOperator.UNION_ALL, List.of("Alpha", "Beta", "Beta", "Gamma"),
         QQueryFilter.SubFilterSetOperator.INTERSECT, List.of("Beta"),
         QQueryFilter.SubFilterSetOperator.EXCEPT, List.of("Alpha"));
      for(QQueryFilter.SubFilterSetOperator operator : QQueryFilter.SubFilterSetOperator.values())
      {
         QQueryFilter filter = new QQueryFilter().withSubFilterSetOperator(operator)
            .withSubFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "Alpha", "Beta")))
            .withSubFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "Beta", "Gamma")))
            .withOrderBy(new QFilterOrderBy("name"));
         String sqlOperator = operator.name().replace('_', ' ');
         List<List<String>> expected = rows("(SELECT id,name FROM field_lab WHERE name IN ('Alpha','Beta')) " + sqlOperator
            + " (SELECT id,name FROM field_lab WHERE name IN ('Beta','Gamma')) ORDER BY name");
         assertEquals(expectedNames.get(operator), expected.stream().map(row -> row.get(1)).toList(), operator.name());
         assertEquals(expected, selectedRows(new QueryInput("fieldLab").withFilter(filter), "id", "name"), operator.name());
      }
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    ** Invalid projected fields, joins, and set branches fail rather than widening.
    *******************************************************************************/
   @Test
   void testInvalidReadProjectionAndSetBranchRejectWithoutMutation() throws Exception
   {
      Map<String, List<List<String>>> before = nativeSnapshot();
      assertThrows(QException.class, () -> query(new QueryInput("person").withFieldNamesToInclude(Set.of())));
      assertThrows(QException.class, () -> query(new QueryInput("person").withFieldNamesToInclude(Set.of("missingField"))));
      assertThrows(QException.class, () -> query(peopleWithPets(false, QueryJoin.Type.INNER).withFieldNamesToInclude(Set.of("id", "animal.id"))));
      assertThrows(QException.class, () -> query(peopleWithPets(true, QueryJoin.Type.INNER).withFieldNamesToInclude(Set.of("id", "pet.id"))));
      assertThrows(QException.class, () -> query(new QueryInput("person").withQueryJoin(new QueryJoin("missingTable").withSelect(true))));
      assertThrows(QException.class, () -> query(new QueryInput("fieldLab").withFilter(new QQueryFilter()
         .withSubFilterSetOperator(QQueryFilter.SubFilterSetOperator.UNION)
         .withSubFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Alpha")))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("missingField", QCriteriaOperator.EQUALS, "Beta"))))));
      assertEquals(before, nativeSnapshot());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertPetTranslations(List<QRecord> records, Set<String> fields)
   {
      assertEquals(List.of(1, 6), ids(records));
      for(QRecord record : records)
      {
         Map<String, String> expected = new HashMap<>();
         if(fields.contains("personId"))
         {
            expected.put("personId", record.getValueInteger("id").equals(1) ? "Avery Sample" : "Casey Sample");
         }
         if(fields.contains("speciesId"))
         {
            expected.put("speciesId", record.getValueInteger("id").equals(1) ? "Dog" : "Cat");
         }
         assertEquals(expected, record.getDisplayValues());
         assertNull(record.getRecordLabel());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput petsInput()
   {
      return new QueryInput("pet").withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 6))
         .withOrderBy(new QFilterOrderBy("id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput peopleWithPets(boolean select, QueryJoin.Type type)
   {
      return new QueryInput("person").withQueryJoin(new QueryJoin("pet").withAlias("animal").withSelect(select).withType(type))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("animal.id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> query(QueryInput input) throws QException
   {
      return new QueryAction().execute(input).getRecords();
   }



   /*******************************************************************************
    ** Enforce the projection independently of the values returned by native SQL.
    *******************************************************************************/
   private List<List<String>> selectedRows(QueryInput input, String... fields) throws QException
   {
      input.setFieldNamesToInclude(Set.of(fields));
      List<QRecord> records = query(input);
      for(QRecord record : records)
      {
         assertEquals(Set.of(fields), record.getValues().keySet());
      }
      return recordRows(records, fields);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> recordRows(List<QRecord> records, String... fields)
   {
      List<List<String>> rows = new ArrayList<>();
      for(QRecord record : records)
      {
         List<String> values = new ArrayList<>();
         for(String field : fields)
         {
            values.add(record.getValueString(field));
         }
         rows.add(values);
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> ids(List<QRecord> records)
   {
      return records.stream().map(record -> record.getValueInteger("id")).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, List<List<String>>> nativeSnapshot() throws Exception
   {
      return Map.of("person", rows("SELECT * FROM person ORDER BY id"),
         "pet", rows("SELECT * FROM pet ORDER BY id"),
         "fieldLab", rows("SELECT * FROM field_lab ORDER BY id"));
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
