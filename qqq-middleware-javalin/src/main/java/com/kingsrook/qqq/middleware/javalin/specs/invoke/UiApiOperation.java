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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Method;


/*******************************************************************************
 ** One invocable UI API operation, as described by its endpoint spec: method,
 ** path template (relative to the version base path), whether it needs a
 ** session, and the full OpenAPI operation (parameters, request body and
 ** responses) for building tool definitions.
 **
 ** @param method       HTTP method
 ** @param pathTemplate path template, e.g. /table/{tableName}/query
 ** @param summary      short summary
 ** @param tag          OpenAPI tag
 ** @param secured      whether a session is required
 ** @param openApi      the OpenAPI operation object
 ** @param specClass    the endpoint spec class name
 *******************************************************************************/
public record UiApiOperation(HttpMethod method, String pathTemplate, String summary, String tag, boolean secured, Method openApi, String specClass)
{
}
