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
import java.util.List;
import java.util.stream.IntStream;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEvent;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Secured field access contract tests.
 *******************************************************************************/
class FieldAccessHelperTest extends FieldAccessTestSupport
{



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void combinationsAndOverrides()
   {
      QFieldMetaData email = table.getField("email");
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      email.setFieldSecurityLock(new FieldSecurityLock().withSecurityKeyType("clearance").withDefaultBehavior(FieldSecurityLock.Behavior.MASK).withOverrideValues(List.of("1")).withMaskStyle(FieldMaskStyle.FULL));
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      assertEquals(FieldMaskStyle.FULL, FieldAccessHelper.getDecision(table, email).maskStyle());
      restrict(FieldAccessTier.REVEALABLE);
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      QContext.getQSession().withSecurityKeyValue("clearance", 1);
      assertEquals(FieldAccessTier.REVEALABLE, FieldAccessHelper.getTier(table, "email"));
      restrict(FieldAccessTier.FULL);
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      email.getFieldSecurityLock().setDefaultBehavior(FieldSecurityLock.Behavior.ALLOW);
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
      QContext.getQSession().withSecurityKeyValue("allClearance", true);
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      restrict(FieldAccessTier.HIDDEN);
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
      MapResolver.tiers.put(TABLE + ".id", FieldAccessTier.HIDDEN);
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "id"));
      QContext.setQSession(new QSystemUserSession());
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void cacheIsolationAndFingerprint()
   {
      restrict(FieldAccessTier.MASKED);
      String fingerprint = FieldAccessHelper.getAccessFingerprint();
      FieldAccessHelper.getTier(table, "email");
      int calls = resolver.calls;
      FieldAccessHelper.getTier(table, "email");
      assertEquals(calls, resolver.calls);
      QContext.setQSession(newSession("second"));
      assertNotEquals(fingerprint, FieldAccessHelper.getAccessFingerprint());
      restrict(FieldAccessTier.FULL);
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      assertTrue(resolver.calls > calls);
      String beforeLock = FieldAccessHelper.getAccessFingerprint();
      table.getField("email").setFieldSecurityLock(new FieldSecurityLock());
      assertNotEquals(beforeLock, FieldAccessHelper.getAccessFingerprint());
      assertEquals(FieldAccessHelper.getAccessFingerprint(), FieldAccessHelper.getAccessFingerprint());
      QContext.init(QContext.getQInstance(), newSession());
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void metadataAndValidation() throws Exception
   {
      assertSame(table, FieldAccessHelper.applyToTable(table));
      restrict(FieldAccessTier.MASKED);
      QTableMetaData clone = FieldAccessHelper.applyToTable(table);
      assertNotSame(table, clone);
      assertFalse(clone.getField("email").getIsEditable());
      assertTrue(table.getField("email").getIsEditable());
      FieldAccessResolverMetaData metadata = FieldAccessResolverMetaData.of(QContext.getQInstance());
      assertThrows(IllegalStateException.class, () -> metadata.withResolver(new QCodeReference(MapResolver.class)));
      assertThrows(IllegalStateException.class, () -> metadata.withResolverInstance(new MapResolver()));
      assertSame(metadata, FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()));
      assertSame(metadata, metadata.withResolverInstance(resolver));
      FieldAccessResolverMetaData invalid = new FieldAccessResolverMetaData().withResolver(new QCodeReference(String.class));
      assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(TestUtils.defineInstance().withSupplementalMetaData(invalid)));
      FieldAccessResolverMetaData valid = new FieldAccessResolverMetaData().withResolver(new QCodeReference(MapResolver.class));
      new QInstanceValidator().validate(TestUtils.defineInstance().withSupplementalMetaData(valid));
      assertNotSame(valid.getResolver(), valid.clone().getResolver());
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void auditKeyCap()
   {
      FieldAccessEvent event = new FieldAccessEvent().withPrimaryKeys(IntStream.range(0, 75).mapToObj(i -> (Serializable) i).toList());
      assertEquals(50, event.getPrimaryKeys().size());
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void noResolverVirtualFieldsAndMaskPrecedence()
   {
      QContext.getQInstance().getSupplementalMetaData().remove(FieldAccessResolverMetaData.NAME);
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      table.getField("email").setFieldSecurityLock(new FieldSecurityLock().withDefaultBehavior(FieldSecurityLock.Behavior.REVEAL).withMaskStyle(FieldMaskStyle.EMAIL));
      assertEquals(FieldAccessTier.REVEALABLE, FieldAccessHelper.getTier(table, "email"));
      QVirtualFieldMetaData virtual = new QVirtualFieldMetaData("virtualSecret", QFieldType.STRING);
      virtual.setFieldSecurityLock(new FieldSecurityLock().withDefaultBehavior(FieldSecurityLock.Behavior.MASK));
      table.withVirtualField(virtual);
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTiers(table).get("virtualSecret"));
      assertFalse(FieldAccessHelper.applyToTable(table).getVirtualField("virtualSecret").getIsEditable());
      FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance((sourceTable, field) -> new FieldAccessDecision(FieldAccessTier.MASKED, FieldMaskStyle.FULL, null));
      assertEquals(FieldMaskStyle.FULL, FieldAccessHelper.getDecision(table, table.getField("email")).maskStyle());
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "id"));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void contextReinitializationDoesNotRetainDecisions()
   {
      restrict(FieldAccessTier.MASKED);
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      MapResolver.tiers.put(TABLE + ".email", FieldAccessTier.FULL);
      QContext.setQSession(newSession("next-session"));
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      MapResolver.tiers.put(TABLE + ".email", FieldAccessTier.HIDDEN);
      QContext.init(QContext.getQInstance(), QContext.getQSession());
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void nullValuesAndAbsentDisplayMap()
   {
      restrict(FieldAccessTier.MASKED);
      QRecord record = new QRecord().withValue("id", 1).withValue("email", null);
      record.setDisplayValues(null);
      FieldAccessHelper.applyToRecords(TABLE, null, List.of(record), "get");
      assertNull(record.getValue("email"));
      assertNull(record.getDisplayValue("email"));
   }
}
