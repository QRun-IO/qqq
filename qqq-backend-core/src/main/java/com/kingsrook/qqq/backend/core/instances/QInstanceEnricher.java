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

package com.kingsrook.qqq.backend.core.instances;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.metadata.JoinGraph;
import com.kingsrook.qqq.backend.core.actions.permissions.BulkTableActionProcessPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.instances.enrichment.plugins.QInstanceEnricherPluginInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.bulk.TableKeyFieldsPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType.FileUploadAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QSupplementalFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStateMachineStep;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QSupplementalProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.delete.BulkDeleteLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.delete.BulkDeleteTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit.BulkEditLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit.BulkEditTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit.PrepareBulkEditStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertPrepareFileMappingStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertPrepareFileUploadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertPrepareValueMappingStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertReceiveFileMappingStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertReceiveValueMappingStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** As part of helping a QInstance be created and/or validated, apply some default
 ** transformations to it, such as populating missing labels based on names.
 **
 *******************************************************************************/
public class QInstanceEnricher
{
   private static final QLogger LOG = QLogger.getLogger(QInstanceEnricher.class);

   private final QInstance qInstance;

   private JoinGraph joinGraph;

   private boolean configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels        = true;
   private boolean configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate = true;

   //////////////////////////////////////////////////////////////////////////////////////////////////
   // let an instance define mappings to be applied during name-to-label enrichments,              //
   // e.g., to avoid ever incorrectly camel-casing an acronym (e.g., "Tla" should always be "TLA") //
   // or to expand abbreviations in code (e.g., "Addr" should always be "Address"                  //
   //////////////////////////////////////////////////////////////////////////////////////////////////
   private static final Map<String, String> labelMappings = new LinkedHashMap<>();

   private static ListingHash<Class<?>, QInstanceEnricherPluginInterface<?>> enricherPlugins = new ListingHash<>();

   /////////////////////////////////////////////////////////////////////////////////
   // in case a section references itself as an alternative, avoid stack overflow //
   /////////////////////////////////////////////////////////////////////////////////
   private Set<QFieldSection> visitedSections = new HashSet<>();


