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

package com.kingsrook.qqq.middleware.javalin;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecution;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListener;
import com.kingsrook.qqq.backend.core.instrumentation.StatementExecutionListeners;
import com.kingsrook.qqq.backend.core.instrumentation.StatementOperation;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWork;
import com.kingsrook.qqq.backend.core.instrumentation.UnitOfWorkKind;
import io.javalin.Javalin;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Each HTTP request is one unit of work named by its route template.
 *******************************************************************************/
class UnitOfWorkJavalinHandlersTest
{
   private static final int PORT = 6297;

   private Javalin service;



   /*******************************************************************************
    ** Stop the server and the listeners.
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      if(service != null)
      {
         service.stop();
      }
      StatementExecutionListeners.clear();
   }



   /*******************************************************************************
    ** Statements in a request carry its unit; the ended unit has the template.
    *******************************************************************************/
   @Test
   void testRequestIsUnitOfWork()
   {
      List<StatementExecution> statements = Collections.synchronizedList(new ArrayList<>());
      List<UnitOfWork>         ended      = Collections.synchronizedList(new ArrayList<>());
      StatementExecutionListeners.register(new StatementExecutionListener()
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
      });

      service = Javalin.create(config ->
      {
         config.routes.get("/things/{id}", context ->
         {
            StatementExecutionListeners.start("rdbms", "thing", StatementOperation.QUERY).succeeded(1).finish("SELECT * FROM thing WHERE id = ?");
            context.result("ok");
         });
         config.routes.before(UnitOfWorkJavalinHandlers::before);
         config.routes.after(UnitOfWorkJavalinHandlers::after);
      }).start(PORT);

      assertThat(Unirest.get("http://localhost:" + PORT + "/things/42").asString().getBody()).isEqualTo("ok");
      Unirest.get("http://localhost:" + PORT + "/nothing-here").asString();

      assertThat(statements).hasSize(1);
      assertThat(statements.get(0).unitOfWork().getKind()).isEqualTo(UnitOfWorkKind.HTTP_REQUEST);
      assertThat(ended).extracting(UnitOfWork::getName).contains("GET /things/{id}", "GET (unmatched)");
   }



   /*******************************************************************************
    ** Without listeners the handlers do nothing.
    *******************************************************************************/
   @Test
   void testInactiveDoesNothing()
   {
      service = Javalin.create(config ->
      {
         config.routes.get("/x", context -> context.result("x"));
         config.routes.before(UnitOfWorkJavalinHandlers::before);
         config.routes.after(UnitOfWorkJavalinHandlers::after);
      }).start(PORT);
      assertThat(Unirest.get("http://localhost:" + PORT + "/x").asString().getBody()).isEqualTo("x");
   }
}
