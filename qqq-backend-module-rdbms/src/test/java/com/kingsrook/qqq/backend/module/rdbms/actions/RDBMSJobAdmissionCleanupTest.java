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


import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Faulting JDBC setup cannot leak a statement before try-with-resources owns it.
 ** These are resource-lifecycle tests, not provider durability qualification.
 *******************************************************************************/
class RDBMSJobAdmissionCleanupTest
{
   /*******************************************************************************
    ** A driver refusing the deadline still has its allocated statement closed.
    *******************************************************************************/
   @Test
   void closesStatementWhenTimeoutSetupFails()
   {
      SQLException setup = new SQLException("Injected timeout setup failure");
      AtomicBoolean closed = new AtomicBoolean();
      QException failure = assertThrows(QException.class, () -> RDBMSJobAdmission.now(connection(setup, null, closed)));
      assertSame(setup, failure.getCause());
      assertTrue(closed.get());
      assertEquals(0, setup.getSuppressed().length);
   }



   /*******************************************************************************
    ** A second close failure cannot obscure the original deadline refusal.
    *******************************************************************************/
   @Test
   void preservesSetupFailureAndSuppressedCloseFailure()
   {
      SQLException setup = new SQLException("Injected timeout setup failure");
      SQLException close = new SQLException("Injected statement close failure");
      AtomicBoolean closed = new AtomicBoolean();
      QException failure = assertThrows(QException.class, () -> RDBMSJobAdmission.now(connection(setup, close, closed)));
      assertSame(setup, failure.getCause());
      assertTrue(closed.get());
      assertEquals(1, setup.getSuppressed().length);
      assertSame(close, setup.getSuppressed()[0]);
   }



   /*******************************************************************************
    ** Minimal JDBC fault injection exercises the production database-clock seam.
    *******************************************************************************/
   private Connection connection(SQLException setup, SQLException close, AtomicBoolean closed)
   {
      PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {PreparedStatement.class}, (proxy, method, args) ->
      {
         if("setQueryTimeout".equals(method.getName()))
         {
            throw (setup);
         }
         if("close".equals(method.getName()))
         {
            closed.set(true);
            if(close != null)
            {
               throw (close);
            }
            return (null);
         }
         throw (new UnsupportedOperationException(method.getName()));
      });
      return ((Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) ->
      {
         if("prepareStatement".equals(method.getName()))
         {
            return (statement);
         }
         throw (new UnsupportedOperationException(method.getName()));
      }));
   }
}
