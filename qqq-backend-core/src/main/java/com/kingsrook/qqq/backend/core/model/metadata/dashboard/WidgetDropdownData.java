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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard;


/*******************************************************************************
 ** Details about dropdown fields on a widget
 **
 *******************************************************************************/
public class WidgetDropdownData
{
   private String  name;
   private String  possibleValueSourceName;
   private String  foreignKeyFieldName;
   private String  label;
   private boolean isRequired;

   private Integer width;
   private String  startIconName;
   private Boolean allowBackAndForth;
   private Boolean backAndForthInverted;
   private Boolean disableClearable;

   ////////////////////////////////////////////////////////////////////////////////////////////////
   // an option to put at the top of the dropdown, that represents a value of "null" (e.g., All) //
   ////////////////////////////////////////////////////////////////////////////////////////////////
   private String labelForNullValue;

   private WidgetDropdownType type = WidgetDropdownType.POSSIBLE_VALUE_SOURCE;



   /*******************************************************************************
    ** Getter for possibleValueSourceName
    **
    *******************************************************************************/
   public String getPossibleValueSourceName()
   {
      return possibleValueSourceName;
   }



   /*******************************************************************************
    ** Setter for possibleValueSourceName
    **
    *******************************************************************************/
   public void setPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for possibleValueSourceName
    **
    *******************************************************************************/
   public WidgetDropdownData withPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for foreignKeyFieldName
    **
    *******************************************************************************/
   public String getForeignKeyFieldName()
   {
      return foreignKeyFieldName;
   }



   /*******************************************************************************
    ** Setter for foreignKeyFieldName
    **
    *******************************************************************************/
   public void setForeignKeyFieldName(String foreignKeyFieldName)
   {
      this.foreignKeyFieldName = foreignKeyFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for foreignKeyFieldName
    **
    *******************************************************************************/
   public WidgetDropdownData withForeignKeyFieldName(String foreignKeyFieldName)
   {
      this.foreignKeyFieldName = foreignKeyFieldName;
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
   public WidgetDropdownData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isRequired
    **
    *******************************************************************************/
   public boolean getIsRequired()
   {
      return isRequired;
   }



   /*******************************************************************************
    ** Setter for isRequired
    **
    *******************************************************************************/
   public void setIsRequired(boolean isRequired)
   {
      this.isRequired = isRequired;
   }



   /*******************************************************************************
    ** Fluent setter for isRequired
    **
    *******************************************************************************/
   public WidgetDropdownData withIsRequired(boolean isRequired)
   {
      this.isRequired = isRequired;
      return (this);
   }



   /*******************************************************************************
    ** Getter for width
    *******************************************************************************/
   public Integer getWidth()
   {
      return (this.width);
   }



   /*******************************************************************************
    ** Setter for width
    *******************************************************************************/
   public void setWidth(Integer width)
   {
      this.width = width;
   }



   /*******************************************************************************
    ** Fluent setter for width
    *******************************************************************************/
   public WidgetDropdownData withWidth(Integer width)
   {
      this.width = width;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startIconName
    *******************************************************************************/
   public String getStartIconName()
   {
      return (this.startIconName);
   }



   /*******************************************************************************
    ** Setter for startIconName
    *******************************************************************************/
   public void setStartIconName(String startIconName)
   {
      this.startIconName = startIconName;
   }



   /*******************************************************************************
    ** Fluent setter for startIconName
    *******************************************************************************/
   public WidgetDropdownData withStartIconName(String startIconName)
   {
      this.startIconName = startIconName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowBackAndForth
    *******************************************************************************/
   public Boolean getAllowBackAndForth()
   {
      return (this.allowBackAndForth);
   }



   /*******************************************************************************
    ** Setter for allowBackAndForth
    *******************************************************************************/
   public void setAllowBackAndForth(Boolean allowBackAndForth)
   {
      this.allowBackAndForth = allowBackAndForth;
   }



   /*******************************************************************************
    ** Fluent setter for allowBackAndForth
    *******************************************************************************/
   public WidgetDropdownData withAllowBackAndForth(Boolean allowBackAndForth)
   {
      this.allowBackAndForth = allowBackAndForth;
      return (this);
   }



   /*******************************************************************************
    ** Getter for disableClearable
    *******************************************************************************/
   public Boolean getDisableClearable()
   {
      return (this.disableClearable);
   }



   /*******************************************************************************
    ** Setter for disableClearable
    *******************************************************************************/
   public void setDisableClearable(Boolean disableClearable)
   {
      this.disableClearable = disableClearable;
   }



   /*******************************************************************************
    ** Fluent setter for disableClearable
    *******************************************************************************/
   public WidgetDropdownData withDisableClearable(Boolean disableClearable)
   {
      this.disableClearable = disableClearable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for labelForNullValue
    *******************************************************************************/
   public String getLabelForNullValue()
   {
      return (this.labelForNullValue);
   }



   /*******************************************************************************
    ** Setter for labelForNullValue
    *******************************************************************************/
   public void setLabelForNullValue(String labelForNullValue)
   {
      this.labelForNullValue = labelForNullValue;
   }



   /*******************************************************************************
    ** Fluent setter for labelForNullValue
    *******************************************************************************/
   public WidgetDropdownData withLabelForNullValue(String labelForNullValue)
   {
      this.labelForNullValue = labelForNullValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backAndForthInverted
    *******************************************************************************/
   public Boolean getBackAndForthInverted()
   {
      return (this.backAndForthInverted);
   }



   /*******************************************************************************
    ** Setter for backAndForthInverted
    *******************************************************************************/
   public void setBackAndForthInverted(Boolean backAndForthInverted)
   {
      this.backAndForthInverted = backAndForthInverted;
   }



   /*******************************************************************************
    ** Fluent setter for backAndForthInverted
    *******************************************************************************/
   public WidgetDropdownData withBackAndForthInverted(Boolean backAndForthInverted)
   {
      this.backAndForthInverted = backAndForthInverted;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public WidgetDropdownType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(WidgetDropdownType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public WidgetDropdownData withType(WidgetDropdownType type)
   {
      this.type = type;
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
   public WidgetDropdownData withName(String name)
   {
      this.name = name;
      return (this);
   }

}
