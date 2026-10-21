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
 ** A session for work a service principal does on behalf of a user: the user's
 ** identity and current access, intersected with the principal's permissions,
 ** attributed to the principal. It is valid only while its parent (the
 ** principal's own session) is valid. Never serialized, never reachable from a
 ** request: only ServiceIdentities mints these.
 *******************************************************************************/
public final class QDelegatedSession extends QSession
{
   private final transient QSession parent;
   private final String             principalName;



   /*******************************************************************************
    ** Constructor - package private: ServiceIdentities only.
    *******************************************************************************/
   QDelegatedSession(QSession parent, String principalName)
   {
      super();
      this.parent = parent;
      this.principalName = principalName;
   }



   /*******************************************************************************
    ** The principal's own session this delegation hangs off.
    *******************************************************************************/
   public QSession getParent()
   {
      return (this.parent);
   }



   /*******************************************************************************
    ** Getter for principalName
    *******************************************************************************/
   public String getPrincipalName()
   {
      return (this.principalName);
   }
}
