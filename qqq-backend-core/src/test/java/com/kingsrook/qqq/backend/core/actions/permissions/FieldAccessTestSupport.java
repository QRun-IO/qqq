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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEvent;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;


/*******************************************************************************
 ** Secured field access contract tests.
 *******************************************************************************/
abstract class FieldAccessTestSupport extends BaseTest
{
   static final String TABLE = TestUtils.TABLE_NAME_PERSON_MEMORY;
   static final String CHILD = "securedChild";
   static final String EMAIL = "SENTINEL-EMAIL-918273645@example.com";
   static final String SSN = "SENTINEL-SSN-918273645";
   static final String NOTES = "SENTINEL-NOTES-ABCDEFG";
   QTableMetaData table;
   MapResolver resolver;



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @BeforeEach
   void setupAccess() throws Exception
   {
      table = QContext.getQInstance().getTable(TABLE);
      table.getField("ssn").setType(QFieldType.STRING);
      table.addField(new QFieldMetaData("notes", QFieldType.STRING));
      table.setRecordLabelFields(List.of("email"));
      table.setRecordLabelFormat("%s");
      table.setSearchFields(List.of("email", "firstName"));
      table.getField("email").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PII));
      table.getField("ssn").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("clearance").withAllAccessKeyName("allClearance"));
      QContext.getQInstance().addTable(new QTableMetaData().withName(CHILD).withBackendName(table.getBackendName()).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("personId", QFieldType.INTEGER)).withField(new QFieldMetaData("notes", QFieldType.STRING)));
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("securedJoin").withLeftTable(TABLE).withRightTable(CHILD).withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "personId")));
      table.withAssociation(new Association().withName("children").withAssociatedTableName(CHILD).withJoinName("securedJoin"));
      QContext.setQSession(new QSystemUserSession());
      new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Alpha").withValue("lastName", "One").withValue("email", EMAIL).withValue("ssn", SSN).withValue("notes", NOTES),
         new QRecord().withValue("id", 2).withValue("firstName", "Beta").withValue("lastName", "Two").withValue("email", EMAIL).withValue("ssn", SSN).withValue("notes", NOTES))));
      new InsertAction().execute(new InsertInput(CHILD).withRecord(new QRecord().withValue("id", 10).withValue("personId", 1).withValue("notes", NOTES)));
      QContext.setQSession(newSession());
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      MapResolver.tiers.clear();
      resolver = new MapResolver();
      FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance(resolver);
   }



   /*******************************************************************************
    ** Set effective policy for each sensitive field.
    *******************************************************************************/
   void restrict(FieldAccessTier tier)
   {
      for(String name : List.of("email", "ssn", "notes"))
      {
         MapResolver.tiers.put(TABLE + "." + name, tier);
      }
      MapResolver.tiers.put(CHILD + ".notes", tier);
      QContext.removeObject(FieldAccessHelper.CACHE_KEY);
   }



   /*******************************************************************************
    ** A value-free test resolver with per-record reveal grants.
    *******************************************************************************/
   public static class MapResolver implements FieldAccessResolverInterface
   {
      static final Map<String, FieldAccessTier> tiers = new HashMap<>();
      final List<FieldAccessEvent> events = new ArrayList<>();
      final List<Serializable> fingerprintValues = new ArrayList<>();
      final Set<String> reveals = new HashSet<>();
      int calls;
      int exactMatchChecks;
      boolean throwOnAccess;
      boolean exactMatchAllowed = true;
      boolean refuseExactMatch;



      /*******************************************************************************
       ** Resolve test policy.
       *******************************************************************************/
      public FieldAccessDecision resolve(QTableMetaData table, QFieldMetaData field)
      {
         calls++;
         return (new FieldAccessDecision(tiers.getOrDefault(table.getName() + "." + field.getName(), FieldAccessTier.FULL), null, "test policy", exactMatchAllowed));
      }



      /*******************************************************************************
       ** Simulated enumeration guard.
       *******************************************************************************/
      public void checkExactMatch(FieldAccessEvent event) throws com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException
      {
         exactMatchChecks++;
         if(refuseExactMatch)
         {
            throw (new com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException("Exact-match search limit reached."));
         }
      }



      /*******************************************************************************
       ** Fixed opaque test fingerprint: never return the literal.
       *******************************************************************************/
      public String fingerprintSearchValue(Serializable value)
      {
         fingerprintValues.add(value);
         return ("opaque-fingerprint");
      }



      /*******************************************************************************
       ** Only specific record-field grants unmask.
       *******************************************************************************/
      public boolean isRevealActive(String tableName, String fieldName, Serializable primaryKey)
      {
         return (reveals.contains(tableName + "." + fieldName + "." + primaryKey));
      }



      /*******************************************************************************
       ** A different session user has a different fingerprint.
       *******************************************************************************/
      public String getAccessFingerprint()
      {
         return (QContext.getQSession().getUser().getIdReference());
      }



      /*******************************************************************************
       ** Record notifications or simulate an unavailable sink.
       *******************************************************************************/
      public void onAccess(FieldAccessEvent event)
      {
         if(throwOnAccess)
         {
            throw (new IllegalStateException("audit unavailable"));
         }
         events.add(event);
      }
   }
}
