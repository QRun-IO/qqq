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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.hervian.reflection.Fun;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ReflectiveBeanLikeClassUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Meta-data to represent a single field in a table.
 **
 *******************************************************************************/
public class QFieldMetaData implements Cloneable, QMetaDataObject
{
   private static final QLogger LOG = QLogger.getLogger(QFieldMetaData.class);

   private String     name;
   private String     label;
   private String     backendName;
   private QFieldType type;
   private boolean    isRequired = false;
   private boolean    isEditable = true;
   private boolean    isHidden   = false;
   private boolean    isHeavy    = false;

   private FieldSecurityLock fieldSecurityLock;

   ///////////////////////////////////////////////////////////////////////////////////
   // if we need "only edit on insert" or "only edit on update" in the future,      //
   // propose doing that in a secondary field, e.g., "onlyEditableOn=insert|update" //
   ///////////////////////////////////////////////////////////////////////////////////

   private String       displayFormat = "%s";
   private Serializable defaultValue;

   private String               possibleValueSourceName;
   private QQueryFilter         possibleValueSourceFilter;
   private QPossibleValueSource inlinePossibleValueSource;

   private Integer               gridColumns;
   private Integer               maxLength;
   private Set<FieldBehavior<?>> behaviors;

   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // w/ longer-term vision for FieldBehaviors                                                                           //
   // - more enums that implement ValueTooLongBehavior e.g., NumberOutsideRangeBehavior or DecimalPrecisionErrorBehavior //
   // - QInstance.Set<FieldBehavior> defaultFieldBehaviors                                                               //
   // - QBackendMetaData.Set<FieldBehavior> defaultFieldBehaviors                                                        //
   // - QTableMetaData.Set<FieldBehavior> defaultFieldBehaviors                                                          //
   // - inherit behaviors all the way down (up?)                                                                         //
   // - instance validation to make sure you don’t specify more than 1 behavior of a given type at a given level.        //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private List<FieldAdornment> adornments;
   private List<QHelpContent>   helpContents;

