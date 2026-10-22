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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableSchemaFingerprint and SchemaDiff
 *******************************************************************************/
class TableSchemaFingerprintTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QFieldMetaData> fields()
   {
      List<QFieldMetaData> fields = new ArrayList<>();
      fields.add(new QFieldMetaData("id", QFieldType.INTEGER));
      fields.add(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(80).withLabel("Name"));
      fields.add(new QFieldMetaData("ssn", QFieldType.STRING).withIsSensitive(true));
      fields.add(new QFieldMetaData("body", QFieldType.BLOB).withIsHeavy(true));
      fields.add(new QFieldMetaData("storeId", QFieldType.INTEGER).withPossibleValueSourceName("store"));
      return (fields);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table(List<QFieldMetaData> fields)
   {
      return (new QTableMetaData()
         .withName("person")
         .withLabel("Person")
         .withPrimaryKeyField("id")
         .withFields(fields)
         .withUniqueKey(new UniqueKey("name", "storeId"))
         .withAssociation(new Association().withName("lines").withAssociatedTableName("line").withJoinName("personLine")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private TableSchemaFingerprint fingerprint(QTableMetaData table)
   {
      return (TableSchemaFingerprint.of(table, "rdbms"));
   }



   /*******************************************************************************
    ** same input, same hash; the hash is a 64-char hex SHA-256 and the
    ** descriptor is canonical JSON.
    *******************************************************************************/
   @Test
   void testStableAcrossRuns()
   {
      TableSchemaFingerprint a = fingerprint(table(fields()));
      TableSchemaFingerprint b = fingerprint(table(fields()));
      assertEquals(a.getHash(), b.getHash());
      assertEquals(a.getDescriptorJson(), b.getDescriptorJson());
      assertEquals(a, b);
      assertTrue(a.getHash().matches("[0-9a-f]{64}"));
      assertTrue(a.getDescriptorJson().startsWith("{\"associations\":[{\"associatedTable\":\"line\""));
      assertFalse(a.getDescriptorJson().contains(" "));

      ///////////////////////////////////////////////////////////////////////////////////
      // pinned value: changing the descriptor format is a breaking change for stored  //
      // fingerprints, so it should be a deliberate edit of this expected hash         //
      ///////////////////////////////////////////////////////////////////////////////////
      assertEquals("f10c31cc7554ea09878895ee3e8b8545c86b322c7fbcf4d3ecd1048f2dd49391", a.getHash());
   }



   /*******************************************************************************
    ** field, unique key and association ordering do not matter
    *******************************************************************************/
   @Test
   void testStableAcrossOrdering()
   {
      List<QFieldMetaData> reversed = fields();
      Collections.reverse(reversed);

      QTableMetaData other = table(reversed);
      other.setUniqueKeys(List.of(new UniqueKey("storeId", "name")));

      assertEquals(fingerprint(table(fields())).getHash(), fingerprint(other).getHash());
   }



   /*******************************************************************************
    ** labels, adornments, help text and the like are not part of the fingerprint
    *******************************************************************************/
   @Test
   void testCosmeticChangesDoNotChangeIt()
   {
      String before = fingerprint(table(fields())).getHash();

      List<QFieldMetaData> cosmetic = fields();
      cosmetic.get(1).setLabel("Full Name");
      cosmetic.get(1).withFieldAdornment(new FieldAdornment(AdornmentType.TOOLTIP));
      cosmetic.get(1).setHelpContents(List.of(new QHelpContent("Help me")));
      cosmetic.get(2).setIsHidden(true);
      cosmetic.get(2).setIsEditable(false);
      cosmetic.get(2).setDisplayFormat("%s!");

      QTableMetaData cosmeticTable = table(cosmetic);
      cosmeticTable.setLabel("Somebody");

      assertEquals(before, fingerprint(cosmeticTable).getHash());
   }



   /*******************************************************************************
    ** every real change changes the hash
    *******************************************************************************/
   @Test
   void testRealChangesChangeIt()
   {
      String base = fingerprint(table(fields())).getHash();

      List<Consumer<QFieldMetaData>> fieldChanges = List.of(
         f -> f.setType(QFieldType.LONG), f -> f.setIsRequired(!f.getIsRequired()), f -> f.setMaxLength(81),
         f -> f.setPossibleValueSourceName("other"), f -> f.setIsHeavy(!f.getIsHeavy()), f -> f.setIsSensitive(!f.getIsSensitive()));
      for(Consumer<QFieldMetaData> change : fieldChanges)
      {
         List<QFieldMetaData> changed = fields();
         change.accept(changed.get(1));
         assertNotEquals(base, fingerprint(table(changed)).getHash());
      }

      List<QFieldMetaData> added = fields();
      added.add(new QFieldMetaData("extra", QFieldType.STRING));
      assertNotEquals(base, fingerprint(table(added)).getHash());

      List<QFieldMetaData> removed = fields();
      removed.remove(2);
      assertNotEquals(base, fingerprint(table(removed)).getHash());

      QTableMetaData differentPk = table(fields());
      differentPk.setPrimaryKeyField("storeId");
      assertNotEquals(base, fingerprint(differentPk).getHash());

      QTableMetaData differentUniqueKey = table(fields());
      differentUniqueKey.setUniqueKeys(List.of(new UniqueKey("name")));
      assertNotEquals(base, fingerprint(differentUniqueKey).getHash());

      QTableMetaData differentAssociation = table(fields());
      differentAssociation.setAssociations(List.of(new Association().withName("lines").withAssociatedTableName("lineItem").withJoinName("personLine")));
      assertNotEquals(base, fingerprint(differentAssociation).getHash());

      assertNotEquals(base, TableSchemaFingerprint.of(table(fields()), "postgres").getHash());
   }



   /*******************************************************************************
    ** a stored descriptor round-trips to the same fingerprint
    *******************************************************************************/
   @Test
   void testDescriptorRoundTrip()
   {
      TableSchemaFingerprint original = fingerprint(table(fields()));
      TableSchemaFingerprint restored = TableSchemaFingerprint.fromDescriptorJson(original.getDescriptorJson());
      assertEquals(original.getHash(), restored.getHash());
      assertEquals("person", original.getDescriptor().get("table"));
      assertTrue(SchemaDiff.of(original, restored).isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSchemaDiff()
   {
      TableSchemaFingerprint before = fingerprint(table(fields()));

      List<QFieldMetaData> changed = fields();
      changed.removeIf(f -> f.getName().equals("body"));
      changed.add(new QFieldMetaData("extra", QFieldType.STRING));
      changed.get(1).setMaxLength(100);
      changed.get(1).setIsSensitive(true);

      QTableMetaData afterTable = table(changed);
      afterTable.setPrimaryKeyField("name");
      SchemaDiff diff = SchemaDiff.of(before, fingerprint(afterTable));

      assertFalse(diff.isEmpty());
      assertEquals(List.of("extra"), diff.getAddedFields());
      assertEquals(List.of("body"), diff.getRemovedFields());
      assertEquals(1, diff.getChangedFields().size());
      assertEquals("name", diff.getChangedFields().get(0).fieldName());
      assertEquals(List.of("isSensitive", "maxLength"), diff.getChangedFields().get(0).changedAttributes());
      assertEquals(80, diff.getChangedFields().get(0).before().get("maxLength"));
      assertEquals(100, diff.getChangedFields().get(0).after().get("maxLength"));
      assertEquals(1, diff.getChangedTableProperties().size());
      assertEquals("primaryKeyField", diff.getChangedTableProperties().get(0).property());
      assertTrue(diff.toString().contains("added fields [extra]"));

      assertTrue(SchemaDiff.of(before, before).isEmpty());
      assertEquals("no schema changes", SchemaDiff.of(before, before).toString());
   }



   /*******************************************************************************
    ** of(table) works, and takes the backend type from the table's backend
    *******************************************************************************/
   @Test
   void testOfResolvesBackendType()
   {
      QTableMetaData table = table(fields()).withBackendName("memory");
      assertEquals("memory", TableSchemaFingerprint.of(table).getDescriptor().get("backendType"));
   }
}
