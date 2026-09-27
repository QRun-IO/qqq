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

package com.kingsrook.sampleapp;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.RenderedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.RenderedReportStatus;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReport;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RenderSavedReportMetaDataProducer;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableProcessRunner;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.JobListenerSupport;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Dispatch saved reports through owned Quartz workers and a loopback SMTP sink.
 *******************************************************************************/
class SampleScheduledReportContractTest
{
   private static final String OWNER = "owned-scheduled-report-user";
   private static final Set<String> OWNER_PERMISSIONS = Set.of("person.read", "renderSavedReport.hasAccess");
   private static final AtomicInteger customizerCalls = new AtomicInteger();
   private static final List<Integer> visitedVariants = new ArrayList<>();

   @TempDir
   Path directory;

   private QInstance instance;
   private QSession schedulerSession;
   private Integer reportId;
   private QuartzScheduler scheduler;
   private SmtpSink smtp;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      schedulerSession = new QSession().withUser(new QUser().withIdReference("owned-scheduler-service"))
         .withPermissions(OWNER_PERMISSIONS).withSecurityKeyValue("schedulerOnly", "private-key")
         .withBackendVariants(new HashMap<>(Map.of("ownedTenant", "owned-tenant")));
      schedulerSession.setIdReference("owned-scheduler-authentication");
      schedulerSession.withValue("apiName", "scheduler-api");
      QContext.init(instance, schedulerSession);
      new SavedReportsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME,
         SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME, table ->
         {
            if(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME.equals(table.getName()))
            {
               table.setBackendDetails(new FilesystemTableBackendDetails().withBasePath("reports")
                  .withCardinality(Cardinality.MANY).withRecordFormat(RecordFormat.CSV));
            }
         });
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getProcess("renderSavedReport").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      customizerCalls.set(0);
      QRecord saved = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withLabel("Owned &copy; scheduled people # ü").withUserId(OWNER).withTableName("person")
         .withColumnsJson("{\"columns\":[{\"name\":\"firstName\"}]}").withQueryFilterJson("{}"))).getRecords().get(0);
      assertTrue(saved.getErrors() == null || saved.getErrors().isEmpty(), String.valueOf(saved.getErrors()));
      reportId = saved.getValueInteger("id");
      assertNotNull(reportId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(scheduler != null)
         {
            scheduler.stop();
            scheduler.unInit();
         }
         if(smtp != null)
         {
            smtp.close();
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Owner identity must not inherit the service account's authentication or grants.
    *******************************************************************************/
   @Test
   void testAutomatedSessionDoesNotInheritSchedulerPrivileges() throws Exception
   {
      QSession owner = new MockAuthenticationModule().createAutomatedSessionForUser(instance, OWNER);
      assertAll(() -> assertEquals(OWNER, owner.getUser().getIdReference()),
         () -> assertFalse(owner.hasPermission("person.read")),
         () -> assertFalse(owner.hasSecurityKeyValue("schedulerOnly", "private-key")),
         () -> assertNotEquals(schedulerSession.getUuid(), owner.getUuid()),
         () -> assertNull(owner.getIdReference()),
         () -> assertNull(owner.getValue("apiName")),
         () -> assertEquals(schedulerSession.getBackendVariants(), owner.getBackendVariants()),
         () -> assertNotSame(schedulerSession.getBackendVariants(), owner.getBackendVariants()),
         () -> assertSame(schedulerSession, QContext.getQSession()));
   }



   /*******************************************************************************
    ** The scheduler's system-session subtype cannot become a report-owner bypass.
    *******************************************************************************/
   @Test
   void testAutomatedSessionFromSystemSchedulerIsOrdinary() throws Exception
   {
      QContext.setQSession(new QSystemUserSession());
      QSession owner = new MockAuthenticationModule().createAutomatedSessionForUser(instance, OWNER);
      assertEquals(QSession.class, owner.getClass());
      assertFalse(owner.hasPermission("person.read"));
      assertEquals(OWNER, owner.getUser().getIdReference());
   }



   /*******************************************************************************
    ** A configured automated customizer grants the owner's own current permissions.
    *******************************************************************************/
   @Test
   void testAutomatedSessionInvokesOwnerCustomizer() throws Exception
   {
      enableOwner();
      schedulerSession.setPermissions(Set.of("scheduler-only"));
      QSession owner = new MockAuthenticationModule().createAutomatedSessionForUser(instance, OWNER);
      assertEquals(1, customizerCalls.get());
      assertEquals(OWNER_PERMISSIONS, owner.getPermissions());
      assertFalse(owner.hasPermission("scheduler-only"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAutomatedSessionRejectsMissingOwner()
   {
      assertThrows(QAuthenticationException.class, () -> new MockAuthenticationModule().createAutomatedSessionForUser(instance, null));
      assertThrows(QAuthenticationException.class, () -> new MockAuthenticationModule().createAutomatedSessionForUser(instance, " "));
   }



   /*******************************************************************************
    ** The real SMTP provider delivers a link to a real archive of canonical H2 rows.
    *******************************************************************************/
   @Test
   void testQuartzDeliversOwnedReportThroughSmtp() throws Exception
   {
      enableOwner();
      configureSmtp(false);
      assertNull(dispatch("recipient@example.com"));
      String message = smtp.message.get(3, TimeUnit.SECONDS);
      Path archive = renderedArchive(RenderedReportStatus.COMPLETE);
      assertTrue(message.contains("Subject: Owned scheduled delivery"), message);
      assertTrue(message.contains("recipient@example.com"), message);
      MimeMessage email = new MimeMessage(Session.getInstance(new Properties()),
         new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
      Multipart content = (Multipart) email.getContent();
      String text = (String) content.getBodyPart(0).getContent();
      String prefix = "To download your report, open this URL in your browser: ";
      assertTrue(text.startsWith(prefix), text);
      String plainLink = text.substring(prefix.length()).strip();
      Element htmlLink = Jsoup.parse((String) content.getBodyPart(1).getContent()).selectFirst("a");
      assertNotNull(htmlLink);
      assertAll(() -> assertEquals(archive.toRealPath(), Path.of(URI.create(plainLink)).toRealPath()),
         () -> assertEquals(archive.toRealPath(), Path.of(URI.create(htmlLink.attr("href"))).toRealPath()),
         () -> assertEquals(archive.getFileName().toString(), htmlLink.text()),
         () -> assertEquals(Files.readString(archive), Files.readString(Path.of(URI.create(plainLink)))));
      assertEquals(1, customizerCalls.get());
   }



   /*******************************************************************************
    ** A rejected SMTP recipient retains completed archive bytes and failed history.
    *******************************************************************************/
   @Test
   void testQuartzSmtpRejectionReportsFailure() throws Exception
   {
      enableOwner();
      configureSmtp(true);
      dispatch("rejected@example.com");
      renderedArchive(RenderedReportStatus.FAILED);
      assertTrue(smtp.rejected);
   }



   /*******************************************************************************
    ** Invalid recipients fail before report generation.
    *******************************************************************************/
   @Test
   void testScheduleRejectsInvalidRecipientBeforeDispatch() throws Exception
   {
      QRecord schedule = new InsertAction().execute(new InsertInput(ScheduledReport.TABLE_NAME).withRecordEntity(new ScheduledReport()
         .withSavedReportId(reportId).withUserId(OWNER).withFormat("CSV").withInputValues("{}")
         .withCronExpression("0 0 0 1 1 ?").withCronTimeZoneId("UTC").withIsActive(false)
         .withToAddresses("not-an-email").withSubject("Owned invalid recipient"))).getRecords().get(0);
      assertTrue(schedule.getErrors() != null && !schedule.getErrors().isEmpty());
      assertTrue(history().isEmpty());
      assertFalse(Files.exists(directory.resolve("reports")));
   }



   /*******************************************************************************
    ** Missing delivery configuration retains completed archive bytes and failed history.
    *******************************************************************************/
   @Test
   void testQuartzMissingMessagingProviderReportsFailure() throws Exception
   {
      enableOwner();
      instance.getProcess("renderSavedReport").getBackendStep("pre").getInputMetaData()
         .getFieldThrowing(RenderSavedReportMetaDataProducer.FROM_EMAIL_ADDRESS).setDefaultValue("sender@example.com");
      dispatch("recipient@example.com");
      renderedArchive(RenderedReportStatus.FAILED);
   }



   /*******************************************************************************
    ** Auth0 retains its owner hook while starting from an isolated identity.
    *******************************************************************************/
   @Test
   void testAuth0AutomatedOwnerStartsIsolated() throws Exception
   {
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new Auth0AuthenticationMetaData()
         .withCustomizer(new QCodeReference(OwnerPermissions.class)));
      assertIsolatedOwnerHook(new Auth0AuthenticationModule());
   }



   /*******************************************************************************
    ** OAuth2 also invokes the existing automated-owner extension point.
    *******************************************************************************/
   @Test
   void testOAuth2AutomatedOwnerStartsIsolated() throws Exception
   {
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new OAuth2AuthenticationMetaData()
         .withCustomizer(new QCodeReference(OwnerPermissions.class)));
      assertIsolatedOwnerHook(new OAuth2AuthenticationModule());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertIsolatedOwnerHook(QAuthenticationModuleInterface module) throws Exception
   {
      QSession owner = module.createAutomatedSessionForUser(instance, OWNER);
      assertEquals(1, customizerCalls.get());
      assertEquals(OWNER_PERMISSIONS, owner.getPermissions());
      assertFalse(owner.hasSecurityKeyValue("schedulerOnly", "private-key"));
      assertNotEquals(schedulerSession.getUuid(), owner.getUuid());
      assertNull(owner.getIdReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuartzRejectsMismatchedAutomatedOwner() throws Exception
   {
      instance.getAuthentication().setCustomizer(new QCodeReference(WrongOwner.class));
      dispatch("recipient@example.com");
      assertTrue(history().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuartzRejectsInvalidAutomatedSession() throws Exception
   {
      instance.getAuthentication().setCustomizer(new QCodeReference(InvalidSession.class));
      dispatch("recipient@example.com");
      assertTrue(history().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuartzReportsAutomatedSessionFailure() throws Exception
   {
      instance.getAuthentication().setCustomizer(new QCodeReference(FailedSession.class));
      dispatch("recipient@example.com");
      assertTrue(history().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enableOwner()
   {
      instance.getAuthentication().setCustomizer(new QCodeReference(OwnerPermissions.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configureSmtp(boolean reject) throws Exception
   {
      smtp = new SmtpSink(reject);
      instance.addMessagingProvider(new EmailMessagingProviderMetaData().withSmtpServer("127.0.0.1")
         .withSmtpPort(String.valueOf(smtp.server.getLocalPort())).withName("owned-smtp"));
      var inputs = instance.getProcess("renderSavedReport").getBackendStep("pre").getInputMetaData();
      inputs.getFieldThrowing(RenderSavedReportMetaDataProducer.SES_PROVIDER_NAME).setDefaultValue("owned-smtp");
      inputs.getFieldThrowing(RenderSavedReportMetaDataProducer.FROM_EMAIL_ADDRESS).setDefaultValue("sender@example.com");
   }



   /*******************************************************************************
    ** Run through the production Quartz adapter and process runner, awaiting one fire.
    *******************************************************************************/
   private JobExecutionException dispatch(String recipient) throws Exception
   {
      QRecord schedule = new InsertAction().execute(new InsertInput(ScheduledReport.TABLE_NAME).withRecordEntity(new ScheduledReport()
         .withSavedReportId(reportId).withUserId(OWNER).withFormat("CSV").withInputValues("{}")
         .withCronExpression("0 0 0 1 1 ?").withCronTimeZoneId("UTC").withIsActive(false)
         .withToAddresses(recipient).withSubject("Owned scheduled delivery"))).getRecords().get(0);
      assertTrue(schedule.getErrors() == null || schedule.getErrors().isEmpty(), String.valueOf(schedule.getErrors()));
      String name = "owned-report-" + UUID.randomUUID();
      Properties properties = new Properties();
      properties.setProperty("org.quartz.scheduler.instanceName", name);
      properties.setProperty("org.quartz.threadPool.threadCount", "1");
      SchedulableType type = new SchedulableType().withName("owned-report-process").withRunner(new QCodeReference(SchedulableProcessRunner.class));
      instance.addSchedulableType(type);
      scheduler = QuartzScheduler.initInstance(instance, name, properties, () -> schedulerSession);
      CountDownLatch finished = new CountDownLatch(1);
      AtomicReference<JobExecutionException> failure = new AtomicReference<>();
      new StdSchedulerFactory(properties).getScheduler().getListenerManager().addJobListener(new JobListenerSupport()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public String getName()
         {
            return "owned-report-result";
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception)
         {
            failure.set(exception);
            finished.countDown();
         }
      });
      scheduler.start();
      scheduler.setupSchedulable(new BasicSchedulableIdentity(name, "Owned report acceptance"), type,
         Map.of("processName", "runScheduledReport", "recordId", schedule.getValueInteger("id")),
         new QScheduleMetaData().withRepeatSeconds(3600), true);
      assertTrue(finished.await(5, TimeUnit.SECONDS), "Quartz did not finish its owned scheduled report");
      assertSame(schedulerSession, QContext.getQSession());
      assertEquals("owned-scheduler-service", schedulerSession.getUser().getIdReference());
      return failure.get();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> history() throws Exception
   {
      return QueryAction.execute(RenderedReport.TABLE_NAME, null).stream()
         .filter(record -> reportId.equals(record.getValueInteger("savedReportId"))).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Path renderedArchive(RenderedReportStatus expectedStatus) throws Exception
   {
      List<QRecord> history = history();
      assertEquals(1, history.size());
      assertEquals(expectedStatus.getId(), history.get(0).getValueInteger("renderedReportStatusId"));
      Path archive = directory.resolve("reports").resolve(history.get(0).getValueString("resultPath"));
      String csv = Files.readString(archive);
      assertTrue(csv.contains("Avery"), csv);
      assertEquals(6, csv.lines().count());
      return archive;
   }



   /*******************************************************************************
    ** The development provider uses the same explicit automated-owner hook as auth integrations.
    *******************************************************************************/
   public static class OwnerPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId) throws QAuthenticationException
      {
         if(!OWNER.equals(userId))
         {
            throw new QAuthenticationException("Unknown owned scheduled-report user");
         }
         customizerCalls.incrementAndGet();
         session.setPermissions(OWNER_PERMISSIONS);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class WrongOwner extends OwnerPermissions
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId) throws QAuthenticationException
      {
         super.customizeAutomatedSessionForUser(qInstance, session, userId);
         session.getUser().setIdReference("different-owner");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class InvalidSession extends OwnerPermissions
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId) throws QAuthenticationException
      {
         super.customizeAutomatedSessionForUser(qInstance, session, userId);
         session.withValue("isInvalid", "true");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailedSession extends OwnerPermissions
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession session, Serializable userId) throws QAuthenticationException
      {
         super.customizeAutomatedSessionForUser(qInstance, session, userId);
         throw new QAuthenticationException("owned-automated-session-failure");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailedVariants implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         Integer variant = (Integer) QContext.getQSession().getBackendVariants().get("ownedPersonVariant");
         visitedVariants.add(variant);
         if(variant == 1 || variant == 3)
         {
            throw new QException("owned-variant-failure-" + variant);
         }
      }
   }



   /*******************************************************************************
    ** Own only one local SMTP transaction; all accepted addresses stay inside this sink.
    *******************************************************************************/
   static class SmtpSink implements AutoCloseable
   {
      private final ServerSocket server;
      private final CompletableFuture<String> message = new CompletableFuture<>();
      private final Thread worker;
      private volatile boolean rejected;



      /*******************************************************************************
       **
       *******************************************************************************/
      SmtpSink(boolean reject) throws Exception
      {
         server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
         server.setSoTimeout(5000);
         worker = Thread.ofPlatform().name("owned-smtp-sink").start(() ->
         {
            try(Socket socket = server.accept();
               BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
               PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8))
            {
               socket.setSoTimeout(5000);
               output.print("220 localhost owned acceptance\r\n");
               output.flush();
               String line;
               while((line = input.readLine()) != null)
               {
                  if(line.startsWith("QUIT"))
                  {
                     output.print("221 closed\r\n");
                     output.flush();
                     break;
                  }
                  if(line.startsWith("RCPT") && reject)
                  {
                     rejected = true;
                     output.print("550 owned-recipient-rejected\r\n");
                  }
                  else if(line.equals("DATA"))
                  {
                     output.print("354 send data\r\n");
                     output.flush();
                     StringBuilder data = new StringBuilder();
                     while((line = input.readLine()) != null && !line.equals("."))
                     {
                        data.append(line.startsWith("..") ? line.substring(1) : line).append('\n');
                     }
                     message.complete(data.toString());
                     output.print("250 accepted\r\n");
                  }
                  else
                  {
                     output.print("250 localhost\r\n");
                  }
                  output.flush();
               }
            }
            catch(Exception e)
            {
               message.completeExceptionally(e);
            }
         });
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      Integer getPort()
      {
         return server.getLocalPort();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close() throws Exception
      {
         server.close();
         worker.join(6000);
         assertFalse(worker.isAlive(), "Owned SMTP sink did not close");
      }
   }
}
