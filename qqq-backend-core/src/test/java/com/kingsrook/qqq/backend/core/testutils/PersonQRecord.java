/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.testutils;


import java.math.BigDecimal;
import java.time.LocalDate;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class PersonQRecord extends QRecord
{
   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withLastName(String lastName)
   {
      setValue("lastName", lastName);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withFirstName(String firstName)
   {
      setValue("firstName", firstName);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withBirthDate(LocalDate birthDate)
   {
      setValue("birthDate", birthDate);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withNoOfShoes(Integer noOfShoes)
   {
      setValue("noOfShoes", noOfShoes);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withPrice(BigDecimal price)
   {
      setValue("price", price);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withCost(BigDecimal cost)
   {
      setValue("cost", cost);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PersonQRecord withHomeStateId(int homeStateId)
   {
      setValue("homeStateId", homeStateId);
      return (this);
   }

}
