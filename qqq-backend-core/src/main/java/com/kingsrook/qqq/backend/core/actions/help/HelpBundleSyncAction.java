/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.help;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentHash;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentSyncContext;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundleEntry;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles.EntryKey;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles.WinningEntry;


/*******************************************************************************
 ** Three-way merge of raw bundle text and user edits, through audited table actions.
 *******************************************************************************/
public class HelpBundleSyncAction
{
   private static final QLogger LOG = QLogger.getLogger(HelpBundleSyncAction.class);
   private static final int BATCH_SIZE = 250;


   /*******************************************************************************
    ** Counts describe planned/applied work; upstreamChanged includes pending review.
    *******************************************************************************/
   public record Result(int inserted, int updated, int upstreamChanged, int retired, int deleted, int unchanged) implements Serializable
   {
   }



   /*******************************************************************************
    ** Apply the current registered bundles.
    *******************************************************************************/
   public Result execute() throws QException
   {
      return (sync(false));
   }



   /*******************************************************************************
    ** Calculate the same decisions without writes, alerts or live metadata changes.
    *******************************************************************************/
   public Result preview() throws QException
   {
      return (sync(true));
   }



   /*******************************************************************************
    ** Serialize local sync/review actions on the per-instance registry.
    *******************************************************************************/
   private Result sync(boolean preview) throws QException
   {
      HelpBundles registry = HelpBundles.of(QContext.getQInstance());
      synchronized(registry)
      {
         Map<EntryKey, WinningEntry> winners = registry.resolveWinningEntries();
         Map<EntryKey, QRecord> rows = new LinkedHashMap<>();
         for(QRecord row : query())
         {
            rows.put(new EntryKey(row.getValueString("key"), row.getValueString("role")), row);
         }
         List<QRecord> inserts = new ArrayList<>();
         List<QRecord> updates = new ArrayList<>();
         List<Serializable> deletes = new ArrayList<>();
         int updated = 0;
         int flagged = 0;
         int retired = 0;
         int unchanged = 0;
         for(var item : winners.entrySet())
         {
            WinningEntry winner = item.getValue();
            HelpBundleEntry entry = winner.entry();
            if(entry.getRetired())
            {
               continue;
            }
            QRecord old = rows.remove(item.getKey());
            String hash = hash(entry);
            if(old == null)
            {
               inserts.add(seed(new QRecord().withValue("key", entry.getKey()).withValue("role", entry.getRole())
                  .withValue("source", winner.bundle().getLayer().name()), winner));
            }
            else if(!hasBundle(old))
            {
               unchanged++;
            }
            else if(Objects.equals(HelpContentHash.of(old), old.getValueString("lastSeededHash")))
            {
               QRecord next = seed(new QRecord(old), winner);
               if(changed(old, next))
               {
                  updates.add(next);
                  updated++;
               }
               else
               {
                  unchanged++;
               }
            }
            else if(!Objects.equals(hash, old.getValueString("lastSeededHash")))
            {
               QRecord next = new QRecord(old).withValue("upstreamStatus", "UPSTREAM_CHANGED")
                  .withValue("upstreamSummary", entry.getSummary()).withValue("upstreamContent", entry.getContent())
                  .withValue("bundleId", winner.bundle().getId()).withValue("bundleVersion", winner.bundle().getVersion());
               if(changed(old, next))
               {
                  updates.add(next);
               }
               flagged++;
            }
            else
            {
               unchanged++;
            }
         }
         for(QRecord old : rows.values())
         {
            if(!hasBundle(old))
            {
               unchanged++;
            }
            else if(Objects.equals(HelpContentHash.of(old), old.getValueString("lastSeededHash")))
            {
               deletes.add(old.getValue("id"));
            }
            else if(!"RETIRED".equals(old.getValueString("upstreamStatus")))
            {
               updates.add(new QRecord(old).withValue("upstreamStatus", "RETIRED").withValue("upstreamSummary", null).withValue("upstreamContent", null));
               retired++;
            }
            else
            {
               unchanged++;
            }
         }
         Result result = new Result(inserts.size(), updated, flagged, retired, deletes.size(), unchanged);
         if(!preview)
         {
            try(HelpContentSyncContext ignored = new HelpContentSyncContext())
            {
               for(int start = 0; start < inserts.size(); start += BATCH_SIZE)
               {
                  check(new InsertAction().execute(new InsertInput(HelpContent.TABLE_NAME).withRecords(inserts.subList(start, Math.min(start + BATCH_SIZE, inserts.size())))).getRecords());
               }
               update(updates);
               for(int start = 0; start < deletes.size(); start += BATCH_SIZE)
               {
                  var output = new DeleteAction().execute(new DeleteInput(HelpContent.TABLE_NAME).withPrimaryKeys(deletes.subList(start, Math.min(start + BATCH_SIZE, deletes.size()))));
                  check(output.getRecordsWithErrors());
               }
            }
            finally
            {
               QInstanceHelpContentManager.reloadHelpContent(QContext.getQInstance());
            }
            updateAlert(flagged);
            LOG.info("Help bundle sync: " + result);
         }
         return (result);
      }
   }



   /*******************************************************************************
    ** Apply upstream text to selected pending rows.
    *******************************************************************************/
   public void acceptUpstream(Collection<? extends Serializable> ids) throws QException
   {
      review(ids, true);
   }



   /*******************************************************************************
    ** Retain local text while acknowledging the current upstream baseline.
    *******************************************************************************/
   public void keepMine(Collection<? extends Serializable> ids) throws QException
   {
      review(ids, false);
   }



