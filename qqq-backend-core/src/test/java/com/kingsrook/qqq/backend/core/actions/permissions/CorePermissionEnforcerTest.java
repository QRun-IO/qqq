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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for CorePermissionEnforcer and its use by the core actions
 ** (issues #1061 / #1062): role x table x action matrix, the instance setting
 ** vs per-input ENFORCE / SKIP, nested (trusted) calls from process steps,
 ** customizers, widgets and async children, system sessions, field security
 ** locks, isSensitive in exports, and dry runs.
 *******************************************************************************/
class CorePermissionEnforcerTest extends BaseTest
{
   private static final String TABLE  = TestUtils.TABLE_NAME_PERSON_MEMORY;
   private static final String KEY    = "clearance";
   private static final String WIDGET = "personCountWidget";

   private static final String PROCESS_QUERIES_TABLE = "processQueriesTable";

   private static final AtomicInteger stepQueryCount = new AtomicInteger();



   /*******************************************************************************
    ** set up: personMemory protected by READ/INSERT/EDIT/DELETE permissions, its
    ** email field locked (DENY unless clearance=pii), 2 people inserted (as the
    ** system user), a process + widget that query the table.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      System.clearProperty(CorePermissionEnforcer.SYSTEM_PROPERTY_NAME);
      QInstance qInstance = QContext.getQInstance();

      ////////////////////////////////////////////////////////////////////////
      // explicit, so the tests don't depend on the environment variable   //
      ////////////////////////////////////////////////////////////////////////
      qInstance.setEnforcePermissionsInCoreActions(false);

      qInstance.addSecurityKeyType(new QSecurityKeyType().withName(KEY).withAllAccessKeyName(KEY + "AllAccess"));

      QTableMetaData table = qInstance.getTable(TABLE);
      table.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      table.getField("email").setFieldSecurityLock(new FieldSecurityLock()
         .withSecurityKeyType(KEY)
         .withDefaultBehavior(FieldSecurityLock.Behavior.DENY)
         .withOverrideValues(List.of("pii")));
      table.getField("noOfShoes").setIsSensitive(true);

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_QUERIES_TABLE)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withStep(new QBackendStepMetaData().withName("query").withCode(new QCodeReference(QueryingStep.class))));

      qInstance.addWidget(new QWidgetMetaData()
         .withName(WIDGET)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withCodeReference(new QCodeReference(CountingWidgetRenderer.class)));

      runAsSystem(() -> new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(
         new QRecord().withValue("firstName", "Darin").withValue("lastName", "K").withValue("email", "darin@example.com").withValue("noOfShoes", 4),
         new QRecord().withValue("firstName", "Tim").withValue("lastName", "C").withValue("email", "tim@example.com").withValue("noOfShoes", 2)))));

      stepQueryCount.set(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      System.clearProperty(CorePermissionEnforcer.SYSTEM_PROPERTY_NAME);
   }



   /*******************************************************************************
    ** helpers
    *******************************************************************************/
   private void enableInstanceEnforcement()
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setSession(String... permissions)
   {
      QSession session = newSession();
      for(String permission : permissions)
      {
         session.withPermission(permission);
      }
      QContext.setQSession(session);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private interface UnsafeRunnable
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      void run() throws Exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runAsSystem(UnsafeRunnable runnable) throws QException
   {
      QSession original = QContext.getQSession();
      try
      {
         QContext.setQSession(new QSystemUserSession());
         runnable.run();
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException(e));
      }
      finally
      {
         QContext.setQSession(original);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean isAllowed(UnsafeRunnable runnable) throws Exception
   {
      try
      {
         runnable.run();
         return (true);
      }
      catch(QPermissionDeniedException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ReplaceInput replaceInput()
   {
      ReplaceInput replaceInput = new ReplaceInput();
      replaceInput.setTableName(TABLE);
      return (replaceInput);
   }



   /*******************************************************************************
    ** Matrix: for each role (set of permissions), which of the table actions are
    ** allowed when the instance setting is on.
    *******************************************************************************/
   @Test
   void testRoleByActionMatrixWithInstanceEnforcement() throws Exception
   {
      enableInstanceEnforcement();

      /*******************************************************************************
       ** a role: its permissions, and which actions it should be allowed to do
       *******************************************************************************/
      record Role(String name, List<String> permissions, boolean read, boolean insert, boolean edit, boolean delete)
      {
      }

      List<Role> roles = List.of(
         new Role("none", List.of(), false, false, false, false),
         new Role("reader", List.of(TABLE + ".read"), true, false, false, false),
         new Role("inserter", List.of(TABLE + ".insert"), false, true, false, false),
         new Role("editor", List.of(TABLE + ".read", TABLE + ".edit"), true, false, true, false),
         new Role("deleter", List.of(TABLE + ".delete"), false, false, false, true),
         new Role("full", List.of(TABLE + ".read", TABLE + ".insert", TABLE + ".edit", TABLE + ".delete"), true, true, true, true));

      for(Role role : roles)
      {
         setSession(role.permissions().toArray(new String[0]));
         String message = "role " + role.name();

         assertEquals(role.read(), isAllowed(() -> new QueryAction().execute(new QueryInput(TABLE))), message + " query");
         assertEquals(role.read(), isAllowed(() -> new GetAction().execute(new GetInput(TABLE).withPrimaryKey(1))), message + " get");
         assertEquals(role.read(), isAllowed(() -> new CountAction().execute(new CountInput(TABLE))), message + " count");
         assertEquals(role.read(), isAllowed(() -> new AggregateAction().execute(new AggregateInput(TABLE).withAggregate(new Aggregate("id", AggregateOperator.COUNT)))), message + " aggregate");
         assertEquals(role.insert(), isAllowed(() -> new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("firstName", "New").withValue("lastName", role.name())))), message + " insert");
         assertEquals(role.edit(), isAllowed(() -> new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 2).withValue("lastName", "C2")))), message + " update");
         assertEquals(role.delete(), isAllowed(() -> new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKeys(List.of(-1)))), message + " delete");
         assertEquals(role.insert() && role.edit(), isAllowed(() -> new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("firstName", "lastName")).withPerformDeletes(false).withRecords(List.of(new QRecord().withValue("firstName", "Rep").withValue("lastName", role.name()))))), message + " replace");
      }
   }



   /*******************************************************************************
    ** With the instance setting off (the default), core actions don't enforce -
    ** existing behavior - unless an input asks for ENFORCE.
    *******************************************************************************/
   @Test
   void testDefaultIsOffAndPerInputEnforce() throws Exception
   {
      setSession();
      assertFalse(CorePermissionEnforcer.isInstanceEnforcementEnabled());

      assertEquals(2, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
      assertEquals(2, new CountAction().execute(new CountInput(TABLE)).getCount());

      QueryInput enforced = new QueryInput(TABLE);
      enforced.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new QueryAction().execute(enforced)).isInstanceOf(QPermissionDeniedException.class);

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_QUERIES_TABLE);
      runProcessInput.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new RunProcessAction().execute(runProcessInput)).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** The system property (and, equivalently, the environment variable) turn on
    ** enforcement when the instance doesn't set it; an explicit instance value wins.
    *******************************************************************************/
   @Test
   void testSystemPropertyAndInstanceOverride() throws Exception
   {
      setSession();
      QContext.getQInstance().setEnforcePermissionsInCoreActions(null);
      System.setProperty(CorePermissionEnforcer.SYSTEM_PROPERTY_NAME, "true");
      assertTrue(CorePermissionEnforcer.isInstanceEnforcementEnabled());
      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(TABLE))).isInstanceOf(QPermissionDeniedException.class);

      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      assertFalse(CorePermissionEnforcer.isInstanceEnforcementEnabled());
      assertEquals(2, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
   }



   /*******************************************************************************
    ** SKIP is only available through withPermissionBypass(reason).
    *******************************************************************************/
   @Test
   void testBypassRequiresReason() throws Exception
   {
      enableInstanceEnforcement();
      setSession();

      QueryInput queryInput = new QueryInput(TABLE);
      assertThatThrownBy(() -> queryInput.setPermissionEnforcement(PermissionEnforcement.SKIP)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> queryInput.withPermissionBypass(" ")).isInstanceOf(IllegalArgumentException.class);

      queryInput.withPermissionBypass("unit test of an internal lookup");
      assertEquals(PermissionEnforcement.SKIP, queryInput.getPermissionEnforcement());
      assertEquals("unit test of an internal lookup", queryInput.getPermissionBypassReason());
      assertEquals(2, new QueryAction().execute(queryInput).getRecords().size());

      ////////////////////////////////////////////////////////////
      // runTrusted - for a block of calls (e.g., a custom route) //
      ////////////////////////////////////////////////////////////
      Integer count = CorePermissionEnforcer.runTrusted("unit test", () -> new CountAction().execute(new CountInput(TABLE)).getCount());
      assertEquals(2, count);
      assertEquals(0, QContext.getAuthorizedActionScopeDepth());
      assertThatThrownBy(() -> CorePermissionEnforcer.runTrusted("", () -> 1)).isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** System user sessions pass, even with enforcement on.
    *******************************************************************************/
   @Test
   void testSystemSession() throws Exception
   {
      enableInstanceEnforcement();
      QContext.setQSession(new QSystemUserSession());
      QueryOutput queryOutput = new QueryAction().execute(new QueryInput(TABLE));
      assertEquals(2, queryOutput.getRecords().size());
      assertEquals("darin@example.com", queryOutput.getRecords().get(0).getValueString("email"));
   }



   /*******************************************************************************
    ** A process the user may run can query a table the user can't read (its steps
    ** are nested in RunProcessAction's authorized scope); a process the user may
    ** not run is denied.
    *******************************************************************************/
   @Test
   void testProcessStepsAreTrusted() throws Exception
   {
      enableInstanceEnforcement();

      setSession(PROCESS_QUERIES_TABLE + ".hasAccess");
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_QUERIES_TABLE);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      RunProcessOutput output = new RunProcessAction().execute(runProcessInput);
      assertTrue(output.getException().isEmpty(), () -> String.valueOf(output.getException()));
      assertEquals(2, stepQueryCount.get());

      setSession(TABLE + ".read");
      RunProcessInput denied = new RunProcessInput();
      denied.setProcessName(PROCESS_QUERIES_TABLE);
      assertThatThrownBy(() -> new RunProcessAction().execute(denied)).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** A customizer on a table the user may write runs actions against a table the
    ** user has no permissions on - as framework-internal (trusted) work.
    *******************************************************************************/
   @Test
   void testCustomizersAreTrusted() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_SHAPE).withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(CountingCustomizer.class));
      enableInstanceEnforcement();

      setSession();
      CountingCustomizer.lastCount.set(null);
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecord(new QRecord().withValue("name", "Hexagon")));
      assertEquals(2, CountingCustomizer.lastCount.get());
   }



   /*******************************************************************************
    ** Async children (e.g., a process's streamed extract) inherit the authorized
    ** scope through QContext.capture.
    *******************************************************************************/
   @Test
   void testAuthorizedScopeIsCapturedForChildThreads() throws Exception
   {
      enableInstanceEnforcement();
      setSession();

      AtomicReference<Object> childResult = new AtomicReference<>();
      CapturedContext         captured;
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         captured = QContext.capture();
      }
      assertEquals(1, captured.authorizedActionScopeDepth());

      Thread thread = new Thread(() ->
      {
         try
         {
            QContext.init(captured);
            childResult.set(new CountAction().execute(new CountInput(TABLE)).getCount());
         }
         catch(Exception e)
         {
            childResult.set(e);
         }
         finally
         {
            QContext.clear();
         }
      });
      thread.start();
      thread.join();
      assertEquals(2, childResult.get());

      ///////////////////////////////////////////////////////////////////////////////
      // a context built by hand (4-arg / 2-arg constructors) is never trusted     //
      ///////////////////////////////////////////////////////////////////////////////
      assertEquals(0, new CapturedContext(QContext.getQInstance(), QContext.getQSession()).authorizedActionScopeDepth());
   }



   /*******************************************************************************
    ** Widgets: the widget permission is checked; the renderer's own queries are
    ** trusted.
    *******************************************************************************/
   @Test
   void testWidgets() throws Exception
   {
      enableInstanceEnforcement();
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(QContext.getQInstance().getWidget(WIDGET));

      setSession();
      assertThatThrownBy(() -> new RenderWidgetAction().execute(input)).isInstanceOf(QPermissionDeniedException.class);

      setSession(WIDGET + ".hasAccess");
      RenderWidgetOutput output = new RenderWidgetAction().execute(input);
      assertEquals(2, CountingWidgetRenderer.lastCount.get());
      assertTrue(output != null);
   }



   /*******************************************************************************
    ** Scheduled processes are marked as a system bypass.
    *******************************************************************************/
   @Test
   void testScheduledProcessBypasses() throws Exception
   {
      enableInstanceEnforcement();
      QInstance qInstance = QContext.getQInstance();
      com.kingsrook.qqq.backend.core.scheduler.SchedulerUtils.runProcess(qInstance, () -> newSession(), qInstance.getProcess(PROCESS_QUERIES_TABLE), null, Map.of());
      assertEquals(2, stepQueryCount.get());
   }



   /*******************************************************************************
    ** Field security lock: with enforcement, the locked field is removed from
    ** query/get output and table meta data, filters/sorts/group-bys on it are
    ** denied, and writes to it are denied - unless the session has the override
    ** value or all-access key.  Without enforcement, nothing changes.
    *******************************************************************************/
   @Test
   void testFieldSecurityLock() throws Exception
   {
      setSession(TABLE + ".read", TABLE + ".insert", TABLE + ".edit");

      ////////////////////////////////////////////
      // without enforcement: legacy behavior   //
      ////////////////////////////////////////////
      assertEquals("darin@example.com", new QueryAction().execute(new QueryInput(TABLE)).getRecords().get(0).getValueString("email"));

      enableInstanceEnforcement();

      ////////////////////////////////////////////
      // query/get: value omitted               //
      ////////////////////////////////////////////
      QRecord queried = new QueryAction().execute(new QueryInput(TABLE).withShouldGenerateDisplayValues(true)).getRecords().get(0);
      assertFalse(queried.getValues().containsKey("email"));
      assertFalse(queried.getDisplayValues().containsKey("email"));
      assertEquals("Darin", queried.getValueString("firstName"));

      QRecord got = new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(1));
      assertFalse(got.getValues().containsKey("email"));

      ////////////////////////////////////////////
      // criteria / sort / group-by: denied     //
      ////////////////////////////////////////////
      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.STARTS_WITH, "darin")))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(TABLE).withFilter(new QQueryFilter().withSubFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("email"))))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new CountAction().execute(new CountInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.IS_NOT_BLANK)))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TABLE).withAggregate(new Aggregate("id", AggregateOperator.COUNT)).withGroupBy(new GroupBy(QContext.getQInstance().getTable(TABLE).getField("email")))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new GetAction().execute(new GetInput(TABLE).withUniqueKey(Map.of("email", "darin@example.com"))))
         .isInstanceOf(QPermissionDeniedException.class);

      ////////////////////////////////////////////
      // writes: denied                         //
      ////////////////////////////////////////////
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("firstName", "X").withValue("lastName", "Y").withValue("email", "x@y.com"))))
         .isInstanceOf(QPermissionDeniedException.class)
         .hasMessageContaining("Email");
      assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 1).withValue("email", null))))
         .isInstanceOf(QPermissionDeniedException.class);
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 1).withValue("lastName", "K2")));
      assertTrue(updateOutput.getRecords().get(0).getErrors().isEmpty());

      ////////////////////////////////////////////
      // table meta data: field omitted         //
      ////////////////////////////////////////////
      TableMetaDataInput tableMetaDataInput = new TableMetaDataInput();
      tableMetaDataInput.setTableName(TABLE);
      TableMetaDataOutput tableMetaDataOutput = new TableMetaDataAction().execute(tableMetaDataInput);
      assertFalse(tableMetaDataOutput.getTable().getFields().containsKey("email"));
      assertTrue(QContext.getQInstance().getTable(TABLE).getFields().containsKey("email"), "instance meta data must not be modified");

      ////////////////////////////////////////////////
      // override value or all-access key: allowed //
      ////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue(KEY, "pii");
      assertEquals("darin@example.com", new QueryAction().execute(new QueryInput(TABLE)).getRecords().get(0).getValueString("email"));

      setSession(TABLE + ".read");
      QContext.getQSession().withSecurityKeyValue(KEY + "AllAccess", true);
      assertEquals("darin@example.com", new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(1)).getValueString("email"));
   }



   /*******************************************************************************
    ** A lock with default ALLOW denies only sessions with an override value.
    *******************************************************************************/
   @Test
   void testFieldSecurityLockDefaultAllow() throws Exception
   {
      QContext.getQInstance().getTable(TABLE).getField("lastName").setFieldSecurityLock(new FieldSecurityLock()
         .withSecurityKeyType(KEY).withDefaultBehavior(FieldSecurityLock.Behavior.ALLOW).withOverrideValues(List.of("restricted")));
      enableInstanceEnforcement();

      setSession(TABLE + ".read");
      assertEquals("K", new QueryAction().execute(new QueryInput(TABLE)).getRecords().get(0).getValueString("lastName"));

      QContext.getQSession().withSecurityKeyValue(KEY, "restricted");
      assertNull(new QueryAction().execute(new QueryInput(TABLE)).getRecords().get(0).getValueString("lastName"));
   }



   /*******************************************************************************
    ** Exports: READ enforced; locked fields omitted; isSensitive fields left out
    ** unless requested.
    *******************************************************************************/
   @Test
   void testExport() throws Exception
   {
      ////////////////////////////////////////////////////////////////////////
      // without enforcement: sensitive field left out unless requested     //
      ////////////////////////////////////////////////////////////////////////
      setSession();
      List<String> fieldNames = List.of("firstName", "noOfShoes", "email");
      assertEquals(Set.of("First Name", "Email"), exportedColumns(false, fieldNames).headers());
      assertEquals(Set.of("First Name", "No Of Shoes", "Email"), exportedColumns(true, fieldNames).headers());

      ////////////////////////////////////////////////////////////////////////
      // with enforcement: READ required; locked field can't be requested,  //
      // and is left out (with its values) of an all-fields export          //
      ////////////////////////////////////////////////////////////////////////
      enableInstanceEnforcement();
      assertThatThrownBy(() -> exportedColumns(false, null)).isInstanceOf(QPermissionDeniedException.class);

      setSession(TABLE + ".read");
      assertThatThrownBy(() -> exportedColumns(false, fieldNames)).isInstanceOf(QPermissionDeniedException.class);

      ExportResult allFields = exportedColumns(true, null);
      assertThat(allFields.headers()).contains("First Name", "No Of Shoes").doesNotContain("Email");
      assertThat(allFields.body()).contains("Darin").doesNotContain("darin@example.com");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record ExportResult(Set<String> headers, String body)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExportResult exportedColumns(boolean includeSensitive, List<String> fieldNames) throws QException
   {
      java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
      ExportInput                   exportInput  = new ExportInput();
      exportInput.setTableName(TABLE);
      exportInput.setFieldNames(fieldNames);
      new ExportAction().execute(exportInput
         .withIncludeSensitiveFields(includeSensitive)
         .withReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(outputStream)));
      String   csv     = outputStream.toString();
      String[] lines   = csv.split("\n");
      Set<String> headers = new java.util.LinkedHashSet<>();
      for(String header : lines[0].split(","))
      {
         headers.add(header.replace("\"", "").trim());
      }
      return (new ExportResult(headers, csv));
   }



   /*******************************************************************************
    ** dry runs: validate and return would-be results, without storing.
    *******************************************************************************/
   @Test
   void testDryRuns() throws Exception
   {
      setSession();

      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(TABLE).withDryRun(true).withRecords(List.of(
         new QRecord().withValue("firstName", "Dry").withValue("lastName", "Run"),
         new QRecord().withValue("firstName", "Darin").withValue("lastName", "K"))));
      assertEquals(2, insertOutput.getRecords().size());
      assertTrue(insertOutput.getRecords().get(0).getErrors().isEmpty());
      assertFalse(insertOutput.getRecords().get(1).getErrors().isEmpty(), "unique key violation should be reported by a dry run");
      assertEquals(2, new CountAction().execute(new CountInput(TABLE)).getCount());

      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TABLE).withDryRun(true).withRecord(new QRecord().withValue("id", 1).withValue("lastName", "Changed")));
      QRecord wouldBe = updateOutput.getRecords().get(0);
      assertEquals("Changed", wouldBe.getValueString("lastName"));
      assertEquals("Darin", wouldBe.getValueString("firstName"), "would-be record should carry the old values too");
      assertEquals("K", new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(1)).getValueString("lastName"));

      UpdateOutput missing = new UpdateAction().execute(new UpdateInput(TABLE).withDryRun(true).withRecord(new QRecord().withValue("id", 999).withValue("lastName", "Nope")));
      assertFalse(missing.getRecords().get(0).getErrors().isEmpty(), "a dry run should report a missing record");

      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(TABLE).withDryRun(true).withQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Tim"))));
      assertEquals(List.of(2), deleteOutput.getDryRunPrimaryKeys());
      assertEquals(0, deleteOutput.getDeletedRecordCount());
      assertEquals(2, new CountAction().execute(new CountInput(TABLE)).getCount());

      DeleteOutput realDelete = new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKeys(List.of(2)));
      assertEquals(1, realDelete.getDeletedRecordCount());
      assertNull(realDelete.getDryRunPrimaryKeys());
   }



   /*******************************************************************************
    ** a dry run is still subject to permissions.
    *******************************************************************************/
   @Test
   void testDryRunIsEnforced() throws Exception
   {
      enableInstanceEnforcement();
      setSession(TABLE + ".read");
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(TABLE).withDryRun(true).withRecord(new QRecord().withValue("firstName", "A"))))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** associated child writes need the child table's permissions and respect its
    ** field security locks; delete cascades need DELETE on the child table.
    *******************************************************************************/
   @Test
   void testAssociatedWrites() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    order     = TestUtils.TABLE_NAME_ORDER;
      String    line      = TestUtils.TABLE_NAME_LINE_ITEM;
      qInstance.getTable(order).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      qInstance.getTable(line).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      qInstance.getTable(line).getField("sku").setFieldSecurityLock(new FieldSecurityLock().withSecurityKeyType(KEY).withDefaultBehavior(FieldSecurityLock.Behavior.DENY).withOverrideValues(List.of("pii")));
      enableInstanceEnforcement();

      setSession(order + ".insert", order + ".delete");
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(order).withDryRun(true).withRecord(new QRecord().withValue("storeId", 1)
         .withAssociatedRecord("orderLine", new QRecord().withValue("lineNumber", "1")))))
         .isInstanceOf(QPermissionDeniedException.class);

      QContext.getQSession().withPermission(line + ".insert");
      assertTrue(isAllowed(() -> new InsertAction().execute(new InsertInput(order).withDryRun(true).withRecord(new QRecord().withValue("storeId", 1)
         .withAssociatedRecord("orderLine", new QRecord().withValue("lineNumber", "1"))))));
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(order).withDryRun(true).withRecord(new QRecord().withValue("storeId", 1)
         .withAssociatedRecord("orderLine", new QRecord().withValue("lineNumber", "1").withValue("sku", "locked")))))
         .isInstanceOf(QPermissionDeniedException.class);

      ///////////////////////////////////////////////////////////////////////
      // delete of an order with lines cascades: needs DELETE on the lines //
      ///////////////////////////////////////////////////////////////////////
      runAsSystem(() -> new InsertAction().execute(new InsertInput(order).withRecord(new QRecord().withValue("id", 77).withValue("storeId", 1)
         .withAssociatedRecord("orderLine", new QRecord().withValue("lineNumber", "1")))));
      assertThatThrownBy(() -> new DeleteAction().execute(new DeleteInput(order).withDryRun(true).withPrimaryKeys(List.of(77))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new DeleteAction().execute(new DeleteInput(order).withDryRun(true).withQueryFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 77)))))
         .isInstanceOf(QPermissionDeniedException.class);
      QContext.getQSession().withPermission(line + ".delete");
      assertTrue(isAllowed(() -> new DeleteAction().execute(new DeleteInput(order).withDryRun(true).withPrimaryKeys(List.of(77)))));
   }



   /*******************************************************************************
    ** a delete (or dry-run delete) filter may not read joined tables the user
    ** may not read.
    *******************************************************************************/
   @Test
   void testDeleteFilterOnJoinedTableNeedsRead() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      enableInstanceEnforcement();
      setSession(TestUtils.TABLE_NAME_ORDER + ".delete", TestUtils.TABLE_NAME_LINE_ITEM + ".delete");
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      assertThatThrownBy(() -> new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_ORDER).withDryRun(true)
         .withQueryFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_LINE_ITEM + ".sku", QCriteriaOperator.EQUALS, "X")))))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** GetAction.executeViaQuery keeps ENFORCE.
    *******************************************************************************/
   @Test
   void testExecuteViaQueryKeepsEnforce() throws Exception
   {
      setSession();
      GetInput getInput = new GetInput(TABLE).withPrimaryKey(1);
      getInput.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new GetAction().executeViaQuery(getInput)).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** a record label built from a denied field is cleared.
    *******************************************************************************/
   @Test
   void testRecordLabelFromLockedFieldIsCleared() throws Exception
   {
      QContext.getQInstance().getTable(TABLE).setRecordLabelFields(List.of("email"));
      QContext.getQInstance().getTable(TABLE).setRecordLabelFormat("%s");
      enableInstanceEnforcement();
      setSession(TABLE + ".read");
      QRecord record = new QueryAction().execute(new QueryInput(TABLE).withShouldGenerateDisplayValues(true)).getRecords().get(0);
      assertNull(record.getRecordLabel());
   }



   /*******************************************************************************
    ** caller-supplied (unregistered) widget / report meta data: refused under
    ** ENFORCE; with the instance setting on, not trusted (queries checked).
    *******************************************************************************/
   @Test
   void testUnregisteredWidgetAndReportMetaData() throws Exception
   {
      setSession(WIDGET + ".hasAccess");
      QWidgetMetaData adHoc = new QWidgetMetaData().withName(WIDGET).withCodeReference(new QCodeReference(CountingWidgetRenderer.class));

      RenderWidgetInput enforced = new RenderWidgetInput().withWidgetMetaData(adHoc);
      enforced.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new RenderWidgetAction().execute(enforced)).isInstanceOf(QPermissionDeniedException.class);

      enableInstanceEnforcement();
      assertThatThrownBy(() -> new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(adHoc))).isInstanceOf(QPermissionDeniedException.class);
      assertTrue(isAllowed(() -> new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(QContext.getQInstance().getWidget(WIDGET)))));

      ReportInput reportInput = new ReportInput().withReportMetaData(new QReportMetaData().withName("adHoc"))
         .withReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(new java.io.ByteArrayOutputStream()));
      reportInput.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new GenerateReportAction().execute(reportInput)).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** Backend step that queries the (protected) table.
    *******************************************************************************/
   public static class QueryingStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         stepQueryCount.set(new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
      }
   }



   /*******************************************************************************
    ** Post-insert customizer that counts the (protected) person table.
    *******************************************************************************/
   public static class CountingCustomizer implements TableCustomizerInterface
   {
      static final AtomicReference<Integer> lastCount = new AtomicReference<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
      {
         lastCount.set(new CountAction().execute(new CountInput(TABLE)).getCount());
         return (records);
      }
   }



   /*******************************************************************************
    ** Widget renderer that counts the (protected) person table.
    *******************************************************************************/
   public static class CountingWidgetRenderer extends AbstractWidgetRenderer
   {
      static final AtomicReference<Integer> lastCount = new AtomicReference<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         lastCount.set(new CountAction().execute(new CountInput(TABLE)).getCount());
         return (new RenderWidgetOutput(null));
      }
   }

}
