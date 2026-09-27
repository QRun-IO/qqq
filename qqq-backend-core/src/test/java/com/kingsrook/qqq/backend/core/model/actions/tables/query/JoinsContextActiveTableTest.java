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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.SelectionValidationHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Execution metadata retains private structural fields without reopening public
 ** selection or mutating the active/canonical table graphs.
 *******************************************************************************/
class JoinsContextActiveTableTest extends BaseTest
{
   private static final String PARENT = "contextParent";
   private static final String CHILD = "contextChild";
   private static final String OWNER = "contextOwner";

   private static final List<InputSource> joinedSources = new ArrayList<>();
   private static int parentCalls;
   private static boolean removeJoinedTable;
   private static QTableMetaData personalizedChild;

   private QInstance instance;
   private QTableMetaData parent;
   private QTableMetaData child;
   private QTableMetaData activeParent;



   /*******************************************************************************
    ** A string owner catches accidental INTEGER fallback after field removal.
    *******************************************************************************/
   @BeforeEach
   void setUpContextTables()
   {
      clearCapture();
      instance = QContext.getQInstance();
      instance.addBackend(new QBackendMetaData().withName("contextMemory").withBackendType(MemoryBackendModule.class));
      parent = table(PARENT);
      child = table(CHILD).withField(new QFieldMetaData("parentId", QFieldType.INTEGER).withBackendName("parent_id"));
      instance.addTable(parent);
      instance.addTable(child);
      instance.addJoin(new QJoinMetaData().withName("contextJoin").withLeftTable(PARENT).withRightTable(CHILD)
         .withJoinOn(new JoinOn("id", "parentId")));
      instance.addSecurityKeyType(new QSecurityKeyType().withName(OWNER));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(JoinedPersonalizer.class));
      QContext.setQSession(new QSession().withSecurityKeyValue(OWNER, "ALPHA"));

