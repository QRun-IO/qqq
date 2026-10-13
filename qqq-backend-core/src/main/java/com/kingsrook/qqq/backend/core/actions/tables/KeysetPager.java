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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.RawReadReason;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;


/*******************************************************************************
 ** Read a table a page at a time using keyset ("seek") paging, rather than
 ** skip/limit:  each page is
 ** <pre>WHERE (filter) AND pk &gt; lastKey ORDER BY pk ASC LIMIT pageSize</pre>
 ** so the cost of a page stays flat no matter how deep into the table it is, and
 ** a run can be resumed from the last key it handled ({@link #startingAfter(Serializable)},
 ** {@link #getLastKey()}).
 **
 ** Works against any backend that supports criteria and ordering on the table's
 ** single primary key field.  Pass a transaction ({@link #withTransaction}) and
 ** every page is read inside it - e.g., inside a consistent-snapshot transaction.
 **
 ** Requirements and edge cases:
 ** <ul>
 ** <li>The primary key must be unique (it is, for a real primary key) and non-null.
 **   Rows with a null key can't be reached by a "key &gt; lastKey" criteria, so they
 **   are not returned.</li>
 ** <li>If the primary key's type is not orderable (e.g., a BLOB or boolean - rare),
 **   {@link #forEachPage} falls back to a single streamed query, chunked into pages
 **   of at most the page size.  That can't be resumed; and the iterator styles
 **   require an orderable key.</li>
 ** <li>The filter given may not have its own skip or limit, as those can't be
 **   combined with paging.  Its order-bys are replaced by the primary key order.</li>
 ** <li>Every page is fetched with the same QueryInput settings - use
 **   {@link #withQueryInputCustomizer} (or {@link #withRawRead}) to set things such
 **   as fieldNamesToInclude (which must include the primary key), join, or the
 **   heavy-field flag.</li>
 ** </ul>
 **
 ** Example:
 ** <pre>
 ** KeysetPager.forTable("person")
 **    .withFilter(new QQueryFilter(new QFilterCriteria("isEmployed", EQUALS, true)))
 **    .withPageSize(5000)
 **    .withTransaction(snapshotTransaction)
 **    .forEachPage(page -&gt; write(page));
 ** </pre>
 *******************************************************************************/
public class KeysetPager
{
   public static final int DEFAULT_PAGE_SIZE = 1000;

   private static final Set<QFieldType> ORDERABLE_PRIMARY_KEY_TYPES = Set.of(QFieldType.STRING, QFieldType.INTEGER, QFieldType.LONG, QFieldType.DECIMAL, QFieldType.DATE, QFieldType.DATE_TIME, QFieldType.TIME);

   private final String               tableName;
   private QQueryFilter               filter;
   private int                        pageSize = DEFAULT_PAGE_SIZE;
   private QBackendTransaction        transaction;
   private Serializable               lastKey;
   private Consumer<QueryInput>       queryInputCustomizer;
   private long                       pagesRead = 0;
   private long                       recordsRead = 0;



