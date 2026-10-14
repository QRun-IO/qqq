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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataClassifications;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Single entry point for field policy, enforcement, masking and audit notifications.
 *******************************************************************************/
public final class FieldAccessHelper
{
   public static final String CACHE_KEY = FieldAccessHelper.class.getName();
   private static final QLogger LOG = QLogger.getLogger(FieldAccessHelper.class);



   /*******************************************************************************
    ** Static utility class.
    *******************************************************************************/
   private FieldAccessHelper()
   {

   }



   /*******************************************************************************
    ** Resolve the instance policy.
    *******************************************************************************/
   private static FieldAccessResolverInterface resolver()
   {
      FieldAccessResolverMetaData metadata = FieldAccessResolverMetaData.of(QContext.getQInstance());
      return (metadata == null ? null : metadata.getResolverInstance());
   }



   /*******************************************************************************
    ** Effective access for a real or virtual field.
    *******************************************************************************/
   public static FieldAccessTier getTier(QTableMetaData table, String fieldName)
   {
      return (getDecision(table, field(table, fieldName)).tier());
   }



   /*******************************************************************************
    ** Find a field without throwing for unknown names.
    *******************************************************************************/
   private static QFieldMetaData field(QTableMetaData table, String name)
   {
      if(table == null || name == null)
      {
         return (null);
      }
      QFieldMetaData field = CollectionUtils.nonNullMap(table.getFields()).get(name);
      return (field == null ? CollectionUtils.nonNullMap(table.getVirtualFields()).get(name) : field);
   }



   /*******************************************************************************
    ** Combine locks, cached resolver decisions and the channel ceiling. Never
    ** cache reveal grants. Primary keys are exempt from every policy ceiling.
    *******************************************************************************/
   public static FieldAccessDecision getDecision(QTableMetaData table, QFieldMetaData field)
   {
      if(field == null || QContext.getQSession() instanceof QSystemUserSession)
      {
         return (FieldAccessDecision.FULL);
      }
      boolean primaryKey = table != null && Objects.equals(field.getName(), table.getPrimaryKeyField());
      if(primaryKey)
      {
         return (FieldAccessDecision.FULL);
      }
      FieldAccessTier lockTier = primaryKey ? FieldAccessTier.FULL : getLockTier(field);
      FieldAccessResolverInterface resolver = resolver();
      FieldAccessDecision decision = FieldAccessDecision.FULL;
      if(resolver != null && table != null && !primaryKey)
      {
         String fingerprint = resolver.getAccessFingerprint();
         DecisionCache cache = QContext.getObject(CACHE_KEY, DecisionCache.class).orElse(null);
         if(cache == null || cache.session != QContext.getQSession() || cache.instance != QContext.getQInstance() || cache.resolver != resolver || !Objects.equals(cache.channel, QContext.getQSession() == null ? null : QContext.getQSession().getClientChannelName()) || !Objects.equals(cache.fingerprint, fingerprint))
         {
            cache = new DecisionCache();
            cache.session = QContext.getQSession();
            cache.instance = QContext.getQInstance();
            cache.resolver = resolver;
            cache.channel = QContext.getQSession() == null ? null : QContext.getQSession().getClientChannelName();
            cache.fingerprint = fingerprint;
            QContext.setObject(CACHE_KEY, cache);
         }
         Map<String, FieldAccessDecision> tableDecisions = cache.decisions.computeIfAbsent(table.getName(), ignored -> new HashMap<>());
         decision = tableDecisions.computeIfAbsent(field.getName(), ignored -> Objects.requireNonNullElse(resolver.resolve(table, field), FieldAccessDecision.FULL));
      }
      FieldAccessDecision channelDecision = FieldAccessDecision.FULL;
      QSession session = QContext.getQSession();
      FieldAccessChannelPolicyInterface channelPolicy = ClientChannelsMetaData.of(QContext.getQInstance()).getFieldAccessPolicy(session == null ? null : session.getClientChannelName());
      if(channelPolicy != null)
      {
         channelDecision = Objects.requireNonNullElse(channelPolicy.resolve(table, field), FieldAccessDecision.FULL);
      }
      FieldMaskStyle style = FieldMaskStyle.mostRestrictive(decision.maskStyle(), channelDecision.maskStyle());
      if(field.getFieldSecurityLock() != null)
      {
         style = FieldMaskStyle.mostRestrictive(style, field.getFieldSecurityLock().getMaskStyle());
      }
      FieldAccessTier tier = lockTier.mostRestrictive(decision.tier()).mostRestrictive(channelDecision.tier());
      String reason = lockTier == tier && lockTier != FieldAccessTier.FULL ? "field security lock"
         : decision.tier() == tier && decision.reason() != null ? decision.reason() : channelDecision.reason();
      return (new FieldAccessDecision(tier, Objects.requireNonNullElse(style, FieldMaskStyle.AUTO), reason, decision.exactMatchAllowed() && channelDecision.exactMatchAllowed()));
   }



