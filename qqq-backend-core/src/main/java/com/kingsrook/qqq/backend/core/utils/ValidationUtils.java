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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import org.apache.commons.validator.EmailValidator;


/*******************************************************************************
 **
 *******************************************************************************/
public class ValidationUtils
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<String> parseAndValidateEmailAddresses(String emailAddresses) throws QUserFacingException
   {
      ////////////////////////////////////////////////////////////////
      // split email address string on spaces, comma, and semicolon //
      ////////////////////////////////////////////////////////////////
      List<String> toEmailAddressList = Arrays.asList(emailAddresses.split("[\\s,;]+"));

      //////////////////////////////////////////////////////
      // check each address keeping track of any bad ones //
      //////////////////////////////////////////////////////
      List<String>   invalidEmails = new ArrayList<>();
      EmailValidator validator     = EmailValidator.getInstance();
      for(String emailAddress : toEmailAddressList)
      {
         if(!validator.isValid(emailAddress))
         {
            invalidEmails.add(emailAddress);
         }
      }

      ///////////////////////////////////////
      // if bad one found, throw exception //
      ///////////////////////////////////////
      if(!invalidEmails.isEmpty())
      {
         throw (new QUserFacingException("The following email addresses were invalid: " + StringUtils.join(",", invalidEmails)));
      }

      return (toEmailAddressList);
   }

}
