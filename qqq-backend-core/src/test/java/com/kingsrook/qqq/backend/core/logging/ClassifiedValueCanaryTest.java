/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.logging;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValues;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.WipeAuditFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.WhiteSpaceBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Cross-egress canary; scans actual stored rows, errors, log events and CSV bytes.
 *******************************************************************************/
class ClassifiedValueCanaryTest extends BaseTest
{
   /*******************************************************************************
    ** Invalid classified numbers stay private through actions and logged causes.
    *******************************************************************************/
   @Test
   void invalidClassifiedNumbers() throws Exception
   {
      String marker = "CANARY-PHI-" + canaryToken();
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      String name = table.getName();
      QFieldMetaData field = new QFieldMetaData("reading", QFieldType.INTEGER).withLabel("Reading")
         .withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      table.addField(field);
      new InsertAction().execute(new InsertInput(name).withRecord(new QRecord().withValue("id", 101).withValue("reading", 1)));
      field.withBehavior(WhiteSpaceBehavior.TRIM);
      assertEquals(1, new QueryAction().execute(new QueryInput(name)
         .withFilter(new QQueryFilter(new QFilterCriteria("reading", QCriteriaOperator.EQUALS, " 1 ")))).getRecords().size());
      QLoggerTest.ListAppender appender = QLoggerTest.ListAppender.createAppender();
      org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
      appender.start();
      logger.addAppender(appender);
      try
      {
         for(DataClassification classification : List.of(DataClassification.PHI, DataClassification.PII))
         {
            field.withSupplementalMetaData(new AiFieldMetaData().withClassification(classification));
            Exception insert = assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(name)
               .withRecord(new QRecord().withValue("id", 102).withValue("reading", marker))));
            Exception update = assertThrows(Exception.class, () -> new UpdateAction().execute(new UpdateInput().withTableName(name)
               .withRecord(new QRecord().withValue("id", 101).withValue("reading", marker))));
            Exception query = assertThrows(Exception.class, () -> new QueryAction().execute(new QueryInput(name)
               .withFilter(new QQueryFilter(new QFilterCriteria("reading", QCriteriaOperator.EQUALS, marker)))));
            for(Exception exception : List.of(insert, update, query))
            {
               assertSafeCauses(exception, marker);
               Throwable cause = exception;
               while(cause.getCause() != null)
               {
                  cause = cause.getCause();
               }
               assertEquals("Value [***] could not be converted for Reading", cause.getMessage());
               QLogger.getLogger(ClassifiedValueCanaryTest.class).warn("Invalid classified number", exception);
            }
         }
      }
      finally
      {
         logger.removeAppender(appender);
         appender.stop();
      }
      assertFalse(appender.getEventList().isEmpty());
      appender.getEventList().forEach(event ->
      {
         assertNoFragments(event.getMessage().getFormattedMessage(), marker);
         assertSafeCauses(event.getThrown(), marker);
      });
   }



   /*******************************************************************************
    ** Cache and association comparisons can run without a JoinsContext.
    *******************************************************************************/
   @Test
   void classifiedConversionWithoutJoinsContext()
   {
      String marker = "CANARY-PHI-" + canaryToken();
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.addField(new QFieldMetaData("readingDate", QFieldType.DATE).withLabel("Reading Date")
         .withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI)));
      QRecord record = new QRecord().withTableName(table.getName()).withValue("readingDate", LocalDate.now());
      QValueException exception = assertThrows(QValueException.class, () -> BackendQueryFilterUtils.doesRecordMatch(
         new QQueryFilter(new QFilterCriteria("readingDate", QCriteriaOperator.GREATER_THAN, marker)), record));
      assertEquals("Value [***] could not be converted for Reading Date", exception.getMessage());
      assertNull(exception.getCause());
      record.setValue("readingDate", marker);
      assertSafeCauses(assertThrows(QValueException.class, () -> record.getValueLocalDate("readingDate")), marker);
   }



   /*******************************************************************************
    ** Inspect every cause, including parser exceptions attached to log events.
    *******************************************************************************/
   private static void assertSafeCauses(Throwable throwable, String marker)
   {
      while(throwable != null)
      {
         assertNoFragments(String.valueOf(throwable.getMessage()), marker);
         throwable = throwable.getCause();
      }
   }



   /*******************************************************************************
    ** No classified substring escapes.
    *******************************************************************************/
   @Test
   void noClassifiedSubstringEscapes() throws Exception
   {
      String marker = "CANARY-PHI-" + canaryToken();
      String updatedMarker = "CANARY-PHI-" + canaryToken();
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      String name = table.getName();
      table.getField("firstName").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PII));
      table.getField("lastName").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      table.setRecordLabelFields(List.of("firstName", "lastName"));
      table.setUniqueKeys(List.of(new UniqueKey("firstName")));
      table.setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      List<String> outputs = new ArrayList<>();
      QLoggerTest.ListAppender appender = QLoggerTest.ListAppender.createAppender();
      org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
      appender.start();
      logger.addAppender(appender);
      try
      {
         QRecord record = new QRecord().withTableName(name).withValue("id", 101).withValue("firstName", marker).withValue("lastName", marker);
         QRecord inserted = new InsertAction().execute(new InsertInput(name).withRecord(record)).getRecords().get(0);
         assertTrue(inserted.getErrors() == null || inserted.getErrors().isEmpty());
         QRecord duplicate = new InsertAction().execute(new InsertInput(name).withRecord(new QRecord().withValue("id", 102).withValue("firstName", marker).withValue("lastName", marker))).getRecords().get(0);
         assertFalse(duplicate.getErrors().isEmpty());
         outputs.add(JsonUtils.toJson(duplicate.getErrors()));
         QLogger.getLogger(ClassifiedValueCanaryTest.class).info("Record log", logPair("record", record));
         QLogger.getLogger(ClassifiedValueCanaryTest.class).info("Record " + record);
         QLogger.getLogger(ClassifiedValueCanaryTest.class).info("Record {}", (Object) record);
         new UpdateAction().execute(new UpdateInput().withTableName(name).withRecord(new QRecord().withValue("id", 101).withValue("firstName", updatedMarker).withValue("lastName", updatedMarker)));

         table.getField("lastName").withMaxLength(3).withBehavior(ValueTooLongBehavior.ERROR);
         QRecord invalid = new InsertAction().execute(new InsertInput(name).withRecord(new QRecord().withValue("id", 103).withValue("firstName", marker).withValue("lastName", marker))).getRecords().get(0);
         assertFalse(invalid.getErrors().isEmpty());
         outputs.add(JsonUtils.toJson(invalid.getErrors()));
         table.getField("lastName").withMaxLength(200);
         QFieldMetaData numeric = new QFieldMetaData("reading", QFieldType.INTEGER).withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
         table.addField(numeric);
         QValueException parse = assertThrows(QValueException.class, () -> ValueUtils.getValueAsFieldType(table, numeric, marker));
         assertNull(parse.getCause());
         outputs.add(parse.toString());
         String lockErrors = JsonUtils.toJson(ValidateRecordSecurityLockHelper.validateRecordSecurityValue(table,
            new RecordSecurityLock().withFieldName("firstName").withSecurityKeyType("canaryKey"), marker, QFieldType.STRING,
            ValidateRecordSecurityLockHelper.Action.UPDATE, Map.of(), QContext.getQSession()));
         assertTrue(lockErrors.contains("***"));
         outputs.add(lockErrors);

         FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance((t, f) -> ClassifiedValues.isClassified(t, f.getName())
            ? new FieldAccessDecision(FieldAccessTier.MASKED, FieldMaskStyle.FULL, "restricted") : FieldAccessDecision.FULL);
         QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         ExportInput export = new ExportInput();
         export.setTableName(name);
         new ExportAction().execute(export.withReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(bytes)));
         outputs.add(bytes.toString(StandardCharsets.UTF_8));
         assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("***"));
         // Restore the ordinary writer for the remaining mutation paths.
         QContext.setQSession(new com.kingsrook.qqq.backend.core.model.session.QSystemUserSession());
         new UpdateAction().execute(new UpdateInput().withTableName(name).withFlag(new WipeAuditFlag("retention"))
            .withRecord(new QRecord().withValue("id", 101).withValue("firstName", null)));
         new DeleteAction().execute(new DeleteInput().withTableName(name).withPrimaryKey(101));
         List<QRecord> details = TestUtils.queryTable("auditDetail");
         assertTrue(details.stream().anyMatch(r -> "Removed Last Name".equals(r.getValue("message"))));
         assertTrue(details.stream().anyMatch(r -> "wiped:v1:retention".equals(r.getValue("newValue"))));
         outputs.add(JsonUtils.toJson(details));
         outputs.add(JsonUtils.toJson(TestUtils.queryTable("audit")));
      }
      finally
      {
         logger.removeAppender(appender);
         appender.stop();
      }
      assertFalse(appender.getEventList().isEmpty());
      appender.getEventList().forEach(event -> outputs.add(event.getMessage().getFormattedMessage()));
      for(String output : outputs)
      {
         assertNoFragments(output, marker);
         assertNoFragments(output, updatedMarker);
      }
   }



   /*******************************************************************************
    ** Every longer substring contains a five-character substring, so scanning five is sufficient.
    *******************************************************************************/
   private static void assertNoFragments(String output, String marker)
   {
      for(int start = 0; start <= marker.length() - 5; start++)
      {
         assertFalse(output.contains(marker.substring(start, start + 5)), "Classified fragment escaped into captured output");
      }
   }


   /*******************************************************************************
    ** A random marker body made only of the letters g-v, so that no 5-character
    ** window of it can coincide with hex ids, UUIDs or timestamps in a payload.
    *******************************************************************************/
   private static String canaryToken()
   {
      StringBuilder token = new StringBuilder();
      for(char c : UUID.randomUUID().toString().replace("-", "").toCharArray())
      {
         token.append((char) ('g' + Character.digit(c, 16)));
      }
      return (token.toString());
   }
}
