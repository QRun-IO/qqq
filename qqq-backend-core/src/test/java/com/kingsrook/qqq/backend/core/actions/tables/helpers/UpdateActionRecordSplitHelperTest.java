/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for UpdateActionRecordSplitHelper 
 *******************************************************************************/
class UpdateActionRecordSplitHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      String tableName = getClass().getSimpleName();
      QContext.getQInstance().addTable(new QTableMetaData()
         .withName(tableName)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("A", QFieldType.INTEGER))
         .withField(new QFieldMetaData("B", QFieldType.INTEGER))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME)));

      Instant now = Instant.now();
      UpdateInput updateInput = new UpdateInput(tableName)
         .withRecord(new QRecord().withValue("id", 1).withValue("A", 1))
         .withRecord(new QRecord().withValue("id", 2).withValue("A", 2))
         .withRecord(new QRecord().withValue("id", 3).withValue("B", 3))
         .withRecord(new QRecord().withValue("id", 4).withValue("B", 3))
         .withRecord(new QRecord().withValue("id", 5).withValue("B", 3))
         .withRecord(new QRecord().withValue("id", 6).withValue("A", 4).withValue("B", 5));
      updateInput.getRecords().forEach(r -> r.setValue("modifyDate", now));
      UpdateActionRecordSplitHelper updateActionRecordSplitHelper = new UpdateActionRecordSplitHelper();
      updateActionRecordSplitHelper.init(updateInput);
      ListingHash<List<String>, QRecord> recordsByFieldBeingUpdated = updateActionRecordSplitHelper.getRecordsByFieldBeingUpdated();

      Function<Collection<QRecord>, Set<Integer>> extractIds = (records) ->
         records.stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet());

      //////////////////////////////////////////////////////////////
      // validate the grouping of records by fields-being-updated //
      //////////////////////////////////////////////////////////////
      assertEquals(3, recordsByFieldBeingUpdated.size());
      assertEquals(Set.of(1, 2), extractIds.apply(recordsByFieldBeingUpdated.get(List.of("A", "modifyDate"))));
      assertEquals(Set.of(3, 4, 5), extractIds.apply(recordsByFieldBeingUpdated.get(List.of("B", "modifyDate"))));
      assertEquals(Set.of(6), extractIds.apply(recordsByFieldBeingUpdated.get(List.of("A", "B", "modifyDate"))));

      ///////////////////////////////////////////////////////////////////
      // validate the output records were built, in the order expected //
      ///////////////////////////////////////////////////////////////////
      List<QRecord> outputRecords = updateActionRecordSplitHelper.getOutputRecords();
      for(int i = 0; i < outputRecords.size(); i++)
      {
         assertEquals(i + 1, outputRecords.get(i).getValueInteger("id"));
      }

      /////////////////////////////////////////////////////
      // test the areAllValuesBeingUpdatedTheSame method //
      /////////////////////////////////////////////////////
      Function<List<String>, Boolean> runAreAllValuesBeingUpdatedTheSame = (fields) ->
         UpdateActionRecordSplitHelper.areAllValuesBeingUpdatedTheSame(updateInput, recordsByFieldBeingUpdated.get(fields), fields);

      assertFalse(runAreAllValuesBeingUpdatedTheSame.apply(List.of("A", "modifyDate")));
      assertTrue(runAreAllValuesBeingUpdatedTheSame.apply(List.of("B", "modifyDate")));
      assertTrue(runAreAllValuesBeingUpdatedTheSame.apply(List.of("A", "B", "modifyDate")));

      ////////////////////////////////////////////////////////////////////
      // make sure that the override of the logic for this method works //
      ////////////////////////////////////////////////////////////////////
      updateInput.setAreAllValuesBeingUpdatedTheSame(true);
      assertTrue(runAreAllValuesBeingUpdatedTheSame.apply(List.of("A", "modifyDate")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordsWithErrors()
   {
      String tableName = getClass().getSimpleName() + "WithErrors";
      QContext.getQInstance().addTable(new QTableMetaData()
         .withName(tableName)
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("A", QFieldType.INTEGER)));

      {
         UpdateInput updateInput = new UpdateInput(tableName)
            .withRecord(new QRecord().withValue("id", 1).withValue("A", 1).withError(new SystemErrorStatusMessage("error")))
            .withRecord(new QRecord().withValue("id", 2).withValue("A", 2).withError(new SystemErrorStatusMessage("error")))
            .withRecord(new QRecord().withValue("id", 2).withValue("A", 3).withError(new SystemErrorStatusMessage("error")));
         UpdateActionRecordSplitHelper updateActionRecordSplitHelper = new UpdateActionRecordSplitHelper();
         updateActionRecordSplitHelper.init(updateInput);
         assertFalse(updateActionRecordSplitHelper.getHaveAnyWithoutErrors());
      }

   }

}