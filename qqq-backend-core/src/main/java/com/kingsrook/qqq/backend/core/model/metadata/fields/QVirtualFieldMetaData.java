/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;


/*******************************************************************************
 * Subclass of QFieldMetaData that is used for virtual fields - that is -
 * fields which don't exist in the backend system, but may instead be the result
 * of calculations or other non-stored data.
 *
 * <p>This type is expected to gain attributes in the future to enable more
 * built-in functionality, but for now it's empty.</p>
 *******************************************************************************/
public class QVirtualFieldMetaData extends QFieldMetaData implements Cloneable
{

   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QVirtualFieldMetaData clone()
   {
      return (QVirtualFieldMetaData) super.clone();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public QVirtualFieldMetaData()
   {
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public QVirtualFieldMetaData(String name, QFieldType type)
   {
      super(name, type);
   }



   /*******************************************************************************
    **
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

}
