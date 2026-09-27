/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata;


import java.util.List;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.api.model.metadata.ApiOperation
 *******************************************************************************/
class ApiOperationTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertTrue(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData(), new ApiTableMetaData())));
      assertTrue(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData(), new ApiTableMetaData().withEnabledOperation(ApiOperation.GET))));
      assertFalse(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData(), new ApiTableMetaData().withDisabledOperation(ApiOperation.GET))));
      assertTrue(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData().withEnabledOperation(ApiOperation.GET), new ApiTableMetaData())));
      assertTrue(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData().withEnabledOperation(ApiOperation.GET), new ApiTableMetaData().withEnabledOperation(ApiOperation.GET))));
      assertFalse(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData().withEnabledOperation(ApiOperation.GET), new ApiTableMetaData().withDisabledOperation(ApiOperation.GET))));
      assertFalse(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData().withDisabledOperation(ApiOperation.GET), new ApiTableMetaData())));
      assertTrue(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData().withDisabledOperation(ApiOperation.GET), new ApiTableMetaData().withEnabledOperation(ApiOperation.GET))));
      assertFalse(ApiOperation.GET.isOperationEnabled(List.of(new ApiInstanceMetaData().withDisabledOperation(ApiOperation.GET), new ApiTableMetaData().withDisabledOperation(ApiOperation.GET))));
   }

}