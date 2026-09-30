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

package com.kingsrook.qqq.backend.core.modules.authentication;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.NotImplementedException;



/*******************************************************************************
 ** Interface that a QAuthenticationModule must implement.
 **
 *******************************************************************************/
public interface QAuthenticationModuleInterface
{
   /*******************************************************************************
    **
    *******************************************************************************/
   QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException;


   /*******************************************************************************
    **
    *******************************************************************************/
   boolean isSessionValid(QInstance instance, QSession session);


   /*******************************************************************************
    **
    *******************************************************************************/
   default QSession createAutomatedSessionForUser(QInstance qInstance, Serializable userId) throws QAuthenticationException
   {
      String ownerId = ValueUtils.getValueAsString(userId);
      if(!StringUtils.hasContent(ownerId))
      {
         throw new QAuthenticationException("An automated session requires a user ID.");
      }

      QSession session = new QSession().withUser(new QUser().withIdReference(ownerId));
      Map<String, Serializable> backendVariants = QContext.getQSession().getBackendVariants();
      if(backendVariants != null)
      {
         session.setBackendVariants(new HashMap<>(backendVariants));
      }
      return session;
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default boolean usesSessionIdCookie()
   {
      return (false);
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default String getLoginRedirectUrl(String originalUrl) throws QAuthenticationException
   {
      throw (new NotImplementedException("The method getLoginRedirectUrl() is not implemented in the authentication module: " + this.getClass().getSimpleName()));
   }



   /***************************************************************************
    ** Logout a session, invalidating it server-side.
    **
    ** @param qInstance the QInstance (provided for implementations that need it)
    ** @param sessionUUID the session UUID to invalidate
    ***************************************************************************/
   default void logout(QInstance qInstance, String sessionUUID)
   {
      ///////////////////////////////////////////////////////////////////////////
      // default implementation is a no-op - modules may override if they need //
      // to clear caches or delete session records. qInstance is part of the   //
      // interface contract for implementations that require instance context. //
      ///////////////////////////////////////////////////////////////////////////
   }

}
