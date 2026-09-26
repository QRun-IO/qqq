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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public interface AuthenticationMetaDataOutputInterface extends AbstractMiddlewareOutputInterface
{
   /***************************************************************************
    **
    ***************************************************************************/
   void setAuthenticationMetaData(QAuthenticationMetaData qAuthenticationMetaData);

   /***************************************************************************
    ** The instance's branding, for the part of it that is safe to show before
    ** sign-in.  Outputs that do not expose branding ignore it.
    ***************************************************************************/
   default void setBranding(QBrandingMetaData branding)
   {
   }
}
