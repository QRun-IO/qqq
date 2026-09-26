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

package com.kingsrook.qqq.api.model;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.api.model.metadata.APIVersion
 *******************************************************************************/
class APIVersionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertEquals(0, new APIVersion("1.0.0").compareTo(new APIVersion("1.0.0")));

      assertTrue(new APIVersion("10").compareTo(new APIVersion("2")) > 0);
      assertTrue(new APIVersion("200").compareTo(new APIVersion("30")) > 0);
      assertTrue(new APIVersion("1.0.1").compareTo(new APIVersion("1.0.0")) > 0);
      assertTrue(new APIVersion("1.0.10").compareTo(new APIVersion("1.0.0")) > 0);
      assertTrue(new APIVersion("0.0.0-2").compareTo(new APIVersion("0.0.0-1")) > 0);

      assertTrue(new APIVersion("1.0.0").compareTo(new APIVersion("1.0.1")) < 0);
      assertTrue(new APIVersion("1.0.0").compareTo(new APIVersion("1.0.10")) < 0);

      assertTrue(new APIVersion("2023.Q1").compareTo(new APIVersion("2023.Q2")) < 0);
      assertTrue(new APIVersion("2024.Q1").compareTo(new APIVersion("2023.Q4")) > 0);
   }

}