   /*******************************************************************************
    ** Resolve against the current winner, including format (not stored separately).
    *******************************************************************************/
   private void review(Collection<? extends Serializable> ids, boolean accept) throws QException
   {
      HelpBundles registry = HelpBundles.of(QContext.getQInstance());
      synchronized(registry)
      {
         var winners = registry.resolveWinningEntries();
         List<QRecord> updates = new ArrayList<>();
         for(QRecord old : query())
         {
            if(!ids.contains(old.getValue("id")) || !"UPSTREAM_CHANGED".equals(old.getValueString("upstreamStatus")))
            {
               continue;
            }
            WinningEntry winner = winners.get(new EntryKey(old.getValueString("key"), old.getValueString("role")));
            if(winner == null || winner.entry().getRetired())
            {
               throw new QException("Upstream entry is no longer available; sync help again before reviewing " + old.getValueString("key"));
            }
            if(!Objects.equals(old.getValueString("bundleId"), winner.bundle().getId())
               || !Objects.equals(old.getValueString("bundleVersion"), winner.bundle().getVersion())
               || !Objects.equals(old.getValueString("upstreamSummary"), winner.entry().getSummary())
               || !Objects.equals(old.getValueString("upstreamContent"), winner.entry().getContent()))
            {
               throw new QException("Upstream text changed again; sync help before reviewing " + old.getValueString("key"));
            }
            QRecord next = seed(new QRecord(old), winner);
            if(!accept)
            {
               for(String field : List.of("format", "summary", "content"))
               {
                  next.setValue(field, old.getValue(field));
               }
            }
            if(QContext.getQSession() != null && QContext.getQSession().getUser() != null)
            {
               next.setValue("modifiedBy", QContext.getQSession().getUser().getIdReference());
            }
            updates.add(next);
         }
         try(HelpContentSyncContext ignored = new HelpContentSyncContext())
         {
            update(updates);
         }
         finally
         {
            QInstanceHelpContentManager.reloadHelpContent(QContext.getQInstance());
         }
         refreshAlert();
      }
   }



   /*******************************************************************************
    ** Refresh the alert from outstanding reviews, including idempotent runs.
    *******************************************************************************/
   private void refreshAlert() throws QException
   {
      long count = query().stream().filter(row -> "UPSTREAM_CHANGED".equals(row.getValueString("upstreamStatus"))).count();
      updateAlert(count);
   }



   /*******************************************************************************
    ** Raise or resolve the single alert for this synchronization scope.
    *******************************************************************************/
   private void updateAlert(long count)
   {
      if(count > 0)
      {
         QAlerts.raise(new AlertSignal("help.upstream.changed").withDedupKey("help-sync")
            .withTitle(count + " help entries changed upstream and need review"));
      }
      else
      {
         QAlerts.resolve("help.upstream.changed", "help-sync", "No help entries need upstream review");
      }
   }



   /*******************************************************************************
    ** Read using the caller's normal permissions.
    *******************************************************************************/
   private List<QRecord> query() throws QException
   {
      List<QRecord> records = new ArrayList<>();
      while(true)
      {
         List<QRecord> page = new QueryAction().execute(new QueryInput(HelpContent.TABLE_NAME).withShouldFetchHeavyFields(true)
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withSkip(records.size()).withLimit(BATCH_SIZE))).getRecords();
         records.addAll(page);
         if(page.size() < BATCH_SIZE)
         {
            return (records);
         }
      }
   }



   /*******************************************************************************
    ** Batch writes without bypassing validation, customizers or auditing.
    *******************************************************************************/
   private void update(List<QRecord> records) throws QException
   {
      for(int start = 0; start < records.size(); start += BATCH_SIZE)
      {
         check(new UpdateAction().execute(new UpdateInput(HelpContent.TABLE_NAME)
            .withRecords(records.subList(start, Math.min(start + BATCH_SIZE, records.size())))).getRecords());
      }
   }



   /*******************************************************************************
    ** Table actions may report record-level failures instead of throwing.
    *******************************************************************************/
   private void check(List<QRecord> records) throws QException
   {
      if(records != null)
      {
         for(QRecord record : records)
         {
            if(record.getErrors() != null && !record.getErrors().isEmpty())
            {
               throw new QException("Help sync write failed: " + record.getErrorsAsString());
            }
         }
      }
   }



   /*******************************************************************************
    ** User-created rows are never seeded or retired by bundles.
    *******************************************************************************/
   private boolean hasBundle(QRecord row)
   {
      return (row.getValueString("bundleId") != null && !row.getValueString("bundleId").isBlank());
   }



   /*******************************************************************************
    ** Seed current text and baseline, preserving original source and author.
    *******************************************************************************/
   private QRecord seed(QRecord row, WinningEntry winner)
   {
      HelpBundleEntry entry = winner.entry();
      return (row.withValue("format", entry.getFormat()).withValue("summary", entry.getSummary()).withValue("content", entry.getContent())
         .withValue("bundleId", winner.bundle().getId()).withValue("bundleVersion", winner.bundle().getVersion())
         .withValue("lastSeededHash", hash(entry)).withValue("upstreamStatus", "CURRENT")
         .withValue("upstreamSummary", null).withValue("upstreamContent", null));
   }



   /*******************************************************************************
    ** Exact hash of the raw bundle fields.
    *******************************************************************************/
   private String hash(HelpBundleEntry entry)
   {
      return (HelpContentHash.of(entry.getFormat(), entry.getSummary(), entry.getContent()));
   }



   /*******************************************************************************
    ** Avoid no-op updates, timestamps and audit entries on repeated runs.
    *******************************************************************************/
   private boolean changed(QRecord before, QRecord after)
   {
      return (!before.getValues().equals(after.getValues()));
   }
}
