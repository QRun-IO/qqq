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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for the identity-token lifecycle of C3P0PooledConnectionProvider:
 ** close is idempotent (even after the token was re-used by a newer pool), and a
 ** failed (partial) init leaves nothing held.
 *******************************************************************************/
class C3P0PooledConnectionProviderLifecycleTest extends BaseTest
{

   /*******************************************************************************
    ** a second close of a provider - after its token was handed to a newer pool -
    ** must not release the newer pool's token.
    *******************************************************************************/
   @Test
   void testCloseIsIdempotentWhenTokenWasReused() throws QException
   {
      RDBMSBackendMetaData backend = backend();

      C3P0PooledConnectionProvider first = new C3P0PooledConnectionProvider();
      first.init(backend);
      assertEquals(backend.getName(), first.getIdentityToken());
      first.close();

      ///////////////////////////////////////////////////////////////////////
      // the released token is re-used by the next pool...                 //
      ///////////////////////////////////////////////////////////////////////
      C3P0PooledConnectionProvider second = new C3P0PooledConnectionProvider();
      second.init(backend);
      assertEquals(backend.getName(), second.getIdentityToken());

      /////////////////////////////////////////////////////////////////////////////
      // ...so a repeat close of the first (e.g., the Cleaner) must do nothing   //
      /////////////////////////////////////////////////////////////////////////////
      first.close();
      first.close();

      C3P0PooledConnectionProvider third = new C3P0PooledConnectionProvider();
      third.init(backend);
      assertEquals(backend.getName() + "#2", third.getIdentityToken());

      second.close();
      second.close();
      third.close();
      third.close();

      C3P0PooledConnectionProvider fourth = new C3P0PooledConnectionProvider();
      fourth.init(backend);
      assertEquals(backend.getName(), fourth.getIdentityToken());
      fourth.close();
   }



   /*******************************************************************************
    ** an init that fails part way (after the pool object is made) releases its
    ** token, holds nothing, and can be closed (a no-op) safely.
    *******************************************************************************/
   @Test
   void testPartialInitFailureCleansUp() throws QException
   {
      RDBMSBackendMetaData bad = backend();
      bad.setVendor("no-such-vendor");
      bad.setJdbcDriverClassName(null);

      C3P0PooledConnectionProvider failed = new C3P0PooledConnectionProvider();
      assertThrows(QException.class, () -> failed.init(bad));
      assertNull(failed.getIdentityToken());
      failed.close();
      failed.close();

      /////////////////////////////////////////////////////////////////////////////////
      // the name's token was released by the failure - and wasn't released twice   //
      /////////////////////////////////////////////////////////////////////////////////
      RDBMSBackendMetaData         good   = backend().withName(bad.getName());
      C3P0PooledConnectionProvider first  = new C3P0PooledConnectionProvider();
      C3P0PooledConnectionProvider second = new C3P0PooledConnectionProvider();
      first.init(good);
      second.init(good);
      assertEquals(good.getName(), first.getIdentityToken());
      assertEquals(good.getName() + "#2", second.getIdentityToken());
      first.close();
      second.close();

      ////////////////////////////////////////////////////////
      // and a provider that was never initialized is fine  //
      ////////////////////////////////////////////////////////
      new C3P0PooledConnectionProvider().close();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RDBMSBackendMetaData backend()
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) TestUtils.defineInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setJdbcUrl("jdbc:h2:mem:c3p0_lifecycle;MODE=MySQL;DB_CLOSE_DELAY=-1");

      /////////////////////////////////////////////////////////////////////////////////////
      // a name no other test uses - the identity tokens are static, for the whole JVM //
      /////////////////////////////////////////////////////////////////////////////////////
      backend.withName("c3p0_lifecycle_" + java.util.UUID.randomUUID().toString().replace("-", ""));
      return (backend);
   }

}