   /*******************************************************************************
    **
    *******************************************************************************/
   public QInstanceEnricher(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void enrich()
   {
      /////////////////////////////////////////////////////////////////////////////////////////
      // at one point, we did apps later - but - it was possible to put tables in an app's   //
      // sections, but not its children list (enrichApp fixes this by adding such tables to  //
      // the children list) so then when enrichTable runs, it looks for fields that are      //
      // possible-values pointed at tables,  for adding LINK adornments - and that could     //
      // cause such links to be omitted, mysteriously!  so, do app enrichment before tables. //
      /////////////////////////////////////////////////////////////////////////////////////////
      if(qInstance.getApps() != null)
      {
         qInstance.getApps().values().forEach(this::enrichApp);
      }

      if(qInstance.getTables() != null)
      {
         qInstance.getTables().values().forEach(this::enrichTable);
         defineTableBulkProcesses(qInstance);
      }

      if(qInstance.getProcesses() != null)
      {
         qInstance.getProcesses().values().forEach(this::enrichProcess);
      }

      if(qInstance.getBackends() != null)
      {
         qInstance.getBackends().values().forEach(this::enrichBackend);
      }

      if(qInstance.getReports() != null)
      {
         qInstance.getReports().values().forEach(this::enrichReport);
      }

      if(qInstance.getPossibleValueSources() != null)
      {
         qInstance.getPossibleValueSources().values().forEach(this::enrichPossibleValueSource);
      }

      if(qInstance.getWidgets() != null)
      {
         qInstance.getWidgets().values().forEach(this::enrichWidget);
      }

      enrichJoins();
      enrichInstance();

      //////////////////////////////////////////////////////////////////////////////
      // if the instance DOES have 1 or more scheduler, but no schedulable types, //
      // then go ahead and add the default set that qqq knows about               //
      //////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(qInstance.getSchedulers()))
      {
         if(CollectionUtils.nullSafeIsEmpty(qInstance.getSchedulableTypes()))
         {
            QScheduleManager.defineDefaultSchedulableTypesInInstance(qInstance);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void enrichInstance()
   {
      ////////////////////////////////////////////////////////////////////////////////////
      // enriching some objects may cause additional ones to be added to the qInstance! //
      // this caused concurrent modification exceptions, when we just iterated.         //
      // we could make a copy of the map and just process that, but then we wouldn't    //
      // enrich any new objects that do get added, so, use this technique instead.      //
      ////////////////////////////////////////////////////////////////////////////////////
      Set<QSupplementalInstanceMetaData> toEnrich = new LinkedHashSet<>(qInstance.getSupplementalMetaData().values());
      Set<QSupplementalInstanceMetaData> enriched = new HashSet<>();
      int                                count    = 0;
      while(!toEnrich.isEmpty())
      {
         Iterator<QSupplementalInstanceMetaData> iterator                     = toEnrich.iterator();
         QSupplementalInstanceMetaData           supplementalInstanceMetaData = iterator.next();
         iterator.remove();

         supplementalInstanceMetaData.enrich(qInstance);
         enriched.add(supplementalInstanceMetaData);

         for(QSupplementalInstanceMetaData possiblyNew : qInstance.getSupplementalMetaData().values())
         {
            if(!toEnrich.contains(possiblyNew) && !enriched.contains(possiblyNew))
            {
               if(count++ > 100)
               {
                  throw (new QRuntimeException("Too many new QSupplementalInstanceMetaData objects were added while enriching others.  This probably indicates a bug in enrichment code.  Throwing to prevent infinite loop."));
               }
               toEnrich.add(possiblyNew);
            }
         }
      }

      runPlugins(QInstance.class, qInstance, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichJoins()
   {
      try
      {
         joinGraph = new JoinGraph(qInstance);

         for(QTableMetaData table : CollectionUtils.nonNullMap(qInstance.getTables()).values())
         {
            Set<JoinGraph.JoinConnectionList> joinConnections = joinGraph.getJoinConnections(table.getName());
            for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
            {
               /////////////////////////////////////////////////////////////////////////////////////////////////////
               // proceed with caution - remember, validator will fail the instance if things are missing/invalid //
               /////////////////////////////////////////////////////////////////////////////////////////////////////
               if(exposedJoin.getJoinTable() != null)
               {
                  QTableMetaData joinTable = qInstance.getTable(exposedJoin.getJoinTable());
                  if(joinTable != null)
                  {
                     //////////////////////////////////////////////////////////////////////////////////
                     // default the exposed join's label to the join table's label, if it wasn't set //
                     //////////////////////////////////////////////////////////////////////////////////
                     if(!StringUtils.hasContent(exposedJoin.getLabel()))
                     {
                        exposedJoin.setLabel(joinTable.getLabel());
                     }

                     ///////////////////////////////////////////////////////////////////////////////
                     // default the exposed join's join-path from the joinGraph, if it wasn't set //
                     ///////////////////////////////////////////////////////////////////////////////
                     if(CollectionUtils.nullSafeIsEmpty(exposedJoin.getJoinPath()))
                     {
                        List<JoinGraph.JoinConnectionList> eligibleJoinConnections = new ArrayList<>();
                        for(JoinGraph.JoinConnectionList joinConnection : joinConnections)
                        {
                           if(joinTable.getName().equals(joinConnection.list().get(joinConnection.list().size() - 1).joinTable()))
                           {
                              eligibleJoinConnections.add(joinConnection);
                           }
                        }

                        if(eligibleJoinConnections.isEmpty())
                        {
                           throw (new QException("Could not infer a joinPath for table [" + table.getName() + "], exposedJoin to [" + exposedJoin.getJoinTable() + "]:  No join connections between these tables exist in this instance."));
                        }
                        else if(eligibleJoinConnections.size() > 1)
                        {
                           throw (new QException("Could not infer a joinPath for table [" + table.getName() + "], exposedJoin to [" + exposedJoin.getJoinTable() + "]:  "
                              + eligibleJoinConnections.size() + " join connections exist between these tables.  You need to specify one:\n"
                              + StringUtils.join("\n", eligibleJoinConnections.stream().map(jcl -> jcl.getJoinNamesAsString()).toList()) + "."
                           ));
                        }
                        else
                        {
                           exposedJoin.setJoinPath(eligibleJoinConnections.get(0).getJoinNamesAsList());
                        }
                     }
                  }
               }
            }
         }

         ///////////////////////////////////////////
         // run plugins on joins if there are any //
         ///////////////////////////////////////////
         for(QJoinMetaData join : qInstance.getJoins().values())
         {
            runPlugins(QJoinMetaData.class, join, qInstance);
         }
      }
      catch(Exception e)
      {
         throw (new RuntimeException("Error enriching instance joins", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichWidget(QWidgetMetaDataInterface widgetMetaData)
   {
      enrichPermissionRules(widgetMetaData);
      runPlugins(QWidgetMetaDataInterface.class, widgetMetaData, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichBackend(QBackendMetaData qBackendMetaData)
   {
      qBackendMetaData.enrich();
      runPlugins(QBackendMetaData.class, qBackendMetaData, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichTable(QTableMetaData table)
   {
      if(!StringUtils.hasContent(table.getLabel()))
      {
         table.setLabel(nameToLabel(table.getName()));
      }

      if(table.getFields() != null)
      {
         for(Map.Entry<String, QFieldMetaData> entry : table.getFields().entrySet())
         {
            String         name  = entry.getKey();
            QFieldMetaData field = entry.getValue();

            ////////////////////////////////////////////////////////////////////////////
            // in case the field wasn't given a name, use its key from the fields map //
            ////////////////////////////////////////////////////////////////////////////
            if(!StringUtils.hasContent(field.getName()))
            {
               field.setName(name);
            }

            enrichField(field);
         }
      }

      if(table.getVirtualFields() != null)
      {
         for(Map.Entry<String, QVirtualFieldMetaData> entry : table.getVirtualFields().entrySet())
         {
            String         name  = entry.getKey();
            QFieldMetaData field = entry.getValue();

            ////////////////////////////////////////////////////////////////////////////
            // in case the field wasn't given a name, use its key from the fields map //
            ////////////////////////////////////////////////////////////////////////////
            if(!StringUtils.hasContent(field.getName()))
            {
               field.setName(name);
            }

            enrichField(field);
         }
      }

      for(QSupplementalTableMetaData supplementalTableMetaData : CollectionUtils.nonNullMap(table.getSupplementalMetaData()).values())
      {
         supplementalTableMetaData.enrich(qInstance, table);
      }

      if(CollectionUtils.nullSafeIsEmpty(table.getSections()))
      {
         generateTableFieldSections(table);
      }
      else
      {
         table.getSections().forEach(this::enrichFieldSection);
      }

      if(CollectionUtils.nullSafeHasContents(table.getRecordLabelFields()) && !StringUtils.hasContent(table.getRecordLabelFormat()))
      {
         table.setRecordLabelFormat(String.join(" ", Collections.nCopies(table.getRecordLabelFields().size(), "%s")));
      }

      enrichPermissionRules(table);
      enrichAuditRules(table);
      runPlugins(QTableMetaData.class, table, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichAuditRules(QTableMetaData table)
   {
      if(table.getAuditRules() == null && qInstance.getDefaultAuditRules() != null)
      {
         table.setAuditRules(qInstance.getDefaultAuditRules());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichPermissionRules(MetaDataWithPermissionRules metaDataWithPermissionRules)
   {
      ///////////////////////////////////////////////////////////////////////
      // make sure there's a permissionsRule object in the metaData object //
      ///////////////////////////////////////////////////////////////////////
      if(metaDataWithPermissionRules.getPermissionRules() == null)
      {
         if(qInstance.getDefaultPermissionRules() != null)
         {
            metaDataWithPermissionRules.setPermissionRules(qInstance.getDefaultPermissionRules().clone());
         }
         else
         {
            metaDataWithPermissionRules.setPermissionRules(QPermissionRules.defaultInstance().clone());
         }
      }

      QPermissionRules permissionRules = metaDataWithPermissionRules.getPermissionRules();

      /////////////////////////////////////////////////////////////////////////////////
      // now make sure the required fields are all set in the permissionRules object //
      /////////////////////////////////////////////////////////////////////////////////
      if(permissionRules.getLevel() == null)
      {
         if(qInstance.getDefaultPermissionRules() != null && qInstance.getDefaultPermissionRules().getLevel() != null)
         {
            permissionRules.setLevel(qInstance.getDefaultPermissionRules().getLevel());
         }
         else
         {
            permissionRules.setLevel(QPermissionRules.defaultInstance().getLevel());
         }
      }

      if(permissionRules.getDenyBehavior() == null)
      {
         if(qInstance.getDefaultPermissionRules() != null && qInstance.getDefaultPermissionRules().getDenyBehavior() != null)
         {
            permissionRules.setDenyBehavior(qInstance.getDefaultPermissionRules().getDenyBehavior());
         }
         else
         {
            permissionRules.setDenyBehavior(QPermissionRules.defaultInstance().getDenyBehavior());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichProcess(QProcessMetaData process)
   {
      if(!StringUtils.hasContent(process.getLabel()))
      {
         process.setLabel(nameToLabel(process.getName()));
      }

      for(QStepMetaData step : CollectionUtils.nonNullMap(process.getAllSteps()).values())
      {
         enrichStep(step);
      }

      for(QSupplementalProcessMetaData supplementalProcessMetaData : CollectionUtils.nonNullMap(process.getSupplementalMetaData()).values())
      {
         supplementalProcessMetaData.enrich(this, process);
      }

      enrichPermissionRules(process);
      runPlugins(QProcessMetaData.class, process, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichStep(QStepMetaData step)
   {
      enrichStep(step, false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void enrichStep(QStepMetaData step, boolean isSubStep)
   {
      if(!StringUtils.hasContent(step.getLabel()))
      {
         if(isSubStep && (step.getName().endsWith(".backend") || step.getName().endsWith(".frontend")))
         {
            step.setLabel(nameToLabel(step.getName().replaceFirst("\\.(backend|frontend)", "")));
         }
         else
         {
            step.setLabel(nameToLabel(step.getName()));
         }
      }

      step.getInputFields().forEach(this::enrichField);
      step.getOutputFields().forEach(this::enrichField);

      if(step instanceof QFrontendStepMetaData frontendStepMetaData)
      {
         if(frontendStepMetaData.getFormFields() != null)
         {
            frontendStepMetaData.getFormFields().forEach(this::enrichField);
         }
         if(frontendStepMetaData.getViewFields() != null)
         {
            frontendStepMetaData.getViewFields().forEach(this::enrichField);
         }
         if(frontendStepMetaData.getRecordListFields() != null)
         {
            frontendStepMetaData.getRecordListFields().forEach(this::enrichField);
         }
      }
      else if(step instanceof QStateMachineStep stateMachineStep)
      {
         for(QStepMetaData subStep : CollectionUtils.nonNullList(stateMachineStep.getSubSteps()))
         {
            enrichStep(subStep, true);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void enrichField(QFieldMetaData field)
   {
      if(!StringUtils.hasContent(field.getLabel()))
      {
         fieldNameToLabel(field);
      }

      //////////////////////////////////////////////////////////////////////////
      // if this field has a possibleValueSource                              //
      // and that PVS exists in the instance                                  //
      // and it's a table-type PVS and the table name is set                  //
      // and it's a valid table in the instance, and the table is in some app //
      // and the field doesn't (already) have a LINK or CHIP adornment        //
      // then add a link-to-record-from-table adornment to the field.         //
      //////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(field.getPossibleValueSourceName()))
      {
         QPossibleValueSource possibleValueSource = qInstance.getPossibleValueSource(field.getPossibleValueSourceName());
         if(possibleValueSource != null)
         {
            String tableName = possibleValueSource.getTableName();
            if(QPossibleValueSourceType.TABLE.equals(possibleValueSource.getType()) && StringUtils.hasContent(tableName))
            {
               if(qInstance.getTable(tableName) != null && doesAnyAppHaveTable(tableName))
               {
                  boolean hasLinkAdornment = false;
                  boolean hasChipAdornment = false;
                  if(field.getAdornments() != null)
                  {
                     hasLinkAdornment = field.getAdornments().stream().anyMatch(a -> AdornmentType.LINK.equals(a.getType()));
                     hasChipAdornment = field.getAdornments().stream().anyMatch(a -> AdornmentType.CHIP.equals(a.getType()));
                  }

                  if(!hasLinkAdornment && !hasChipAdornment)
                  {
                     field.withFieldAdornment(new FieldAdornment().withType(AdornmentType.LINK)
                        .withValue(AdornmentType.LinkValues.TO_RECORD_FROM_TABLE, tableName));
                  }
               }
            }
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // add field behaviors for create date & modify date, if so configured //
      /////////////////////////////////////////////////////////////////////////
      if(configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate)
      {
         if("createDate".equals(field.getName()) && field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class) == null)
         {
            field.withBehavior(DynamicDefaultValueBehavior.CREATE_DATE);
         }

         if("modifyDate".equals(field.getName()) && field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class) == null)
         {
            field.withBehavior(DynamicDefaultValueBehavior.MODIFY_DATE);
         }
      }

      ////////////////////////////////////////////////////
      // enrich any supplemental meta data on the field //
      ////////////////////////////////////////////////////
      for(QSupplementalFieldMetaData supplementalFieldMetaData : CollectionUtils.nonNullMap(field.getSupplementalMetaData()).values())
      {
         supplementalFieldMetaData.enrich(qInstance, field);
      }

      runPlugins(QFieldMetaData.class, field, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void fieldNameToLabel(QFieldMetaData field)
   {
      if(configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels && StringUtils.hasContent(field.getPossibleValueSourceName()) && field.getName() != null && field.getName().endsWith("Id"))
      {
         field.setLabel(nameToLabel(field.getName().substring(0, field.getName().length() - 2)));
      }
      else
      {
         field.setLabel(nameToLabel(field.getName()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean doesAnyAppHaveTable(String tableName)
   {
      if(qInstance.getApps() != null)
      {
         for(QAppMetaData app : qInstance.getApps().values())
         {
            if(app.getChildren() != null)
            {
               for(QAppChildMetaData child : app.getChildren())
               {
                  if(child instanceof QTableMetaData && tableName.equals(child.getName()))
                  {
                     return (true);
                  }
               }
            }
         }
      }

      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichApp(QAppMetaData app)
   {
      if(!StringUtils.hasContent(app.getLabel()))
      {
         app.setLabel(nameToLabel(app.getName()));
      }

      if(CollectionUtils.nullSafeIsEmpty(app.getSections()))
      {
         generateAppSections(app);
      }

      for(QAppSection section : CollectionUtils.nonNullList(app.getSections()))
      {
         enrichAppSection(section);
      }

      ensureAppSectionMembersAreAppChildren(app);

      enrichPermissionRules(app);
      runPlugins(QAppMetaData.class, app, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void ensureAppSectionMembersAreAppChildren(QAppMetaData app)
   {
      ListingHash<Class<? extends QAppChildMetaData>, String> childrenByType = new ListingHash<>();
      childrenByType.put(QTableMetaData.class, new ArrayList<>());
      childrenByType.put(QProcessMetaData.class, new ArrayList<>());
      childrenByType.put(QReportMetaData.class, new ArrayList<>());

      for(QAppChildMetaData qAppChildMetaData : CollectionUtils.nonNullList(app.getChildren()))
      {
         childrenByType.add(qAppChildMetaData.getClass(), qAppChildMetaData.getName());
      }

      for(QAppSection section : CollectionUtils.nonNullList(app.getSections()))
      {
         for(String tableName : CollectionUtils.nonNullList(section.getTables()))
         {
            if(!childrenByType.get(QTableMetaData.class).contains(tableName))
            {
               QTableMetaData table = qInstance.getTable(tableName);
               if(table != null)
               {
                  app.withChild(table);
               }
            }
         }

         for(String processName : CollectionUtils.nonNullList(section.getProcesses()))
         {
            if(!childrenByType.get(QProcessMetaData.class).contains(processName))
            {
               QProcessMetaData process = qInstance.getProcess(processName);
               if(process != null)
               {
                  app.withChild(process);
               }
            }
         }

         for(String reportName : CollectionUtils.nonNullList(section.getReports()))
         {
            if(!childrenByType.get(QReportMetaData.class).contains(reportName))
            {
               QReportMetaData report = qInstance.getReport(reportName);
               if(report != null)
               {
                  app.withChild(report);
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichAppSection(QAppSection section)
   {
      if(!StringUtils.hasContent(section.getLabel()))
      {
         section.setLabel(nameToLabel(section.getName()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichFieldSection(QFieldSection section)
   {
      if(visitedSections.contains(section))
      {
         return;
      }

      visitedSections.add(section);

      if(!StringUtils.hasContent(section.getLabel()))
      {
         section.setLabel(nameToLabel(section.getName()));
      }

      for(QFieldSection alternativeSection : CollectionUtils.nonNullMap(section.getAlternatives()).values())
      {
         enrichFieldSection(alternativeSection);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichReport(QReportMetaData report)
   {
      if(!StringUtils.hasContent(report.getLabel()))
      {
         report.setLabel(nameToLabel(report.getName()));
      }

      if(report.getInputFields() != null)
      {
         report.getInputFields().forEach(this::enrichField);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there's only 1 data source in the report, and it doesn't have a name, give it a default name //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      String singleDataSourceName = null;
      if(report.getDataSources() != null)
      {
         if(report.getDataSources().size() == 1)
         {
            QReportDataSource dataSource = report.getDataSources().get(0);
            if(!StringUtils.hasContent(dataSource.getName()))
            {
               dataSource.setName("DEFAULT");
            }
            singleDataSourceName = dataSource.getName();
         }
      }

      if(report.getViews() != null)
      {
         //////////////////////////////////////////////////////////////////////////////////////////////
         // if there's only 1 view in the report, and it doesn't have a name, give it a default name //
         //////////////////////////////////////////////////////////////////////////////////////////////
         if(report.getViews().size() == 1)
         {
            QReportView view = report.getViews().get(0);
            if(!StringUtils.hasContent(view.getName()))
            {
               view.setName("DEFAULT");
            }
         }

         /////////////////////////////////////////////////////////////////////////////
         // for any views in the report, if they don't specify a data source name,  //
         // but there's only 1 data source, then use that single data source's name //
         /////////////////////////////////////////////////////////////////////////////
         for(QReportView view : report.getViews())
         {
            if(!StringUtils.hasContent(view.getDataSourceName()) && singleDataSourceName != null)
            {
               view.setDataSourceName(singleDataSourceName);
            }
         }
      }

      enrichPermissionRules(report);
      runPlugins(QReportMetaData.class, report, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String nameToLabel(String name)
   {
      if(!StringUtils.hasContent(name))
      {
         return (name);
      }

      if(name.length() == 1)
      {
         return (name.substring(0, 1).toUpperCase(Locale.ROOT));
      }

      String suffix = name.substring(1)

         //////////////////////////////////////////////////////////////////////
         // Put a space before capital letters or numbers embedded in a name //
         // e.g., omethingElse -> omething Else; umber1 -> umber 1           //
         //////////////////////////////////////////////////////////////////////
         .replaceAll("([A-Z0-9]+)", " $1")

         ////////////////////////////////////////////////////////////////
         // put a space between numbers and words that come after them //
         // e.g., umber1dad -> number 1 dad                            //
         ////////////////////////////////////////////////////////////////
         .replaceAll("([0-9])([A-Za-z])", "$1 $2");

      String label = name.substring(0, 1).toUpperCase(Locale.ROOT) + suffix;

      /////////////////////////////////////////////////////////////////////////////////////////////
      // apply any label mappings - e.g., to force app-specific acronyms/initialisms to all-caps //
      /////////////////////////////////////////////////////////////////////////////////////////////
      for(Map.Entry<String, String> entry : labelMappings.entrySet())
      {
         label = label.replaceAll(entry.getKey(), entry.getValue());
      }

      return (label);
   }



   /*******************************************************************************
    ** Add bulk insert/edit/delete processes to all tables (unless the meta data
    ** already had these processes defined (e.g., the user defined custom ones)
    *******************************************************************************/
   private void defineTableBulkProcesses(QInstance qInstance)
   {
      if(qInstance.getPossibleValueSource(TableKeyFieldsPossibleValueSource.NAME) == null)
      {
         qInstance.addPossibleValueSource(defineTableKeyFieldsPossibleValueSource());
      }

      for(QTableMetaData table : qInstance.getTables().values())
      {
         if(table.getFields() == null)
         {
            /////////////////////////////////////////////////////////////////
            // these processes can't be defined if there aren't any fields //
            /////////////////////////////////////////////////////////////////
            continue;
         }

         QBackendMetaData backend = qInstance.getBackend(table.getBackendName());

         // todo - add idea of 'supportsBulkX'?
         String bulkInsertProcessName = table.getName() + ".bulkInsert";
         if(qInstance.getProcess(bulkInsertProcessName) == null && table.isCapabilityEnabled(backend, Capability.TABLE_INSERT))
         {
            defineTableBulkInsert(qInstance, table, bulkInsertProcessName);
         }

         String bulkEditProcessName = table.getName() + ".bulkEdit";
         if(qInstance.getProcess(bulkEditProcessName) == null && table.isCapabilityEnabled(backend, Capability.TABLE_UPDATE))
         {
            defineTableBulkEdit(qInstance, table, bulkEditProcessName);
         }

         String bulkEditWithFileProcessName = table.getName() + ".bulkEditWithFile";
         if(qInstance.getProcess(bulkEditWithFileProcessName) == null && table.isCapabilityEnabled(backend, Capability.TABLE_UPDATE))
         {
            defineTableBulkEditWithFile(qInstance, table, bulkEditWithFileProcessName);
         }

         String bulkDeleteProcessName = table.getName() + ".bulkDelete";
         if(qInstance.getProcess(bulkDeleteProcessName) == null && table.isCapabilityEnabled(backend, Capability.TABLE_DELETE))
         {
            defineTableBulkDelete(qInstance, table, bulkDeleteProcessName);
         }

      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineTableBulkInsert(QInstance qInstance, QTableMetaData table, String processName)
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, table.getName());

      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
            BulkInsertExtractStep.class,
            BulkInsertTransformStep.class,
            BulkInsertLoadStep.class,
            values
         )
         .withName(processName)
         .withIcon(new QIcon().withName("library_add"))
         .withLabel(table.getLabel() + " Bulk Insert")
         .withTableName(table.getName())
         .withIsHidden(true)
         .withPermissionRules(qInstance.getDefaultPermissionRules().clone()
            .withCustomPermissionChecker(new QCodeReference(BulkTableActionProcessPermissionChecker.class)));

      List<QFieldMetaData> editableFields = new ArrayList<>();
      for(QFieldSection section : CollectionUtils.nonNullList(table.getSections()))
      {
         for(String fieldName : CollectionUtils.nonNullList(section.getFieldNames()))
         {
            try
            {
               QFieldMetaData field = table.getField(fieldName);
               if(field.getIsEditable() && !field.getType().equals(QFieldType.BLOB))
               {
                  editableFields.add(field);
               }
            }
            catch(Exception e)
            {
               // shrug?
            }
         }
      }

      QBackendStepMetaData prepareFileUploadStep = new QBackendStepMetaData()
         .withName("prepareFileUpload")
         .withCode(new QCodeReference(BulkInsertPrepareFileUploadStep.class));

      QFrontendStepMetaData uploadScreen = new QFrontendStepMetaData()
         .withName("upload")
         .withLabel("Upload File")
         .withFormField(new QFieldMetaData("theFile", QFieldType.BLOB)
            .withFieldAdornment(FileUploadAdornment.newFieldAdornment()
               .withValue(FileUploadAdornment.formatDragAndDrop())
               .withValue(FileUploadAdornment.widthFull()))
            .withLabel(table.getLabel() + " File")
            .withIsRequired(true))
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.HTML))
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM));

      QBackendStepMetaData prepareFileMappingStep = new QBackendStepMetaData()
         .withName("prepareFileMapping")
         .withCode(new QCodeReference(BulkInsertPrepareFileMappingStep.class));

      QFrontendStepMetaData fileMappingScreen = new QFrontendStepMetaData()
         .withName("fileMapping")
         .withLabel("File Mapping")
         .withBackStepName("prepareFileUpload")
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_FILE_MAPPING_FORM))
         .withFormField(new QFieldMetaData("hasHeaderRow", QFieldType.BOOLEAN))
         .withFormField(new QFieldMetaData("layout", QFieldType.STRING)); // is actually PVS, but, this field is only added to help support helpContent, so :shrug:

      QBackendStepMetaData receiveFileMappingStep = new QBackendStepMetaData()
         .withName("receiveFileMapping")
         .withCode(new QCodeReference(BulkInsertReceiveFileMappingStep.class));

      QBackendStepMetaData prepareValueMappingStep = new QBackendStepMetaData()
         .withName("prepareValueMapping")
         .withCode(new QCodeReference(BulkInsertPrepareValueMappingStep.class));

      QFrontendStepMetaData valueMappingScreen = new QFrontendStepMetaData()
         .withName("valueMapping")
         .withLabel("Value Mapping")
         .withBackStepName("prepareFileMapping")
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_VALUE_MAPPING_FORM));

      QBackendStepMetaData receiveValueMappingStep = new QBackendStepMetaData()
         .withName("receiveValueMapping")
         .withCode(new QCodeReference(BulkInsertReceiveValueMappingStep.class));

      int i = 0;
      process.withStep(i++, prepareFileUploadStep);
      process.withStep(i++, uploadScreen);

      process.withStep(i++, prepareFileMappingStep);
      process.withStep(i++, fileMappingScreen);
      process.withStep(i++, receiveFileMappingStep);

      process.withStep(i++, prepareValueMappingStep);
      process.withStep(i++, valueMappingScreen);
      process.withStep(i++, receiveValueMappingStep);

      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW).setRecordListFields(editableFields);

      //////////////////////////////////////////////////////////////////////////////////////////
      // put the bulk-load profile form (e.g., for saving it) on the review & result screens) //
      //////////////////////////////////////////////////////////////////////////////////////////
      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW)
         .withBackStepName("prepareFileMapping")
         .getComponents().add(0, new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_PROFILE_FORM));

      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_RESULT)
         .getComponents().add(0, new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_PROFILE_FORM));

      qInstance.addProcess(process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineTableBulkEdit(QInstance qInstance, QTableMetaData table, String processName)
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, table.getName());
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, table.getName());
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, StreamedETLWithFrontendProcess.DEFAULT_PREVIEW_MESSAGE_FOR_UPDATE);

      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
            ExtractViaQueryStep.class,
            BulkEditTransformStep.class,
            BulkEditLoadStep.class,
            values
         )
         .withName(processName)
         .withLabel(table.getLabel() + " Bulk Edit")
         .withTableName(table.getName())
         .withIsHidden(true)
         .withPermissionRules(qInstance.getDefaultPermissionRules().clone()
            .withCustomPermissionChecker(new QCodeReference(BulkTableActionProcessPermissionChecker.class)));

      List<QFieldMetaData> editableFields = table.getFields().values().stream()
         .filter(QFieldMetaData::getIsEditable)
         .filter(f -> !f.getType().equals(QFieldType.BLOB))
         .map(QFieldMetaData::clone)
         .map(QInstanceEnricher::addProcessFallbacksToPvsFilter)
         .toList();

      QFrontendStepMetaData editScreen = new QFrontendStepMetaData()
         .withName("edit")
         .withLabel("Edit Values")
         .withFormFields(editableFields)
         .withComponent(new QFrontendComponentMetaData()
            .withType(QComponentType.HELP_TEXT)
            .withValue("text", """
               Flip the switches next to the fields that you want to edit.
               The values you supply here will be updated in all of the records you are bulk editing.
               You can clear out the value in a field by flipping the switch on for that field and leaving the input field blank.
               Fields whose switches are off will not be updated."""))
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.BULK_EDIT_FORM));

      process.withStep(0, editScreen);
      process.getFrontendStep("review").setRecordListFields(editableFields);

      QBackendStepMetaData prepareBulkEditStep = new QBackendStepMetaData()
         .withName("prepareBulkEdit")
         .withCode(new QCodeReference(PrepareBulkEditStep.class));
      process.withStep(0, prepareBulkEditStep);

      qInstance.addProcess(process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineTableBulkEditWithFile(QInstance qInstance, QTableMetaData table, String processName)
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, table.getName());
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, "This is a preview of the records that will be updated.");

      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
            BulkInsertExtractStep.class,
            BulkInsertTransformStep.class,
            BulkEditLoadStep.class,
            values
         )
         .withName(processName)
         .withLabel(table.getLabel() + " Bulk Edit With File")
         .withTableName(table.getName())
         .withIsHidden(true)
         .withPermissionRules(qInstance.getDefaultPermissionRules().clone()
            .withCustomPermissionChecker(new QCodeReference(BulkTableActionProcessPermissionChecker.class)));

      List<QFieldMetaData> editableFields = table.getFields().values().stream()
         .filter(QFieldMetaData::getIsEditable)
         .filter(f -> !f.getType().equals(QFieldType.BLOB))
         .toList();

      QBackendStepMetaData prepareFileUploadStep = new QBackendStepMetaData()
         .withName("prepareFileUpload")
         .withCode(new QCodeReference(BulkInsertPrepareFileUploadStep.class));

      QFrontendStepMetaData uploadScreen = new QFrontendStepMetaData()
         .withName("upload")
         .withLabel("Upload File")
         .withFormField(new QFieldMetaData("theFile", QFieldType.BLOB)
            .withFieldAdornment(FileUploadAdornment.newFieldAdornment()
               .withValue(FileUploadAdornment.formatDragAndDrop())
               .withValue(FileUploadAdornment.widthFull()))
            .withLabel(table.getLabel() + " File")
            .withIsRequired(true))
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.HTML))
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM));

      QBackendStepMetaData prepareFileMappingStep = new QBackendStepMetaData()
         .withName("prepareFileMapping")
         .withCode(new QCodeReference(BulkInsertPrepareFileMappingStep.class));

      QFrontendStepMetaData fileMappingScreen = new QFrontendStepMetaData()
         .withName("fileMapping")
         .withLabel("File Mapping")
         .withBackStepName("prepareFileUpload")
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_FILE_MAPPING_FORM))
         .withFormField(new QFieldMetaData("hasHeaderRow", QFieldType.BOOLEAN))
         .withFormField(new QFieldMetaData("layout", QFieldType.STRING)) // is actually PVS, but, this field is only added to help support helpContent, so :shrug:
         .withFormField(new QFieldMetaData("tableKeyFields", QFieldType.STRING).withPossibleValueSourceName(TableKeyFieldsPossibleValueSource.NAME));

      QBackendStepMetaData receiveFileMappingStep = new QBackendStepMetaData()
         .withName("receiveFileMapping")
         .withCode(new QCodeReference(BulkInsertReceiveFileMappingStep.class));

      QBackendStepMetaData prepareValueMappingStep = new QBackendStepMetaData()
         .withName("prepareValueMapping")
         .withCode(new QCodeReference(BulkInsertPrepareValueMappingStep.class));

      QFrontendStepMetaData valueMappingScreen = new QFrontendStepMetaData()
         .withName("valueMapping")
         .withLabel("Value Mapping")
         .withBackStepName("prepareFileMapping")
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_VALUE_MAPPING_FORM));

      QBackendStepMetaData receiveValueMappingStep = new QBackendStepMetaData()
         .withName("receiveValueMapping")
         .withCode(new QCodeReference(BulkInsertReceiveValueMappingStep.class));

      int i = 0;
      process.withStep(i++, prepareFileUploadStep);
      process.withStep(i++, uploadScreen);

      process.withStep(i++, prepareFileMappingStep);
      process.withStep(i++, fileMappingScreen);
      process.withStep(i++, receiveFileMappingStep);

      process.withStep(i++, prepareValueMappingStep);
      process.withStep(i++, valueMappingScreen);
      process.withStep(i++, receiveValueMappingStep);

      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW).setRecordListFields(editableFields);

      //////////////////////////////////////////////////////////////////////////////////////////
      // put the bulk-load profile form (e.g., for saving it) on the review & result screens) //
      //////////////////////////////////////////////////////////////////////////////////////////
      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW)
         .withBackStepName("prepareFileMapping")
         .getComponents().add(0, new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_PROFILE_FORM));

      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_RESULT)
         .getComponents().add(0, new QFrontendComponentMetaData().withType(QComponentType.BULK_LOAD_PROFILE_FORM));

      qInstance.addProcess(process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QPossibleValueSource defineTableKeyFieldsPossibleValueSource()
   {
      return (new QPossibleValueSource()
         .withName(TableKeyFieldsPossibleValueSource.NAME)
         .withType(QPossibleValueSourceType.CUSTOM)
         .withCustomCodeReference(new QCodeReference(TableKeyFieldsPossibleValueSource.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineTableBulkDelete(QInstance qInstance, QTableMetaData table, String processName)
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, table.getName());
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, table.getName());
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, StreamedETLWithFrontendProcess.DEFAULT_PREVIEW_MESSAGE_FOR_DELETE);

      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
            ExtractViaQueryStep.class,
            BulkDeleteTransformStep.class,
            BulkDeleteLoadStep.class,
            values
         )
         .withName(processName)
         .withLabel(table.getLabel() + " Bulk Delete")
         .withTableName(table.getName())
         .withIsHidden(true)
         .withPermissionRules(qInstance.getDefaultPermissionRules().clone()
            .withCustomPermissionChecker(new QCodeReference(BulkTableActionProcessPermissionChecker.class)));

      List<QFieldMetaData> tableFields = table.getFields().values().stream().toList();
      process.getFrontendStep("review").setRecordListFields(tableFields);

      qInstance.addProcess(process);
   }



   /*******************************************************************************
    ** for all fields in a table, set their backendName, using the default "inference" logic
    ** see {@link #inferBackendName(String)}
    *******************************************************************************/
   public static void setInferredFieldBackendNames(QTableMetaData tableMetaData)
   {
      if(tableMetaData == null)
      {
         LOG.warn("Requested to infer field backend names with a null table as input.  Returning with noop.");
         return;
      }

      if(CollectionUtils.nullSafeIsEmpty(tableMetaData.getFields()))
      {
         LOG.warn("Requested to infer field backend names on a table [" + tableMetaData.getName() + "] with no fields.  Returning with noop.");
         return;
      }

      for(QFieldMetaData field : tableMetaData.getFields().values())
      {
         String fieldName        = field.getName();
         String fieldBackendName = field.getBackendName();
         if(!StringUtils.hasContent(fieldBackendName))
         {
            String backendName = inferBackendName(fieldName);
            field.setBackendName(backendName);
         }
      }
   }



   /*******************************************************************************
    ** Do a default mapping from a camelCase field name to an underscore_style
    ** name for a backend.
    **
    ** Examples:
    ** <ul>
    **   <li>wordAnotherWordMoreWords -> word_another_word_more_words</li>
    **   <li>lUlUlUl -> l_ul_ul_ul</li>
    **   <li>StartsUpper -> starts_upper</li>
    **   <li>TLAFirst -> tla_first</li>
    **   <li>wordThenTLAInMiddle -> word_then_tla_in_middle</li>
    **   <li>endWithTLA -> end_with_tla</li>
    **   <li>TLAAndAnotherTLA -> tla_and_another_tla</li>
    ** </ul>
    *******************************************************************************/
   public static String inferBackendName(String fieldName)
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // build a list of words in the name, then join them with _ and lower-case the result //
      ////////////////////////////////////////////////////////////////////////////////////////
      List<String>  words       = new ArrayList<>();
      StringBuilder currentWord = new StringBuilder();
      for(int i = 0; i < fieldName.length(); i++)
      {
         Character thisChar = fieldName.charAt(i);
         Character nextChar = i < (fieldName.length() - 1) ? fieldName.charAt(i + 1) : null;

         /////////////////////////////////////////////////////////////////////////////////////
         // if we're at the end of the whole string, then we're at the end of the last word //
         /////////////////////////////////////////////////////////////////////////////////////
         if(nextChar == null)
         {
            currentWord.append(thisChar);
            words.add(currentWord.toString());
         }

         ///////////////////////////////////////////////////////////
         // transitioning from a lower to an upper starts a word. //
         ///////////////////////////////////////////////////////////
         else if(Character.isLowerCase(thisChar) && Character.isUpperCase(nextChar))
         {
            currentWord.append(thisChar);
            words.add(currentWord.toString());
            currentWord = new StringBuilder();
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // transitioning from an upper to a lower - it starts a word, as long as there were already letters in the current word                                    //
         // e.g., on wordThenTLAInMiddle, when thisChar=I and nextChar=n.  currentWord will be "TLA".  So finish that word, and start a new one with the 'I'        //
         // but the normal single-upper condition, e.g., firstName, when thisChar=N and nextChar=a, current word will be empty string, so just append the 'a' to it //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         else if(Character.isUpperCase(thisChar) && Character.isLowerCase(nextChar) && currentWord.length() > 0)
         {
            words.add(currentWord.toString());
            currentWord = new StringBuilder();
            currentWord.append(thisChar);
         }

         /////////////////////////////////////////////////////////////
         // by default, just add this character to the current word //
         /////////////////////////////////////////////////////////////
         else
         {
            currentWord.append(thisChar);
         }
      }

      return (String.join("_", words).toLowerCase(Locale.ROOT));
   }



   /*******************************************************************************
    ** Do a default mapping from an underscore_style field name to a camelCase name.
    **
    ** Examples:
    ** <ul>
    **   <li>word_another_word_more_words -> wordAnotherWordMoreWords</li>
    **   <li>l_ul_ul_ul -> lUlUlUl</li>
    **   <li>tla_first -> tlaFirst</li>
    **   <li>word_then_tla_in_middle -> wordThenTlaInMiddle</li>
    **   <li>end_with_tla -> endWithTla</li>
    **   <li>tla_and_another_tla -> tlaAndAnotherTla</li>
    **   <li>ALL_CAPS -> allCaps</li>
    ** </ul>
    *******************************************************************************/
   public static String inferNameFromBackendName(String backendName)
   {
      StringBuilder rs = new StringBuilder();

      ////////////////////////////////////////////////////////////////////////////////////////
      // build a list of words in the name, then join them with _ and lower-case the result //
      ////////////////////////////////////////////////////////////////////////////////////////
      String[] words = backendName.toLowerCase(Locale.ROOT).split("_");
      for(int i = 0; i < words.length; i++)
      {
         String word = words[i];
         if(i == 0)
         {
            rs.append(word);
         }
         else
         {
            rs.append(word.substring(0, 1).toUpperCase());
            if(word.length() > 1)
            {
               rs.append(word.substring(1));
            }
         }
      }

      return (rs.toString());
   }



   /*******************************************************************************
    ** If a app didn't have any sections, generate "sensible defaults"
    *******************************************************************************/
   private void generateAppSections(QAppMetaData app)
   {
      if(CollectionUtils.nullSafeIsEmpty(app.getChildren()))
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////
         // assume this app is valid if it has no children, but surely it doesn't need any sections then. //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         return;
      }

      //////////////////////////////////////////////////////////////////////////////
      // create an identity section for the id and any fields in the record label //
      //////////////////////////////////////////////////////////////////////////////
      QAppSection defaultSection = new QAppSection(app.getName(), app.getLabel(), new QIcon("badge"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

      boolean foundNonAppChild = false;
      if(CollectionUtils.nullSafeHasContents(app.getChildren()))
      {
         for(QAppChildMetaData child : app.getChildren())
         {
            //////////////////////////////////////////////////////////////////////////////////////////
            // only tables, processes, and reports are allowed to be in sections at this time, apps //
            // might be children but not in sections so keep track if we find any non-app           //
            //////////////////////////////////////////////////////////////////////////////////////////
            if(child.getClass().equals(QTableMetaData.class))
            {
               defaultSection.getTables().add(child.getName());
               foundNonAppChild = true;
            }
            else if(child.getClass().equals(QProcessMetaData.class))
            {
               defaultSection.getProcesses().add(child.getName());
               foundNonAppChild = true;
            }
            else if(child.getClass().equals(QReportMetaData.class))
            {
               defaultSection.getReports().add(child.getName());
               foundNonAppChild = true;
            }
         }
      }

      if(foundNonAppChild)
      {
         app.addSection(defaultSection);
      }
   }



   /*******************************************************************************
    ** If a table didn't have any sections, generate "sensible defaults"
    *******************************************************************************/
   private void generateTableFieldSections(QTableMetaData table)
   {
      if(CollectionUtils.nullSafeIsEmpty(table.getFields()))
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // assume this table is invalid if it has no fields, but surely it doesn't need any sections then. //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         return;
      }

      //////////////////////////////////////////////////////////////////////////////
      // create an identity section for the id and any fields in the record label //
      //////////////////////////////////////////////////////////////////////////////
      QFieldSection identitySection = new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, new ArrayList<>());

      Set<String> usedFieldNames = new HashSet<>();

      if(StringUtils.hasContent(table.getPrimaryKeyField()))
      {
         identitySection.getFieldNames().add(table.getPrimaryKeyField());
         usedFieldNames.add(table.getPrimaryKeyField());
      }

      if(CollectionUtils.nullSafeHasContents(table.getRecordLabelFields()))
      {
         for(String fieldName : table.getRecordLabelFields())
         {
            if(!usedFieldNames.contains(fieldName) && table.getFields().containsKey(fieldName))
            {
               identitySection.getFieldNames().add(fieldName);
               usedFieldNames.add(fieldName);
            }
         }
      }

      if(!identitySection.getFieldNames().isEmpty())
      {
         table.addSection(identitySection);
      }

      ///////////////////////////////////////////////////////////////////////////////
      // if there are more fields, then add them in a default/Other Fields section //
      ///////////////////////////////////////////////////////////////////////////////
      QFieldSection otherSection = new QFieldSection("otherFields", "Other Fields", new QIcon("dataset"), Tier.T2, new ArrayList<>());
      if(CollectionUtils.nullSafeHasContents(table.getFields()))
      {
         for(String fieldName : table.getFields().keySet())
         {
            QFieldMetaData field = table.getField(fieldName);
            if(!field.getIsHidden() && !usedFieldNames.contains(fieldName))
            {
               otherSection.getFieldNames().add(fieldName);
               usedFieldNames.add(fieldName);
            }
         }
      }

      if(!otherSection.getFieldNames().isEmpty())
      {
         table.addSection(otherSection);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enrichPossibleValueSource(QPossibleValueSource possibleValueSource)
   {
      if(QPossibleValueSourceType.TABLE.equals(possibleValueSource.getType()))
      {
         if(CollectionUtils.nullSafeIsEmpty(possibleValueSource.getSearchFields()))
         {
            QTableMetaData table = qInstance.getTable(possibleValueSource.getTableName());
            if(table != null)
            {
               if(table.getPrimaryKeyField() != null)
               {
                  possibleValueSource.withSearchField(table.getPrimaryKeyField());
               }

               for(String recordLabelField : CollectionUtils.nonNullList(table.getRecordLabelFields()))
               {
                  possibleValueSource.withSearchField(recordLabelField);
               }
            }
         }

         if(CollectionUtils.nullSafeIsEmpty(possibleValueSource.getOrderByFields()))
         {
            QTableMetaData table = qInstance.getTable(possibleValueSource.getTableName());
            if(table != null)
            {
               for(String recordLabelField : CollectionUtils.nonNullList(table.getRecordLabelFields()))
               {
                  possibleValueSource.withOrderByField(recordLabelField);
               }

               if(table.getPrimaryKeyField() != null)
               {
                  possibleValueSource.withOrderByField(table.getPrimaryKeyField());
               }
            }
         }

         if(possibleValueSource.getIdType() == null)
         {
            QTableMetaData table = qInstance.getTable(possibleValueSource.getTableName());
            if(table != null && table.getFields() != null)
            {
               String         primaryKeyField         = table.getPrimaryKeyField();
               QFieldMetaData primaryKeyFieldMetaData = CollectionUtils.nonNullMap(table.getFields()).get(primaryKeyField);
               if(primaryKeyFieldMetaData != null)
               {
                  possibleValueSource.setIdType(primaryKeyFieldMetaData.getType());
               }
            }
         }
      }
      else if(QPossibleValueSourceType.ENUM.equals(possibleValueSource.getType()))
      {
         if(possibleValueSource.getIdType() == null)
         {
            if(CollectionUtils.nullSafeHasContents(possibleValueSource.getEnumValues()))
            {
               Object id = possibleValueSource.getEnumValues().get(0).getId();
               try
               {
                  possibleValueSource.setIdType(QFieldType.fromClass(id.getClass()));
               }
               catch(Exception e)
               {
                  LOG.warn("Error enriching possible value source with idType based on first enum value", e, logPair("possibleValueSource", possibleValueSource.getName()), logPair("id", id));
               }
            }
         }
      }
      else if(QPossibleValueSourceType.CUSTOM.equals(possibleValueSource.getType()))
      {
         if(possibleValueSource.getIdType() == null)
         {
            try
            {
               QCustomPossibleValueProvider<?> customPossibleValueProvider = QCodeLoader.getAdHoc(QCustomPossibleValueProvider.class, possibleValueSource.getCustomCodeReference());

               Method getPossibleValueMethod = customPossibleValueProvider.getClass().getMethod("getPossibleValue", Serializable.class);
               Type   returnType             = getPossibleValueMethod.getGenericReturnType();
               Type   idType                 = ((ParameterizedType) returnType).getActualTypeArguments()[0];

               if(idType instanceof Class<?> c)
               {
                  possibleValueSource.setIdType(QFieldType.fromClass(c));
               }
            }
            catch(Exception e)
            {
               LOG.warn("Error enriching possible value source with idType based on first custom value", e, logPair("possibleValueSource", possibleValueSource.getName()));
            }
         }
      }

      runPlugins(QPossibleValueSource.class, possibleValueSource, qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void addEnricherPlugin(QInstanceEnricherPluginInterface<?> plugin)
   {
      Optional<Method> enrichMethod = Arrays.stream(plugin.getClass().getDeclaredMethods())
         .filter(m -> m.getName().equals("enrich")
            && m.getParameterCount() == 2
            && !m.getParameterTypes()[0].equals(Object.class)
            && m.getParameterTypes()[1].equals(QInstance.class)
         ).findFirst();

      if(enrichMethod.isPresent())
      {
         Class<?> parameterType = enrichMethod.get().getParameterTypes()[0];

         Set<String> existingPluginIdentifiers = enricherPlugins.getOrDefault(parameterType, Collections.emptyList())
            .stream().map(p -> p.getPluginIdentifier())
            .collect(Collectors.toSet());
         if(existingPluginIdentifiers.contains(plugin.getPluginIdentifier()))
         {
            LOG.debug("Enricher plugin is already registered - not re-adding it", logPair("pluginIdentifer", plugin.getPluginIdentifier()));
         }
         else
         {
            enricherPlugins.add(parameterType, plugin);
         }
      }
      else
      {
         LOG.warn("Could not find enrich method on enricher plugin [" + plugin.getClass().getName() + "] (to infer type being enriched) - this plugin will not be used.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void removeAllEnricherPlugins()
   {
      enricherPlugins.clear();
   }



   /*******************************************************************************
    ** Getter for enricherPlugins
    **
    *******************************************************************************/
   public static ListingHash<Class<?>, QInstanceEnricherPluginInterface<?>> getEnricherPlugins()
   {
      return enricherPlugins;
   }



   /***************************************************************************
    ** scan the classpath for classes in the specified package name which
    ** implement the QInstanceEnricherPluginInterface - any found get added
    ***************************************************************************/
   public static void discoverAndAddPluginsInPackage(String packageName) throws QException
   {
      try
      {
         for(Class<?> aClass : ClassPathUtils.getClassesInPackage(packageName))
         {
            if(QInstanceEnricherPluginInterface.class.isAssignableFrom(aClass))
            {
               QInstanceEnricherPluginInterface<?> plugin = (QInstanceEnricherPluginInterface<?>) aClass.getConstructor().newInstance();
               addEnricherPlugin(plugin);
            }
         }
      }
      catch(Exception e)
      {
         throw (new QException("Error discovering and adding enricher plugins in package [" + packageName + "]", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <T> void runPlugins(Class<T> c, T t, QInstance qInstance)
   {
      for(QInstanceEnricherPluginInterface<?> plugin : CollectionUtils.nonNullList(enricherPlugins.get(c)))
      {
         @SuppressWarnings("unchecked")
         QInstanceEnricherPluginInterface<T> castedPlugin = (QInstanceEnricherPluginInterface<T>) plugin;
         if(castedPlugin.isEnabled())
         {
            castedPlugin.enrich(t, qInstance);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public JoinGraph getJoinGraph()
   {
      return (this.joinGraph);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void addLabelMapping(String from, String to)
   {
      labelMappings.put(from, to);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void removeLabelMapping(String from)
   {
      labelMappings.remove(from);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void clearLabelMappings()
   {
      labelMappings.clear();
   }



   /*******************************************************************************
    ** Getter for configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels
    *******************************************************************************/
   public boolean getConfigRemoveIdFromNameWhenCreatingPossibleValueFieldLabels()
   {
      return (this.configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels);
   }



   /*******************************************************************************
    ** Setter for configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels
    *******************************************************************************/
   public void setConfigRemoveIdFromNameWhenCreatingPossibleValueFieldLabels(boolean configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels)
   {
      this.configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels = configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels;
   }



   /*******************************************************************************
    ** Fluent setter for configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels
    *******************************************************************************/
   public QInstanceEnricher withConfigRemoveIdFromNameWhenCreatingPossibleValueFieldLabels(boolean configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels)
   {
      this.configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels = configRemoveIdFromNameWhenCreatingPossibleValueFieldLabels;
      return (this);
   }



   /*******************************************************************************
    ** Getter for configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate
    *******************************************************************************/
   public boolean getConfigAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate()
   {
      return (this.configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate);
   }



   /*******************************************************************************
    ** Setter for configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate
    *******************************************************************************/
   public void setConfigAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate(boolean configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate)
   {
      this.configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate = configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate
    *******************************************************************************/
   public QInstanceEnricher withConfigAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate(boolean configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate)
   {
      this.configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate = configAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate;
      return (this);
   }



   /*******************************************************************************
    ** For any QFieldMetaData that has a PVS filter referencing ${input.someVar},
    ** add a process-level fallback so it becomes:
    **   ${input.someVar}??${processValues.possibleValueFilterValueSomeVar}
    **
    ** This keeps table definitions clean (${input.x} only) and pushes knowledge
    ** of process fallbacks into the enricher-generated processes.
    *******************************************************************************/
   private static QFieldMetaData addProcessFallbacksToPvsFilter(QFieldMetaData field)
   {
      QQueryFilter filter = field.getPossibleValueSourceFilter();
      if(filter == null)
      {
         return field;
      }

      ////////////////////////////////////////////////////////////////////////////////
      // Transform each criterion value that is a String containing ${input.<name>} //
      ////////////////////////////////////////////////////////////////////////////////
      List<QFilterCriteria> newCriteria = new ArrayList<>();
      for(QFilterCriteria c : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         Serializable[]     newValues = null;
         List<Serializable> vals      = new ArrayList<>();
         for(Serializable v : CollectionUtils.nonNullList(c.getValues()))
         {
            if(v instanceof String s)
            {
               String transformed = java.util.regex.Pattern
                  .compile("\\$\\{input\\.([A-Za-z0-9_]+)}")
                  .matcher(s)
                  .replaceAll(mr ->
                  {
                     String var = mr.group(1);
                     String cap = var.substring(0, 1).toUpperCase(Locale.ROOT) + (var.length() > 1 ? var.substring(1) : "");
                     return java.util.regex.Matcher.quoteReplacement("${input." + var + "}??${processValues.possibleValueFilterValue" + cap + "}");
                  });
               vals.add(transformed);
            }
            else
            {
               vals.add(v);
            }
         }
         newValues = vals.toArray(new Serializable[0]);
         newCriteria.add(new QFilterCriteria(c.getFieldName(), c.getOperator(), newValues));
      }

      QQueryFilter newFilter = new QQueryFilter();
      for(QFilterCriteria nc : newCriteria)
      {
         newFilter.addCriteria(nc);
      }
      ///////////////////////////////
      // copy order-bys if present //
      ///////////////////////////////
      for(QFilterOrderBy ob : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         newFilter.withOrderBy(new QFilterOrderBy(ob.getFieldName(), ob.getIsAscending()));
      }

      field.withPossibleValueSourceFilter(newFilter);
      return field;
   }
}