      activeParent = parent.clone();
      activeParent.getFields().remove("ownerKey");
      activeParent.getField("name").setBackendName("active_parent_name");
      activeParent.setRecordSecurityLocks(new ArrayList<>(List.of(ownerLock())));
   }



   /*******************************************************************************
    ** Clear only this fixture's callback state; BaseTest owns context cleanup.
    *******************************************************************************/
   @AfterEach
   void clearContextCapture()
   {
      clearCapture();
   }



   /*******************************************************************************
    ** Two aliases share one USER personalization; active root locks and mappings
    ** remain authoritative while restored definitions are private copies.
    *******************************************************************************/
   @Test
   void testActiveUserMetadataAndAliasesRemainIsolated() throws QException
   {
      AggregateInput input = input(QInputSource.USER);
      String canonicalBefore = JsonUtils.toJson(List.of(parent, child, instance.getJoin("contextJoin")));
      String activeBefore = JsonUtils.toJson(activeParent);
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      QQueryFilter filter = new QQueryFilter();
      JoinsContext context = new JoinsContext(instance, input, filter);

      assertEquals(0, parentCalls, "Already active root metadata must not be personalized again");
      assertEquals(List.of(QInputSource.USER), joinedSources);
      assertSame(activeParent, input.getTable());
      assertNotSame(activeParent, context.getTable(PARENT));
      assertNotSame(personalizedChild, context.getTable(CHILD));
      assertNotSame(parent.getField("ownerKey"), context.getTable(PARENT).getField("ownerKey"));
      assertNotSame(child.getField("parentId"), context.getTable(CHILD).getField("parentId"));
      assertEquals("active_parent_name", context.getFieldAndTableNameOrAlias("name").field().getBackendName());
      assertEquals("active_child_name", context.getFieldAndTableNameOrAlias("first.name").field().getBackendName());
      assertSame(context.getFieldAndTableNameOrAlias("first.name").field(), context.getFieldAndTableNameOrAlias("second.name").field());
      assertEquals(CHILD, context.resolveTableNameOrAliasToTableName("first"));
      assertEquals(CHILD, context.resolveTableNameOrAliasToTableName("second"));
      assertEquals("parent_id", context.getFieldAndTableNameOrAlias("first.parentId").field().getBackendName());
      assertOwnerCriterion(criteria(filter), PARENT + ".ownerKey");
      assertEquals(2, context.getQueryJoins().size());
      for(QueryJoin join : context.getQueryJoins())
      {
         assertOwnerCriterion(join.getSecurityCriteria(), join.getJoinTableOrItsAlias() + ".ownerKey");
      }
      assertFalse(activeParent.getFields().containsKey("ownerKey"));
      assertFalse(personalizedChild.getFields().containsKey("parentId"));
      assertFalse(personalizedChild.getFields().containsKey("ownerKey"));
      assertEquals(canonicalBefore, JsonUtils.toJson(List.of(parent, child, instance.getJoin("contextJoin"))));
      assertEquals(activeBefore, JsonUtils.toJson(activeParent));
      assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins()));

      context.getTable(PARENT).getField("ownerKey").setBackendName("private_probe");
      context.getTable(CHILD).getField("parentId").setBackendName("private_child_probe");
      assertEquals("owner_key", parent.getField("ownerKey").getBackendName());
      assertEquals("parent_id", child.getField("parentId").getBackendName());
   }



   /*******************************************************************************
    ** Native-only restored fields remain rejected by public selection validation.
    *******************************************************************************/
   @Test
   void testRestoredFieldsDoNotBecomePublicSelections() throws QException
   {
      AggregateInput input = input(QInputSource.USER);
      JoinsContext context = new JoinsContext(instance, input, new QQueryFilter());
      assertTrue(context.getTable(PARENT).getFields().containsKey("ownerKey"));
      assertTrue(context.getTable(CHILD).getFields().containsKey("parentId"));
      Set<String> removed = Set.of("ownerKey", "first.ownerKey", "first.parentId");
      assertEquals(removed, Set.copyOf(SelectionValidationHelper.getUnrecognizedFieldNames(input, removed)));
      assertTrue(SelectionValidationHelper.getUnrecognizedFieldNames(input, Set.of("id", "first.name")).isEmpty());
      assertFalse(input.getTable().getFields().containsKey("ownerKey"));
   }



   /*******************************************************************************
    ** An explicitly removed joined table must never fall back to canonical data.
    *******************************************************************************/
   @Test
   void testNullPersonalizedJoinedTableRefusesContext() throws QException
   {
      removeJoinedTable = true;
      AggregateInput input = input(QInputSource.USER);
      String canonicalBefore = JsonUtils.toJson(child);
      QException failure = assertThrows(QException.class, () -> new JoinsContext(instance, input, new QQueryFilter()));
      assertTrue(failure.getMessage().contains("Unrecognized name for join table"));
      assertEquals(List.of(QInputSource.USER), joinedSources);
      assertSame(child, instance.getTable(CHILD));
      assertEquals(canonicalBefore, JsonUtils.toJson(child));
   }



   /*******************************************************************************
    ** Existing constructor calls retain canonical tables and skip personalization.
    *******************************************************************************/
   @Test
   void testLegacyConstructorRemainsCanonical() throws QException
   {
      removeJoinedTable = true;
      AggregateInput input = input(QInputSource.USER);
      JoinsContext context = new JoinsContext(instance, PARENT, input.getQueryJoins(), new QQueryFilter());
      assertSame(parent, context.getTable(PARENT));
      assertSame(child, context.getTable(CHILD));
      assertTrue(joinedSources.isEmpty());
      assertEquals(0, parentCalls);
      assertEquals("name", context.getFieldAndTableNameOrAlias("first.name").field().getBackendName());
   }



   /*******************************************************************************
    ** SYSTEM is passed unchanged to the joined-table personalizer.
    *******************************************************************************/
   @Test
   void testSystemSourceReachesJoinedPersonalizer() throws QException
   {
      AggregateInput input = input(QInputSource.SYSTEM);
      input.setTableMetaData(parent);
      JoinsContext context = new JoinsContext(instance, input, new QQueryFilter());
      assertEquals(List.of(QInputSource.SYSTEM), joinedSources);
      assertSame(parent, context.getTable(PARENT));
      assertSame(child, context.getTable(CHILD));
      assertEquals(0, parentCalls);
   }



   /*******************************************************************************
    ** Both explicit aliases use the same registered relationship.
    *******************************************************************************/
   private AggregateInput input(InputSource source)
   {
      AggregateInput input = new AggregateInput(PARENT).withInputSource(source);
      input.setTableMetaData(activeParent);
      for(String alias : List.of("first", "second"))
      {
         input.withQueryJoin(new QueryJoin().withJoinTable(CHILD).withAlias(alias).withType(QueryJoin.Type.INNER)
            .withJoinMetaData(instance.getJoin("contextJoin")));
      }
      return input;
   }



   /*******************************************************************************
    ** Minimal physical definitions used by both tables.
    *******************************************************************************/
   private QTableMetaData table(String name)
   {
      return new QTableMetaData().withName(name).withBackendName("contextMemory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id"))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withBackendName("name"))
         .withField(new QFieldMetaData("ownerKey", QFieldType.STRING).withBackendName("owner_key"));
   }



   /*******************************************************************************
    ** The private field remains required to enforce READ authorization.
    *******************************************************************************/
   private static RecordSecurityLock ownerLock()
   {
      return new RecordSecurityLock().withSecurityKeyType(OWNER).withFieldName("ownerKey")
         .withLockScope(RecordSecurityLock.LockScope.READ);
   }



   /*******************************************************************************
    ** Inspect generated predicates without relying on their tree nesting shape.
    *******************************************************************************/
   private List<QFilterCriteria> criteria(QQueryFilter filter)
   {
      List<QFilterCriteria> result = new ArrayList<>(CollectionUtils.nonNullList(filter.getCriteria()));
      for(QQueryFilter childFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         result.addAll(criteria(childFilter));
      }
      return result;
   }



   /*******************************************************************************
    ** String-valued owner matching must survive hidden structural metadata.
    *******************************************************************************/
   private void assertOwnerCriterion(List<QFilterCriteria> criteria, String fieldName)
   {
      List<QFilterCriteria> matches = CollectionUtils.nonNullList(criteria).stream().filter(criterion -> fieldName.equals(criterion.getFieldName())).toList();
      assertEquals(1, matches.size());
      assertEquals(QCriteriaOperator.IN, matches.get(0).getOperator());
      assertEquals(List.of("ALPHA"), matches.get(0).getValues());
   }



   /*******************************************************************************
    ** Static callback state belongs to one test only.
    *******************************************************************************/
   private static void clearCapture()
   {
      joinedSources.clear();
      parentCalls = 0;
      removeJoinedTable = false;
      personalizedChild = null;
   }



   /*******************************************************************************
    ** Remove presentation fields while retaining a READ lock and active mapping.
    *******************************************************************************/
   public static class JoinedPersonalizer implements TableMetaDataPersonalizerInterface
   {
      /***************************************************************************
       ** Clone every USER change; canonical metadata is never mutated.
       ***************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(PARENT.equals(input.getTableName()))
         {
            parentCalls++;
         }
         if(!CHILD.equals(input.getTableName()))
         {
            return input.getTable();
         }
         joinedSources.add(input.getInputSource());
         if(QInputSource.USER.equals(input.getInputSource()))
         {
            if(removeJoinedTable)
            {
               return null;
            }
            personalizedChild = input.getTable().clone();
            personalizedChild.getFields().remove("parentId");
            personalizedChild.getFields().remove("ownerKey");
            personalizedChild.getField("name").setBackendName("active_child_name");
            personalizedChild.setRecordSecurityLocks(new ArrayList<>(List.of(ownerLock())));
            return personalizedChild;
         }
         return input.getTable();
      }
   }
}
