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

package com.kingsrook.qqq.backend.core.actions.tables.query;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.actions.tables.query.CriteriaExpansionContext;
import com.kingsrook.qqq.backend.core.actions.tables.query.SearchTermKind;
import com.kingsrook.qqq.backend.core.actions.tables.query.VirtualFieldCriteriaExpanderInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QSubQuery;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Query hooks integration tests against memory.
 *******************************************************************************/
public class QueryHooksTest extends BaseTest
{
   /*******************************************************************************
    ** Seed three parents, visible links and an inaccessible link to parent 3.
    *******************************************************************************/
   @BeforeEach
   void setupTags() throws Exception
   {
      
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("tagTenant"));
      QTableMetaData parents = new QTableMetaData().withName("tagParent").withBackendName("memory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withVirtualField(new QVirtualFieldMetaData("tags", QFieldType.STRING).withCriteriaExpander(new QCodeReference(TagExpander.class)))
         .withSearchFields("name", "tags");
      QTableMetaData links = new QTableMetaData().withName("tagLink").withBackendName("memory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("recordId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("tag", QFieldType.STRING))
         .withField(new QFieldMetaData("tenant", QFieldType.INTEGER));
      
      QContext.getQInstance().addTable(parents);
      QContext.getQInstance().addTable(links);
      new QInstanceEnricher(QContext.getQInstance()).enrich();
      new InsertAction().execute(new InsertInput("tagParent").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "First"),
         new QRecord().withValue("id", 2).withValue("name", "Second"),
         new QRecord().withValue("id", 3).withValue("name", "urgent plain text"))));
      new InsertAction().execute(new InsertInput("tagLink").withRecords(List.of(
         link(1, 1, "urgent", 1), link(2, 1, "blue", 1), link(3, 2, "blue", 1), link(4, 3, "urgent", 2))));
      links.withRecordSecurityLock(new RecordSecurityLock().withFieldName("tenant").withSecurityKeyType("tagTenant"));
      QContext.getQSession().withSecurityKeyValue("tagTenant", 1);
   }



   /*******************************************************************************
    ** Construct one link row.
    *******************************************************************************/
   private static QRecord link(int id, int recordId, String tag, int tenant)
   {
      return (new QRecord().withValue("id", id).withValue("recordId", recordId).withValue("tag", tag).withValue("tenant", tenant));
   }



   /*******************************************************************************
    ** Query the parents using virtual criteria.
    *******************************************************************************/
   private static List<Serializable> ids(QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput("tagParent").withFilter(filter)).getRecords().stream().map(r -> r.getValue("id")).toList());
   }



   /*******************************************************************************
    ** Construct the user's virtual criterion.
    *******************************************************************************/
   private static QQueryFilter tags(QCriteriaOperator operator, Serializable... values)
   {
      return (new QQueryFilter(new QFilterCriteria("tags", operator, values)));
   }



   /*******************************************************************************
    ** Membership, absence, all-of, and empty membership preserve their semantics.
    *******************************************************************************/
   @Test
   void testOperatorsAndSecurity() throws Exception
   {
      assertThat(ids(tags(QCriteriaOperator.IN, "urgent"))).containsExactly(1);
      assertThat(ids(tags(QCriteriaOperator.NOT_IN, "urgent"))).containsExactlyInAnyOrder(2, 3);
      assertThat(ids(tags(QCriteriaOperator.EQUALS, "urgent"))).containsExactly(1);
      assertThat(ids(tags(QCriteriaOperator.NOT_EQUALS, "urgent"))).containsExactlyInAnyOrder(2, 3);
      assertThat(ids(tags(QCriteriaOperator.IS_BLANK))).containsExactly(3);
      assertThat(ids(tags(QCriteriaOperator.IS_NOT_BLANK))).containsExactlyInAnyOrder(1, 2);
      assertThat(ids(tags(QCriteriaOperator.CONTAINS_ALL, "urgent", "blue"))).containsExactly(1);
      assertThat(ids(tags(QCriteriaOperator.IN, "missing"))).isEmpty();
      assertThat(ids(tags(QCriteriaOperator.NOT_IN, "missing"))).containsExactlyInAnyOrder(1, 2, 3);
      QContext.getQSession().withSecurityKeyValue("tagTenant", 2);
      assertThat(ids(tags(QCriteriaOperator.IN, "urgent"))).containsExactlyInAnyOrder(1, 3);
   }



   /*******************************************************************************
    ** Expansions preserve the parent's boolean operator and nested grouping.
    *******************************************************************************/
   @Test
   void testNestedCountAndAggregate() throws Exception
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria("id", QCriteriaOperator.EQUALS, 3)
         .withSubFilter(tags(QCriteriaOperator.CONTAINS_ALL, "urgent", "blue"));
      assertThat(ids(filter)).containsExactlyInAnyOrder(1, 3);
      assertThat(CountAction.execute("tagParent", filter)).isEqualTo(2);
      Aggregate sum = new Aggregate("id", AggregateOperator.SUM);
      assertThat(new AggregateAction().execute(new AggregateInput("tagParent").withFilter(filter).withAggregate(sum))
         .getResults().get(0).getAggregateValue(sum).toString()).isEqualTo("4");
      assertThat(filter.getSubFilters().get(0).getCriteria().get(0).getFieldName()).isEqualTo("tags");
   }



   /*******************************************************************************
    ** TEXT includes plain fields, whereas TAG asks only the virtual expander.
    *******************************************************************************/
   @Test
   void testQuickSearch() throws Exception
   {
      assertThat(search("urgent")).containsExactlyInAnyOrder(1, 3);
      assertThat(search("  #urgent  ")).containsExactly(1);
      assertThat(search("#urg")).isEmpty();
      QContext.getQInstance().getTable("tagParent").withSearchFields("name");
      assertThat(search("#urgent")).isEmpty();
   }



   /*******************************************************************************
    ** Search just the fixture table.
    *******************************************************************************/
   private static List<Serializable> search(String term) throws QException
   {
      return (new RecordSearchAction().execute(new RecordSearchInput().withSearchTerm(term).withTableNames(List.of("tagParent")))
         .getResults().stream().map(RecordSearchResult::getRecordId).toList());
   }



   /*******************************************************************************
    ** Both backend paths enforce link-table permission even for internal callers.
    *******************************************************************************/
   @Test
   void testLinkPermission() throws Exception
   {
      QContext.getQInstance().getTable("tagLink").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      assertThatThrownBy(() -> ids(tags(QCriteriaOperator.IN, "urgent"))).isInstanceOf(QException.class).hasMessageContaining("Permission");
   }



   /*******************************************************************************
    ** Validation accepts expander-only metadata and rejects invalid inner filters.
    *******************************************************************************/
   @Test
   void testValidation() throws Exception
   {
      new QInstanceValidator().validate(QContext.getQInstance());
      assertThat(new QFrontendVirtualFieldMetaData(QContext.getQInstance().getTable("tagParent").getVirtualFields().get("tags")).getIsQueryCriteria()).isTrue();
      assertThatThrownBy(() -> ids(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.CONTAINS_ALL, "a"))))
         .hasMessageContaining("CONTAINS_ALL is only supported");
      assertThatThrownBy(() -> ids(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN,
         new QSubQuery("tagLink", "recordId", new QQueryFilter(new QFilterCriteria("missing", QCriteriaOperator.EQUALS, 1)))))))
         .hasMessageContaining("unrecognized field");
      assertThatThrownBy(() -> ids(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, new QSubQuery("tagLink", "recordId", null)))))
         .hasMessageContaining("single value of IN or NOT_IN");
      assertThatThrownBy(() -> ids(tags(QCriteriaOperator.GREATER_THAN, "a"))).hasMessageContaining("Unsupported tag operator");
   }



   /*******************************************************************************
    ** Cross-backend materialization and its hard limit.
    *******************************************************************************/
   @Test
   void testMaterializationLimit() throws Exception
   {
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("otherMemory").withBackendType(MemoryBackendModule.class));
      QContext.getQInstance().getTable("tagLink").setBackendName("otherMemory");
      QueryFilterPreprocessor.setMaxMaterialized(2);
      try
      {
         assertThat(ids(tags(QCriteriaOperator.IN, "urgent"))).containsExactly(1);
         assertThat(ids(tags(QCriteriaOperator.IN, "blue"))).containsExactlyInAnyOrder(1, 2);
         assertThatThrownBy(() -> ids(tags(QCriteriaOperator.IS_NOT_BLANK)))
            .isInstanceOf(QUserFacingException.class).hasMessage("This filter matches too many records to combine across databases.");
      }
      finally
      {
         QueryFilterPreprocessor.setMaxMaterialized(QSubQuery.MAX_MATERIALIZED);
      }
   }




   /*******************************************************************************
    ** Re-expansion is rejected rather than recursing or leaking virtual fields.
    *******************************************************************************/
   @Test
   void testSinglePassAndQualifiedField() throws Exception
   {
      assertThat(ids(new QQueryFilter(new QFilterCriteria("tagParent.tags", QCriteriaOperator.IN, "urgent")))).containsExactly(1);
      QContext.getQInstance().getTable("tagParent").getVirtualFields().get("tags").withCriteriaExpander(new QCodeReference(RecursiveExpander.class));
      assertThatThrownBy(() -> ids(tags(QCriteriaOperator.IN, "urgent")))
         .isInstanceOf(QUserFacingException.class).hasMessageContaining("must return ordinary criteria");
   }



   /*******************************************************************************
    ** Deliberately invalid expander used to prove the single-pass guard.
    *******************************************************************************/
   public static class RecursiveExpander extends TagExpander
   {
      /*******************************************************************************
       ** Return the same virtual criterion.
       *******************************************************************************/
      @Override
      public QQueryFilter expand(QTableMetaData table, QFilterCriteria criteria, CriteriaExpansionContext context)
      {
         return (new QQueryFilter(criteria));
      }
   }



   /*******************************************************************************
    ** Test-only tag implementation; business policy belongs to the expander.
    *******************************************************************************/
   public static class TagExpander implements VirtualFieldCriteriaExpanderInterface
   {
      /*******************************************************************************
       ** Expand all-of as an AND of independent membership sub-queries.
       *******************************************************************************/
      @Override
      public QQueryFilter expand(QTableMetaData table, QFilterCriteria criteria, CriteriaExpansionContext context) throws QException
      {
         if(criteria.getOperator() == QCriteriaOperator.CONTAINS_ALL)
         {
            QQueryFilter result = new QQueryFilter();
            for(Serializable value : criteria.getValues())
            {
               result.addSubFilter(expand(table, new QFilterCriteria("tags", QCriteriaOperator.IN, value), context));
            }
            return (result);
         }
         QCriteriaOperator outer = switch(criteria.getOperator())
         {
            case IN, EQUALS, IS_NOT_BLANK -> QCriteriaOperator.IN;
            case NOT_IN, NOT_EQUALS, IS_BLANK -> QCriteriaOperator.NOT_IN;
            default -> throw new QUserFacingException("Unsupported tag operator: " + criteria.getOperator());
         };
         QQueryFilter inner = new QQueryFilter();
         if(criteria.getOperator() != QCriteriaOperator.IS_BLANK && criteria.getOperator() != QCriteriaOperator.IS_NOT_BLANK)
         {
            inner.addCriteria(new QFilterCriteria("tag", QCriteriaOperator.IN, criteria.getValues()));
         }
         return (new QQueryFilter(new QFilterCriteria("id", outer, new QSubQuery("tagLink", "recordId", inner))));
      }



      /*******************************************************************************
       ** Exact tag names for TAG, partial labels for TEXT.
       *******************************************************************************/
      @Override
      public QQueryFilter expandSearchTerm(QTableMetaData table, String term, SearchTermKind kind, CriteriaExpansionContext context)
      {
         return (new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, new QSubQuery("tagLink", "recordId",
            new QQueryFilter(new QFilterCriteria("tag", kind == SearchTermKind.TAG ? QCriteriaOperator.EQUALS : QCriteriaOperator.CONTAINS, term))))));
      }
   }
}
