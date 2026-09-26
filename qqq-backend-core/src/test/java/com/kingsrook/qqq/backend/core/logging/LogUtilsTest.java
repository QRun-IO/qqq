/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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


import java.math.BigDecimal;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.jsonLog;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.logging.LogUtils
 *******************************************************************************/
@SuppressWarnings("checkstyle:FileTabCharacter")
class LogUtilsTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(LogUtilsTest.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      ////////////////
      // null cases //
      ////////////////
      assertEquals("{}", jsonLog());
      assertEquals("{}", jsonLog((LogPair) null));
      assertEquals("{}", jsonLog((LogPair[]) null));
      assertEquals("""
         {"null":null}""", jsonLog(logPair(null, (LogPair) null)));
      assertEquals("""
         {"null":null}""", jsonLog(logPair(null, (LogPair[]) null)));

      //////////////
      // escaping //
      //////////////
      assertEquals("""
         {"f.o.o":"b\\"a\\"r"}""", jsonLog(logPair("f\"o\"o", "b\"a\"r")));

      //////////////////
      // normal stuff //
      //////////////////
      assertEquals("""
         {"foo":"bar"}""", jsonLog(logPair("foo", "bar")));

      assertEquals("""
         {"bar":1}""", jsonLog(logPair("bar", 1)));

      assertEquals("""
         {"baz":3.50}""", jsonLog(logPair("baz", new BigDecimal("3.50"))));

      ////////////////
      // many pairs //
      ////////////////
      assertEquals("""
         {"foo":"bar","bar":1,"baz":3.50}""", jsonLog(logPair("foo", "bar"), logPair("bar", 1), logPair("baz", new BigDecimal("3.50"))));

      //////////////////
      // nested pairs //
      //////////////////
      assertEquals("""
         {"foo":{"bar":1,"baz":2}}""", jsonLog(logPair("foo", logPair("bar", 1), logPair("baz", 2))));

      assertEquals("""
         {
            "foo":
            {
               "bar":1,
               "baz":2
            }
         }""".replaceAll("\\s", ""), jsonLog(logPair("foo", logPair("bar", 1), logPair("baz", 2))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLog2()
   {
      LOG.info(jsonLog(logPair("message", "Doing a thing"), logPair("trackingNo", "1Z123123123"), logPair("Order", logPair("id", 89101324), logPair("client", "ACME"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLogging()
   {
      LOG.info(jsonLog(logPair("message", "Doing a thing"), logPair("trackingNo", "1Z123123123"), logPair("Order", logPair("id", 89101324), logPair("client", "ACME"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("checkstyle:FileTabCharacter")
   @Test
   void testFilterStackTraceMySqlConnection()
   {
      String filtered = LogUtils.filterStackTrace("""
         com.kingsrook.qqq.backend.core.exceptions.QException: Error executing query
         	at com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSQueryAction.execute(RDBMSQueryAction.java:183)
         	at com.kingsrook.qqq.backend.core.actions.tables.QueryAction.execute(QueryAction.java:76)
         	at com.kingsrook.qqq.backend.core.actions.automation.polling.PollingAutomationPerTableRunner.lambda$processTableInsertOrUpdate$0(PollingAutomationPerTableRunner.java:239)
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager.runAsyncJob(AsyncJobManager.java:142)
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager.lambda$startJob$0(AsyncJobManager.java:80)
         	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768)
         	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1760)
         	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:373)
         	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1182)
         	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1655)
         	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1622)
         	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:165)
         Caused by: com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
                  
         The last packet sent successfully to the server was 0 milliseconds ago. The driver has not received any packets from the server.
         	at com.mysql.cj.jdbc.exceptions.SQLError.createCommunicationsException(SQLError.java:174)
         	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:64)
         	at com.mysql.cj.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:828)
         	at com.mysql.cj.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:448)
         	at com.mysql.cj.jdbc.ConnectionImpl.getInstance(ConnectionImpl.java:241)
         	at com.mysql.cj.jdbc.NonRegisteringDriver.connect(NonRegisteringDriver.java:198)
         	at java.sql/java.sql.DriverManager.getConnection(DriverManager.java:681)
         	at java.sql/java.sql.DriverManager.getConnection(DriverManager.java:229)
         	at com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager.getConnection(ConnectionManager.java:69)
         	at com.kingsrook.qqq.backend.module.rdbms.actions.AbstractRDBMSAction.getConnection(AbstractRDBMSAction.java:119)
         	at com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSQueryAction.execute(RDBMSQueryAction.java:108)
         	... 11 more
         Caused by: com.mysql.cj.exceptions.CJCommunicationsException: Communications link failure
                  
         The last packet sent successfully to the server was 0 milliseconds ago. The driver has not received any packets from the server.
         	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
         	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
         	at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
         	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:499)
         	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:480)
         	at com.mysql.cj.exceptions.ExceptionFactory.createException(ExceptionFactory.java:61)
         	at com.mysql.cj.exceptions.ExceptionFactory.createException(ExceptionFactory.java:105)
         	at com.mysql.cj.exceptions.ExceptionFactory.createException(ExceptionFactory.java:151)
         	at com.mysql.cj.exceptions.ExceptionFactory.createCommunicationsException(ExceptionFactory.java:167)
         	at com.mysql.cj.protocol.a.NativeSocketConnection.connect(NativeSocketConnection.java:89)
         	at com.mysql.cj.NativeSession.connect(NativeSession.java:120)
         	at com.mysql.cj.jdbc.ConnectionImpl.connectOneTryOnly(ConnectionImpl.java:948)
         	at com.mysql.cj.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:818)
         	... 19 more
         Caused by: java.net.ConnectException: Connection refused
         	at java.base/sun.nio.ch.Net.connect0(Native Method)
         	at java.base/sun.nio.ch.Net.connect(Net.java:579)
         	at java.base/sun.nio.ch.Net.connect(Net.java:568)
         	at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:588)
         	at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:327)
         	at java.base/java.net.Socket.connect(Socket.java:633)
         	at com.mysql.cj.protocol.StandardSocketFactory.connect(StandardSocketFactory.java:153)
         	at com.mysql.cj.protocol.a.NativeSocketConnection.connect(NativeSocketConnection.java:63)
         	... 22 more
         """);

      System.out.println(filtered);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilterStackTraceApiBadGateway()
   {
      String filtered = LogUtils.filterStackTrace("""
         com.kingsrook.qqq.backend.core.exceptions.QException: Job failed with an error
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop.run(AsyncRecordPipeLoop.java:150) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLExecuteStep.run(StreamedETLExecuteStep.java:107) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.processes.RunBackendStepAction.runStepCode(RunBackendStepAction.java:221) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.processes.RunBackendStepAction.execute(RunBackendStepAction.java:92) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction.runBackendStep(RunProcessAction.java:315) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction.execute(RunProcessAction.java:183) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.scheduler.ScheduleManager.lambda$startProcess$1(ScheduleManager.java:252) ~[app.jar:?]
         	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539) [?:?]
         	at java.util.concurrent.FutureTask.runAndReset(FutureTask.java:305) [?:?]
         	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305) [?:?]
         	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) [?:?]
         	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) [?:?]
         	at java.lang.Thread.run(Thread.java:833) [?:?]
         Caused by: com.kingsrook.qqq.backend.core.exceptions.QException: Error executing query: HTTP GET for table [infoplusShipment] failed with status 502: <html>
         <head><title>502 Bad Gateway</title></head>
         <body>
         <center><h1>502 Bad Gateway</h1></center>
         </body>
         </html>
                  
         	at com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil.doQuery(BaseAPIActionUtil.java:284) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.module.api.actions.APIQueryAction.execute(APIQueryAction.java:44) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.tables.QueryAction.execute(QueryAction.java:77) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep.run(ExtractViaQueryStep.java:87) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLExecuteStep.lambda$run$0(StreamedETLExecuteStep.java:109) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager.runAsyncJob(AsyncJobManager.java:139) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager.lambda$startJob$0(AsyncJobManager.java:77) ~[app.jar:?]
         	at java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768) ~[?:?]
         	... 1 more
         Caused by: com.kingsrook.qqq.backend.core.exceptions.QException: HTTP GET for table [infoplusShipment] failed with status 502: <html>
         <head><title>502 Bad Gateway</title></head>
         <body>
         <center><h1>502 Bad Gateway</h1></center>
         </body>
         </html>
                  
         	at com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil.handleResponseError(BaseAPIActionUtil.java:496) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil.makeRequest(BaseAPIActionUtil.java:873) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil.doQuery(BaseAPIActionUtil.java:243) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.module.api.actions.APIQueryAction.execute(APIQueryAction.java:44) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.tables.QueryAction.execute(QueryAction.java:77) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep.run(ExtractViaQueryStep.java:87) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLExecuteStep.lambda$run$0(StreamedETLExecuteStep.java:109) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager.runAsyncJob(AsyncJobManager.java:139) ~[app.jar:?]
         	at com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager.lambda$startJob$0(AsyncJobManager.java:77) ~[app.jar:?]
         	at java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768) ~[?:?]
         	... 1 more
         """);

      System.out.println(filtered);
   }

}