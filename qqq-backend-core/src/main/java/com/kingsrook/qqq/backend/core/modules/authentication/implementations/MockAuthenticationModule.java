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


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class MockAuthenticationModule implements QAuthenticationModuleInterface
{
   private static final QLogger logger         = QLogger.getLogger(MockAuthenticationModule.class);
   private static final int     USER_ID_MODULO = 10_000;

   //////////////////////////////////////////////////////////////////////////////////
   // do not use this var directly - rather - always call the getCustomizer method //
   //////////////////////////////////////////////////////////////////////////////////
   private QAuthenticationModuleCustomizerInterface _customizer                = null;
   private boolean                                  customizerHasBeenRequested = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
   {
      if("Deny".equalsIgnoreCase(context.get("accessToken")))
      {
         throw (new QAuthenticationException("Access denied (per accessToken requesting as such)"));
      }

      QUser qUser = new QUser();
      qUser.setIdReference("User:" + (System.currentTimeMillis() % USER_ID_MODULO));
      qUser.setFullName("John Smith");

      QSession qSession = new QSession();
      if(StringUtils.hasContent(context.get("sessionId")))
      {
         qSession.setUuid(context.get("sessionId"));
      }
      qSession.setIdReference(qSession.getUuid());
      qSession.setUser(qUser);

      if(getCustomizer() != null)
      {
         getCustomizer().customizeSession(qInstance, qSession, Map.of());
      }

      return (qSession);
   }



   /*******************************************************************************
    ** Establish automation grants through the owner's configured session customizer.
    *******************************************************************************/
   @Override
   public QSession createAutomatedSessionForUser(QInstance qInstance, Serializable userId) throws QAuthenticationException
   {
      QSession session = QAuthenticationModuleInterface.super.createAutomatedSessionForUser(qInstance, userId);
      if(getCustomizer() != null)
      {
         getCustomizer().customizeAutomatedSessionForUser(qInstance, session, userId);
      }
      return session;
   }



   /*******************************************************************************
    ** Keep development sessions stable across browser requests.
    *******************************************************************************/
   @Override
   public boolean usesSessionIdCookie()
   {
      return true;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isSessionValid(QInstance instance, QSession session)
   {
      if(session == null)
      {
         logger.info("Session is null, which is not valid.");
         return (false);
      }

      if(session.getValue("isInvalid") != null)
      {
         logger.info("Session contains the valid 'isInvalid', which is not valid.");
         return (false);
      }

      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getLoginRedirectUrl(String originalUrl)
   {
      return originalUrl + "?createMockSession=true";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QAuthenticationModuleCustomizerInterface getCustomizer()
   {
      try
      {
         if(!customizerHasBeenRequested)
         {
            customizerHasBeenRequested = true;

            QAuthenticationMetaData metaData = QContext.getQInstance().getAuthentication();

            if(metaData.getCustomizer() != null)
            {
               _customizer = QCodeLoader.getAdHoc(QAuthenticationModuleCustomizerInterface.class, metaData.getCustomizer());
            }
         }

         return (_customizer);
      }
      catch(Exception e)
      {
         logger.warn("Error getting customizer.", e);
         return (null);
      }
   }

}
