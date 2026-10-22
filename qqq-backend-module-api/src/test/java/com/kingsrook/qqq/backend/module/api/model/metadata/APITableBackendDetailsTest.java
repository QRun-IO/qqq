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

package com.kingsrook.qqq.backend.module.api.model.metadata;


import com.kingsrook.qqq.backend.module.api.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for APITableBackendDetails 
 *******************************************************************************/
class APITableBackendDetailsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testClone()
   {
      APITableBackendDetails tableBackendDetails = (APITableBackendDetails) new APITableBackendDetails()
         .withTablePath("a")
         .withTableWrapperObjectName("b")
         .withBackendType("c");

      APITableBackendDetails clonedTableBackendDetails = (APITableBackendDetails) tableBackendDetails.clone();
      clonedTableBackendDetails.withTablePath("x");

      assertEquals("a", tableBackendDetails.getTablePath());
      assertEquals("b", tableBackendDetails.getTableWrapperObjectName());
      assertEquals("c", tableBackendDetails.getBackendType());

      assertEquals("x", clonedTableBackendDetails.getTablePath());
      assertEquals("b", clonedTableBackendDetails.getTableWrapperObjectName());
      assertEquals("c", clonedTableBackendDetails.getBackendType());

      clonedTableBackendDetails.withBackendType("z");
      assertEquals("c", tableBackendDetails.getBackendType());
      assertEquals("z", clonedTableBackendDetails.getBackendType());

   }

}