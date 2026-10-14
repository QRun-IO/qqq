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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEvent;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEventType;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessOutcome;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exact-match enumeration guard and outcome contracts.
 *******************************************************************************/
class FieldAccessExactMatchTest extends FieldAccessTestSupport
{
   /*******************************************************************************
    ** Completed actions report counts and fingerprints, never literals.
    *******************************************************************************/
   @Test
   void outcomes() throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL));
      new QueryAction().execute(new QueryInput(TABLE).withFilter(filter));
      assertOutcome(FieldAccessOutcome.MATCH, 2);
      new CountAction().execute(new CountInput(TABLE).withFilter(filter));
      assertOutcome(FieldAccessOutcome.MATCH, 2);
      new QueryAction().execute(new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.IN, List.of("missing")))));
      assertOutcome(FieldAccessOutcome.NO_MATCH, 0);
      table.withUniqueKey(new UniqueKey("email"));
      new GetAction().execute(new GetInput(TABLE).withUniqueKey(java.util.Map.of("email", "missing")));
      assertOutcome(FieldAccessOutcome.NO_MATCH, 0);
      assertFalse(JsonUtils.toJson(resolver.events).contains(EMAIL));
   }



   /*******************************************************************************
    ** Consume one completion and verify its value-free search information.
    *******************************************************************************/
   private void assertOutcome(FieldAccessOutcome outcome, int count)
   {
      List<FieldAccessEvent> events = resolver.events.stream().filter(event -> event.getType() == FieldAccessEventType.EXACT_MATCH_FILTER).toList();
      assertEquals(1, events.size());
      assertEquals(outcome, events.get(0).getOutcome());
      assertEquals(count, events.get(0).getRecordCount());
      assertEquals(List.of("opaque-fingerprint"), events.get(0).getSearchValueFingerprints());
      resolver.events.clear();
   }



   /*******************************************************************************
    ** Refusals preserve the policy's message; low-entropy fields cannot be filtered.
    *******************************************************************************/
   @Test
   void refusals() throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      resolver.refuseExactMatch = true;
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL));
      assertEquals("Exact-match search limit reached.", assertThrows(QPermissionDeniedException.class, () -> new CountAction().execute(new CountInput(TABLE).withFilter(filter))).getMessage());
      assertEquals(FieldAccessOutcome.REFUSED, resolver.events.get(0).getOutcome());
      resolver.exactMatchAllowed = false;
      QContext.removeObject(FieldAccessHelper.CACHE_KEY);
      assertFalse(FieldAccessHelper.canFilter(table, "email", QCriteriaOperator.EQUALS));
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)));
   }



   /*******************************************************************************
    ** Channel denial precedes all field policy resolution.
    *******************************************************************************/
   @Test
   void channelDeniedBeforeResolution()
   {
      table.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED).withDeniedClients(Set.of("assistant")));
      QContext.getQSession().setClientChannel("assistant");
      int calls = resolver.calls;
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE)));
      assertEquals(calls, resolver.calls);
   }



   /*******************************************************************************
    ** History ignores reveal grants and scrubs both value channels and messages.
    *******************************************************************************/
   @Test
   void auditDetails()
   {
      restrict(FieldAccessTier.REVEALABLE);
      resolver.reveals.add(TABLE + ".email.1");
      QRecord detail = new QRecord().withValue("fieldName", "email").withValue("oldValue", EMAIL).withValue("newValue", EMAIL).withValue("message", EMAIL);
      ArrayList<QRecord> details = new ArrayList<>(List.of(detail));
      FieldAccessHelper.applyToAuditDetails(TABLE, details);
      assertNotEquals(EMAIL, detail.getValue("newValue"));
      assertEquals(detail.getValue("newValue"), detail.getDisplayValue("newValue"));
      assertNull(detail.getValue("message"));
      assertFalse(FieldAccessHelper.isRecordLabelFullAccess(table));
      restrict(FieldAccessTier.HIDDEN);
      FieldAccessHelper.applyToAuditDetails(TABLE, details);
      assertTrue(details.isEmpty());
   }



   /*******************************************************************************
    ** Internal nested and explicitly bypassed reads do not emit exact-match outcomes.
    *******************************************************************************/
   @Test
   void internalReadsDoNotReportOutcomes() throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      QueryInput input = new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL)));
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         new QueryAction().execute(input);
      }
      assertTrue(resolver.events.isEmpty());
      input.withPermissionBypass("Internal test read");
      new QueryAction().execute(input);
      assertTrue(resolver.events.isEmpty());
   }
}
