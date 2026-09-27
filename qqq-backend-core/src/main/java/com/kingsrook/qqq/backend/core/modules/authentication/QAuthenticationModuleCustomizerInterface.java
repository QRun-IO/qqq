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

package com.kingsrook.qqq.backend.core.modules.authentication;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Interface for customizing behavior of an Authentication module.
 *******************************************************************************/
public interface QAuthenticationModuleCustomizerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   default void addSecurityKeyValueToSession(QSession session, String keyName, Serializable value)
   {
      session.withSecurityKeyValue(keyName, value);
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
   {
      //////////
      // noop //
      //////////
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default void finalCustomizeSession(QInstance qInstance, QSession qSession)
   {
      //////////
      // noop //
      //////////
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default void customizeAutomatedSessionForUser(QInstance qInstance, QSession automatedSessionForUser, Serializable userId) throws QAuthenticationException
   {
      //////////
      // noop //
      //////////
   }

}
