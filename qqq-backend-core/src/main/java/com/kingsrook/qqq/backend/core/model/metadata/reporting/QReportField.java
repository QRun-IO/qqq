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

package com.kingsrook.qqq.backend.core.model.metadata.reporting;


import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 ** Field within a report
 *******************************************************************************/
public class QReportField
{
   private String     name;
   private String     label;
   private QFieldType type;
   private String     formula;
   private String     displayFormat;

   ///////////////////////////////////////////////////////////////////////////
   // Note: new attributes added here probably belong in the toField method //
   ///////////////////////////////////////////////////////////////////////////

   private boolean isVirtual = false;

   private boolean showPossibleValueLabel = false;
   private String  sourceFieldName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QReportField()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QReportField(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData toField()
   {
      return new QFieldMetaData()
         .withName(name)
         .withLabel(label)
         .withType(type)
         .withDisplayFormat(displayFormat);
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QReportField withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public QReportField withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public QFieldType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(QFieldType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public QReportField withType(QFieldType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formula
    **
    *******************************************************************************/
   public String getFormula()
   {
      return formula;
   }



   /*******************************************************************************
    ** Setter for formula
    **
    *******************************************************************************/
   public void setFormula(String formula)
   {
      this.formula = formula;
   }



   /*******************************************************************************
    ** Fluent setter for formula
    **
    *******************************************************************************/
   public QReportField withFormula(String formula)
   {
      this.formula = formula;
      return (this);
   }



   /*******************************************************************************
    ** Getter for displayFormat
    **
    *******************************************************************************/
   public String getDisplayFormat()
   {
      return displayFormat;
   }



   /*******************************************************************************
    ** Setter for displayFormat
    **
    *******************************************************************************/
   public void setDisplayFormat(String displayFormat)
   {
      this.displayFormat = displayFormat;
   }



   /*******************************************************************************
    ** Fluent setter for displayFormat
    **
    *******************************************************************************/
   public QReportField withDisplayFormat(String displayFormat)
   {
      this.displayFormat = displayFormat;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isVirtual
    **
    *******************************************************************************/
   public boolean getIsVirtual()
   {
      return isVirtual;
   }



   /*******************************************************************************
    ** Setter for isVirtual
    **
    *******************************************************************************/
   public void setIsVirtual(boolean isVirtual)
   {
      this.isVirtual = isVirtual;
   }



   /*******************************************************************************
    ** Fluent setter for isVirtual
    **
    *******************************************************************************/
   public QReportField withIsVirtual(boolean isVirtual)
   {
      this.isVirtual = isVirtual;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceFieldName
    **
    *******************************************************************************/
   public String getSourceFieldName()
   {
      return sourceFieldName;
   }



   /*******************************************************************************
    ** Setter for sourceFieldName
    **
    *******************************************************************************/
   public void setSourceFieldName(String sourceFieldName)
   {
      this.sourceFieldName = sourceFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceFieldName
    **
    *******************************************************************************/
   public QReportField withSourceFieldName(String sourceFieldName)
   {
      this.sourceFieldName = sourceFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for showPossibleValueLabel
    **
    *******************************************************************************/
   public boolean getShowPossibleValueLabel()
   {
      return showPossibleValueLabel;
   }



   /*******************************************************************************
    ** Setter for showPossibleValueLabel
    **
    *******************************************************************************/
   public void setShowPossibleValueLabel(boolean showPossibleValueLabel)
   {
      this.showPossibleValueLabel = showPossibleValueLabel;
   }



   /*******************************************************************************
    ** Fluent setter for showPossibleValueLabel
    **
    *******************************************************************************/
   public QReportField withShowPossibleValueLabel(boolean showPossibleValueLabel)
   {
      this.showPossibleValueLabel = showPossibleValueLabel;
      return (this);
   }

}
