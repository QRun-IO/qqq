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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Native file key reads retain types, all source files and heavy projected values.
 *******************************************************************************/
class FilesystemUniqueKeyLookupTest extends FilesystemActionTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvFindsAllOwnersAcrossFilesAndTypedStoredKey() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_CSV);
      UniqueKey key = new UniqueKey("lastName");
      table.withUniqueKey(key);
      table.getField("lastName").setIsHidden(true);
      assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("lastName", "S"), null))
         .extracting(record -> record.getValueInteger("id")).containsExactlyInAnyOrder(3, 4, 5);
      List<QRecord> stored = UniqueKeyLookup.readStoredComponents(table, List.of(4), null);
      assertEquals(1, stored.size());
      assertEquals("S", stored.get(0).getValueString("lastName"));
      assertEquals(4, stored.get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** A filename predicate still needs the heavy contents used by the unique key.
    *******************************************************************************/
   @Test
   void testOneFileHeavyKeyProjectionAndBinaryConflict() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);
      UniqueKey key = new UniqueKey("contents");
      table.withUniqueKey(key);
      table.getField("contents").withIsHeavy(true).withIsHidden(true);
      byte[] expected = "Hi Bob".getBytes(StandardCharsets.UTF_8);
      List<QRecord> stored = UniqueKeyLookup.readStoredComponents(table, List.of("BLOB-2.txt"), null);
      assertEquals(1, stored.size());
      assertArrayEquals(expected, (byte[]) stored.get(0).getValue("contents"));
      assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("contents", expected), null))
         .extracting(record -> record.getValueString("fileName")).containsExactly("BLOB-2.txt");
   }



   /*******************************************************************************
    ** INSERT only needs conflict existence, including on tables without a primary key.
    *******************************************************************************/
   @Test
   void testJsonConflictDoesNotRequirePrimaryKey() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      UniqueKey key = new UniqueKey("lastName");
      table.withUniqueKey(key).setPrimaryKeyField(null);
      assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("lastName", "S"), null)).hasSize(1);
   }



   /*******************************************************************************
    ** A missing nullable JSON key component is null only in the native projection.
    *******************************************************************************/
   @Test
   void testJsonMissingNullableComponentHasExplicitNullProjection() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      table.withField(new QFieldMetaData("suffix", QFieldType.STRING).withIsHidden(true).withIsHeavy(true));
      UniqueKey key = new UniqueKey("firstName", "suffix");
      table.withUniqueKey(key);
      Path file = Path.of(TestUtils.defineLocalFilesystemBackend().getBasePath(), "persons", "NULLABLE.json");
      String json = "[{\"id\":\"4\",\"firstName\":\"John\"}]";
      Files.writeString(file, json);

      QRecord stored = UniqueKeyLookup.readStoredComponents(table, List.of("4"), null).get(0);
      assertThat(stored.getValues()).containsOnlyKeys("id", "firstName", "suffix").containsEntry("id", 4)
         .containsEntry("firstName", "John").containsEntry("suffix", null);
      assertThat(UniqueKeyLookup.findConflicts(table, key, stored, null)).isEmpty();
      table.getField("suffix").setIsRequired(true);
      assertThat(UniqueKeyLookup.readStoredComponents(table, List.of("4"), null).get(0).getValues()).containsEntry("suffix", null);
      assertThat(new FilesystemQueryAction().execute(new QueryInput(table.getName())).getRecords())
         .allSatisfy(record -> assertThat(record.getValues()).doesNotContainKey("suffix"));
      assertEquals(json, Files.readString(file));
   }



   /*******************************************************************************
    ** A malformed owner's absent PK cannot hide the existence of its occupied key.
    *******************************************************************************/
   @Test
   void testJsonConflictRetainsOwnerWithMissingPrimaryKey() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      UniqueKey key = new UniqueKey("firstName");
      table.withUniqueKey(key);
      Path file = Path.of(TestUtils.defineLocalFilesystemBackend().getBasePath(), "persons", "NO-ID.json");
      String json = "[{\"firstName\":\"Unique owner without ID\"}]";
      Files.writeString(file, json);
      List<QRecord> owners = UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("firstName", "Unique owner without ID"), null);
      assertThat(owners).singleElement().satisfies(record -> assertThat(record.getValues()).containsOnlyKeys("id", "firstName")
         .containsEntry("id", null).containsEntry("firstName", "Unique owner without ID"));
      assertEquals(json, Files.readString(file));
   }



   /*******************************************************************************
    ** The table-driven CSV decoder already projects missing columns as null.
    *******************************************************************************/
   @Test
   void testCsvMissingColumnHasExplicitNullProjection() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_CSV);
      table.withField(new QFieldMetaData("suffix", QFieldType.STRING)).withUniqueKey(new UniqueKey("lastName", "suffix"));
      QRecord stored = UniqueKeyLookup.readStoredComponents(table, List.of("4"), null).get(0);
      assertThat(stored.getValues()).containsOnlyKeys("id", "lastName", "suffix").containsEntry("id", 4)
         .containsEntry("lastName", "S").containsEntry("suffix", null);
   }



   /*******************************************************************************
    ** A broken later file cannot be interpreted as absence of a conflicting record.
    *******************************************************************************/
   @Test
   void testInvalidSourceFileFailsLookup() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      UniqueKey key = new UniqueKey("lastName");
      table.withUniqueKey(key);
      Path directory = Path.of(TestUtils.defineLocalFilesystemBackend().getBasePath(), "persons");
      Files.writeString(directory.resolve("BROKEN.json"), "{invalid-json");
      assertThrows(QException.class, () -> UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("lastName", "S"), null));
   }



   /*******************************************************************************
    ** CSV values are typed and projected from every file, including absent columns.
    *******************************************************************************/
   @Test
   void testStoredAssociationValuesFromCsv() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_CSV);
      table.getField("lastName").withIsHidden(true).withIsHeavy(true);
      table.withField(new QFieldMetaData("suffix", QFieldType.STRING));
      Association association = storedAssociation(table, List.of("lastName", "suffix"));
      assertThat(AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of("4"), null))
         .singleElement().satisfies(record -> assertThat(record.getValues()).containsOnlyKeys("id", "lastName", "suffix")
            .containsEntry("id", 4).containsEntry("lastName", "S").containsEntry("suffix", null));
   }



   /*******************************************************************************
    ** Only successful JSON decoding can identify absent stored values as null.
    *******************************************************************************/
   @Test
   void testStoredAssociationValuesFromJsonAndMalformedFile() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      table.withField(new QFieldMetaData("suffix", QFieldType.STRING).withIsHidden(true).withIsHeavy(true));
      Association association = storedAssociation(table, List.of("firstName", "suffix"));
      Path directory = Path.of(TestUtils.defineLocalFilesystemBackend().getBasePath(), "persons");
      Path file = directory.resolve("STORED-PARENTS.json");
      String json = "[{\"id\":\"400\",\"firstName\":\"Stored\"},{\"id\":401,\"firstName\":\"Stored\"},{\"id\":402,\"firstName\":\"Stored\",\"suffix\":\"null\"}]";
      Files.writeString(file, json);
      List<QRecord> stored = AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of(400, "401"), null);
      assertThat(stored).extracting(record -> record.getValueInteger("id")).containsExactlyInAnyOrder(400, 401);
      assertThat(stored).allSatisfy(record -> assertThat(record.getValues()).containsOnlyKeys("id", "firstName", "suffix")
         .containsEntry("firstName", "Stored").containsEntry("suffix", null));
      assertThat(AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of(402), null))
         .singleElement().satisfies(record -> assertEquals("null", record.getValueString("suffix")));
      assertThat(new FilesystemQueryAction().execute(new QueryInput(table.getName())).getRecords())
         .filteredOn(record -> Integer.valueOf(400).equals(record.getValueInteger("id")))
         .singleElement().satisfies(record -> assertThat(record.getValues()).doesNotContainKey("suffix"));
      assertEquals(json, Files.readString(file));
      Files.writeString(directory.resolve("BROKEN.json"), "{invalid-json");
      assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of(400), null));
   }



   /*******************************************************************************
    ** A filename lookup still fetches heavy contents when the join requires them.
    *******************************************************************************/
   @Test
   void testStoredAssociationValuesFromOneFile() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);
      table.getField("contents").withIsHidden(true).withIsHeavy(true);
      Association association = storedAssociation(table, List.of("contents"));
      List<QRecord> stored = AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of("BLOB-2.txt"), null);
      assertThat(stored).singleElement().satisfies(record ->
      {
         assertThat(record.getValues()).containsOnlyKeys("fileName", "contents");
         assertArrayEquals("Hi Bob".getBytes(StandardCharsets.UTF_8), (byte[]) record.getValue("contents"));
      });
   }



   /*******************************************************************************
    ** A named self relationship keeps the native projection fixture independent.
    *******************************************************************************/
   private Association storedAssociation(QTableMetaData table, List<String> fields)
   {
      Association association = new Association().withName("stored relationship").withAssociatedTableName(table.getName()).withJoinName(table.getName() + "StoredValues");
      table.withAssociation(association);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName(association.getJoinName()).withLeftTable(table.getName()).withRightTable(table.getName())
         .withType(JoinType.ONE_TO_MANY).withJoinOns(fields.stream().map(field -> new JoinOn(field, field)).toList()));
      return association;
   }

}
