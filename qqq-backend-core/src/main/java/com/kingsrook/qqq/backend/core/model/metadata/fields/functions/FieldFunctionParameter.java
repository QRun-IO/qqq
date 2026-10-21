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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/***************************************************************************
 * Describes a single parameter accepted by a {@link FieldFunctionType}.
 *
 * <p>Each parameter has a name used as its key in the arguments map, a declared
 * QFieldType, a flag indicating whether it is required, and an optional default
 * value used when the argument is not explicitly supplied.</p>
 ***************************************************************************/
public class FieldFunctionParameter
{
   private String       name;
   private QFieldType   type;
   private boolean      isRequired = true;
   private Serializable defaultValue;



   /*******************************************************************************
    * Getter for name
    * @see #withName(String)
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    * Setter for name
    * @see #withName(String)
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    * Fluent setter for name
    *
    * @param name
    * The parameter's name, used as the key in the FieldFunction arguments map.
    * @return this
    *******************************************************************************/
   public FieldFunctionParameter withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    * Getter for type
    * @see #withType(QFieldType)
    *******************************************************************************/
   public QFieldType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    * Setter for type
    * @see #withType(QFieldType)
    *******************************************************************************/
   public void setType(QFieldType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    * Fluent setter for type
    *
    * @param type
    * The expected QFieldType of this parameter's value.
    * @return this
    *******************************************************************************/
   public FieldFunctionParameter withType(QFieldType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    * Getter for isRequired
    * @see #withIsRequired(boolean)
    *******************************************************************************/
   public boolean getIsRequired()
   {
      return (this.isRequired);
   }



   /*******************************************************************************
    * Setter for isRequired
    * @see #withIsRequired(boolean)
    *******************************************************************************/
   public void setIsRequired(boolean isRequired)
   {
      this.isRequired = isRequired;
   }



   /*******************************************************************************
    * Fluent setter for isRequired
    *
    * @param isRequired
    * Whether this parameter must be explicitly provided; defaults to true.
    * @return this
    *******************************************************************************/
   public FieldFunctionParameter withIsRequired(boolean isRequired)
   {
      this.isRequired = isRequired;
      return (this);
   }



   /*******************************************************************************
    * Getter for defaultValue
    * @see #withDefaultValue(Serializable)
    *******************************************************************************/
   public Serializable getDefaultValue()
   {
      return (this.defaultValue);
   }



   /*******************************************************************************
    * Setter for defaultValue
    * @see #withDefaultValue(Serializable)
    *******************************************************************************/
   public void setDefaultValue(Serializable defaultValue)
   {
      this.defaultValue = defaultValue;
   }



   /*******************************************************************************
    * Fluent setter for defaultValue
    *
    * @param defaultValue
    * The value to use when this parameter is not supplied in the FieldFunction arguments.
    * @return this
    *******************************************************************************/
   public FieldFunctionParameter withDefaultValue(Serializable defaultValue)
   {
      this.defaultValue = defaultValue;
      return (this);
   }

}
