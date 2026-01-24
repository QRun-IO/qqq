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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.audits.ProcessedAuditHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.JoinGraph;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataActionCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataFilterInterface;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.ReportCustomRecordSourceInterface;
import com.kingsrook.qqq.backend.core.actions.scripts.TestScriptActionInterface;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerType;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditHandlerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.automation.QAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.ParentWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QSupplementalFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QSupplementalProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QueueType;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTracking;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantSetting;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeLambda;
import org.apache.commons.lang3.BooleanUtils;
import org.quartz.CronExpression;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class that knows how to take a look at the data in a QInstance, and report
 ** if it is all valid - e.g., non-null things are set; references line-up (e.g.,
 ** a table's backend must be a defined backend).
 **
 ** Prior to doing validation, the the QInstanceEnricher is ran over the QInstance,
 ** e.g., to fill in things that can be defaulted or assumed.  TODO let the instance
 ** customize or opt-out of Enrichment.
 **
 *******************************************************************************/
public class QInstanceValidator
{
   private static final QLogger LOG = QLogger.getLogger(QInstanceValidator.class);

   private boolean printWarnings = false;

   private static ListingHash<Class<?>, QInstanceValidatorPluginInterface<?>> validatorPlugins = new ListingHash<>();

   private JoinGraph joinGraph = null;

   private List<String> errors = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance qInstance) throws QInstanceValidationException
   {
      if(qInstance.getHasBeenValidated() || qInstance.getValidationIsRunning())
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         // don't re-validate if previously complete or currently running (avoids recursive re-validation chaos!) //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         return;
      }

      ////////////////////////////////////
      // mark validation as running now //
      ////////////////////////////////////
      QInstanceValidationKey validationKey = new QInstanceValidationKey();
      qInstance.setValidationIsRunning(validationKey);

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // the enricher will build a join graph (if there are any joins).  we'd like to only do that       //
      // once, during the enrichment/validation work, so, capture it, and store it back in the instance. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      long start = System.currentTimeMillis();
      try
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////
         // before validation, enrich the object (e.g., to fill in values that the user doesn't have to //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         // TODO - possible point of customization (use a different enricher, or none, or pass it options).
         QInstanceEnricher qInstanceEnricher = new QInstanceEnricher(qInstance);
         qInstanceEnricher.enrich();
         this.joinGraph = qInstanceEnricher.getJoinGraph();
      }
      catch(Exception e)
      {
         System.out.println();
         LOG.error("Error enriching instance prior to validation", e);
         System.out.println();
         throw (new QInstanceValidationException("Error enriching qInstance prior to validation.", e));
      }

      //////////////////////////////////////////////////////////////////////////
      // do the validation checks - a good qInstance has all conditions TRUE! //
      //////////////////////////////////////////////////////////////////////////
      try
      {
         validateInstanceAttributes(qInstance);
         validateBackends(qInstance);
         validateAuthentication(qInstance);
         validateScopedAuthentication(qInstance);
         validateAutomationProviders(qInstance);
         validateAuditHandlers(qInstance);
         validateTables(qInstance, joinGraph);
         validateProcesses(qInstance);
         validateReports(qInstance);
         validateApps(qInstance);
         validateWidgets(qInstance);
         validatePossibleValueSources(qInstance);
         validateQueuesAndProviders(qInstance);
         validateJoins(qInstance);
         validateSecurityKeyTypes(qInstance);
         validateSupplementalMetaData(qInstance);
         validateSupplementalCustomizers(qInstance);

         validateUniqueTopLevelNames(qInstance);

         runPlugins(QInstance.class, qInstance, qInstance);

         long end = System.currentTimeMillis();
         LOG.info("Validation (and enrichment) performance", logPair("millis", (end - start)));
      }
      catch(Exception e)
      {
         throw (new QInstanceValidationException("Error performing qInstance validation.", e));
      }

      if(!errors.isEmpty())
      {
         throw (new QInstanceValidationException(errors));
      }

      //////////////////////////////
      // mark validation complete //
      //////////////////////////////
      qInstance.setJoinGraph(validationKey, joinGraph);
      qInstance.setHasBeenValidated(validationKey);
   }



   /***************************************************************************
    * this method still supports the deprecated MetaDataFilter (plus its
    * replacement, MetaDataActionCustomizer
    ***************************************************************************/
   @SuppressWarnings("deprecation")
   private void validateInstanceAttributes(QInstance qInstance)
   {
      if(qInstance.getMetaDataFilter() != null)
      {
         validateSimpleCodeReference("Instance metaDataFilter ", qInstance.getMetaDataFilter(), MetaDataFilterInterface.class);
      }

      if(qInstance.getMetaDataActionCustomizer() != null)
      {
         validateSimpleCodeReference("Instance metaDataActionCustomizer ", qInstance.getMetaDataActionCustomizer(), MetaDataActionCustomizerInterface.class);
      }

      if(qInstance.getTableCustomizers() != null)
      {
         for(Map.Entry<String, List<QCodeReference>> entry : qInstance.getTableCustomizers().entrySet())
         {
            for(QCodeReference codeReference : CollectionUtils.nonNullList(entry.getValue()))
            {
               validateSimpleCodeReference("Instance tableCustomizer of type " + entry.getKey() + ": ", codeReference, TableCustomizerInterface.class);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void revalidate(QInstance qInstance) throws QInstanceValidationException
   {
      qInstance.setHasBeenValidated(null);
      validate(qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void addValidatorPlugin(QInstanceValidatorPluginInterface<?> plugin)
   {
      Optional<Method> validateMethod = Arrays.stream(plugin.getClass().getDeclaredMethods())
         .filter(m -> m.getName().equals("validate")
            && m.getParameterCount() == 3
            && !m.getParameterTypes()[0].equals(Object.class)
            && m.getParameterTypes()[1].equals(QInstance.class)
            && m.getParameterTypes()[2].equals(QInstanceValidator.class)
         ).findFirst();

      if(validateMethod.isPresent())
      {
         Class<?> parameterType = validateMethod.get().getParameterTypes()[0];

         Set<String> existingPluginIdentifiers = validatorPlugins.getOrDefault(parameterType, Collections.emptyList())
            .stream().map(p -> p.getPluginIdentifier())
            .collect(Collectors.toSet());
         if(existingPluginIdentifiers.contains(plugin.getPluginIdentifier()))
         {
            LOG.debug("Validator plugin is already registered - not re-adding it", logPair("pluginIdentifer", plugin.getPluginIdentifier()));
         }
         else
         {
            validatorPlugins.add(parameterType, plugin);
         }
      }
      else
      {
         LOG.warn("Could not find validate method on validator plugin [" + plugin.getClass().getName() + "] (to infer type being validated) - this plugin will not be used.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void removeAllValidatorPlugins()
   {
      validatorPlugins.clear();
   }



   /*******************************************************************************
    ** Getter for validatorPlugins
    **
    *******************************************************************************/
   public static ListingHash<Class<?>, QInstanceValidatorPluginInterface<?>> getValidatorPlugins()
   {
      return validatorPlugins;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <T> void runPlugins(Class<T> c, T t, QInstance qInstance)
   {
      for(QInstanceValidatorPluginInterface<?> plugin : CollectionUtils.nonNullList(validatorPlugins.get(c)))
      {
         @SuppressWarnings("unchecked")
         QInstanceValidatorPluginInterface<T> castedPlugin = (QInstanceValidatorPluginInterface<T>) plugin;
         if(castedPlugin.isEnabled())
         {
            castedPlugin.validate(t, qInstance, this);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateSupplementalMetaData(QInstance qInstance)
   {
      for(QSupplementalInstanceMetaData supplementalInstanceMetaData : CollectionUtils.nonNullMap(qInstance.getSupplementalMetaData()).values())
      {
         supplementalInstanceMetaData.validate(qInstance, this);

         runPlugins(QSupplementalInstanceMetaData.class, supplementalInstanceMetaData, qInstance);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateSecurityKeyTypes(QInstance qInstance)
   {
      Set<String> usedNames = new HashSet<>();
      qInstance.getSecurityKeyTypes().forEach((name, securityKeyType) ->
      {
         if(assertCondition(StringUtils.hasContent(securityKeyType.getName()), "Missing name for a securityKeyType"))
         {
            assertCondition(Objects.equals(name, securityKeyType.getName()), "Inconsistent naming for securityKeyType: " + name + "/" + securityKeyType.getName() + ".");

            String duplicateNameMessagePrefix = "More than one SecurityKeyType with name (or allAccessKeyName or nullValueBehaviorKeyName) of: ";
            assertCondition(!usedNames.contains(name), duplicateNameMessagePrefix + name);
            usedNames.add(name);

            if(StringUtils.hasContent(securityKeyType.getAllAccessKeyName()))
            {
               assertCondition(!usedNames.contains(securityKeyType.getAllAccessKeyName()), duplicateNameMessagePrefix + securityKeyType.getAllAccessKeyName());
               usedNames.add(securityKeyType.getAllAccessKeyName());
            }

            if(StringUtils.hasContent(securityKeyType.getNullValueBehaviorKeyName()))
            {
               assertCondition(!usedNames.contains(securityKeyType.getNullValueBehaviorKeyName()), duplicateNameMessagePrefix + securityKeyType.getNullValueBehaviorKeyName());
               usedNames.add(securityKeyType.getNullValueBehaviorKeyName());
            }

            if(StringUtils.hasContent(securityKeyType.getPossibleValueSourceName()))
            {
               assertCondition(qInstance.getPossibleValueSource(securityKeyType.getPossibleValueSourceName()) != null, "Unrecognized possibleValueSourceName in securityKeyType: " + name);
            }

            runPlugins(QSecurityKeyType.class, securityKeyType, qInstance);
         }
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateJoins(QInstance qInstance)
   {
      qInstance.getJoins().forEach((joinName, join) ->
      {
         assertCondition(Objects.equals(joinName, join.getName()), "Inconsistent naming for join: " + joinName + "/" + join.getName() + ".");

         assertCondition(StringUtils.hasContent(join.getLeftTable()), "Missing left-table name in join: " + joinName);
         assertCondition(StringUtils.hasContent(join.getRightTable()), "Missing right-table name in join: " + joinName);
         assertCondition(join.getType() != null, "Missing type for join: " + joinName);
         assertCondition(CollectionUtils.nullSafeHasContents(join.getJoinOns()), "Missing joinOns for join: " + joinName);

         boolean leftTableExists  = assertCondition(qInstance.getTable(join.getLeftTable()) != null, "Left-table name " + join.getLeftTable() + " in join " + joinName + " is not a defined table in this instance.");
         boolean rightTableExists = assertCondition(qInstance.getTable(join.getRightTable()) != null, "Right-table name " + join.getRightTable() + " in join " + joinName + " is not a defined table in this instance.");

         for(JoinOn joinOn : CollectionUtils.nonNullList(join.getJoinOns()))
         {
            assertCondition(StringUtils.hasContent(joinOn.getLeftField()), "Missing left-field name in a joinOn for join: " + joinName);
            assertCondition(StringUtils.hasContent(joinOn.getRightField()), "Missing right-field name in a joinOn for join: " + joinName);

            if(leftTableExists)
            {
               assertNoException(() -> qInstance.getTable(join.getLeftTable()).getField(joinOn.getLeftField()), "Left field name in joinOn " + joinName + " is not a defined field in table " + join.getLeftTable());
            }

            if(rightTableExists)
            {
               assertNoException(() -> qInstance.getTable(join.getRightTable()).getField(joinOn.getRightField()), "Right field name in joinOn " + joinName + " is not a defined field in table " + join.getRightTable());
            }
         }

         for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(join.getOrderBys()))
         {
            if(rightTableExists)
            {
               assertNoException(() -> qInstance.getTable(join.getRightTable()).getField(orderBy.getFieldName()), "Field name " + orderBy.getFieldName() + " in orderBy for join " + joinName + " is not a defined field in the right-table " + join.getRightTable());
            }
         }

         runPlugins(QJoinMetaData.class, join, qInstance);
      });
   }



   /*******************************************************************************
    ** there can be some unexpected bad-times if you have a table and process, or
    ** table and app (etc) with the same name (e.g., in app tree building).  So,
    ** just go ahead and make sure those are all unique.
    *******************************************************************************/
   private void validateUniqueTopLevelNames(QInstance qInstance)
   {
      String      suffix    = " is not unique across tables, processes, and apps (but it needs to be)";
      Set<String> usedNames = new HashSet<>();
      if(qInstance.getTables() != null)
      {
         for(String tableName : qInstance.getTables().keySet())
         {
            assertCondition(!usedNames.contains(tableName), "Table name " + tableName + suffix);
            usedNames.add(tableName);
         }
      }

      if(qInstance.getProcesses() != null)
      {
         for(String processName : qInstance.getProcesses().keySet())
         {
            assertCondition(!usedNames.contains(processName), "Process name " + processName + suffix);
            usedNames.add(processName);
         }
      }

      if(qInstance.getApps() != null)
      {
         for(String appName : qInstance.getApps().keySet())
         {
            assertCondition(!usedNames.contains(appName), "App name " + appName + suffix);
            usedNames.add(appName);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateQueuesAndProviders(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getQueueProviders()))
      {
         qInstance.getQueueProviders().forEach((name, queueProvider) ->
         {
            assertCondition(Objects.equals(name, queueProvider.getName()), "Inconsistent naming for queueProvider: " + name + "/" + queueProvider.getName() + ".");
            assertCondition(queueProvider.getType() != null, "Missing type for queueProvider: " + name);

            if(queueProvider instanceof SQSQueueProviderMetaData sqsQueueProvider)
            {
               if(queueProvider.getType() != null)
               {
                  assertCondition(queueProvider.getType().equals(QueueType.SQS), "Inconsistent Type/class given for queueProvider: " + name + " (SQSQueueProviderMetaData is not allowed for type " + queueProvider.getType() + ")");
               }

               assertCondition(StringUtils.hasContent(sqsQueueProvider.getAccessKey()), "Missing accessKey for SQSQueueProvider: " + name);
               assertCondition(StringUtils.hasContent(sqsQueueProvider.getSecretKey()), "Missing secretKey for SQSQueueProvider: " + name);
               assertCondition(StringUtils.hasContent(sqsQueueProvider.getBaseURL()), "Missing baseURL for SQSQueueProvider: " + name);
               assertCondition(StringUtils.hasContent(sqsQueueProvider.getRegion()), "Missing region for SQSQueueProvider: " + name);
            }
            else if(queueProvider.getClass().equals(QQueueProviderMetaData.class))
            {
               /////////////////////////////////////////////////////////////////////
               // this just means a subtype wasn't used, so, it should be allowed //
               // (unless we had a case where a type required a subtype?)         //
               /////////////////////////////////////////////////////////////////////
            }
            else
            {
               if(queueProvider.getType() != null)
               {
                  assertCondition(!queueProvider.getType().equals(QueueType.SQS), "Inconsistent Type/class given for queueProvider: " + name + " (" + queueProvider.getClass().getSimpleName() + " is not allowed for type " + queueProvider.getType() + ")");
               }
            }

            runPlugins(QQueueProviderMetaData.class, queueProvider, qInstance);
         });
      }

      if(CollectionUtils.nullSafeHasContents(qInstance.getQueues()))
      {
         qInstance.getQueues().forEach((name, queue) ->
         {
            assertCondition(Objects.equals(name, queue.getName()), "Inconsistent naming for queue: " + name + "/" + queue.getName() + ".");

            QQueueProviderMetaData queueProvider = qInstance.getQueueProvider(queue.getProviderName());
            if(assertCondition(queueProvider != null, "Unrecognized queue providerName for queue: " + name))
            {
               if(queue instanceof SQSQueueMetaData)
               {
                  assertCondition(queueProvider.getType().equals(QueueType.SQS), "Inconsistent class given for queueMetaData: " + name + " (SQSQueueMetaData is not allowed for queue provider of type " + queueProvider.getType() + ")");
               }
               else if(queue.getClass().equals(QQueueMetaData.class))
               {
                  ////////////////////////////////////////////////////////////////////
                  // this just means a subtype wasn't used, so, it should be        //
                  // allowed (unless we had a case where a type required a subtype? //
                  ////////////////////////////////////////////////////////////////////
               }
               else
               {
                  assertCondition(!queueProvider.getType().equals(QueueType.SQS), "Inconsistent class given for queueProvider: " + name + " (" + queue.getClass().getSimpleName() + " is not allowed for type " + queueProvider.getType() + ")");
               }
            }

            assertCondition(StringUtils.hasContent(queue.getQueueName()), "Missing queueName for queue: " + name);
            if(assertCondition(StringUtils.hasContent(queue.getProcessName()), "Missing processName for queue: " + name))
            {
               assertCondition(qInstance.getProcesses() != null && qInstance.getProcess(queue.getProcessName()) != null, "Unrecognized processName for queue: " + name);
            }

            if(queue.getSchedule() != null)
            {
               validateScheduleMetaData(queue.getSchedule(), qInstance, "SQSQueueProvider " + name + ", schedule: ");
            }

            runPlugins(QQueueMetaData.class, queue, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateBackends(QInstance qInstance)
   {
      if(assertCondition(CollectionUtils.nullSafeHasContents(qInstance.getBackends()), "At least 1 backend must be defined."))
      {
         qInstance.getBackends().forEach((backendName, backend) ->
         {
            assertCondition(Objects.equals(backendName, backend.getName()), "Inconsistent naming for backend: " + backendName + "/" + backend.getName() + ".");

            ///////////////////////
            // validate variants //
            ///////////////////////
            BackendVariantsConfig backendVariantsConfig = backend.getBackendVariantsConfig();
            if(BooleanUtils.isTrue(backend.getUsesVariants()))
            {
               if(assertCondition(backendVariantsConfig != null, "Missing backendVariantsConfig in backend [" + backendName + "] which is marked as usesVariants"))
               {
                  assertCondition(StringUtils.hasContent(backendVariantsConfig.getVariantTypeKey()), "Missing variantTypeKey in backendVariantsConfig in [" + backendName + "]");

                  String         optionsTableName = backendVariantsConfig.getOptionsTableName();
                  QTableMetaData optionsTable     = qInstance.getTable(optionsTableName);
                  if(assertCondition(StringUtils.hasContent(optionsTableName), "Missing optionsTableName in backendVariantsConfig in [" + backendName + "]"))
                  {
                     if(assertCondition(optionsTable != null, "Unrecognized optionsTableName [" + optionsTableName + "] in backendVariantsConfig in [" + backendName + "]"))
                     {
                        QQueryFilter optionsFilter = backendVariantsConfig.getOptionsFilter();
                        if(optionsFilter != null)
                        {
                           validateQueryFilter(qInstance, "optionsFilter in backendVariantsConfig in backend [" + backendName + "]: ", optionsTable, optionsFilter, null);
                        }
                     }
                  }

                  Map<BackendVariantSetting, String> backendSettingSourceFieldNameMap = backendVariantsConfig.getBackendSettingSourceFieldNameMap();
                  if(assertCondition(CollectionUtils.nullSafeHasContents(backendSettingSourceFieldNameMap), "Missing or empty backendSettingSourceFieldNameMap in backendVariantsConfig in [" + backendName + "]"))
                  {
                     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     // only validate field names in the backendSettingSourceFieldNameMap if there is NOT a variantRecordSupplier //
                     // (the idea being, that the supplier might be building a record with fieldNames that aren't in the table... //
                     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     if(optionsTable != null && backendVariantsConfig.getVariantRecordLookupFunction() == null)
                     {
                        for(Map.Entry<BackendVariantSetting, String> entry : backendSettingSourceFieldNameMap.entrySet())
                        {
                           assertCondition(optionsTable.getFields().containsKey(entry.getValue()), "Unrecognized fieldName [" + entry.getValue() + "] in backendSettingSourceFieldNameMap in backendVariantsConfig in [" + backendName + "]");
                        }
                     }
                  }

                  if(backendVariantsConfig.getVariantRecordLookupFunction() != null)
                  {
                     validateSimpleCodeReference("VariantRecordSupplier in backendVariantsConfig in backend [" + backendName + "]: ", backendVariantsConfig.getVariantRecordLookupFunction(), UnsafeFunction.class, Function.class);
                  }
               }
            }
            else
            {
               assertCondition(backendVariantsConfig == null, "Should not have a backendVariantsConfig in backend [" + backendName + "] which is not marked as usesVariants");
            }

            ///////////////////////////////////////////
            // let the backend do its own validation //
            ///////////////////////////////////////////
            backend.performValidation(this);

            runPlugins(QBackendMetaData.class, backend, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateAutomationProviders(QInstance qInstance)
   {
      if(qInstance.getAutomationProviders() != null)
      {
         qInstance.getAutomationProviders().forEach((name, automationProvider) ->
         {
            assertCondition(Objects.equals(name, automationProvider.getName()), "Inconsistent naming for automationProvider: " + name + "/" + automationProvider.getName() + ".");
            assertCondition(automationProvider.getType() != null, "Missing type for automationProvider: " + name);

            runPlugins(QAutomationProviderMetaData.class, automationProvider, qInstance);
         });
      }
   }



   /*******************************************************************************
    ** Validate audit handlers.
    *******************************************************************************/
   private void validateAuditHandlers(QInstance qInstance)
   {
      if(qInstance.getAuditHandlers() != null)
      {
         qInstance.getAuditHandlers().forEach((name, handler) ->
         {
            String prefix = "AuditHandler " + name + ": ";
            assertCondition(Objects.equals(name, handler.getName()), prefix + "Inconsistent naming: " + name + "/" + handler.getName());
            assertCondition(handler.getHandlerType() != null, prefix + "Missing handlerType");
            assertCondition(handler.getHandlerCode() != null, prefix + "Missing handlerCode");

            if(handler.getHandlerCode() != null)
            {
               if(handler.getHandlerType() == AuditHandlerType.DML)
               {
                  validateSimpleCodeReference(prefix + "handlerCode ", handler.getHandlerCode(), DMLAuditHandlerInterface.class);
               }
               else if(handler.getHandlerType() == AuditHandlerType.PROCESSED)
               {
                  validateSimpleCodeReference(prefix + "handlerCode ", handler.getHandlerCode(), ProcessedAuditHandlerInterface.class);
               }
            }

            runPlugins(QAuditHandlerMetaData.class, handler, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateAuthentication(QInstance qInstance)
   {
      QAuthenticationMetaData authentication = qInstance.getAuthentication();
      if(assertCondition(authentication != null, "Authentication MetaData must be defined."))
      {
         if(authentication.getCustomizer() != null)
         {
            validateSimpleCodeReference("Instance Authentication meta data customizer ", authentication.getCustomizer(), QAuthenticationModuleCustomizerInterface.class);
         }

         authentication.validate(qInstance, this);

         runPlugins(QAuthenticationMetaData.class, authentication, qInstance);
      }
   }



   /*******************************************************************************
    ** Validate scoped authentication providers.
    **
    ** <p>Ensures:
    ** <ul>
    **   <li>If any scoped providers are registered, instance default must exist</li>
    **   <li>API scopes reference valid ApiInstanceMetaData</li>
    **   <li>Route provider scopes reference valid JavalinRouteProviderMetaData</li>
    ** </ul>
    ** </p>
    *******************************************************************************/
   private void validateScopedAuthentication(QInstance qInstance)
   {
      Map<AuthScope, QAuthenticationMetaData> scopedProviders =
         qInstance.getScopedAuthenticationProviders();
      if(scopedProviders == null || scopedProviders.isEmpty())
      {
         // No scoped providers - nothing to validate
         return;
      }

      ///////////////////////////////////////////////////////////////////////////
      // Check that instance default exists if any scoped providers exist    //
      ///////////////////////////////////////////////////////////////////////////
      boolean hasInstanceDefault =
         scopedProviders.containsKey(AuthScope.instanceDefault());
      boolean hasScopedProviders =
         scopedProviders.size() > (hasInstanceDefault ? 1 : 0);

      if(hasScopedProviders && !hasInstanceDefault)
      {
         errors.add("Instance default authentication provider must be "
            + "registered when scoped providers are used. Register using "
            + "registerAuthenticationProvider(AuthScope.instanceDefault(), "
            + "authMetaData) or setAuthentication(authMetaData).");
      }

      ///////////////////////////////////////////////////////////////////////////
      // Validate each scope references valid metadata                        //
      ///////////////////////////////////////////////////////////////////////////
      for(Map.Entry<AuthScope, QAuthenticationMetaData> entry : scopedProviders.entrySet())
      {
         AuthScope scope = entry.getKey();
         if(scope instanceof AuthScope.Api apiScope)
         {
            validateApiScope(qInstance, apiScope);
         }
         else if(scope instanceof AuthScope.RouteProvider routeScope)
         {
            validateRouteProviderScope(qInstance, routeScope);
         }
         // InstanceDefault scope doesn't need validation
      }
   }


   /*******************************************************************************
    ** Validate that an API scope references a valid ApiInstanceMetaData.
    **
    ** @param qInstance The QInstance to validate against
    ** @param apiScope The API scope to validate
    *******************************************************************************/
   private void validateApiScope(QInstance qInstance, AuthScope.Api apiScope)
   {
      Object apiMetaData = apiScope.getApiMetaData();
      if(apiMetaData == null)
      {
         errors.add("AuthScope.Api has null apiMetaData");
         return;
      }

      ///////////////////////////////////////////////////////////////////////////
      // Try to access API metadata container via reflection                  //
      ///////////////////////////////////////////////////////////////////////////
      try
      {
         QSupplementalInstanceMetaData apiContainer =
            qInstance.getSupplementalMetaData("api");
         if(apiContainer == null)
         {
            errors.add("AuthScope.Api references ApiInstanceMetaData but no "
               + "API metadata container found in QInstance.");
            return;
         }

         // Use reflection to get the apis map and check if this API exists
         Method getApisMethod = apiContainer.getClass().getMethod("getApis");
         Object apisMap = getApisMethod.invoke(apiContainer);
         if(apisMap instanceof Map)
         {
            // Try to get the API name from the metadata object
            Method getNameMethod = apiMetaData.getClass().getMethod("getName");
            Object apiName = getNameMethod.invoke(apiMetaData);
            if(apiName != null)
            {
               boolean exists =
                  ((Map<?, ?>)apisMap).containsKey(apiName.toString());
               if(!exists)
               {
                  errors.add("AuthScope.Api references ApiInstanceMetaData "
                     + "with name '" + apiName
                     + "' but no such API exists in "
                     + "ApiInstanceMetaDataContainer.");
               }
            }
         }
      }
      catch(Exception e)
      {
         // If we can't validate via reflection, log a warning but don't fail
         LOG.warn("Could not validate API scope reference", e);
      }
   }


   /*******************************************************************************
    ** Validate that a route provider scope references a valid
    ** JavalinRouteProviderMetaData.
    **
    ** @param qInstance The QInstance to validate against
    ** @param routeScope The route provider scope to validate
    *******************************************************************************/
   private void validateRouteProviderScope(QInstance qInstance,
      AuthScope.RouteProvider routeScope)
   {
      Object routeMetaData = routeScope.getRouteMetaData();
      if(routeMetaData == null)
      {
         errors.add("AuthScope.RouteProvider has null routeMetaData");
         return;
      }

      ///////////////////////////////////////////////////////////////////////////
      // Try to access Javalin metadata via reflection                        //
      ///////////////////////////////////////////////////////////////////////////
      try
      {
         QSupplementalInstanceMetaData javalinMetaData =
            qInstance.getSupplementalMetaData("javalin");
         if(javalinMetaData == null)
         {
            errors.add("AuthScope.RouteProvider references "
               + "JavalinRouteProviderMetaData but no Javalin metadata found "
               + "in QInstance");
            return;
         }

         // Use reflection to get the routeProviders list and check if route exists
         Method getRouteProvidersMethod =
            javalinMetaData.getClass().getMethod("getRouteProviders");
         Object routeProvidersList = getRouteProvidersMethod.invoke(javalinMetaData);
         if(routeProvidersList instanceof List)
         {
            // Try to get the route provider name from the metadata object
            Method getNameMethod = routeMetaData.getClass().getMethod("getName");
            Object routeName = getNameMethod.invoke(routeMetaData);
            if(routeName != null)
            {
               boolean exists = ((List<?>) routeProvidersList).stream()
                  .anyMatch(rp ->
                  {
                     try
                     {
                        Object rpName = rp.getClass().getMethod("getName").invoke(rp);
                        return routeName.equals(rpName);
                     }
                     catch(Exception e)
                     {
                        return false;
                     }
                  });
               if(!exists)
               {
                  errors.add("AuthScope.RouteProvider references "
                     + "JavalinRouteProviderMetaData with name '" + routeName
                     + "' but no such route provider exists in "
                     + "QJavalinMetaData");
               }
            }
         }
      }
      catch(Exception e)
      {
         // If we can't validate via reflection, log a warning but don't fail
         LOG.warn("Could not validate route provider scope reference", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTables(QInstance qInstance, JoinGraph joinGraph)
   {
      if(assertCondition(CollectionUtils.nullSafeHasContents(qInstance.getTables()), "At least 1 table must be defined."))
      {
         qInstance.getTables().forEach((tableName, table) ->
         {
            assertCondition(Objects.equals(tableName, table.getName()), "Inconsistent naming for table: " + tableName + "/" + table.getName() + ".");

            ////////////////////////////////////////
            // validate the backend for the table //
            ////////////////////////////////////////
            if(assertCondition(StringUtils.hasContent(table.getBackendName()), "Missing backend name for table " + tableName + "."))
            {
               if(CollectionUtils.nullSafeHasContents(qInstance.getBackends()))
               {
                  QBackendMetaData backendForTable = qInstance.getBackendForTable(tableName);
                  if(assertCondition(backendForTable != null, "Unrecognized backend " + table.getBackendName() + " for table " + tableName + "."))
                  {
                     ////////////////////////////////////////////////////////////
                     // if the backend requires primary keys, then validate it //
                     ////////////////////////////////////////////////////////////
                     if(backendForTable.requiresPrimaryKeyOnTables())
                     {
                        if(assertCondition(StringUtils.hasContent(table.getPrimaryKeyField()), "Missing primary key for table: " + tableName))
                        {
                           assertNoException(() -> table.getField(table.getPrimaryKeyField()), "Primary key for table " + tableName + " is not a recognized field on this table.");
                        }
                     }
                  }
               }
            }

            //////////////////////////////////
            // validate fields in the table //
            //////////////////////////////////
            Set<String> usedFieldNames = new HashSet<>();
            if(assertCondition(CollectionUtils.nullSafeHasContents(table.getFields()), "At least 1 field must be defined in table " + tableName + "."))
            {
               table.getFields().forEach((fieldName, field) ->
               {
                  if(usedFieldNames.contains(fieldName))
                  {
                     errors.add("Duplicated field name [" + fieldName + "] in table " + tableName);
                  }
                  usedFieldNames.add(fieldName);

                  validateTableField(qInstance, tableName, fieldName, table, field);
               });
            }

            /////////////////////////////
            // validate virtual fields //
            /////////////////////////////
            Set<String> usedVirtualFieldNames = new HashSet<>();
            for(Map.Entry<String, QVirtualFieldMetaData> entry : CollectionUtils.nonNullMap(table.getVirtualFields()).entrySet())
            {
               String fieldName = entry.getKey();
               if(usedFieldNames.contains(fieldName))
               {
                  errors.add("Virtual field [" + fieldName + "] collides with a non-virtual field of the same name, in table " + tableName);
               }
               if(usedVirtualFieldNames.contains(fieldName))
               {
                  errors.add("Duplicated virtual field name [" + fieldName + "] in table " + tableName);
               }
               usedVirtualFieldNames.add(fieldName);

               validateTableField(qInstance, tableName, fieldName, table, entry.getValue());
            }

            //////////////////////////////////////////
            // validate field sections in the table //
            //////////////////////////////////////////
            Set<String>   fieldNamesInSections = new HashSet<>();
            QFieldSection tier1Section         = null;
            Set<String>   usedSectionNames     = new HashSet<>();
            Set<String>   usedSectionLabels    = new HashSet<>();
            if(table.getSections() != null)
            {
               for(QFieldSection section : table.getSections())
               {
                  validateTableSection(qInstance, table, section, fieldNamesInSections, false);
                  if(assertCondition(section.getTier() != null, "Table " + tableName + " " + section.getName() + " is missing its tier"))
                  {
                     if(section.getTier().equals(Tier.T1))
                     {
                        assertCondition(tier1Section == null, "Table " + tableName + " has more than 1 section listed as Tier 1");
                        tier1Section = section;
                     }
                  }

                  assertCondition(!usedSectionNames.contains(section.getName()), "Table " + tableName + " has more than 1 section named " + section.getName());
                  usedSectionNames.add(section.getName());

                  assertCondition(!usedSectionLabels.contains(section.getLabel()), "Table " + tableName + " has more than 1 section labeled " + section.getLabel());
                  usedSectionLabels.add(section.getLabel());
               }
            }

            for(String fieldName : CollectionUtils.nonNullMap(table.getFields()).keySet())
            {
               if(table.getField(fieldName).getIsHidden())
               {
                  assertCondition(!fieldNamesInSections.contains(fieldName), "Table " + tableName + " field " + fieldName + " is listed in a field section, but it is marked as hidden.");
               }
               else
               {
                  assertCondition(fieldNamesInSections.contains(fieldName), "Table " + tableName + " field " + fieldName + " is not listed in any field sections.");
               }
            }

            if(table.getRecordLabelFields() != null && table.getFields() != null)
            {
               for(String recordLabelField : table.getRecordLabelFields())
               {
                  assertCondition(table.getFields().containsKey(recordLabelField), "Table " + tableName + " record label field " + recordLabelField + " is not a field on this table.");
               }
            }

            for(Map.Entry<String, QCodeReference> entry : CollectionUtils.nonNullMap(table.getCustomizers()).entrySet())
            {
               validateTableCustomizer(tableName, entry.getKey(), entry.getValue());
            }

            if(table.getBackendDetails() != null)
            {
               table.getBackendDetails().validate(qInstance, table, this);
            }

            validateTableAutomationDetails(qInstance, table);
            validateTableUniqueKeys(table);
            validateAssociatedScripts(table);
            validateTableCacheOf(qInstance, table);
            validateTableRecordSecurityLocks(qInstance, table);
            validateTableAssociations(qInstance, table);
            validateExposedJoins(qInstance, joinGraph, table);

            for(QSupplementalTableMetaData supplementalTableMetaData : CollectionUtils.nonNullMap(table.getSupplementalMetaData()).values())
            {
               supplementalTableMetaData.validate(qInstance, table, this);
            }

            if(table.getShareableTableMetaData() != null)
            {
               table.getShareableTableMetaData().validate(qInstance, table, this);
            }

            runPlugins(QTableMetaData.class, table, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateExposedJoins(QInstance qInstance, JoinGraph joinGraph, QTableMetaData table)
   {
      Set<JoinGraph.JoinConnectionList> joinConnectionsForTable = null;
      Set<String>                       usedLabels              = new HashSet<>();
      Set<List<String>>                 usedJoinPaths           = new HashSet<>();

      String tablePrefix = "Table " + table.getName() + " ";
      for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
      {
         String joinPrefix = tablePrefix + "exposedJoin [missingJoinTableName] ";
         if(assertCondition(StringUtils.hasContent(exposedJoin.getJoinTable()), tablePrefix + "has an exposedJoin that is missing a joinTable name."))
         {
            joinPrefix = tablePrefix + "exposedJoin " + exposedJoin.getJoinTable() + " ";
            if(assertCondition(qInstance.getTable(exposedJoin.getJoinTable()) != null, joinPrefix + "is referencing an unrecognized table"))
            {
               if(assertCondition(CollectionUtils.nullSafeHasContents(exposedJoin.getJoinPath()), joinPrefix + "is missing a joinPath."))
               {
                  if(joinGraph != null)
                  {
                     joinConnectionsForTable = Objects.requireNonNullElseGet(joinConnectionsForTable, () -> joinGraph.getJoinConnections(table.getName()));

                     boolean foundJoinConnection = false;
                     for(JoinGraph.JoinConnectionList joinConnectionList : joinConnectionsForTable)
                     {
                        if(joinConnectionList.matchesJoinPath(exposedJoin.getJoinPath()))
                        {
                           foundJoinConnection = true;
                        }
                     }
                     assertCondition(foundJoinConnection, joinPrefix + "specified a joinPath [" + exposedJoin.getJoinPath() + "] which does not match a valid join connection in the instance.");
                  }

                  assertCondition(!usedJoinPaths.contains(exposedJoin.getJoinPath()), tablePrefix + "has more than one join with the joinPath: " + exposedJoin.getJoinPath());
                  usedJoinPaths.add(exposedJoin.getJoinPath());
               }
            }
         }

         if(assertCondition(StringUtils.hasContent(exposedJoin.getLabel()), joinPrefix + "is missing a label."))
         {
            assertCondition(!usedLabels.contains(exposedJoin.getLabel()), tablePrefix + "has more than one join labeled: " + exposedJoin.getLabel());
            usedLabels.add(exposedJoin.getLabel());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableAssociations(QInstance qInstance, QTableMetaData table)
   {
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         if(assertCondition(StringUtils.hasContent(association.getName()), "missing a name for an Association on table " + table.getName()))
         {
            String  messageSuffix   = " for Association " + association.getName() + " on table " + table.getName();
            boolean recognizedTable = false;
            if(assertCondition(StringUtils.hasContent(association.getAssociatedTableName()), "missing associatedTableName" + messageSuffix))
            {
               if(assertCondition(qInstance.getTable(association.getAssociatedTableName()) != null, "unrecognized associatedTableName " + association.getAssociatedTableName() + messageSuffix))
               {
                  recognizedTable = true;
               }
            }

            if(assertCondition(StringUtils.hasContent(association.getJoinName()), "missing joinName" + messageSuffix))
            {
               QJoinMetaData join = qInstance.getJoin(association.getJoinName());
               if(assertCondition(join != null, "unrecognized joinName " + association.getJoinName() + messageSuffix))
               {
                  assert join != null; // covered by the assertCondition

                  if(recognizedTable)
                  {
                     boolean isLeftToRight = join.getLeftTable().equals(table.getName()) && join.getRightTable().equals(association.getAssociatedTableName());
                     boolean isRightToLeft = join.getRightTable().equals(table.getName()) && join.getLeftTable().equals(association.getAssociatedTableName());
                     assertCondition(isLeftToRight || isRightToLeft, "join [" + association.getJoinName() + "] does not connect tables [" + table.getName() + "] and [" + association.getAssociatedTableName() + "]" + messageSuffix);
                     if(isLeftToRight)
                     {
                        assertCondition(join.getType().equals(JoinType.ONE_TO_MANY) || join.getType().equals(JoinType.ONE_TO_ONE), "Join type does not have 'one' on this table's side side (left)" + messageSuffix);
                     }
                     else if(isRightToLeft)
                     {
                        assertCondition(join.getType().equals(JoinType.MANY_TO_ONE) || join.getType().equals(JoinType.ONE_TO_ONE), "Join type does not have 'one' on this table's side (right)" + messageSuffix);
                     }
                  }
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableRecordSecurityLocks(QInstance qInstance, QTableMetaData table)
   {
      String prefix = "Table " + table.getName() + " ";

      for(RecordSecurityLock recordSecurityLock : CollectionUtils.nonNullList(table.getRecordSecurityLocks()))
      {
         if(!assertCondition(recordSecurityLock != null, prefix + "has a null recordSecurityLock (did you mean to give it a null list of locks?)"))
         {
            continue;
         }

         if(recordSecurityLock instanceof MultiRecordSecurityLock multiRecordSecurityLock)
         {
            validateMultiRecordSecurityLock(qInstance, table, multiRecordSecurityLock, prefix);
         }
         else
         {
            validateRecordSecurityLock(qInstance, table, recordSecurityLock, prefix);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateMultiRecordSecurityLock(QInstance qInstance, QTableMetaData table, MultiRecordSecurityLock multiRecordSecurityLock, String prefix)
   {
      assertCondition(multiRecordSecurityLock.getOperator() != null, prefix + "has a MultiRecordSecurityLock that is missing an operator");

      for(RecordSecurityLock lock : multiRecordSecurityLock.getLocks())
      {
         validateRecordSecurityLock(qInstance, table, lock, prefix);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateRecordSecurityLock(QInstance qInstance, QTableMetaData table, RecordSecurityLock recordSecurityLock, String prefix)
   {
      String securityKeyTypeName = recordSecurityLock.getSecurityKeyType();
      if(assertCondition(StringUtils.hasContent(securityKeyTypeName), prefix + "has a recordSecurityLock that is missing a securityKeyType"))
      {
         assertCondition(qInstance.getSecurityKeyType(securityKeyTypeName) != null, prefix + "has a recordSecurityLock with an unrecognized securityKeyType: " + securityKeyTypeName);
      }

      prefix = "Table " + table.getName() + " recordSecurityLock (of key type " + securityKeyTypeName + ") ";

      assertCondition(recordSecurityLock.getLockScope() != null, prefix + " is missing its lockScope");

      boolean hasAnyBadJoins = false;
      for(String joinName : CollectionUtils.nonNullList(recordSecurityLock.getJoinNameChain()))
      {
         if(!assertCondition(qInstance.getJoin(joinName) != null, prefix + "has an unrecognized joinName: " + joinName))
         {
            hasAnyBadJoins = true;
         }
      }

      String fieldName = recordSecurityLock.getFieldName();

      ////////////////////////////////////////////////////////////////////////////////
      // don't bother trying to validate field names if we know we have a bad join. //
      ////////////////////////////////////////////////////////////////////////////////
      if(assertCondition(StringUtils.hasContent(fieldName), prefix + "is missing a fieldName") && !hasAnyBadJoins)
      {
         if(fieldName.contains("."))
         {
            if(assertCondition(CollectionUtils.nullSafeHasContents(recordSecurityLock.getJoinNameChain()), prefix + "field name " + fieldName + " looks like a join (has a dot), but no joinNameChain was given."))
            {
               String[] split         = fieldName.split("\\.");
               String   joinTableName = split[0];
               String   joinFieldName = split[1];

               List<QueryJoin> joins = new ArrayList<>();

               ///////////////////////////////////////////////////////////////////////////////////////////////////
               // ok - so - the join name chain is going to be like this:                                       //
               // for a table:  orderLineItemExtrinsic (that's 2 away from order, where the security field is): //
               // - securityFieldName = order.clientId                                                          //
               // - joinNameChain = orderJoinOrderLineItem, orderLineItemJoinOrderLineItemExtrinsic             //
               // so - to navigate from the table to the security field, we need to reverse the joinNameChain,  //
               // and step (via tmpTable variable) back to the securityField                                    //
               ///////////////////////////////////////////////////////////////////////////////////////////////////
               ArrayList<String> joinNameChain = new ArrayList<>(CollectionUtils.nonNullList(recordSecurityLock.getJoinNameChain()));
               Collections.reverse(joinNameChain);

               QTableMetaData tmpTable = table;

               for(String joinName : joinNameChain)
               {
                  QJoinMetaData join = qInstance.getJoin(joinName);
                  if(join == null)
                  {
                     errors.add(prefix + "joinNameChain contained an unrecognized join: " + joinName);
                     return;
                  }

                  if(join.getLeftTable().equals(tmpTable.getName()))
                  {
                     joins.add(new QueryJoin(join));
                     tmpTable = qInstance.getTable(join.getRightTable());
                  }
                  else if(join.getRightTable().equals(tmpTable.getName()))
                  {
                     joins.add(new QueryJoin(join.flip()));
                     tmpTable = qInstance.getTable(join.getLeftTable());
                  }
                  else
                  {
                     errors.add(prefix + "joinNameChain could not be followed through join: " + joinName);
                     return;
                  }
               }

               assertCondition(Objects.equals(tmpTable.getName(), joinTableName), prefix + "has a joinNameChain doesn't end in the expected table [" + joinTableName + "] (was: " + tmpTable.getName() + ")");

               assertCondition(findField(qInstance, table, joins, fieldName), prefix + "has an unrecognized fieldName: " + fieldName);
            }
         }
         else
         {
            if(assertCondition(CollectionUtils.nullSafeIsEmpty(recordSecurityLock.getJoinNameChain()), prefix + "field name " + fieldName + " does not look like a join (does not have a dot), but a joinNameChain was given."))
            {
               assertNoException(() -> table.getField(fieldName), prefix + "has an unrecognized fieldName: " + fieldName);
            }
         }
      }

      assertCondition(recordSecurityLock.getNullValueBehavior() != null, prefix + "is missing a nullValueBehavior");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableField(QInstance qInstance, String tableName, String fieldName, QTableMetaData table, QFieldMetaData field)
   {
      assertCondition(Objects.equals(fieldName, field.getName()),
         "Inconsistent naming in table " + tableName + " for field " + fieldName + "/" + field.getName() + ".");

      String prefix = "Field " + fieldName + " in table " + tableName + " ";
      validateField(qInstance, prefix, Optional.of(table), field);
   }



   /***************************************************************************
    * run validation on a field - optionally within the context of a table.
    ***************************************************************************/
   public void validateField(QInstance qInstance, String prefix, Optional<QTableMetaData> table, QFieldMetaData field)
   {
      validateFieldPossibleValueSourceAttributes(qInstance, field, prefix);

      assertCondition(StringUtils.hasContent(field.getName()), prefix + "is missing a name");
      assertCondition(field.getType() != null, prefix + "is missing a type");

      ///////////////////////////////////////////////////
      // validate things we know about field behaviors //
      ///////////////////////////////////////////////////
      ValueTooLongBehavior behavior = field.getBehaviorOrDefault(qInstance, ValueTooLongBehavior.class);
      if(behavior != null && !behavior.equals(ValueTooLongBehavior.PASS_THROUGH))
      {
         assertCondition(field.getMaxLength() != null, prefix + "specifies a ValueTooLongBehavior, but not a maxLength.");
      }

      Set<Class<FieldBehavior<?>>> usedFieldBehaviorTypes = new HashSet<>();
      if(field.getBehaviors() != null)
      {
         String prefixMinusTrailingSpace = prefix.replaceFirst(" *$", "");

         for(FieldBehavior<?> fieldBehavior : field.getBehaviors())
         {
            @SuppressWarnings("unchecked")
            Class<FieldBehavior<?>> behaviorClass = (Class<FieldBehavior<?>>) fieldBehavior.getClass();

            try
            {
               List<String> behaviorErrors = fieldBehavior.validateBehaviorConfiguration(table.orElse(null), field);
               if(behaviorErrors != null)
               {
                  for(String behaviorError : behaviorErrors)
                  {
                     errors.add(prefixMinusTrailingSpace + ": " + behaviorClass.getSimpleName() + ": " + behaviorError);
                  }
               }
            }
            catch(Exception e)
            {
               LOG.warn(e);
               errors.add(prefixMinusTrailingSpace + ": " + behaviorClass.getSimpleName() + ": error validating behavior: " + e.getMessage());
            }

            if(!fieldBehavior.allowMultipleBehaviorsOfThisType())
            {
               assertCondition(!usedFieldBehaviorTypes.contains(behaviorClass), prefix + "has more than 1 fieldBehavior of type " + behaviorClass.getSimpleName() + ", which is not allowed for this type");
            }
            usedFieldBehaviorTypes.add(behaviorClass);
         }
      }

      if(field.getMaxLength() != null)
      {
         assertCondition(field.getMaxLength() > 0, prefix + "has an invalid maxLength (" + field.getMaxLength() + ") - must be greater than 0.");
         assertCondition(field.getType().isStringLike(), prefix + "has maxLength, but is not of a supported type (" + field.getType() + ") - must be a string-like type.");

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // this condition doesn't make sense/apply - because the default value-too-long behavior is pass-through, so, idk, just omit //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // assertCondition(behavior != null, prefix + "specifies a maxLength, but no ValueTooLongBehavior.");
      }

      FieldSecurityLock fieldSecurityLock = field.getFieldSecurityLock();
      if(fieldSecurityLock != null)
      {
         String securityKeyTypeName = fieldSecurityLock.getSecurityKeyType();
         if(assertCondition(StringUtils.hasContent(securityKeyTypeName), prefix + "has a fieldSecurityLock that is missing a securityKeyType"))
         {
            assertCondition(qInstance.getSecurityKeyType(securityKeyTypeName) != null, prefix + "has a fieldSecurityLock with an unrecognized securityKeyType: " + securityKeyTypeName);
         }

         assertCondition(fieldSecurityLock.getDefaultBehavior() != null, prefix + "has a fieldSecurityLock that is missing a defaultBehavior");
         assertCondition(CollectionUtils.nullSafeHasContents(fieldSecurityLock.getOverrideValues()), prefix + "has a fieldSecurityLock that is missing overrideValues");
      }

      for(FieldAdornment adornment : CollectionUtils.nonNullList(field.getAdornments()))
      {
         Map<String, Serializable> adornmentValues = CollectionUtils.nonNullMap(adornment.getValues());
         if(assertCondition(adornment.getType() != null, prefix + "has an adornment that is missing a type"))
         {
            String adornmentPrefix = prefix.trim() + ", " + adornment.getType() + " adornment ";
            switch(adornment.getType())
            {
               case SIZE ->
               {
                  String width = ValueUtils.getValueAsString(adornmentValues.get("width"));
                  if(assertCondition(StringUtils.hasContent(width), adornmentPrefix + "is missing a width value"))
                  {
                     assertNoException(() -> AdornmentType.Size.valueOf(width.toUpperCase()), adornmentPrefix + "has an unrecognized width value [" + width + "]");
                  }
               }
               case FILE_DOWNLOAD ->
               {
                  String fileNameField = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.FILE_NAME_FIELD));
                  if(StringUtils.hasContent(fileNameField)) // file name isn't required - but if given, must be a field on the table.
                  {
                     if(assertCondition(table.isPresent(), adornmentPrefix + "with fileNameField cannot be used outside of the context of a table"))
                     {
                        //noinspection OptionalGetWithoutIsPresent - is checked in the assertCondition line
                        assertNoException(() -> table.get().getField(fileNameField), adornmentPrefix + "specifies an unrecognized fileNameField [" + fileNameField + "]");
                     }
                  }

                  if(adornmentValues.containsKey(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT_FIELDS))
                  {
                     try
                     {
                        @SuppressWarnings("unchecked")
                        List<String> formatFieldNames = (List<String>) adornmentValues.get(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT_FIELDS);
                        for(String formatFieldName : CollectionUtils.nonNullList(formatFieldNames))
                        {
                           if(assertCondition(table.isPresent(), adornmentPrefix + "with formatFieldNames cannot be used outside of the context of a table"))
                           {
                              //noinspection OptionalGetWithoutIsPresent - is checked in the assertCondition line
                              assertNoException(() -> table.get().getField(formatFieldName), adornmentPrefix + "specifies an unrecognized field name in fileNameFormatFields [" + formatFieldName + "]");
                           }
                        }
                     }
                     catch(Exception e)
                     {
                        errors.add(adornmentPrefix + "fileNameFormatFields could not be accessed (is it a List<String>?)");
                     }
                  }
               }
               case WIDGET ->
               {
                  String widgetName = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.WidgetValues.WIDGET_NAME));
                  if(assertCondition(StringUtils.hasContent(widgetName), adornmentPrefix + "is missing a widget name, which is required."))
                  {
                     assertCondition(qInstance.getWidget(widgetName) != null, adornmentPrefix + "specifies an unrecognized widgetName [" + widgetName + "]");
                  }
               }
               default ->
               {
                  // no validations by default
               }
            }
         }
      }

      validateFieldSupplementalMetaData(field, qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void validateFieldSupplementalMetaData(QFieldMetaData field, QInstance qInstance)
   {
      for(QSupplementalFieldMetaData supplementalFieldMetaData : CollectionUtils.nonNullMap(field.getSupplementalMetaData()).values())
      {
         supplementalFieldMetaData.validate(qInstance, field, this);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void validateFieldPossibleValueSourceAttributes(QInstance qInstance, QFieldMetaData field, String prefix)
   {
      if(field.getPossibleValueSourceName() != null)
      {
         assertCondition(qInstance.getPossibleValueSource(field.getPossibleValueSourceName()) != null,
            prefix + "has an unrecognized possibleValueSourceName " + field.getPossibleValueSourceName());

         assertCondition(field.getInlinePossibleValueSource() == null, prefix.trim() + " has both a possibleValueSourceName and an inlinePossibleValueSource, which is not allowed.");
      }

      if(field.getInlinePossibleValueSource() != null)
      {
         String name = "inlinePossibleValueSource for " + prefix.trim();
         if(assertCondition(QPossibleValueSourceType.ENUM.equals(field.getInlinePossibleValueSource().getType()), name + " must have a type of ENUM."))
         {
            validatePossibleValueSource(qInstance, name, field.getInlinePossibleValueSource());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableCacheOf(QInstance qInstance, QTableMetaData table)
   {
      CacheOf cacheOf = table.getCacheOf();
      if(cacheOf == null)
      {
         return;
      }

      String prefix          = "Table " + table.getName() + " cacheOf ";
      String sourceTableName = cacheOf.getSourceTable();
      if(assertCondition(StringUtils.hasContent(sourceTableName), prefix + "is missing a sourceTable name"))
      {
         assertCondition(qInstance.getTable(sourceTableName) != null, prefix + "is referencing an unknown sourceTable: " + sourceTableName);

         boolean hasExpirationSeconds  = cacheOf.getExpirationSeconds() != null;
         boolean hasCacheDateFieldName = StringUtils.hasContent(cacheOf.getCachedDateFieldName());
         assertCondition(hasExpirationSeconds && hasCacheDateFieldName || (!hasExpirationSeconds && !hasCacheDateFieldName), prefix + "is missing either expirationSeconds or cachedDateFieldName (must either have both, or neither.)");

         if(hasCacheDateFieldName)
         {
            assertNoException(() -> table.getField(cacheOf.getCachedDateFieldName()), prefix + "cachedDateFieldName " + cacheOf.getCachedDateFieldName() + " is an unrecognized field.");
         }

         if(assertCondition(CollectionUtils.nullSafeHasContents(cacheOf.getUseCases()), prefix + "does not have any useCases defined."))
         {
            for(CacheUseCase useCase : cacheOf.getUseCases())
            {
               assertCondition(useCase.getType() != null, prefix + "has a useCase without a type.");
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateAssociatedScripts(QTableMetaData table)
   {
      Set<String> usedFieldNames = new HashSet<>();
      for(AssociatedScript associatedScript : CollectionUtils.nonNullList(table.getAssociatedScripts()))
      {
         if(assertCondition(StringUtils.hasContent(associatedScript.getFieldName()), "Table " + table.getName() + " has an associatedScript without a fieldName"))
         {
            assertCondition(!usedFieldNames.contains(associatedScript.getFieldName()), "Table " + table.getName() + " has more than one associatedScript specifying field: " + associatedScript.getFieldName());
            usedFieldNames.add(associatedScript.getFieldName());
            assertNoException(() -> table.getField(associatedScript.getFieldName()), "Table " + table.getName() + " has an associatedScript specifying an unrecognized field: " + associatedScript.getFieldName());
         }

         assertCondition(associatedScript.getScriptTypeId() != null, "Table " + table.getName() + " associatedScript on field " + associatedScript.getFieldName() + " is missing a scriptTypeId");
         if(associatedScript.getScriptTester() != null)
         {
            String prefix = "Table " + table.getName() + " associatedScript on field " + associatedScript.getFieldName();
            if(preAssertionsForCodeReference(associatedScript.getScriptTester(), prefix))
            {
               validateSimpleCodeReference(prefix, associatedScript.getScriptTester(), TestScriptActionInterface.class);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableUniqueKeys(QTableMetaData table)
   {
      Set<Set<String>> ukSets = new HashSet<>();
      for(UniqueKey uniqueKey : CollectionUtils.nonNullList(table.getUniqueKeys()))
      {
         if(assertCondition(CollectionUtils.nullSafeHasContents(uniqueKey.getFieldNames()), table.getName() + " has a uniqueKey with no fields"))
         {
            Set<String> fieldNamesInThisUK = new HashSet<>();
            for(String fieldName : uniqueKey.getFieldNames())
            {
               assertNoException(() -> table.getField(fieldName), table.getName() + " has a uniqueKey with an unrecognized field name: " + fieldName);
               assertCondition(!fieldNamesInThisUK.contains(fieldName), table.getName() + " has a uniqueKey with the same field multiple times: " + fieldName);
               fieldNamesInThisUK.add(fieldName);
            }

            assertCondition(!ukSets.contains(fieldNamesInThisUK), table.getName() + " has more than one uniqueKey with the same set of fields: " + fieldNamesInThisUK);
            ukSets.add(fieldNamesInThisUK);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableAutomationDetails(QInstance qInstance, QTableMetaData table)
   {
      QTableAutomationDetails automationDetails = table.getAutomationDetails();
      if(automationDetails == null)
      {
         return;
      }

      String tableName = table.getName();
      String prefix    = "Table " + tableName + " automationDetails ";

      //////////////////////////////////////
      // validate the automation provider //
      //////////////////////////////////////
      String providerName = automationDetails.getProviderName();
      if(assertCondition(StringUtils.hasContent(providerName), prefix + " is missing a providerName"))
      {
         assertCondition(qInstance.getAutomationProvider(providerName) != null, " has an unrecognized providerName: " + providerName);
      }

      if(automationDetails.getSchedule() != null)
      {
         validateScheduleMetaData(automationDetails.getSchedule(), qInstance, prefix + " automationDetails, schedule: ");
      }

      //////////////////////////////////
      // validate the status tracking //
      //////////////////////////////////
      AutomationStatusTracking statusTracking = automationDetails.getStatusTracking();
      if(assertCondition(statusTracking != null, prefix + "do not have statusTracking defined."))
      {
         if(assertCondition(statusTracking.getType() != null, prefix + "statusTracking is missing a type"))
         {
            if(statusTracking.getType().equals(AutomationStatusTrackingType.FIELD_IN_TABLE))
            {
               if(assertCondition(StringUtils.hasContent(statusTracking.getFieldName()), prefix + "statusTracking of type fieldInTable is missing its fieldName"))
               {
                  assertNoException(() -> table.getField(statusTracking.getFieldName()), prefix + "statusTracking field is not a defined field on this table.");
               }
            }
         }
      }

      //////////////////////////
      // validate the actions //
      //////////////////////////
      Set<String> usedNames = new HashSet<>();
      if(automationDetails.getActions() != null)
      {
         automationDetails.getActions().forEach(action ->
         {
            assertCondition(StringUtils.hasContent(action.getName()), prefix + "has an action missing a name");
            assertCondition(!usedNames.contains(action.getName()), prefix + "has more than one action named " + action.getName());
            usedNames.add(action.getName());

            String actionPrefix = prefix + "action " + action.getName() + " ";
            assertCondition(action.getTriggerEvent() != null, actionPrefix + "is missing a triggerEvent");

            /////////////////////////////////////////////////////
            // validate the code or process used by the action //
            /////////////////////////////////////////////////////
            int numberSet = 0;
            if(action.getCodeReference() != null)
            {
               numberSet++;
               if(preAssertionsForCodeReference(action.getCodeReference(), actionPrefix))
               {
                  validateSimpleCodeReference(actionPrefix + "code reference: ", action.getCodeReference(), RecordAutomationHandlerInterface.class);
               }
            }

            if(action.getProcessName() != null)
            {
               numberSet++;
               QProcessMetaData process = qInstance.getProcess(action.getProcessName());
               if(assertCondition(process != null, actionPrefix + "has an unrecognized processName: " + action.getProcessName()))
               {
                  if(process.getTableName() != null)
                  {
                     assertCondition(tableName.equals(process.getTableName()), actionPrefix + " references a process from a different table");
                  }
               }
            }

            assertCondition(numberSet != 0, actionPrefix + "is missing both a codeReference and a processName");
            assertCondition(!(numberSet > 1), actionPrefix + "has both a codeReference and a processName (which is not allowed)");

            ///////////////////////////////////////////
            // validate the filter (if there is one) //
            ///////////////////////////////////////////
            if(action.getFilter() != null && action.getFilter().getCriteria() != null)
            {
               action.getFilter().getCriteria().forEach((criteria) ->
               {
                  if(assertCondition(StringUtils.hasContent(criteria.getFieldName()), actionPrefix + "has a filter criteria without a field name"))
                  {
                     assertNoException(() -> table.getField(criteria.getFieldName()), actionPrefix + "has a filter criteria referencing an unrecognized field: " + criteria.getFieldName());
                  }

                  assertCondition(criteria.getOperator() != null, actionPrefix + "has a filter criteria without an operator");

                  // todo - validate cardinality of values...
               });
            }
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableCustomizer(String tableName, String roleName, QCodeReference codeReference)
   {
      String prefix = "Table " + tableName + ", customizer " + roleName + ": ";

      if(!preAssertionsForCodeReference(codeReference, prefix))
      {
         return;
      }

      //////////////////////////////////////////////////////////////////////////////
      // make sure (at this time) that it's a java type, then do some java checks //
      //////////////////////////////////////////////////////////////////////////////
      if(assertCondition(codeReference.getCodeType().equals(QCodeType.JAVA), prefix + "Only JAVA customizers are supported at this time."))
      {
         ///////////////////////////////////////
         // make sure the class can be loaded //
         ///////////////////////////////////////
         Class<?> customizerClass = getClassForCodeReference(codeReference, prefix);
         if(customizerClass != null)
         {
            //////////////////////////////////////////////////
            // make sure the customizer can be instantiated //
            //////////////////////////////////////////////////
            Object customizerInstance = getInstanceOfCodeReference(prefix, customizerClass, codeReference);

            TableCustomizers tableCustomizer = TableCustomizers.forRole(roleName);
            if(tableCustomizer == null)
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////////
               // todo - in the future, load customizers from backend-modules (e.g., FilesystemTableCustomizers) //
               ////////////////////////////////////////////////////////////////////////////////////////////////////
               warn(prefix + "Unrecognized table customizer name (at least at backend-core level)");
            }
            else
            {
               ////////////////////////////////////////////////////////////////////////
               // make sure the customizer instance can be cast to the expected type //
               ////////////////////////////////////////////////////////////////////////
               if(customizerInstance != null && tableCustomizer.getExpectedType() != null)
               {
                  assertObjectCanBeCasted(prefix, customizerInstance, tableCustomizer.getExpectedType());
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** Make sure that a given object can be casted to an expected type.
    *******************************************************************************/
   private void assertObjectCanBeCasted(String errorPrefix, Object object, Class<?>... anyOfExpectedClasses)
   {
      for(Class<?> expectedClass : anyOfExpectedClasses)
      {
         try
         {
            expectedClass.cast(object);
            return;
         }
         catch(ClassCastException e)
         {
            /////////////////////////////////////
            // try next type (if there is one) //
            /////////////////////////////////////
         }
      }

      if(anyOfExpectedClasses.length == 1)
      {
         errors.add(errorPrefix + "CodeReference is not of the expected type: " + anyOfExpectedClasses[0]);
      }
      else
      {
         errors.add(errorPrefix + "CodeReference is not any of the expected types: " + Arrays.stream(anyOfExpectedClasses).map(c -> c.getName()).collect(Collectors.joining(", ")));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Object getInstanceOfCodeReference(String prefix, Class<?> clazz, QCodeReference codeReference)
   {
      if(codeReference instanceof QCodeReferenceLambda<?> lambdaCodeReference)
      {
         return (lambdaCodeReference.getLambda());
      }

      Object instance = null;
      try
      {
         instance = clazz.getConstructor().newInstance();
      }
      catch(InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e)
      {
         prefix += "Instance of " + clazz.getSimpleName() + " could not be created";
         if(Modifier.isAbstract(clazz.getModifiers()))
         {
            errors.add(prefix + " because it is abstract");
         }
         else if(Modifier.isInterface(clazz.getModifiers()))
         {
            errors.add(prefix + " because it is an interface");
         }
         else if(!Modifier.isPublic(clazz.getModifiers()))
         {
            //////////////////////////////////////////////////////////////
            // seems like this doesn't get hit, for private classses... //
            //////////////////////////////////////////////////////////////
            errors.add(prefix + " because it is not public");
         }
         else
         {
            //////////////////////////////////
            // check for no-arg constructor //
            //////////////////////////////////
            boolean hasNoArgConstructor = Stream.of(clazz.getConstructors()).anyMatch(c -> c.getParameterCount() == 0);
            if(!hasNoArgConstructor)
            {
               errors.add(prefix + " because it does not have a public parameterless constructor");
            }
            else
            {
               //////////////////////////////////////////
               // otherwise, just append the exception //
               //////////////////////////////////////////
               e.printStackTrace();
               errors.add(prefix + ": " + e.getMessage());
            }
         }
      }
      return instance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateTableSection(QInstance qInstance, QTableMetaData table, QFieldSection section, Set<String> fieldNamesInSections, boolean isAlternativeSection)
   {
      assertCondition(StringUtils.hasContent(section.getName()), "Missing a name for field section in table " + table.getName() + ".");
      assertCondition(StringUtils.hasContent(section.getLabel()), "Missing a label for field section in table " + table.getLabel() + ".");

      boolean hasFields = CollectionUtils.nullSafeHasContents(section.getFieldNames());
      boolean hasWidget = StringUtils.hasContent(section.getWidgetName());

      String sectionPrefix = "Table " + table.getName() + " section " + section.getName() + " ";
      if(assertCondition(hasFields || hasWidget, sectionPrefix + "does not have any fields or a widget."))
      {
         if(table.getFields() != null && hasFields)
         {
            for(String fieldName : section.getFieldNames())
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // note - this was originally written as an assertion:                                                                                                                                       //
               // if(assertCondition(qInstance.getTable(otherTableName) != null, sectionPrefix + "join-field " + fieldName + ", which is referencing an unrecognized table name [" + otherTableName + "]")) //
               // but... then a field name with dots gives us a bad time here, so...                                                                                                                        //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(fieldName.contains(".") && qInstance.getTable(fieldName.split("\\.")[0]) != null)
               {
                  String[] parts            = fieldName.split("\\.");
                  String   otherTableName   = parts[0];
                  String   foreignFieldName = parts[1];

                  if(assertCondition(qInstance.getTable(otherTableName) != null, sectionPrefix + "join-field " + fieldName + ", which is referencing an unrecognized table name [" + otherTableName + "]"))
                  {
                     List<ExposedJoin> matchedExposedJoins = CollectionUtils.nonNullList(table.getExposedJoins()).stream().filter(ej -> otherTableName.equals(ej.getJoinTable())).toList();
                     if(assertCondition(CollectionUtils.nullSafeHasContents(matchedExposedJoins), sectionPrefix + "join-field " + fieldName + ", referencing table [" + otherTableName + "] which is not an exposed join on this table."))
                     {
                        assertCondition(!matchedExposedJoins.get(0).getIsMany(qInstance), sectionPrefix + "join-field " + fieldName + " references an is-many join, which is not supported.");
                     }
                     assertCondition(qInstance.getTable(otherTableName).getFields().containsKey(foreignFieldName), sectionPrefix + "join-field " + fieldName + " specifies a fieldName [" + foreignFieldName + "] which does not exist in that table [" + otherTableName + "].");
                  }
               }
               else
               {
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // for alternative sections, allow virtual fields.                                                     //
                  // this is our first pass at rules for virtual fields... eventually they may be supported more broadly //
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////
                  boolean isField = table.getFields().containsKey(fieldName);
                  boolean isVirtualField = CollectionUtils.nonNullMap(table.getVirtualFields()).containsKey(fieldName);
                  if(isAlternativeSection)
                  {
                     assertCondition(isField || isVirtualField, sectionPrefix + "specifies fieldName " + fieldName + ", which is not a field (or virtual field) on this table.");
                  }
                  else
                  {
                     ////////////////////////////////////////////////////////////////////////////////
                     // else, "main" sections, they only (at this time) support non-virtual fields //
                     ////////////////////////////////////////////////////////////////////////////////
                     if(!isField)
                     {
                        if(isVirtualField)
                        {
                           errors.add(sectionPrefix + "specifies fieldName " + fieldName + ", which is a virtual field, which is not allowed in a base section (only alternative sections)");
                        }
                        else
                        {
                           errors.add(sectionPrefix + "specifies fieldName " + fieldName + ", which is not a field on this table.");
                        }
                     }
                  }
               }

               if(!isAlternativeSection)
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // don't consider field names in alternative sections as part of the set of field names in some-section //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////
                  assertCondition(!fieldNamesInSections.contains(fieldName), "Table " + table.getName() + " has field " + fieldName + " listed more than once in its field sections.");
                  fieldNamesInSections.add(fieldName);
               }
            }
         }
         else if(hasWidget)
         {
            assertCondition(qInstance.getWidget(section.getWidgetName()) != null, sectionPrefix + "specifies widget " + section.getWidgetName() + ", which is not a widget in this instance.");
         }
      }

      if(isAlternativeSection)
      {
         assertCondition(CollectionUtils.nullSafeIsEmpty(section.getAlternatives()), sectionPrefix + "is an alternative section, which itself has alternative sections, which is not allowed");
      }
      else
      {
         ////////////////////////////////////////////////////////////////
         // recursively process alternative sections for main sections //
         ////////////////////////////////////////////////////////////////
         for(QFieldSection alternativeSection : CollectionUtils.nonNullMap(section.getAlternatives()).values())
         {
            validateTableSection(qInstance, table, alternativeSection, fieldNamesInSections, true);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateAppSection(QAppMetaData app, QAppSection section, Set<String> childNamesInSections)
   {
      assertCondition(StringUtils.hasContent(section.getName()), "Missing a name for a section in app " + app.getName() + ".");
      assertCondition(StringUtils.hasContent(section.getLabel()), "Missing a label for a section in app " + app.getLabel() + ".");
      boolean hasTables    = CollectionUtils.nullSafeHasContents(section.getTables());
      boolean hasProcesses = CollectionUtils.nullSafeHasContents(section.getProcesses());
      boolean hasReports   = CollectionUtils.nullSafeHasContents(section.getReports());
      if(assertCondition(hasTables || hasProcesses || hasReports, "App " + app.getName() + " section " + section.getName() + " does not have any children."))
      {
         if(hasTables)
         {
            for(String tableName : section.getTables())
            {
               assertCondition(app.getChildren().stream().anyMatch(c -> c.getName().equals(tableName)), "App " + app.getName() + " section " + section.getName() + " specifies table " + tableName + ", which is not a child of this app.");
               assertCondition(!childNamesInSections.contains(tableName), "App " + app.getName() + " has table " + tableName + " listed more than once in its sections.");

               childNamesInSections.add(tableName);
            }
         }
         if(hasProcesses)
         {
            for(String processName : section.getProcesses())
            {
               assertCondition(app.getChildren().stream().anyMatch(c -> c.getName().equals(processName)), "App " + app.getName() + " section " + section.getName() + " specifies process " + processName + ", which is not a child of this app.");
               assertCondition(!childNamesInSections.contains(processName), "App " + app.getName() + " has process " + processName + " listed more than once in its sections.");

               childNamesInSections.add(processName);
            }
         }
         if(hasReports)
         {
            for(String reportName : section.getReports())
            {
               assertCondition(app.getChildren().stream().anyMatch(c -> c.getName().equals(reportName)), "App " + app.getName() + " section " + section.getName() + " specifies report " + reportName + ", which is not a child of this app.");
               assertCondition(!childNamesInSections.contains(reportName), "App " + app.getName() + " has report " + reportName + " listed more than once in its sections.");

               childNamesInSections.add(reportName);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateProcesses(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getProcesses()))
      {
         qInstance.getProcesses().forEach((processName, process) ->
         {
            assertCondition(Objects.equals(processName, process.getName()), "Inconsistent naming for process: " + processName + "/" + process.getName() + ".");

            /////////////////////////////////////////////
            // validate the table name for the process //
            /////////////////////////////////////////////
            if(process.getTableName() != null)
            {
               assertCondition(qInstance.getTable(process.getTableName()) != null, "Unrecognized table " + process.getTableName() + " for process " + processName + ".");
            }

            ///////////////////////////////////
            // validate steps in the process //
            ///////////////////////////////////
            Set<String> usedStepNames = new HashSet<>();
            if(assertCondition(CollectionUtils.nullSafeHasContents(process.getStepList()), "At least 1 step must be defined in process " + processName + "."))
            {
               int index = -1;
               for(QStepMetaData step : process.getStepList())
               {
                  index++;
                  if(assertCondition(StringUtils.hasContent(step.getName()), "Missing name for a step at index " + index + " in process " + processName))
                  {
                     assertCondition(!usedStepNames.contains(step.getName()), "Duplicate step name [" + step.getName() + "] in process " + processName);
                     usedStepNames.add(step.getName());
                  }

                  ////////////////////////////////////////////
                  // validate instantiation of step classes //
                  ////////////////////////////////////////////
                  if(step instanceof QBackendStepMetaData backendStepMetaData)
                  {
                     if(assertCondition(backendStepMetaData.getCode() != null, "Missing code for a backend step at index " + index + " in process " + processName))
                     {
                        validateSimpleCodeReference("Process " + processName + ", backend step at index " + index + ", code reference: ", backendStepMetaData.getCode(), BackendStep.class);
                     }

                     if(backendStepMetaData.getInputMetaData() != null && CollectionUtils.nullSafeHasContents(backendStepMetaData.getInputMetaData().getFieldList()))
                     {
                        for(QFieldMetaData fieldMetaData : backendStepMetaData.getInputMetaData().getFieldList())
                        {
                           if(fieldMetaData.getDefaultValue() != null && fieldMetaData.getDefaultValue() instanceof QCodeReference codeReference)
                           {
                              ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                              // by default, assume that any process field which is a QCodeReference should be a reference to a BackendStep... //
                              // but... allow a secondary field name to be set, to tell us what class to *actually* expect here...             //
                              ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                              Class<?> expectedClass = BackendStep.class;
                              try
                              {
                                 Optional<QFieldMetaData> expectedTypeField = backendStepMetaData.getInputMetaData().getField(fieldMetaData.getName() + "_expectedType");
                                 if(expectedTypeField.isPresent() && expectedTypeField.get().getDefaultValue() != null)
                                 {
                                    expectedClass = Class.forName(ValueUtils.getValueAsString(expectedTypeField.get().getDefaultValue()));
                                 }
                              }
                              catch(Exception e)
                              {
                                 warn("Error loading expectedType for field [" + fieldMetaData.getName() + "] in process [" + processName + "]: " + e.getMessage());
                              }

                              validateSimpleCodeReference("Process " + processName + " code reference:", codeReference, expectedClass);
                           }

                           validateFieldSupplementalMetaData(fieldMetaData, qInstance);
                        }
                     }
                  }
               }
            }

            for(QFieldMetaData field : process.getInputFields())
            {
               validateFieldPossibleValueSourceAttributes(qInstance, field, "Process " + processName + ", input field " + field.getName() + " ");
            }

            for(QFieldMetaData field : process.getOutputFields())
            {
               validateFieldPossibleValueSourceAttributes(qInstance, field, "Process " + processName + ", output field " + field.getName() + " ");
            }

            if(process.getCancelStep() != null)
            {
               if(assertCondition(process.getCancelStep().getCode() != null, "Cancel step is missing a code reference, in process " + processName))
               {
                  validateSimpleCodeReference("Process " + processName + " cancel step code reference: ", process.getCancelStep().getCode(), BackendStep.class);
               }
            }

            ///////////////////////////////////////////////////////////////////////////////
            // if the process has a schedule, make sure required schedule data populated //
            ///////////////////////////////////////////////////////////////////////////////
            if(process.getSchedule() != null)
            {
               QScheduleMetaData schedule = process.getSchedule();
               validateScheduleMetaData(schedule, qInstance, "Process " + processName + ", schedule: ");
            }

            if(process.getVariantBackend() != null)
            {
               if(qInstance.getBackends() != null)
               {
                  assertCondition(qInstance.getBackend(process.getVariantBackend()) != null, "Process " + processName + ", a variant backend was not found named " + process.getVariantBackend());
               }

               assertCondition(process.getVariantRunStrategy() != null, "A variant run strategy was not set for process " + processName + " (which does specify a variant backend)");
            }
            else
            {
               assertCondition(process.getVariantRunStrategy() == null, "A variant run strategy was set for process " + processName + " (which isn't allowed, since it does not specify a variant backend)");
            }

            for(QSupplementalProcessMetaData supplementalProcessMetaData : CollectionUtils.nonNullMap(process.getSupplementalMetaData()).values())
            {
               supplementalProcessMetaData.validate(qInstance, process, this);
            }

            runPlugins(QProcessMetaData.class, process, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateScheduleMetaData(QScheduleMetaData schedule, QInstance qInstance, String prefix)
   {
      boolean isRepeat = schedule.getRepeatMillis() != null || schedule.getRepeatSeconds() != null;
      boolean isCron   = StringUtils.hasContent(schedule.getCronExpression());
      assertCondition(isRepeat || isCron, prefix + " either repeatMillis or repeatSeconds or cronExpression must be set");
      assertCondition(!(isRepeat && isCron), prefix + " both a repeat time and cronExpression may not be set");

      if(isCron)
      {
         boolean hasDelay = schedule.getInitialDelayMillis() != null || schedule.getInitialDelaySeconds() != null;
         assertCondition(!hasDelay, prefix + " a cron schedule may not have an initial delay");

         try
         {
            CronExpression.validateExpression(schedule.getCronExpression());
         }
         catch(ParseException pe)
         {
            errors.add(prefix + " invalid cron expression: " + pe.getMessage());
         }

         if(assertCondition(StringUtils.hasContent(schedule.getCronTimeZoneId()), prefix + " a cron schedule must specify a cronTimeZoneId"))
         {
            String[]         availableIDs = TimeZone.getAvailableIDs();
            Optional<String> first        = Arrays.stream(availableIDs).filter(id -> id.equals(schedule.getCronTimeZoneId())).findFirst();
            assertCondition(first.isPresent(), prefix + " unrecognized cronTimeZoneId: " + schedule.getCronTimeZoneId());
         }
      }
      else
      {
         assertCondition(!StringUtils.hasContent(schedule.getCronTimeZoneId()), prefix + " a non-cron schedule must not specify a cronTimeZoneId");
      }

      if(assertCondition(StringUtils.hasContent(schedule.getSchedulerName()), prefix + " is missing a scheduler name"))
      {
         assertCondition(qInstance.getScheduler(schedule.getSchedulerName()) != null, prefix + " is referencing an unknown scheduler name: " + schedule.getSchedulerName());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateReports(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getReports()))
      {
         qInstance.getReports().forEach((reportName, report) ->
         {
            assertCondition(Objects.equals(reportName, report.getName()), "Inconsistent naming for report: " + reportName + "/" + report.getName() + ".");

            ////////////////////////////////////////
            // validate dataSources in the report //
            ////////////////////////////////////////
            Set<String> usedDataSourceNames = new HashSet<>();
            if(assertCondition(CollectionUtils.nullSafeHasContents(report.getDataSources()), "At least 1 data source must be defined in report " + reportName + "."))
            {
               int index = 0;
               for(QReportDataSource dataSource : report.getDataSources())
               {
                  assertCondition(StringUtils.hasContent(dataSource.getName()), "Missing name for a dataSource at index " + index + " in report " + reportName);
                  index++;

                  assertCondition(!usedDataSourceNames.contains(dataSource.getName()), "More than one dataSource with name " + dataSource.getName() + " in report " + reportName);
                  usedDataSourceNames.add(dataSource.getName());

                  String dataSourceErrorPrefix = "Report " + reportName + " data source " + dataSource.getName() + " ";

                  boolean hasASource = false;

                  if(StringUtils.hasContent(dataSource.getSourceTable()))
                  {
                     hasASource = true;
                     assertCondition(dataSource.getStaticDataSupplier() == null, dataSourceErrorPrefix + "has both a sourceTable and a staticDataSupplier (not compatible together).");
                     if(assertCondition(qInstance.getTable(dataSource.getSourceTable()) != null, dataSourceErrorPrefix + "source table " + dataSource.getSourceTable() + " is not a table in this instance."))
                     {
                        if(dataSource.getQueryFilter() != null)
                        {
                           validateQueryFilter(qInstance, "In " + dataSourceErrorPrefix + "query filter - ", qInstance.getTable(dataSource.getSourceTable()), dataSource.getQueryFilter(), dataSource.getQueryJoins());
                        }
                     }
                  }

                  if(dataSource.getStaticDataSupplier() != null)
                  {
                     assertCondition(dataSource.getCustomRecordSource() == null, dataSourceErrorPrefix + "has both a staticDataSupplier and a customRecordSource (not compatible together).");
                     hasASource = true;
                     validateSimpleCodeReference(dataSourceErrorPrefix, dataSource.getStaticDataSupplier(), Supplier.class);
                  }

                  if(dataSource.getCustomRecordSource() != null)
                  {
                     hasASource = true;
                     validateSimpleCodeReference(dataSourceErrorPrefix, dataSource.getCustomRecordSource(), ReportCustomRecordSourceInterface.class);
                  }

                  assertCondition(hasASource, dataSourceErrorPrefix + "does not have a sourceTable, customRecordSource, or a staticDataSupplier.");
               }
            }

            //////////////////////////////////
            // validate views in the report //
            //////////////////////////////////
            if(assertCondition(CollectionUtils.nullSafeHasContents(report.getViews()), "At least 1 view must be defined in report " + reportName + "."))
            {
               int         index         = 0;
               Set<String> usedViewNames = new HashSet<>();
               for(QReportView view : report.getViews())
               {
                  assertCondition(StringUtils.hasContent(view.getName()), "Missing name for a view at index " + index + " in report " + reportName);
                  index++;

                  assertCondition(!usedViewNames.contains(view.getName()), "More than one view with name " + view.getName() + " in report " + reportName);
                  usedViewNames.add(view.getName());

                  String viewErrorPrefix = "Report " + reportName + " view " + view.getName() + " ";
                  assertCondition(view.getType() != null, viewErrorPrefix + "is missing its type.");
                  if(assertCondition(StringUtils.hasContent(view.getDataSourceName()), viewErrorPrefix + "is missing a dataSourceName"))
                  {
                     assertCondition(usedDataSourceNames.contains(view.getDataSourceName()), viewErrorPrefix + "has an unrecognized dataSourceName: " + view.getDataSourceName());
                  }

                  if(StringUtils.hasContent(view.getVarianceDataSourceName()))
                  {
                     assertCondition(usedDataSourceNames.contains(view.getVarianceDataSourceName()), viewErrorPrefix + "has an unrecognized varianceDataSourceName: " + view.getVarianceDataSourceName());
                  }

                  boolean hasColumns        = CollectionUtils.nullSafeHasContents(view.getColumns());
                  boolean hasViewCustomizer = view.getViewCustomizer() != null;
                  assertCondition(hasColumns || hasViewCustomizer, viewErrorPrefix + "does not have any columns or a view customizer.");

                  Set<String> usedColumnNames = new HashSet<>();
                  for(QReportField column : CollectionUtils.nonNullList(view.getColumns()))
                  {
                     assertCondition(StringUtils.hasContent(column.getName()), viewErrorPrefix + "has a column with no name.");
                     assertCondition(!usedColumnNames.contains(column.getName()), viewErrorPrefix + "has multiple columns named: " + column.getName());
                     usedColumnNames.add(column.getName());

                     // todo - is field name valid?
                  }

                  // todo - all these too...
                  // view.getPivotFields();
                  // view.getViewCustomizer(); // validate code ref
                  // view.getRecordTransformStep(); // validate code ref
                  // view.getOrderByFields(); // make sure valid field names?
                  // view.getIncludePivotSubTotals(); // only for pivot type
                  // view.getTitleFormat(); view.getTitleFields(); // validate these match?
               }
            }

            runPlugins(QReportMetaData.class, report, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validateQueryFilter(QInstance qInstance, String context, QTableMetaData table, QQueryFilter queryFilter, List<QueryJoin> queryJoins)
   {
      for(QFilterCriteria criterion : CollectionUtils.nonNullList(queryFilter.getCriteria()))
      {
         String fieldName = criterion.getFieldName();
         if(assertCondition(StringUtils.hasContent(fieldName), context + "Missing fieldName for a criteria"))
         {
            assertCondition(findField(qInstance, table, queryJoins, fieldName), context + "Criteria fieldName " + fieldName + " is not a field in this table (or in any given joins).");
         }
         assertCondition(criterion.getOperator() != null, context + "Missing operator for a criteria on fieldName " + fieldName);
         assertCondition(criterion.getValues() != null, context + "Missing values for a criteria on fieldName " + fieldName); // todo - what about ops w/ no value (BLANK)
      }

      for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(queryFilter.getOrderBys()))
      {
         if(assertCondition(StringUtils.hasContent(orderBy.getFieldName()), context + "Missing fieldName for an orderBy"))
         {
            assertCondition(findField(qInstance, table, queryJoins, orderBy.getFieldName()), context + "OrderBy fieldName " + orderBy.getFieldName() + " is not a field in this table (or in any given joins).");
         }
      }

      for(QQueryFilter subFilter : CollectionUtils.nonNullList(queryFilter.getSubFilters()))
      {
         validateQueryFilter(qInstance, context, table, subFilter, queryJoins);
      }
   }



   /*******************************************************************************
    ** Look for a field name in either a table, or the tables referenced in a list of query joins.
    *******************************************************************************/
   private boolean findField(QInstance qInstance, QTableMetaData table, List<QueryJoin> queryJoins, String fieldName)
   {
      boolean foundField = false;
      try
      {
         table.getField(fieldName);
         foundField = true;
      }
      catch(Exception e)
      {
         if(fieldName.contains("."))
         {
            String fieldNameAfterDot  = fieldName.substring(fieldName.lastIndexOf(".") + 1);
            String tableNameBeforeDot = fieldName.substring(0, fieldName.lastIndexOf("."));

            if(CollectionUtils.nullSafeHasContents(queryJoins))
            {
               for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
               {
                  QTableMetaData joinTable = qInstance.getTable(queryJoin.getJoinTable());
                  if(joinTable != null)
                  {
                     try
                     {
                        joinTable.getField(fieldNameAfterDot);
                        foundField = true;
                        break;
                     }
                     catch(Exception e2)
                     {
                        continue;
                     }
                  }
               }
            }
            else
            {
               if(this.joinGraph != null)
               {
                  Set<JoinGraph.JoinConnectionList> joinConnections = joinGraph.getJoinConnections(table.getName());
                  for(JoinGraph.JoinConnectionList joinConnectionList : joinConnections)
                  {
                     JoinGraph.JoinConnection joinConnection = joinConnectionList.list().get(joinConnectionList.list().size() - 1);
                     if(tableNameBeforeDot.equals(joinConnection.joinTable()))
                     {
                        QTableMetaData joinTable = qInstance.getTable(tableNameBeforeDot);
                        if(joinTable.getFields().containsKey(fieldNameAfterDot))
                        {
                           /////////////////////////
                           // mmm, looks valid... //
                           /////////////////////////
                           return (true);
                        }
                     }
                  }
               }

               //////////////////////////////////////////////////////////////////////////////////////
               // todo - not sure how vulnerable we are to ongoing issues here...                  //
               // idea:  let a filter (or any object?) be opted out of validation, some version of //
               // a static map of objects we can check at the top of various validate methods...   //
               //////////////////////////////////////////////////////////////////////////////////////
               errors.add("Failed to find field named: " + fieldName);
            }
         }
      }
      return foundField;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateApps(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getApps()))
      {
         qInstance.getApps().forEach((appName, app) ->
         {
            assertCondition(Objects.equals(appName, app.getName()), "Inconsistent naming for app: " + appName + "/" + app.getName() + ".");

            validateAppChildHasValidParentAppName(qInstance, app);

            Set<String> appsVisited = new HashSet<>();
            visitAppCheckingForCycles(app, appsVisited);

            if(app.getChildren() != null)
            {
               Set<String> childNames = new HashSet<>();
               for(QAppChildMetaData child : app.getChildren())
               {
                  if(child instanceof QAppMetaData childApp)
                  {
                     assertCondition(Objects.equals(appName, childApp.getParentAppName()), "Child app " + child.getName() + " of app " + appName + " does not have its parent app properly set.");
                  }
                  assertCondition(!childNames.contains(child.getName()), "App " + appName + " contains more than one child named " + child.getName());
                  childNames.add(child.getName());
               }
            }

            ////////////////////////////////////////
            // validate field sections in the app //
            ////////////////////////////////////////
            Set<String> childNamesInSections = new HashSet<>();
            if(app.getSections() != null)
            {
               for(QAppSection section : app.getSections())
               {
                  validateAppSection(app, section, childNamesInSections);
               }
            }

            if(CollectionUtils.nullSafeHasContents(app.getChildren()))
            {
               for(QAppChildMetaData child : app.getChildren())
               {
                  if(!child.getClass().equals(QAppMetaData.class))
                  {
                     assertCondition(childNamesInSections.contains(child.getName()), "App " + appName + " child " + child.getName() + " is not listed in any app sections.");
                  }
               }
            }

            //////////////////////
            // validate widgets //
            //////////////////////
            for(String widgetName : CollectionUtils.nonNullList(app.getWidgets()))
            {
               assertCondition(qInstance.getWidget(widgetName) != null, "App " + appName + " widget " + widgetName + " is not a recognized widget.");
            }

            runPlugins(QAppMetaData.class, app, qInstance);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateWidgets(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getWidgets()))
      {
         qInstance.getWidgets().forEach((widgetName, widget) ->
            {
               assertCondition(Objects.equals(widgetName, widget.getName()), "Inconsistent naming for widget: " + widgetName + "/" + widget.getName() + ".");

               if(assertCondition(widget.getCodeReference() != null, "Missing codeReference for widget: " + widgetName))
               {
                  validateSimpleCodeReference("Widget " + widgetName + " code reference: ", widget.getCodeReference(), AbstractWidgetRenderer.class);
               }

               if(widget instanceof ParentWidgetMetaData parentWidgetMetaData)
               {
                  if(assertCondition(CollectionUtils.nullSafeHasContents(parentWidgetMetaData.getChildWidgetNameList()), "Missing child widgets for parent widget: " + widget.getName()))
                  {
                     for(String childWidgetName : parentWidgetMetaData.getChildWidgetNameList())
                     {
                        assertCondition(qInstance.getWidget(childWidgetName) != null, "Unrecognized child widget name [" + childWidgetName + "] in parent widget: " + widget.getName());
                     }
                  }
               }

               if(widget.getValidatorPlugin() != null && widget.getValidatorPlugin().isEnabled())
               {
                  widget.getValidatorPlugin().validate(widget, qInstance, this);
               }

               runPlugins(QWidgetMetaDataInterface.class, widget, qInstance);
            }
         );
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validatePossibleValueSources(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getPossibleValueSources()))
      {
         qInstance.getPossibleValueSources().forEach((pvsName, possibleValueSource) ->
         {
            assertCondition(Objects.equals(pvsName, possibleValueSource.getName()), "Inconsistent naming for possibleValueSource: " + pvsName + "/" + possibleValueSource.getName() + ".");
            validatePossibleValueSource(qInstance, pvsName, possibleValueSource);
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateSupplementalCustomizers(QInstance qInstance)
   {
      if(CollectionUtils.nullSafeHasContents(qInstance.getSupplementalCustomizers()))
      {
         qInstance.getSupplementalCustomizers().forEach((customizerType, codeReference) ->
         {
            Class<?> requiredType = customizerType.getCodeReferenceType();
            validateSimpleCodeReference("Supplemental Customizer class mismatch: ", codeReference, requiredType);
         });
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void validatePossibleValueSource(QInstance qInstance, String name, QPossibleValueSource possibleValueSource)
   {
      if(assertCondition(possibleValueSource.getType() != null, "Missing type for possibleValueSource: " + name))
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////
         // assert about fields that should and should not be set, based on possible value source type //
         // do additional type-specific validations as well                                            //
         ////////////////////////////////////////////////////////////////////////////////////////////////
         switch(possibleValueSource.getType())
         {
            case ENUM ->
            {
               assertCondition(!StringUtils.hasContent(possibleValueSource.getTableName()), "enum-type possibleValueSource " + name + " should not have a tableName.");
               assertCondition(!CollectionUtils.nullSafeHasContents(possibleValueSource.getSearchFields()), "enum-type possibleValueSource " + name + " should not have searchFields.");
               assertCondition(!CollectionUtils.nullSafeHasContents(possibleValueSource.getOrderByFields()), "enum-type possibleValueSource " + name + " should not have orderByFields.");
               assertCondition(possibleValueSource.getCustomCodeReference() == null, "enum-type possibleValueSource " + name + " should not have a customCodeReference.");

               assertCondition(CollectionUtils.nullSafeHasContents(possibleValueSource.getEnumValues()), "enum-type possibleValueSource " + name + " is missing enum values");
            }
            case TABLE ->
            {
               assertCondition(CollectionUtils.nullSafeIsEmpty(possibleValueSource.getEnumValues()), "table-type possibleValueSource " + name + " should not have enum values.");
               assertCondition(possibleValueSource.getCustomCodeReference() == null, "table-type possibleValueSource " + name + " should not have a customCodeReference.");

               QTableMetaData tableMetaData = null;
               if(assertCondition(StringUtils.hasContent(possibleValueSource.getTableName()), "table-type possibleValueSource " + name + " is missing a tableName."))
               {
                  tableMetaData = qInstance.getTable(possibleValueSource.getTableName());
                  assertCondition(tableMetaData != null, "Unrecognized table " + possibleValueSource.getTableName() + " for possibleValueSource " + name + ".");
               }

               if(assertCondition(CollectionUtils.nullSafeHasContents(possibleValueSource.getSearchFields()), "table-type possibleValueSource " + name + " is missing searchFields."))
               {
                  if(tableMetaData != null)
                  {
                     QTableMetaData finalTableMetaData = tableMetaData;
                     for(String searchField : possibleValueSource.getSearchFields())
                     {
                        assertNoException(() -> finalTableMetaData.getField(searchField), "possibleValueSource " + name + " has an unrecognized searchField: " + searchField);
                     }
                  }
               }

               if(assertCondition(CollectionUtils.nullSafeHasContents(possibleValueSource.getOrderByFields()), "table-type possibleValueSource " + name + " is missing orderByFields."))
               {
                  if(tableMetaData != null)
                  {
                     QTableMetaData finalTableMetaData = tableMetaData;

                     for(QFilterOrderBy orderByField : possibleValueSource.getOrderByFields())
                     {
                        assertNoException(() -> finalTableMetaData.getField(orderByField.getFieldName()), "possibleValueSource " + name + " has an unrecognized orderByField: " + orderByField.getFieldName());
                     }
                  }
               }
            }
            case CUSTOM ->
            {
               assertCondition(CollectionUtils.nullSafeIsEmpty(possibleValueSource.getEnumValues()), "custom-type possibleValueSource " + name + " should not have enum values.");
               assertCondition(!StringUtils.hasContent(possibleValueSource.getTableName()), "custom-type possibleValueSource " + name + " should not have a tableName.");
               assertCondition(!CollectionUtils.nullSafeHasContents(possibleValueSource.getSearchFields()), "custom-type possibleValueSource " + name + " should not have searchFields.");
               assertCondition(!CollectionUtils.nullSafeHasContents(possibleValueSource.getOrderByFields()), "custom-type possibleValueSource " + name + " should not have orderByFields.");

               if(assertCondition(possibleValueSource.getCustomCodeReference() != null, "custom-type possibleValueSource " + name + " is missing a customCodeReference."))
               {
                  validateSimpleCodeReference("PossibleValueSource " + name + " custom code reference: ", possibleValueSource.getCustomCodeReference(), QCustomPossibleValueProvider.class);
               }
            }
            default -> errors.add("Unexpected possibleValueSource type: " + possibleValueSource.getType());
         }

         assertCondition(possibleValueSource.getIdType() != null, "possibleValueSource " + name + " is missing its idType.");

         runPlugins(QPossibleValueSource.class, possibleValueSource, qInstance);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validateSimpleCodeReference(String prefix, QCodeReference codeReference, Class<?>... anyOfExpectedClasses)
   {
      if(!preAssertionsForCodeReference(codeReference, prefix))
      {
         return;
      }

      //////////////////////////////////////////////////////////////////////////////
      // make sure (at this time) that it's a java type, then do some java checks //
      //////////////////////////////////////////////////////////////////////////////
      if(assertCondition(codeReference.getCodeType().equals(QCodeType.JAVA), prefix + "Only JAVA code references are supported at this time."))
      {
         ///////////////////////////////////////
         // make sure the class can be loaded //
         ///////////////////////////////////////
         Class<?> clazz = getClassForCodeReference(codeReference, prefix);
         if(clazz != null)
         {
            //////////////////////////////////////////////////
            // make sure the customizer can be instantiated //
            //////////////////////////////////////////////////
            Object classInstance = getInstanceOfCodeReference(prefix, clazz, codeReference);

            ////////////////////////////////////////////////////////////////////////
            // make sure the customizer instance can be cast to the expected type //
            ////////////////////////////////////////////////////////////////////////
            if(classInstance != null)
            {
               assertObjectCanBeCasted(prefix, classInstance, anyOfExpectedClasses);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Class<?> getClassForCodeReference(QCodeReference codeReference, String prefix)
   {
      Class<?> clazz = null;
      try
      {
         if(codeReference instanceof QCodeReferenceLambda<?> lambdaCodeReference)
         {
            return (lambdaCodeReference.getLambda().getClass());
         }

         clazz = Class.forName(codeReference.getName());
      }
      catch(ClassNotFoundException e)
      {
         errors.add(prefix + "Class for " + codeReference.getName() + " could not be found.");
      }
      return clazz;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean preAssertionsForCodeReference(QCodeReference codeReference, String prefix)
   {
      boolean okay = true;
      if(!assertCondition(StringUtils.hasContent(codeReference.getName()), prefix + " is missing a code reference name"))
      {
         okay = false;
      }

      if(!assertCondition(codeReference.getCodeType() != null, prefix + " is missing a code type"))
      {
         okay = false;
      }

      return (okay);
   }



   /*******************************************************************************
    ** Check if an app's child list can recursively be traversed without finding a
    ** duplicate, which would indicate a cycle (e.g., an error)
    *******************************************************************************/
   private void visitAppCheckingForCycles(QAppMetaData app, Set<String> appsVisited)
   {
      if(assertCondition(!appsVisited.contains(app.getName()), "Circular app reference detected, involving " + app.getName()))
      {
         appsVisited.add(app.getName());
         if(app.getChildren() != null)
         {
            for(QAppChildMetaData child : app.getChildren())
            {
               if(child instanceof QAppMetaData childApp)
               {
                  visitAppCheckingForCycles(childApp, appsVisited);
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateAppChildHasValidParentAppName(QInstance qInstance, QAppMetaData appChild)
   {
      if(appChild.getParentAppName() != null)
      {
         assertCondition(qInstance.getApp(appChild.getParentAppName()) != null, "Unrecognized parent app " + appChild.getParentAppName() + " for " + appChild.getName() + ".");
      }
   }



   /*******************************************************************************
    ** For the given input condition, if it's true, then we're all good (and return true).
    ** But if it's false, add the provided message to the list of errors (and return false,
    ** e.g., in case you need to stop evaluating rules to avoid exceptions).
    *******************************************************************************/
   public boolean assertCondition(boolean condition, String message)
   {
      if(!condition)
      {
         errors.add(message);
      }

      return (condition);
   }



   /*******************************************************************************
    ** For the given lambda, if it doesn't throw an exception, then we're all good (and return true).
    ** But if it throws, add the provided message to the list of errors (and return false,
    ** e.g., in case you need to stop evaluating rules to avoid exceptions).
    *******************************************************************************/
   public boolean assertNoException(UnsafeLambda unsafeLambda, String message)
   {
      try
      {
         unsafeLambda.run();
         return (true);
      }
      catch(Exception e)
      {
         errors.add(message);
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(String message)
   {
      if(printWarnings)
      {
         LOG.info("Validation warning: " + message);
      }
   }



   /*******************************************************************************
    ** Getter for errors
    **
    *******************************************************************************/
   public List<String> getErrors()
   {
      return errors;
   }
}
