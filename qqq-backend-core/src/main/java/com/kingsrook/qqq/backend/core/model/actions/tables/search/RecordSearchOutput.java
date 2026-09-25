/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
