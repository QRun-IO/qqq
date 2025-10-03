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

package com.kingsrook.qqq.backend.core.actions.values;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QPossibleValueTranslator
 *******************************************************************************/
public class QPossibleValueTranslatorTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueEnum()
   {
      QInstance                qInstance               = QContext.getQInstance();
      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      QFieldMetaData           stateField              = qInstance.getTable("person").getField("homeStateId");
      QPossibleValueSource     possibleValueSource     = qInstance.getPossibleValueSource(stateField.getPossibleValueSourceName());

      //////////////////////////////////////////////////////////////////////////
      // assert the default formatting for a not-found value is a null string //
      //////////////////////////////////////////////////////////////////////////
      assertNull(possibleValueTranslator.translatePossibleValue(stateField, null));
      assertNull(possibleValueTranslator.translatePossibleValue(stateField, -1));

      //////////////////////////////////////////////////////////////////////
      // let the not-found value be a simple string (no formatted values) //
      //////////////////////////////////////////////////////////////////////
      possibleValueSource.setValueFormatIfNotFound("?");
      assertEquals("?", possibleValueTranslator.translatePossibleValue(stateField, null));
      assertEquals("?", possibleValueTranslator.translatePossibleValue(stateField, -1));

      /////////////////////////////////////////////////////////////
      // let the not-found value be a string w/ formatted values //
      /////////////////////////////////////////////////////////////
      possibleValueSource.setValueFormatIfNotFound("? (%s)");
      possibleValueSource.setValueFieldsIfNotFound(List.of("id"));
      assertEquals("? ()", possibleValueTranslator.translatePossibleValue(stateField, null));
      assertEquals("? (-1)", possibleValueTranslator.translatePossibleValue(stateField, -1));

      /////////////////////////////////////////////////////
      // assert the default formatting is just the label //
      /////////////////////////////////////////////////////
      assertEquals("MO", possibleValueTranslator.translatePossibleValue(stateField, 2));
      assertEquals("IL", possibleValueTranslator.translatePossibleValue(stateField, 1));

      /////////////////////////////////////////////////////////////////
      // assert the LABEL_ONLY format (when called out specifically) //
      /////////////////////////////////////////////////////////////////
      possibleValueSource.setValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY);
      assertEquals("IL", possibleValueTranslator.translatePossibleValue(stateField, 1));

      ///////////////////////////////////////
      // assert the LABEL_PARAMS_ID format //
      ///////////////////////////////////////
      possibleValueSource.setValueFormatAndFields(PVSValueFormatAndFields.LABEL_PARENS_ID);
      assertEquals("IL (1)", possibleValueTranslator.translatePossibleValue(stateField, 1));

      //////////////////////////////////////
      // assert the ID_COLON_LABEL format //
      //////////////////////////////////////
      possibleValueSource.setValueFormat(PVSValueFormatAndFields.ID_COLON_LABEL.getFormat());
      possibleValueSource.setValueFields(PVSValueFormatAndFields.ID_COLON_LABEL.getFields());
      assertEquals("1: IL", possibleValueTranslator.translatePossibleValue(stateField, 1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueTable() throws QException
   {
      QInstance                qInstance               = QContext.getQInstance();
      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      QFieldMetaData           shapeField              = qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("favoriteShapeId");
      QPossibleValueSource     possibleValueSource     = qInstance.getPossibleValueSource(shapeField.getPossibleValueSourceName());

      TestUtils.insertDefaultShapes(qInstance);

      //////////////////////////////////////////////////////////////////////////
      // assert the default formatting for a not-found value is a null string //
      //////////////////////////////////////////////////////////////////////////
      assertNull(possibleValueTranslator.translatePossibleValue(shapeField, null));
      assertNull(possibleValueTranslator.translatePossibleValue(shapeField, -1));

      //////////////////////////////////////////////////////////////////////
      // let the not-found value be a simple string (no formatted values) //
      //////////////////////////////////////////////////////////////////////
      possibleValueSource.setValueFormatIfNotFound("?");
      assertEquals("?", possibleValueTranslator.translatePossibleValue(shapeField, null));
      assertEquals("?", possibleValueTranslator.translatePossibleValue(shapeField, -1));

      /////////////////////////////////////////////////////
      // assert the default formatting is just the label //
      /////////////////////////////////////////////////////
      assertEquals("Square", possibleValueTranslator.translatePossibleValue(shapeField, 2));
      assertEquals("Triangle", possibleValueTranslator.translatePossibleValue(shapeField, 1));

      ///////////////////////////////////////
      // assert the LABEL_PARAMS_ID format //
      ///////////////////////////////////////
      possibleValueSource.setValueFormatAndFields(PVSValueFormatAndFields.LABEL_PARENS_ID);
      assertEquals("Circle (3)", possibleValueTranslator.translatePossibleValue(shapeField, 3));

      ///////////////////////////////////////////////////////////
      // assert that we don't re-run queries for cached values //
      ///////////////////////////////////////////////////////////
      possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      MemoryRecordStore.setCollectStatistics(true);
      MemoryRecordStore.resetStatistics();
      possibleValueTranslator.translatePossibleValue(shapeField, 1);
      possibleValueTranslator.translatePossibleValue(shapeField, 2);
      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran 2 queries so far");
      possibleValueTranslator.translatePossibleValue(shapeField, 2);
      possibleValueTranslator.translatePossibleValue(shapeField, 3);
      possibleValueTranslator.translatePossibleValue(shapeField, 3);
      assertEquals(3, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran 3 queries in total");

      ///////////////////////////////////////////////////////////////
      // assert that if we prime the cache, we can do just 1 query //
      ///////////////////////////////////////////////////////////////
      possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      List<QRecord> personRecords = List.of(
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 1),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 1),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 2),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 2),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 3)
      );
      QTableMetaData personTable = qInstance.getTable(TestUtils.TABLE_NAME_PERSON);
      MemoryRecordStore.resetStatistics();
      possibleValueTranslator.primePvsCache(personTable, personRecords, null, null); // todo - test non-null queryJoins
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should only run 1 query");
      possibleValueTranslator.translatePossibleValue(shapeField, 1);
      possibleValueTranslator.translatePossibleValue(shapeField, 2);
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should only run 1 query");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueTableWithBadForeignKeys() throws QException
   {
      QInstance                qInstance               = QContext.getQInstance();
      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      QFieldMetaData           shapeField              = qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("favoriteShapeId");

      TestUtils.insertDefaultShapes(qInstance);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert that we don't re-run queries for cached values, even ones that aren't found (e.g., 4 below). //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      MemoryRecordStore.setCollectStatistics(true);
      possibleValueTranslator.translatePossibleValue(shapeField, 1);
      possibleValueTranslator.translatePossibleValue(shapeField, 2);
      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran 2 queries so far");
      assertNull(possibleValueTranslator.translatePossibleValue(shapeField, 4));
      assertNull(possibleValueTranslator.translatePossibleValue(shapeField, 4));
      assertEquals(3, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran 3 queries in total");
   }



   /*******************************************************************************
    ** Make sure that if we have 2 different PVS's pointed at the same 1 table,
    ** that we avoid re-doing queries, and that we actually get different (formatted) values.
    *******************************************************************************/
   @Test
   void testPossibleValueTableMultiplePvsForATable() throws QException
   {
      QInstance      qInstance   = QContext.getQInstance();
      QTableMetaData shapeTable  = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      QTableMetaData personTable = qInstance.getTable(TestUtils.TABLE_NAME_PERSON);

      ////////////////////////////////////////////////////////////////////
      // define a second version of the Shape PVS, with a unique format //
      ////////////////////////////////////////////////////////////////////
      qInstance.addPossibleValueSource(new QPossibleValueSource()
         .withName("shapeV2")
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TestUtils.TABLE_NAME_SHAPE)
         .withValueFormat("%d: %s")
         .withValueFields(List.of("id", "label"))
      );

      //////////////////////////////////////////////////////
      // use that PVS in a new column on the person table //
      //////////////////////////////////////////////////////
      personTable.addField(new QFieldMetaData("currentShapeId", QFieldType.INTEGER)
         .withPossibleValueSourceName("shapeV2")
      );

      TestUtils.insertDefaultShapes(qInstance);

      ///////////////////////////////////////////////////////
      // define a list of persons pointing at those shapes //
      ///////////////////////////////////////////////////////
      List<QRecord> personRecords = List.of(
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 1).withValue("currentShapeId", 2),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 1).withValue("currentShapeId", 3),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 2).withValue("currentShapeId", 3),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 2).withValue("currentShapeId", 3)
      );

      /////////////////////////
      // translate the PVS's //
      /////////////////////////
      MemoryRecordStore.setCollectStatistics(true);
      new QPossibleValueTranslator(qInstance, new QSession()).translatePossibleValuesInRecords(personTable, personRecords);

      /////////////////////////////////
      // assert only 1 query was ran //
      /////////////////////////////////
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should only run 1 query");

      ////////////////////////////////////////
      // assert expected values and formats //
      ////////////////////////////////////////
      assertEquals("Triangle", personRecords.get(0).getDisplayValue("favoriteShapeId"));
      assertEquals("2: Square", personRecords.get(0).getDisplayValue("currentShapeId"));
      assertEquals("Triangle", personRecords.get(1).getDisplayValue("favoriteShapeId"));
      assertEquals("3: Circle", personRecords.get(1).getDisplayValue("currentShapeId"));
      assertEquals("Square", personRecords.get(2).getDisplayValue("favoriteShapeId"));
      assertEquals("3: Circle", personRecords.get(2).getDisplayValue("currentShapeId"));
   }



   /*******************************************************************************
    ** Make sure that if we have 2 different PVS's pointed at the same 1 table,
    ** that we avoid re-doing queries, and that we actually get different (formatted) values.
    *******************************************************************************/
   @Test
   void testCustomPossibleValue() throws QException
   {
      QInstance      qInstance   = QContext.getQInstance();
      QTableMetaData personTable = qInstance.getTable(TestUtils.TABLE_NAME_PERSON);
      String         fieldName   = "customValue";

      //////////////////////////////////////////////////////////////
      // define a list of persons with values in the custom field //
      //////////////////////////////////////////////////////////////
      List<QRecord> personRecords = List.of(
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue(fieldName, 1),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue(fieldName, 2),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue(fieldName, "Buckle my shoe"),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue(fieldName, -1) // should come out of the CustomPossibleProvider as null
      );

      /////////////////////////
      // translate the PVS's //
      /////////////////////////
      new QPossibleValueTranslator(qInstance, new QSession()).translatePossibleValuesInRecords(personTable, personRecords);

      ////////////////////////////////////////
      // assert expected values and formats //
      ////////////////////////////////////////
      assertEquals("Custom[1]", personRecords.get(0).getDisplayValue(fieldName));
      assertEquals("Custom[2]", personRecords.get(1).getDisplayValue(fieldName));
      assertEquals("Custom[Buckle my shoe]", personRecords.get(2).getDisplayValue(fieldName));
      assertNull(personRecords.get(3).getDisplayValue(fieldName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetDisplayValuesInRecords()
   {
      QTableMetaData table = TestUtils.defineTablePerson();

      /////////////////////////////////////////////////////////////////
      // first, make sure it doesn't crash with null or empty inputs //
      /////////////////////////////////////////////////////////////////
      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), new QSession());
      possibleValueTranslator.translatePossibleValuesInRecords(table, null);
      possibleValueTranslator.translatePossibleValuesInRecords(table, Collections.emptyList());

      List<QRecord> records = List.of(
         new QRecord()
            .withValue("firstName", "Tim")
            .withValue("lastName", "Chamberlain")
            .withValue("price", new BigDecimal("3.50"))
            .withValue("homeStateId", 1),
         new QRecord()
            .withValue("firstName", "Tyler")
            .withValue("lastName", "Samples")
            .withValue("price", new BigDecimal("174999.99"))
            .withValue("homeStateId", 2)
      );

      possibleValueTranslator.translatePossibleValuesInRecords(table, records);

      assertNull(records.get(0).getRecordLabel()); // regular display stuff NOT done by PVS translator
      assertNull(records.get(0).getDisplayValue("price"));

      assertEquals("IL", records.get(0).getDisplayValue("homeStateId"));
      assertEquals("MO", records.get(1).getDisplayValue("homeStateId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueWithSecondaryPossibleValueLabel() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      qInstance.addTable(new QTableMetaData()
         .withName("city")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("regionId", QFieldType.INTEGER).withPossibleValueSourceName("region")));

      qInstance.addTable(new QTableMetaData()
         .withName("region")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s of %s")
         .withRecordLabelFields("name", "countryId")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("countryId", QFieldType.INTEGER).withPossibleValueSourceName("country")));

      qInstance.addTable(new QTableMetaData()
         .withName("country")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING)));

      qInstance.addPossibleValueSource(new QPossibleValueSource()
         .withName("region")
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName("region")
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY));

      qInstance.addPossibleValueSource(new QPossibleValueSource()
         .withName("country")
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName("country")
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY));

      List<QRecord> regions   = List.of(new QRecord().withValue("id", 11).withValue("name", "Missouri").withValue("countryId", 111));
      List<QRecord> countries = List.of(new QRecord().withValue("id", 111).withValue("name", "U.S.A"));

      TestUtils.insertRecords(qInstance.getTable("region"), regions);
      TestUtils.insertRecords(qInstance.getTable("country"), countries);

      MemoryRecordStore.resetStatistics();
      MemoryRecordStore.setCollectStatistics(true);

      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // verify that if we run w/ an empty set for the param limitedToFieldNames, that we do NOT translate the regionId //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         List<QRecord> cities = List.of(new QRecord().withValue("id", 1).withValue("name", "St. Louis").withValue("regionId", 11));
         possibleValueTranslator.translatePossibleValuesInRecords(qInstance.getTable("city"), cities, null, Set.of());
         assertNull(cities.get(0).getDisplayValue("regionId"));
      }

      ////////////////////////////////////////////////////////////////////////
      // ditto a set that contains something, but not the field in question //
      ////////////////////////////////////////////////////////////////////////
      {
         List<QRecord> cities = List.of(new QRecord().withValue("id", 1).withValue("name", "St. Louis").withValue("regionId", 11));
         possibleValueTranslator.translatePossibleValuesInRecords(qInstance.getTable("city"), cities, null, Set.of("foobar"));
         assertNull(cities.get(0).getDisplayValue("regionId"));
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now re-run, w/ regionId - and we should see it get translated - and - the possible-value that it uses (countryId) as part of its label also gets translated. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         List<QRecord> cities = List.of(new QRecord().withValue("id", 1).withValue("name", "St. Louis").withValue("regionId", 11));
         possibleValueTranslator.translatePossibleValuesInRecords(qInstance.getTable("city"), cities, null, Set.of("regionId"));
         assertEquals("Missouri of U.S.A", cities.get(0).getDisplayValue("regionId"));
      }

      /////////////////////////////////////////////////////////////////////////////////
      // finally, verify that a null limitedToFieldNames means to translate them all //
      /////////////////////////////////////////////////////////////////////////////////
      {
         List<QRecord> cities = List.of(new QRecord().withValue("id", 1).withValue("name", "St. Louis").withValue("regionId", 11));
         possibleValueTranslator.translatePossibleValuesInRecords(qInstance.getTable("city"), cities, null, null);
         assertEquals("Missouri of U.S.A", cities.get(0).getDisplayValue("regionId"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testClearingInternalCaches() throws QException
   {
      QInstance                qInstance               = QContext.getQInstance();
      QTableMetaData           personTable             = qInstance.getTable(TestUtils.TABLE_NAME_PERSON);
      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      QFieldMetaData           shapeField              = qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("favoriteShapeId");

      TestUtils.insertDefaultShapes(qInstance);
      TestUtils.insertExtraShapes(qInstance);

      List<QRecord> personRecords = List.of(
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 1),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 2),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 3),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 4),
         new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON).withValue("favoriteShapeId", 5)
      );

      MemoryRecordStore.setCollectStatistics(true);
      MemoryRecordStore.resetStatistics();

      possibleValueTranslator.primePvsCache(personTable, personRecords, null, null);
      assertEquals("Triangle", possibleValueTranslator.translatePossibleValue(shapeField, 1));
      assertEquals("Square", possibleValueTranslator.translatePossibleValue(shapeField, 2));
      assertEquals("Circle", possibleValueTranslator.translatePossibleValue(shapeField, 3));
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran just 1 query");

      possibleValueTranslator.primePvsCache(personTable, personRecords, null, null);
      assertEquals("Triangle", possibleValueTranslator.translatePossibleValue(shapeField, 1));
      assertEquals("Square", possibleValueTranslator.translatePossibleValue(shapeField, 2));
      assertEquals("Circle", possibleValueTranslator.translatePossibleValue(shapeField, 3));
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should still just have ran just 1 query");

      possibleValueTranslator.setMaxSizePerPvsCache(2);
      possibleValueTranslator.primePvsCache(personTable, personRecords, null, null);

      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Now, should have ran another query");

      assertEquals("Triangle", possibleValueTranslator.translatePossibleValue(shapeField, 1));
      assertEquals("Square", possibleValueTranslator.translatePossibleValue(shapeField, 2));
      assertEquals("Circle", possibleValueTranslator.translatePossibleValue(shapeField, 3));

      ///////////////////////////
      // reset and start again //
      ///////////////////////////
      possibleValueTranslator = new QPossibleValueTranslator(qInstance, new QSession());
      MemoryRecordStore.resetStatistics();
      possibleValueTranslator.translatePossibleValuesInRecords(personTable, personRecords);
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran just 1 query");
      possibleValueTranslator.translatePossibleValuesInRecords(personTable, personRecords);
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran just 1 query");

      possibleValueTranslator.setMaxSizePerPvsCache(2);
      possibleValueTranslator.translatePossibleValuesInRecords(personTable, personRecords);
      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran another query");

      MemoryRecordStore.resetStatistics();
      possibleValueTranslator.translatePossibleValuesInRecords(personTable, personRecords.subList(0, 3));
      possibleValueTranslator.translatePossibleValuesInRecords(personTable, personRecords.subList(3, 5));
      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN), "Should have ran 2 more queries");
   }

}
