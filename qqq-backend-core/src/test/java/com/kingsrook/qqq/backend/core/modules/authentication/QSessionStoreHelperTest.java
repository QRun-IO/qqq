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
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Tests for QSessionStoreHelper.
 *******************************************************************************/
class QSessionStoreHelperTest
{

   /***************************************************************************
    ** Test that session store is not available when QBit is not on classpath.
    ***************************************************************************/
   @Test
   void testSessionStoreNotAvailable()
   {
      //////////////////////////////////////////////////////////////////////////
      // without the qbit-session-store dependency, the store is unavailable //
      //////////////////////////////////////////////////////////////////////////
      assertThat(QSessionStoreHelper.isSessionStoreAvailable()).isFalse();
   }



   /***************************************************************************
    ** Test that load returns empty when store is not available.
    ***************************************************************************/
   @Test
   void testLoadReturnsEmptyWhenNotAvailable()
   {
      Optional<QSession> result = QSessionStoreHelper.loadSession("test-uuid");
      assertThat(result).isEmpty();
   }



   /***************************************************************************
    ** Test that store is a no-op when not available.
    ***************************************************************************/
   @Test
   void testStoreIsNoOpWhenNotAvailable()
   {
      ///////////////////////////////////////////
      // should not throw, just silently no-op //
      ///////////////////////////////////////////
      QSession session = new QSession();
      session.setUuid("test-uuid");
      QSessionStoreHelper.storeSession("test-uuid", session, Duration.ofHours(1));
   }



   /***************************************************************************
    ** Test that touch is a no-op when not available.
    ***************************************************************************/
   @Test
   void testTouchIsNoOpWhenNotAvailable()
   {
      ///////////////////////////////////////////
      // should not throw, just silently no-op //
      ///////////////////////////////////////////
      QSessionStoreHelper.touchSession("test-uuid");
   }



   /***************************************************************************
    ** Test that getDefaultTtl returns 1 hour when not available.
    ***************************************************************************/
   @Test
   void testGetDefaultTtlReturnsOneHourWhenNotAvailable()
   {
      Duration ttl = QSessionStoreHelper.getDefaultTtl();
      assertThat(ttl).isEqualTo(Duration.ofHours(1));
   }

}
