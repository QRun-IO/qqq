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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;


/*******************************************************************************
 ** Authenticate automated deliveries without borrowing another user's authority.
 *******************************************************************************/
public final class DeliverySessions
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DeliverySessions()
   {
   }



   /*******************************************************************************
    ** Rebuild and validate the owner, as scheduled reports do.
    *******************************************************************************/
   public static QSession forOwner(String ownerUserId) throws QException
   {
      QSession current = QContext.getQSession();
      if(ownerUserId == null || ownerUserId.isBlank() || current == null)
      {
         throw new QPermissionDeniedException("Delivery requires an owner session.");
      }
      if(!(current instanceof QSystemUserSession))
      {
         if(current.getUser() == null || !Objects.equals(ownerUserId, current.getUser().getIdReference()))
         {
            throw new QPermissionDeniedException("Delivery must run as its owner.");
         }
         return (current);
      }
      QInstance instance = QContext.getQInstance();
      QAuthenticationModuleInterface authentication = new QAuthenticationModuleDispatcher().getQModule(instance.getAuthentication());
      QSession owner = authentication.createAutomatedSessionForUser(instance, ownerUserId);
      if(owner == null || owner instanceof QSystemUserSession || owner.getUser() == null
         || !Objects.equals(ownerUserId, owner.getUser().getIdReference()) || !authentication.isSessionValid(instance, owner))
      {
         throw new QPermissionDeniedException("Could not establish the delivery owner's session.");
      }
      return (owner);
   }
}
