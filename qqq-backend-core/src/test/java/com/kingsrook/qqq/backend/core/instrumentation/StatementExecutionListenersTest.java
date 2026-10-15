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

package com.kingsrook.qqq.backend.core.instrumentation;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for StatementExecutionListeners, StatementTimer and UnitsOfWork.
 *******************************************************************************/
class StatementExecutionListenersTest extends BaseTest
{

   /*******************************************************************************
    ** Leave no listener behind.
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      StatementExecutionListeners.clear();
      UnitsOfWork.reset();
   }



   /*******************************************************************************
    ** With no listener, timers and scopes are shared no-ops.
    *******************************************************************************/
   @Test
   void testInactiveIsNoop()
   {
      assertThat(StatementExecutionListeners.isActive()).isFalse();
      assertThat(StatementExecutionListeners.start("b", "t", StatementOperation.QUERY)).isSameAs(StatementTimer.NOOP);
      StatementTimer.NOOP.succeeded(1).finish("select 1");
      try(UnitsOfWork.Scope scope = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "p"))
      {
         assertThat(scope.getUnitOfWork()).isNull();
         assertThat(UnitsOfWork.current()).isNull();
      }
   }



   /*******************************************************************************
    ** Statements carry the outermost unit and process; only the outermost end
    ** is reported; a failing listener does not stop the others.
    *******************************************************************************/
   @Test
   void testEventsUnitsAndNesting()
   {
      List<StatementExecution> statements = new ArrayList<>();
      List<UnitOfWork>         ended      = new ArrayList<>();
      StatementExecutionListeners.register(execution ->
      {
         throw (new IllegalStateException("broken listener"));
      });
      StatementExecutionListener recorder = new StatementExecutionListener()
      {
         @Override
         public void onStatement(StatementExecution execution)
         {
            statements.add(execution);
         }



         @Override
         public void onUnitOfWorkEnd(UnitOfWork unitOfWork)
         {
            ended.add(unitOfWork);
         }
      };
      StatementExecutionListeners.register(recorder);
      StatementExecutionListeners.register(recorder);
      assertThat(StatementExecutionListeners.isActive()).isTrue();

      QContext.pushAction(new RunProcessInput().withProcessName("myProcess"));
      try(UnitsOfWork.Scope outer = UnitsOfWork.begin(UnitOfWorkKind.HTTP_REQUEST, "GET (unmatched)"))
      {
         try(UnitsOfWork.Scope inner = UnitsOfWork.begin(UnitOfWorkKind.PROCESS, "myProcess"))
         {
            assertThat(inner.getUnitOfWork()).isSameAs(outer.getUnitOfWork());
            StatementExecutionListeners.start("rdbms", "person", StatementOperation.QUERY).succeeded(3).finish("SELECT * FROM person WHERE id = ?");
         }
         assertThat(ended).isEmpty();

         StatementTimer failed = StatementExecutionListeners.start("rdbms", "person", StatementOperation.DELETE);
         failed.finish("DELETE FROM person WHERE id = ?");
         failed.finish("again: reported once");
         outer.getUnitOfWork().setName("GET /qqq/v1/table/{tableName}/query");
      }
      QContext.popAction();

      assertThat(statements).hasSize(2);
      StatementExecution first = statements.get(0);
      assertThat(first.backendName()).isEqualTo("rdbms");
      assertThat(first.tableName()).isEqualTo("person");
      assertThat(first.operation()).isEqualTo(StatementOperation.QUERY);
      assertThat(first.rowCount()).isEqualTo(3);
      assertThat(first.succeeded()).isTrue();
      assertThat(first.elapsedNanos()).isGreaterThanOrEqualTo(0);
      assertThat(first.processName()).isEqualTo("myProcess");
      assertThat(first.unitOfWork().getKind()).isEqualTo(UnitOfWorkKind.HTTP_REQUEST);
      assertThat(statements.get(1).succeeded()).isFalse();
      assertThat(statements.get(1).rowCount()).isNull();

      assertThat(ended).hasSize(1);
      assertThat(ended.get(0).getName()).isEqualTo("GET /qqq/v1/table/{tableName}/query");
      assertThat(ended.get(0).toString()).contains("HTTP_REQUEST");
      assertThat(UnitsOfWork.current()).isNull();

      StatementExecutionListeners.unregister(recorder);
      assertThat(StatementExecutionListeners.isActive()).isTrue();
   }
}
