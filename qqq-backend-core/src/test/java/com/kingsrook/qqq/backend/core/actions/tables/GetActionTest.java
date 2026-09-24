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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for GetAction
 **
 *******************************************************************************/
class GetActionTest extends BaseTest
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
    ** At the core level, there isn't much that can be asserted, as it uses the
    ** mock implementation - just confirming that all of the "wiring" works.
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      GetInput request = new GetInput();
      request.setTableName("person");
      request.setPrimaryKey(1);
      request.setShouldGenerateDisplayValues(true);
      request.setShouldTranslatePossibleValues(true);
      GetOutput result = new GetAction().execute(request);
      assertNotNull(result);
      assertNotNull(result.getRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilterFieldBehaviors() throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // insert one shape with a mixed-case name, one with an all-lower name //
      /////////////////////////////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecords(List.of(
         new QRecord().withValue("name", "Triangle"),
         new QRecord().withValue("name", "square")
      )));

      ///////////////////////////////////////////////////////////////////////////
      // now set the shape table's name field to have a to-lower-case behavior //
      ///////////////////////////////////////////////////////////////////////////
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      table.withUniqueKey(new UniqueKey("name"));
      QFieldMetaData field = table.getField("name");
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // confirm that if we query for "Triangle", we can't find it (because query will to-lower-case the criteria) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertNull(GetAction.execute(TestUtils.TABLE_NAME_SHAPE, Map.of("name", "Triangle")));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // confirm that if we query for "SQUARE", we do find it (because query will to-lower-case the criteria) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertNotNull(GetAction.execute(TestUtils.TABLE_NAME_SHAPE, Map.of("name", "sQuArE")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablePersonalization() throws QException
   {
      QContext.getQSession().getUser().setIdReference("jdoe");
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("firstName", "Darin")));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // customize firstName field to do a to-upper-case                                                   //
      // this is verifying that QueryAction.postRecordActions has access to the personalized tableMetaData //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals("Darin", new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(1).withInputSource(QInputSource.USER)).getValueString("firstName"));
      ExamplePersonalizer.addFieldToAddForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY,
         QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("firstName").clone().withBehavior(CaseChangeBehavior.TO_UPPER_CASE),
         QContext.getQSession().getUser().getIdReference());
      assertEquals("DARIN", new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(1).withInputSource(QInputSource.USER)).getValueString("firstName"));
   }



   /*******************************************************************************
    ** static shorthand GetAction.execute(tableName, primaryKey) returns the record
    ** when found, and null when the id does not exist.
    *******************************************************************************/
   @Test
   void testStaticShorthandByPrimaryKey_foundAndMissing() throws QException
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(
         new QRecord().withValue("firstName", "Alice")));

      QRecord found = GetAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, (Serializable) 1);
      assertNotNull(found);
      assertEquals("Alice", found.getValueString("firstName"));

      QRecord missing = GetAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, (Serializable) 999);
      assertNull(missing);
   }



   /*******************************************************************************
    ** static shorthand GetAction.execute(tableName, uniqueKeyMap) returns the record
    ** when found by a unique-key lookup, and null when no match.
    *******************************************************************************/
   @Test
   void testStaticShorthandByUniqueKey_foundAndMissing() throws QException
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecord(
         new QRecord().withValue("name", "circle")));

      QTableMetaData shapeTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SHAPE);
      shapeTable.withUniqueKey(new UniqueKey("name"));

      QRecord found = GetAction.execute(TestUtils.TABLE_NAME_SHAPE, Map.of("name", "circle"));
      assertNotNull(found);
      assertEquals("circle", found.getValueString("name"));

      QRecord missing = GetAction.execute(TestUtils.TABLE_NAME_SHAPE, Map.of("name", "triangle"));
      assertNull(missing);
   }



   /*******************************************************************************
    ** requesting a table that does not exist in the QInstance must throw a
    ** QException rather than a NullPointerException.
    *******************************************************************************/
   @Test
   void testGetUnrecognizedTable_throwsQException()
   {
      assertThatThrownBy(() -> new GetAction().execute(new GetInput("doesNotExist").withPrimaryKey(1)))
         .isInstanceOf(QException.class)
         .hasMessageContaining("doesNotExist");
   }



   /*******************************************************************************
    ** convertGetInputToQueryInput must throw when neither primaryKey nor uniqueKey
    ** is set on the input — prevents silent empty-filter queries.
    *******************************************************************************/
   @Test
   void testConvertGetInputToQueryInput_neitherKeySet_throwsQException()
   {
      assertThatThrownBy(() -> GetAction.convertGetInputToQueryInput(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY)))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Missing required input");
   }



   /*******************************************************************************
    ** executeViaQuery must find the same record as execute() for a basic pkey lookup.
    *******************************************************************************/
   @Test
   void testExecuteViaQuery_basicFetch() throws QException
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(
         new QRecord().withValue("firstName", "Bob")));

      GetInput getInput = new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(1);
      GetOutput output = new GetAction().executeViaQuery(getInput);
      assertNotNull(output.getRecord());
      assertEquals("Bob", output.getRecord().getValueString("firstName"));
   }



   /*******************************************************************************
    ** shouldMaskPasswords must replace the raw value of PASSWORD-type fields with
    ** asterisks while optionally preserving the display value as the real string.
    *******************************************************************************/
   @Test
   void testShouldMaskPasswords() throws QException
   {
      QInstance      qInstance     = QContext.getQInstance();
      QTableMetaData personMemory  = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      String         passwordField = "testPassword";

      personMemory.addField(new QFieldMetaData(passwordField, QFieldType.PASSWORD));

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(
         new QRecord().withValue("firstName", "Carol").withValue(passwordField, "s3cr3t")));

      GetInput maskedInput = new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withPrimaryKey(1)
         .withShouldMaskPasswords(true);
      QRecord maskedRecord = new GetAction().executeForRecord(maskedInput);
      assertNotNull(maskedRecord);
      assertThat(maskedRecord.getValueString(passwordField)).isEqualTo("************");

      GetInput unmaskedInput = new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withPrimaryKey(1)
         .withShouldMaskPasswords(false);
      QRecord unmaskedRecord = new GetAction().executeForRecord(unmaskedInput);
      assertThat(unmaskedRecord.getValueString(passwordField)).isEqualTo("s3cr3t");
   }



   /*******************************************************************************
    ** shouldOmitHiddenFields must strip fields whose isHidden flag is set.
    *******************************************************************************/
   @Test
   void testShouldOmitHiddenFields() throws QException
   {
      QInstance      qInstance   = QContext.getQInstance();
      QTableMetaData personMemory = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      String         hiddenField = "internalNote";

      personMemory.addField(new QFieldMetaData(hiddenField, QFieldType.STRING).withIsHidden(true));

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(
         new QRecord().withValue("firstName", "Dave").withValue(hiddenField, "internal")));

      QRecord omitted = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withPrimaryKey(1)
         .withShouldOmitHiddenFields(true));
      assertNotNull(omitted);
      assertNull(omitted.getValue(hiddenField));

      QRecord included = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withPrimaryKey(1)
         .withShouldOmitHiddenFields(false));
      assertNotNull(included);
      assertEquals("internal", included.getValueString(hiddenField));
   }



}
