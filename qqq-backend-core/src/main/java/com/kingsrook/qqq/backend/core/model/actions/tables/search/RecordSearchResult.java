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
import java.time.Instant;
import java.util.List;


/*******************************************************************************
 ** One record matched by record search: its table, primary key and label.
 *******************************************************************************/
public class RecordSearchResult implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String                      tableName;
   private String                      tableLabel;
   private Serializable                recordId;
   private String                      recordLabel;
   private Double                      score;
   private Instant                     updatedAt;
   private List<RecordSearchHighlight> highlights;
   private List<RecordSearchDetail>    details;



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public RecordSearchResult withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableLabel
    *******************************************************************************/
   public String getTableLabel()
   {
      return (this.tableLabel);
   }



   /*******************************************************************************
    ** Setter for tableLabel
    *******************************************************************************/
   public void setTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
   }



   /*******************************************************************************
    ** Fluent setter for tableLabel
    *******************************************************************************/
   public RecordSearchResult withTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordId
    *******************************************************************************/
   public Serializable getRecordId()
   {
      return (this.recordId);
   }



   /*******************************************************************************
    ** Setter for recordId
    *******************************************************************************/
   public void setRecordId(Serializable recordId)
   {
      this.recordId = recordId;
   }



   /*******************************************************************************
    ** Fluent setter for recordId (the record's primary key value)
    *******************************************************************************/
   public RecordSearchResult withRecordId(Serializable recordId)
   {
      this.recordId = recordId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordLabel
    *******************************************************************************/
   public String getRecordLabel()
   {
      return (this.recordLabel);
   }



   /*******************************************************************************
    ** Setter for recordLabel
    *******************************************************************************/
   public void setRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
   }



   /*******************************************************************************
    ** Fluent setter for recordLabel
    *******************************************************************************/
   public RecordSearchResult withRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for score
    *******************************************************************************/
   public Double getScore()
   {
      return (this.score);
   }



   /*******************************************************************************
    ** Setter for score
    *******************************************************************************/
   public void setScore(Double score)
   {
      this.score = score;
   }



   /*******************************************************************************
    ** Fluent setter for score - relevance from a record search provider (higher is better; null for the default search)
    *******************************************************************************/
   public RecordSearchResult withScore(Double score)
   {
      this.score = score;
      return (this);
   }



   /*******************************************************************************
    ** Getter for updatedAt
    *******************************************************************************/
   public Instant getUpdatedAt()
   {
      return (this.updatedAt);
   }



   /*******************************************************************************
    ** Setter for updatedAt
    *******************************************************************************/
   public void setUpdatedAt(Instant updatedAt)
   {
      this.updatedAt = updatedAt;
   }



   /*******************************************************************************
    ** Fluent setter for updatedAt - when the record was last modified (its table's MODIFY_DATE field), if the session may see that field
    *******************************************************************************/
   public RecordSearchResult withUpdatedAt(Instant updatedAt)
   {
      this.updatedAt = updatedAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for highlights
    *******************************************************************************/
   public List<RecordSearchHighlight> getHighlights()
   {
      return (this.highlights);
   }



   /*******************************************************************************
    ** Setter for highlights
    *******************************************************************************/
   public void setHighlights(List<RecordSearchHighlight> highlights)
   {
      this.highlights = highlights;
   }



   /*******************************************************************************
    ** Fluent setter for highlights - plain-text snippets of the fields that match the term
    *******************************************************************************/
   public RecordSearchResult withHighlights(List<RecordSearchHighlight> highlights)
   {
      this.highlights = highlights;
      return (this);
   }



   /*******************************************************************************
    ** Getter for details
    *******************************************************************************/
   public List<RecordSearchDetail> getDetails()
   {
      return (this.details);
   }



   /*******************************************************************************
    ** Setter for details
    *******************************************************************************/
   public void setDetails(List<RecordSearchDetail> details)
   {
      this.details = details;
   }



   /*******************************************************************************
    ** Fluent setter for details - secondary field values to show under the record label
    *******************************************************************************/
   public RecordSearchResult withDetails(List<RecordSearchDetail> details)
   {
      this.details = details;
      return (this);
   }

}
