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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ValidationUtils 
 *******************************************************************************/
class ValidationUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QUserFacingException
   {
      assertThatThrownBy(() -> ValidationUtils.parseAndValidateEmailAddresses("notEmail"))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("email addresses were invalid: notEmail");

      assertThatThrownBy(() -> ValidationUtils.parseAndValidateEmailAddresses("foo@bar.com, whatever"))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("email addresses were invalid: whatever");

      assertThatThrownBy(() -> ValidationUtils.parseAndValidateEmailAddresses("foo whatever"))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("email addresses were invalid: foo,whatever");

      assertEquals(List.of("foo@bar.com"), ValidationUtils.parseAndValidateEmailAddresses("foo@bar.com ")); // space here intentional!
      assertEquals(List.of("foo@bar.com"), ValidationUtils.parseAndValidateEmailAddresses("foo@bar.com;"));
      assertEquals(List.of("foo@bar.com", "fiz@buz.com"), ValidationUtils.parseAndValidateEmailAddresses("foo@bar.com, fiz@buz.com"));
   }

}