   /*******************************************************************************
    ** Evaluate the legacy override flip and all-access keys.
    *******************************************************************************/
   public static FieldAccessTier getLockTier(QFieldMetaData field)
   {
      FieldSecurityLock lock = field.getFieldSecurityLock();
      if(lock == null)
      {
         return (FieldAccessTier.FULL);
      }

      QSession session = QContext.getQSession();
      if(session instanceof QSystemUserSession)
      {
         return (FieldAccessTier.FULL);
      }

      FieldSecurityLock.Behavior defaultBehavior = Objects.requireNonNullElse(lock.getDefaultBehavior(), FieldSecurityLock.Behavior.DENY);
      if(session == null)
      {
         return (tierForBehavior(defaultBehavior));
      }

      QInstance        qInstance = QContext.getQInstance();
      QSecurityKeyType keyType   = qInstance == null ? null : qInstance.getSecurityKeyType(lock.getSecurityKeyType());
      if(keyType != null && keyType.getAllAccessKeyName() != null && sessionHasValue(session, keyType.getAllAccessKeyName(), List.of(true)))
      {
         return (FieldAccessTier.FULL);
      }

      boolean overridden = sessionHasValue(session, lock.getSecurityKeyType(), lock.getOverrideValues());
      FieldSecurityLock.Behavior effectiveBehavior = defaultBehavior;
      if(overridden)
      {
         effectiveBehavior = FieldSecurityLock.Behavior.ALLOW.equals(defaultBehavior) ? FieldSecurityLock.Behavior.DENY : FieldSecurityLock.Behavior.ALLOW;
      }

      return (tierForBehavior(effectiveBehavior));
   }



   /*******************************************************************************
    ** Map legacy lock behavior to the access contract.
    *******************************************************************************/
   private static FieldAccessTier tierForBehavior(FieldSecurityLock.Behavior behavior)
   {
      return (switch(behavior)
      {
         case ALLOW -> FieldAccessTier.FULL;
         case REVEAL -> FieldAccessTier.REVEALABLE;
         case MASK -> FieldAccessTier.MASKED;
         case DENY -> FieldAccessTier.HIDDEN;
      });
   }



