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

package com.kingsrook.qqq.backend.core.actions.tables.query;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Converts a virtual field into ordinary query filters. Implementations reject
 ** unsupported operators with QUserFacingException. Returned filters must not
 ** contain further expander criteria; expansion is a single pass.
 *******************************************************************************/
public interface VirtualFieldCriteriaExpanderInterface
{
   /*******************************************************************************
    ** Expand one criterion, preserving its semantics as a nested filter.
    *******************************************************************************/
   QQueryFilter expand(QTableMetaData table, QFilterCriteria criteria, CriteriaExpansionContext context) throws QException;

   /*******************************************************************************
    ** Return a matching filter, or null when this field does not match the term.
    ** TAG terms have their leading hash removed; matching policy belongs here.
    *******************************************************************************/
   QQueryFilter expandSearchTerm(QTableMetaData table, String term, SearchTermKind kind, CriteriaExpansionContext context) throws QException;
}
