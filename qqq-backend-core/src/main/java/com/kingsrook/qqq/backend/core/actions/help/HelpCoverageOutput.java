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

package com.kingsrook.qqq.backend.core.actions.help;


import java.util.List;


/*******************************************************************************
    ** HelpCoverageOutput data.
    *******************************************************************************/
public class HelpCoverageOutput
{
   private List<HelpCoverageTotal> totals;
   private List<HelpCoverageEntry> entries;



   /*******************************************************************************
    ** Getter for totals
    *******************************************************************************/
   public List<HelpCoverageTotal> getTotals()
   {
      return (this.totals);
   }



   /*******************************************************************************
    ** Setter for totals
    *******************************************************************************/
   public void setTotals(List<HelpCoverageTotal> totals)
   {
      this.totals = totals;
   }



   /*******************************************************************************
    ** Fluent setter for totals
    *******************************************************************************/
   public HelpCoverageOutput withTotals(List<HelpCoverageTotal> totals)
   {
      this.totals = totals;
      return (this);
   }



   /*******************************************************************************
    ** Getter for entries
    *******************************************************************************/
   public List<HelpCoverageEntry> getEntries()
   {
      return (this.entries);
   }



   /*******************************************************************************
    ** Setter for entries
    *******************************************************************************/
   public void setEntries(List<HelpCoverageEntry> entries)
   {
      this.entries = entries;
   }



   /*******************************************************************************
    ** Fluent setter for entries
    *******************************************************************************/
   public HelpCoverageOutput withEntries(List<HelpCoverageEntry> entries)
   {
      this.entries = entries;
      return (this);
   }
}
