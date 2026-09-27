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
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for {@link JoinsContext}.
 **
 ** Uses a "company hierarchy" metadata model with 6 tables and 12 joins:
 ** <pre>
 **   company 1:N department 1:N employee 1:1 employeeDetail
 **               department N:1 employee (manager)
 **                              employee N:1 employee (mentor, self-join)
 **                              employee 1:N jobHistory N:1 jobTitle
 ** </pre>
 *******************************************************************************/
class JoinsContextTest extends BaseTest
{
   private static final String BACKEND_NAME = "memory";

   private static final String COMPANY_TABLE         = "company";
   private static final String DEPARTMENT_TABLE      = "department";
   private static final String EMPLOYEE_TABLE        = "employee";
   private static final String EMPLOYEE_DETAIL_TABLE = "employeeDetail";
   private static final String JOB_TITLE_TABLE       = "jobTitle";
   private static final String JOB_HISTORY_TABLE     = "jobHistory";

   private static final String COMPANY_JOIN_DEPARTMENT          = "companyDepartments";
   private static final String DEPARTMENT_JOIN_COMPANY          = "departmentCompany";
   private static final String DEPARTMENT_JOIN_EMPLOYEE         = "departmentEmployees";
   private static final String EMPLOYEE_JOIN_DEPARTMENT         = "employeeDepartment";
   private static final String EMPLOYEE_JOIN_EMPLOYEE_AS_MENTOR = "employeeMentor";
   private static final String EMPLOYEE_JOIN_EMPLOYEE_DETAIL    = "employeeEmployeeDetail";
   private static final String EMPLOYEE_DETAIL_JOIN_EMPLOYEE    = "employeeDetailEmployee";
   private static final String EMPLOYEE_JOIN_JOB_HISTORY        = "employeeJobHistories";
   private static final String JOB_HISTORY_JOIN_EMPLOYEE        = "jobHistoryEmployee";
   private static final String JOB_HISTORY_JOIN_JOB_TITLE       = "jobHistoryJobTitle";
   private static final String JOB_TITLE_JOIN_JOB_HISTORY       = "jobTitleJobHistories";
   private static final String DEPARTMENT_JOIN_MANAGER          = "departmentManager";

   private static final String SECURITY_KEY_TYPE_COMPANY       = "companyKey";
   private static final String SECURITY_KEY_COMPANY_ALL_ACCESS = "companyAllAccess";



   /***************************************************************************
    ** Build the base QInstance with 6 tables and 12 joins, no security locks.
    ***************************************************************************/
   private QInstance buildBaseInstance()
   {
      QInstance instance = new QInstance();
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      instance.addBackend(new QBackendMetaData().withName(BACKEND_NAME).withBackendType(MemoryBackendModule.class));

      ////////////
      // Tables //
      ////////////
      instance.addTable(new QTableMetaData()
         .withName(COMPANY_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("industry", QFieldType.STRING))
         .withField(new QFieldMetaData("foundedDate", QFieldType.DATE)));

      instance.addTable(new QTableMetaData()
         .withName(DEPARTMENT_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("companyId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("managerId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("budget", QFieldType.DECIMAL)));

      instance.addTable(new QTableMetaData()
         .withName(EMPLOYEE_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("departmentId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("mentorEmployeeId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("hireDate", QFieldType.DATE))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN)));

      instance.addTable(new QTableMetaData()
         .withName(EMPLOYEE_DETAIL_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("employeeId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("phone", QFieldType.STRING))
         .withField(new QFieldMetaData("emergencyContact", QFieldType.STRING))
         .withField(new QFieldMetaData("salary", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("benefitsTier", QFieldType.STRING)));

      instance.addTable(new QTableMetaData()
         .withName(JOB_TITLE_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("grade", QFieldType.STRING))
         .withField(new QFieldMetaData("isExempt", QFieldType.BOOLEAN)));

      instance.addTable(new QTableMetaData()
         .withName(JOB_HISTORY_TABLE)
         .withBackendName(BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("employeeId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("jobTitleId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("startDate", QFieldType.DATE))
         .withField(new QFieldMetaData("endDate", QFieldType.DATE))
         .withField(new QFieldMetaData("salaryAtTime", QFieldType.DECIMAL)));

      ///////////
      // Joins //
      ///////////
      instance.addJoin(new QJoinMetaData().withName(COMPANY_JOIN_DEPARTMENT)
         .withLeftTable(COMPANY_TABLE).withRightTable(DEPARTMENT_TABLE)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "companyId")));

      instance.addJoin(new QJoinMetaData().withName(DEPARTMENT_JOIN_EMPLOYEE)
         .withLeftTable(DEPARTMENT_TABLE).withRightTable(EMPLOYEE_TABLE)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "departmentId")));

      instance.addJoin(new QJoinMetaData().withName(EMPLOYEE_JOIN_EMPLOYEE_AS_MENTOR)
         .withLeftTable(EMPLOYEE_TABLE).withRightTable(EMPLOYEE_TABLE)
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("mentorEmployeeId", "id")));