   /*******************************************************************************
    ** Constructor - see {@link #forTable(String)}
    *******************************************************************************/
   private KeysetPager(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Start building a pager for a table.
    *******************************************************************************/
   public static KeysetPager forTable(String tableName)
   {
      return (new KeysetPager(tableName));
   }



   /*******************************************************************************
    ** Only page through records matching this filter.
    *******************************************************************************/
   public KeysetPager withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** The maximum number of records per page.
    *******************************************************************************/
   public KeysetPager withPageSize(int pageSize)
   {
      if(pageSize < 1)
      {
         throw (new IllegalArgumentException("Page size must be at least 1"));
      }
      this.pageSize = pageSize;
      return (this);
   }



   /*******************************************************************************
    ** Run every page's query inside this transaction (e.g., a snapshot).  The
    ** pager neither commits nor closes it.
    *******************************************************************************/
   public KeysetPager withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /*******************************************************************************
    ** Resume:  only return records whose primary key is greater than this one
    ** (e.g., the {@link #getLastKey()} of an earlier, interrupted run).
    *******************************************************************************/
   public KeysetPager startingAfter(Serializable lastKey)
   {
      this.lastKey = lastKey;
      return (this);
   }



   /*******************************************************************************
    ** Adjust the QueryInput used for each page, e.g., fieldNamesToInclude, or
    ** shouldFetchHeavyFields.  Don't set the filter or transaction here.
    *******************************************************************************/
   public KeysetPager withQueryInputCustomizer(Consumer<QueryInput> queryInputCustomizer)
   {
      this.queryInputCustomizer = queryInputCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Read every page as a raw read - see {@link QueryInput#withRawRead(RawReadReason)}.
    ** The permission check and audit happen for each page's query.
    *******************************************************************************/
   public KeysetPager withRawRead(RawReadReason rawReadReason)
   {
      return (withQueryInputCustomizer(queryInput -> queryInput.withRawRead(rawReadReason)));
   }



   /*******************************************************************************
    ** The primary key of the last record of the last page that was fully handled
    ** (that is, that {@link #forEachPage}'s consumer returned from, or that the
    ** iterator moved past) - or the {@link #startingAfter} key, if no page has been
    ** handled yet.  Pass it to {@link #startingAfter} to resume after a failure.
    *******************************************************************************/
   public Serializable getLastKey()
   {
      return (lastKey);
   }



   /*******************************************************************************
    ** The number of pages (queries that returned records) read so far.
    *******************************************************************************/
   public long getPagesRead()
   {
      return (pagesRead);
   }



   /*******************************************************************************
    ** The number of records read so far.
    *******************************************************************************/
   public long getRecordsRead()
   {
      return (recordsRead);
   }



   /*******************************************************************************
    ** Is the table's primary key of a type that can be keyset-paged?
    *******************************************************************************/
   public boolean isPrimaryKeyOrderable() throws QException
   {
      return (getPrimaryKeyField().getType() != null && ORDERABLE_PRIMARY_KEY_TYPES.contains(getPrimaryKeyField().getType()));
   }



   /*******************************************************************************
    ** Run the consumer on each page, in primary key order, until the records are
    ** exhausted.  Pages are at most the page size; all but the last are full.
    *******************************************************************************/
   public void forEachPage(UnsafeConsumer<List<QRecord>, QException> consumer) throws QException
   {
      if(!isPrimaryKeyOrderable())
      {
         if(lastKey != null)
         {
            throw (new QException("Can't resume paging of table [" + tableName + "], because its primary key is not of an orderable type."));
         }
         streamInPages(consumer);
         return;
      }

      while(true)
      {
         List<QRecord> page = fetchNextPage();
         if(page.isEmpty())
         {
            return;
         }

         consumer.run(page);
         lastKey = keyOf(page.get(page.size() - 1));

         if(page.size() < pageSize)
         {
            return;
         }
      }
   }



   /*******************************************************************************
    ** Iterate over pages (lazily - each is queried as the iterator is advanced).
    ** The last key is updated when the iterator moves on from a page, to the next
    ** page or to the end.  A QException while querying is thrown as a QRuntimeException.
    *******************************************************************************/
   public Iterator<List<QRecord>> pageIterator() throws QException
   {
      if(!isPrimaryKeyOrderable())
      {
         throw (new QException("Can't iterate pages of table [" + tableName + "], because its primary key is not of an orderable type.  Use forEachPage, which falls back to a streamed query."));
      }

      return (new Iterator<>()
      {
         private List<QRecord> current = null;
         private List<QRecord> next    = null;
         private boolean       done    = false;



         /***************************************************************************
          **
          ***************************************************************************/
         @Override
         public boolean hasNext()
         {
            if(next != null)
            {
               return (true);
            }
            if(done)
            {
               return (false);
            }

            if(current != null)
            {
               lastKey = keyOf(current.get(current.size() - 1));
               if(current.size() < pageSize)
               {
                  done = true;
                  current = null;
                  return (false);
               }
            }

            try
            {
               List<QRecord> page = fetchNextPage();
               if(page.isEmpty())
               {
                  done = true;
                  current = null;
                  return (false);
               }
               next = page;
               return (true);
            }
            catch(QException e)
            {
               throw (new QRuntimeException("Error fetching a page of table [" + tableName + "]", e));
            }
         }



         /***************************************************************************
          **
          ***************************************************************************/
         @Override
         public List<QRecord> next()
         {
            if(!hasNext())
            {
               throw (new NoSuchElementException());
            }
            current = next;
            next = null;
            return (current);
         }
      });
   }



   /*******************************************************************************
    ** Iterate over individual records, fetching a page at a time.  See
    ** {@link #pageIterator()}.
    *******************************************************************************/
   public Iterator<QRecord> recordIterator() throws QException
   {
      Iterator<List<QRecord>> pages = pageIterator();
      return (new Iterator<>()
      {
         private Iterator<QRecord> currentPage = null;



         /***************************************************************************
          **
          ***************************************************************************/
         @Override
         public boolean hasNext()
         {
            while(currentPage == null || !currentPage.hasNext())
            {
               if(!pages.hasNext())
               {
                  return (false);
               }
               currentPage = pages.next().iterator();
            }
            return (true);
         }



         /***************************************************************************
          **
          ***************************************************************************/
         @Override
         public QRecord next()
         {
            if(!hasNext())
            {
               throw (new NoSuchElementException());
            }
            return (currentPage.next());
         }
      });
   }



   /*******************************************************************************
    ** Query the page after the current last key.
    *******************************************************************************/
   private List<QRecord> fetchNextPage() throws QException
   {
      String primaryKeyFieldName = getPrimaryKeyField().getName();

      QQueryFilter pageFilter = buildBaseFilter();
      if(lastKey != null)
      {
         pageFilter.addCriteria(new QFilterCriteria(primaryKeyFieldName, QCriteriaOperator.GREATER_THAN, lastKey));
      }
      pageFilter.setOrderBys(new ArrayList<>(List.of(new QFilterOrderBy(primaryKeyFieldName, true))));
      pageFilter.setLimit(pageSize);

      QueryInput queryInput = buildQueryInput(pageFilter);
      List<QRecord> records = new QueryAction().execute(queryInput).getRecords();
      if(records == null)
      {
         records = new ArrayList<>();
      }

      if(!records.isEmpty())
      {
         pagesRead++;
         recordsRead += records.size();
         keyOf(records.get(records.size() - 1));
      }
      return (records);
   }



   /*******************************************************************************
    ** A new top-level AND filter, containing the user's filter (if any) as a
    ** sub-filter, so the key criteria always combines with it by AND, even if the
    ** user's filter is an OR.
    *******************************************************************************/
   private QQueryFilter buildBaseFilter() throws QException
   {
      QQueryFilter base = new QQueryFilter();
      if(filter != null)
      {
         if(filter.getSkip() != null || filter.getLimit() != null)
         {
            throw (new QException("A filter with a skip or limit can't be used with KeysetPager."));
         }

         QQueryFilter userFilter = filter.clone();
         userFilter.setOrderBys(new ArrayList<>());
         if(!userFilter.hasAnyCriteria())
         {
            return (base);
         }
         base.addSubFilter(userFilter);
      }
      return (base);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput buildQueryInput(QQueryFilter queryFilter)
   {
      QueryInput queryInput = new QueryInput(tableName);
      queryInput.setFilter(queryFilter);
      queryInput.setTransaction(transaction);
      if(queryInputCustomizer != null)
      {
         queryInputCustomizer.accept(queryInput);
      }
      return (queryInput);
   }



   /*******************************************************************************
    ** Fallback for a non-orderable primary key:  one streamed query, in pages.
    *******************************************************************************/
   private void streamInPages(UnsafeConsumer<List<QRecord>, QException> consumer) throws QException
   {
      RecordPipe    recordPipe = new RecordPipe();
      List<QRecord> buffer     = new ArrayList<>();

      QQueryFilter streamFilter = buildBaseFilter();
      if(streamFilter.getSubFilters() != null && streamFilter.getSubFilters().size() == 1)
      {
         streamFilter = streamFilter.getSubFilters().get(0);
      }
      final QQueryFilter finalStreamFilter = streamFilter;

      new AsyncRecordPipeLoop().run("KeysetPager>stream>" + tableName, null, recordPipe, (callback) ->
      {
         QueryInput queryInput = buildQueryInput(finalStreamFilter);
         queryInput.setRecordPipe(recordPipe);
         return (new QueryAction().execute(queryInput));
      }, () ->
      {
         List<QRecord> records = recordPipe.consumeAvailableRecords();
         buffer.addAll(records);
         while(buffer.size() >= pageSize)
         {
            deliverStreamedPage(consumer, new ArrayList<>(buffer.subList(0, pageSize)));
            buffer.subList(0, pageSize).clear();
         }
         return (records.size());
      });

      if(!buffer.isEmpty())
      {
         deliverStreamedPage(consumer, new ArrayList<>(buffer));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void deliverStreamedPage(UnsafeConsumer<List<QRecord>, QException> consumer, List<QRecord> page) throws QException
   {
      pagesRead++;
      recordsRead += page.size();
      consumer.run(page);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable keyOf(QRecord record)
   {
      Serializable key = record.getValue(getPrimaryKeyFieldUnchecked().getName());
      if(key == null)
      {
         throw (new QRuntimeException("A record of table [" + tableName + "] was returned without its primary key value (is the primary key field in fieldNamesToInclude?), so paging can't continue."));
      }
      return (key);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QFieldMetaData getPrimaryKeyField() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QException("Table [" + tableName + "] was not found in the active QInstance"));
      }
      if(table.getPrimaryKeyField() == null)
      {
         throw (new QException("Table [" + tableName + "] has no primary key field, so KeysetPager can't be used with it."));
      }
      return (table.getField(table.getPrimaryKeyField()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QFieldMetaData getPrimaryKeyFieldUnchecked()
   {
      try
      {
         return (getPrimaryKeyField());
      }
      catch(QException e)
      {
         throw (new QRuntimeException(e.getMessage(), e));
      }
   }

}
