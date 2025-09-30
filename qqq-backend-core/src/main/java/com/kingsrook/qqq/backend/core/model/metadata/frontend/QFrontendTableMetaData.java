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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Version of QTableMetaData that's meant for transmitting to a frontend.
 * e.g., it excludes backend-only details.
 *
 *******************************************************************************/
@JsonInclude(Include.NON_NULL)
public class QFrontendTableMetaData
{
   private static final QLogger LOG = QLogger.getLogger(QFrontendTableMetaData.class);

   private String  name;
   private String  label;
   private boolean isHidden;
   private String  primaryKeyField;
   private QIcon   icon;

   private Map<String, QFrontendFieldMetaData>     fields;
   private List<QFieldSection>                     sections;
   private List<QFrontendExposedJoin>              exposedJoins;
   private Map<String, QSupplementalTableMetaData> supplementalTableMetaData;
   private Set<String>                             capabilities;

   private boolean readPermission;
   private boolean insertPermission;
   private boolean editPermission;
   private boolean deletePermission;

   private boolean usesVariants;
   private String  variantTableLabel;

   private ShareableTableMetaData          shareableTableMetaData;
   private Map<String, List<QHelpContent>> helpContents;

   //////////////////////////////////////////////////////////////////////////////////
   // do not add setters.  take values from the source-object in the constructor!! //
   //////////////////////////////////////////////////////////////////////////////////

   /***************************************************************************
    ** standard constructor - uses all fields on the table.
    ***************************************************************************/
   public QFrontendTableMetaData(AbstractActionInput actionInput, QBackendMetaData backendForTable, QTableMetaData tableMetaData, boolean includeFullMetaData, boolean includeJoins)
   {
      this(actionInput, backendForTable, tableMetaData, includeFullMetaData, includeJoins, tableMetaData.getFields());
   }



