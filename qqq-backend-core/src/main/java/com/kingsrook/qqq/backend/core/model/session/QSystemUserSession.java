/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.session;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Special session, indicating that an action being executed is being done not
 ** on behalf of a (human or otherwise) user - but instead, is the application/
 ** system itself.
 **
 ** Generally this means, escalated privileges - e.g., permission to all tables,
 ** processes etc, and all security keys (e.g., all-access keys).
 *******************************************************************************/
public class QSystemUserSession extends QSession
{
   private static final QLogger LOG = QLogger.getLogger(QSystemUserSession.class);



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QSystemUserSession()
   {
      this(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Create a system session before its instance has been installed in QContext.
    ** Read the key types for each new session: instances can be replaced, and
    ** their security metadata can change while an application is being built.
    *******************************************************************************/
   public QSystemUserSession(QInstance qInstance)
   {
      super();
      if(qInstance == null)
      {
         LOG.warn("QInstance was not supplied when creating a QSystemUserSession. This SystemUserSession will NOT have any allAccessKeys.");
         return;
      }

      ////////////////////////////////////////////////////////
      // always give system user all of the all-access keys //
      ////////////////////////////////////////////////////////
      for(QSecurityKeyType securityKeyType : qInstance.getSecurityKeyTypes().values())
      {
         if(StringUtils.hasContent(securityKeyType.getAllAccessKeyName()))
         {
            withSecurityKeyValue(securityKeyType.getAllAccessKeyName(), true);
         }
      }
   }



   /*******************************************************************************
    ** System User Sessions should always have permission to all the things.
    *******************************************************************************/
   @Override
   public boolean hasPermission(String permissionName)
   {
      return (true);
   }

}
