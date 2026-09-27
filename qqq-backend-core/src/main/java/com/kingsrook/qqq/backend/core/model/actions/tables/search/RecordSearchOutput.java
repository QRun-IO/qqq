/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.search;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 ** Output for {@link com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction}:
 ** matching records, grouped by table in instance order.
 *******************************************************************************/
public class RecordSearchOutput extends AbstractActionOutput
{
   private List<RecordSearchResult> results = new ArrayList<>();



   /*******************************************************************************
    ** Getter for results
    *******************************************************************************/
   public List<RecordSearchResult> getResults()
   {
      return (this.results);
   }



   /*******************************************************************************
    ** Setter for results
    *******************************************************************************/
   public void setResults(List<RecordSearchResult> results)
   {
      this.results = results;
   }



   /*******************************************************************************
    ** Fluent setter for results
    *******************************************************************************/
   public RecordSearchOutput withResults(List<RecordSearchResult> results)
   {
      this.results = results;
      return (this);
   }

}
