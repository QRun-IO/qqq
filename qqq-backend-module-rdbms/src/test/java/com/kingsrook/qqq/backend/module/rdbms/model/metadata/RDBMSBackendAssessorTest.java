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

package com.kingsrook.qqq.backend.module.rdbms.model.metadata;


import java.sql.Connection;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.assessment.QInstanceAssessor;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/*******************************************************************************
 ** Unit test for RDBMSBackendAssessor 
 *******************************************************************************/
class RDBMSBackendAssessorTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSBackendAssessorTest.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccess() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");
      QInstanceAssessor assessor = new QInstanceAssessor(QContext.getQInstance());
      assessor.assess();
      System.out.println(assessor.getSummary());
      assertEquals(0, assessor.getErrors().size());
      assertEquals(0, assessor.getWarnings().size());
      assertEquals(0, assessor.getExitCode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableIssues() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////////////
      // start from primed database, but make a few alters to it and the meta-data //
      ///////////////////////////////////////////////////////////////////////////////
      TestUtils.primeTestDatabase("prime-test-database.sql");
      ConnectionManager connectionManager = new ConnectionManager();
      try(Connection connection = connectionManager.getConnection(TestUtils.defineBackend()))
      {
         QueryManager.executeUpdate(connection, "ALTER TABLE person ADD COLUMN suffix VARCHAR(20)");
         QueryManager.executeUpdate(connection, "ALTER TABLE person ADD UNIQUE u_name (first_name, last_name)");
      }

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withField(new QFieldMetaData("middleName", QFieldType.STRING))
         .withUniqueKey(new UniqueKey("firstName", "middleName", "lastName"));

      ///////////////////////////
      // un-prime the database //
      ///////////////////////////
      QInstanceAssessor assessor = new QInstanceAssessor(QContext.getQInstance());
      assessor.assess();
      LOG.info(assessor.getSummary());
      assertNotEquals(0, assessor.getErrors().size());
      assertNotEquals(0, assessor.getExitCode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTotalFailure() throws Exception
   {
      ///////////////////////////
      // un-prime the database //
      ///////////////////////////
      TestUtils.primeTestDatabase("drop-test-database.sql");
      QInstanceAssessor assessor = new QInstanceAssessor(QContext.getQInstance());
      assessor.assess();
      System.out.println(assessor.getSummary());
      assertNotEquals(0, assessor.getErrors().size());
      assertNotEquals(0, assessor.getExitCode());
   }

}