      instance.addJoin(new QJoinMetaData().withName(EMPLOYEE_JOIN_EMPLOYEE_DETAIL)
         .withLeftTable(EMPLOYEE_TABLE).withRightTable(EMPLOYEE_DETAIL_TABLE)
         .withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "employeeId")));

      instance.addJoin(new QJoinMetaData().withName(EMPLOYEE_JOIN_JOB_HISTORY)
         .withLeftTable(EMPLOYEE_TABLE).withRightTable(JOB_HISTORY_TABLE)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "employeeId")));

      instance.addJoin(new QJoinMetaData().withName(JOB_HISTORY_JOIN_JOB_TITLE)
         .withLeftTable(JOB_HISTORY_TABLE).withRightTable(JOB_TITLE_TABLE)
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("jobTitleId", "id")));

      instance.addJoin(new QJoinMetaData().withName(DEPARTMENT_JOIN_MANAGER)
         .withLeftTable(DEPARTMENT_TABLE).withRightTable(EMPLOYEE_TABLE)
         .withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("managerId", "id")));

      return (instance);
   }



   /***************************************************************************
    ** Add a "companyKey" security key type and a record security lock on the
    ** company table's "id" field.
    ***************************************************************************/
   private void addCompanySecurityLock(QInstance instance)
   {
      addCompanySecurityLock(instance, RecordSecurityLock.NullValueBehavior.DENY);
   }



   /***************************************************************************
    ** Add a "companyKey" security key type and a record security lock on the
    ** company table's "id" field, with a specified null-value behavior.
    ***************************************************************************/
   private void addCompanySecurityLock(QInstance instance, RecordSecurityLock.NullValueBehavior nullValueBehavior)
   {
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(COMPANY_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("id")
         .withNullValueBehavior(nullValueBehavior));
   }



   /***************************************************************************
    ** Use the given instance in QContext (replacing the default one from BaseTest).
    ***************************************************************************/
   private void useInstance(QInstance instance)
   {
      reInitInstanceInContext(instance);
   }



   /*******************************************************************************
    ** Basic construction with no joins and no filter — verifies empty queryJoins
    ** and table presence checks.
    *******************************************************************************/
   @Test
   void testBasicConstructionNoJoinsNoFilter() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), new QQueryFilter());

      assertTrue(joinsContext.getQueryJoins().isEmpty());
      assertTrue(joinsContext.hasTable(COMPANY_TABLE));
      assertFalse(joinsContext.hasTable(DEPARTMENT_TABLE));
      assertTrue(joinsContext.hasAliasOrTable(COMPANY_TABLE));
   }



   /*******************************************************************************
    ** Explicit QueryJoin for a table — verifies it's processed and the table is
    ** visible in the context.
    *******************************************************************************/
   @Test
   void testJoinProcessing() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      QueryJoin departmentJoin = new QueryJoin()
         .withJoinTable(DEPARTMENT_TABLE)
         .withType(QueryJoin.Type.INNER)
         .withJoinMetaData(instance.getJoin(COMPANY_JOIN_DEPARTMENT));
      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(departmentJoin), new QQueryFilter());

      assertEquals(1, joinsContext.getQueryJoins().size());
      assertEquals(DEPARTMENT_TABLE, joinsContext.getQueryJoins().get(0).getJoinTable());
      assertTrue(joinsContext.hasTable(DEPARTMENT_TABLE));
      assertTrue(joinsContext.hasAliasOrTable(DEPARTMENT_TABLE));
      assertEquals(DEPARTMENT_TABLE, joinsContext.resolveTableNameOrAliasToTableName(DEPARTMENT_TABLE));
   }



   /*******************************************************************************
    ** QueryJoin without explicit joinMetaData — verifies that metadata is
    ** auto-filled from the instance's joins.
    *******************************************************************************/
   @Test
   void testJoinMetaDataResolution() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      /////////////////////////////////////////////////////////////////////////////
      // use employee→employeeDetail — only one join between these two tables,   //
      // so metadata auto-resolution is unambiguous.                              //
      /////////////////////////////////////////////////////////////////////////////
      QueryJoin detailJoin = new QueryJoin().withJoinTable(EMPLOYEE_DETAIL_TABLE);

      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(detailJoin), new QQueryFilter());

      QJoinMetaData resolvedMetaData = joinsContext.getQueryJoins().get(0).getJoinMetaData();
      assertNotNull(resolvedMetaData, "JoinMetaData should have been auto-filled");
      assertEquals(EMPLOYEE_TABLE, resolvedMetaData.getLeftTable());
      assertEquals(EMPLOYEE_DETAIL_TABLE, resolvedMetaData.getRightTable());
   }



   /*******************************************************************************
    ** QueryJoin with an alias — verifies alias resolution, hasTable vs hasAliasOrTable
    ** distinctions.
    *******************************************************************************/
   @Test
   void testAliasManagement() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      QueryJoin managerJoin = new QueryJoin()
         .withJoinTable(EMPLOYEE_TABLE)
         .withAlias("mgr")
         .withJoinMetaData(instance.getJoin(DEPARTMENT_JOIN_MANAGER));

      JoinsContext joinsContext = new JoinsContext(instance, DEPARTMENT_TABLE, List.of(managerJoin), new QQueryFilter());

      assertEquals(EMPLOYEE_TABLE, joinsContext.resolveTableNameOrAliasToTableName("mgr"));
      assertTrue(joinsContext.hasAliasOrTable("mgr"));
      assertTrue(joinsContext.hasTable(EMPLOYEE_TABLE));

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // "employee" is NOT a key in the alias map (the key is "mgr"), so hasAliasOrTable is false. //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      assertFalse(joinsContext.hasAliasOrTable(EMPLOYEE_TABLE));
   }



   /*******************************************************************************
    ** Verifies hasTable and hasAliasOrTable behavior with a mix of aliased and
    ** non-aliased joins.
    *******************************************************************************/
   @Test
   void testHasTableAndHasAliasOrTable() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(
         new QueryJoin().withJoinTable(DEPARTMENT_TABLE).withAlias("dept")
            .withJoinMetaData(instance.getJoin(COMPANY_JOIN_DEPARTMENT)),
         new QueryJoin().withJoinTable(EMPLOYEE_TABLE).withBaseTableOrAlias("dept")
            .withJoinMetaData(instance.getJoin(DEPARTMENT_JOIN_EMPLOYEE))
      ), new QQueryFilter());

      //////////////////
      // hasTable     //
      //////////////////
      assertTrue(joinsContext.hasTable(COMPANY_TABLE), "main table");
      assertTrue(joinsContext.hasTable(DEPARTMENT_TABLE), "actual table name for aliased join");
      assertFalse(joinsContext.hasTable("dept"), "alias is not a table name");
      assertTrue(joinsContext.hasTable(EMPLOYEE_TABLE), "unaliased join table");

      ////////////////////////
      // hasAliasOrTable    //
      ////////////////////////
      assertTrue(joinsContext.hasAliasOrTable(COMPANY_TABLE), "main table");
      assertTrue(joinsContext.hasAliasOrTable("dept"), "alias key");
      assertFalse(joinsContext.hasAliasOrTable(DEPARTMENT_TABLE), "aliased — key is 'dept', not 'department'");
      assertTrue(joinsContext.hasAliasOrTable(EMPLOYEE_TABLE), "unaliased join — key is table name");
   }



   /*******************************************************************************
    ** getFieldAndTableNameOrAlias — verifies field resolution with and without
    ** table prefix, and error on malformed input.
    *******************************************************************************/
   @Test
   void testGetFieldAndTableNameOrAlias() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(
         new QueryJoin().withJoinTable(DEPARTMENT_TABLE)
            .withJoinMetaData(instance.getJoin(COMPANY_JOIN_DEPARTMENT))
      ), new QQueryFilter());

      //////////////////////////////////////////////////
      // unqualified field → resolved from main table //
      //////////////////////////////////////////////////
      JoinsContext.FieldAndTableNameOrAlias result = joinsContext.getFieldAndTableNameOrAlias("name");
      assertEquals(COMPANY_TABLE, result.tableNameOrAlias());
      assertEquals("name", result.field().getName());

      ////////////////////////////////////////////////////
      // qualified with joined table name                //
      ////////////////////////////////////////////////////
      result = joinsContext.getFieldAndTableNameOrAlias("department.name");
      assertEquals("department", result.tableNameOrAlias());
      assertEquals("name", result.field().getName());

      ////////////////////////////////////////////////////
      // qualified with main table name                  //
      ////////////////////////////////////////////////////
      result = joinsContext.getFieldAndTableNameOrAlias("company.industry");
      assertEquals("company", result.tableNameOrAlias());
      assertEquals("industry", result.field().getName());

      ///////////////////////////
      // malformed → exception //
      ///////////////////////////
      assertThatThrownBy(() -> joinsContext.getFieldAndTableNameOrAlias("a.b.c"))
         .isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** Filter references a table not in the explicit join list — verifies that
    ** JoinsContext auto-adds a join for it.
    *******************************************************************************/
   @Test
   void testFilterDrivenJoinAddition() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("department.name", QCriteriaOperator.EQUALS, "Engineering"));

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      assertEquals(1, joinsContext.getQueryJoins().size());
      assertEquals(DEPARTMENT_TABLE, joinsContext.getQueryJoins().get(0).getJoinTable());
      assertNotNull(joinsContext.getQueryJoins().get(0).getJoinMetaData(), "metadata should have been auto-filled");
   }



   /*******************************************************************************
    * Filter references a table not in the explicit join list that is not directly
    * joined with the table being queried.  Noting that to find such an indirect
    * join at this time, the main table must have the far-away table identified as
    * an exposed join...
    *******************************************************************************/
   @Test
   void testFilterDrivenJoinAdditionNotDirectlyJoinedToMainTableButFoundAsExposedJoin() throws QException
   {
      QInstance instance = buildBaseInstance();

      instance.getTable(COMPANY_TABLE)
         .withExposedJoin(new ExposedJoin().withJoinTable(EMPLOYEE_TABLE).withLabel("Employees").withJoinPath(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)));

      useInstance(instance);

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("employee.firstName", QCriteriaOperator.EQUALS, "Dave"));

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      assertEquals(2, joinsContext.getQueryJoins().size());
      assertEquals(EMPLOYEE_TABLE, joinsContext.getQueryJoins().get(0).getJoinTable());
      assertEquals(DEPARTMENT_TABLE, joinsContext.getQueryJoins().get(1).getJoinTable());
      assertNotNull(joinsContext.getQueryJoins().get(0).getJoinMetaData(), "metadata should have been populated");
      assertNotNull(joinsContext.getQueryJoins().get(1).getJoinMetaData(), "metadata should have been populated");
   }



   /*******************************************************************************
    ** findJoinMetaData — verifies forward/reverse lookup and null for no direct join.
    *******************************************************************************/
   @Test
   void testFindJoinMetaData() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      //////////////////////////////////////////////////////////////////////
      // use employee as main table — only 1 join to employeeDetail,     //
      // so lookup is unambiguous                                         //
      //////////////////////////////////////////////////////////////////////
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), new QQueryFilter());

      /////////////////////
      // forward lookup  //
      /////////////////////
      QJoinMetaData found = joinsContext.findJoinMetaData(EMPLOYEE_TABLE, EMPLOYEE_DETAIL_TABLE, true);
      assertNotNull(found);
      assertThat(found.getName()).isIn(EMPLOYEE_JOIN_EMPLOYEE_DETAIL, EMPLOYEE_DETAIL_JOIN_EMPLOYEE);

      /////////////////////
      // reverse lookup  //
      /////////////////////
      found = joinsContext.findJoinMetaData(EMPLOYEE_DETAIL_TABLE, EMPLOYEE_TABLE, true);
      assertNotNull(found);

      /////////////////////////////
      // no direct join → null   //
      /////////////////////////////
      found = joinsContext.findJoinMetaData(EMPLOYEE_TABLE, JOB_TITLE_TABLE, true);
      assertNull(found);
   }



   /*******************************************************************************
    ** findJoinMetaData throws when multiple joins match between the same pair of
    ** tables (e.g., department→employee has both departmentEmployees and
    ** departmentManager) and exposed joins don't disambiguate.
    *******************************************************************************/
   @Test
   void testFindJoinMetaDataMultipleMatchesThrows() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      ///////////////////////////////////////////////////////////////
      // department→employee has 2 joins, so ambiguity should fail //
      ///////////////////////////////////////////////////////////////
      JoinsContext joinsContext = new JoinsContext(instance, DEPARTMENT_TABLE, List.of(), new QQueryFilter());

      assertThatThrownBy(() -> joinsContext.findJoinMetaData(DEPARTMENT_TABLE, EMPLOYEE_TABLE, false))
         .isInstanceOf(RuntimeException.class)
         .hasMessageContaining("More than 1 join was found");
   }



   /*******************************************************************************
    ** Exposed join resolution — an indirect join resolved via an ExposedJoin path.
    ** Querying company and joining to jobHistory should add intermediate joins
    ** (department, employee) automatically.
    *******************************************************************************/
   @Test
   void testExposedJoinResolution() throws QException
   {
      QInstance instance = buildBaseInstance();

      ////////////////////////////////////////////////////////////////////////////////
      // add an exposed join on the company table that leads to jobHistory via path //
      ////////////////////////////////////////////////////////////////////////////////
      instance.getTable(COMPANY_TABLE).withExposedJoin(new ExposedJoin()
         .withJoinTable(JOB_HISTORY_TABLE)
         .withLabel("Job Histories")
         .withJoinPath(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE, EMPLOYEE_JOIN_JOB_HISTORY)));

      useInstance(instance);

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(
         new QueryJoin().withJoinTable(JOB_HISTORY_TABLE).withType(QueryJoin.Type.LEFT)
      ), new QQueryFilter());

      ////////////////////////////////////////////////////////////////////////////////////
      // should have at least 3 joins: department, employee, jobHistory (the requested  //
      // one plus intermediates)                                                         //
      ////////////////////////////////////////////////////////////////////////////////////
      assertThat(joinsContext.getQueryJoins().size()).isGreaterThanOrEqualTo(3);
      assertTrue(joinsContext.hasTable(DEPARTMENT_TABLE));
      assertTrue(joinsContext.hasTable(EMPLOYEE_TABLE));
      assertTrue(joinsContext.hasTable(JOB_HISTORY_TABLE));
   }



   /*******************************************************************************
    ** RecordSecurityLock on the main table's own field — verifies that the filter
    ** gets a security sub-filter with an IN criterion.
    *******************************************************************************/
   @Test
   void testSecurityLockOnMainTable() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance);
      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      ////////////////////////////////////////////////////////////////
      // no extra joins needed — lock field is on the main table    //
      ////////////////////////////////////////////////////////////////
      assertThat(joinsContext.getQueryJoins()).noneMatch(qj -> qj instanceof ImplicitQueryJoinForSecurityLock);

      ///////////////////////////////////////////////////////////
      // filter should have a security sub-filter with IN (1)  //
      ///////////////////////////////////////////////////////////
      assertThat(filter.getSubFilters()).isNotEmpty();
      QFilterCriteria securityCriteria = findSecurityCriteria(filter);
      assertNotNull(securityCriteria, "Should find a security criterion");
      assertEquals(QCriteriaOperator.IN, securityCriteria.getOperator());
      assertThat(securityCriteria.getValues()).contains(1);
   }



   /*******************************************************************************
    ** All-access key bypasses security filter — verifies no security sub-filters
    ** are added when the session has the all-access key.
    *******************************************************************************/
   @Test
   void testSecurityLockAllAccessBypasses() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance);
      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_COMPANY_ALL_ACCESS, true));

      QQueryFilter filter = new QQueryFilter();
      new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      ///////////////////////////////////////////////////////////////
      // with all-access, no security sub-filters should be added //
      ///////////////////////////////////////////////////////////////
      assertTrue(filter.getSubFilters() == null || filter.getSubFilters().isEmpty(),
         "No security sub-filters expected with all-access key");
   }



   /*******************************************************************************
    ** NullValueBehavior.ALLOW with empty session — verifies IS_BLANK criterion.
    *******************************************************************************/
   @Test
   void testSecurityLockNullValueBehaviorAllow() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance, RecordSecurityLock.NullValueBehavior.ALLOW);
      useInstance(instance);
      QContext.setQSession(new QSession());

      QQueryFilter filter = new QQueryFilter();
      new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      QFilterCriteria securityCriteria = findSecurityCriteria(filter);
      assertNotNull(securityCriteria, "Should find a security criterion");
      assertEquals(QCriteriaOperator.IS_BLANK, securityCriteria.getOperator());
   }



   /*******************************************************************************
    ** NullValueBehavior.DENY with empty session — verifies FALSE criterion (no rows).
    *******************************************************************************/
   @Test
   void testSecurityLockNullValueBehaviorDeny() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance, RecordSecurityLock.NullValueBehavior.DENY);
      useInstance(instance);
      QContext.setQSession(new QSession());

      QQueryFilter filter = new QQueryFilter();
      new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      QFilterCriteria securityCriteria = findSecurityCriteria(filter);
      assertNotNull(securityCriteria, "Should find a security criterion");
      assertEquals(QCriteriaOperator.FALSE, securityCriteria.getOperator());
   }



   /*******************************************************************************
    ** Security lock with a single-hop joinNameChain — verifies that an
    ** ImplicitQueryJoinForSecurityLock is added.
    *******************************************************************************/
   @Test
   void testSecurityLockWithJoinChain() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(DEPARTMENT_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT)));

      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 42));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, DEPARTMENT_TABLE, List.of(), filter);

      /////////////////////////////////////////////////////////////////////
      // should have an ImplicitQueryJoinForSecurityLock in the join list //
      /////////////////////////////////////////////////////////////////////
      assertThat(joinsContext.getQueryJoins())
         .anyMatch(qj -> qj instanceof ImplicitQueryJoinForSecurityLock);

      //////////////////////////////////////////////////////////
      // the implicit join should have an alias containing     //
      // "_forSecurityJoin_"                                   //
      //////////////////////////////////////////////////////////
      QueryJoin securityJoin = joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .findFirst().orElseThrow();
      assertThat(securityJoin.getAlias()).contains("_forSecurityJoin_");
   }



   /*******************************************************************************
    ** Security lock with a multi-hop joinNameChain — verifies multiple implicit
    ** joins are added.
    *******************************************************************************/
   @Test
   void testSecurityLockMultiHopJoinChain() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(EMPLOYEE_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)));

      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 7));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), filter);

      ////////////////////////////////////////////////////
      // should have 2 ImplicitQueryJoinForSecurityLock  //
      ////////////////////////////////////////////////////
      long implicitCount = joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .count();
      assertEquals(2, implicitCount, "Should have 2 implicit security joins for 2-hop chain");
   }



   /*******************************************************************************
    ** MultiRecordSecurityLock with OR operator — verifies OR sub-filter is created
    ** and implicit joins are LEFT type.
    *******************************************************************************/
   @Test
   void testMultiRecordSecurityLocksOr() throws QException
   {
      QInstance instance = buildBaseInstance();

      String departmentKeyType = "departmentKey";
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(departmentKeyType));

      instance.getTable(EMPLOYEE_TABLE).withRecordSecurityLock(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
            .withFieldName("company.id")
            .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)))
         .withLock(new RecordSecurityLock()
            .withSecurityKeyType(departmentKeyType)
            .withFieldName("departmentId")));

      useInstance(instance);
      QContext.setQSession(new QSession()
         .withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1)
         .withSecurityKeyValue(departmentKeyType, 10));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), filter);

      /////////////////////////////////////////////////////////////////
      // the filter should contain an OR sub-filter for the two locks //
      /////////////////////////////////////////////////////////////////
      assertThat(filter.getSubFilters()).isNotEmpty();
      boolean foundOr = hasSubFilterWithOperator(filter, QQueryFilter.BooleanOperator.OR);
      assertTrue(foundOr, "Should have an OR sub-filter for the multi-lock");

      ///////////////////////////////////////////////////////////////////////////
      // implicit security joins in an OR context should be LEFT (not INNER)   //
      ///////////////////////////////////////////////////////////////////////////
      joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .forEach(qj -> assertEquals(QueryJoin.Type.LEFT, qj.getType(),
            "Implicit security join in OR context should be LEFT"));
   }



   /*******************************************************************************
    ** omitSecurity constructor — verifies no security joins/filters are added
    ** even when a security lock is defined.
    *******************************************************************************/
   @Test
   void testOmitSecurityConstructor() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance);
      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(COMPANY_TABLE, filter, true);

      assertTrue(joinsContext.getQueryJoins().isEmpty());
      assertTrue(filter.getSubFilters() == null || filter.getSubFilters().isEmpty(),
         "No security sub-filters with omitSecurity=true");
   }



   /*******************************************************************************
    ** OR input filter with security — verifies the filter is restructured:
    ** the original OR becomes a sub-filter inside a new AND, alongside the
    ** security filter.
    *******************************************************************************/
   @Test
   void testOrFilterWithSecurity() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance);
      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1));

      QQueryFilter filter = new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Acme"))
         .withCriteria(new QFilterCriteria("industry", QCriteriaOperator.EQUALS, "Tech"));

      new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      /////////////////////////////////////////////////////////////////////////
      // the top-level filter should now be AND (wrapping the original OR)   //
      /////////////////////////////////////////////////////////////////////////
      assertEquals(QQueryFilter.BooleanOperator.AND, filter.getBooleanOperator());

      /////////////////////////////////////////////////////////////////////////////
      // one sub-filter should be the original OR; another should be security    //
      /////////////////////////////////////////////////////////////////////////////
      assertThat(filter.getSubFilters()).hasSizeGreaterThanOrEqualTo(2);
      boolean hasOrSub = filter.getSubFilters().stream()
         .anyMatch(sf -> sf.getBooleanOperator() == QQueryFilter.BooleanOperator.OR);
      assertTrue(hasOrSub, "Should have the original OR as a sub-filter");
   }



   /*******************************************************************************
    ** Duplicate alias/table name — verifies QException is thrown.
    *******************************************************************************/
   @Test
   void testDuplicateAliasThrows()
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      assertThatThrownBy(() -> new JoinsContext(instance, COMPANY_TABLE, List.of(
         new QueryJoin().withJoinTable(DEPARTMENT_TABLE),
         new QueryJoin().withJoinTable(DEPARTMENT_TABLE)
      ), new QQueryFilter()))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Duplicate table name or alias");
   }



   /*******************************************************************************
    ** Unrecognized join table — verifies QException is thrown.
    *******************************************************************************/
   @Test
   void testUnrecognizedJoinTableThrows()
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      assertThatThrownBy(() -> new JoinsContext(instance, COMPANY_TABLE, List.of(
         new QueryJoin().withJoinTable("nonExistentTable")
      ), new QQueryFilter()))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Unrecognized name for join table");
   }



   /*******************************************************************************
    ** NullValueBehavior.ALLOW with session values — verifies IS_NULL_OR_IN
    ** criterion (the user HAS key values AND null records are also allowed).
    *******************************************************************************/
   @Test
   void testSecurityLockNullValueBehaviorAllowWithSessionValues() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance, RecordSecurityLock.NullValueBehavior.ALLOW);
      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 5));

      QQueryFilter filter = new QQueryFilter();
      new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      QFilterCriteria securityCriteria = findSecurityCriteria(filter);
      assertNotNull(securityCriteria, "Should find a security criterion");
      assertEquals(QCriteriaOperator.IS_NULL_OR_IN, securityCriteria.getOperator());
      assertThat(securityCriteria.getValues()).contains(5);
   }



   /*******************************************************************************
    ** All-access key in an OR multi-lock context — verifies TRUE operator is used
    ** for the all-access lock, and implicit joins are LEFT.
    *******************************************************************************/
   @Test
   void testSecurityLockAllAccessInOrContext() throws QException
   {
      QInstance instance = buildBaseInstance();

      String departmentKeyType = "departmentKey";
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(departmentKeyType));

      instance.getTable(EMPLOYEE_TABLE).withRecordSecurityLock(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
            .withFieldName("company.id")
            .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)))
         .withLock(new RecordSecurityLock()
            .withSecurityKeyType(departmentKeyType)
            .withFieldName("departmentId")));

      useInstance(instance);

      /////////////////////////////////////////////////////////////////////////
      // user has companyAllAccess = true, but NO departmentKey values at all //
      /////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession()
         .withSecurityKeyValue(SECURITY_KEY_COMPANY_ALL_ACCESS, true));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), filter);

      /////////////////////////////////////////////////////////////////////
      // should have an OR sub-filter; one child should use TRUE operator //
      /////////////////////////////////////////////////////////////////////
      assertTrue(hasSubFilterWithOperator(filter, QQueryFilter.BooleanOperator.OR), "Should have an OR sub-filter");
      QFilterCriteria trueCriteria = findCriteriaWithOperator(filter, QCriteriaOperator.TRUE);
      assertNotNull(trueCriteria, "Should have a TRUE criterion for all-access in OR context");

      ////////////////////////////////////////////////////////////////////////
      // implicit joins in OR context should be LEFT                         //
      ////////////////////////////////////////////////////////////////////////
      joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .forEach(qj -> assertEquals(QueryJoin.Type.LEFT, qj.getType(),
            "Implicit security joins in OR context with all-access should be LEFT"));
   }



   /*******************************************************************************
    ** MultiRecordSecurityLock with AND operator — verifies AND sub-filter is
    ** created with both criteria as IN.
    *******************************************************************************/
   @Test
   void testMultiRecordSecurityLocksAnd() throws QException
   {
      QInstance instance = buildBaseInstance();

      String departmentKeyType = "departmentKey";
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(departmentKeyType));

      instance.getTable(EMPLOYEE_TABLE).withRecordSecurityLock(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
         .withLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
            .withFieldName("company.id")
            .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)))
         .withLock(new RecordSecurityLock()
            .withSecurityKeyType(departmentKeyType)
            .withFieldName("departmentId")));

      useInstance(instance);
      QContext.setQSession(new QSession()
         .withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1)
         .withSecurityKeyValue(departmentKeyType, 10));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), filter);

      ///////////////////////////////////////////////////////////////////
      // filter should have an AND sub-filter with both criteria as IN //
      ///////////////////////////////////////////////////////////////////
      assertThat(filter.getSubFilters()).isNotEmpty();
      assertTrue(hasSubFilterWithOperator(filter, QQueryFilter.BooleanOperator.AND), "Should have an AND sub-filter for the multi-lock");

      //////////////////////////////////////////////////////////////////////////////////
      // for AND, implicit security joins should be INNER (not LEFT as they are in OR) //
      //////////////////////////////////////////////////////////////////////////////////
      joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .forEach(qj -> assertEquals(QueryJoin.Type.INNER, qj.getType(),
            "Implicit security joins in AND context should be INNER"));
   }



   /*******************************************************************************
    ** Security lock join chain walk hits an existing join — verifies no
    ** ImplicitQueryJoinForSecurityLock is created for that hop (reuses existing).
    *******************************************************************************/
   @Test
   void testSecurityLockJoinChainWithExistingJoin() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(DEPARTMENT_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT)));

      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 42));

      ///////////////////////////////////////////////////////////////////////////////
      // user explicitly adds a join to company with the companyDepartments join   //
      ///////////////////////////////////////////////////////////////////////////////
      QueryJoin companyJoin = new QueryJoin()
         .withJoinTable(COMPANY_TABLE)
         .withType(QueryJoin.Type.INNER)
         .withJoinMetaData(instance.getJoin(COMPANY_JOIN_DEPARTMENT));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, DEPARTMENT_TABLE, List.of(companyJoin), filter);

      ////////////////////////////////////////////////////////////////////////////////////
      // since the user already added the companyDepartments join, no implicit joins     //
      // should be created for the security lock — the existing join should be reused.   //
      ////////////////////////////////////////////////////////////////////////////////////
      long implicitCount = joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .count();
      assertEquals(0, implicitCount, "No implicit security joins needed — existing join should be reused");

      /////////////////////////////////////////////////////
      // security criteria should still be applied though //
      /////////////////////////////////////////////////////
      QFilterCriteria securityCriteria = findSecurityCriteria(filter);
      assertNotNull(securityCriteria, "Security criteria should still be applied");
   }



   /*******************************************************************************
    ** Cascading security: company as main table, LEFT join to department,
    ** department has a security lock on company.id with joinNameChain.
    ** Verifies implicit security joins inherit the LEFT type from the source join.
    *******************************************************************************/
   @Test
   void testSecurityLockOnJoinedTableCascade() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(DEPARTMENT_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT)));

      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1));

      ////////////////////////////////////////////////////////////
      // company as main table, user adds LEFT join to department //
      ////////////////////////////////////////////////////////////
      QueryJoin deptJoin = new QueryJoin()
         .withJoinTable(DEPARTMENT_TABLE)
         .withType(QueryJoin.Type.LEFT)
         .withJoinMetaData(instance.getJoin(COMPANY_JOIN_DEPARTMENT));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(deptJoin), filter);

      //////////////////////////////////////////////////////////////////////////////
      // any implicit security joins added for department's lock should also be   //
      // LEFT (inherited from the source join's type)                              //
      //////////////////////////////////////////////////////////////////////////////
      joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .forEach(qj -> assertEquals(QueryJoin.Type.LEFT, qj.getType(),
            "Implicit security joins should inherit LEFT from source join"));
   }



   /*******************************************************************************
    ** LEFT source join → LEFT implicit security joins when walking the chain.
    ** Employee as main, LEFT join to department; department has a security lock
    ** on company.id via 1-hop joinNameChain.
    *******************************************************************************/
   @Test
   void testSecurityLockOnLeftJoinProducesLeftSecurityJoins() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(DEPARTMENT_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT)));

      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1));

      ///////////////////////////////////////////////////////////////////////
      // employee main table, LEFT join to department                       //
      ///////////////////////////////////////////////////////////////////////
      QueryJoin deptJoin = new QueryJoin()
         .withJoinTable(DEPARTMENT_TABLE)
         .withType(QueryJoin.Type.LEFT)
         .withJoinMetaData(instance.getJoin(DEPARTMENT_JOIN_EMPLOYEE));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(deptJoin), filter);

      ///////////////////////////////////////////////////////////////////////////////
      // implicit security joins for company should be LEFT (not INNER) because    //
      // the source join (employee→department) is LEFT                              //
      ///////////////////////////////////////////////////////////////////////////////
      joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .forEach(qj -> assertEquals(QueryJoin.Type.LEFT, qj.getType(),
            "Implicit security joins should be LEFT because source join is LEFT"));
   }



   /*******************************************************************************
    ** Filter with orderBy referencing another table — verifies that a join is
    ** auto-added for the referenced table.
    *******************************************************************************/
   @Test
   void testFilterWithOrderByDrivenJoin() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      QQueryFilter filter = new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("department.name"));

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      assertEquals(1, joinsContext.getQueryJoins().size());
      assertEquals(DEPARTMENT_TABLE, joinsContext.getQueryJoins().get(0).getJoinTable());
   }



   /*******************************************************************************
    ** Filter with subFilter referencing another table — verifies that a join is
    ** auto-added for the referenced table.
    *******************************************************************************/
   @Test
   void testFilterWithSubFilterDrivenJoin() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      QQueryFilter subFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("department.name", QCriteriaOperator.EQUALS, "Engineering"));

      QQueryFilter filter       = new QQueryFilter().withSubFilter(subFilter);
      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      assertEquals(1, joinsContext.getQueryJoins().size());
      assertEquals(DEPARTMENT_TABLE, joinsContext.getQueryJoins().get(0).getJoinTable());
   }



   /*******************************************************************************
    ** Filter with otherFieldName referencing another table — verifies that a join
    ** is auto-added for the referenced table.
    *******************************************************************************/
   @Test
   void testFilterWithOtherFieldNameDrivenJoin() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      ///////////////////////////////////////////////////////////////
      // employee as main table, join to department already present //
      // filter compares employee.departmentId = department.id      //
      // using otherFieldName to reference department table         //
      ///////////////////////////////////////////////////////////////
      QFilterCriteria criteria = new QFilterCriteria()
         .withFieldName("departmentId")
         .withOperator(QCriteriaOperator.EQUALS)
         .withOtherFieldName("department.id");

      QQueryFilter filter       = new QQueryFilter().withCriteria(criteria);
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), filter);

      //////////////////////////////////////////////////////////////////////////
      // the department join should be auto-added because of the otherFieldName //
      //////////////////////////////////////////////////////////////////////////
      assertTrue(joinsContext.hasTable(DEPARTMENT_TABLE));
   }



   /*******************************************************************************
    ** fillInMissingJoinMetaData flip detection — when a QueryJoin has joinMetaData
    ** whose leftTable isn't the main table or any other join's base table,
    ** the metadata should be flipped.
    *******************************************************************************/
   @Test
   void testJoinMetaDataFlipping() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      ///////////////////////////////////////////////////////////////////////////////
      // employee as main table; attach joinMetaData with left=department          //
      // (department is NOT in the query as main or as another join's base table). //
      // So the code should detect this and flip the metadata.                      //
      ///////////////////////////////////////////////////////////////////////////////
      QJoinMetaData deptEmpJoin = instance.getJoin(DEPARTMENT_JOIN_EMPLOYEE);
      QueryJoin queryJoin = new QueryJoin()
         .withJoinTable(DEPARTMENT_TABLE)
         .withJoinMetaData(deptEmpJoin);

      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(queryJoin), new QQueryFilter());

      ////////////////////////////////////////////////////////////////////////
      // after construction, the context's copy of the join should have its //
      // metadata flipped: leftTable should now be employee (main table)    //
      ////////////////////////////////////////////////////////////////////////
      QJoinMetaData resolvedMetaData = joinsContext.getQueryJoins().get(0).getJoinMetaData();
      assertNotNull(resolvedMetaData, "JoinMetaData should still be present after flip");
      assertEquals(EMPLOYEE_TABLE, resolvedMetaData.getLeftTable(), "Left table should be employee (flipped)");
      assertEquals(DEPARTMENT_TABLE, resolvedMetaData.getRightTable(), "Right table should be department (flipped)");
   }



   /*******************************************************************************
    ** findJoinMetaData with exposed join disambiguation — when multiple joins
    ** match between two tables, an ExposedJoin can disambiguate.
    *******************************************************************************/
   @Test
   void testFindJoinMetaDataWithExposedJoinDisambiguation() throws QException
   {
      QInstance instance = buildBaseInstance();

      //////////////////////////////////////////////////////////////////////////////////
      // department→employee is ambiguous: departmentEmployees AND departmentManager   //
      // Add an ExposedJoin on department table to disambiguate                         //
      //////////////////////////////////////////////////////////////////////////////////
      instance.getTable(DEPARTMENT_TABLE).withExposedJoin(new ExposedJoin()
         .withJoinTable(EMPLOYEE_TABLE)
         .withLabel("Employees")
         .withJoinPath(List.of(DEPARTMENT_JOIN_EMPLOYEE)));

      useInstance(instance);

      JoinsContext joinsContext = new JoinsContext(instance, DEPARTMENT_TABLE, List.of(), new QQueryFilter());

      //////////////////////////////////////////////////////////////////
      // This should NOT throw, because the exposed join disambiguates //
      //////////////////////////////////////////////////////////////////
      QJoinMetaData found = joinsContext.findJoinMetaData(DEPARTMENT_TABLE, EMPLOYEE_TABLE, true);
      assertNotNull(found, "Should find a join via exposed join disambiguation");
      assertEquals(DEPARTMENT_JOIN_EMPLOYEE, found.getName(), "Should resolve to the departmentEmployees join");
   }



   /*******************************************************************************
    ** AND input filter with security — verifies no wrapping occurs (the security
    ** sub-filter is appended directly to the existing AND filter).
    *******************************************************************************/
   @Test
   void testAndFilterWithSecurity() throws QException
   {
      QInstance instance = buildBaseInstance();
      addCompanySecurityLock(instance);
      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 1));

      QQueryFilter filter = new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Acme"));

      new JoinsContext(instance, COMPANY_TABLE, List.of(), filter);

      ///////////////////////////////////////////////////////////////////////////
      // the filter should remain AND (no wrapping needed as it was already AND) //
      ///////////////////////////////////////////////////////////////////////////
      assertEquals(QQueryFilter.BooleanOperator.AND, filter.getBooleanOperator());

      ///////////////////////////////////////////////
      // should still have the original criterion  //
      ///////////////////////////////////////////////
      assertThat(filter.getCriteria()).anyMatch(c -> c.getFieldName().equals("name"));

      /////////////////////////////////
      // plus a security sub-filter  //
      /////////////////////////////////
      assertThat(filter.getSubFilters()).isNotEmpty();
      QFilterCriteria securityCriteria = findSecurityCriteria(filter);
      assertNotNull(securityCriteria, "Should have security criteria");
      assertEquals(QCriteriaOperator.IN, securityCriteria.getOperator());
   }



   /*******************************************************************************
    ** resolveTableNameOrAliasToTableName with unknown name — verifies it returns
    ** the input as-is when the name is not in the alias map.
    *******************************************************************************/
   @Test
   void testResolveTableNameOrAliasNotFound() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(), new QQueryFilter());

      assertEquals("unknownAlias", joinsContext.resolveTableNameOrAliasToTableName("unknownAlias"),
         "Unknown name should be returned as-is");
   }



   /*******************************************************************************
    ** Security criteria placed in JOIN ON — when a user-added INNER join
    ** has a security lock, and the context is NOT an OR multi-lock, the
    ** lock criteria should be placed in the join's securityCriteria (JOIN ON)
    ** rather than the WHERE clause.
    *******************************************************************************/
   @Test
   void testSecurityCriteriaPlacedInJoinOn() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(DEPARTMENT_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT)));

      useInstance(instance);
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 99));

      ////////////////////////////////////////////////////////////////////////////
      // company as main table, user adds INNER join to department              //
      ////////////////////////////////////////////////////////////////////////////
      QueryJoin deptJoin = new QueryJoin()
         .withJoinTable(DEPARTMENT_TABLE)
         .withType(QueryJoin.Type.INNER)
         .withJoinMetaData(instance.getJoin(COMPANY_JOIN_DEPARTMENT));

      QQueryFilter filter       = new QQueryFilter();
      JoinsContext joinsContext = new JoinsContext(instance, COMPANY_TABLE, List.of(deptJoin), filter);

      ///////////////////////////////////////////////////////////////////////////////
      // find the query join that corresponds to the security lock chain walk.     //
      // The security criteria should be placed in JOIN ON (on the matching join), //
      // NOT in the WHERE clause filter.                                           //
      ///////////////////////////////////////////////////////////////////////////////
      boolean foundSecurityCriteriaOnJoin = joinsContext.getQueryJoins().stream()
         .anyMatch(qj -> qj.getSecurityCriteria() != null && !qj.getSecurityCriteria().isEmpty());
      assertTrue(foundSecurityCriteriaOnJoin,
         "Security criteria should be placed in JOIN ON (securityCriteria on QueryJoin), not in WHERE");
   }



   /*******************************************************************************
    ** findJoinMetaData with null baseTableName — verifies it finds any join to
    ** the target table that has its other side already in the query.
    *******************************************************************************/
   @Test
   void testFindJoinMetaDataNullBaseTable() throws QException
   {
      QInstance instance = buildBaseInstance();
      useInstance(instance);

      ///////////////////////////////////////////////////////////////////////////
      // employee as main table; search for employeeDetail with null base      //
      // — should find employee→employeeDetail since employee is the main table //
      ///////////////////////////////////////////////////////////////////////////
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), new QQueryFilter());

      QJoinMetaData found = joinsContext.findJoinMetaData(null, EMPLOYEE_DETAIL_TABLE, false);
      assertNotNull(found, "Should find a join to employeeDetail when baseTable is null and employee is main table");
   }



   /***************************************************************************
    ** Recursively search a filter tree for a QFilterCriteria that looks like
    ** a security criterion (operators: IN, IS_BLANK, IS_NULL_OR_IN, FALSE, TRUE).
    ***************************************************************************/
   private QFilterCriteria findSecurityCriteria(QQueryFilter filter)
   {
      if(filter == null)
      {
         return null;
      }

      for(QFilterCriteria criteria : filter.getCriteria())
      {
         if(isSecurityOperator(criteria.getOperator()))
         {
            return criteria;
         }
      }

      for(QQueryFilter subFilter : filter.getSubFilters())
      {
         QFilterCriteria found = findSecurityCriteria(subFilter);
         if(found != null)
         {
            return found;
         }
      }

      return null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private boolean isSecurityOperator(QCriteriaOperator operator)
   {
      return operator == QCriteriaOperator.IN
         || operator == QCriteriaOperator.IS_BLANK
         || operator == QCriteriaOperator.IS_NULL_OR_IN
         || operator == QCriteriaOperator.FALSE
         || operator == QCriteriaOperator.TRUE;
   }



   /***************************************************************************
    ** Recursively search a filter tree for a QFilterCriteria with the given
    ** operator.
    ***************************************************************************/
   private QFilterCriteria findCriteriaWithOperator(QQueryFilter filter, QCriteriaOperator operator)
   {
      if(filter == null)
      {
         return null;
      }

      for(QFilterCriteria criteria : filter.getCriteria())
      {
         if(criteria.getOperator() == operator)
         {
            return criteria;
         }
      }

      for(QQueryFilter subFilter : filter.getSubFilters())
      {
         QFilterCriteria found = findCriteriaWithOperator(subFilter, operator);
         if(found != null)
         {
            return found;
         }
      }

      return null;
   }



   /***************************************************************************
    ** Recursively check if a filter tree contains a sub-filter with the given
    ** boolean operator.
    ***************************************************************************/
   private boolean hasSubFilterWithOperator(QQueryFilter filter, QQueryFilter.BooleanOperator operator)
   {
      if(filter == null)
      {
         return false;
      }

      for(QQueryFilter sub : filter.getSubFilters())
      {
         if(sub.getBooleanOperator() == operator)
         {
            return true;
         }
         if(hasSubFilterWithOperator(sub, operator))
         {
            return true;
         }
      }

      return false;
   }



   /*******************************************************************************
    ** Verify that a 2-hop security lock chain produces correctly-oriented join
    ** metadata for both hops.  Before the fix in fillInMissingJoinMetaData, the
    ** second hop's metadata was double-flipped: it was correctly flipped during
    ** ensureRecordSecurityLockIsRepresented, then incorrectly flipped again by
    ** fillInMissingJoinMetaData because it couldn't see that the intermediate
    ** table was already in the query under an alias.
    **
    ** Scenario:
    ** - main table = employee
    ** - security lock on company.id via joinNameChain [companyDepartments, departmentEmployees]
    ** - reversed chain walks: employee → department (hop 1), department → company (hop 2)
    ** - hop 1 creates an aliased join like "department_forSecurityJoin_departmentEmployees"
    ** - hop 2's metadata should have leftTable=department, rightTable=company
    **   (i.e., still flipped from the original companyDepartments direction)
    **   but was getting double-flipped back to leftTable=company, rightTable=department
    *******************************************************************************/
   @Test
   void testTwoHopSecurityLockJoinMetaDataNotDoubleFlipped() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(EMPLOYEE_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)));

      useInstance(instance);

      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 42));
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), new QQueryFilter());

      List<QueryJoin> securityJoins = joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .toList();

      assertEquals(2, securityJoins.size(), "Should have 2 security joins for a 2-hop chain");

      //////////////////////////////////////////////////////////////////////////////////////////
      // Hop 1: employee → department.  The original departmentEmployees join has             //
      // left=department, right=employee, so it gets flipped: left=employee, right=department //
      //////////////////////////////////////////////////////////////////////////////////////////
      QueryJoin hop1 = securityJoins.get(0);
      QJoinMetaData hop1Meta = hop1.getJoinMetaData();
      assertNotNull(hop1Meta);
      assertEquals(EMPLOYEE_TABLE, hop1Meta.getLeftTable(),
         "Hop 1 left table should be employee (main table, flipped from original)");
      assertEquals(DEPARTMENT_TABLE, hop1Meta.getRightTable(),
         "Hop 1 right table should be department");

      /////////////////////////////////////////////////////////////////////////////////////////////
      // Hop 2: department → company.  The original companyDepartments join has                  //
      // left=company, right=department, so it gets flipped: left=department, right=company.     //
      // Before the fix, fillInMissingJoinMetaData would double-flip this back to the original   //
      // orientation (left=company, right=department), because it couldn't see that "department" //
      // was already in the query under the aliased hop-1 join.                                  //
      /////////////////////////////////////////////////////////////////////////////////////////////
      QueryJoin hop2 = securityJoins.get(1);
      QJoinMetaData hop2Meta = hop2.getJoinMetaData();
      assertNotNull(hop2Meta);
      assertEquals(DEPARTMENT_TABLE, hop2Meta.getLeftTable(),
         "Hop 2 left table should be department (flipped from original companyDepartments)");
      assertEquals(COMPANY_TABLE, hop2Meta.getRightTable(),
         "Hop 2 right table should be company");

      ////////////////////////////////////////////////////////////////////////////////////////////
      // Also verify the joinOn fields are oriented correctly after the (single) flip.          //
      // Original companyDepartments: JoinOn(id, companyId) meaning company.id = dept.companyId //
      // After flip: JoinOn(companyId, id) meaning dept.companyId = company.id                  //
      ////////////////////////////////////////////////////////////////////////////////////////////
      JoinOn hop2JoinOn = hop2Meta.getJoinOns().get(0);
      assertEquals("companyId", hop2JoinOn.getLeftField(),
         "After flip, left field should be companyId (department's FK)");
      assertEquals("id", hop2JoinOn.getRightField(),
         "After flip, right field should be id (company's PK)");
   }



   /*******************************************************************************
    ** Same 2-hop chain scenario, but verifying the security filter criteria are
    ** correctly placed.  This matters for RDBMS backends, where the security filter
    ** becomes part of the WHERE clause and must reference the correct alias.
    *******************************************************************************/
   @Test
   void testTwoHopSecurityLockFilterCriteria() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(EMPLOYEE_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE)));

      useInstance(instance);

      //////////////////////////////////////////////////////////////////////////////////
      // With a restricted session, the security criteria should be placed on the     //
      // last security join in the chain (the one closest to the company table).      //
      // For join-chain locks, criteria go on the QueryJoin's securityCriteria field, //
      // not in the main WHERE filter.                                                //
      //////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 42));
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), new QQueryFilter());

      List<QueryJoin> restrictedSecurityJoins = joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .toList();

      assertEquals(2, restrictedSecurityJoins.size());

      QueryJoin lastHop = restrictedSecurityJoins.get(restrictedSecurityJoins.size() - 1);
      assertFalse(lastHop.getSecurityCriteria().isEmpty(),
         "Last security join should have security criteria for restricted session");
      assertThat(lastHop.getSecurityCriteria().get(0).getFieldName()).contains("id");
      assertThat(lastHop.getSecurityCriteria().get(0).getValues()).contains(42);

      ///////////////////////////////////////////////////////////////////////////////////
      // now with all-access, verify the security joins are LEFT and have no criteria. //
      ///////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_COMPANY_ALL_ACCESS, true));
      JoinsContext allAccessContext = new JoinsContext(instance, EMPLOYEE_TABLE, List.of(), new QQueryFilter());

      List<QueryJoin> allAccessSecurityJoins = allAccessContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .toList();

      assertEquals(2, allAccessSecurityJoins.size());
      for(QueryJoin join : allAccessSecurityJoins)
      {
         assertEquals(QueryJoin.Type.LEFT, join.getType(),
            "All-access session should produce LEFT joins, not INNER");
         assertTrue(join.getSecurityCriteria().isEmpty(),
            "All-access session should have no security criteria");
      }
   }



   /*******************************************************************************
    ** 3-hop security lock chain: employeeDetail → employee → department → company.
    ** Ensures fillInMissingJoinMetaData handles even deeper chains correctly,
    ** where each hop's intermediate table is under a unique alias.
    *******************************************************************************/
   @Test
   void testThreeHopSecurityLockJoinMetaDataOrientation() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(EMPLOYEE_DETAIL_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT, DEPARTMENT_JOIN_EMPLOYEE, EMPLOYEE_JOIN_EMPLOYEE_DETAIL)));

      useInstance(instance);

      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 42));
      JoinsContext joinsContext = new JoinsContext(instance, EMPLOYEE_DETAIL_TABLE, List.of(), new QQueryFilter());

      List<QueryJoin> securityJoins = joinsContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .toList();

      assertEquals(3, securityJoins.size(), "Should have 3 security joins for a 3-hop chain");

      /////////////////////////////////////////////////////////////////////////////
      // Hop 1: employeeDetail → employee (flipped from employee→employeeDetail) //
      /////////////////////////////////////////////////////////////////////////////
      QJoinMetaData hop1Meta = securityJoins.get(0).getJoinMetaData();
      assertEquals(EMPLOYEE_DETAIL_TABLE, hop1Meta.getLeftTable());
      assertEquals(EMPLOYEE_TABLE, hop1Meta.getRightTable());

      /////////////////////////////////////////////////////////////////////
      // Hop 2: employee → department (flipped from department→employee) //
      /////////////////////////////////////////////////////////////////////
      QJoinMetaData hop2Meta = securityJoins.get(1).getJoinMetaData();
      assertEquals(EMPLOYEE_TABLE, hop2Meta.getLeftTable(),
         "Hop 2 left should be employee (not double-flipped back to department)");
      assertEquals(DEPARTMENT_TABLE, hop2Meta.getRightTable());

      ///////////////////////////////////////////////////////////////////
      // Hop 3: department → company (flipped from company→department) //
      ///////////////////////////////////////////////////////////////////
      QJoinMetaData hop3Meta = securityJoins.get(2).getJoinMetaData();
      assertEquals(DEPARTMENT_TABLE, hop3Meta.getLeftTable(),
         "Hop 3 left should be department (not double-flipped back to company)");
      assertEquals(COMPANY_TABLE, hop3Meta.getRightTable());
   }



   /*******************************************************************************
    ** Demonstrates the shared-mutable-state bug: when the same queryJoins list
    ** is passed to JoinsContext twice (as happens with ChildRecordListRenderer's
    ** widget metadata), ImplicitQueryJoinForSecurityLock objects from the first
    ** construction leak into the list and are reused by the second construction
    ** without re-evaluating security.
    **
    ** Scenario (mirrors the ColdTrack-Live lineItem widget bug):
    ** - department table has a companyKey security lock via joinNameChain
    ** - First JoinsContext: restricted session (companyKey = [42]) → adds an
    **   INNER security join with criteria companyId IN (42) to the shared list
    ** - Second JoinsContext: all-access session → should produce a LEFT join
    **   with no criteria, but finds the stale INNER join and reuses it
    *******************************************************************************/
   @Test
   void testSharedQueryJoinListIsNotMutatedBetweenConstructions() throws QException
   {
      QInstance instance = buildBaseInstance();
      instance.addSecurityKeyType(new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_COMPANY)
         .withAllAccessKeyName(SECURITY_KEY_COMPANY_ALL_ACCESS));

      instance.getTable(DEPARTMENT_TABLE).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_TYPE_COMPANY)
         .withFieldName("company.id")
         .withJoinNameChain(List.of(COMPANY_JOIN_DEPARTMENT)));

      useInstance(instance);

      /////////////////////////////////////////////////////////////////////////////////
      // Simulate a shared queryJoins list (like the one stored in widget metadata). //
      // It starts with a single user-defined join, analogous to the item LEFT JOIN  //
      // in the lineItems widget.                                                    //
      /////////////////////////////////////////////////////////////////////////////////
      QueryJoin userJoin = new QueryJoin()
         .withJoinTable(EMPLOYEE_TABLE)
         .withType(QueryJoin.Type.LEFT)
         .withSelect(true)
         .withJoinMetaData(instance.getJoin(DEPARTMENT_JOIN_EMPLOYEE));

      List<QueryJoin> sharedQueryJoins = new ArrayList<>(List.of(userJoin));
      int originalSize = sharedQueryJoins.size();

      //////////////////////////////////////////////////////////////////////////////
      // First construction: restricted session with specific company key values. //
      // This should NOT modify the shared list.                                  //
      //////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_TYPE_COMPANY, 42));
      new JoinsContext(instance, DEPARTMENT_TABLE, sharedQueryJoins, new QQueryFilter());

      assertEquals(originalSize, sharedQueryJoins.size(),
         "JoinsContext should not add security joins to the caller's queryJoins list");

      ///////////////////////////////////////////////////////////////////////////////
      // Second construction: all-access session.                                  //
      // Because the list was mutated above, the stale INNER join with companyId   //
      // IN (42) is found "already in the query" and reused without re-evaluation. //
      ///////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(SECURITY_KEY_COMPANY_ALL_ACCESS, true));
      JoinsContext allAccessContext = new JoinsContext(instance, DEPARTMENT_TABLE, sharedQueryJoins, new QQueryFilter());

      //////////////////////////////////////////////////////////////////////////////
      // With all-access, the security join should be LEFT (not INNER) and should //
      // have no security criteria on it.                                         //
      //////////////////////////////////////////////////////////////////////////////
      QueryJoin securityJoin = allAccessContext.getQueryJoins().stream()
         .filter(qj -> qj instanceof ImplicitQueryJoinForSecurityLock)
         .findFirst()
         .orElse(null);

      assertNotNull(securityJoin, "All-access context should still have a security join");
      assertEquals(QueryJoin.Type.LEFT, securityJoin.getType(),
         "All-access session should produce a LEFT join, not INNER");
      assertTrue(securityJoin.getSecurityCriteria().isEmpty(),
         "All-access session should have no security criteria on the join");
   }

}
