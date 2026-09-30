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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSCountActionTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUnfilteredCount() throws QException
   {
      CountInput  countInput  = initCountRequest();
      CountOutput countOutput = new RDBMSCountAction().execute(countInput);
      assertEquals(5, countOutput.getCount(), "Unfiltered query should find all rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testEqualsQueryCount() throws QException
   {
      String email = "darin.kelkhoff@gmail.com";

      CountInput countInput = initCountRequest();
      countInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.EQUALS)
            .withValues(List.of(email)))
      );
      CountOutput countOutput = new RDBMSCountAction().execute(countInput);
      assertEquals(1, countOutput.getCount(), "Expected # of rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotEqualsQuery() throws QException
   {
      String email = "darin.kelkhoff@gmail.com";

      CountInput countInput = initCountRequest();
      countInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.NOT_EQUALS)
            .withValues(List.of(email)))
      );
      CountOutput countOutput = new RDBMSCountAction().execute(countInput);
      assertEquals(4, countOutput.getCount(), "Expected # of rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CountInput initCountRequest()
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(TestUtils.defineTablePerson().getName());
      return countInput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneInnerJoinWithoutWhere() throws QException
   {
      CountInput countInput = initCountRequest();
      countInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSON, TestUtils.TABLE_NAME_PERSONAL_ID_CARD));
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(3, countOutput.getCount(), "Join count should find 3 rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneLeftJoinWithoutWhere() throws QException
   {
      CountInput countInput = initCountRequest();
      countInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSON, TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withType(QueryJoin.Type.LEFT));
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(5, countOutput.getCount(), "Left Join count should find 5 rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneRightJoinWithoutWhere() throws QException
   {
      CountInput countInput = initCountRequest();
      countInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSON, TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withType(QueryJoin.Type.RIGHT));
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(6, countOutput.getCount(), "Right Join count should find 6 rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneInnerJoinWithWhere() throws QException
   {
      CountInput countInput = initCountRequest();
      countInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSON, TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withSelect(true));
      countInput.setFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber", QCriteriaOperator.STARTS_WITH, "1980")));
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(2, countOutput.getCount(), "Right Join count should find 2 rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurity() throws QException
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      QContext.setQSession(new QSession());
      assertThat(new CountAction().execute(countInput).getCount()).isEqualTo(0);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new CountAction().execute(countInput).getCount()).isEqualTo(8);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new CountAction().execute(countInput).getCount()).isEqualTo(5);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityWithLockFromJoinTableWhereTheKeyIsOnTheManySide() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      CountInput countInput = new CountInput();
      countInput.setTableName(TestUtils.TABLE_NAME_WAREHOUSE);

      assertThat(new CountAction().execute(countInput).getCount()).isEqualTo(4);
   }

}
