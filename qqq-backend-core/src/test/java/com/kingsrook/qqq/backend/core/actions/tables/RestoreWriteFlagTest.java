/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelperTest.CapturingListener;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for restore-mode writes (RestoreWriteFlag) on Insert, Update and
 ** Replace.
 *******************************************************************************/
class RestoreWriteFlagTest extends BaseTest
{
   private static final String TABLE = TestUtils.TABLE_NAME_PERSON_MEMORY;

   private static final Instant THEN = Instant.parse("2020-02-02T02:02:02Z");



   /*******************************************************************************
    ** run as a session that has the restore permission (and nothing else).
    *******************************************************************************/
   private void asRestorer()
   {
      RecordChangeListenerHelperTest.resetListeners();
      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      setSession(RestoreWriteFlag.REQUIRED_PERMISSION);
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
   private static QRecord person(String firstName)
   {
      return new QRecord().withValue("firstName", firstName).withValue("lastName", "L" + firstName);
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
    **
    *******************************************************************************/
   private static QRecord getPerson(int id) throws QException
   {
      return TestUtils.queryTable(TABLE).stream().filter(r -> r.getValueInteger("id").equals(id)).findFirst().orElseThrow();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineAudits() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.getTable(TABLE).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));
   }



   /*******************************************************************************
    ** the flag is rejected for a session without the permission - on all 3 actions,
    ** and regardless of the instance setting, a permission bypass, or a trusted
    ** (nested) scope.  Nothing is written.
    *******************************************************************************/
   @Test
   void testRejectedWithoutPermission() throws Exception
   {
      for(boolean instanceEnforcement : List.of(false, true))
      {
         asRestorer();
         QContext.getQInstance().setEnforcePermissionsInCoreActions(instanceEnforcement);
         setSession("some.other.permission");

         assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(TABLE).withRecord(person("A")).withFlag(new RestoreWriteFlag())))
            .isInstanceOf(QPermissionDeniedException.class).hasMessageContaining("backups.restore");

         assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "B")).withFlag(new RestoreWriteFlag())))
            .isInstanceOf(QPermissionDeniedException.class);

         assertThatThrownBy(() -> new ReplaceAction().execute(replaceInput().withRecords(List.of(person("C"))).withFlag(new RestoreWriteFlag())))
            .isInstanceOf(QPermissionDeniedException.class);

         /////////////////////////////////////////////////////////////////////
         // a bypass of table-permission enforcement is not a bypass here   //
         /////////////////////////////////////////////////////////////////////
         InsertInput bypassInput = new InsertInput(TABLE).withRecord(person("D")).withFlag(new RestoreWriteFlag());
         bypassInput.withPermissionBypass("test");
         assertThatThrownBy(() -> new InsertAction().execute(bypassInput)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> CorePermissionEnforcer.runTrusted("test", () -> new InsertAction().execute(new InsertInput(TABLE).withRecord(person("E")).withFlag(new RestoreWriteFlag()))))
            .isInstanceOf(QPermissionDeniedException.class);

         assertThat(TestUtils.queryTable(TABLE)).isEmpty();
      }

      /////////////////////////////////////////////
      // a session with no permissions at all    //
      /////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(TABLE).withRecord(person("F")).withFlag(new RestoreWriteFlag())))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThat(TestUtils.queryTable(TABLE)).isEmpty();
   }



   /*******************************************************************************
    ** with the permission (or as the system user), the flag is accepted; without
    ** the flag, no permission is needed.
    *******************************************************************************/
   @Test
   void testAcceptedWithPermission() throws QException
   {
      asRestorer();
      new InsertAction().execute(new InsertInput(TABLE).withRecord(person("A")).withFlag(new RestoreWriteFlag()));
      assertEquals(1, TestUtils.queryTable(TABLE).size());

      QContext.setQSession(new QSystemUserSession());
      new InsertAction().execute(new InsertInput(TABLE).withRecord(person("B")).withFlag(new RestoreWriteFlag()));
      assertEquals(2, TestUtils.queryTable(TABLE).size());

      setSession("some.other.permission");
      new InsertAction().execute(new InsertInput(TABLE).withRecord(person("C")));
      assertEquals(3, TestUtils.queryTable(TABLE).size());
   }



   /*******************************************************************************
    ** option defaults, and finding the flag in a set
    *******************************************************************************/
   @Test
   void testDefaultsAndLookup()
   {
      RestoreWriteFlag flag = new RestoreWriteFlag();
      assertTrue(flag.getSuppressAutomations());
      assertFalse(flag.getRunCustomizers());
      assertFalse(flag.getPerRecordAudit());
      assertFalse(flag.getPublishEvents());

      ActionFlag otherFlag = new ActionFlag() {};
      assertNull(RestoreWriteFlag.from(null));
      assertNull(RestoreWriteFlag.from(new InsertInput(TABLE).withFlag(otherFlag).getFlags()));
      assertEquals(flag, RestoreWriteFlag.from(new InsertInput(TABLE).withFlag(otherFlag).withFlag(flag).getFlags()));
   }



   /*******************************************************************************
    ** supplied createDate and modifyDate are kept (insert, update and replace); null
    ** ones are filled; and without the flag they are overwritten.
    *******************************************************************************/
   @Test
   void testDatesKept() throws QException
   {
      asRestorer();

      ///////////////////////////////////////////////////////////////////////
      // insert: record 1 supplies both, record 2 supplies only createDate //
      ///////////////////////////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecords(List.of(
         person("A").withValue("createDate", THEN).withValue("modifyDate", THEN),
         person("B").withValue("createDate", THEN))));
      assertEquals(THEN, getPerson(1).getValueInstant("createDate"));
      assertEquals(THEN, getPerson(1).getValueInstant("modifyDate"));
      assertEquals(THEN, getPerson(2).getValueInstant("createDate"));
      assertThat(getPerson(2).getValueInstant("modifyDate")).isAfter(THEN);

      //////////////////////////////////////////////////////
      // update: supplied modifyDate kept; null is filled //
      //////////////////////////////////////////////////////
      Instant other = Instant.parse("2021-03-03T03:03:03Z");
      new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A2").withValue("modifyDate", other)));
      assertEquals("A2", getPerson(1).getValueString("firstName"));
      assertEquals(other, getPerson(1).getValueInstant("modifyDate"));
      assertEquals(THEN, getPerson(1).getValueInstant("createDate"));

      new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A3")));
      assertThat(getPerson(1).getValueInstant("modifyDate")).isAfter(other);

      //////////////////////////////////////////////////////
      // replace: both the insert and the update path keep //
      //////////////////////////////////////////////////////
      Instant third = Instant.parse("2022-04-04T04:04:04Z");
      new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("firstName", "lastName")).withFlag(new RestoreWriteFlag())
         .withRecords(List.of(
            person("A3").withValue("lastName", "LA").withValue("modifyDate", third),
            person("C").withValue("createDate", third).withValue("modifyDate", third))));
      assertEquals(third, getPerson(1).getValueInstant("modifyDate"));
      assertEquals(third, getPerson(3).getValueInstant("createDate"));
      assertEquals(third, getPerson(3).getValueInstant("modifyDate"));

      /////////////////////////////////
      // control: no flag overwrites //
      /////////////////////////////////
      new InsertAction().execute(new InsertInput(TABLE).withRecord(person("Z").withValue("createDate", THEN).withValue("modifyDate", THEN)));
      QRecord control = TestUtils.queryTable(TABLE).stream().filter(r -> "Z".equals(r.getValueString("firstName"))).findFirst().orElseThrow();
      assertThat(control.getValueInstant("createDate")).isAfter(THEN);
      assertThat(control.getValueInstant("modifyDate")).isAfter(THEN);
   }



   /*******************************************************************************
    ** automation status is OK (not PENDING_INSERT) - or as supplied - in restore
    ** mode; suppressAutomations=false (or no flag) gives the usual pending status;
    ** an update leaves it alone.
    *******************************************************************************/
   @Test
   void testAutomationStatus() throws QException
   {
      asRestorer();

      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecords(List.of(
         person("A"),
         person("B").withValue("qqqAutomationStatus", AutomationStatus.FAILED_UPDATE_AUTOMATIONS.getId()))));
      assertEquals(AutomationStatus.OK.getId(), getPerson(1).getValueInteger("qqqAutomationStatus"));
      assertEquals(AutomationStatus.FAILED_UPDATE_AUTOMATIONS.getId(), getPerson(2).getValueInteger("qqqAutomationStatus"));

      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag().withSuppressAutomations(false)).withRecord(person("C")));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), getPerson(3).getValueInteger("qqqAutomationStatus"));

      new InsertAction().execute(new InsertInput(TABLE).withRecord(person("D")));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), getPerson(4).getValueInteger("qqqAutomationStatus"));

      //////////////////////////////////////////////////////////////////////////////
      // update: restore mode leaves the status; without it, goes pending-update  //
      //////////////////////////////////////////////////////////////////////////////
      new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A2")));
      assertEquals(AutomationStatus.OK.getId(), getPerson(1).getValueInteger("qqqAutomationStatus"));

      new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A3")));
      assertEquals(AutomationStatus.PENDING_UPDATE_AUTOMATIONS.getId(), getPerson(1).getValueInteger("qqqAutomationStatus"));
   }



   /*******************************************************************************
    ** pre- and post-insert / update customizers only run when runCustomizers=true
    *******************************************************************************/
   @Test
   void testCustomizers() throws QException
   {
      asRestorer();

      QTableMetaData table = QContext.getQInstance().getTable(TABLE);
      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(InsertActionTest.TestPreInsertCustomizer.class));
      table.withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(InsertActionTest.TestPostInsertCustomizer.class));
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PreUpdate.class));
      table.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(PostUpdate.class));

      ///////////////////////////////////////
      // default: customizers do not run   //
      ///////////////////////////////////////
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(person("A")));
      assertNull(getPerson(1).getValueInteger("noOfShoes"));
      assertNull(insertOutput.getRecords().get(0).getValueInteger("homeStateId"));

      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A2")));
      assertNull(getPerson(1).getValueInteger("noOfShoes"));
      assertNull(updateOutput.getRecords().get(0).getValueInteger("homeStateId"));

      /////////////////////////////////
      // runCustomizers: they do run //
      /////////////////////////////////
      insertOutput = new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag().withRunCustomizers(true)).withRecord(person("B")));
      assertEquals(1701, getPerson(2).getValueInteger("noOfShoes"));
      assertEquals(47, insertOutput.getRecords().get(0).getValueInteger("homeStateId"));

      updateOutput = new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag().withRunCustomizers(true)).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A3")));
      assertEquals(1702, getPerson(1).getValueInteger("noOfShoes"));
      assertEquals(48, updateOutput.getRecords().get(0).getValueInteger("homeStateId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PreUpdate extends AbstractPreUpdateCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         List<QRecord> rs = new ArrayList<>();
         records.forEach(r -> rs.add(new QRecord(r).withValue("noOfShoes", 1702)));
         return (rs);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PostUpdate extends AbstractPostUpdateCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         List<QRecord> rs = new ArrayList<>();
         records.forEach(r -> rs.add(new QRecord(r).withValue("homeStateId", 48)));
         return (rs);
      }
   }



   /*******************************************************************************
    ** one summary audit per call instead of one per record; perRecordAudit brings
    ** back the standard audits.
    *******************************************************************************/
   @Test
   void testAudits() throws QException
   {
      asRestorer();
      defineAudits();

      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecords(List.of(person("A"), person("B"), person("C"))));
      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size());
      assertEquals("Restore-mode insert of 3 record(s)", audits.get(0).getValueString("message"));

      new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag()).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "A2"), new QRecord().withValue("id", 2).withValue("firstName", "B2"))));
      audits = TestUtils.queryTable("audit");
      assertEquals(2, audits.size());
      assertThat(audits).extracting(a -> a.getValueString("message")).contains("Restore-mode update of 2 record(s)");

      ///////////////////////////////////
      // replace, including its delete //
      ///////////////////////////////////
      new ReplaceAction().execute(replaceInput().withKey(new UniqueKey("firstName", "lastName")).withPerformDeletes(true).withFlag(new RestoreWriteFlag())
         .withRecords(List.of(person("D"))));
      audits = TestUtils.queryTable("audit");
      assertThat(audits).extracting(a -> a.getValueString("message")).contains("Restore-mode insert of 1 record(s)", "Restore-mode delete of 3 record(s)");
      assertEquals(4, audits.size());

      ///////////////////////////////////////////////////////////////
      // perRecordAudit: standard audits - one per record - return //
      ///////////////////////////////////////////////////////////////
      int before = TestUtils.queryTable("audit").size();
      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag().withPerRecordAudit(true)).withRecords(List.of(person("X"), person("Y"))));
      assertEquals(before + 2, TestUtils.queryTable("audit").size());

      ////////////////////////////////////////////////////
      // control (no flag), and omitDmlAudit is honored //
      ////////////////////////////////////////////////////
      before = TestUtils.queryTable("audit").size();
      new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(person("P"), person("Q"))));
      assertEquals(before + 2, TestUtils.queryTable("audit").size());

      before = TestUtils.queryTable("audit").size();
      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withOmitDmlAudit(true).withRecords(List.of(person("R"))));
      assertEquals(before, TestUtils.queryTable("audit").size());
   }



   /*******************************************************************************
    ** record-change listeners (e.g., ESB publication) are skipped unless publishEvents
    *******************************************************************************/
   @Test
   void testEvents() throws QException
   {
      asRestorer();
      QContext.getQInstance().withRecordChangeListener(new QCodeReference(CapturingListener.class));

      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(person("A")));
      new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A2")));
      assertThat(CapturingListener.events).isEmpty();

      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag().withPublishEvents(true)).withRecord(person("B")));
      new UpdateAction().execute(new UpdateInput(TABLE).withFlag(new RestoreWriteFlag().withPublishEvents(true)).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "A3")));
      assertEquals(2, CapturingListener.events.size());

      new InsertAction().execute(new InsertInput(TABLE).withRecord(person("C")));
      assertEquals(3, CapturingListener.events.size());
   }



   /*******************************************************************************
    ** things that must still run, even in restore mode:  record security locks,
    ** unique keys, required fields.
    *******************************************************************************/
   @Test
   void testValidationAndSecurityStillRun() throws QException
   {
      asRestorer();

      ///////////////////////
      // record security   //
      ///////////////////////
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("storeId", 2)));
      assertEquals("You do not have permission to insert a record with a value of 2 in the field: Store Id", insertOutput.getRecords().get(0).getErrors().get(0).getMessage());
      assertEquals(0, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).size());

      insertOutput = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("storeId", 1)));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isEmpty();

      ///////////////
      // unique key //
      ///////////////
      new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(person("A")));
      insertOutput = new InsertAction().execute(new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(person("A")));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isNotEmpty();
      assertEquals(1, TestUtils.queryTable(TABLE).size());

      ///////////////////////////////////////////
      // required field (for input source USER) //
      ///////////////////////////////////////////
      QContext.getQInstance().getTable(TABLE).getField("birthDate").setIsRequired(true);
      insertOutput = new InsertAction().execute(new InsertInput(TABLE).withInputSource(QInputSource.USER).withFlag(new RestoreWriteFlag()).withRecord(person("B")));
      assertThat(insertOutput.getRecords().get(0).getErrors()).isNotEmpty();
      assertEquals(1, TestUtils.queryTable(TABLE).size());
   }



   /*******************************************************************************
    ** record security also still applies to an update in restore mode
    *******************************************************************************/
   @Test
   void testRecordSecurityOnUpdateStillRuns() throws QException
   {
      asRestorer();

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("storeId", 1)));

      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withFlag(new RestoreWriteFlag()).withRecord(new QRecord().withValue("id", 1).withValue("storeId", 2)));
      assertThat(updateOutput.getRecords().get(0).getErrors()).isNotEmpty();
      assertEquals(1, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).get(0).getValueInteger("storeId"));
   }



   /*******************************************************************************
    ** field-security write checks still run (when the call is enforced).
    *******************************************************************************/
   @Test
   void testFieldSecurityStillRuns() throws QException
   {
      asRestorer();

      QInstance qInstance = QContext.getQInstance();
      qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clearance").withAllAccessKeyName("clearanceAllAccess"));
      qInstance.getTable(TABLE).getField("email").setFieldSecurityLock(new FieldSecurityLock()
         .withSecurityKeyType("clearance")
         .withDefaultBehavior(FieldSecurityLock.Behavior.DENY)
         .withOverrideValues(List.of("pii")));

      InsertInput deniedInsert = new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(person("A").withValue("email", "a@b.com"));
      deniedInsert.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new InsertAction().execute(deniedInsert)).isInstanceOf(QPermissionDeniedException.class);
      assertThat(TestUtils.queryTable(TABLE)).isEmpty();

      ///////////////////////////////////////////////
      // and with the key value, it goes through   //
      ///////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValue("clearance", "pii");
      InsertInput allowedInsert = new InsertInput(TABLE).withFlag(new RestoreWriteFlag()).withRecord(person("A").withValue("email", "a@b.com"));
      allowedInsert.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      new InsertAction().execute(allowedInsert);
      assertEquals(1, TestUtils.queryTable(TABLE).size());
   }

}
