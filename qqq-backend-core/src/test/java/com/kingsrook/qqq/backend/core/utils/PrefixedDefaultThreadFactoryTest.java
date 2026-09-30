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

package com.kingsrook.qqq.backend.core.utils;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for PrefixedDefaultThreadFactory 
 *******************************************************************************/
class PrefixedDefaultThreadFactoryTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertThat(new PrefixedDefaultThreadFactory("ItsMe").newThread(this::noop).getName()).startsWith("ItsMe-pool");
      assertThat(new PrefixedDefaultThreadFactory("ItsMe-").newThread(this::noop).getName()).startsWith("ItsMe-pool");
      assertThat(new PrefixedDefaultThreadFactory("ItsMe--").newThread(this::noop).getName()).startsWith("ItsMe-pool");
      assertThat(new PrefixedDefaultThreadFactory((String) null).newThread(this::noop).getName()).startsWith("pool");
      assertThat(new PrefixedDefaultThreadFactory((Class<?>) null).newThread(this::noop).getName()).startsWith("pool");
      assertThat(new PrefixedDefaultThreadFactory((Object) null).newThread(this::noop).getName()).startsWith("pool");
      assertThat(new PrefixedDefaultThreadFactory("").newThread(this::noop).getName()).startsWith("pool");
      assertThat(new PrefixedDefaultThreadFactory(" ").newThread(this::noop).getName()).startsWith("pool");
      assertThat(new PrefixedDefaultThreadFactory(this).newThread(this::noop).getName()).startsWith(getClass().getSimpleName() + "-pool");
      assertThat(new PrefixedDefaultThreadFactory(InsertAction.class).newThread(this::noop).getName()).startsWith(InsertAction.class.getSimpleName() + "-pool");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   void noop()
   {
      System.out.println("noop");
   }

}