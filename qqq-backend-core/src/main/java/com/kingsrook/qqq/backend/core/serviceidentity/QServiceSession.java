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

package com.kingsrook.qqq.backend.core.serviceidentity;


import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** A local service principal's session (TABLE_BASED, MOCK, fully anonymous
 ** deployments). Its permissions and security keys are exactly the principal's
 ** declared local ones - it is NOT a QSystemUserSession. Only the local
 ** provider mints these (in this JVM), and only modules that support local
 ** principals accept them; OAuth2 and Auth0 never do.
 *******************************************************************************/
public final class QServiceSession extends QSession
{
   private final String principalName;



   /*******************************************************************************
    ** Constructor - package private: LocalServiceIdentityProvider only.
    *******************************************************************************/
   QServiceSession(String principalName)
   {
      super();
      this.principalName = principalName;
   }



   /*******************************************************************************
    ** Getter for principalName
    *******************************************************************************/
   public String getPrincipalName()
   {
      return (this.principalName);
   }
}
