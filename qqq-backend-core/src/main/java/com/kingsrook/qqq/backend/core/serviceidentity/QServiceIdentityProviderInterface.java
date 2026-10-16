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


import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** How an authentication module turns a declared service principal into a
 ** session (docs/SERVICE-IDENTITY.md section 4.2). The module decides: OAuth2
 ** obtains a real access token (client_credentials) and builds a bearer
 ** session; local modules mint a QServiceSession.
 *******************************************************************************/
public interface QServiceIdentityProviderInterface
{
   /*******************************************************************************
    ** A fresh session for the principal. Throws when the principal is
    ** unavailable (no credentials, refused, revoked); never falls back to a
    ** system session.
    *******************************************************************************/
   QSession sessionFor(QInstance qInstance, QServicePrincipalMetaData principal) throws QAuthenticationException;

}
