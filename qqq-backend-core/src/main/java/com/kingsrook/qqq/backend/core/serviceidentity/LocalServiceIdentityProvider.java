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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;


/*******************************************************************************
 ** Service principals for deployments without an external identity provider
 ** (TABLE_BASED, MOCK, fully anonymous). The session holds exactly the
 ** principal's declared local permissions and security keys. Each minted
 ** session is remembered (weakly, by identity) so a hand-made or deserialized
 ** QServiceSession is never valid.
 *******************************************************************************/
public class LocalServiceIdentityProvider implements QServiceIdentityProviderInterface
{
   private static final Set<QSession> MINTED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSession sessionFor(QInstance qInstance, QServicePrincipalMetaData principal) throws QAuthenticationException
   {
      if(principal == null)
      {
         throw (new QAuthenticationException("No such service principal."));
      }
      QServiceSession session = new QServiceSession(principal.getName());
      session.setUser(new QUser().withIdReference(principal.getPrincipalId()).withFullName(principal.getPrincipalId()));
      session.setIdReference(session.getUuid());
      Set<String> permissions = new TreeSet<>(principal.getLocalPermissions());
      session.setPermissions(permissions);
      for(Map.Entry<String, List<Serializable>> entry : principal.getLocalSecurityKeyValues().entrySet())
      {
         for(Serializable value : new ArrayList<>(entry.getValue()))
         {
            session.withSecurityKeyValue(entry.getKey(), value);
         }
      }
      MINTED.add(session);
      return (session);
   }



   /*******************************************************************************
    ** Whether this session was minted here, in this JVM.
    *******************************************************************************/
   public static boolean isMinted(QSession session)
   {
      return (session instanceof QServiceSession && MINTED.contains(session));
   }
}
