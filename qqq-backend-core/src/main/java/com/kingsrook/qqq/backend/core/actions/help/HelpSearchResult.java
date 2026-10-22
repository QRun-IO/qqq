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


import com.fasterxml.jackson.annotation.JsonIgnore;




/*******************************************************************************
    ** HelpSearchResult data.
    *******************************************************************************/
public class HelpSearchResult
{
   private String searchSummary;
   private String kind;
   private String key;
   private String name;
   private String table;
   private String process;
   private String label;
   private String summary;
   private String text;
   private String contentAsHtml;
   private String href;



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public String getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(String kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public HelpSearchResult withKind(String kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (this.key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public HelpSearchResult withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public HelpSearchResult withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for table
    *******************************************************************************/
   public String getTable()
   {
      return (this.table);
   }



   /*******************************************************************************
    ** Setter for table
    *******************************************************************************/
   public void setTable(String table)
   {
      this.table = table;
   }



   /*******************************************************************************
    ** Fluent setter for table
    *******************************************************************************/
   public HelpSearchResult withTable(String table)
   {
      this.table = table;
      return (this);
   }



   /*******************************************************************************
    ** Getter for process
    *******************************************************************************/
   public String getProcess()
   {
      return (this.process);
   }



   /*******************************************************************************
    ** Setter for process
    *******************************************************************************/
   public void setProcess(String process)
   {
      this.process = process;
   }



   /*******************************************************************************
    ** Fluent setter for process
    *******************************************************************************/
   public HelpSearchResult withProcess(String process)
   {
      this.process = process;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public HelpSearchResult withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public HelpSearchResult withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for text
    *******************************************************************************/
   public String getText()
   {
      return (this.text);
   }



   /*******************************************************************************
    ** Setter for text
    *******************************************************************************/
   public void setText(String text)
   {
      this.text = text;
   }



   /*******************************************************************************
    ** Fluent setter for text
    *******************************************************************************/
   public HelpSearchResult withText(String text)
   {
      this.text = text;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentAsHtml
    *******************************************************************************/
   public String getContentAsHtml()
   {
      return (this.contentAsHtml);
   }



   /*******************************************************************************
    ** Setter for contentAsHtml
    *******************************************************************************/
   public void setContentAsHtml(String contentAsHtml)
   {
      this.contentAsHtml = contentAsHtml;
   }



   /*******************************************************************************
    ** Fluent setter for contentAsHtml
    *******************************************************************************/
   public HelpSearchResult withContentAsHtml(String contentAsHtml)
   {
      this.contentAsHtml = contentAsHtml;
      return (this);
   }



   /*******************************************************************************
    ** Getter for href
    *******************************************************************************/
   public String getHref()
   {
      return (this.href);
   }



   /*******************************************************************************
    ** Setter for href
    *******************************************************************************/
   public void setHref(String href)
   {
      this.href = href;
   }



   /*******************************************************************************
    ** Fluent setter for href
    *******************************************************************************/
   public HelpSearchResult withHref(String href)
   {
      this.href = href;
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchSummary
    *******************************************************************************/
   @JsonIgnore
   public String getSearchSummary()
   {
      return (this.searchSummary);
   }



   /*******************************************************************************
    ** Setter for searchSummary
    *******************************************************************************/
   public void setSearchSummary(String searchSummary)
   {
      this.searchSummary = searchSummary;
   }



   /*******************************************************************************
    ** Fluent setter for searchSummary
    *******************************************************************************/
   public HelpSearchResult withSearchSummary(String searchSummary)
   {
      this.searchSummary = searchSummary;
      return (this);
   }
}
