/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;


/*******************************************************************************
 ** Response for the OIDC back-channel logout endpoint.
 **
 ** Per OIDC spec, successful logout returns HTTP 200 with empty body.
 *******************************************************************************/
public class BackChannelLogoutResponseV1 implements BackChannelLogoutOutputInterface, ToSchema
{

}
