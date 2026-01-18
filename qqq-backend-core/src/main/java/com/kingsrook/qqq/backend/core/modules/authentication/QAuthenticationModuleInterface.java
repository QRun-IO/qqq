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

package com.kingsrook.qqq.backend.core.modules.authentication;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
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
      try
      {
         QSession clone = QContext.getQSession().clone();
         if(clone.getUser() != null)
         {
            clone.getUser().setIdReference(ValueUtils.getValueAsString(userId));
         }
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw (new QAuthenticationException("Cloning session failed", e));
      }
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
