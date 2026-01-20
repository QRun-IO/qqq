/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntityField;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenu;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Meta-Data to define a table in a QQQ instance.
 **
 *******************************************************************************/
public class QTableMetaData implements QAppChildMetaData, Serializable, MetaDataWithPermissionRules, TopLevelMetaDataInterface, SourceQBitAware, Cloneable
{
   private static final QLogger LOG = QLogger.getLogger(QTableMetaData.class);

   private String name;
   private String label;

   private String  backendName;
   private String  primaryKeyField;
   private boolean isHidden = false;

   private String sourceQBitName;

   private Map<String, QFieldMetaData> fields;
   private List<UniqueKey>             uniqueKeys;
   private List<Association>           associations;

   private Map<String, QVirtualFieldMetaData> virtualFields;

   private List<RecordSecurityLock> recordSecurityLocks;
   private QPermissionRules         permissionRules;
   private QAuditRules              auditRules;

   private QTableBackendDetails    backendDetails;
   private QTableAutomationDetails automationDetails;

   private Map<String, QCodeReference> customizers;

   private QIcon icon;

   private String       recordLabelFormat;
   private List<String> recordLabelFields;

   private List<QFieldSection> sections;

   private List<AssociatedScript> associatedScripts;

   private Set<Capability> enabledCapabilities  = new HashSet<>();
   private Set<Capability> disabledCapabilities = new HashSet<>();

   private CacheOf cacheOf;

   private Map<String, QSupplementalTableMetaData> supplementalMetaData;

   private List<ExposedJoin> exposedJoins;

   private ShareableTableMetaData shareableTableMetaData;

   protected Map<String, List<QHelpContent>> helpContent;

