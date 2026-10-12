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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Instance-level provider that {@link RecordSearchAction} (and so the v1
 ** POST /search endpoint) asks to search the tables it claims - e.g., a search
 ** index such as OpenSearch - instead of the default "contains" query over each
 ** table's search fields.  Register one with
 ** QInstance.withRecordSearchProvider(new QCodeReference(YourProvider.class)).
 **
 ** A table is only ever searched (by a provider or not) when it declares
 ** search fields, the session may read it, and it supports query - so a
 ** provider that indexes a table should also give it search fields (which are
 ** what frontends use to find searchable tables, and what the default search
 ** falls back to).  Tables the provider does not claim keep the default search,
 ** and both sets of results are merged into one output, in instance table order.
 **
 ** Core keeps the security model: it only offers the provider tables the session
 ** may read, then re-reads the records the provider returns through QueryAction
 ** (by primary key), so record security locks apply and records deleted since
 ** they were indexed drop out.  Record labels in the output come from that
 ** query.  Because some ids may drop out, a provider may return more than
 ** limitPerTable ids per table (core considers up to
 ** RecordSearchAction.MAX_PROVIDER_IDS_PER_TABLE, in the provider's order, and
 ** keeps the first limitPerTable that the session may read).
 **
 ** If the provider throws (or can't be loaded), the error is logged as a
 ** warning and the claimed tables are searched with the default search instead,
 ** so an unavailable search index degrades search rather than failing it.
 **
 ** A new instance is loaded (via QCodeLoader) for each search, so keep any
 ** shared state (e.g., a client connection) outside the instance.
 *******************************************************************************/
public interface RecordSearchProviderInterface
{
   /*******************************************************************************
    ** Whether this provider searches this table (rather than the default search).
    ** Called for each table that is about to be searched, so keep it cheap - e.g.,
    ** a lookup in the provider's configured set of indexed tables.
    *******************************************************************************/
   boolean claimsTable(QTableMetaData table);

   /*******************************************************************************
    ** Search the claimed tables.  The input's searchTerm is trimmed and
    ** validated, its tableNames are exactly the claimed tables the session may
    ** read (never empty), its limitPerTable is the effective (bounded) limit, its
    ** offset is the (bounded, non-negative) number of matches to skip in each
    ** table, its sort is the requested order (never null; honor RECENT if you
    ** can, else return relevance order), and its inputSource is the caller's.
    ** Return results only for those tables, each with at least tableName and
    ** recordId (the primary key value), ordered within each table.
    **
    ** Optionally, also return on each result a score (relevance, higher is
    ** better) and highlights (plain text - never markup - with match ranges as
    ** character offsets; core keeps only well-formed ones, for fields the
    ** session can see, where its own highlighting found no match), and on the
    ** output a totalCountByTable (approximate total matches per table, used to
    ** tell callers there are more).
    *******************************************************************************/
   RecordSearchOutput search(RecordSearchInput input) throws QException;

}
