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

package com.kingsrook.qqq.backend.core.model.metadata.preferences;


import java.util.Map;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.apache.commons.validator.EmailValidator;


/*******************************************************************************
 ** Provider context bound to the signed-in user.
 *******************************************************************************/
public class UserPreferenceRequestContext
{
   private final UserPreferenceSectionMetaData section;
   private final QSession session;



   /*******************************************************************************
    ** Bind the provider to the authenticated session; identity is never request data.
    *******************************************************************************/
   public UserPreferenceRequestContext(UserPreferenceSectionMetaData section, QSession session)
   {
      if(session == null || session.getUser() == null || session.getUser().getIdReference() == null)
      {
         throw (new IllegalArgumentException("A session user with an id is required for preferences"));
      }
      this.section = section;
      this.session = session;
   }



   /*******************************************************************************
    ** Get the section.
    *******************************************************************************/
   public UserPreferenceSectionMetaData getSection()
   {
      return (section);
   }



   /*******************************************************************************
    ** Get the original session, including its client channel.
    *******************************************************************************/
   public QSession getSession()
   {
      return (session);
   }



   /*******************************************************************************
    ** Get the identity from the session user only.
    *******************************************************************************/
   public String getUserId()
   {
      return (session.getUser().getIdReference());
   }



   /*******************************************************************************
    ** Prefer the OAuth frontend user email, then an email-shaped session identity.
    ** Neither value comes from the preference request.
    *******************************************************************************/
   public String getUserEmailAddress()
   {
      if(session.getValuesForFrontend() != null && session.getValuesForFrontend().get("user") instanceof Map<?, ?> user
         && user.get("email") instanceof String email && !email.isBlank())
      {
         return (email);
      }
      return (EmailValidator.getInstance().isValid(getUserId()) ? getUserId() : null);
   }
}
