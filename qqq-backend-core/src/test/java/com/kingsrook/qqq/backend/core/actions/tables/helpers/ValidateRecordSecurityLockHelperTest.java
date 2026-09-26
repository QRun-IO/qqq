/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper.RecordWithErrors;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock.BooleanOperator.AND;
import static com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock.BooleanOperator.OR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ValidateRecordSecurityLockHelper 
 *******************************************************************************/
class ValidateRecordSecurityLockHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordWithErrors()
   {
      {
         RecordWithErrors recordWithErrors = new RecordWithErrors(new QRecord());
         recordWithErrors.add(new BadInputStatusMessage("0"), List.of(0));
         System.out.println(recordWithErrors);
         recordWithErrors.propagateErrorsToRecord(new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(new RecordSecurityLock())));
         System.out.println("----------------------------------------------------------------------------");
      }

      {
         RecordWithErrors recordWithErrors = new RecordWithErrors(new QRecord());
         recordWithErrors.add(new BadInputStatusMessage("1"), List.of(1));
         System.out.println(recordWithErrors);
         recordWithErrors.propagateErrorsToRecord(new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock())));
         System.out.println("----------------------------------------------------------------------------");
      }

      {
         RecordWithErrors recordWithErrors = new RecordWithErrors(new QRecord());
         recordWithErrors.add(new BadInputStatusMessage("0"), List.of(0));
         recordWithErrors.add(new BadInputStatusMessage("1"), List.of(1));
         System.out.println(recordWithErrors);
         recordWithErrors.propagateErrorsToRecord(new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock())));
         System.out.println("----------------------------------------------------------------------------");
      }

      {
         RecordWithErrors recordWithErrors = new RecordWithErrors(new QRecord());
         recordWithErrors.add(new BadInputStatusMessage("1,1"), List.of(1, 1));
         System.out.println(recordWithErrors);
         recordWithErrors.propagateErrorsToRecord(new MultiRecordSecurityLock().withLocks(List.of(
            new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock())),
            new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock()))
         )));
         System.out.println("----------------------------------------------------------------------------");
      }

      {
         RecordWithErrors recordWithErrors = new RecordWithErrors(new QRecord());
         recordWithErrors.add(new BadInputStatusMessage("0,0"), List.of(0, 0));
         recordWithErrors.add(new BadInputStatusMessage("1,1"), List.of(1, 1));
         System.out.println(recordWithErrors);
         recordWithErrors.propagateErrorsToRecord(new MultiRecordSecurityLock().withLocks(List.of(
            new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock())),
            new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock()))
         )));
         System.out.println("----------------------------------------------------------------------------");
      }

      {
         RecordWithErrors recordWithErrors = new RecordWithErrors(new QRecord());
         recordWithErrors.add(new BadInputStatusMessage("0"), List.of(0));
         recordWithErrors.add(new BadInputStatusMessage("1,1"), List.of(1, 1));
         System.out.println(recordWithErrors);
         recordWithErrors.propagateErrorsToRecord(new MultiRecordSecurityLock().withLocks(List.of(
            new RecordSecurityLock(),
            new MultiRecordSecurityLock().withLocks(List.of(new RecordSecurityLock(), new RecordSecurityLock()))
         )));
         System.out.println("----------------------------------------------------------------------------");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAllowedToReadRecord() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTables().get(TestUtils.TABLE_NAME_ORDER);

      QSession sessionWithStore1          = new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      QSession sessionWithStore2          = new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 2);
      QSession sessionWithStore1and2      = new QSession().withSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, List.of(1, 2)));
      QSession sessionWithStoresAllAccess = new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QSession sessionWithNoStores        = new QSession();

      QRecord recordStore1  = new QRecord().withValue("storeId", 1);

      assertTrue(ValidateRecordSecurityLockHelper.allowedToReadRecord(table, recordStore1, sessionWithStore1, null));
      assertFalse(ValidateRecordSecurityLockHelper.allowedToReadRecord(table, recordStore1, sessionWithStore2, null));
      assertTrue(ValidateRecordSecurityLockHelper.allowedToReadRecord(table, recordStore1, sessionWithStore1and2, null));
      assertTrue(ValidateRecordSecurityLockHelper.allowedToReadRecord(table, recordStore1, sessionWithStoresAllAccess, null));
      assertFalse(ValidateRecordSecurityLockHelper.allowedToReadRecord(table, recordStore1, sessionWithNoStores, null));
   }



   /*******************************************************************************
    ** Omitted and explicit-null memory owners both obey the declared null policy.
    *******************************************************************************/
   @Test
   void testJoinedMemoryNullableOwners() throws QException
   {
      QTableMetaData parent = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      QTableMetaData child = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      parent.setRecordSecurityLocks(List.of());
      for(boolean explicitNull : List.of(true, false))
      {
         QRecord values = new QRecord();
         if(explicitNull)
         {
            values.setValue("storeId", null);
         }
         QRecord stored = new InsertAction().execute(new InsertInput(parent.getName()).withRecord(values)).getRecords().get(0);
         assertTrue(stored.getErrors() == null || stored.getErrors().isEmpty(), stored.getErrorsAsString());
         for(RecordSecurityLock.NullValueBehavior policy : List.of(RecordSecurityLock.NullValueBehavior.ALLOW, RecordSecurityLock.NullValueBehavior.DENY))
         {
            child.getRecordSecurityLocks().get(0).setNullValueBehavior(policy);
            QRecord record = new QRecord().withValue("orderId", stored.getValue("id"));
            ValidateRecordSecurityLockHelper.validateSecurityFields(child, List.of(record), ValidateRecordSecurityLockHelper.Action.INSERT, null);
            assertEquals(policy == RecordSecurityLock.NullValueBehavior.ALLOW, record.getErrors() == null || record.getErrors().isEmpty());
         }
      }
   }



   /*******************************************************************************
    ** OR has two branches even when its failing AND branch contributes two leaf
    ** errors. All three leaf failures must deny rather than disappear.
    *******************************************************************************/
   @Test
   void testOrOfFailingConjunctionAndFailingLeafDenies() throws QException
   {
      QRecord record = validateNestedOr("9");
      assertEquals(3, record.getErrors().size());
      assertTrue(record.getErrors().stream().allMatch(error -> error instanceof PermissionDeniedMessage));
   }



   /*******************************************************************************
    ** Two errors in one AND branch cannot outvote a successful OR alternative.
    *******************************************************************************/
   @Test
   void testOrOfFailingConjunctionAndSuccessfulLeafAllows() throws QException
   {
      QRecord record = validateNestedOr("1");
      assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString());
   }



   /*******************************************************************************
    ** Existing Order fields and Store security keys exercise actual leaf checks,
    ** not synthetic error counts. A supplied id avoids temporary-key behavior.
    *******************************************************************************/
   private QRecord validateNestedOr(String orderNo) throws QException
   {
      QTableMetaData original = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      QTableMetaData table = original.clone();
      MultiRecordSecurityLock conjunction = new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
         new RecordSecurityLock().withFieldName("id").withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE).withLockScope(RecordSecurityLock.LockScope.WRITE),
         new RecordSecurityLock().withFieldName("storeId").withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE).withLockScope(RecordSecurityLock.LockScope.WRITE)
      ));
      table.setRecordSecurityLocks(List.of(new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
         conjunction,
         new RecordSecurityLock().withFieldName("orderNo").withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE).withLockScope(RecordSecurityLock.LockScope.WRITE)
      ))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1));
      QRecord record = new QRecord().withValue("id", 2).withValue("storeId", 3).withValue("orderNo", orderNo);
      Map<String, ?> beforeValues = Map.copyOf(record.getValues());
      ValidateRecordSecurityLockHelper.validateSecurityFields(table, List.of(record), ValidateRecordSecurityLockHelper.Action.UPDATE, null);
      assertEquals(beforeValues, record.getValues());
      assertEquals(1, original.getRecordSecurityLocks().size());
      assertEquals("storeId", original.getRecordSecurityLocks().get(0).getFieldName());
      assertEquals(RecordSecurityLock.LockScope.READ_AND_WRITE, original.getRecordSecurityLocks().get(0).getLockScope());
      return record;
   }

}
