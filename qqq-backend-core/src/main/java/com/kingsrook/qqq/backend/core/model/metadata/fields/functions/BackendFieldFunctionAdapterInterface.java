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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 * interface for an "adapter" class that implements a {@link FieldFunction} for a
 * particular backend ({@link QBackendModuleInterface}).
 *
 * <p>For example, RDBMS - needs to wrap column names in function calls.</p>
 *
 * <p>It is expected that individual modules would define their own subinterfaces
 * which would define the actual methods appropriate for that backend.</p>
 *
 * <p>This interface just provides a common type to be exposed in the
 * {@link BackendFieldFunctionAdapterRegistry}</p>
 *******************************************************************************/
public interface BackendFieldFunctionAdapterInterface extends Serializable
{
}
