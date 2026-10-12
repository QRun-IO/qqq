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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Settings for an {@link AiToolKernel}.
 **
 ** maxRowsToModel - the most rows any tool sends to the model (a server cap -
 **    a larger requested limit is reduced to this).
 ** maxCellChars - longer string values are truncated (after redaction).
 ** defaultExposure - the exposure of tables and fields that have no explicit
 **    {@link AiTableMetaData} / {@link AiFieldMetaData} and no more restrictive
 **    default (e.g., METADATA_ONLY for a high-sensitivity deployment).
 ** customRedactionPatterns - additional patterns the redactor replaces with
 **    [REDACTED:custom], on top of its built-in secret and PII patterns.
 ** hiddenWidgetNames - widgets the render_widget tool may never render (e.g.,
 **    widgets that render data as a system user).
 *******************************************************************************/
public class AiToolKernelSettings
{
   public static final int DEFAULT_MAX_ROWS_TO_MODEL = 50;
   public static final int DEFAULT_MAX_CELL_CHARS    = 500;

   private Integer       maxRowsToModel          = DEFAULT_MAX_ROWS_TO_MODEL;
   private Integer       maxCellChars            = DEFAULT_MAX_CELL_CHARS;
   private AiExposure    defaultExposure         = AiExposure.VALUES;
   private List<Pattern> customRedactionPatterns = new ArrayList<>();
   private Set<String>   hiddenWidgetNames       = new HashSet<>();



   /*******************************************************************************
    ** Fluent method to add one custom redaction pattern.
    *******************************************************************************/
   public AiToolKernelSettings withCustomRedactionPattern(Pattern pattern)
   {
      if(this.customRedactionPatterns == null)
      {
         this.customRedactionPatterns = new ArrayList<>();
      }
      this.customRedactionPatterns.add(pattern);
      return (this);
   }



   /*******************************************************************************
    ** Fluent method to add one hidden widget name.
    *******************************************************************************/
   public AiToolKernelSettings withHiddenWidgetName(String widgetName)
   {
      if(this.hiddenWidgetNames == null)
      {
         this.hiddenWidgetNames = new HashSet<>();
      }
      this.hiddenWidgetNames.add(widgetName);
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxRowsToModel
    *******************************************************************************/
   public Integer getMaxRowsToModel()
   {
      return (this.maxRowsToModel);
   }



   /*******************************************************************************
    ** Setter for maxRowsToModel
    *******************************************************************************/
   public void setMaxRowsToModel(Integer maxRowsToModel)
   {
      this.maxRowsToModel = maxRowsToModel;
   }



   /*******************************************************************************
    ** Fluent setter for maxRowsToModel
    *******************************************************************************/
   public AiToolKernelSettings withMaxRowsToModel(Integer maxRowsToModel)
   {
      this.maxRowsToModel = maxRowsToModel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxCellChars
    *******************************************************************************/
   public Integer getMaxCellChars()
   {
      return (this.maxCellChars);
   }



   /*******************************************************************************
    ** Setter for maxCellChars
    *******************************************************************************/
   public void setMaxCellChars(Integer maxCellChars)
   {
      this.maxCellChars = maxCellChars;
   }



   /*******************************************************************************
    ** Fluent setter for maxCellChars
    *******************************************************************************/
   public AiToolKernelSettings withMaxCellChars(Integer maxCellChars)
   {
      this.maxCellChars = maxCellChars;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultExposure
    *******************************************************************************/
   public AiExposure getDefaultExposure()
   {
      return (this.defaultExposure);
   }



   /*******************************************************************************
    ** Setter for defaultExposure
    *******************************************************************************/
   public void setDefaultExposure(AiExposure defaultExposure)
   {
      this.defaultExposure = defaultExposure;
   }



   /*******************************************************************************
    ** Fluent setter for defaultExposure
    *******************************************************************************/
   public AiToolKernelSettings withDefaultExposure(AiExposure defaultExposure)
   {
      this.defaultExposure = defaultExposure;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customRedactionPatterns
    *******************************************************************************/
   public List<Pattern> getCustomRedactionPatterns()
   {
      return (this.customRedactionPatterns);
   }



   /*******************************************************************************
    ** Setter for customRedactionPatterns
    *******************************************************************************/
   public void setCustomRedactionPatterns(List<Pattern> customRedactionPatterns)
   {
      this.customRedactionPatterns = customRedactionPatterns;
   }



   /*******************************************************************************
    ** Fluent setter for customRedactionPatterns
    *******************************************************************************/
   public AiToolKernelSettings withCustomRedactionPatterns(List<Pattern> customRedactionPatterns)
   {
      this.customRedactionPatterns = customRedactionPatterns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hiddenWidgetNames
    *******************************************************************************/
   public Set<String> getHiddenWidgetNames()
   {
      return (this.hiddenWidgetNames);
   }



   /*******************************************************************************
    ** Setter for hiddenWidgetNames
    *******************************************************************************/
   public void setHiddenWidgetNames(Set<String> hiddenWidgetNames)
   {
      this.hiddenWidgetNames = hiddenWidgetNames;
   }



   /*******************************************************************************
    ** Fluent setter for hiddenWidgetNames
    *******************************************************************************/
   public AiToolKernelSettings withHiddenWidgetNames(Set<String> hiddenWidgetNames)
   {
      this.hiddenWidgetNames = hiddenWidgetNames;
      return (this);
   }
}
