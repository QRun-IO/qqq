/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.actions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 **
 *******************************************************************************/
public class PostgreSQLUpdateActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      getBaseRDBMSActionStrategyAndActivateCollectingStatistics();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUpdateNullList() throws QException
   {
      UpdateInput updateInput = initUpdateRequest();
      updateInput.setRecords(null);
      UpdateOutput updateResult = new UpdateAction().execute(updateInput);
      assertEquals(0, updateResult.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUpdateEmptyList() throws QException
   {
      UpdateInput updateInput = initUpdateRequest();
      updateInput.setRecords(Collections.emptyList());
      new UpdateAction().execute(updateInput);
      UpdateOutput updateResult = new UpdateAction().execute(updateInput);
      assertEquals(0, updateResult.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUpdateOne() throws Exception
   {
      UpdateInput updateInput = initUpdateRequest();
      QRecord record = new QRecord()
         .withValue("id", 2)
         .withValue("firstName", "James")
         .withValue("lastName", "Kirk")
         .withValue("email", "jamestk@starfleet.net")
         .withValue("birthDate", "2210-05-20");
      updateInput.setRecords(List.of(record));

      UpdateOutput         updateResult = new UpdateAction().execute(updateInput);
      Map<String, Integer> statistics   = getBaseRDBMSActionStrategy().getStatistics();
      assertEquals(2, statistics.get(BaseRDBMSActionStrategy.STAT_QUERIES_RAN));

      assertEquals(1, updateResult.getRecords().size(), "Should return 1 row");
      assertEquals(2, updateResult.getRecords().get(0).getValue("id"), "Should have id=2 in the row");
      // todo - add errors to QRecord? assertTrue(updateResult.getRecords().stream().noneMatch(qrs -> CollectionUtils.nullSafeHasContents(qrs.getErrors())), "There should be no errors");
      runTestSql("SELECT * FROM person WHERE last_name = 'Kirk'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(2, rs.getInt("id"));
            assertEquals("James", rs.getString("first_name"));
            assertEquals("2210-05-20", rs.getString("birth_date"));
         }
         assertEquals(1, rowsFound);
      }));
      runTestSql("SELECT * FROM person WHERE last_name = 'Maes'", (rs ->
      {
         if(rs.next())
         {
            fail("Should not have found Maes any more.");
         }
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUpdateManyWithDifferentColumnsAndValues() throws Exception
   {
      UpdateInput updateInput = initUpdateRequest();
      QRecord record1 = new QRecord()
         .withValue("id", 1)
         .withValue("firstName", "Darren")
         .withValue("lastName", "From Bewitched")
         .withValue("birthDate", "1900-01-01");

      QRecord record2 = new QRecord()
         .withValue("id", 3)
         .withValue("firstName", "Wilt")
         .withValue("birthDate", null);

      QRecord record3 = new QRecord()
         .withValue("id", 5)
         .withValue("firstName", "Richard")
         .withValue("birthDate", null);

      updateInput.setRecords(List.of(record1, record2, record3));

      UpdateOutput updateResult = new UpdateAction().execute(updateInput);

      // this test runs one batch and one regular query
      Map<String, Integer> statistics = getBaseRDBMSActionStrategy().getStatistics();
      assertEquals(1, statistics.get(BaseRDBMSActionStrategy.STAT_BATCHES_RAN));
      assertEquals(2, statistics.get(BaseRDBMSActionStrategy.STAT_QUERIES_RAN));

      assertEquals(3, updateResult.getRecords().size(), "Should return 3 rows");
      assertEquals(1, updateResult.getRecords().get(0).getValue("id"), "Should have expected ids in the row");
      assertEquals(3, updateResult.getRecords().get(1).getValue("id"), "Should have expected ids in the row");
      assertEquals(5, updateResult.getRecords().get(2).getValue("id"), "Should have expected ids in the row");
      // todo - add errors to QRecord? assertTrue(updateResult.getRecords().stream().noneMatch(qrs -> CollectionUtils.nullSafeHasContents(qrs.getErrors())), "There should be no errors");
      runTestSql("SELECT * FROM person WHERE last_name = 'From Bewitched'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(1, rs.getInt("id"));
            assertEquals("Darren", rs.getString("first_name"));
            assertEquals("From Bewitched", rs.getString("last_name"));
            assertEquals("1900-01-01", rs.getString("birth_date"));
         }
         assertEquals(1, rowsFound);
      }));
      runTestSql("SELECT * FROM person WHERE last_name = 'Chamberlain'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(3, rs.getInt("id"));
            assertEquals("Wilt", rs.getString("first_name"));
            assertNull(rs.getString("birth_date"));
         }
         assertEquals(1, rowsFound);
      }));
      runTestSql("SELECT * FROM person WHERE last_name = 'Richardson'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(5, rs.getInt("id"));
            assertEquals("Richard", rs.getString("first_name"));
            assertNull(rs.getString("birth_date"));
         }
         assertEquals(1, rowsFound);
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUpdateManyWithSameColumnsDifferentValues() throws Exception
   {
      UpdateInput updateInput = initUpdateRequest();
      QRecord record1 = new QRecord()
         .withValue("id", 1)
         .withValue("firstName", "Darren")
         .withValue("lastName", "From Bewitched")
         .withValue("birthDate", "1900-01-01");

      QRecord record2 = new QRecord()
         .withValue("id", 3)
         .withValue("firstName", "Wilt")
         .withValue("lastName", "Tim's Uncle")
         .withValue("birthDate", null);

      updateInput.setRecords(List.of(record1, record2));

      UpdateOutput         updateResult = new UpdateAction().execute(updateInput);
      Map<String, Integer> statistics   = getBaseRDBMSActionStrategy().getStatistics();
      assertEquals(1, statistics.get(BaseRDBMSActionStrategy.STAT_BATCHES_RAN));

      assertEquals(2, updateResult.getRecords().size(), "Should return 2 rows");
      assertEquals(1, updateResult.getRecords().get(0).getValue("id"), "Should have expected ids in the row");
      assertEquals(3, updateResult.getRecords().get(1).getValue("id"), "Should have expected ids in the row");
      // todo - add errors to QRecord? assertTrue(updateResult.getRecords().stream().noneMatch(qrs -> CollectionUtils.nullSafeHasContents(qrs.getErrors())), "There should be no errors");
      runTestSql("SELECT * FROM person WHERE last_name = 'From Bewitched'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(1, rs.getInt("id"));
            assertEquals("Darren", rs.getString("first_name"));
            assertEquals("From Bewitched", rs.getString("last_name"));
            assertEquals("1900-01-01", rs.getString("birth_date"));
         }
         assertEquals(1, rowsFound);
      }));
      runTestSql("SELECT * FROM person WHERE last_name = 'Tim''s Uncle'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(3, rs.getInt("id"));
            assertEquals("Wilt", rs.getString("first_name"));
            assertEquals("Tim's Uncle", rs.getString("last_name"));
            assertNull(rs.getString("birth_date"));
         }
         assertEquals(1, rowsFound);
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUpdateManyWithSameColumnsSameValues() throws Exception
   {
      UpdateInput   updateInput = initUpdateRequest();
      List<QRecord> records     = new ArrayList<>();
      for(int i = 1; i <= 5; i++)
      {
         records.add(new QRecord()
            .withValue("id", i)
            .withValue("birthDate", "1999-09-09"));
      }

      updateInput.setRecords(records);

      UpdateOutput         updateResult = new UpdateAction().execute(updateInput);
      Map<String, Integer> statistics   = getBaseRDBMSActionStrategy().getStatistics();
      assertEquals(2, statistics.get(BaseRDBMSActionStrategy.STAT_QUERIES_RAN));

      assertEquals(5, updateResult.getRecords().size(), "Should return 5 rows");
      // todo - add errors to QRecord? assertTrue(updateResult.getRecords().stream().noneMatch(qrs -> CollectionUtils.nullSafeHasContents(qrs.getErrors())), "There should be no errors");
      runTestSql("SELECT * FROM person WHERE id <= 5", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals("1999-09-09", rs.getString("birth_date"));
         }
         assertEquals(5, rowsFound);
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testModifyDateGetsUpdated() throws Exception
   {
      String originalModifyDate = selectModifyDate(1);

      UpdateInput   updateInput = initUpdateRequest();
      List<QRecord> records     = new ArrayList<>();
      records.add(new QRecord()
         .withValue("id", 1)
         .withValue("firstName", "Johnny Updated"));
      updateInput.setRecords(records);
      new UpdateAction().execute(updateInput);

      String updatedModifyDate = selectModifyDate(1);

      assertTrue(StringUtils.hasContent(originalModifyDate));
      assertTrue(StringUtils.hasContent(updatedModifyDate));
      assertNotEquals(originalModifyDate, updatedModifyDate);
   }



   /*******************************************************************************
    ** This situation - fails in a real mysql, but not in h2... anyway, because mysql
    ** didn't want to convert the date-time string format to a date-time.
    *******************************************************************************/
   @Test
   void testDateTimesCanBeModifiedFromIsoStrings() throws Exception
   {
      UpdateInput   updateInput = initUpdateRequest();
      List<QRecord> records     = new ArrayList<>();
      records.add(new QRecord()
         .withValue("id", 1)
         .withValue("createDate", "2022-10-03T10:29:35Z")
         .withValue("firstName", "Johnny Updated"));
      updateInput.setRecords(records);
      new UpdateAction().execute(updateInput);
   }



   /*******************************************************************************
    ** Make sure that records without a primary key come back with error.
    *******************************************************************************/
   @Test
   void testWithoutPrimaryKeyErrors() throws Exception
   {
      {
         UpdateInput   updateInput = initUpdateRequest();
         List<QRecord> records     = new ArrayList<>();
         records.add(new QRecord()
            .withValue("firstName", "Johnny Updated"));
         updateInput.setRecords(records);
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertFalse(updateOutput.getRecords().get(0).getErrors().isEmpty());
         assertEquals("Missing value in primary key field", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
      }

      {
         UpdateInput   updateInput = initUpdateRequest();
         List<QRecord> records     = new ArrayList<>();
         records.add(new QRecord()
            .withValue("id", null)
            .withValue("firstName", "Johnny Updated"));
         updateInput.setRecords(records);
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertFalse(updateOutput.getRecords().get(0).getErrors().isEmpty());
         assertEquals("Missing value in primary key field", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());
      }

      {
         UpdateInput   updateInput = initUpdateRequest();
         List<QRecord> records     = new ArrayList<>();
         records.add(new QRecord()
            .withValue("id", null)
            .withValue("firstName", "Johnny Not Updated"));
         records.add(new QRecord()
            .withValue("id", 2)
            .withValue("firstName", "Johnny Updated"));
         updateInput.setRecords(records);
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

         assertFalse(updateOutput.getRecords().get(0).getErrors().isEmpty());
         assertEquals("Missing value in primary key field", updateOutput.getRecords().get(0).getErrors().get(0).getMessage());

         assertTrue(updateOutput.getRecords().get(1).getErrors().isEmpty());

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         getInput.setPrimaryKey(2);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals("Johnny Updated", getOutput.getRecord().getValueString("firstName"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String selectModifyDate(Integer id) throws Exception
   {
      StringBuilder modifyDate = new StringBuilder();
      runTestSql("SELECT modify_date FROM person WHERE id = " + id, (rs ->
      {
         if(rs.next())
         {
            modifyDate.append(rs.getString("modify_date"));
         }
      }));
      return (modifyDate.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private UpdateInput initUpdateRequest()
   {
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      return updateInput;
   }

}