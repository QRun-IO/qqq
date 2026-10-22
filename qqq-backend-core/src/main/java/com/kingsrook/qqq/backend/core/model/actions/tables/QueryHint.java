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

package com.kingsrook.qqq.backend.core.model.actions.tables;


/*******************************************************************************
 ** Information about the query that an application (or qqq service) may know and
 ** want to tell the backend, that can help influence how the backend processes
 ** query.
 **
 ** For example, a query with potentially a large result set, for MySQL backend,
 ** we may want to configure the result set to stream results rather than do its
 ** default in-memory thing.  See RDBMSQueryAction for usage.
 *******************************************************************************/
public enum QueryHint
{
   POTENTIALLY_LARGE_NUMBER_OF_RESULTS,
   MAY_USE_READ_ONLY_BACKEND
}