   private Map<String, QSupplementalFieldMetaData> supplementalMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QFieldMetaData clone()
   {
      try
      {
         QFieldMetaData clone = (QFieldMetaData) super.clone();
         if(adornments != null)
         {
            clone.setAdornments(new ArrayList<>(adornments));
         }
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw new RuntimeException(e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData(String name, QFieldType type)
   {
      this.name = name;
      this.type = type;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("QFieldMetaData[" + name + "]");
   }



   /*******************************************************************************
    ** Initialize a fieldMetaData from a reference to a getter on an entity.
    ** e.g., new QFieldMetaData(Order::getOrderNo).
    *******************************************************************************/
   public <T> QFieldMetaData(Fun.With1ParamAndVoid<T> getterRef) throws QException
   {
      Method getter = Fun.toMethod(getterRef);
      constructFromGetter(getter);
   }



   /*******************************************************************************
    ** Initialize a fieldMetaData from a getter method from an entity
    **
    *******************************************************************************/
   public <T> QFieldMetaData(Method getter) throws QException
   {
      constructFromGetter(getter);
   }



   /*******************************************************************************
    ** From a getter method, populate attributes in this field meta-data, including
    ** those from the @QField annotation on the field in the class, if present.
    *******************************************************************************/
   private void constructFromGetter(Method getter) throws QException
   {
      try
      {
         this.name = ReflectiveBeanLikeClassUtils.getFieldNameFromGetter(getter);
         this.type = QFieldType.fromClass(getter.getReturnType());

         @SuppressWarnings("unchecked")
         Optional<QField> optionalFieldAnnotation = QRecordEntity.getQFieldAnnotation((Class<? extends QRecordEntity>) getter.getDeclaringClass(), this.name);

         if(optionalFieldAnnotation.isPresent())
         {
            QField fieldAnnotation = optionalFieldAnnotation.get();
            setIsRequired(fieldAnnotation.isRequired());
            setIsEditable(fieldAnnotation.isEditable());
            setIsHidden(fieldAnnotation.isHidden());
            setGridColumns(fieldAnnotation.gridColumns());

            if(StringUtils.hasContent(fieldAnnotation.label()))
            {
               setLabel(fieldAnnotation.label());
            }

            if(StringUtils.hasContent(fieldAnnotation.backendName()))
            {
               setBackendName(fieldAnnotation.backendName());
            }

            if(StringUtils.hasContent(fieldAnnotation.displayFormat()))
            {
               setDisplayFormat(fieldAnnotation.displayFormat());
            }

            if(StringUtils.hasContent(fieldAnnotation.possibleValueSourceName()))
            {
               setPossibleValueSourceName(fieldAnnotation.possibleValueSourceName());
            }

            if(fieldAnnotation.maxLength() != Integer.MAX_VALUE)
            {
               setMaxLength(fieldAnnotation.maxLength());
            }

            if(fieldAnnotation.valueTooLongBehavior() != ValueTooLongBehavior.values()[0].getDefault())
            {
               withBehavior(fieldAnnotation.valueTooLongBehavior());
            }

            if(fieldAnnotation.dynamicDefaultValueBehavior() != DynamicDefaultValueBehavior.values()[0].getDefault())
            {
               withBehavior(fieldAnnotation.dynamicDefaultValueBehavior());
            }

            if(StringUtils.hasContent(fieldAnnotation.defaultValue()))
            {
               withDefaultValue(ValueUtils.getValueAsFieldType(this.type, fieldAnnotation.defaultValue()));
            }
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error constructing field from getter method: " + getter.getName(), e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData withName(String name)
   {
      this.name = name;
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
    **
    *******************************************************************************/
   public QFieldMetaData withType(QFieldType type)
   {
      this.type = type;
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
    **
    *******************************************************************************/
   public QFieldMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backendName
    **
    *******************************************************************************/
   public String getBackendName()
   {
      return backendName;
   }



   /*******************************************************************************
    ** Setter for backendName
    **
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getPossibleValueSourceName()
   {
      return possibleValueSourceName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData withPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultValue
    **
    *******************************************************************************/
   public Serializable getDefaultValue()
   {
      return defaultValue;
   }



   /*******************************************************************************
    ** Setter for defaultValue
    **
    *******************************************************************************/
   public void setDefaultValue(Serializable defaultValue)
   {
      this.defaultValue = defaultValue;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData withDefaultValue(Serializable defaultValue)
   {
      this.defaultValue = defaultValue;
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
    **
    *******************************************************************************/
   public QFieldMetaData withIsRequired(boolean isRequired)
   {
      this.isRequired = isRequired;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isEditable
    **
    *******************************************************************************/
   public boolean getIsEditable()
   {
      return isEditable;
   }



   /*******************************************************************************
    ** Setter for isEditable
    **
    *******************************************************************************/
   public void setIsEditable(boolean isEditable)
   {
      this.isEditable = isEditable;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData withIsEditable(boolean isEditable)
   {
      this.isEditable = isEditable;
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
   public QFieldMetaData withDisplayFormat(String displayFormat)
   {
      this.displayFormat = displayFormat;
      return (this);
   }



   /*******************************************************************************
    ** does this field have the given addornment
    **
    *******************************************************************************/
   public boolean hasAdornmentType(AdornmentType adornmentType)
   {
      for(FieldAdornment thisAdornment : CollectionUtils.nonNullList(this.adornments))
      {
         if(AdornmentType.REVEAL.equals(thisAdornment.getType()))
         {
            return (true);
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** Getter for adornments
    **
    *******************************************************************************/
   public List<FieldAdornment> getAdornments()
   {
      return adornments;
   }



   /*******************************************************************************
    ** Getter for adornments
    **
    *******************************************************************************/
   @JsonIgnore
   public Optional<FieldAdornment> getAdornment(AdornmentType adornmentType)
   {
      if(adornmentType != null && adornments != null)
      {
         for(FieldAdornment adornment : adornments)
         {
            if(adornmentType.equals(adornment.getType()))
            {
               return Optional.of((adornment));
            }
         }
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    ** Setter for adornments
    **
    *******************************************************************************/
   public void setAdornments(List<FieldAdornment> adornments)
   {
      this.adornments = adornments;
   }



   /*******************************************************************************
    ** Fluent setter for adornments
    **
    *******************************************************************************/
   public QFieldMetaData withFieldAdornments(List<FieldAdornment> adornments)
   {
      this.adornments = adornments;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adornments
    **
    *******************************************************************************/
   public QFieldMetaData withFieldAdornment(FieldAdornment adornment)
   {
      if(this.adornments == null)
      {
         this.adornments = new ArrayList<>();
      }
      this.adornments.add(adornment);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adornments
    **
    *******************************************************************************/
   public QFieldMetaData withFieldAdornment(AdornmentType adornmentType)
   {
      if(this.adornments == null)
      {
         this.adornments = new ArrayList<>();
      }
      this.adornments.add(new FieldAdornment(adornmentType));
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxLength
    **
    *******************************************************************************/
   public Integer getMaxLength()
   {
      return maxLength;
   }



   /*******************************************************************************
    ** Setter for maxLength
    **
    *******************************************************************************/
   public void setMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
   }



   /*******************************************************************************
    ** Fluent setter for maxLength
    **
    *******************************************************************************/
   public QFieldMetaData withMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
      return (this);
   }



   /*******************************************************************************
    ** Getter for behaviors
    **
    *******************************************************************************/
   public Set<FieldBehavior<?>> getBehaviors()
   {
      return behaviors;
   }



   /*******************************************************************************
    ** Get the FieldBehavior object of a given behaviorType (class) - but - if one
    ** isn't set, then use the default from that type.
    *******************************************************************************/
   public <T extends FieldBehavior<T>> T getBehaviorOrDefault(QInstance instance, Class<T> behaviorType)
   {
      for(FieldBehavior<?> fieldBehavior : CollectionUtils.nonNullCollection(behaviors))
      {
         if(behaviorType.isInstance(fieldBehavior))
         {
            return (behaviorType.cast(fieldBehavior));
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // todo - cascade/inherit behaviors down from table, backend, instance //
      /////////////////////////////////////////////////////////////////////////

      ///////////////////////////////////////////
      // return default behavior for this type //
      ///////////////////////////////////////////
      if(behaviorType.isEnum())
      {
         return (behaviorType.getEnumConstants()[0].getDefault());
      }
      else
      {
         try
         {
            return (behaviorType.getConstructor().newInstance().getDefault());
         }
         catch(Exception e)
         {
            LOG.warn("Error getting default behaviorType for [" + behaviorType.getSimpleName() + "]", e);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Get the FieldBehavior object of a given behaviorType (class) - and if one
    ** isn't set, then return null.
    *******************************************************************************/
   public <T extends FieldBehavior<T>> T getBehaviorOnlyIfSet(Class<T> behaviorType)
   {
      if(behaviors == null)
      {
         return (null);
      }

      for(FieldBehavior<?> fieldBehavior : CollectionUtils.nonNullCollection(behaviors))
      {
         if(behaviorType.isInstance(fieldBehavior))
         {
            return (behaviorType.cast(fieldBehavior));
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Setter for behaviors
    **
    *******************************************************************************/
   public void setBehaviors(Set<FieldBehavior<?>> behaviors)
   {
      this.behaviors = behaviors;
   }



   /*******************************************************************************
    ** Fluent setter for behaviors
    **
    *******************************************************************************/
   public QFieldMetaData withBehaviors(Set<FieldBehavior<?>> behaviors)
   {
      this.behaviors = behaviors;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for behaviors
    **
    *******************************************************************************/
   public QFieldMetaData withBehavior(FieldBehavior<?> behavior)
   {
      if(behavior == null)
      {
         LOG.debug("Skipping request to add null behavior", logPair("fieldName", getName()));
         return (this);
      }

      if(behaviors == null)
      {
         behaviors = new HashSet<>();
      }

      if(!behavior.allowMultipleBehaviorsOfThisType())
      {
         @SuppressWarnings("unchecked")
         FieldBehavior<?> existingBehaviorOfThisType = getBehaviorOnlyIfSet(behavior.getClass());
         if(existingBehaviorOfThisType != null)
         {
            LOG.debug("Replacing a field behavior", logPair("fieldName", getName()), logPair("oldBehavior", existingBehaviorOfThisType), logPair("newBehavior", behavior));
            this.behaviors.remove(existingBehaviorOfThisType);
         }
      }

      this.behaviors.add(behavior);
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldSecurityLock
    *******************************************************************************/
   public FieldSecurityLock getFieldSecurityLock()
   {
      return (this.fieldSecurityLock);
   }



   /*******************************************************************************
    ** Setter for fieldSecurityLock
    *******************************************************************************/
   public void setFieldSecurityLock(FieldSecurityLock fieldSecurityLock)
   {
      this.fieldSecurityLock = fieldSecurityLock;
   }



   /*******************************************************************************
    ** Fluent setter for fieldSecurityLock
    *******************************************************************************/
   public QFieldMetaData withFieldSecurityLock(FieldSecurityLock fieldSecurityLock)
   {
      this.fieldSecurityLock = fieldSecurityLock;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isHeavy
    *******************************************************************************/
   public boolean getIsHeavy()
   {
      return (this.isHeavy);
   }



   /*******************************************************************************
    ** Setter for isHeavy
    *******************************************************************************/
   public void setIsHeavy(boolean isHeavy)
   {
      this.isHeavy = isHeavy;
   }



   /*******************************************************************************
    ** Fluent setter for isHeavy
    *******************************************************************************/
   public QFieldMetaData withIsHeavy(boolean isHeavy)
   {
      this.isHeavy = isHeavy;
      return (this);
   }



   /*******************************************************************************
    ** Getter for possibleValueSourceFilter
    *******************************************************************************/
   public QQueryFilter getPossibleValueSourceFilter()
   {
      return (this.possibleValueSourceFilter);
   }



   /*******************************************************************************
    ** Setter for possibleValueSourceFilter
    *******************************************************************************/
   public void setPossibleValueSourceFilter(QQueryFilter possibleValueSourceFilter)
   {
      this.possibleValueSourceFilter = possibleValueSourceFilter;
   }



   /*******************************************************************************
    ** Fluent setter for possibleValueSourceFilter
    *******************************************************************************/
   public QFieldMetaData withPossibleValueSourceFilter(QQueryFilter possibleValueSourceFilter)
   {
      this.possibleValueSourceFilter = possibleValueSourceFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public Map<String, QSupplementalFieldMetaData> getSupplementalMetaData()
   {
      return (this.supplementalMetaData);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public QSupplementalFieldMetaData getSupplementalMetaData(String type)
   {
      if(this.supplementalMetaData == null)
      {
         return (null);
      }
      return this.supplementalMetaData.get(type);
   }



   /*******************************************************************************
    ** Setter for supplementalMetaData
    *******************************************************************************/
   public void setSupplementalMetaData(Map<String, QSupplementalFieldMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QFieldMetaData withSupplementalMetaData(Map<String, QSupplementalFieldMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QFieldMetaData withSupplementalMetaData(QSupplementalFieldMetaData supplementalMetaData)
   {
      if(this.supplementalMetaData == null)
      {
         this.supplementalMetaData = new HashMap<>();
      }
      this.supplementalMetaData.put(supplementalMetaData.getType(), supplementalMetaData);
      return (this);
   }



   /*******************************************************************************
    ** Getter for isHidden
    *******************************************************************************/
   public boolean getIsHidden()
   {
      return (this.isHidden);
   }



   /*******************************************************************************
    ** Setter for isHidden
    *******************************************************************************/
   public void setIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
   }



   /*******************************************************************************
    ** Fluent setter for isHidden
    *******************************************************************************/
   public QFieldMetaData withIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
      return (this);
   }



   /*******************************************************************************
    ** Getter for helpContents
    *******************************************************************************/
   public List<QHelpContent> getHelpContents()
   {
      return (this.helpContents);
   }



   /*******************************************************************************
    ** Setter for helpContents
    *******************************************************************************/
   public void setHelpContents(List<QHelpContent> helpContents)
   {
      this.helpContents = helpContents;
   }



   /*******************************************************************************
    ** Fluent setter for helpContents
    *******************************************************************************/
   public QFieldMetaData withHelpContents(List<QHelpContent> helpContents)
   {
      this.helpContents = helpContents;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adding 1 helpContent
    *******************************************************************************/
   public QFieldMetaData withHelpContent(QHelpContent helpContent)
   {
      if(this.helpContents == null)
      {
         this.helpContents = new ArrayList<>();
      }

      QInstanceHelpContentManager.putHelpContentInList(helpContent, this.helpContents);
      return (this);
   }



   /*******************************************************************************
    ** remove a single helpContent based on its set of roles
    *******************************************************************************/
   public void removeHelpContent(Set<HelpRole> roles)
   {
      QInstanceHelpContentManager.removeHelpContentByRoleSetFromList(roles, this.helpContents);
   }



   /*******************************************************************************
    ** Getter for inlinePossibleValueSource
    *******************************************************************************/
   public QPossibleValueSource getInlinePossibleValueSource()
   {
      return (this.inlinePossibleValueSource);
   }



   /*******************************************************************************
    ** Setter for inlinePossibleValueSource
    *******************************************************************************/
   public void setInlinePossibleValueSource(QPossibleValueSource inlinePossibleValueSource)
   {
      this.inlinePossibleValueSource = inlinePossibleValueSource;
   }



   /*******************************************************************************
    ** Fluent setter for inlinePossibleValueSource
    *******************************************************************************/
   public QFieldMetaData withInlinePossibleValueSource(QPossibleValueSource inlinePossibleValueSource)
   {
      this.inlinePossibleValueSource = inlinePossibleValueSource;
      return (this);
   }



   /*******************************************************************************
    ** Getter for gridColumns
    *******************************************************************************/
   public Integer getGridColumns()
   {
      return (this.gridColumns);
   }



   /*******************************************************************************
    ** Setter for gridColumns
    *******************************************************************************/
   public void setGridColumns(Integer gridColumns)
   {
      this.gridColumns = gridColumns;
   }



   /*******************************************************************************
    ** Fluent setter for gridColumns
    *******************************************************************************/
   public QFieldMetaData withGridColumns(Integer gridColumns)
   {
      this.gridColumns = gridColumns;
      return (this);
   }

}
