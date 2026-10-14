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


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
    ** HelpSearchInput data.
    *******************************************************************************/
public class HelpSearchInput extends AbstractActionInput
{
   private String query;
   private Integer limit;



   /*******************************************************************************
    ** Getter for query
    *******************************************************************************/
   public String getQuery()
   {
      return (this.query);
   }



   /*******************************************************************************
    ** Setter for query
    *******************************************************************************/
   public void setQuery(String query)
   {
      this.query = query;
   }



   /*******************************************************************************
    ** Fluent setter for query
    *******************************************************************************/
   public HelpSearchInput withQuery(String query)
   {
      this.query = query;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    *******************************************************************************/
   public Integer getLimit()
   {
      return (this.limit);
   }



   /*******************************************************************************
    ** Setter for limit
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    *******************************************************************************/
   public HelpSearchInput withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }
}
