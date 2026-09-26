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

package com.kingsrook.qqq.backend.core.model.metadata.security;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for NullValueBehaviorUtil 
 *******************************************************************************/
class NullValueBehaviorUtilTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // if session doesn't have a null-value key, then always get back the lock's behavior //
      ////////////////////////////////////////////////////////////////////////////////////////
      for(RecordSecurityLock.NullValueBehavior lockNullValueBehavior : RecordSecurityLock.NullValueBehavior.values())
      {
         assertEquals(lockNullValueBehavior, NullValueBehaviorUtil.getEffectiveNullValueBehavior(new RecordSecurityLock()
            .withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE)
            .withNullValueBehavior(lockNullValueBehavior)));
      }

      /////////////////////////////////////////////////////////////////////
      // if session DOES have a null-value key, then always gete it back //
      /////////////////////////////////////////////////////////////////////
      for(RecordSecurityLock.NullValueBehavior sessionNullValueBehavior : RecordSecurityLock.NullValueBehavior.values())
      {
         QContext.getQSession().withSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE_NULL_BEHAVIOR, List.of(sessionNullValueBehavior.toString())));

         for(RecordSecurityLock.NullValueBehavior lockNullValueBehavior : RecordSecurityLock.NullValueBehavior.values())
         {
            assertEquals(sessionNullValueBehavior, NullValueBehaviorUtil.getEffectiveNullValueBehavior(new RecordSecurityLock()
               .withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE)
               .withNullValueBehavior(lockNullValueBehavior)));
         }
      }

      ////////////////////////////////////////////////////////////////////
      // if session has an invalid key, always get back lock's behavior //
      ////////////////////////////////////////////////////////////////////
      for(RecordSecurityLock.NullValueBehavior lockNullValueBehavior : RecordSecurityLock.NullValueBehavior.values())
      {
         QContext.getQSession().withSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE_NULL_BEHAVIOR, List.of("xyz")));

         assertEquals(lockNullValueBehavior, NullValueBehaviorUtil.getEffectiveNullValueBehavior(new RecordSecurityLock()
            .withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE)
            .withNullValueBehavior(lockNullValueBehavior)));
      }
   }

}