   /*******************************************************************************
    ** compare session key values to candidate values as strings, so that, e.g., an
    ** Integer 1 in the session matches a String "1" in a lock's overrideValues.
    *******************************************************************************/
   private static boolean sessionHasValue(QSession session, String keyName, Collection<? extends Serializable> candidateValues)
   {
      if(keyName == null || CollectionUtils.nullSafeIsEmpty(candidateValues))
      {
         return (false);
      }

      for(Serializable sessionValue : CollectionUtils.nonNullList(session.getSecurityKeyValues(keyName)))
      {
         for(Serializable candidateValue : candidateValues)
         {
            if(sessionValue != null && candidateValue != null && String.valueOf(sessionValue).equals(String.valueOf(candidateValue)))
            {
               return (true);
            }
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** Access for real and virtual fields, in metadata order.
    *******************************************************************************/
   public static Map<String, FieldAccessTier> getTiers(QTableMetaData table)
   {
      Map<String, FieldAccessTier> tiers = new LinkedHashMap<>();
      if(table != null)
      {
         for(String name : CollectionUtils.nonNullMap(table.getFields()).keySet())
         {
            tiers.put(name, getTier(table, name));
         }
         for(String name : CollectionUtils.nonNullMap(table.getVirtualFields()).keySet())
         {
            tiers.put(name, getTier(table, name));
         }
      }
      return (tiers);
   }



   /*******************************************************************************
    ** Stable fingerprint of the resolver, channel and effective restrictions.
    *******************************************************************************/
   public static String getAccessFingerprint()
   {
      Map<String, String> restrictedLocks = new TreeMap<>();
      QInstance instance = QContext.getQInstance();
      if(instance != null && !(QContext.getQSession() instanceof QSystemUserSession))
      {
         for(QTableMetaData table : CollectionUtils.nonNullMap(instance.getTables()).values())
         {
            Map<String, QFieldMetaData> fields = new TreeMap<>(CollectionUtils.nonNullMap(table.getFields()));
            fields.putAll(CollectionUtils.nonNullMap(table.getVirtualFields()));
            for(QFieldMetaData field : fields.values())
            {
               FieldAccessDecision decision = getDecision(table, field);
               if(decision.tier() != FieldAccessTier.FULL)
               {
                  restrictedLocks.put(table.getName() + "." + field.getName(), decision.tier() + ":" + decision.maskStyle() + ":" + decision.exactMatchAllowed());
               }
            }
         }
      }
      try
      {
         FieldAccessResolverInterface resolver = resolver();
         String fingerprint = resolver == null ? "" : Objects.toString(resolver.getAccessFingerprint(), "");
         String channel = QContext.getQSession() == null ? "" : Objects.toString(QContext.getQSession().getClientChannelName(), "");
         return (fingerprint + "|" + channel + "|" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(restrictedLocks.toString().getBytes(StandardCharsets.UTF_8))));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException(e));
      }
   }



   /*******************************************************************************
    ** Check filters and ordering before querying; successful exact searches are audited once per table.
    *******************************************************************************/
   public static PendingExactMatch assertCriteriaAllowed(String tableName, List<QueryJoin> joins, QQueryFilter filter, Collection<String> sortGroupAggregateFieldNames) throws QPermissionDeniedException
   {
      return (assertCriteriaAllowed(tableName, joins, filter, sortGroupAggregateFieldNames, true));
   }



   /*******************************************************************************
    ** Criteria-only mode has no fingerprints, guardrail callbacks or audit events.
    *******************************************************************************/
   public static PendingExactMatch assertCriteriaAllowed(String tableName, List<QueryJoin> joins, QQueryFilter filter, Collection<String> sortGroupAggregateFieldNames, boolean checkExactMatch) throws QPermissionDeniedException
   {
      Map<String, String> aliases = getAliasToTableNameMap(tableName, joins);
      CriteriaCheck exactMatches = new CriteriaCheck(checkExactMatch);
      checkFilter(tableName, aliases, filter, exactMatches);
      for(String name : CollectionUtils.nonNullCollection(sortGroupAggregateFieldNames))
      {
         checkField(tableName, aliases, name, null, true, exactMatches);
      }
      FieldAccessResolverInterface resolver = resolver();
      for(FieldAccessEvent event : exactMatches.events.values())
      {
         if(resolver != null && checkExactMatch)
         {
            try
            {
               resolver.checkExactMatch(event);
            }
            catch(QPermissionDeniedException e)
            {
               notifyAccess(event.withOutcome(FieldAccessOutcome.REFUSED));
               throw e;
            }
         }
      }
      return (new PendingExactMatch(new ArrayList<>(exactMatches.events.values())));
   }



   /*******************************************************************************
    ** Walk nested filters, including comparisons to another field.
    *******************************************************************************/
   private static void checkFilter(String tableName, Map<String, String> aliases, QQueryFilter filter, CriteriaCheck exactMatches) throws QPermissionDeniedException
   {
      if(filter == null)
      {
         return;
      }
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(criteria != null)
         {
            boolean comparison = criteria.getOtherFieldName() != null;
            checkField(tableName, aliases, criteria.getFieldName(), criteria, comparison, exactMatches);
            checkField(tableName, aliases, criteria.getOtherFieldName(), criteria, comparison, exactMatches);
         }
      }
      for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         if(orderBy != null)
         {
            checkField(tableName, aliases, orderBy.getFieldName(), null, true, exactMatches);
         }
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         checkFilter(tableName, aliases, subFilter, exactMatches);
      }
   }



   /*******************************************************************************
    ** Field comparisons require FULL: they are not a search for a known literal value.
    *******************************************************************************/
   private static void checkField(String tableName, Map<String, String> aliases, String name, QFilterCriteria criteria, boolean requiresFull, CriteriaCheck exactMatches) throws QPermissionDeniedException
   {
      if(name == null || QContext.getQInstance() == null)
      {
         return;
      }
      TableAndField resolved = resolve(tableName, aliases, name);
      QTableMetaData table = QContext.getQInstance().getTable(resolved.tableName());
      FieldAccessTier tier = getTier(table, resolved.fieldName());
      boolean allowed = requiresFull ? tier == FieldAccessTier.FULL : canFilter(table, resolved.fieldName(), criteria.getOperator());
      // Audit payload ownership is resolved per row, not proven by arbitrary boolean filters.
      if((AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL.equals(resolved.tableName()) && Set.of("oldValue", "newValue", "message").contains(resolved.fieldName()))
         || (AuditsMetaDataProvider.TABLE_NAME_AUDIT.equals(resolved.tableName()) && "message".equals(resolved.fieldName()))
         || (name.contains(".") && table == null))
      {
         allowed = false;
      }
      if(tier == FieldAccessTier.REVEALABLE && criteria != null)
      {
         int count = exactMatches.valueCounts.merge(resolved, CollectionUtils.nonNullList(criteria.getValues()).size(), Integer::sum);
         if(count > 25 || criteria.getFieldFunction() != null || CollectionUtils.nonNullList(criteria.getValues()).stream().anyMatch(value -> !isLiteral(value)))
         {
            allowed = false;
         }
      }
      if(!allowed)
      {
         if(exactMatches.audit)
         {
            notifyAccess(new FieldAccessEvent().withType(FieldAccessEventType.DENIED).withTableName(resolved.tableName()).withFieldNames(List.of(resolved.fieldName())));
         }
         throw (new QPermissionDeniedException("Permission denied."));
      }
      if(tier == FieldAccessTier.REVEALABLE && exactMatches.audit)
      {
         FieldAccessEvent event = exactMatches.events.computeIfAbsent(resolved.tableName(), ignored -> new FieldAccessEvent()
            .withType(FieldAccessEventType.EXACT_MATCH_FILTER).withTableName(resolved.tableName()).withFieldNames(new ArrayList<>()).withSearchValueFingerprints(List.of()));
         if(!event.getFieldNames().contains(resolved.fieldName()))
         {
            event.getFieldNames().add(resolved.fieldName());
         }
         FieldAccessResolverInterface resolver = resolver();
         if(resolver != null)
         {
            List<String> fingerprints = new ArrayList<>(event.getSearchValueFingerprints());
            for(Serializable value : CollectionUtils.nonNullList(criteria.getValues()))
            {
               String fingerprint = resolver.fingerprintSearchValue(value);
               if(fingerprint != null)
               {
                  fingerprints.add(fingerprint);
               }
            }
            event.withSearchValueFingerprints(fingerprints);
         }
      }
   }



   /*******************************************************************************
    ** Only scalar query literals may enter the exact-match fingerprint callback.
    ** Expression strings are rejected before expression expansion as well.
    *******************************************************************************/
   private static boolean isLiteral(Serializable value)
   {
      return (value == null || (value instanceof String string && !string.contains("${"))
         || value instanceof Boolean || value instanceof Character || value instanceof Byte || value instanceof Short
         || value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double
         || value instanceof BigDecimal || value instanceof BigInteger || value instanceof LocalDate
         || value instanceof LocalTime || value instanceof Instant || value instanceof java.util.UUID);
   }



   /*******************************************************************************
    ** One budget per resolved field across the complete filter tree.
    *******************************************************************************/
   private static class CriteriaCheck
   {
      private final Map<String, FieldAccessEvent> events = new LinkedHashMap<>();
      private final Map<TableAndField, Integer> valueCounts = new HashMap<>();
      private final boolean audit;



      /*******************************************************************************
       ** Criteria-only checks have no guardrail or audit side effects.
       *******************************************************************************/
      private CriteriaCheck(boolean audit)
      {
         this.audit = audit;
      }
   }



   /*******************************************************************************
    ** Clone only when restricting metadata; do not expose restricted defaults.
    *******************************************************************************/
   public static QTableMetaData applyToTable(QTableMetaData table)
   {
      Map<String, FieldAccessTier> tiers = getTiers(table);
      Set<String> deniedFieldNames = new LinkedHashSet<>();
      tiers.forEach((name, tier) ->
      {
         if(tier == FieldAccessTier.HIDDEN)
         {
            deniedFieldNames.add(name);
         }
      });
      if(tiers.values().stream().allMatch(tier -> tier == FieldAccessTier.FULL))
      {
         return (table);
      }

      QTableMetaData clone = table.clone();

      Map<String, QFieldMetaData> fields = new LinkedHashMap<>(CollectionUtils.nonNullMap(clone.getFields()));
      fields.keySet().removeAll(deniedFieldNames);
      clone.setFields(fields);

      if(clone.getVirtualFields() != null)
      {
         clone.getVirtualFields().keySet().removeAll(deniedFieldNames);
      }

      if(clone.getSections() != null)
      {
         List<QFieldSection> sections = new ArrayList<>();
         for(QFieldSection section : clone.getSections())
         {
            QFieldSection sectionClone = section.clone();
            if(sectionClone.getFieldNames() != null)
            {
               sectionClone.getFieldNames().removeAll(deniedFieldNames);
            }
            sections.add(sectionClone);
         }
         clone.setSections(sections);
      }

      for(Map.Entry<String, FieldAccessTier> entry : tiers.entrySet())
      {
         QFieldMetaData field = field(clone, entry.getKey());
         if(field != null && entry.getValue() != FieldAccessTier.FULL)
         {
            field.setIsEditable(false);
            field.setDefaultValue(null);
         }
      }
      return (clone);
   }



   /*******************************************************************************
    ** Refuse supplied values, including explicit null, for non-FULL fields.
    *******************************************************************************/
   public static void assertWritesAllowed(QTableMetaData table, List<QRecord> records) throws QPermissionDeniedException
   {
      Set<String> deniedFieldNames = new LinkedHashSet<>();
      getTiers(table).forEach((name, tier) ->
      {
         if(!tier.canWrite())
         {
            deniedFieldNames.add(name);
         }
      });
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         for(QRecord record : CollectionUtils.nonNullList(records))
         {
            if(record != null && childTable != null)
            {
               assertWritesAllowed(childTable, CollectionUtils.nonNullMap(record.getAssociatedRecords()).get(association.getName()));
            }
         }
      }

      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(record == null || record.getValues() == null)
         {
            continue;
         }

         for(String deniedFieldName : deniedFieldNames)
         {
            if(record.getValues().containsKey(deniedFieldName))
            {
               QFieldMetaData field = table.getFields().containsKey(deniedFieldName) ? table.getField(deniedFieldName) : null;
               String         label = field == null || field.getLabel() == null ? deniedFieldName : field.getLabel();
               notifyAccess(new FieldAccessEvent().withType(FieldAccessEventType.DENIED).withTableName(table.getName()).withFieldNames(List.of(deniedFieldName)));
               throw (new QPermissionDeniedException("Permission denied: you may not write to the field [" + label + "]."));
            }
         }
      }
   }



   /*******************************************************************************
    ** Mask once, recursively, and emit one value-free view event per table.
    *******************************************************************************/
   public static void applyToRecords(String tableName, List<QueryJoin> joins, List<QRecord> records, String actionName)
   {
      Map<String, ViewAudit> audits = new LinkedHashMap<>();
      applyRecords(tableName, joins, records, actionName, audits, new IdentityHashMap<>());
      for(Map.Entry<String, ViewAudit> entry : audits.entrySet())
      {
         ViewAudit audit = entry.getValue();
         if(!audit.fields.isEmpty())
         {
            notifyAccess(new FieldAccessEvent().withType(FieldAccessEventType.VIEW).withTableName(entry.getKey()).withFieldNames(new ArrayList<>(audit.fields)).withPrimaryKeys(new ArrayList<>(audit.keys)).withRecordCount(audit.count).withActionName(actionName));
         }
      }
   }



   /*******************************************************************************
    ** Use each joined record identity for reveal checks; shared associated objects are masked only once.
    *******************************************************************************/
   private static void applyRecords(String tableName, List<QueryJoin> joins, List<QRecord> records, String actionName, Map<String, ViewAudit> audits, Map<QRecord, Set<String>> visited)
   {
      if(QContext.getQInstance() == null || records == null)
      {
         return;
      }
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         return;
      }
      Map<String, String> aliases = getAliasToTableNameMap(tableName, joins);
      List<QRecord> visible = new ArrayList<>(records);
      try
      {
         AuditFieldAccess.apply(tableName, joins, visible);
      }
      catch(QException e)
      {
         LOG.warn("Unable to resolve audit ownership; withholding record values");
         visible.clear();
      }
      if(visible.size() != records.size())
      {
         List<QRecord> withheld = new ArrayList<>(records);
         withheld.removeAll(visible);
         withheld.forEach(FieldAccessHelper::withholdValues);
         try
         {
            records.removeAll(withheld);
         }
         catch(UnsupportedOperationException e)
         {
            // Fixed-size caller collections retain only the sanitized record identities.
         }
      }
      for(QRecord record : visible)
      {
         if(record == null || !visited.computeIfAbsent(record, ignored -> new LinkedHashSet<>()).add(tableName))
         {
            continue;
         }
         if(CollectionUtils.nonNullList(table.getRecordLabelFields()).stream().anyMatch(name -> getTier(table, name) != FieldAccessTier.FULL))
         {
            record.setRecordLabel(null);
         }
         Set<String> keys = new LinkedHashSet<>(CollectionUtils.nonNullMap(record.getValues()).keySet());
         keys.addAll(CollectionUtils.nonNullMap(record.getDisplayValues()).keySet());
         Set<String> seenTables = new LinkedHashSet<>();
         for(String key : keys)
         {
            TableAndField resolved = resolve(tableName, aliases, key);
            QTableMetaData valueTable = QContext.getQInstance().getTable(resolved.tableName());
            if(valueTable == null && key.contains("."))
            {
               if(record.getValues() != null)
               {
                  record.getValues().remove(key);
               }
               if(record.getDisplayValues() != null)
               {
                  record.getDisplayValues().remove(key);
               }
               continue;
            }
            QFieldMetaData field = field(valueTable, resolved.fieldName());
            if(field == null)
            {
               continue;
            }
            FieldAccessDecision decision = getDecision(valueTable, field);
            QPossibleValueSource source = field.getPossibleValueSourceName() == null ? null : QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
            if(source != null && source.getType() == QPossibleValueSourceType.TABLE)
            {
               QTableMetaData target = QContext.getQInstance().getTable(source.getTableName());
               if(target == null || !isRecordLabelFullAccess(target))
               {
                  record.setDisplayValue(key, Objects.toString(record.getValue(key), null));
               }
            }
            String prefix = key.contains(".") ? key.substring(0, key.indexOf('.') + 1) : "";
            Serializable primaryKey = record.getValue(prefix + valueTable.getPrimaryKeyField());
            DataClassification classification = AiDataClassifications.ofField(valueTable, field.getName());
            if(decision.tier() != FieldAccessTier.FULL || classification == DataClassification.PII || classification == DataClassification.PHI)
            {
               ViewAudit audit = audits.computeIfAbsent(valueTable.getName(), ignored -> new ViewAudit());
               audit.fields.add(field.getName());
               if(primaryKey != null && audit.keys.size() < 50)
               {
                  audit.keys.add(primaryKey);
               }
               if(seenTables.add(valueTable.getName()))
               {
                  audit.count++;
               }
            }
            if(decision.tier() == FieldAccessTier.HIDDEN)
            {
               if(record.getValues() != null)
               {
                  record.getValues().remove(key);
               }
               if(record.getDisplayValues() != null)
               {
                  record.getDisplayValues().remove(key);
               }
            }
            else if(decision.tier() != FieldAccessTier.FULL)
            {
               FieldAccessResolverInterface resolver = resolver();
               boolean revealed = decision.tier() == FieldAccessTier.REVEALABLE && primaryKey != null && resolver != null && !"export".equals(actionName) && !"report".equals(actionName) && resolver.isRevealActive(valueTable.getName(), field.getName(), primaryKey);
               if(!revealed)
               {
                  Serializable value = record.getValue(key);
                  String masked = FieldValueMasker.mask(value == null ? null : String.valueOf(value), field.getType(), decision.maskStyle());
                  record.setValue(key, masked);
                  if(record.getDisplayValues() == null)
                  {
                     record.setDisplayValues(new HashMap<>());
                  }
                  record.setDisplayValue(key, masked);
               }
            }
         }
         for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
         {
            applyRecords(association.getAssociatedTableName(), null, CollectionUtils.nonNullMap(record.getAssociatedRecords()).get(association.getName()), actionName, audits, visited);
         }
      }
   }



   /*******************************************************************************
    ** Process records can belong to different tables, independently of the process.
    ** Unknown ownership cannot safely inherit another table's field policy.
    *******************************************************************************/
   public static void applyToProcessRecords(String processName, String fallbackTable, List<QRecord> records)
   {
      if(QContext.getQSession() instanceof QSystemUserSession)
      {
         return;
      }
      for(QRecord record : records)
      {
         String tableName = record.getTableName() == null ? fallbackTable : record.getTableName();
         if(QContext.getQInstance().getTable(tableName) != null)
         {
            applyToRecords(tableName, null, List.of(record), "process");
         }
         else if(resolver() != null || QContext.getQInstance().getTables().values().stream().anyMatch(table -> getTiers(table).values().stream().anyMatch(tier -> tier != FieldAccessTier.FULL)))
         {
            withholdValues(record);
            String warningKey = FieldAccessHelper.class.getName() + ".unknownProcessTable." + processName;
            if(QContext.getObject(warningKey, Boolean.class).isEmpty())
            {
               LOG.warn("Withholding process record values with unknown table ownership", com.kingsrook.qqq.backend.core.logging.LogUtils.logPair("process", processName));
               QContext.setObject(warningKey, true);
            }
         }
      }
   }



   /*******************************************************************************
    ** Preserve only the conventional record identity when ownership is unknown.
    *******************************************************************************/
   private static void withholdValues(QRecord record)
   {
      Serializable id = record.getValue("id");
      record.setValues(new LinkedHashMap<>());
      if(id != null)
      {
         record.setValue("id", id);
      }
      record.setDisplayValues(new HashMap<>());
      record.setRecordLabel(null);
      record.setAssociatedRecords(new LinkedHashMap<>());
   }



   /*******************************************************************************
    ** Audit failures do not change the outcome; exception messages may contain private data.
    *******************************************************************************/
   private static void notifyAccess(FieldAccessEvent event)
   {
      FieldAccessResolverInterface resolver = resolver();
      if(resolver != null)
      {
         try
         {
            resolver.onAccess(event);
         }
         catch(Exception e)
         {
            LOG.warn("Field access audit callback failed");
         }
      }
   }



   /*******************************************************************************
    ** map of table-name-or-alias to table name, for the base table and the joins.
    *******************************************************************************/
   private static Map<String, String> getAliasToTableNameMap(String tableName, List<QueryJoin> queryJoins)
   {
      Map<String, String> rs = new LinkedHashMap<>();
      rs.put(tableName, tableName);
      for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
      {
         if(queryJoin != null && queryJoin.getJoinTable() != null)
         {
            rs.put(queryJoin.getJoinTableOrItsAlias(), queryJoin.getJoinTable());
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** resolve a (possibly "tableOrAlias.fieldName") field name to its table name
    ** and plain field name.  Unknown prefixes are treated as table names (e.g.,
    ** for joins that are implied by the filter, not listed in the input's joins).
    *******************************************************************************/
   private static TableAndField resolve(String tableName, Map<String, String> aliasToTableName, String fieldName)
   {
      int dotIndex = fieldName.indexOf('.');
      if(dotIndex > 0)
      {
         String tableOrAlias = fieldName.substring(0, dotIndex);
         return (new TableAndField(aliasToTableName.getOrDefault(tableOrAlias, tableOrAlias), fieldName.substring(dotIndex + 1)));
      }
      return (new TableAndField(tableName, fieldName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record TableAndField(String tableName, String fieldName)
   {
   }




   /*******************************************************************************
    ** Request-local resolver decisions. QContext resets this on session changes.
    *******************************************************************************/
   private static class DecisionCache implements Serializable
   {
      private transient QSession session;
      private transient QInstance instance;
      private transient FieldAccessResolverInterface resolver;
      private String fingerprint;
      private String channel;
      private final Map<String, Map<String, FieldAccessDecision>> decisions = new HashMap<>();
   }



   /*******************************************************************************
    ** Accumulate value-free audit information across joins and associations.
    *******************************************************************************/
   private static class ViewAudit
   {
      private final Set<String> fields = new LinkedHashSet<>();
      private final Set<Serializable> keys = new LinkedHashSet<>();
      private int count;
   }



   /*******************************************************************************
    ** Selecting masks is allowed; explicit selection of a HIDDEN field retains the legacy denial.
    *******************************************************************************/
   public static void assertSelectedFieldsAllowed(String tableName, List<QueryJoin> joins, Collection<String> names) throws QPermissionDeniedException
   {
      Map<String, String> aliases = getAliasToTableNameMap(tableName, joins);
      for(String name : CollectionUtils.nonNullCollection(names))
      {
         TableAndField resolved = resolve(tableName, aliases, name);
         if(getTier(QContext.getQInstance().getTable(resolved.tableName()), resolved.fieldName()) == FieldAccessTier.HIDDEN)
         {
            checkField(tableName, aliases, name, null, true, new CriteriaCheck(true));
         }
      }
   }



   /*******************************************************************************
    ** Whether the policy permits this filter operator, including enumeration restrictions.
    *******************************************************************************/
   public static boolean canFilter(QTableMetaData table, String fieldName, QCriteriaOperator operator)
   {
      FieldAccessDecision decision = getDecision(table, field(table, fieldName));
      return (decision.tier().canFilter(operator) && (decision.tier() != FieldAccessTier.REVEALABLE || decision.exactMatchAllowed()));
   }



   /*******************************************************************************
    ** Labels must contain only fields that are fully readable.
    *******************************************************************************/
   public static boolean isRecordLabelFullAccess(QTableMetaData table)
   {
      return (CollectionUtils.nonNullList(table.getRecordLabelFields()).stream().allMatch(name -> getTier(table, name) == FieldAccessTier.FULL));
   }



   /*******************************************************************************
    ** Per-invocation exact searches, completed only by the action that enforced them.
    *******************************************************************************/
   public record PendingExactMatch(List<FieldAccessEvent> events)
   {
      /*******************************************************************************
       ** Report the successful execution outcome, including streamed result counts.
       *******************************************************************************/
      public void complete(int count, String actionName)
      {
         for(FieldAccessEvent event : events)
         {
            notifyAccess(event.withRecordCount(count).withActionName(actionName).withOutcome(count > 0 ? FieldAccessOutcome.MATCH : FieldAccessOutcome.NO_MATCH));
         }
      }
   }



   /*******************************************************************************
    ** Mask historical values without honoring reveal grants. Supports joined audit detail rows.
    *******************************************************************************/
   public static void applyToAuditDetails(String tableName, List<QRecord> auditDetails)
   {
      String prefix = auditDetails.stream().anyMatch(record -> record.getValues().containsKey("auditDetail.fieldName")) ? "auditDetail." : "";
      applyToAuditDetails(tableName, auditDetails, prefix);
   }



   /*******************************************************************************
    ** Handle the actual join alias at audit query boundaries.
    *******************************************************************************/
   static void applyToAuditDetails(String tableName, List<QRecord> auditDetails, String prefix)
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      auditDetails.removeIf(record ->
      {
         String fieldName = record.getValueString(prefix + "fieldName");
         FieldAccessDecision decision = getDecision(table, field(table, fieldName));
         if(decision.tier() == FieldAccessTier.HIDDEN)
         {
            return (true);
         }
         if(decision.tier() != FieldAccessTier.FULL)
         {
            QFieldMetaData field = field(table, fieldName);
            for(String name : List.of("oldValue", "newValue"))
            {
               String key = prefix + name;
               String mask = FieldValueMasker.mask(record.getValueString(key), field.getType(), decision.maskStyle());
               record.setValue(key, mask);
               record.setDisplayValue(key, mask);
            }
            record.setValue(prefix + "message", null);
            record.setDisplayValue(prefix + "message", null);
            record.setValue("message", null);
            record.setDisplayValue("message", null);
            record.setRecordLabel(null);
         }
         return (false);
      });
   }
}
