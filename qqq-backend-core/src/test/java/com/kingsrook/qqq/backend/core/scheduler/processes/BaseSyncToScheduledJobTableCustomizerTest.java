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

package com.kingsrook.qqq.backend.core.scheduler.processes;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for BaseSyncToScheduledJobTableCustomizer 
 *******************************************************************************/
class BaseSyncToScheduledJobTableCustomizerTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      new AbstractRecordSyncToScheduledJobProcessTest().beforeEach();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      SchedulerTestUtils.afterEach();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      BaseSyncToScheduledJobTableCustomizer.setTableCustomizers(table, new AbstractRecordSyncToScheduledJobProcessTest.SyncPersonToScheduledJobProcess());

      QRecord person = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("firstName", "Darin"))).getRecords().get(0);
      assertEquals(1, QueryAction.execute(ScheduledJob.TABLE_NAME, null).size());

      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(person.getValue("id")));
      assertEquals(0, QueryAction.execute(ScheduledJob.TABLE_NAME, null).size());
   }

}