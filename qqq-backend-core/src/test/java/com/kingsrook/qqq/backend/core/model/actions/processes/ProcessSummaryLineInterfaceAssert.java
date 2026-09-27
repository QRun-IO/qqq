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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** AssertJ assert class for ProcessSummaryLine.
 *******************************************************************************/
public class ProcessSummaryLineInterfaceAssert extends AbstractAssert<ProcessSummaryLineInterfaceAssert, ProcessSummaryLineInterface>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   protected ProcessSummaryLineInterfaceAssert(ProcessSummaryLineInterface actual, Class<?> selfType)
   {
      super(actual, selfType);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ProcessSummaryLineInterfaceAssert assertThat(ProcessSummaryLineInterface actual)
   {
      return (new ProcessSummaryLineInterfaceAssert(actual, ProcessSummaryLineInterfaceAssert.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasCount(Integer count)
   {
      if(actual instanceof ProcessSummaryLine psl)
      {
         assertEquals(count, psl.getCount(), "Expected count in process summary line");
      }
      else
      {
         failWithMessage("ProcessSummaryLineInterface is not of concrete type ProcessSummaryLine (is: " + actual.getClass().getSimpleName() + ")");
      }

      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasStatus(Status status)
   {
      assertEquals(status, actual.getStatus(), "Expected status in process summary line");
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasMessageMatching(String regExp)
   {
      if(actual.getMessage() == null)
      {
         actual.prepareForFrontend(false);
      }

      Assertions.assertThat(actual.getMessage()).matches(regExp);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasMessageContaining(String substring)
   {
      if(actual.getMessage() == null)
      {
         actual.prepareForFrontend(false);
      }

      Assertions.assertThat(actual.getMessage()).contains(substring);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert doesNotHaveMessageMatching(String regExp)
   {
      if(actual.getMessage() == null)
      {
         actual.prepareForFrontend(false);
      }

      Assertions.assertThat(actual.getMessage()).doesNotMatch(regExp);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert doesNotHaveMessageContaining(String substring)
   {
      if(actual.getMessage() == null)
      {
         actual.prepareForFrontend(false);
      }

      Assertions.assertThat(actual.getMessage()).doesNotContain(substring);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasAnyBulletsOfTextContaining(String substring)
   {
      if(actual instanceof ProcessSummaryLine psl)
      {
         Assertions.assertThat(psl.getBulletsOfText())
            .isNotNull()
            .anyMatch(s -> s.contains(substring));
      }
      else
      {
         Assertions.fail("Process Summary Line was not the expected type.");
      }

      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert doesNotHaveAnyBulletsOfTextContaining(String substring)
   {
      if(actual instanceof ProcessSummaryLine psl)
      {
         if(psl.getBulletsOfText() != null)
         {
            Assertions.assertThat(psl.getBulletsOfText())
               .noneMatch(s -> s.contains(substring));
         }
      }

      return (this);
   }


   /***************************************************************************
    **
    ***************************************************************************/
   public ProcessSummaryLineInterface getLine()
   {
      return actual;
   }

}