   private List<QMenu> menus;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public QTableMetaData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("QTableMetaData[" + name + "]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData getField(String fieldName)
   {
      if(fields == null)
      {
         throw (new IllegalArgumentException("Table [" + name + "] does not have its fields defined."));
      }

      QFieldMetaData field = getFields().get(fieldName);
      if(field == null)
      {
         throw (new IllegalArgumentException("Field [" + fieldName + "] was not found in table [" + name + "]."));
      }

      return (field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QVirtualFieldMetaData getVirtualField(String fieldName)
   {
      if(virtualFields == null)
      {
         return (null);
      }

      QVirtualFieldMetaData virtualField = getVirtualFields().get(fieldName);
      return (virtualField);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withFieldsFromEntity(Class<? extends QRecordEntity> entityClass) throws QException
   {
      List<QRecordEntityField> recordEntityFieldList = QRecordEntity.getFieldList(entityClass);

      boolean setPrimaryKey = false;

      for(QRecordEntityField recordEntityField : recordEntityFieldList)
      {
         QFieldMetaData field = new QFieldMetaData(recordEntityField.getGetter());
         addField(field);

         if(recordEntityField.getFieldAnnotation().isPrimaryKey())
         {
            if(setPrimaryKey)
            {
               throw (new QException("Attempt to set more than one field as primary key (" + primaryKeyField + "," + field.getName() + ")."));
            }

            setPrimaryKeyField(field.getName());
            setPrimaryKey = true;
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // stash a reference from this entityClass to this table in the QRecordEntity class              //
      // (used within that class later, if it wants to know about a table that an Entity helped build) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      QRecordEntity.registerTable(entityClass, this);

      return (this);
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
   public QTableMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withLabel(String label)
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
   public QTableMetaData withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getPrimaryKeyField()
   {
      return primaryKeyField;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setPrimaryKeyField(String primaryKeyField)
   {
      this.primaryKeyField = primaryKeyField;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withPrimaryKeyField(String primaryKeyField)
   {
      this.primaryKeyField = primaryKeyField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isHidden
    **
    *******************************************************************************/
   public boolean getIsHidden()
   {
      return (isHidden);
   }



   /*******************************************************************************
    ** Setter for isHidden
    **
    *******************************************************************************/
   public void setIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
   }



   /*******************************************************************************
    ** Fluent Setter for isHidden
    **
    *******************************************************************************/
   public QTableMetaData withIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, QFieldMetaData> getFields()
   {
      return fields;
   }



   /*******************************************************************************
    ** Setter for fields
    **
    *******************************************************************************/
   public void setFields(Map<String, QFieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withFields(Map<String, QFieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withFields(List<QFieldMetaData> fields)
   {
      this.fields = new LinkedHashMap<>();
      for(QFieldMetaData field : fields)
      {
         this.addField(field);
      }
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addField(QFieldMetaData field)
   {
      if(this.fields == null)
      {
         this.fields = new LinkedHashMap<>();
      }

      if(this.fields.containsKey(field.getName()))
      {
         throw (new IllegalArgumentException("Attempt to add a second field with name [" + field.getName() + "] to table [" + name + "]."));
      }

      this.fields.put(field.getName(), field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withField(QFieldMetaData field)
   {
      if(this.fields == null)
      {
         this.fields = new LinkedHashMap<>();
      }
      this.fields.put(field.getName(), field);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withVirtualFields(List<QVirtualFieldMetaData> virtualFields)
   {
      this.virtualFields = new LinkedHashMap<>();
      for(QVirtualFieldMetaData virtualField : virtualFields)
      {
         this.addVirtualField(virtualField);
      }
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addVirtualField(QVirtualFieldMetaData virtualField)
   {
      if(this.virtualFields == null)
      {
         this.virtualFields = new LinkedHashMap<>();
      }

      if(this.virtualFields.containsKey(virtualField.getName()))
      {
         throw (new IllegalArgumentException("Attempt to add a second virtualField with name [" + virtualField.getName() + "] to table [" + name + "]."));
      }

      this.virtualFields.put(virtualField.getName(), virtualField);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withVirtualField(QVirtualFieldMetaData virtualField)
   {
      if(this.virtualFields == null)
      {
         this.virtualFields = new LinkedHashMap<>();
      }
      this.virtualFields.put(virtualField.getName(), virtualField);
      return (this);
   }



   /*******************************************************************************
    ** Getter for backendDetails
    **
    *******************************************************************************/
   public QTableBackendDetails getBackendDetails()
   {
      return backendDetails;
   }



   /*******************************************************************************
    ** Setter for backendDetails
    **
    *******************************************************************************/
   public void setBackendDetails(QTableBackendDetails backendDetails)
   {
      this.backendDetails = backendDetails;
   }



   /*******************************************************************************
    ** Fluent Setter for backendDetails
    **
    *******************************************************************************/
   public QTableMetaData withBackendDetails(QTableBackendDetails backendDetails)
   {
      this.backendDetails = backendDetails;
      return (this);
   }



   /*******************************************************************************
    ** Getter for automationDetails
    **
    *******************************************************************************/
   public QTableAutomationDetails getAutomationDetails()
   {
      return automationDetails;
   }



   /*******************************************************************************
    ** Setter for automationDetails
    **
    *******************************************************************************/
   public void setAutomationDetails(QTableAutomationDetails automationDetails)
   {
      this.automationDetails = automationDetails;
   }



   /*******************************************************************************
    ** Fluent Setter for automationDetails
    **
    *******************************************************************************/
   public QTableMetaData withAutomationDetails(QTableAutomationDetails automationDetails)
   {
      this.automationDetails = automationDetails;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Optional<QCodeReference> getCustomizer(String customizerName)
   {
      if(customizers == null)
      {
         return (Optional.empty());
      }

      QCodeReference function = customizers.get(customizerName);
      return (Optional.ofNullable(function));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, QCodeReference> getCustomizers()
   {
      return customizers;
   }



   /*******************************************************************************
    ** Setter for customizers
    **
    *******************************************************************************/
   public void setCustomizers(Map<String, QCodeReference> customizers)
   {
      this.customizers = customizers;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withCustomizer(String role, QCodeReference customizer)
   {
      if(this.customizers == null)
      {
         this.customizers = new HashMap<>();
      }

      if(this.customizers.containsKey(role))
      {
         throw (new IllegalArgumentException("Attempt to add a second customizer with role [" + role + "] to table [" + name + "]."));
      }
      this.customizers.put(role, customizer);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withCustomizer(TableCustomizers tableCustomizer, QCodeReference customizer)
   {
      return (withCustomizer(tableCustomizer.getRole(), customizer));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withCustomizers(Map<String, QCodeReference> customizers)
   {
      this.customizers = customizers;
      return (this);
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public QIcon getIcon()
   {
      return icon;
   }



   /*******************************************************************************
    ** Setter for icon
    **
    *******************************************************************************/
   public void setIcon(QIcon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public QTableMetaData withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordLabelFormat
    **
    *******************************************************************************/
   public String getRecordLabelFormat()
   {
      return recordLabelFormat;
   }



   /*******************************************************************************
    ** Setter for recordLabelFormat
    **
    *******************************************************************************/
   public void setRecordLabelFormat(String recordLabelFormat)
   {
      this.recordLabelFormat = recordLabelFormat;
   }



   /*******************************************************************************
    ** Fluent setter for recordLabelFormat
    **
    *******************************************************************************/
   public QTableMetaData withRecordLabelFormat(String recordLabelFormat)
   {
      this.recordLabelFormat = recordLabelFormat;
      return (this);
   }



   /*******************************************************************************
    ** fluent setter for both recordLabelFormat and recordLabelFields
    *******************************************************************************/
   public QTableMetaData withRecordLabelFormatAndFields(String format, String... fields)
   {
      setRecordLabelFormat(format);
      setRecordLabelFields(Arrays.asList(fields));
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordLabelFields
    **
    *******************************************************************************/
   public List<String> getRecordLabelFields()
   {
      return recordLabelFields;
   }



   /*******************************************************************************
    ** Setter for recordLabelFields
    **
    *******************************************************************************/
   public void setRecordLabelFields(List<String> recordLabelFields)
   {
      this.recordLabelFields = recordLabelFields;
   }



   /*******************************************************************************
    ** Fluent setter for recordLabelFields
    **
    *******************************************************************************/
   public QTableMetaData withRecordLabelFields(List<String> recordLabelFields)
   {
      this.recordLabelFields = recordLabelFields;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for recordLabelFields
    **
    *******************************************************************************/
   public QTableMetaData withRecordLabelFields(String... recordLabelFields)
   {
      this.recordLabelFields = Arrays.asList(recordLabelFields);
      return (this);
   }



   /*******************************************************************************
    ** Getter for sections
    **
    *******************************************************************************/
   public List<QFieldSection> getSections()
   {
      return sections;
   }



   /*******************************************************************************
    ** Getter for sections
    **
    *******************************************************************************/
   public QFieldSection getSection(String name)
   {
      for(QFieldSection qFieldSection : CollectionUtils.nonNullList(sections))
      {
         if(qFieldSection.getName().equals(name))
         {
            return (qFieldSection);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Setter for sections
    **
    *******************************************************************************/
   public void setSections(List<QFieldSection> sections)
   {
      this.sections = sections;
   }



   /*******************************************************************************
    ** Fluent setter for sections
    **
    *******************************************************************************/
   public QTableMetaData withSections(List<QFieldSection> fieldSections)
   {
      this.sections = fieldSections;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addSection(QFieldSection fieldSection)
   {
      if(this.sections == null)
      {
         this.sections = new ArrayList<>();
      }
      this.sections.add(fieldSection);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData withSection(QFieldSection fieldSection)
   {
      addSection(fieldSection);
      return (this);
   }



   /*******************************************************************************
    ** Getter for associatedScripts
    **
    *******************************************************************************/
   public List<AssociatedScript> getAssociatedScripts()
   {
      return associatedScripts;
   }



   /*******************************************************************************
    ** Setter for associatedScripts
    **
    *******************************************************************************/
   public void setAssociatedScripts(List<AssociatedScript> associatedScripts)
   {
      this.associatedScripts = associatedScripts;
   }



   /*******************************************************************************
    ** Fluent setter for associatedScripts
    **
    *******************************************************************************/
   public QTableMetaData withAssociatedScripts(List<AssociatedScript> associatedScripts)
   {
      this.associatedScripts = associatedScripts;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for associatedScripts
    **
    *******************************************************************************/
   public QTableMetaData withAssociatedScript(AssociatedScript associatedScript)
   {
      if(this.associatedScripts == null)
      {
         this.associatedScripts = new ArrayList<>();
      }
      this.associatedScripts.add(associatedScript);
      return (this);
   }



   /*******************************************************************************
    ** Getter for uniqueKeys
    **
    *******************************************************************************/
   public List<UniqueKey> getUniqueKeys()
   {
      return uniqueKeys;
   }



   /*******************************************************************************
    ** Setter for uniqueKeys
    **
    *******************************************************************************/
   public void setUniqueKeys(List<UniqueKey> uniqueKeys)
   {
      this.uniqueKeys = uniqueKeys;
   }



   /*******************************************************************************
    ** Fluent setter for uniqueKeys
    **
    *******************************************************************************/
   public QTableMetaData withUniqueKeys(List<UniqueKey> uniqueKeys)
   {
      this.uniqueKeys = uniqueKeys;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for uniqueKeys
    **
    *******************************************************************************/
   public QTableMetaData withUniqueKey(UniqueKey uniqueKey)
   {
      ////////////////////////////////////////////////////////////////////////////////////
      // you can't add a null key, so, if someone tried, just gracefully return w/ noop //
      ////////////////////////////////////////////////////////////////////////////////////
      if(uniqueKey == null)
      {
         LOG.debug("Skipping request to add null uniqueKey", logPair("tableName", name));
         return (this);
      }

      if(this.uniqueKeys == null)
      {
         this.uniqueKeys = new ArrayList<>();
      }
      this.uniqueKeys.add(uniqueKey);
      return (this);
   }



   /*******************************************************************************
    ** Fluently add a section and fields in that section.
    *******************************************************************************/
   public QTableMetaData withSectionOfFields(QFieldSection fieldSection, QFieldMetaData... fields)
   {
      withSection(fieldSection);

      List<String> fieldNames = new ArrayList<>();
      for(QFieldMetaData field : fields)
      {
         withField(field);
         fieldNames.add(field.getName());
      }

      fieldSection.setFieldNames(fieldNames);

      return (this);
   }



   /*******************************************************************************
    ** Getter for enabledCapabilities
    **
    *******************************************************************************/
   public Set<Capability> getEnabledCapabilities()
   {
      return enabledCapabilities;
   }



   /*******************************************************************************
    ** Setter for enabledCapabilities
    **
    *******************************************************************************/
   public void setEnabledCapabilities(Set<Capability> enabledCapabilities)
   {
      this.enabledCapabilities = enabledCapabilities;
      if(this.disabledCapabilities != null)
      {
         try
         {
            this.disabledCapabilities.removeAll(enabledCapabilities);
         }
         catch(UnsupportedOperationException e)
         {
            this.disabledCapabilities = new HashSet<>(this.disabledCapabilities);
            this.disabledCapabilities.removeAll(enabledCapabilities);
         }
      }
   }



   /*******************************************************************************
    ** Fluent setter for enabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withEnabledCapabilities(Set<Capability> enabledCapabilities)
   {
      setEnabledCapabilities(enabledCapabilities);
      return (this);
   }



   /*******************************************************************************
    ** Alternative fluent setter for enabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withCapabilities(Set<Capability> enabledCapabilities)
   {
      for(Capability enabledCapability : enabledCapabilities)
      {
         withCapability(enabledCapability);
      }
      return (this);
   }



   /*******************************************************************************
    ** Alternative fluent setter for a single enabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withCapability(Capability capability)
   {
      if(this.enabledCapabilities == null)
      {
         this.enabledCapabilities = new HashSet<>();
      }
      this.enabledCapabilities.add(capability);
      this.disabledCapabilities.remove(capability);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for enabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withCapabilities(Capability... enabledCapabilities)
   {
      for(Capability enabledCapability : enabledCapabilities)
      {
         withCapability(enabledCapability);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for disabledCapabilities
    **
    *******************************************************************************/
   public Set<Capability> getDisabledCapabilities()
   {
      return disabledCapabilities;
   }



   /*******************************************************************************
    ** Setter for disabledCapabilities
    **
    *******************************************************************************/
   public void setDisabledCapabilities(Set<Capability> disabledCapabilities)
   {
      this.disabledCapabilities = disabledCapabilities;
      if(this.enabledCapabilities != null)
      {
         try
         {
            this.enabledCapabilities.removeAll(disabledCapabilities);
         }
         catch(UnsupportedOperationException e)
         {
            this.enabledCapabilities = new HashSet<>(this.enabledCapabilities);
            this.enabledCapabilities.removeAll(disabledCapabilities);
         }
      }
   }



   /*******************************************************************************
    ** Fluent setter for disabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withDisabledCapabilities(Set<Capability> disabledCapabilities)
   {
      setDisabledCapabilities(disabledCapabilities);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for disabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withoutCapabilities(Capability... disabledCapabilities)
   {
      for(Capability disabledCapability : disabledCapabilities)
      {
         withoutCapability(disabledCapability);
      }
      return (this);
   }



   /*******************************************************************************
    ** Alternative fluent setter for disabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withoutCapabilities(Set<Capability> disabledCapabilities)
   {
      for(Capability disabledCapability : disabledCapabilities)
      {
         withoutCapability(disabledCapability);
      }
      return (this);
   }



   /*******************************************************************************
    ** Alternative fluent setter for a single disabledCapabilities
    **
    *******************************************************************************/
   public QTableMetaData withoutCapability(Capability capability)
   {
      if(this.disabledCapabilities == null)
      {
         this.disabledCapabilities = new HashSet<>();
      }
      this.disabledCapabilities.add(capability);
      this.enabledCapabilities.remove(capability);
      return (this);
   }



   /*******************************************************************************
    ** Getter for cacheOf
    **
    *******************************************************************************/
   public CacheOf getCacheOf()
   {
      return cacheOf;
   }



   /*******************************************************************************
    ** Setter for cacheOf
    **
    *******************************************************************************/
   public void setCacheOf(CacheOf cacheOf)
   {
      this.cacheOf = cacheOf;
   }



   /*******************************************************************************
    ** Fluent setter for cacheOf
    **
    *******************************************************************************/
   public QTableMetaData withCacheOf(CacheOf cacheOf)
   {
      this.cacheOf = cacheOf;
      return (this);
   }



   /*******************************************************************************
    ** Test if a capability is enabled - checking both at the table level and
    ** at the backend level.
    **
    ** If backend says disabled, then disable - UNLESS - the table says enable.
    ** If backend either doesn't specify, or says enable, return what the table says (if it says).
    ** else, return the default (of enabled).
    *******************************************************************************/
   public boolean isCapabilityEnabled(QBackendMetaData backend, Capability capability)
   {
      ///////////////////////////////////////////////
      // by default, every table can do everything //
      ///////////////////////////////////////////////
      boolean hasCapability = true;

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the table's backend says the capability is disabled, then by default, then the capability is disabled... //
      // (or if the backend isn't given, then it doesn't say one way or the other, so just check the table)          //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(backend != null && backend.getDisabledCapabilities().contains(capability))
      {
         hasCapability = false;

         /////////////////////////////////////////////////////////////////
         // unless the table overrides that and says that it IS enabled //
         /////////////////////////////////////////////////////////////////
         if(getEnabledCapabilities().contains(capability))
         {
            hasCapability = true;
         }
      }
      else
      {
         /////////////////////////////////////////////////////////////////////////////////////////
         // if the backend doesn't specify the capability, then disable it if the table says so //
         /////////////////////////////////////////////////////////////////////////////////////////
         if(getDisabledCapabilities().contains(capability))
         {
            hasCapability = false;
         }
      }

      return (hasCapability);
   }



   /*******************************************************************************
    ** Getter for recordSecurityLocks
    *******************************************************************************/
   public List<RecordSecurityLock> getRecordSecurityLocks()
   {
      return (this.recordSecurityLocks);
   }



   /*******************************************************************************
    ** Setter for recordSecurityLocks
    *******************************************************************************/
   public void setRecordSecurityLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      this.recordSecurityLocks = recordSecurityLocks;
   }



   /*******************************************************************************
    ** Fluent setter for recordSecurityLocks
    *******************************************************************************/
   public QTableMetaData withRecordSecurityLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      this.recordSecurityLocks = recordSecurityLocks;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for recordSecurityLocks
    *******************************************************************************/
   public QTableMetaData withRecordSecurityLock(RecordSecurityLock recordSecurityLock)
   {
      /////////////////////////////////////////////////////////////////////////////////////
      // you can't add a null lock, so, if someone tried, just gracefully return w/ noop //
      /////////////////////////////////////////////////////////////////////////////////////
      if(recordSecurityLock == null)
      {
         LOG.debug("Skipping request to add null recordSecurityLock", logPair("tableName", name));
         return (this);
      }

      if(this.recordSecurityLocks == null)
      {
         this.recordSecurityLocks = new ArrayList<>();
      }
      this.recordSecurityLocks.add(recordSecurityLock);
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionRules
    *******************************************************************************/
   public QPermissionRules getPermissionRules()
   {
      return (this.permissionRules);
   }



   /*******************************************************************************
    ** Setter for permissionRules
    *******************************************************************************/
   public void setPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
   }



   /*******************************************************************************
    ** Fluent setter for permissionRules
    *******************************************************************************/
   public QTableMetaData withPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
      return (this);
   }



   /*******************************************************************************
    ** Getter for auditRules
    *******************************************************************************/
   public QAuditRules getAuditRules()
   {
      return (this.auditRules);
   }



   /*******************************************************************************
    ** Setter for auditRules
    *******************************************************************************/
   public void setAuditRules(QAuditRules auditRules)
   {
      this.auditRules = auditRules;
   }



   /*******************************************************************************
    ** Fluent setter for auditRules
    *******************************************************************************/
   public QTableMetaData withAuditRules(QAuditRules auditRules)
   {
      this.auditRules = auditRules;
      return (this);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public Map<String, QSupplementalTableMetaData> getSupplementalMetaData()
   {
      return (this.supplementalMetaData);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public QSupplementalTableMetaData getSupplementalMetaData(String type)
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
   public void setSupplementalMetaData(Map<String, QSupplementalTableMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QTableMetaData withSupplementalMetaData(Map<String, QSupplementalTableMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QTableMetaData withSupplementalMetaData(QSupplementalTableMetaData supplementalMetaData)
   {
      if(this.supplementalMetaData == null)
      {
         this.supplementalMetaData = new HashMap<>();
      }
      this.supplementalMetaData.put(supplementalMetaData.getType(), supplementalMetaData);
      return (this);
   }



   /*******************************************************************************
    ** Getter for associations
    *******************************************************************************/
   public List<Association> getAssociations()
   {
      return (this.associations);
   }



   /*******************************************************************************
    ** Getter for an association by name
    *******************************************************************************/
   public Optional<Association> getAssociationByName(String name)
   {
      if(associations == null)
      {
         return (Optional.empty());
      }

      return (getAssociations().stream().filter(a -> a.getName().equals(name)).findFirst());
   }



   /*******************************************************************************
    ** Setter for associations
    *******************************************************************************/
   public void setAssociations(List<Association> associations)
   {
      this.associations = associations;
   }



   /*******************************************************************************
    ** Fluent setter for associations
    *******************************************************************************/
   public QTableMetaData withAssociations(List<Association> associations)
   {
      this.associations = associations;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for associations
    *******************************************************************************/
   public QTableMetaData withAssociation(Association association)
   {
      if(this.associations == null)
      {
         this.associations = new ArrayList<>();
      }
      this.associations.add(association);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addTable(this);
   }



   /*******************************************************************************
    ** Getter for exposedJoins
    *******************************************************************************/
   public List<ExposedJoin> getExposedJoins()
   {
      return (this.exposedJoins);
   }



   /*******************************************************************************
    ** Setter for exposedJoins
    *******************************************************************************/
   public void setExposedJoins(List<ExposedJoin> exposedJoins)
   {
      this.exposedJoins = exposedJoins;
   }



   /*******************************************************************************
    ** Fluent setter for exposedJoins
    *******************************************************************************/
   public QTableMetaData withExposedJoins(List<ExposedJoin> exposedJoins)
   {
      this.exposedJoins = exposedJoins;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for exposedJoins
    *******************************************************************************/
   public QTableMetaData withExposedJoin(ExposedJoin exposedJoin)
   {
      if(this.exposedJoins == null)
      {
         this.exposedJoins = new ArrayList<>();
      }
      this.exposedJoins.add(exposedJoin);
      return (this);
   }



   /*******************************************************************************
    ** Getter for shareableTableMetaData
    *******************************************************************************/
   public ShareableTableMetaData getShareableTableMetaData()
   {
      return (this.shareableTableMetaData);
   }



   /*******************************************************************************
    ** Setter for shareableTableMetaData
    *******************************************************************************/
   public void setShareableTableMetaData(ShareableTableMetaData shareableTableMetaData)
   {
      this.shareableTableMetaData = shareableTableMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for shareableTableMetaData
    *******************************************************************************/
   public QTableMetaData withShareableTableMetaData(ShareableTableMetaData shareableTableMetaData)
   {
      this.shareableTableMetaData = shareableTableMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for helpContent
    *******************************************************************************/
   public Map<String, List<QHelpContent>> getHelpContent()
   {
      return (this.helpContent);
   }



   /*******************************************************************************
    ** Setter for helpContent
    *******************************************************************************/
   public void setHelpContent(Map<String, List<QHelpContent>> helpContent)
   {
      this.helpContent = helpContent;
   }



   /*******************************************************************************
    ** Fluent setter for helpContent
    *******************************************************************************/
   public QTableMetaData withHelpContent(Map<String, List<QHelpContent>> helpContent)
   {
      this.helpContent = helpContent;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adding 1 helpContent (for a slot)
    *******************************************************************************/
   public QTableMetaData withHelpContent(String slot, QHelpContent helpContent)
   {
      if(this.helpContent == null)
      {
         this.helpContent = new HashMap<>();
      }

      List<QHelpContent> listForSlot = this.helpContent.computeIfAbsent(slot, (k) -> new ArrayList<>());
      QInstanceHelpContentManager.putHelpContentInList(helpContent, listForSlot);

      return (this);
   }



   /*******************************************************************************
    ** remove a helpContent for a slot based on its set of roles
    *******************************************************************************/
   public void removeHelpContent(String slot, Set<HelpRole> roles)
   {
      if(this.helpContent == null)
      {
         return;
      }

      List<QHelpContent> listForSlot = this.helpContent.get(slot);
      if(listForSlot == null)
      {
         return;
      }

      QInstanceHelpContentManager.removeHelpContentByRoleSetFromList(roles, listForSlot);
   }



   /*******************************************************************************
    ** Getter for sourceQBitName
    *******************************************************************************/
   @Override
   public String getSourceQBitName()
   {
      return (this.sourceQBitName);
   }



   /*******************************************************************************
    ** Setter for sourceQBitName
    *******************************************************************************/
   @Override
   public void setSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceQBitName
    *******************************************************************************/
   @Override
   public QTableMetaData withSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData clone()
   {
      try
      {
         QTableMetaData clone = (QTableMetaData) super.clone();

         if(fields != null)
         {
            Map<String, QFieldMetaData> clonedFields = new LinkedHashMap<>();
            for(Map.Entry<String, QFieldMetaData> entry : fields.entrySet())
            {
               clonedFields.put(entry.getKey(), entry.getValue().clone());
            }
            clone.setFields(clonedFields);
         }

         if(virtualFields != null)
         {
            Map<String, QVirtualFieldMetaData> clonedVirtualFields = new LinkedHashMap<>();
            for(Map.Entry<String, QVirtualFieldMetaData> entry : virtualFields.entrySet())
            {
               clonedVirtualFields.put(entry.getKey(), entry.getValue().clone());
            }
            clone.setVirtualFields(clonedVirtualFields);
         }

         if(uniqueKeys != null)
         {
            List<UniqueKey> clonedUniqueKeys = new ArrayList<>();
            for(UniqueKey uniqueKey : uniqueKeys)
            {
               clonedUniqueKeys.add(uniqueKey.clone());
            }
            clone.setUniqueKeys(clonedUniqueKeys);
         }

         if(associations != null)
         {
            List<Association> clonedAssociations = new ArrayList<>();
            for(Association association : associations)
            {
               clonedAssociations.add(association.clone());
            }
            clone.setAssociations(clonedAssociations);
         }

         if(recordSecurityLocks != null)
         {
            List<RecordSecurityLock> clonedRecordSecurityLocks = new ArrayList<>();
            for(RecordSecurityLock lock : recordSecurityLocks)
            {
               clonedRecordSecurityLocks.add(lock.clone());
            }
            clone.setRecordSecurityLocks(clonedRecordSecurityLocks);
         }

         if(permissionRules != null)
         {
            clone.setPermissionRules(permissionRules.clone());
         }

         if(auditRules != null)
         {
            clone.setAuditRules(auditRules.clone());
         }

         if(backendDetails != null)
         {
            clone.setBackendDetails(backendDetails.clone());
         }

         if(automationDetails != null)
         {
            clone.setAutomationDetails(automationDetails.clone());
         }

         if(customizers != null)
         {
            Map<String, QCodeReference> clonedCustomizers = new HashMap<>();
            for(Map.Entry<String, QCodeReference> entry : customizers.entrySet())
            {
               clonedCustomizers.put(entry.getKey(), entry.getValue().clone());
            }
            clone.setCustomizers(clonedCustomizers);
         }

         if(icon != null)
         {
            clone.setIcon(icon.clone());
         }

         if(recordLabelFields != null)
         {
            clone.setRecordLabelFields(new ArrayList<>(recordLabelFields));
         }

         if(sections != null)
         {
            List<QFieldSection> clonedSections = new ArrayList<>();
            for(QFieldSection section : sections)
            {
               clonedSections.add(section.clone());
            }
            clone.setSections(clonedSections);
         }

         if(associatedScripts != null)
         {
            List<AssociatedScript> clonedAssociatedScripts = new ArrayList<>();
            for(AssociatedScript script : associatedScripts)
            {
               clonedAssociatedScripts.add(script.clone());
            }
            clone.setAssociatedScripts(clonedAssociatedScripts);
         }

         if(enabledCapabilities != null)
         {
            clone.setEnabledCapabilities(new HashSet<>(enabledCapabilities));
         }

         if(disabledCapabilities != null)
         {
            clone.setDisabledCapabilities(new HashSet<>(disabledCapabilities));
         }

         if(cacheOf != null)
         {
            clone.setCacheOf(cacheOf.clone());
         }

         if(supplementalMetaData != null)
         {
            Map<String, QSupplementalTableMetaData> clonedSupplementalMetaData = new HashMap<>();
            for(Map.Entry<String, QSupplementalTableMetaData> entry : supplementalMetaData.entrySet())
            {
               clonedSupplementalMetaData.put(entry.getKey(), entry.getValue().clone());
            }
            clone.setSupplementalMetaData(clonedSupplementalMetaData);
         }

         if(exposedJoins != null)
         {
            List<ExposedJoin> clonedExposedJoins = new ArrayList<>();
            for(ExposedJoin exposedJoin : exposedJoins)
            {
               clonedExposedJoins.add(exposedJoin.clone());
            }
            clone.setExposedJoins(clonedExposedJoins);
         }

         if(shareableTableMetaData != null)
         {
            clone.setShareableTableMetaData(shareableTableMetaData.clone());
         }

         if(helpContent != null)
         {
            Map<String, List<QHelpContent>> clonedHelpContent = new HashMap<>();
            for(Map.Entry<String, List<QHelpContent>> entry : helpContent.entrySet())
            {
               List<QHelpContent> clonedHelpContentList = new ArrayList<>();
               for(QHelpContent helpContent : entry.getValue())
               {
                  clonedHelpContentList.add(helpContent.clone());
               }
               clonedHelpContent.put(entry.getKey(), clonedHelpContentList);
            }
            clone.setHelpContent(clonedHelpContent);
         }

         if(menus != null)
         {
            List<QMenu> clonedMenus = new ArrayList<>();
            for(QMenu menu : menus)
            {
               clonedMenus.add(menu.clone());
            }
            clone.setMenus(clonedMenus);
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new RuntimeException(e);
      }
   }




   /*******************************************************************************
    * Getter for virtualFields
    * @see #withVirtualFields(Map)
    *******************************************************************************/
   public Map<String, QVirtualFieldMetaData> getVirtualFields()
   {
      return (this.virtualFields);
   }



   /*******************************************************************************
    * Setter for virtualFields
    * @see #withVirtualFields(Map)
    *******************************************************************************/
   public void setVirtualFields(Map<String, QVirtualFieldMetaData> virtualFields)
   {
      this.virtualFields = virtualFields;
   }



   /*******************************************************************************
    * Fluent setter for virtualFields
    *
    * @param virtualFields
    * Map of virtual fields - keyed by the virtual field's name, value is meta data
    * for the virtual field.
    * <p>Virtual fields do not exist in the backend system.  Rather, they
    * are (or at least tend to be) computed from other fields - e.g., math operations
    * (like a percentage based on 2 numbers), or concatenation of strings, or mappings
    * from e.g., booleans to strings - or more complex logic based on multiple fields</p>
    * @return this
    *******************************************************************************/
   public QTableMetaData withVirtualFields(Map<String, QVirtualFieldMetaData> virtualFields)
   {
      this.virtualFields = virtualFields;
      return (this);
   }



   /*******************************************************************************
    * Getter for menus
    * @see #withMenus(List)
    *******************************************************************************/
   public List<QMenu> getMenus()
   {
      return (this.menus);
   }



   /*******************************************************************************
    * Setter for menus
    * @see #withMenus(List)
    *******************************************************************************/
   public void setMenus(List<QMenu> menus)
   {
      this.menus = menus;
   }



   /*******************************************************************************
    * Fluent setter for menus
    *
    * @param menus
    * Specialized menus to be used for this table in UI's.
    * @return this
    *******************************************************************************/
   public QTableMetaData withMenus(List<QMenu> menus)
   {
      this.menus = menus;
      return (this);
   }



   /*******************************************************************************
    * Fluently add a single menu
    *
    * @param menu
    * A Specialized menu to be used for this table in UI's.
    * @return this
    *******************************************************************************/
   public QTableMetaData withMenu(QMenu menu)
   {
      if(this.menus == null)
      {
         this.menus = new ArrayList<>();
      }
      this.menus.add(menu);
      return (this);
   }

}
