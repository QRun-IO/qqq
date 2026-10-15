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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
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
    ** Serialize local sync/review actions on the per-instance registry. Decisions
    ** are planned from one snapshot, then made again per batch from freshly read
    ** rows just before writing, so a second instance syncing the same bundles at
    ** the same time (replicas starting together) finds its work already done
    ** instead of writing, and auditing, every row a second time.
    *******************************************************************************/
   private Result sync(boolean preview) throws QException
   {
      HelpBundles registry = HelpBundles.of(QContext.getQInstance());
      synchronized(registry)
      {
         Map<EntryKey, WinningEntry> winners = new LinkedHashMap<>();
         registry.resolveWinningEntries().forEach((key, winner) ->
         {
            if(!winner.entry().getRetired())
            {
               winners.put(key, winner);
            }
         });
         Map<EntryKey, QRecord> rows = new LinkedHashMap<>();
         for(QRecord row : query(null))
         {
            rows.put(key(row), row);
         }
         List<EntryKey> keys = new ArrayList<>(winners.keySet());
         rows.keySet().stream().filter(key -> !winners.containsKey(key)).forEach(keys::add);

         Counts planned = new Counts();
         for(EntryKey key : keys)
         {
            planned.add(decide(rows.get(key), winners.get(key)));
         }
         if(preview)
         {
            return (planned.result());
         }

         Counts applied = new Counts();
         try(HelpContentSyncContext ignored = new HelpContentSyncContext())
         {
            for(int start = 0; start < keys.size(); start += BATCH_SIZE)
            {
               List<EntryKey> batch = keys.subList(start, Math.min(start + BATCH_SIZE, keys.size()));
               Map<EntryKey, QRecord> current = new LinkedHashMap<>();
               for(QRecord row : query(batch.stream().map(EntryKey::key).distinct().toList()))
               {
                  current.put(key(row), row);
               }
               List<QRecord> inserts = new ArrayList<>();
               List<QRecord> updates = new ArrayList<>();
               List<Serializable> deletes = new ArrayList<>();
               for(EntryKey key : batch)
               {
                  Decision decision = decide(current.get(key), winners.get(key));
                  applied.add(decision);
                  switch(decision.kind())
                  {
                     case INSERT -> inserts.add(decision.write());
                     case DELETE -> deletes.add(decision.write().getValue("id"));
                     default ->
                     {
                        if(decision.write() != null)
                        {
                           updates.add(decision.write());
                        }
                     }
                  }
               }
               if(!inserts.isEmpty())
               {
                  check(new InsertAction().execute(new InsertInput(HelpContent.TABLE_NAME).withRecords(inserts)).getRecords());
               }
               update(updates);
               if(!deletes.isEmpty())
               {
                  check(new DeleteAction().execute(new DeleteInput(HelpContent.TABLE_NAME).withPrimaryKeys(deletes)).getRecordsWithErrors());
               }
            }
         }
         finally
         {
            QInstanceHelpContentManager.reloadHelpContent(QContext.getQInstance());
         }
         Result result = applied.result();
         updateAlert(result.upstreamChanged());
         LOG.info("Help bundle sync: " + result);
         return (result);
      }
   }



   /*******************************************************************************
    ** What one key needs: a row to insert, update or delete, or nothing.
    *******************************************************************************/
   private enum Kind
   {
      INSERT, UPDATE, UPSTREAM_CHANGED, RETIRE, DELETE, UNCHANGED
   }



   /*******************************************************************************
    ** A decision and the record to write, or null when nothing needs writing.
    *******************************************************************************/
   private record Decision(Kind kind, QRecord write)
   {
   }



   /*******************************************************************************
    ** Tally decisions into a result.
    *******************************************************************************/
   private static class Counts
   {
      private final Map<Kind, Integer> counts = new java.util.EnumMap<>(Kind.class);



      /*******************************************************************************
       ** Count one decision.
       *******************************************************************************/
      void add(Decision decision)
      {
         counts.merge(decision.kind(), 1, Integer::sum);
      }



      /*******************************************************************************
       ** The public result shape.
       *******************************************************************************/
      Result result()
      {
         return (new Result(get(Kind.INSERT), get(Kind.UPDATE), get(Kind.UPSTREAM_CHANGED), get(Kind.RETIRE), get(Kind.DELETE), get(Kind.UNCHANGED)));
      }



      /*******************************************************************************
       ** A count, zero when absent.
       *******************************************************************************/
      private int get(Kind kind)
      {
         return (counts.getOrDefault(kind, 0));
      }
   }



   /*******************************************************************************
    ** Decide one key against its stored row (null when absent) and its winning,
    ** non-retired bundle entry (null when no bundle provides it any more). Rows are
    ** written only when their text, upstream state or owning bundle changes: a new
    ** bundle version with the same text is not a change, so it neither rewrites
    ** nor audits the row (bundleVersion records the version that last seeded it).
    *******************************************************************************/
   private Decision decide(QRecord old, WinningEntry winner)
   {
      if(old == null)
      {
         return (winner == null ? new Decision(Kind.UNCHANGED, null) : new Decision(Kind.INSERT,
            seed(new QRecord().withValue("key", winner.entry().getKey()).withValue("role", winner.entry().getRole())
               .withValue("source", winner.bundle().getLayer().name()), winner)));
      }
      if(!hasBundle(old))
      {
         return (new Decision(Kind.UNCHANGED, null));
      }
      boolean unedited = Objects.equals(HelpContentHash.of(old), old.getValueString("lastSeededHash"));
      if(winner == null)
      {
         if(unedited)
         {
            return (new Decision(Kind.DELETE, old));
         }
         if(!"RETIRED".equals(old.getValueString("upstreamStatus")))
         {
            return (new Decision(Kind.RETIRE, new QRecord(old).withValue("upstreamStatus", "RETIRED").withValue("upstreamSummary", null).withValue("upstreamContent", null)));
         }
         return (new Decision(Kind.UNCHANGED, null));
      }
      if(unedited)
      {
         QRecord next = seed(new QRecord(old), winner);
         return (changed(old, next) ? new Decision(Kind.UPDATE, next) : new Decision(Kind.UNCHANGED, null));
      }
      HelpBundleEntry entry = winner.entry();
      if(!Objects.equals(hash(entry), old.getValueString("lastSeededHash")))
      {
         QRecord next = new QRecord(old).withValue("upstreamStatus", "UPSTREAM_CHANGED")
            .withValue("upstreamSummary", entry.getSummary()).withValue("upstreamContent", entry.getContent())
            .withValue("bundleId", winner.bundle().getId()).withValue("bundleVersion", winner.bundle().getVersion());
         return (new Decision(Kind.UPSTREAM_CHANGED, changed(old, next) ? next : null));
      }
      return (new Decision(Kind.UNCHANGED, null));
   }



   /*******************************************************************************
    ** The merge key of a stored row.
    *******************************************************************************/
   private static EntryKey key(QRecord row)
   {
      return (new EntryKey(row.getValueString("key"), row.getValueString("role")));
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
         for(QRecord old : query(null))
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
            //////////////////////////////////////////////////////////////////////
            // The text is what the reviewer saw; a version bump with the same   //
            // text is not a change (sync no longer rewrites rows for it).       //
            //////////////////////////////////////////////////////////////////////
            if(!Objects.equals(old.getValueString("bundleId"), winner.bundle().getId())
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
      long count = query(null).stream().filter(row -> "UPSTREAM_CHANGED".equals(row.getValueString("upstreamStatus"))).count();
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
    ** Read using the caller's normal permissions: every row, or the rows for some keys.
    *******************************************************************************/
   private List<QRecord> query(List<String> keys) throws QException
   {
      List<QRecord> records = new ArrayList<>();
      while(true)
      {
         QQueryFilter filter = new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withSkip(records.size()).withLimit(BATCH_SIZE);
         if(keys != null)
         {
            filter.addCriteria(new QFilterCriteria("key", QCriteriaOperator.IN, keys));
         }
         List<QRecord> page = new QueryAction().execute(new QueryInput(HelpContent.TABLE_NAME).withShouldFetchHeavyFields(true)
            .withFilter(filter)).getRecords();
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
    ** Fields whose change is a real change to the row. bundleVersion is left out:
    ** it is recorded whenever the row is written, but never a reason to write.
    *******************************************************************************/
   private static final List<String> SIGNIFICANT = List.of("format", "summary", "content", "bundleId", "lastSeededHash",
      "upstreamStatus", "upstreamSummary", "upstreamContent");



   /*******************************************************************************
    ** Avoid no-op updates, timestamps and audit entries on repeated runs. Null and
    ** empty text compare equal, as they do in the seeded hash.
    *******************************************************************************/
   private boolean changed(QRecord before, QRecord after)
   {
      for(String field : SIGNIFICANT)
      {
         if(!Objects.equals(Objects.requireNonNullElse(before.getValueString(field), ""), Objects.requireNonNullElse(after.getValueString(field), "")))
         {
            return (true);
         }
      }
      return (false);
   }
}
