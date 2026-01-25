/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.authentication;


import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for QSessionStoreHelper and QSessionStoreRegistry.
 *******************************************************************************/
class QSessionStoreHelperTest extends BaseTest
{

   @AfterEach
   void tearDown()
   {
      ////////////////////////////////////////////////////
      // Clear the registry after each test to isolate  //
      ////////////////////////////////////////////////////
      QSessionStoreRegistry.getInstance().clear();
   }



   /*******************************************************************************
    ** Test that session store is not available when no provider is registered.
    *******************************************************************************/
   @Test
   void testIsSessionStoreAvailable_returnsFalseWhenNoProviderRegistered()
   {
      assertThat(QSessionStoreHelper.isSessionStoreAvailable()).isFalse();
   }



   /*******************************************************************************
    ** Test that session store is available when provider is registered.
    *******************************************************************************/
   @Test
   void testIsSessionStoreAvailable_returnsTrueWhenProviderRegistered()
   {
      QSessionStoreRegistry.getInstance().register(new TestSessionStoreProvider());
      assertThat(QSessionStoreHelper.isSessionStoreAvailable()).isTrue();
   }



   /*******************************************************************************
    ** Test that load returns empty Optional when no provider is registered.
    *******************************************************************************/
   @Test
   void testLoadSession_returnsEmptyWhenNotAvailable()
   {
      Optional<QSession> result = QSessionStoreHelper.loadSession("test-uuid");
      assertThat(result).isEmpty();
   }



   /*******************************************************************************
    ** Test that store is a no-op when no provider is registered.
    *******************************************************************************/
   @Test
   void testStoreSession_isNoOpWhenNotAvailable()
   {
      ///////////////////////////////////////////
      // should not throw, just silently no-op //
      ///////////////////////////////////////////
      QSession session = new QSession();
      session.setUuid("test-uuid");
      QSessionStoreHelper.storeSession("test-uuid", session, Duration.ofHours(1));
   }



   /*******************************************************************************
    ** Test that touch is a no-op when no provider is registered.
    *******************************************************************************/
   @Test
   void testTouchSession_isNoOpWhenNotAvailable()
   {
      ///////////////////////////////////////////
      // should not throw, just silently no-op //
      ///////////////////////////////////////////
      QSessionStoreHelper.touchSession("test-uuid");
   }



   /*******************************************************************************
    ** Test that getDefaultTtl returns 1 hour fallback when no provider is registered.
    *******************************************************************************/
   @Test
   void testGetDefaultTtl_returnsOneHourFallbackWhenNotAvailable()
   {
      Duration ttl = QSessionStoreHelper.getDefaultTtl();
      assertThat(ttl).isEqualTo(Duration.ofHours(1));
   }



   /*******************************************************************************
    ** Test that loadAndTouchSession returns empty when no provider is registered.
    *******************************************************************************/
   @Test
   void testLoadAndTouchSession_returnsEmptyWhenNotAvailable()
   {
      Optional<QSession> result = QSessionStoreHelper.loadAndTouchSession("test-uuid");
      assertThat(result).isEmpty();
   }



   /*******************************************************************************
    ** Test store and load with a registered provider.
    *******************************************************************************/
   @Test
   void testStoreAndLoad_withProvider()
   {
      TestSessionStoreProvider provider = new TestSessionStoreProvider();
      QSessionStoreRegistry.getInstance().register(provider);

      QSession session = new QSession();
      session.setUuid("test-uuid");

      QSessionStoreHelper.storeSession("test-uuid", session, Duration.ofHours(1));

      Optional<QSession> result = QSessionStoreHelper.loadSession("test-uuid");
      assertThat(result).isPresent();
      assertThat(result.get().getUuid()).isEqualTo("test-uuid");
   }



   /*******************************************************************************
    ** Test loadAndTouchSession with a registered provider.
    *******************************************************************************/
   @Test
   void testLoadAndTouchSession_withProvider()
   {
      TestSessionStoreProvider provider = new TestSessionStoreProvider();
      QSessionStoreRegistry.getInstance().register(provider);

      QSession session = new QSession();
      session.setUuid("test-uuid");

      QSessionStoreHelper.storeSession("test-uuid", session, Duration.ofHours(1));

      Optional<QSession> result = QSessionStoreHelper.loadAndTouchSession("test-uuid");
      assertThat(result).isPresent();
      assertThat(result.get().getUuid()).isEqualTo("test-uuid");
      assertThat(provider.touchCount).isEqualTo(1);
   }



   /*******************************************************************************
    ** Test that getDefaultTtl returns provider's TTL when registered.
    *******************************************************************************/
   @Test
   void testGetDefaultTtl_withProvider()
   {
      TestSessionStoreProvider provider = new TestSessionStoreProvider();
      QSessionStoreRegistry.getInstance().register(provider);

      Duration ttl = QSessionStoreHelper.getDefaultTtl();
      assertThat(ttl).isEqualTo(Duration.ofMinutes(30));
   }



   /*******************************************************************************
    ** Test that registry replaces existing provider with warning.
    *******************************************************************************/
   @Test
   void testRegistry_replacesProvider()
   {
      TestSessionStoreProvider provider1 = new TestSessionStoreProvider();
      TestSessionStoreProvider provider2 = new TestSessionStoreProvider();

      QSessionStoreRegistry.getInstance().register(provider1);
      QSessionStoreRegistry.getInstance().register(provider2);

      assertThat(QSessionStoreRegistry.getInstance().getProvider()).contains(provider2);
   }



   /*******************************************************************************
    ** Test session store provider for testing purposes.
    *******************************************************************************/
   private static class TestSessionStoreProvider implements QSessionStoreProviderInterface
   {
      private final ConcurrentHashMap<String, QSession> sessions = new ConcurrentHashMap<>();
      int touchCount = 0;



      @Override
      public void store(String sessionUuid, QSession session, Duration ttl)
      {
         sessions.put(sessionUuid, session);
      }



      @Override
      public Optional<QSession> load(String sessionUuid)
      {
         return Optional.ofNullable(sessions.get(sessionUuid));
      }



      @Override
      public void remove(String sessionUuid)
      {
         sessions.remove(sessionUuid);
      }



      @Override
      public void touch(String sessionUuid)
      {
         touchCount++;
      }



      @Override
      public Duration getDefaultTtl()
      {
         return Duration.ofMinutes(30);
      }
   }

}
