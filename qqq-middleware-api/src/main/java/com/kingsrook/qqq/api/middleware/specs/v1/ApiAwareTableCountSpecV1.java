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

package com.kingsrook.qqq.api.middleware.specs.v1;


import com.kingsrook.qqq.api.middleware.executors.ApiAwareTableCountExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.TableCountExecutor;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableCountSpecV1;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiAwareTableCountSpecV1 extends TableCountSpecV1
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public TableCountExecutor newExecutor()
   {
      return new ApiAwareTableCountExecutor();
   }

}
