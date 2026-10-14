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

package com.kingsrook.qqq.backend.core.model.actions.tables.aggregate;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 ** Output for an aggregate action
 **
 *******************************************************************************/
public class AggregateOutput extends AbstractActionOutput
{
   private boolean truncated;
   private long rowsScanned;
   private boolean sampled;
   private BigDecimal samplePercent;

   private List<AggregateResult> results;



   /*******************************************************************************
    ** Getter for results
    **
    *******************************************************************************/
   public List<AggregateResult> getResults()
   {
      return results;
   }



   /*******************************************************************************
    ** Setter for results
    **
    *******************************************************************************/
   public void setResults(List<AggregateResult> results)
   {
      this.results = results;
   }




   /*******************************************************************************
    ** Getter for truncated
    *******************************************************************************/
   public boolean getTruncated()
   {
      return (this.truncated);
   }



   /*******************************************************************************
    ** Setter for truncated
    *******************************************************************************/
   public void setTruncated(boolean truncated)
   {
      this.truncated = truncated;
   }



   /*******************************************************************************
    ** Fluent setter for truncated
    *******************************************************************************/
   public AggregateOutput withTruncated(boolean truncated)
   {
      setTruncated(truncated);
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsScanned
    *******************************************************************************/
   public long getRowsScanned()
   {
      return (this.rowsScanned);
   }



   /*******************************************************************************
    ** Setter for rowsScanned
    *******************************************************************************/
   public void setRowsScanned(long rowsScanned)
   {
      this.rowsScanned = rowsScanned;
   }



   /*******************************************************************************
    ** Fluent setter for rowsScanned
    *******************************************************************************/
   public AggregateOutput withRowsScanned(long rowsScanned)
   {
      setRowsScanned(rowsScanned);
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampled
    *******************************************************************************/
   public boolean getSampled()
   {
      return (this.sampled);
   }



   /*******************************************************************************
    ** Setter for sampled
    *******************************************************************************/
   public void setSampled(boolean sampled)
   {
      this.sampled = sampled;
   }



   /*******************************************************************************
    ** Fluent setter for sampled
    *******************************************************************************/
   public AggregateOutput withSampled(boolean sampled)
   {
      setSampled(sampled);
      return (this);
   }



   /*******************************************************************************
    ** Getter for samplePercent
    *******************************************************************************/
   public BigDecimal getSamplePercent()
   {
      return (this.samplePercent);
   }



   /*******************************************************************************
    ** Setter for samplePercent
    *******************************************************************************/
   public void setSamplePercent(BigDecimal samplePercent)
   {
      this.samplePercent = samplePercent;
   }



   /*******************************************************************************
    ** Fluent setter for samplePercent
    *******************************************************************************/
   public AggregateOutput withSamplePercent(BigDecimal samplePercent)
   {
      setSamplePercent(samplePercent);
      return (this);
   }
}
