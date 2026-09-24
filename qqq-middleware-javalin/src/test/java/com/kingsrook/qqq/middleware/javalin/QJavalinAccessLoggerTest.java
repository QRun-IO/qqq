/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.middleware.javalin.QJavalinAccessLogger.DISABLED_PROPERTY;


/*******************************************************************************
 ** Unit test for QJavalinAccessLogger
 **
 ** Note - we're not injecting any kind of logger mock, so we aren't making any
 ** assertions here - we're just verifying we don't blow up - other than that,
 ** manually verify results by reviewing log
 *******************************************************************************/
class QJavalinAccessLoggerTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDefaultOn() throws QInstanceValidationException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new QJavalinImplementation(qInstance, new QJavalinMetaData());

      System.out.println("All should log");
      QJavalinAccessLogger.logStart("test");
      QJavalinAccessLogger.logEndSuccess();
      QJavalinAccessLogger.logEndFail(new Exception());

      QJavalinAccessLogger.logStart("testSlow");
      QJavalinAccessLogger.logEndSuccessIfSlow(-1);

      QJavalinAccessLogger.logProcessSummary("testProcess", UUID.randomUUID().toString(), new RunProcessOutput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTurnedOffByCode() throws QInstanceValidationException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new QJavalinImplementation(qInstance, new QJavalinMetaData()
         .withLoggerDisabled(true));

      System.out.println("Nothing should log");
      QJavalinAccessLogger.logStart("test");
      QJavalinAccessLogger.logEndSuccess();
      QJavalinAccessLogger.logEndFail(new Exception());

      QJavalinAccessLogger.logStart("testSlow");
      QJavalinAccessLogger.logEndSuccessIfSlow(-1);

      QJavalinAccessLogger.logProcessSummary("testProcess", UUID.randomUUID().toString(), new RunProcessOutput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTurnedOffBySystemPropertyWithJavalinMetaData() throws QInstanceValidationException
   {
      System.setProperty(DISABLED_PROPERTY, "true");
      QInstance qInstance = TestUtils.defineInstance();
      new QJavalinImplementation(qInstance, new QJavalinMetaData());

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logStart("test");
      System.clearProperty(DISABLED_PROPERTY);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTurnedOffBySystemPropertyWithoutJavalinMetaData() throws QInstanceValidationException
   {
      System.setProperty(DISABLED_PROPERTY, "true");
      QInstance qInstance = TestUtils.defineInstance();
      new QJavalinImplementation(qInstance);

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logStart("test");
      System.clearProperty(DISABLED_PROPERTY);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilter() throws QInstanceValidationException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new QJavalinImplementation(qInstance, new QJavalinMetaData()
         .withLoggerDisabled(false)
         .withLogFilter(logEntry -> switch(logEntry.logType())
         {
            case START, PROCESS_SUMMARY -> false;
            case END_SUCCESS, END_SUCCESS_SLOW -> true;
            case END_FAIL -> logEntry.actionName().startsWith("yes");
         }));

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logStart("test"); // shouldn't log
      System.out.println("should log");
      QJavalinAccessLogger.logEndSuccess(); // SHOULD log

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logStart("no"); // shouldn't log
      System.out.println("shouldn't log");
      QJavalinAccessLogger.logEndFail(new Exception()); // shouldn't log

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logStart("yes"); // shouldn't log
      System.out.println("should log");
      QJavalinAccessLogger.logEndFail(new Exception()); // SHOULD log

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logStart("testSlow"); // shouldn't log
      System.out.println("should log");
      QJavalinAccessLogger.logEndSuccessIfSlow(-1); // SHOULD log

      System.out.println("shouldn't log");
      QJavalinAccessLogger.logProcessSummary("testProcess", UUID.randomUUID().toString(), new RunProcessOutput()); // shouldn't log
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSlow()
   {
      System.out.println("should log");
      QJavalinAccessLogger.logStart("test");

      System.out.println("should log");
      SleepUtils.sleep(2, TimeUnit.MILLISECONDS);
      QJavalinAccessLogger.logEndSuccessIfSlow(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLogProcessSummary()
   {
      RunProcessOutput runProcessOutput = new RunProcessOutput();
      runProcessOutput.addValue(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY, new ArrayList<>(List.of(
         new ProcessSummaryLine(Status.OK, 5, "Test")
      )));
      System.out.println("should log");
      QJavalinAccessLogger.logProcessSummary("testProcess", UUID.randomUUID().toString(), runProcessOutput);

      runProcessOutput = new RunProcessOutput();
      runProcessOutput.addValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, new ArrayList<>(List.of(
         new ProcessSummaryLine(Status.OK, 5, "Test")
      )));
      System.out.println("should log");
      QJavalinAccessLogger.logProcessSummary("testProcess", UUID.randomUUID().toString(), runProcessOutput);

   }

}