/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin;


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Exercise the middleware authorization boundary with actual metadata and rules.
 *******************************************************************************/
class JoinedTablePermissionsTest
{
   private QInstance instance;

   /*******************************************************************************
    ** Only the relationship targets vary; no backend query is needed to authorize.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      instance = TestUtils.defineInstance();
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.addTable(new QTableMetaData().withName("toy").withBackendName(instance.getTable("pet").getBackendName())
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("petId", QFieldType.INTEGER))
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS)));
      instance.addJoin(new QJoinMetaData().withName("petJoinToy").withLeftTable("pet").withRightTable("toy")
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "petId")));
      instance.getTable("person").withExposedJoin(new ExposedJoin().withJoinTable("toy").withLabel("Toys")
         .withJoinPath(List.of("personJoinPet", "petJoinToy")));
      QContext.init(instance, new QSession().withPermissions("person.read", "pet.write", "toy.read"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Filters and ordering infer reads even without explicit joins.
    *******************************************************************************/
   @Test
   void testImplicitFilterAndOrderingRequireRead() throws Exception
   {
      List<QQueryFilter> filters = List.of(
         new QQueryFilter(new QFilterCriteria("pet.species", QCriteriaOperator.EQUALS, "dog")),
         new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria().withFieldName("firstName")
            .withOperator(QCriteriaOperator.EQUALS).withOtherFieldName("pet.name"))),
         new QQueryFilter().withOrderBy(new QFilterOrderBy("pet.name")));
      for(QQueryFilter filter : filters)
      {
         QContext.setQSession(new QSession().withPermissions("person.read", "pet.write"));
         CountInput input = new CountInput("person").withInputSource(QInputSource.USER).withFilter(filter);
         assertDeniedWithoutMutation(input);
         QContext.getQSession().withPermissions("pet.read");
         assertAllowedWithoutMutation(input);
      }
   }



   /*******************************************************************************
    ** Permission for the exposed endpoint does not grant its intermediate table.
    *******************************************************************************/
   @Test
   void testExposedPathChecksIntermediateTable() throws Exception
   {
      CountInput input = new CountInput("person").withQueryJoin(new QueryJoin("toy").withSelect(true));
      assertDeniedWithoutMutation(input);
      QContext.getQSession().withPermissions("pet.read");
      assertAllowedWithoutMutation(input);
   }



   /*******************************************************************************
    ** A client join cannot use an internal security alias to read a denied base.
    *******************************************************************************/
   @Test
   void testOrdinaryJoinChecksSecurityAliasBase() throws Exception
   {
      addPetSecurityLock();
      CountInput input = new CountInput("person").withQueryJoin(new QueryJoin(instance.getJoin("petJoinToy"))
         .withBaseTableOrAlias("pet_forSecurityJoin_personJoinPet"));
      assertDeniedWithoutMutation(input);
      QContext.getQSession().withPermissions("pet.read");
      assertAllowedWithoutMutation(input);
   }



   /*******************************************************************************
    ** A security-only join remains usable, while requesting it requires READ.
    *******************************************************************************/
   @Test
   void testSecurityOnlyJoinDoesNotGrantExplicitAccess() throws Exception
   {
      addPetSecurityLock();
      CountInput input = new CountInput("person").withFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2)));
      assertAllowedWithoutMutation(input);
      input.withQueryJoin(new QueryJoin(instance.getJoin("personJoinPet")).withAlias("animal"));
      assertDeniedWithoutMutation(input);
      QContext.getQSession().withPermissions("pet.read");
      assertAllowedWithoutMutation(input);
   }



   /*******************************************************************************
    ** Custom checkers receive the original action and real table, once per table.
    *******************************************************************************/
   @Test
   void testAliasesUseCustomPermissionRules() throws Exception
   {
      CountInput input = new CountInput("person").withInputSource(QInputSource.USER)
         .withQueryJoin(new QueryJoin(instance.getJoin("personJoinPet")).withAlias("animal"))
         .withQueryJoin(new QueryJoin(instance.getJoin("personJoinPet")).withAlias("otherAnimal"));
      AtomicBoolean allow = new AtomicBoolean();
      AtomicInteger checks = new AtomicInteger();
      QContext.setObject("permissionTestInput", new AtomicReference<>(input));
      QContext.setObject("permissionTestAllowed", allow);
      QContext.setObject("permissionTestChecks", checks);
      instance.getTable("pet").getPermissionRules().setCustomPermissionChecker(new QCodeReference(OriginalInputPermissionChecker.class));
      assertDeniedWithoutMutation(input);
      assertEquals(1, checks.get());
      allow.set(true);
      assertAllowedWithoutMutation(input);
      assertEquals(2, checks.get());
   }



   /*******************************************************************************
    ** All requested descendant tables are authorized and guard cycles terminate.
    *******************************************************************************/
   @Test
   void testNestedAssociationsAndCycles() throws Exception
   {
      instance.getTable("pet").withAssociation(new Association().withName("toys").withAssociatedTableName("toy").withJoinName("petJoinToy"));
      instance.getTable("toy").withField(new QFieldMetaData("personId", QFieldType.INTEGER))
         .withAssociation(new Association().withName("people").withAssociatedTableName("person").withJoinName("toyJoinPerson"));
      instance.addJoin(new QJoinMetaData().withName("toyJoinPerson").withLeftTable("toy").withRightTable("person")
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("personId", "id")));
      QContext.setQSession(new QSession().withPermissions("person.read", "pet.read"));
      GetInput input = new GetInput("person").withInputSource(QInputSource.USER);
      assertDoesNotThrow(() -> JoinedTablePermissions.checkAssociationReadPermissions(input));
      input.setIncludeAssociations(true);
      assertThrows(QPermissionDeniedException.class, () -> JoinedTablePermissions.checkAssociationReadPermissions(input));
      QContext.getQSession().withPermissions("toy.read");
      assertDoesNotThrow(() -> JoinedTablePermissions.checkAssociationReadPermissions(input));
      assertEquals(QInputSource.USER, input.getInputSource());
   }



   /*******************************************************************************
    ** Associations removed by personalization are never read or permission-checked.
    *******************************************************************************/
   @Test
   void testAssociationTraversalUsesPersonalizedUserMetadata() throws Exception
   {
      GetInput input = new GetInput("person").withInputSource(QInputSource.USER).withIncludeAssociations(true);
      QContext.setObject("associationTableToHide", "person");
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE,
         new QCodeReference(HideAssociationsForUser.class));
      assertDoesNotThrow(() -> JoinedTablePermissions.checkAssociationReadPermissions(input));

      instance.getTable("pet").withAssociation(new Association().withName("toys").withAssociatedTableName("toy").withJoinName("petJoinToy"));
      QContext.setQSession(new QSession().withPermissions("person.read", "pet.read"));
      QContext.setObject("associationTableToHide", "pet");
      assertDoesNotThrow(() -> JoinedTablePermissions.checkAssociationReadPermissions(input));
      assertEquals(1, instance.getTable("person").getAssociations().size());
      assertEquals(1, instance.getTable("pet").getAssociations().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addPetSecurityLock()
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("species"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("species")
         .withFieldName("pet.species").withJoinNameChain(List.of("personJoinPet")));
      QContext.getQSession().withSecurityKeyValue("species", "dog");
   }



   /*******************************************************************************
    ** Permission resolution must not change the query it is authorizing.
    *******************************************************************************/
   private void assertDeniedWithoutMutation(CountInput input) throws Exception
   {
      String filter = JsonUtils.toJson(input.getFilter());
      String joins = JsonUtils.toJson(input.getQueryJoins());
      QPermissionDeniedException denied = assertThrows(QPermissionDeniedException.class,
         () -> JoinedTablePermissions.checkReadPermissions(input, input.getQueryJoins(), input.getFilter()));
      assertEquals("Permission denied.", denied.getMessage());
      assertEquals(filter, JsonUtils.toJson(input.getFilter()));
      assertEquals(joins, JsonUtils.toJson(input.getQueryJoins()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertAllowedWithoutMutation(CountInput input) throws Exception
   {
      String filter = JsonUtils.toJson(input.getFilter());
      String joins = JsonUtils.toJson(input.getQueryJoins());
      assertDoesNotThrow(() -> JoinedTablePermissions.checkReadPermissions(input, input.getQueryJoins(), input.getFilter()));
      assertEquals(filter, JsonUtils.toJson(input.getFilter()));
      assertEquals(joins, JsonUtils.toJson(input.getQueryJoins()));
   }



   /*******************************************************************************
    ** The ad-hoc permission customizer uses the same context as its caller.
    *******************************************************************************/
   public static class OriginalInputPermissionChecker implements CustomPermissionChecker
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkPermissionsThrowing(AbstractActionInput action, MetaDataWithPermissionRules table) throws QPermissionDeniedException
      {
         assertSame(((AtomicReference<?>) QContext.getObject("permissionTestInput")).get(), action);
         assertEquals("pet", table.getName());
         ((AtomicInteger) QContext.getObject("permissionTestChecks")).incrementAndGet();
         if(!((AtomicBoolean) QContext.getObject("permissionTestAllowed")).get())
         {
            throw new QPermissionDeniedException("Application-specific denial details");
         }
      }
   }



   /*******************************************************************************
    ** Hide a requested association edge only for user-sourced metadata.
    *******************************************************************************/
   public static class HideAssociationsForUser implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = input.getTable();
         return table.getName().equals(QContext.getObject("associationTableToHide")) && QInputSource.USER.equals(input.getInputSource())
            ? table.clone().withAssociations(List.of()) : table;
      }
   }
}
