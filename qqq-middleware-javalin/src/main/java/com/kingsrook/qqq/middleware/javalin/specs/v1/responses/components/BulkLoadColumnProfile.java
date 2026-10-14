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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** One column of an uploaded file: its shape, and sample values only where allowed.
 *******************************************************************************/
public class BulkLoadColumnProfile implements ToSchema
{
   @OpenAPIDescription("Zero-based column index in the file.")
   private Integer columnIndex;

   @OpenAPIDescription("Spreadsheet-style column letter (A, B, ... AA).")
   private String columnLetter;

   @OpenAPIDescription("Header text (or Column A, Column B ... when the file has no header row).")
   private String header;

   @OpenAPIDescription("INTEGER, DECIMAL, BOOLEAN, DATE, DATE_TIME or STRING, from the profiled values.")
   private String inferredType;

   @OpenAPIDescription("Percent of profiled rows where the column is blank.")
   private Double percentBlank;

   @OpenAPIDescription("Distinct non-blank values counted (capped at 1000).")
   private Integer distinctCount;

   @OpenAPIDescription("Whether more distinct values exist than were counted.")
   private Boolean distinctCountCapped;

   @OpenAPIDescription("Longest value length.")
   private Integer maxLength;

   @OpenAPIDescription("DATE_LIKE, EMAIL_LIKE, NUMERIC, CODE_LIKE, TEXT or EMPTY.")
   private String patternClass;

   @OpenAPIDescription("The table field the suggested mapping assigns this column to, or null.")
   private String mappedFieldName;

   @OpenAPIDescription("Data classification of the mapped field (PUBLIC, CONFIDENTIAL, PII, PHI), or null.")
   private String classification;

   @OpenAPIDescription("Whether sample values may be shown: an AI route of hipaa, or a mapped field that is unclassified or PUBLIC. Unmapped columns are treated as classified.")
   private Boolean samplesAllowed;

   @OpenAPIDescription("Up to sampleCount distinct values, only when samplesAllowed; otherwise empty.")
   @OpenAPIListItems(value = String.class, useRef = false)
   private List<String> sampleValues;



   /*******************************************************************************
    ** Getter for columnIndex
    *******************************************************************************/
   public Integer getColumnIndex()
   {
      return (this.columnIndex);
   }



   /*******************************************************************************
    ** Fluent setter for columnIndex
    *******************************************************************************/
   public BulkLoadColumnProfile withColumnIndex(Integer columnIndex)
   {
      this.columnIndex = columnIndex;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columnLetter
    *******************************************************************************/
   public String getColumnLetter()
   {
      return (this.columnLetter);
   }



   /*******************************************************************************
    ** Fluent setter for columnLetter
    *******************************************************************************/
   public BulkLoadColumnProfile withColumnLetter(String columnLetter)
   {
      this.columnLetter = columnLetter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for header
    *******************************************************************************/
   public String getHeader()
   {
      return (this.header);
   }



   /*******************************************************************************
    ** Fluent setter for header
    *******************************************************************************/
   public BulkLoadColumnProfile withHeader(String header)
   {
      this.header = header;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inferredType
    *******************************************************************************/
   public String getInferredType()
   {
      return (this.inferredType);
   }



   /*******************************************************************************
    ** Fluent setter for inferredType
    *******************************************************************************/
   public BulkLoadColumnProfile withInferredType(String inferredType)
   {
      this.inferredType = inferredType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percentBlank
    *******************************************************************************/
   public Double getPercentBlank()
   {
      return (this.percentBlank);
   }



   /*******************************************************************************
    ** Fluent setter for percentBlank
    *******************************************************************************/
   public BulkLoadColumnProfile withPercentBlank(Double percentBlank)
   {
      this.percentBlank = percentBlank;
      return (this);
   }



   /*******************************************************************************
    ** Getter for distinctCount
    *******************************************************************************/
   public Integer getDistinctCount()
   {
      return (this.distinctCount);
   }



   /*******************************************************************************
    ** Fluent setter for distinctCount
    *******************************************************************************/
   public BulkLoadColumnProfile withDistinctCount(Integer distinctCount)
   {
      this.distinctCount = distinctCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for distinctCountCapped
    *******************************************************************************/
   public Boolean getDistinctCountCapped()
   {
      return (this.distinctCountCapped);
   }



   /*******************************************************************************
    ** Fluent setter for distinctCountCapped
    *******************************************************************************/
   public BulkLoadColumnProfile withDistinctCountCapped(Boolean distinctCountCapped)
   {
      this.distinctCountCapped = distinctCountCapped;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxLength
    *******************************************************************************/
   public Integer getMaxLength()
   {
      return (this.maxLength);
   }



   /*******************************************************************************
    ** Fluent setter for maxLength
    *******************************************************************************/
   public BulkLoadColumnProfile withMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
      return (this);
   }



   /*******************************************************************************
    ** Getter for patternClass
    *******************************************************************************/
   public String getPatternClass()
   {
      return (this.patternClass);
   }



   /*******************************************************************************
    ** Fluent setter for patternClass
    *******************************************************************************/
   public BulkLoadColumnProfile withPatternClass(String patternClass)
   {
      this.patternClass = patternClass;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mappedFieldName
    *******************************************************************************/
   public String getMappedFieldName()
   {
      return (this.mappedFieldName);
   }



   /*******************************************************************************
    ** Fluent setter for mappedFieldName
    *******************************************************************************/
   public BulkLoadColumnProfile withMappedFieldName(String mappedFieldName)
   {
      this.mappedFieldName = mappedFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for classification
    *******************************************************************************/
   public String getClassification()
   {
      return (this.classification);
   }



   /*******************************************************************************
    ** Fluent setter for classification
    *******************************************************************************/
   public BulkLoadColumnProfile withClassification(String classification)
   {
      this.classification = classification;
      return (this);
   }



   /*******************************************************************************
    ** Getter for samplesAllowed
    *******************************************************************************/
   public Boolean getSamplesAllowed()
   {
      return (this.samplesAllowed);
   }



   /*******************************************************************************
    ** Fluent setter for samplesAllowed
    *******************************************************************************/
   public BulkLoadColumnProfile withSamplesAllowed(Boolean samplesAllowed)
   {
      this.samplesAllowed = samplesAllowed;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleValues
    *******************************************************************************/
   public List<String> getSampleValues()
   {
      return (this.sampleValues);
   }



   /*******************************************************************************
    ** Fluent setter for sampleValues
    *******************************************************************************/
   public BulkLoadColumnProfile withSampleValues(List<String> sampleValues)
   {
      this.sampleValues = sampleValues;
      return (this);
   }

}
