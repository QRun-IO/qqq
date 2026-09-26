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

package com.kingsrook.qqq.backend.core.model.actions.values;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;


/*******************************************************************************
 ** Output for the Search possible value source action
 *******************************************************************************/
public class SearchPossibleValueSourceOutput extends AbstractActionOutput
{
   private List<QPossibleValue<?>> results = new ArrayList<>();

   private String warning;


   /*******************************************************************************
    **
    *******************************************************************************/
   public SearchPossibleValueSourceOutput()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addResult(QPossibleValue<?> possibleValue)
   {
      results.add(possibleValue);
   }



   /*******************************************************************************
    ** Getter for results
    **
    *******************************************************************************/
   public List<QPossibleValue<?>> getResults()
   {
      return results;
   }



   /*******************************************************************************
    ** Setter for results
    **
    *******************************************************************************/
   public void setResults(List<QPossibleValue<?>> results)
   {
      this.results = results;
   }



   /*******************************************************************************
    ** Fluent setter for results
    **
    *******************************************************************************/
   public SearchPossibleValueSourceOutput withResults(List<QPossibleValue<?>> results)
   {
      this.results = results;
      return (this);
   }


   /*******************************************************************************
    ** Getter for warning
    *******************************************************************************/
   public String getWarning()
   {
      return (this.warning);
   }



   /*******************************************************************************
    ** Setter for warning
    *******************************************************************************/
   public void setWarning(String warning)
   {
      this.warning = warning;
   }



   /*******************************************************************************
    ** Fluent setter for warning
    *******************************************************************************/
   public SearchPossibleValueSourceOutput withWarning(String warning)
   {
      this.warning = warning;
      return (this);
   }


}
