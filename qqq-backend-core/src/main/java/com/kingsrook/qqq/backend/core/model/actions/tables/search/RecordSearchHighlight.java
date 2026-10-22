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


import java.io.Serializable;
import java.util.List;


/*******************************************************************************
 ** A plain-text snippet of one field of a record search result, with the spans
 ** that match the search term.  The text is never markup: render it as text
 ** and emphasize the match ranges.
 *******************************************************************************/
public class RecordSearchHighlight implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String                           fieldName;
   private String                           fieldLabel;
   private String                           text;
   private List<RecordSearchHighlightMatch> matches;



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName - the field the snippet is from
    *******************************************************************************/
   public RecordSearchHighlight withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldLabel
    *******************************************************************************/
   public String getFieldLabel()
   {
      return (this.fieldLabel);
   }



   /*******************************************************************************
    ** Setter for fieldLabel
    *******************************************************************************/
   public void setFieldLabel(String fieldLabel)
   {
      this.fieldLabel = fieldLabel;
   }



   /*******************************************************************************
    ** Fluent setter for fieldLabel - the field's user-facing label
    *******************************************************************************/
   public RecordSearchHighlight withFieldLabel(String fieldLabel)
   {
      this.fieldLabel = fieldLabel;
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
    ** Fluent setter for text - plain-text snippet (an ellipsis marks trimmed ends)
    *******************************************************************************/
   public RecordSearchHighlight withText(String text)
   {
      this.text = text;
      return (this);
   }



   /*******************************************************************************
    ** Getter for matches
    *******************************************************************************/
   public List<RecordSearchHighlightMatch> getMatches()
   {
      return (this.matches);
   }



   /*******************************************************************************
    ** Setter for matches
    *******************************************************************************/
   public void setMatches(List<RecordSearchHighlightMatch> matches)
   {
      this.matches = matches;
   }



   /*******************************************************************************
    ** Fluent setter for matches - matched spans in text, sorted and non-overlapping
    *******************************************************************************/
   public RecordSearchHighlight withMatches(List<RecordSearchHighlightMatch> matches)
   {
      this.matches = matches;
      return (this);
   }

}
