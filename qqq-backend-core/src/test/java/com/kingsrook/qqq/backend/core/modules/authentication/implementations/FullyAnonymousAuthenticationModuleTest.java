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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the FullyAnonymousAuthenticationModule
 *******************************************************************************/
public class FullyAnonymousAuthenticationModuleTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test()
   {
      FullyAnonymousAuthenticationModule fullyAnonymousAuthenticationModule = new FullyAnonymousAuthenticationModule();

      QSession session = fullyAnonymousAuthenticationModule.createSession(null, null);

      QSession restored = fullyAnonymousAuthenticationModule.createSession(null, Map.of("sessionId", session.getUuid()));
      assertEquals(session.getIdReference(), restored.getIdReference());
      assertNotNull(session, "Session should not be null");
      assertNotNull(session.getIdReference(), "Session id ref should not be null");
      assertNotNull(session.getUser(), "Session User should not be null");
      assertNotNull(session.getUser().getIdReference(), "Session User id ref should not be null");
      assertTrue(fullyAnonymousAuthenticationModule.isSessionValid(null, session), "Any session should be valid");
      assertFalse(fullyAnonymousAuthenticationModule.isSessionValid(null, null), "null should be not valid");
   }

}
