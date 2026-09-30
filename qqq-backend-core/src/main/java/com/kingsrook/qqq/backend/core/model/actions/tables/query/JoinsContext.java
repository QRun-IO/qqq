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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.metadata.JoinGraph;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.NullValueBehaviorUtil;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;
import org.apache.logging.log4j.Level;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Helper object used throughout query, count, aggregate, and reporting actions
 ** to assemble the full set of joins needed for a query, manage table aliases,
 ** and enforce record-level security.
 **
 ** <p><b>Overview:</b> When a backend action (e.g., {@code RDBMSQueryAction},
 ** {@code RDBMSCountAction}, {@code RDBMSAggregateAction}) needs to generate
 ** SQL (or an equivalent query for other backends), it constructs a
 ** {@code JoinsContext} from the main table name, the caller-supplied list of
 ** {@link QueryJoin} objects, and the {@link QQueryFilter}.  During construction,
 ** this class modifies the join list and the filter to include everything
 ** needed for a correct, secure query.</p>
 **
 ** <p><b>Initialization pipeline ({@code init()}):</b></p>
 ** <ol>
 **   <li><b>Process existing QueryJoins</b>: validates that each join's
 **       table exists, and populates the {@code aliasToTableNameMap} so that
 **       later steps can resolve aliases to real table names.</li>
 **   <li><b>{@code ensureFilterIsRepresented}</b>: scans all filter
 **       criteria, other-field-names, and order-bys for {@code table.field}
 **       references.  If a referenced table is not yet in the join list, a
 **       join is added for it (with metadata resolved from the instance).</li>
 **   <li><b>{@code ensureRecordSecurityLockIsRepresented}</b> (main table):
 **      for each read-scope {@link RecordSecurityLock} on the main
 **       table, ensures that any joins needed to reach the lock's security
 **       field are present, and builds security filter criteria.</li>
 **   <li><b>{@code fillInMissingJoinMetaData}</b>: for joins that were
 **       added without explicit {@link QJoinMetaData} (e.g., only a table
 **       name was specified), finds matching metadata from the
 **       {@link QInstance}'s joins.  Also resolves indirect/multi-hop joins
 **       via {@link ExposedJoin} paths on the main table.</li>
 **   <li><b>{@code ensureAllJoinRecordSecurityLocksAreRepresented}</b>:
 **       iterates over all joined tables and applies their security locks as
 **       well, potentially adding further implicit joins.</li>
 **   <li><b>{@code addSecurityFiltersToInputFilter}</b>: merges the
 **       accumulated security filter into the original input filter.  If the
 **       input filter uses {@code OR}, it is wrapped in a new {@code AND}
 **       alongside the security filter.</li>
 ** </ol>
 **
 ** <p><b>Alias management:</b> The internal {@code aliasToTableNameMap} maps
 ** both aliases and bare table names to actual table names.  It is populated
 ** as joins are processed.  Use {@link #resolveTableNameOrAliasToTableName}
 ** to convert any alias/name to the real table name.</p>
 **
 ** <p><b>Security lock pipeline:</b> When a {@link RecordSecurityLock} has a
 ** non-empty {@code joinNameChain}, the chain is walked (in reverse) to add
 ** {@link ImplicitQueryJoinForSecurityLock} joins as needed.  The resulting
 ** security criteria are placed either in the JOIN's {@code ON} clause (via
 ** {@link QueryJoin#withSecurityCriteria}) or in the {@code WHERE} clause,
 ** depending on whether the lock is part of an {@code OR} filter or applies
 ** to the main table directly.</p>
 **
 ** <p><b>Key consumers:</b> {@code AbstractRDBMSAction} (uses this context to
 ** build {@code FROM}, {@code WHERE}, {@code ORDER BY}, and {@code GROUP BY}
 ** clauses), {@code MemoryRecordStore}, and {@code GenerateReportAction}.</p>
 *******************************************************************************/
public class JoinsContext
{
   private static final QLogger LOG = QLogger.getLogger(JoinsContext.class);

   private final QInstance       instance;
   private final String          mainTableName;
   private final List<QueryJoin> queryJoins;

   private final Map<String, QTableMetaData> tables = new HashMap<>();
   private InputSource inputSource;

   private final QQueryFilter securityFilter;

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // pointer either at securityFilter, or at a sub-filter within it, for when we're doing a recursive build-out of multi-locks //
   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private QQueryFilter securityFilterCursor;

   ////////////////////////////////////////////////////////////////
   // note - will have entries for all tables, not just aliases. //
   ////////////////////////////////////////////////////////////////
   private final Map<String, String> aliasToTableNameMap = new HashMap<>();

   ////////////////////////////////////////////////////////////////////////////////////////
   // option (see constructor that takes it) to not expand the filter for security locks //
   ////////////////////////////////////////////////////////////////////////////////////////
   private boolean omitSecurity = false;

   /////////////////////////////////////////////////////////////////////////////
   // we will get a TON of more output if this gets turned up, so be cautious //
   /////////////////////////////////////////////////////////////////////////////
   private Level logLevel          = Level.OFF;
   private Level logLevelForFilter = Level.OFF;



   /*******************************************************************************
    ** Constructor that obtains the {@link QInstance} from {@link QContext}.
    **
    ** @param tableName  the main (root) table for the query.
    ** @param queryJoins caller-supplied list of joins; may be empty.  Additional
    **                   joins may be added during initialization.
    ** @param filter     the query filter; may be mutated to include security criteria.
    ** @throws QException if a referenced table or join cannot be resolved.
    *******************************************************************************/
   public JoinsContext(String tableName, List<QueryJoin> queryJoins, QQueryFilter filter) throws QException
   {
      this(QContext.getQInstance(), tableName, queryJoins, filter);
   }



   /*******************************************************************************
    ** Primary constructor.
    **
    ** @param instance   the QInstance containing table and join metadata.
    ** @param tableName  the main (root) table for the query.
    ** @param queryJoins caller-supplied list of joins; may be empty.  Additional
    **                   joins may be added during initialization.
    ** @param filter     the query filter; may be mutated to include security criteria.
    ** @throws QException if a referenced table or join cannot be resolved.
    *******************************************************************************/
   public JoinsContext(QInstance instance, String tableName, List<QueryJoin> queryJoins, QQueryFilter filter) throws QException
   {
      this(instance, tableName, queryJoins, filter, null);
   }



   /*******************************************************************************
    ** Keep the active root metadata and personalize joined tables using the same
    ** input source. The canonical instance still supplies the join registry.
    *******************************************************************************/
   public JoinsContext(QInstance instance, QueryOrCountInputInterface input, QQueryFilter filter) throws QException
   {
      this(instance, input.getTableName(), input.getQueryJoins(), filter, input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JoinsContext(QInstance instance, String tableName, List<QueryJoin> queryJoins, QQueryFilter filter, QueryOrCountInputInterface input) throws QException
   {
      this.instance = instance;
      this.mainTableName = tableName;
      if(input != null)
      {
         inputSource = input.getInputSource();
         tables.put(tableName, executionTable(input.getTable()));
      }

      /////////////////////////////////////////////////////////////////////////////
      // clone the incoming query joins - as this class will mutate them!  so in //
      // case they came from some meta-data, we don't want to change them there. //
      /////////////////////////////////////////////////////////////////////////////
      this.queryJoins = new ArrayList<>(CollectionUtils.nonNullList(queryJoins).stream().map(QueryJoin::clone).toList());

      this.securityFilter = new QQueryFilter();
      this.securityFilterCursor = this.securityFilter;

      init(instance, tableName, filter);
   }



   /*******************************************************************************
    ** Constructor that starts with an empty join list and optionally skips
    ** security-lock processing.  Obtains the {@link QInstance} from {@link QContext}.
    **
    ** @param tableName    the main (root) table for the query.
    ** @param filter       the query filter; may be mutated to include security criteria
    **                     (unless {@code omitSecurity} is {@code true}).
    ** @param omitSecurity if {@code true}, the initialization pipeline will not
    **                     process any {@link RecordSecurityLock}s or add security
    **                     filters.
    ** @throws QException if a referenced table or join cannot be resolved.
    *******************************************************************************/
   public JoinsContext(String tableName, QQueryFilter filter, boolean omitSecurity) throws QException
   {
      this.instance = QContext.getQInstance();
      this.mainTableName = tableName;
      this.queryJoins = new ArrayList<>();
      this.omitSecurity = omitSecurity;
      this.securityFilter = new QQueryFilter();
      this.securityFilterCursor = this.securityFilter;

      init(instance, tableName, filter);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void init(QInstance instance, String tableName, QQueryFilter filter) throws QException
   {
      // log("--- START ----------------------------------------------------------------------", logPair("mainTable", tableName));
      dumpDebug(true, false);

      for(QueryJoin queryJoin : this.queryJoins)
      {
         processQueryJoin(queryJoin);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure that all tables specified in filter columns are being brought into the query as joins //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      ensureFilterIsRepresented(filter);
      logFilter("After ensureFilterIsRepresented:", securityFilter);

      ///////////////////////////////////////////////////////////////////////////////////////
      // ensure that any record locks on the main table, which require a join, are present //
      ///////////////////////////////////////////////////////////////////////////////////////
      if(!omitSecurity)
      {
         MultiRecordSecurityLock multiRecordSecurityLock = RecordSecurityLockFilters.filterForReadLockTree(CollectionUtils.nonNullList(getTable(tableName).getRecordSecurityLocks()));
         for(RecordSecurityLock lock : multiRecordSecurityLock.getLocks())
         {
            ensureRecordSecurityLockIsRepresented(tableName, tableName, lock, null);
            logFilter("After ensureRecordSecurityLockIsRepresented[fieldName=" + lock.getFieldName() + "]:", securityFilter);
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////
      // make sure that all joins in the query have meta data specified                //
      // e.g., a user-added join may just specify the join-table                       //
      // or a join implicitly added from a filter may also not have its join meta data //
      ///////////////////////////////////////////////////////////////////////////////////
      fillInMissingJoinMetaData();
      logFilter("After fillInMissingJoinMetaData:", securityFilter);

      ///////////////////////////////////////////////////////////////
      // ensure any joins that contribute a recordLock are present //
      ///////////////////////////////////////////////////////////////
      if(!omitSecurity)
      {
         ensureAllJoinRecordSecurityLocksAreRepresented(instance);
         logFilter("After ensureAllJoinRecordSecurityLocksAreRepresented:", securityFilter);

         ////////////////////////////////////////////////////////////////////////////////////
         // if there were any security filters built, then put those into the input filter //
         ////////////////////////////////////////////////////////////////////////////////////
         addSecurityFiltersToInputFilter(filter);
      }

      log("Constructed JoinsContext", logPair("mainTableName", this.mainTableName), logPair("queryJoins", this.queryJoins.stream().map(qj -> qj.getJoinTable()).collect(Collectors.joining(","))));
      log("", logPair("securityFilter", securityFilter));
      log("", logPair("fullFilter", filter));
      dumpDebug(false, true);
      // log("--- END ------------------------------------------------------------------------");
   }



   /*******************************************************************************
    ** Update the input filter with any security filters that were built.
    *******************************************************************************/
   private void addSecurityFiltersToInputFilter(QQueryFilter filter)
   {
      ////////////////////////////////////////////////////////////////////////////////////
      // if there's no security filter criteria (including sub-filters), return w/ noop //
      ////////////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeIsEmpty(securityFilter.getSubFilters()))
      {
         return;
      }

      ///////////////////////////////////////////////////////////////////////
      // if the input filter is an OR we need to replace it with a new AND //
      ///////////////////////////////////////////////////////////////////////
      if(filter.getBooleanOperator().equals(QQueryFilter.BooleanOperator.OR))
      {
         List<QFilterCriteria> originalCriteria   = filter.getCriteria();
         List<QQueryFilter>    originalSubFilters = filter.getSubFilters();

         QQueryFilter replacementFilter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
         replacementFilter.setCriteria(originalCriteria);
         replacementFilter.setSubFilters(originalSubFilters);

         filter.setCriteria(new ArrayList<>());
         filter.setSubFilters(new ArrayList<>());
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.AND);
         filter.addSubFilter(replacementFilter);
      }

      filter.addSubFilter(securityFilter);
   }



   /*******************************************************************************
    ** In case we've added any joins to the query that have security locks which
    ** weren't previously added to the query, add them now.  basically, this is
    ** calling ensureRecordSecurityLockIsRepresented for each queryJoin.
    *******************************************************************************/
   private void ensureAllJoinRecordSecurityLocksAreRepresented(QInstance instance) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // avoid concurrent modification exceptions by doing a double-loop and breaking the inner any time anything gets added //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      Set<QueryJoin> processedQueryJoins   = new HashSet<>();
      boolean        addedAnyThisIteration = true;
      while(addedAnyThisIteration)
      {
         addedAnyThisIteration = false;

         for(QueryJoin queryJoin : this.queryJoins)
         {
            boolean addedAnyForThisJoin = false;

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // avoid double-processing the same query join                                                                                            //
            // or adding security filters for a join who was only added to the query so that we could add locks (an ImplicitQueryJoinForSecurityLock) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(processedQueryJoins.contains(queryJoin) || queryJoin instanceof ImplicitQueryJoinForSecurityLock)
            {
               continue;
            }
            processedQueryJoins.add(queryJoin);

            //////////////////////////////////////////////////////////////////////////////////////////
            // process all locks on this join's join-table.  keep track if any new joins were added //
            //////////////////////////////////////////////////////////////////////////////////////////
            QTableMetaData joinTable = getTable(queryJoin.getJoinTable());

            MultiRecordSecurityLock multiRecordSecurityLock = RecordSecurityLockFilters.filterForReadLockTree(CollectionUtils.nonNullList(joinTable.getRecordSecurityLocks()));
            for(RecordSecurityLock lock : multiRecordSecurityLock.getLocks())
            {
               List<QueryJoin> addedQueryJoins = ensureRecordSecurityLockIsRepresented(joinTable.getName(), queryJoin.getJoinTableOrItsAlias(), lock, queryJoin);

               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if any joins were added by this call, add them to the set of processed ones, so they don't get re-processed. //
               // also mark the flag that any were added for this join, to manage the double-looping                           //
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(CollectionUtils.nullSafeHasContents(addedQueryJoins))
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // make all new joins added in that method be of the same type (inner/left/etc) as the query join they are connected to //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  for(QueryJoin addedQueryJoin : addedQueryJoins)
                  {
                     addedQueryJoin.setType(queryJoin.getType());
                  }

                  processedQueryJoins.addAll(addedQueryJoins);
                  addedAnyForThisJoin = true;
               }
            }

            ///////////////////////////////////////////////////////////////////////////////////////////////
            // if any new joins were added, we need to break the inner-loop, and continue the outer loop //
            // e.g., to process the next query join (but we can't just go back to the foreach queryJoin, //
            // because it would fail with concurrent modification)                                       //
            ///////////////////////////////////////////////////////////////////////////////////////////////
            if(addedAnyForThisJoin)
            {
               addedAnyThisIteration = true;
               break;
            }
         }
      }
   }



   /*******************************************************************************
    ** For a given recordSecurityLock on a given table (with a possible alias),
    ** make sure that if any joins are needed to get to the lock, that they are in the query.
    **
    ** returns the list of query joins that were added, if any were added
    *******************************************************************************/
   private List<QueryJoin> ensureRecordSecurityLockIsRepresented(String tableName, String tableNameOrAlias, RecordSecurityLock recordSecurityLock, QueryJoin sourceQueryJoin) throws QException
   {
      List<QueryJoin> addedQueryJoins = new ArrayList<>();

      ////////////////////////////////////////////////////////////////////////////
      // if this lock is a multi-lock, then recursively process its child-locks //
      ////////////////////////////////////////////////////////////////////////////
      if(recordSecurityLock instanceof MultiRecordSecurityLock multiRecordSecurityLock)
      {
         log("Processing MultiRecordSecurityLock...");

         /////////////////////////////////////////////////////////////////////////////////////////////////////////
         // make a new level in the filter-tree - storing old cursor, and updating cursor to point at new level //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////
         QQueryFilter oldSecurityFilterCursor = this.securityFilterCursor;
         QQueryFilter nextLevelSecurityFilter = new QQueryFilter();
         this.securityFilterCursor.addSubFilter(nextLevelSecurityFilter);
         this.securityFilterCursor = nextLevelSecurityFilter;

         ///////////////////////////////////////
         // set the boolean operator to match //
         ///////////////////////////////////////
         nextLevelSecurityFilter.setBooleanOperator(multiRecordSecurityLock.getOperator().toFilterOperator());

         //////////////////////
         // process children //
         //////////////////////
         for(RecordSecurityLock childLock : CollectionUtils.nonNullList(multiRecordSecurityLock.getLocks()))
         {
            log(" - Recursive call for childLock: " + childLock);
            addedQueryJoins.addAll(ensureRecordSecurityLockIsRepresented(tableName, tableNameOrAlias, childLock, sourceQueryJoin));
         }

         ////////////////////
         // restore cursor //
         ////////////////////
         this.securityFilterCursor = oldSecurityFilterCursor;

         return addedQueryJoins;
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // A join name chain is going to look like this:                                                 //
      // for a table:  orderLineItemExtrinsic (that's 2 away from order, where its security field is): //
      // - securityFieldName = order.clientId                                                          //
      // - joinNameChain = orderJoinOrderLineItem, orderLineItemJoinOrderLineItemExtrinsic             //
      // so - to navigate from the table to the security field, we need to reverse the joinNameChain,  //
      // and step (via tmpTable variable) back to the securityField                                    //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      ArrayList<String> joinNameChain = new ArrayList<>(CollectionUtils.nonNullList(recordSecurityLock.getJoinNameChain()));
      Collections.reverse(joinNameChain);
      log("Evaluating recordSecurityLock.  Join name chain is of length: " + joinNameChain.size(), logPair("tableNameOrAlias", tableNameOrAlias), logPair("recordSecurityLock", recordSecurityLock.getFieldName()), logPair("joinNameChain", joinNameChain));

      QTableMetaData tmpTable                = getTable(tableName);
      String         securityFieldTableAlias = tableNameOrAlias;
      String         baseTableOrAlias        = tableNameOrAlias;

      boolean chainIsInner = true;
      if(sourceQueryJoin != null && QueryJoin.Type.isOuter(sourceQueryJoin.getType()))
      {
         chainIsInner = false;
      }

      for(String joinName : joinNameChain)
      {
         //////////////////////////////////////////////////////////////////////////////////////////////////
         // check the joins currently in the query - if any are THIS join, then we don't need to add one //
         //////////////////////////////////////////////////////////////////////////////////////////////////
         List<QueryJoin> matchingQueryJoins = this.queryJoins.stream().filter(queryJoin ->
         {
            QJoinMetaData joinMetaData = queryJoin.getJoinMetaData();
            return (joinMetaData != null && Objects.equals(joinMetaData.getName(), joinName));
         }).toList();

         if(CollectionUtils.nullSafeHasContents(matchingQueryJoins))
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // note - if a user added a join as an outer type, we need to change it to be inner, for the security purpose. //
            // todo - is this always right?  what about nulls-allowed?                                                     //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            log("- skipping join already in the query", logPair("joinName", joinName));

            QueryJoin matchedQueryJoin = matchingQueryJoins.get(0);

            if(matchedQueryJoin.getType().equals(QueryJoin.Type.LEFT) || matchedQueryJoin.getType().equals(QueryJoin.Type.RIGHT))
            {
               chainIsInner = false;
            }

            /* ?? todo ??
            if(matchedQueryJoin.getType().equals(QueryJoin.Type.LEFT) || matchedQueryJoin.getType().equals(QueryJoin.Type.RIGHT))
            {
               log("- - although... it was here as an outer - so switching it to INNER", logPair("joinName", joinName));
               matchedQueryJoin.setType(QueryJoin.Type.INNER);
            }
            */

            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // as we're walking from tmpTable to the table which ultimately has the security key field,         //
            // if the queryJoin we just found is joining out to tmpTable, then we need to advance tmpTable back //
            // to the queryJoin's base table - else, tmpTable advances to the matched queryJoin's joinTable     //
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            if(tmpTable.getName().equals(matchedQueryJoin.getJoinTable()))
            {
               securityFieldTableAlias = Objects.requireNonNullElse(matchedQueryJoin.getBaseTableOrAlias(), mainTableName);
            }
            else
            {
               securityFieldTableAlias = matchedQueryJoin.getJoinTableOrItsAlias();
            }
            tmpTable = getTable(aliasToTableNameMap.getOrDefault(securityFieldTableAlias, securityFieldTableAlias));

            ////////////////////////////////////////////////////////////////////////////////////////
            // set the baseTableOrAlias for the next iteration to be this join's joinTableOrAlias //
            ////////////////////////////////////////////////////////////////////////////////////////
            baseTableOrAlias = securityFieldTableAlias;

            continue;
         }

         QJoinMetaData join = instance.getJoin(joinName);
         if(join.getLeftTable().equals(tmpTable.getName()))
         {
            securityFieldTableAlias = join.getRightTable() + "_forSecurityJoin_" + join.getName();
            QueryJoin queryJoin = new ImplicitQueryJoinForSecurityLock()
               .withJoinMetaData(join)
               .withType(chainIsInner ? QueryJoin.Type.INNER : QueryJoin.Type.LEFT)
               .withBaseTableOrAlias(baseTableOrAlias)
               .withAlias(securityFieldTableAlias);

            if(securityFilterCursor.getBooleanOperator() == QQueryFilter.BooleanOperator.OR)
            {
               queryJoin.withType(QueryJoin.Type.LEFT);
               chainIsInner = false;
            }

            if(hasAllAccessKey(recordSecurityLock))
            {
               queryJoin.withType(QueryJoin.Type.LEFT);
               chainIsInner = false;
            }

            addQueryJoin(queryJoin, "forRecordSecurityLock (non-flipped)", "- ");
            addedQueryJoins.add(queryJoin);
            tmpTable = getTable(join.getRightTable());
         }
         else if(join.getRightTable().equals(tmpTable.getName()))
         {
            securityFieldTableAlias = join.getLeftTable() + "_forSecurityJoin_" + join.getName();
            QueryJoin queryJoin = new ImplicitQueryJoinForSecurityLock()
               .withJoinMetaData(join.flip())
               .withType(chainIsInner ? QueryJoin.Type.INNER : QueryJoin.Type.LEFT)
               .withBaseTableOrAlias(baseTableOrAlias)
               .withAlias(securityFieldTableAlias);

            if(securityFilterCursor.getBooleanOperator() == QQueryFilter.BooleanOperator.OR)
            {
               queryJoin.withType(QueryJoin.Type.LEFT);
               chainIsInner = false;
            }

            if(hasAllAccessKey(recordSecurityLock))
            {
               queryJoin.withType(QueryJoin.Type.LEFT);
               chainIsInner = false;
            }

            addQueryJoin(queryJoin, "forRecordSecurityLock (flipped)", "- ");
            addedQueryJoins.add(queryJoin);
            tmpTable = getTable(join.getLeftTable());
         }
         else
         {
            dumpDebug(false, true);
            throw (new QException("Error adding security lock joins to query - table name [" + tmpTable.getName() + "] not found in join [" + joinName + "]"));
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // for the next iteration of the loop, set the next join's baseTableOrAlias to be the alias we just created //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////
         baseTableOrAlias = securityFieldTableAlias;
      }

      ////////////////////////////////////////////////////////////////////////////////////
      // now that we know the joins/tables are in the query, add to the security filter //
      ////////////////////////////////////////////////////////////////////////////////////
      QueryJoin lastAddedQueryJoin = addedQueryJoins.isEmpty() ? null : addedQueryJoins.get(addedQueryJoins.size() - 1);
      if(sourceQueryJoin != null && lastAddedQueryJoin == null)
      {
         lastAddedQueryJoin = sourceQueryJoin;
      }
      addSubFilterForRecordSecurityLock(recordSecurityLock, tmpTable, securityFieldTableAlias, !chainIsInner, lastAddedQueryJoin);

      log("Finished evaluating recordSecurityLock");

      return (addedQueryJoins);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private boolean hasAllAccessKey(RecordSecurityLock recordSecurityLock)
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // check if the key type has an all-access key, and if so, if it's set to true for the current user/session //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QSecurityKeyType securityKeyType = instance.getSecurityKeyType(recordSecurityLock.getSecurityKeyType());
      if(StringUtils.hasContent(securityKeyType.getAllAccessKeyName()))
      {
         QSession session = QContext.getQSession();
         if(session.hasSecurityKeyValue(securityKeyType.getAllAccessKeyName(), true, QFieldType.BOOLEAN))
         {
            return (true);
         }
      }

      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addSubFilterForRecordSecurityLock(RecordSecurityLock recordSecurityLock, QTableMetaData table, String tableNameOrAlias, boolean isOuter, QueryJoin sourceQueryJoin)
   {
      boolean haveAllAccessKey = hasAllAccessKey(recordSecurityLock);
      if(haveAllAccessKey)
      {
         ////////////////////////////////////////////////////////////////////////////////////////
         // if we're in an AND filter, then we don't need a criteria for this lock, so return. //
         ////////////////////////////////////////////////////////////////////////////////////////
         boolean inAnAndFilter = securityFilterCursor.getBooleanOperator() == QQueryFilter.BooleanOperator.AND;
         if(inAnAndFilter)
         {
            return;
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////
      // for locks w/o a join chain, the lock fieldName will simply be a field on the table. //
      // so just prepend that with the tableNameOrAlias.                                     //
      /////////////////////////////////////////////////////////////////////////////////////////
      String fieldName = tableNameOrAlias + "." + recordSecurityLock.getFieldName();
      if(CollectionUtils.nullSafeHasContents(recordSecurityLock.getJoinNameChain()))
      {
         /////////////////////////////////////////////////////////////////////////////////
         // else, expect a "table.field" in the lock fieldName - but we want to replace //
         // the table name part with a possible alias that we took in.                  //
         /////////////////////////////////////////////////////////////////////////////////
         String[] parts = recordSecurityLock.getFieldName().split("\\.");
         if(parts.length != 2)
         {
            dumpDebug(false, true);
            throw new IllegalArgumentException("Mal-formatted recordSecurityLock fieldName for lock with joinNameChain in query: " + fieldName);
         }
         fieldName = tableNameOrAlias + "." + parts[1];
      }

      ///////////////////////////////////////////////////////////////////////////////////////////
      // else - get the key values from the session and decide what kind of criterion to build //
      ///////////////////////////////////////////////////////////////////////////////////////////
      QQueryFilter          lockFilter   = new QQueryFilter();
      List<QFilterCriteria> lockCriteria = new ArrayList<>();
      lockFilter.setCriteria(lockCriteria);

      QFieldType type = QFieldType.INTEGER;
      try
      {
         JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = getFieldAndTableNameOrAlias(fieldName);
         type = fieldAndTableNameOrAlias.field().getType();
      }
      catch(Exception e)
      {
         LOG.debug("Error getting field type...  Trying Integer", e);
      }

      if(haveAllAccessKey)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////
         // if we have an all access key (but we got here because we're part of an OR query), then //
         // write a criterion that will always be true - e.g., field=field                         //
         ////////////////////////////////////////////////////////////////////////////////////////////
         lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.TRUE));
      }
      else
      {
         List<Serializable> securityKeyValues = QContext.getQSession().getSecurityKeyValues(recordSecurityLock.getSecurityKeyType(), type);
         if(CollectionUtils.nullSafeIsEmpty(securityKeyValues))
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // handle user with no values -- they can only see null values, and only iff the lock's null-value behavior is ALLOW //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(RecordSecurityLock.NullValueBehavior.ALLOW.equals(NullValueBehaviorUtil.getEffectiveNullValueBehavior(recordSecurityLock)))
            {
               lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IS_BLANK));
            }
            else
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // else, if no user/session values, and null-value behavior is deny, then setup a FALSE condition, to allow no rows.         //
               // todo - maybe avoid running the whole query - as you're not allowed ANY records (based on boolean tree down to this point) //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.FALSE));
            }
         }
         else
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // else, if user/session has some values, build an IN rule -                                                //
            // noting that if the lock's null-value behavior is ALLOW, then we actually want IS_NULL_OR_IN, not just IN //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(RecordSecurityLock.NullValueBehavior.ALLOW.equals(NullValueBehaviorUtil.getEffectiveNullValueBehavior(recordSecurityLock)))
            {
               lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IS_NULL_OR_IN, securityKeyValues));
            }
            else
            {
               lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IN, securityKeyValues));
            }
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there's a sourceQueryJoin, then set the lockCriteria on that join - so it gets written into the JOIN ... ON clause //
      // ... unless we're writing an OR filter.  then we need the condition in the WHERE clause                                //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      boolean doNotPutCriteriaInJoinOn = securityFilterCursor.getBooleanOperator() == QQueryFilter.BooleanOperator.OR;
      if(sourceQueryJoin != null && !doNotPutCriteriaInJoinOn)
      {
         sourceQueryJoin.withSecurityCriteria(lockCriteria);
      }
      else
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // we used to add an OR IS NULL for cases of an outer-join - but instead, this is now handled by putting the lockCriteria //
         // into the join (see above) - so this check is probably deprecated.                                                      //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         /*
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if this field is on the outer side of an outer join, then if we do a straight filter on it, then we're basically      //
            // nullifying the outer join... so for an outer join use-case, OR the security field criteria with a primary-key IS NULL //
            // which will make missing rows from the join be found.                                                                  //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(isOuter)
            {
               if(table == null)
               {
                  table = QContext.getQInstance().getTable(aliasToTableNameMap.get(tableNameOrAlias));
               }

               lockFilter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
               lockFilter.addCriteria(new QFilterCriteria(tableNameOrAlias + "." + table.getPrimaryKeyField(), QCriteriaOperator.IS_BLANK));
            }
         */

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // If this filter isn't for a queryJoin, then just add it to the main list of security sub-filters //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         this.securityFilterCursor.addSubFilter(lockFilter);
      }
   }



   /*******************************************************************************
    ** Add a query join to the list of query joins, and "process it"
    **
    ** use this method to add to the list, instead of ever adding directly, as it's
    ** important do to that process step (and we've had bugs when it wasn't done).
    *******************************************************************************/
   private void addQueryJoin(QueryJoin queryJoin, String reason, String logPrefix) throws QException
   {
      log(Objects.requireNonNullElse(logPrefix, "") + "Adding query join to context",
         logPair("reason", reason),
         logPair("joinTable", queryJoin.getJoinTable()),
         logPair("joinMetaData.name", () -> queryJoin.getJoinMetaData().getName()),
         logPair("joinMetaData.leftTable", () -> queryJoin.getJoinMetaData().getLeftTable()),
         logPair("joinMetaData.rightTable", () -> queryJoin.getJoinMetaData().getRightTable())
      );
      this.queryJoins.add(queryJoin);
      processQueryJoin(queryJoin);
      dumpDebug(false, false);
   }



   /*******************************************************************************
    ** If there are any joins in the context that don't have a join meta data, see
    ** if we can find the JoinMetaData to use for them by looking at all joins in the
    ** instance, or at the main table's exposed joins, and using their join paths.
    *******************************************************************************/
   private void fillInMissingJoinMetaData() throws QException
   {
      log("Begin adding missing join meta data");

      ////////////////////////////////////////////////////////////////////////////////
      // do a double-loop, to avoid concurrent modification on the queryJoins list. //
      // that is to say, we'll loop over that list, but possibly add things to it,  //
      // in which case we'll set this flag, and break the inner loop, to go again.  //
      ////////////////////////////////////////////////////////////////////////////////
      Set<QueryJoin> processedQueryJoins = new HashSet<>();
      boolean        addedJoin;
      do
      {
         addedJoin = false;
         for(QueryJoin queryJoin : queryJoins)
         {
            if(processedQueryJoins.contains(queryJoin))
            {
               continue;
            }
            processedQueryJoins.add(queryJoin);

            ///////////////////////////////////////////////////////////////////////////////////////////////
            // if the join has joinMetaData, then we don't need to process it... unless it needs flipped //
            ///////////////////////////////////////////////////////////////////////////////////////////////
            QJoinMetaData joinMetaData = queryJoin.getJoinMetaData();
            if(joinMetaData != null)
            {
               log("- QueryJoin already has joinMetaData", logPair("joinMetaDataName", joinMetaData.getName()));

               boolean isJoinLeftTableInQuery = false;
               String  joinMetaDataLeftTable  = joinMetaData.getLeftTable();
               if(joinMetaDataLeftTable.equals(mainTableName))
               {
                  isJoinLeftTableInQuery = true;
               }

               ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // check the other joins in this query - if any of them have this join's left-table as their baseTable, then set the flag to true //
               // also check if any other join's join-table resolves to this left table (handles aliased joins, e.g., security joins that bring  //
               // a table into the query under an alias like "orderLine_forSecurityJoin_lineItemLineItemExtrinsic")                              //
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               for(QueryJoin otherJoin : queryJoins)
               {
                  if(otherJoin == queryJoin)
                  {
                     continue;
                  }

                  if(Objects.equals(otherJoin.getBaseTableOrAlias(), joinMetaDataLeftTable))
                  {
                     isJoinLeftTableInQuery = true;
                     break;
                  }

                  String otherJoinTableName = resolveTableNameOrAliasToTableName(otherJoin.getJoinTableOrItsAlias());
                  if(Objects.equals(otherJoinTableName, joinMetaDataLeftTable))
                  {
                     isJoinLeftTableInQuery = true;
                     break;
                  }
               }

               /////////////////////////////////////////////////////////////////////////////////
               // if the join's left-table isn't in the query, then we need to flip the join. //
               /////////////////////////////////////////////////////////////////////////////////
               if(!isJoinLeftTableInQuery)
               {
                  log("- - Flipping queryJoin because its leftTable wasn't found in the query", logPair("joinMetaDataName", joinMetaData.getName()), logPair("leftTable", joinMetaDataLeftTable));
                  queryJoin.setJoinMetaData(joinMetaData.flip());
               }
            }
            else
            {
               //////////////////////////////////////////////////////////////////////
               // try to find a direct join between the main table and this table. //
               // if one is found, then put it (the meta data) on the query join.  //
               //////////////////////////////////////////////////////////////////////
               log("- QueryJoin doesn't have metaData - looking for it", logPair("joinTableOrItsAlias", queryJoin.getJoinTableOrItsAlias()));

               String        baseTableName = Objects.requireNonNullElse(resolveTableNameOrAliasToTableName(queryJoin.getBaseTableOrAlias()), mainTableName);
               QJoinMetaData found         = findJoinMetaData(baseTableName, queryJoin.getJoinTable(), true);
               if(found != null)
               {
                  log("- - Found joinMetaData - setting it in queryJoin", logPair("joinMetaDataName", found.getName()), logPair("baseTableName", baseTableName), logPair("joinTable", queryJoin.getJoinTable()));
                  queryJoin.setJoinMetaData(found);
               }
               else
               {
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // else, the join must be indirect - so look for an exposedJoin that will have a joinPath that will connect us //
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  QTableMetaData mainTable          = getTable(mainTableName);
                  boolean        addedAnyQueryJoins = false;
                  boolean        foundJoinMetaData  = false;

                  List<JoinTableAndPath> joinTablesAndPaths = getJoinTablesAndPaths(mainTable, true);
                  for(JoinTableAndPath joinTableAndPath : joinTablesAndPaths)
                  {
                     if(queryJoin.getJoinTable().equals(joinTableAndPath.joinTable()))
                     {
                        log("- - Found an exposed join", logPair("mainTable", mainTableName), logPair("joinTable", queryJoin.getJoinTable()), logPair("joinPath", joinTableAndPath.joinPath()));

                        /////////////////////////////////////////////////////////////////////////////////////
                        // loop backward through the join path (from the joinTable back to the main table) //
                        // adding joins to the table (if they aren't already in the query)                 //
                        /////////////////////////////////////////////////////////////////////////////////////
                        String tmpTable = queryJoin.getJoinTable();
                        for(int i = joinTableAndPath.joinPath().size() - 1; i >= 0; i--)
                        {
                           String joinName = joinTableAndPath.joinPath().get(i);
                           QJoinMetaData joinToAdd = instance.getJoin(joinName);
                           log("- - - evaluating joinPath element", logPair("i", i), logPair("joinName", joinName));

                           /////////////////////////////////////////////////////////////////////////////
                           // get the name from the opposite side of the join (flipping it if needed) //
                           /////////////////////////////////////////////////////////////////////////////
                           String nextTable;
                           if(joinToAdd.getRightTable().equals(tmpTable))
                           {
                              nextTable = joinToAdd.getLeftTable();
                           }
                           else
                           {
                              nextTable = joinToAdd.getRightTable();
                              joinToAdd = joinToAdd.flip();
                           }

                           if(doesJoinNeedAddedToQuery(joinName))
                           {
                              ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                              // if this is the last element in the joinPath, then we want to set this joinMetaData on the outer queryJoin //
                              // - else, we need to add a new queryJoin to this context                                                    //
                              ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                              if(i == joinTableAndPath.joinPath().size() - 1)
                              {
                                 if(queryJoin.getBaseTableOrAlias() == null)
                                 {
                                    queryJoin.setBaseTableOrAlias(nextTable);
                                 }
                                 queryJoin.setJoinMetaData(joinToAdd);
                                 log("- - - - this is the last element in the join path, so setting this joinMetaData on the original queryJoin");
                              }
                              else
                              {
                                 QueryJoin queryJoinToAdd = makeQueryJoinFromJoinAndTableNames(nextTable, tmpTable, joinToAdd);
                                 queryJoinToAdd.setType(queryJoin.getType());
                                 addedAnyQueryJoins = true;
                                 log("- - - - this is not the last element in the join path, so adding a new query join:");
                                 addQueryJoin(queryJoinToAdd, "forExposedJoin", "- - - - - - ");
                                 dumpDebug(false, false);
                              }
                           }
                           else
                           {
                              log("- - - - join doesn't need added to the query");
                           }

                           tmpTable = nextTable;
                        }

                        ////////////////////////////////////////////////////////////////
                        // break the loop over exposed joins - and mark that we found //
                        // join meta data for this query join that was missing it.    //
                        ////////////////////////////////////////////////////////////////
                        foundJoinMetaData = true;
                        break;
                     }
                  }

                  if(!foundJoinMetaData)
                  {
                     LOG.warn("Unable to find joinMetaData for queryJoin", logPair("mainTable", mainTable), logPair("queryJoin", queryJoin));
                  }

                  ///////////////////////////////////////////////////////////////////////////////////////////////////
                  // break the inner loop (it would fail due to a concurrent modification), but continue the outer //
                  ///////////////////////////////////////////////////////////////////////////////////////////////////
                  if(addedAnyQueryJoins)
                  {
                     addedJoin = true;
                     break;
                  }
               }
            }
         }
      }
      while(addedJoin);

      log("Done adding missing join meta data");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean doesJoinNeedAddedToQuery(String joinName)
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // look at all queryJoins already in context - if any have this join's name, and aren't implicit-security joins, then we don't need this join... //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(QueryJoin queryJoin : queryJoins)
      {
         if(queryJoin.getJoinMetaData() != null && queryJoin.getJoinMetaData().getName().equals(joinName) && !(queryJoin instanceof ImplicitQueryJoinForSecurityLock))
         {
            return (false);
         }
      }

      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void processQueryJoin(QueryJoin queryJoin) throws QException
   {
      String joinTableName = queryJoin.getJoinTable();
      if(inputSource != null && !tables.containsKey(joinTableName))
      {
         QTableMetaData table = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput()
            .withTableMetaData(instance.getTable(joinTableName)).withInputSource(inputSource));
         tables.put(joinTableName, executionTable(table));
      }

      QTableMetaData joinTable = getTable(joinTableName);
      if(joinTable == null)
      {
         throw (new QException("Unrecognized name for join table: " + queryJoin.getJoinTable()));
      }

      String tableNameOrAlias = queryJoin.getJoinTableOrItsAlias();
      if(tableNameOrAlias.equals(mainTableName) || aliasToTableNameMap.containsKey(tableNameOrAlias))
      {
         dumpDebug(false, true);
         throw (new QException("Duplicate table name or alias: " + tableNameOrAlias));
      }
      aliasToTableNameMap.put(tableNameOrAlias, joinTable.getName());
   }



   /*******************************************************************************
    ** Public selection is validated against active metadata. Native joins and
    ** READ locks can still require physical fields removed from that public view.
    *******************************************************************************/
   private QTableMetaData executionTable(QTableMetaData activeTable)
   {
      if(activeTable == null)
      {
         return null;
      }
      QTableMetaData canonicalTable = instance.getTable(activeTable.getName());
      if(canonicalTable != null && !activeTable.getFields().keySet().containsAll(canonicalTable.getFields().keySet()))
      {
         activeTable = activeTable.clone();
         for(QFieldMetaData field : canonicalTable.getFields().values())
         {
            if(!activeTable.getFields().containsKey(field.getName()))
            {
               activeTable.addField(field.clone());
            }
         }
      }
      return activeTable;
   }



   /*******************************************************************************
    ** Metadata resolved for this query. Joined tables are personalized once when
    ** they enter the context; legacy constructors retain canonical metadata.
    *******************************************************************************/
   public QTableMetaData getTable(String tableName)
   {
      return tables.containsKey(tableName) ? tables.get(tableName) : instance.getTable(tableName);
   }



   /*******************************************************************************
    ** Returns the live list of {@link QueryJoin} objects in this context.  This
    ** includes both caller-supplied joins and any that were added automatically
    ** (for filters, security locks, or exposed-join paths).
    **
    ** @return the mutable list of query joins.
    *******************************************************************************/
   public List<QueryJoin> getQueryJoins()
   {
      return queryJoins;
   }



   /*******************************************************************************
    ** Resolves a name (which may be an alias or an actual table name) to the
    ** real table name that can be passed to {@code qInstance.getTable()}.
    ** If the name is not found in the alias map, it is returned as-is.
    **
    ** @param nameOrAlias  an alias or table name present in this context.
    ** @return the actual table name.
    *******************************************************************************/
   public String resolveTableNameOrAliasToTableName(String nameOrAlias)
   {
      if(aliasToTableNameMap.containsKey(nameOrAlias))
      {
         return (aliasToTableNameMap.get(nameOrAlias));
      }
      return (nameOrAlias);
   }



   /*******************************************************************************
    ** Parses a field reference (optionally prefixed with {@code tableOrAlias.})
    ** and returns the resolved {@link QFieldMetaData} together with the table
    ** name or alias it belongs to.
    **
    ** <p>If {@code fieldName} contains a dot (e.g., {@code "department.name"}),
    ** the part before the dot is treated as a table name or alias and resolved
    ** via the alias map.  If there is no dot, the field is looked up on the main
    ** table.</p>
    **
    ** @param fieldName  a field name, optionally qualified as {@code table.field}.
    ** @return a {@link FieldAndTableNameOrAlias} record.
    ** @throws IllegalArgumentException if the name has more than one dot, or the
    **         table/field cannot be found.
    *******************************************************************************/
   public FieldAndTableNameOrAlias getFieldAndTableNameOrAlias(String fieldName)
   {
      return getFieldAndTableNameOrAlias(fieldName, false);
   }



   /***************************************************************************
    ** Parses a field reference (optionally prefixed with {@code tableOrAlias.})
    ** and returns the resolved {@link QFieldMetaData} together with the table
    ** name or alias it belongs to.
    **
    ** <p>If {@code fieldName} contains a dot (e.g., {@code "department.name"}),
    ** the part before the dot is treated as a table name or alias and resolved
    ** via the alias map.  If there is no dot, the field is looked up on the main
    ** table.</p>
    **
    ** @param fieldName  a field name, optionally qualified as {@code table.field}.
    ** @param allowVirtualFields  whether to allow virtual fields in the lookup
    ** @return a {@link FieldAndTableNameOrAlias} record.
    ** @throws IllegalArgumentException if the name has more than one dot, or the
    **         table/field cannot be found.
    ***************************************************************************/
   public FieldAndTableNameOrAlias getFieldAndTableNameOrAlias(String fieldName, boolean allowVirtualFields)
   {
      if(fieldName.contains("."))
      {
         String[] parts = fieldName.split("\\.");
         if(parts.length != 2)
         {
            dumpDebug(false, true);
            throw new IllegalArgumentException("Mal-formatted field name in query: " + fieldName);
         }

         String tableOrAlias  = parts[0];
         String baseFieldName = parts[1];
         String tableName     = resolveTableNameOrAliasToTableName(tableOrAlias);

         QTableMetaData table = getTable(tableName);
         if(table == null)
         {
            dumpDebug(false, true);
            throw new IllegalArgumentException("Could not find table [" + tableName + "] in instance for query");
         }

         return getFieldAndTableNameOrAlias(baseFieldName, table, tableOrAlias, allowVirtualFields);
      }

      return getFieldAndTableNameOrAlias(fieldName, getTable(mainTableName), mainTableName, allowVirtualFields);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private FieldAndTableNameOrAlias getFieldAndTableNameOrAlias(String fieldName, QTableMetaData tableMetaData, String tableNameOrAlias, boolean allowVirtualFields)
   {
      QFieldMetaData field;

      try
      {
         field = tableMetaData.getField(fieldName);
      }
      catch(IllegalArgumentException e)
      {
         if(allowVirtualFields)
         {
            field = tableMetaData.getVirtualField(fieldName);
            if(field == null)
            {
               throw e;
            }
         }
         else
         {
            throw e;
         }
      }

      return new FieldAndTableNameOrAlias(field, tableNameOrAlias);
   }



   /*******************************************************************************
    ** Check if the given table name exists in the query - but that name may NOT
    ** be an alias - it must be an actual table name.
    **
    ** e.g., Given:
    **   FROM `order` INNER JOIN line_item li
    ** hasTable("order") => true
    ** hasTable("li") => false
    ** hasTable("line_item") => true
    *******************************************************************************/
   public boolean hasTable(String table)
   {
      return (mainTableName.equals(table) || aliasToTableNameMap.containsValue(table));
   }



   /*******************************************************************************
    ** Check if the given tableOrAlias exists in the query - but note, if a table
    ** is in the query, but with an alias, then it would not be found by this method.
    **
    ** e.g., Given:
    **   FROM `order` INNER JOIN line_item li
    ** hasAliasOrTable("order") => false
    ** hasAliasOrTable("li") => true
    ** hasAliasOrTable("line_item") => false
    *******************************************************************************/
   public boolean hasAliasOrTable(String tableOrAlias)
   {
      return (mainTableName.equals(tableOrAlias) || aliasToTableNameMap.containsKey(tableOrAlias));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void ensureFilterIsRepresented(QQueryFilter filter) throws QException
   {
      Set<String> filterTables = new HashSet<>();
      populateFilterTablesSet(filter, filterTables);

      for(String filterTable : filterTables)
      {
         log("Evaluating filter", logPair("filterTable", filterTable));
         if(!aliasToTableNameMap.containsKey(filterTable) && !Objects.equals(mainTableName, filterTable))
         {
            log("- table not in query - adding a join for it", logPair("filterTable", filterTable));
            boolean found = false;
            for(QJoinMetaData join : CollectionUtils.nonNullMap(QContext.getQInstance().getJoins()).values())
            {
               QueryJoin queryJoin = makeQueryJoinFromJoinAndTableNames(mainTableName, filterTable, join);
               if(queryJoin != null)
               {
                  addQueryJoin(queryJoin, "forFilter (join found in instance)", "- - ");
                  found = true;
                  break;
               }
            }

            if(!found)
            {
               QueryJoin queryJoin = new QueryJoin().withJoinTable(filterTable).withType(QueryJoin.Type.INNER);
               addQueryJoin(queryJoin, "forFilter (join not found in instance)", "- - ");
            }
         }
         else
         {
            log("- table is already in query - not adding any joins", logPair("filterTable", filterTable));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryJoin makeQueryJoinFromJoinAndTableNames(String tableA, String tableB, QJoinMetaData join)
   {
      QueryJoin queryJoin = null;
      if(join.getLeftTable().equals(tableA) && join.getRightTable().equals(tableB))
      {
         queryJoin = new QueryJoin().withJoinMetaData(join).withType(QueryJoin.Type.INNER);
      }
      else
      {
         join = join.flip();
         if(join.getLeftTable().equals(tableA) && join.getRightTable().equals(tableB))
         {
            queryJoin = new QueryJoin().withJoinMetaData(join).withType(QueryJoin.Type.INNER);
         }
      }
      return queryJoin;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void populateFilterTablesSet(QQueryFilter filter, Set<String> filterTables)
   {
      if(filter != null)
      {
         for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
         {
            getTableNameFromFieldNameAndAddToSet(criteria.getFieldName(), filterTables);
            getTableNameFromFieldNameAndAddToSet(criteria.getOtherFieldName(), filterTables);
         }

         for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(filter.getOrderBys()))
         {
            getTableNameFromFieldNameAndAddToSet(orderBy.getFieldName(), filterTables);
         }

         for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
         {
            populateFilterTablesSet(subFilter, filterTables);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void getTableNameFromFieldNameAndAddToSet(String fieldName, Set<String> filterTables)
   {
      if(fieldName != null && fieldName.contains("."))
      {
         String tableName = fieldName.replaceFirst("\\..*", "");
         filterTables.add(tableName);
      }
   }



   /*******************************************************************************
    ** Searches the {@link QInstance}'s joins for a {@link QJoinMetaData} that
    ** connects {@code baseTableName} to {@code joinTableName} (checking both
    ** directions and flipping if needed).
    **
    ** <p>If multiple matches are found and {@code useExposedJoins} is
    ** {@code true}, the main table's {@link ExposedJoin} list is consulted to
    ** disambiguate.  If disambiguation fails, a {@link RuntimeException} is
    ** thrown.</p>
    **
    ** @param baseTableName   the "left" / base table (may be {@code null} to
    **                        match any table already in this context).
    ** @param joinTableName   the "right" / target table.
    ** @param useExposedJoins whether to use exposed joins for disambiguation
    **                        when multiple matches are found.
    ** @return the matching {@link QJoinMetaData}, or {@code null} if none found.
    ** @throws RuntimeException if more than one join matches and disambiguation
    **         fails.
    *******************************************************************************/
   public QJoinMetaData findJoinMetaData(String baseTableName, String joinTableName, boolean useExposedJoins)
   {
      List<QJoinMetaData> matches = new ArrayList<>();
      if(baseTableName != null)
      {
         ///////////////////////////////////////////////////////////////////////////
         // if query specified a left-table, look for a join between left & right //
         ///////////////////////////////////////////////////////////////////////////
         for(QJoinMetaData join : instance.getJoins().values())
         {
            if(join.getLeftTable().equals(baseTableName) && join.getRightTable().equals(joinTableName))
            {
               matches.add(join);
            }

            //////////////////////////////
            // look in both directions! //
            //////////////////////////////
            if(join.getRightTable().equals(baseTableName) && join.getLeftTable().equals(joinTableName))
            {
               matches.add(join.flip());
            }
         }
      }
      else
      {
         /////////////////////////////////////////////////////////////////////////////////////
         // if query didn't specify a left-table, then look for any join to the right table //
         /////////////////////////////////////////////////////////////////////////////////////
         for(QJoinMetaData join : instance.getJoins().values())
         {
            if(join.getRightTable().equals(joinTableName) && this.hasTable(join.getLeftTable()))
            {
               matches.add(join);
            }

            //////////////////////////////
            // look in both directions! //
            //////////////////////////////
            if(join.getLeftTable().equals(joinTableName) && this.hasTable(join.getRightTable()))
            {
               matches.add(join.flip());
            }
         }
      }

      if(matches.size() == 1)
      {
         return (matches.get(0));
      }
      else if(matches.size() > 1)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // if we found more than one join, but we're allowed to useExposedJoins, then //
         // see if we can tell which match to used based on the table's exposed joins  //
         ////////////////////////////////////////////////////////////////////////////////
         if(useExposedJoins)
         {
            QTableMetaData mainTable = getTable(mainTableName);
            for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(mainTable.getExposedJoins()))
            {
               if(exposedJoin.getJoinTable().equals(joinTableName))
               {
                  // todo ... is it wrong to always use 0??
                  return instance.getJoin(exposedJoin.getJoinPath().get(0));
               }
            }
         }

         ///////////////////////////////////////////////
         // if we couldn't figure it out, then throw. //
         ///////////////////////////////////////////////
         dumpDebug(false, true);
         throw (new RuntimeException("More than 1 join was found between [" + baseTableName + "] and [" + joinTableName + "] "
            + (useExposedJoins ? "(and exposed joins didn't clarify which one to use). " : "") + "Specify which one in your QueryJoin."));
      }

      return (null);
   }



   /*******************************************************************************
    ** Record pairing a resolved {@link QFieldMetaData} with the table name or
    ** alias from which it was resolved.
    **
    ** @param field            the field's metadata.
    ** @param tableNameOrAlias the table name or alias that the field belongs to,
    **                         as it should appear in generated SQL.
    *******************************************************************************/
   public record FieldAndTableNameOrAlias(QFieldMetaData field, String tableNameOrAlias)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void log(String message, LogPair... logPairs)
   {
      LOG.log(logLevel, message, null, logPairs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void logFilter(String message, QQueryFilter filter)
   {
      if(logLevelForFilter.equals(Level.OFF))
      {
         return;
      }
      System.out.println(message + "\n" + filter);
   }



   /*******************************************************************************
    ** Print (to stdout, for easier reading) the object in a big table format for
    ** debugging.  Happens any time logLevel is > OFF.  Not meant for loggly.
    *******************************************************************************/
   private void dumpDebug(boolean isStart, boolean isEnd)
   {
      if(logLevel.equals(Level.OFF))
      {
         return;
      }

      int sm       = 8;
      int md       = 30;
      int lg       = 50;
      int overhead = 14;
      int full     = sm + 3 * md + lg + overhead;

      if(isStart)
      {
         System.out.println("\n" + StringUtils.safeTruncate("--- Start [main table: " + this.mainTableName + "] " + "-".repeat(full), full));
      }

      StringBuilder rs           = new StringBuilder();
      String        formatString = "| %-" + md + "s | %-" + md + "s %-" + md + "s | %-" + lg + "s | %-" + sm + "s |\n";
      rs.append(String.format(formatString, "Base Table", "Join Table", "(Alias)", "Join Meta Data", "Type"));
      String dashesLg = "-".repeat(lg);
      String dashesMd = "-".repeat(md);
      String dashesSm = "-".repeat(sm);
      rs.append(String.format(formatString, dashesMd, dashesMd, dashesMd, dashesLg, dashesSm));
      if(CollectionUtils.nullSafeHasContents(queryJoins))
      {
         for(QueryJoin queryJoin : queryJoins)
         {
            rs.append(String.format(
               formatString,
               StringUtils.hasContent(queryJoin.getBaseTableOrAlias()) ? StringUtils.safeTruncate(queryJoin.getBaseTableOrAlias(), md) : "--",
               StringUtils.safeTruncate(queryJoin.getJoinTable(), md),
               (StringUtils.hasContent(queryJoin.getAlias()) ? "(" + StringUtils.safeTruncate(queryJoin.getAlias(), md - 2) + ")" : ""),
               queryJoin.getJoinMetaData() == null ? "--" : StringUtils.safeTruncate(queryJoin.getJoinMetaData().getName(), lg),
               queryJoin.getType()));
         }
      }
      else
      {
         rs.append(String.format(formatString, "-empty-", "", "", "", ""));
      }

      System.out.print(rs);

      System.out.println(securityFilter);

      if(isEnd)
      {
         System.out.println(StringUtils.safeTruncate("--- End " + "-".repeat(full), full) + "\n");
      }
      else
      {
         System.out.println(StringUtils.safeTruncate("-".repeat(full), full));
      }
   }



   /***************************************************************************
    * For a given table, get a list of tables that can be joined to it, along
    * with a path of joins that go from the input table to the join table.
    *
    * Per the @{code onlyExposedJoins} parameter, this method will only consider
    * exposed joins on the table, or it may also include all joins that are
    * available in the instance, based on the join graph.
    *
    * @param table the table to get joins for.
    * @param onlyExposedJoins whether to only consider exposed joins.
    * @return a list of joins that can be used to join to the given table.
    ***************************************************************************/
   private List<JoinTableAndPath> getJoinTablesAndPaths(QTableMetaData table, boolean onlyExposedJoins)
   {
      List<JoinTableAndPath> rs = new ArrayList<>();

      for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
      {
         rs.add(new JoinTableAndPath(exposedJoin.getJoinTable(), exposedJoin.getJoinPath()));
      }

      if(!onlyExposedJoins)
      {
         JoinGraph                         joinGraph       = QContext.getQInstance().getJoinGraph();
         Set<JoinGraph.JoinConnectionList> joinConnections = joinGraph.getJoinConnections(table.getName());
         for(JoinGraph.JoinConnectionList joinConnection : joinConnections)
         {
            List<String>  joinNamesAsList = joinConnection.getJoinNamesAsList();
            String        lastJoinName    = joinNamesAsList.getLast();
            QJoinMetaData lastJoin        = QContext.getQInstance().getJoin(lastJoinName);

            rs.add(new JoinTableAndPath(lastJoin.getLeftTable(), joinNamesAsList));
            rs.add(new JoinTableAndPath(lastJoin.getRightTable(), joinNamesAsList));
         }
      }

      return (rs);
   }



   /***************************************************************************
    * helper record that connects a joinTable to a path of join names that
    * get from a source table to that joinTable.
    ***************************************************************************/
   private record JoinTableAndPath(String joinTable, List<String> joinPath)
   {
   }
}