   /*******************************************************************************
    ** alternative constructor - takes a map of fields to use (e.g., for an old
    ** api version of the table w/ different fields!)
    *******************************************************************************/
   public QFrontendTableMetaData(AbstractActionInput actionInput, QBackendMetaData backendForTable, QTableMetaData tableMetaData, boolean includeFullMetaData, boolean includeJoins, Map<String, QFieldMetaData> overrideFields)
   {
      this.name = tableMetaData.getName();
      this.label = tableMetaData.getLabel();
      this.isHidden = tableMetaData.getIsHidden();

      Map<String, QFieldMetaData> inputFields = overrideFields == null ? tableMetaData.getFields() : overrideFields;

      if(includeFullMetaData)
      {
         this.primaryKeyField = tableMetaData.getPrimaryKeyField();
         this.fields = new HashMap<>();
         for(String fieldName : inputFields.keySet())
         {
            QFieldMetaData field = inputFields.get(fieldName);
            if(!field.getIsHidden())
            {
               this.fields.put(fieldName, new QFrontendFieldMetaData(field));
            }
         }

         this.sections = tableMetaData.getSections();

         this.shareableTableMetaData = tableMetaData.getShareableTableMetaData();
      }

      if(includeJoins)
      {
         QInstance qInstance = QContext.getQInstance();

         this.exposedJoins = new ArrayList<>();
         for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(tableMetaData.getExposedJoins()))
         {
            try
            {
               QFrontendExposedJoin frontendExposedJoin = new QFrontendExposedJoin();

               QTableMetaData joinTable = qInstance.getTable(exposedJoin.getJoinTable());

               /////////////////////////////////////////////////////////////////////////////////////////////////////
               // apply personalizations to the exposed join table - so user only sees fields they're supposed to //
               /////////////////////////////////////////////////////////////////////////////////////////////////////
               TableMetaDataInput tableMetaDataInput = new TableMetaDataInput();
               tableMetaDataInput.setTableName(joinTable.getName());
               if(actionInput instanceof AbstractTableActionInput abstractTableActionInput)
               {
                  tableMetaDataInput.setInputSource(abstractTableActionInput.getInputSource());
               }
               joinTable = TableMetaDataPersonalizerAction.execute(tableMetaDataInput);

               frontendExposedJoin.setLabel(exposedJoin.getLabel());
               frontendExposedJoin.setIsMany(exposedJoin.getIsMany());
               frontendExposedJoin.setJoinTable(new QFrontendTableMetaData(actionInput, backendForTable, joinTable, includeFullMetaData, false));
               for(String joinName : exposedJoin.getJoinPath())
               {
                  frontendExposedJoin.addJoin(qInstance.getJoin(joinName));
               }

               this.exposedJoins.add(frontendExposedJoin);
            }
            catch(Exception e)
            {
               LOG.warn("Error setting up exposed join", e, logPair("tableName", tableMetaData.getName()), logPair("joinTable", exposedJoin.getJoinTable()));
            }
         }
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // include supplemental meta data, based on if it's meant for full or partial frontend meta-data requests //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(QSupplementalTableMetaData supplementalTableMetaData : CollectionUtils.nonNullMap(tableMetaData.getSupplementalMetaData()).values())
      {
         boolean include;
         if(includeFullMetaData)
         {
            include = supplementalTableMetaData.includeInFullFrontendMetaData();
         }
         else
         {
            include = supplementalTableMetaData.includeInPartialFrontendMetaData();
         }

         if(include)
         {
            this.supplementalTableMetaData = Objects.requireNonNullElseGet(this.supplementalTableMetaData, HashMap::new);
            this.supplementalTableMetaData.put(supplementalTableMetaData.getType(), supplementalTableMetaData);
         }
      }

      this.icon = tableMetaData.getIcon();

      setCapabilities(backendForTable, tableMetaData);

      readPermission = PermissionsHelper.hasTablePermission(actionInput, tableMetaData.getName(), TablePermissionSubType.READ);
      insertPermission = PermissionsHelper.hasTablePermission(actionInput, tableMetaData.getName(), TablePermissionSubType.INSERT);
      editPermission = PermissionsHelper.hasTablePermission(actionInput, tableMetaData.getName(), TablePermissionSubType.EDIT);
      deletePermission = PermissionsHelper.hasTablePermission(actionInput, tableMetaData.getName(), TablePermissionSubType.DELETE);

      QBackendMetaData backend = QContext.getQInstance().getBackend(tableMetaData.getBackendName());
      if(backend != null && backend.getUsesVariants())
      {
         usesVariants = true;
         variantTableLabel = QContext.getQInstance().getTable(backend.getBackendVariantsConfig().getOptionsTableName()).getLabel();
      }

      this.helpContents = tableMetaData.getHelpContent();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setCapabilities(QBackendMetaData backend, QTableMetaData table)
   {
      Set<Capability> enabledCapabilities = new LinkedHashSet<>();
      for(Capability capability : Capability.values())
      {
         if(table.isCapabilityEnabled(backend, capability))
         {
            ///////////////////////////////////////
            // todo - check if user is allowed!! //
            ///////////////////////////////////////

            enabledCapabilities.add(capability);
         }
      }

      this.capabilities = enabledCapabilities.stream().map(Enum::name).collect(Collectors.toSet());
   }



   /*******************************************************************************
    ** Alternative fluent setter for a single disabledCapabilities
    **
    *******************************************************************************/
   public QFrontendTableMetaData withoutCapability(Capability capability)
   {
      if(this.capabilities == null)
      {
         this.capabilities = new HashSet<>();
      }
      this.capabilities.remove(capability);
      return (this);
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
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Getter for primaryKeyField
    **
    *******************************************************************************/
   public String getPrimaryKeyField()
   {
      return primaryKeyField;
   }



   /*******************************************************************************
    ** Getter for fields
    **
    *******************************************************************************/
   public Map<String, QFrontendFieldMetaData> getFields()
   {
      return fields;
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
    ** Getter for isHidden
    **
    *******************************************************************************/
   public boolean getIsHidden()
   {
      return isHidden;
   }



   /*******************************************************************************
    ** Getter for iconName
    **
    *******************************************************************************/
   public String getIconName()
   {
      return (icon == null ? null : icon.getName());
   }



   /*******************************************************************************
    ** Getter for capabilities
    **
    *******************************************************************************/
   public Set<String> getCapabilities()
   {
      return capabilities;
   }



   /*******************************************************************************
    ** Getter for readPermission
    **
    *******************************************************************************/
   public boolean getReadPermission()
   {
      return readPermission;
   }



   /*******************************************************************************
    ** Getter for insertPermission
    **
    *******************************************************************************/
   public boolean getInsertPermission()
   {
      return insertPermission;
   }



   /*******************************************************************************
    ** Getter for editPermission
    **
    *******************************************************************************/
   public boolean getEditPermission()
   {
      return editPermission;
   }



   /*******************************************************************************
    ** Getter for deletePermission
    **
    *******************************************************************************/
   public boolean getDeletePermission()
   {
      return deletePermission;
   }



   /*******************************************************************************
    ** Getter for usesVariants
    **
    *******************************************************************************/
   public boolean getUsesVariants()
   {
      return usesVariants;
   }



   /*******************************************************************************
    ** Getter for exposedJoins
    **
    *******************************************************************************/
   public List<QFrontendExposedJoin> getExposedJoins()
   {
      return exposedJoins;
   }



   /*******************************************************************************
    ** Getter for supplementalTableMetaData
    **
    *******************************************************************************/
   public Map<String, QSupplementalTableMetaData> getSupplementalTableMetaData()
   {
      return supplementalTableMetaData;
   }



   /*******************************************************************************
    ** Getter for variantTableLabel
    *******************************************************************************/
   public String getVariantTableLabel()
   {
      return (this.variantTableLabel);
   }



   /*******************************************************************************
    ** Getter for shareableTableMetaData
    **
    *******************************************************************************/
   public ShareableTableMetaData getShareableTableMetaData()
   {
      return shareableTableMetaData;
   }



   /*******************************************************************************
    ** Getter for helpContents
    **
    *******************************************************************************/
   public Map<String, List<QHelpContent>> getHelpContents()
   {
      return helpContents;
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public QIcon getIcon()
   {
      return icon;
   }

}
