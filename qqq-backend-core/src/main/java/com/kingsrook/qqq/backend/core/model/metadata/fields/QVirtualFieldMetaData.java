/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;


/*******************************************************************************
 * Subclass of QFieldMetaData that is used for virtual fields - that is -
 * fields which don't exist in the backend system, but may instead be the result
 * of calculations or other non-stored data.
 *
 * <p>Optionally carries a {@link FieldFunction} that specifies a transformation
 * applied to a source field's value (e.g., extracting the weekday from a date).</p>
 *
 * <p>The {@code isQueryCriteria} and {@code isQuerySelectable} flags control whether
 * the virtual field may be used as a filter criterion and if it is included in query
 * output, respectively.</p>
 *
 * <p>For a field to be {@code isQueryCriteria}, it MUST use a {@code fieldFunction}.
 * However, to be {@code isQuerySelectable}, it MAY either use a  {@code fieldFunction},
 * or alternatively, its table can have a {@link TableCustomizers#POST_QUERY_RECORD}.</p>
 *******************************************************************************/
public class QVirtualFieldMetaData extends QFieldMetaData implements Cloneable
{
   private FieldFunction fieldFunction;

   private boolean isQueryCriteria   = false;
   private boolean isQuerySelectable = false;


   /***************************************************************************
    * Returns a clone of this virtual field metadata.
    ***************************************************************************/
   @Override
   public QVirtualFieldMetaData clone()
   {
      return (QVirtualFieldMetaData) super.clone();
   }



   /***************************************************************************
    * Default no-arg constructor.
    ***************************************************************************/
   public QVirtualFieldMetaData()
   {
   }



   /***************************************************************************
    * Constructs a QVirtualFieldMetaData with the given field name and type.
    ***************************************************************************/
   public QVirtualFieldMetaData(String name, QFieldType type)
   {
      super(name, type);
   }



   /*******************************************************************************
    * Fluent setter for name; overridden to return {@link QVirtualFieldMetaData}
    * for method chaining.
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withName(String name)
   {
      super.withName(name);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withType(QFieldType type)
   {
      super.withType(type);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withLabel(String label)
   {
      super.withLabel(label);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withBackendName(String backendName)
   {
      super.withBackendName(backendName);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withPossibleValueSourceName(String possibleValueSourceName)
   {
      super.withPossibleValueSourceName(possibleValueSourceName);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withDefaultValue(Serializable defaultValue)
   {
      super.withDefaultValue(defaultValue);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withIsRequired(boolean isRequired)
   {
      super.withIsRequired(isRequired);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withIsEditable(boolean isEditable)
   {
      super.withIsEditable(isEditable);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withDisplayFormat(String displayFormat)
   {
      super.withDisplayFormat(displayFormat);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withFieldAdornments(List<FieldAdornment> adornments)
   {
      super.withFieldAdornments(adornments);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withFieldAdornment(FieldAdornment adornment)
   {
      super.withFieldAdornment(adornment);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withFieldAdornment(AdornmentType adornmentType)
   {
      super.withFieldAdornment(adornmentType);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withMaxLength(Integer maxLength)
   {
      super.withMaxLength(maxLength);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withBehaviors(Set<FieldBehavior<?>> behaviors)
   {
      super.withBehaviors(behaviors);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withBehavior(FieldBehavior<?> behavior)
   {
      super.withBehavior(behavior);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withFieldSecurityLock(FieldSecurityLock fieldSecurityLock)
   {
      super.withFieldSecurityLock(fieldSecurityLock);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withIsHeavy(boolean isHeavy)
   {
      super.withIsHeavy(isHeavy);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withPossibleValueSourceFilter(QQueryFilter possibleValueSourceFilter)
   {
      super.withPossibleValueSourceFilter(possibleValueSourceFilter);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withSupplementalMetaData(Map<String, QSupplementalFieldMetaData> supplementalMetaData)
   {
      super.withSupplementalMetaData(supplementalMetaData);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withSupplementalMetaData(QSupplementalFieldMetaData supplementalMetaData)
   {
      super.withSupplementalMetaData(supplementalMetaData);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withIsHidden(boolean isHidden)
   {
      super.withIsHidden(isHidden);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withHelpContents(List<QHelpContent> helpContents)
   {
      super.withHelpContents(helpContents);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withHelpContent(QHelpContent helpContent)
   {
      super.withHelpContent(helpContent);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withInlinePossibleValueSource(QPossibleValueSource inlinePossibleValueSource)
   {
      super.withInlinePossibleValueSource(inlinePossibleValueSource);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QVirtualFieldMetaData withGridColumns(Integer gridColumns)
   {
      super.withGridColumns(gridColumns);
      return (this);
   }



   /*******************************************************************************
    * Getter for fieldFunction
    * @see #withFieldFunction(FieldFunction)
    *******************************************************************************/
   public FieldFunction getFieldFunction()
   {
      return (this.fieldFunction);
   }



   /*******************************************************************************
    * Setter for fieldFunction
    * @see #withFieldFunction(FieldFunction)
    *******************************************************************************/
   public void setFieldFunction(FieldFunction fieldFunction)
   {
      this.fieldFunction = fieldFunction;
   }



   /*******************************************************************************
    * Fluent setter for fieldFunction
    *
    * @param fieldFunction
    * The FieldFunction that transforms a source field's value to produce this
    * virtual field's value.
    * @return this
    *******************************************************************************/
   public QVirtualFieldMetaData withFieldFunction(FieldFunction fieldFunction)
   {
      this.fieldFunction = fieldFunction;
      return (this);
   }



   /*******************************************************************************
    * Getter for isQueryCriteria
    * @see #withIsQueryCriteria(boolean)
    *******************************************************************************/
   public boolean getIsQueryCriteria()
   {
      return (this.isQueryCriteria);
   }



   /*******************************************************************************
    * Setter for isQueryCriteria
    * @see #withIsQueryCriteria(boolean)
    *******************************************************************************/
   public void setIsQueryCriteria(boolean isQueryCriteria)
   {
      this.isQueryCriteria = isQueryCriteria;
   }



   /*******************************************************************************
    * Fluent setter for isQueryCriteria
    *
    * @param isQueryCriteria
    * Whether this virtual field may be used as a filter criterion in queries;
    * defaults to false.
    * @return this
    *******************************************************************************/
   public QVirtualFieldMetaData withIsQueryCriteria(boolean isQueryCriteria)
   {
      this.isQueryCriteria = isQueryCriteria;
      return (this);
   }



   /*******************************************************************************
    * Getter for isQuerySelectable
    * @see #withIsQuerySelectable(boolean)
    *******************************************************************************/
   public boolean getIsQuerySelectable()
   {
      return (this.isQuerySelectable);
   }



   /*******************************************************************************
    * Setter for isQuerySelectable
    * @see #withIsQuerySelectable(boolean)
    *******************************************************************************/
   public void setIsQuerySelectable(boolean isQuerySelectable)
   {
      this.isQuerySelectable = isQuerySelectable;
   }



   /*******************************************************************************
    * Fluent setter for isQuerySelectable
    *
    * @param isQuerySelectable
    * Whether this virtual field may be included in query output (i.e., selected);
    * defaults to false.
    * @return this
    *******************************************************************************/
   public QVirtualFieldMetaData withIsQuerySelectable(boolean isQuerySelectable)
   {
      this.isQuerySelectable = isQuerySelectable;
      return (this);
   }


}
