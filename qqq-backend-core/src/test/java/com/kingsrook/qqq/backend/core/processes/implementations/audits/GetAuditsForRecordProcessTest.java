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

package com.kingsrook.qqq.backend.core.processes.implementations.audits;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for GetAuditsForRecordProcess 
 *******************************************************************************/
class GetAuditsForRecordProcessTest extends BaseTest
{
   private Integer recordId = 1701;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSimple() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit", List.of(
         new QRecord().withValue("message", "My Detail")
      ));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("recordId", recordId);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);
      List<QRecord>    audits           = (List<QRecord>) runProcessOutput.getValue("audits");

      assertEquals(1, audits.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMany() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit A", List.of(
         new QRecord().withValue("message", "Detail 1"),
         new QRecord().withValue("message", "Detail 2")
      ));

      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit B", List.of(
         new QRecord().withValue("message", "Detail 3"),
         new QRecord().withValue("message", "Detail 4")
      ));

      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit C", List.of(
         new QRecord().withValue("message", "Detail 5")
      ));

      //////////////////////////////////////////////////////////////////////
      // run with limit smaller than the number of details we've inserted //
      //////////////////////////////////////////////////////////////////////
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("recordId", recordId);
      input.addValue("limit", 4);
      input.addValue("isSortAscending", true);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);

      List<QRecord> audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(4, audits.size());
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 1"));
      assertThat(audits).allMatch(qRecord -> qRecord.getDisplayValue("auditUserId").equals("John Doe"));
      assertThat(audits).noneMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 5"));
      assertEquals(5, runProcessOutput.getValueInteger("distinctCount"));

      //////////////////////////////////
      // run again with reversed sort //
      //////////////////////////////////
      input.addValue("isSortAscending", false);
      runProcessOutput = new RunProcessAction().execute(input);
      audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(4, audits.size());
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 5"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // a somewhat sloppy mid-audit pagination break issue puts Detail 1 in this result... but we'll lve with it //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(audits).noneMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStrippedFieldDetails() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit A", List.of(
         new QRecord().withValue("message", "Detail 1"),
         new QRecord().withValue("message", "Detail 2").withValue("fieldName", "firstName").withValue("newValue", "John"),
         new QRecord().withValue("message", "Detail 3").withValue("fieldName", "noSuchField").withValue("newValue", "Xyz"),
         new QRecord().withValue("message", "Detail 4").withValue("fieldName", "lastName").withValue("newValue", "Doe")
      ));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("recordId", recordId);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);

      /////////////////////////////////////////////////////////////////////////////////////
      // assert we got the audit details for the valid fields, and the one with no field //
      /////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(3, audits.size());
      assertThat(audits).anyMatch(qRecord -> "firstName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).noneMatch(qRecord -> "noSuchField".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> "lastName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.fieldName") == null);

      ////////////////////////////////////////////////////
      // personalize table to take away firstName field //
      ////////////////////////////////////////////////////
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName", "jdoe");
      QContext.setQSession(new QSession().withUser(new QUser().withIdReference("jdoe")));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert we got the audit details for the valid fields, but not the one we can't see (firstName), and the one with no field //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      runProcessOutput = new RunProcessAction().execute(input);
      audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(2, audits.size());
      assertThat(audits).noneMatch(qRecord -> "firstName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).noneMatch(qRecord -> "noSuchField".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> "lastName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.fieldName") == null);
   }

}