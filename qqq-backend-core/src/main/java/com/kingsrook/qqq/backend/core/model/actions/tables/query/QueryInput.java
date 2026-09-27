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


import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.ActionFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;


/*******************************************************************************
 ** Input data for the Query action
 **
 ** Todo - maybe make a class between AbstractTableActionInput and {QueryInput,
 ** CountInput, and AggregateInput}, with common attributes for all of these
 ** "read" operations (like, queryHints,
 *******************************************************************************/
public class QueryInput extends AbstractTableActionInput implements QueryOrGetInputInterface, QueryOrCountInputInterface, Cloneable
{
   private QBackendTransaction transaction;
   private QQueryFilter        filter;

   private RecordPipe recordPipe;
   private Integer    timeoutSeconds;

   private boolean shouldTranslatePossibleValues = false;
   private boolean shouldGenerateDisplayValues   = false;
   private boolean shouldFetchHeavyFields        = false;
   private boolean shouldOmitHiddenFields        = true;
   private boolean shouldMaskPasswords           = true;

   /////////////////////////////////////////////////////////////////////////////////////////
   // this field - only applies if shouldTranslatePossibleValues is true.                 //
   // if this field is null, then ALL possible value fields get translated.               //
   // if this field is non-null, then ONLY the fieldNames in this set will be translated. //
   /////////////////////////////////////////////////////////////////////////////////////////
   private Set<String> fieldsToTranslatePossibleValues;

   private List<QueryJoin> queryJoins     = null;
   private boolean         selectDistinct = false;

   /////////////////////////////////////////////////////////////////////////////
   // if this set is null, then the default (all fields) should be included   //
   // if it's an empty set, that should throw an error                        //
   // or if there are any fields in it that aren't valid fields on the table, //
   // or in a selected queryJoin.                                             //
   /////////////////////////////////////////////////////////////////////////////
   private Set<String> fieldNamesToInclude;

   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // if you say you want to includeAssociations, you can limit which ones by passing them in associationNamesToInclude. //
   // if you leave it null, you get all associations defined on the table.  if you pass it as empty, you get none.       //
   // to go to a recursive level of associations, you need to dot-qualify the names.  e.g., A, B, A.C, A.D, A.C.E        //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private boolean            includeAssociations       = false;
   private Collection<String> associationNamesToInclude = null;

   private EnumSet<QueryHint> queryHints = EnumSet.noneOf(QueryHint.class);
   private Set<ActionFlag>    flags;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QueryInput(String tableName)
   {
      setTableName(tableName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryInput clone() throws CloneNotSupportedException
   {
      QueryInput clone = (QueryInput) super.clone();

      if(fieldsToTranslatePossibleValues != null)
      {
         clone.fieldsToTranslatePossibleValues = new HashSet<>(fieldsToTranslatePossibleValues);
      }

      if(queryJoins != null)
      {
         clone.queryJoins = new ArrayList<>(queryJoins);
      }

      if(clone.associationNamesToInclude != null)
      {
         clone.associationNamesToInclude = new HashSet<>(associationNamesToInclude);
      }

      if(queryHints != null)
      {
         clone.queryHints = EnumSet.noneOf(QueryHint.class);
         clone.queryHints.addAll(queryHints);
      }

      return (clone);
   }



   /*******************************************************************************
    ** Getter for filter
    **
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return filter;
   }



   /*******************************************************************************
    ** Setter for filter
    **
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    ** Getter for recordPipe
    **
    *******************************************************************************/
   public RecordPipe getRecordPipe()
   {
      return recordPipe;
   }



   /*******************************************************************************
    ** Setter for recordPipe
    **
    *******************************************************************************/
   public void setRecordPipe(RecordPipe recordPipe)
   {
      this.recordPipe = recordPipe;
   }



   /*******************************************************************************
    ** Getter for shouldTranslatePossibleValues
    **
    *******************************************************************************/
   public boolean getShouldTranslatePossibleValues()
   {
      return shouldTranslatePossibleValues;
   }



   /*******************************************************************************
    ** Setter for shouldTranslatePossibleValues
    **
    *******************************************************************************/
   public void setShouldTranslatePossibleValues(boolean shouldTranslatePossibleValues)
   {
      this.shouldTranslatePossibleValues = shouldTranslatePossibleValues;
   }



   /*******************************************************************************
    ** Getter for shouldGenerateDisplayValues
    **
    *******************************************************************************/
   public boolean getShouldGenerateDisplayValues()
   {
      return shouldGenerateDisplayValues;
   }



   /*******************************************************************************
    ** Setter for shouldGenerateDisplayValues
    **
    *******************************************************************************/
   public void setShouldGenerateDisplayValues(boolean shouldGenerateDisplayValues)
   {
      this.shouldGenerateDisplayValues = shouldGenerateDisplayValues;
   }



   /*******************************************************************************
    ** Getter for shouldFetchHeavyFields
    **
    *******************************************************************************/
   public boolean getShouldFetchHeavyFields()
   {
      return shouldFetchHeavyFields;
   }



   /*******************************************************************************
    ** Setter for shouldFetchHeavyFields
    **
    *******************************************************************************/
   public void setShouldFetchHeavyFields(boolean shouldFetchHeavyFields)
   {
      this.shouldFetchHeavyFields = shouldFetchHeavyFields;
   }



   /*******************************************************************************
    ** Getter for transaction
    **
    *******************************************************************************/
   public QBackendTransaction getTransaction()
   {
      return transaction;
   }



   /*******************************************************************************
    ** Setter for transaction
    **
    *******************************************************************************/
   public void setTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
   }



   /*******************************************************************************
    ** Fluent setter for transaction
    **
    *******************************************************************************/
   public QueryInput withTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryJoins
    **
    *******************************************************************************/
   public List<QueryJoin> getQueryJoins()
   {
      return queryJoins;
   }



   /*******************************************************************************
    ** Setter for queryJoins
    **
    *******************************************************************************/
   public void setQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public QueryInput withQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public QueryInput withQueryJoin(QueryJoin queryJoin)
   {
      if(this.queryJoins == null)
      {
         this.queryJoins = new ArrayList<>();
      }
      this.queryJoins.add(queryJoin);
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldsToTranslatePossibleValues
    **
    *******************************************************************************/
   public Set<String> getFieldsToTranslatePossibleValues()
   {
      return fieldsToTranslatePossibleValues;
   }



   /*******************************************************************************
    ** Setter for fieldsToTranslatePossibleValues
    **
    *******************************************************************************/
   public void setFieldsToTranslatePossibleValues(Set<String> fieldsToTranslatePossibleValues)
   {
      this.fieldsToTranslatePossibleValues = fieldsToTranslatePossibleValues;
   }



   /*******************************************************************************
    ** Fluent setter for fieldsToTranslatePossibleValues
    **
    *******************************************************************************/
   public QueryInput withFieldsToTranslatePossibleValues(Set<String> fieldsToTranslatePossibleValues)
   {
      this.fieldsToTranslatePossibleValues = fieldsToTranslatePossibleValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public QueryInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for recordPipe
    *******************************************************************************/
   public QueryInput withRecordPipe(RecordPipe recordPipe)
   {
      this.recordPipe = recordPipe;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for shouldTranslatePossibleValues
    *******************************************************************************/
   public QueryInput withShouldTranslatePossibleValues(boolean shouldTranslatePossibleValues)
   {
      this.shouldTranslatePossibleValues = shouldTranslatePossibleValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for shouldGenerateDisplayValues
    *******************************************************************************/
   public QueryInput withShouldGenerateDisplayValues(boolean shouldGenerateDisplayValues)
   {
      this.shouldGenerateDisplayValues = shouldGenerateDisplayValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for shouldFetchHeavyFields
    *******************************************************************************/
   public QueryInput withShouldFetchHeavyFields(boolean shouldFetchHeavyFields)
   {
      this.shouldFetchHeavyFields = shouldFetchHeavyFields;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryInput withTableName(String tableName)
   {
      super.withTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeAssociations
    *******************************************************************************/
   public boolean getIncludeAssociations()
   {
      return (this.includeAssociations);
   }



   /*******************************************************************************
    ** Setter for includeAssociations
    *******************************************************************************/
   public void setIncludeAssociations(boolean includeAssociations)
   {
      this.includeAssociations = includeAssociations;
   }



   /*******************************************************************************
    ** Fluent setter for includeAssociations
    *******************************************************************************/
   public QueryInput withIncludeAssociations(boolean includeAssociations)
   {
      this.includeAssociations = includeAssociations;
      return (this);
   }



   /*******************************************************************************
    ** Getter for associationNamesToInclude
    *******************************************************************************/
   public Collection<String> getAssociationNamesToInclude()
   {
      return (this.associationNamesToInclude);
   }



   /*******************************************************************************
    ** Setter for associationNamesToInclude
    *******************************************************************************/
   public void setAssociationNamesToInclude(Collection<String> associationNamesToInclude)
   {
      this.associationNamesToInclude = associationNamesToInclude;
   }



   /*******************************************************************************
    ** Fluent setter for associationNamesToInclude
    *******************************************************************************/
   public QueryInput withAssociationNamesToInclude(Collection<String> associationNamesToInclude)
   {
      this.associationNamesToInclude = associationNamesToInclude;
      return (this);
   }



   /*******************************************************************************
    ** Getter for shouldMaskPasswords
    *******************************************************************************/
   public boolean getShouldMaskPasswords()
   {
      return (this.shouldMaskPasswords);
   }



   /*******************************************************************************
    ** Setter for shouldMaskPasswords
    *******************************************************************************/
   public void setShouldMaskPasswords(boolean shouldMaskPasswords)
   {
      this.shouldMaskPasswords = shouldMaskPasswords;
   }



   /*******************************************************************************
    ** Fluent setter for shouldMaskPasswords
    *******************************************************************************/
   public QueryInput withShouldMaskPasswords(boolean shouldMaskPasswords)
   {
      this.shouldMaskPasswords = shouldMaskPasswords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for shouldOmitHiddenFields
    *******************************************************************************/
   public boolean getShouldOmitHiddenFields()
   {
      return (this.shouldOmitHiddenFields);
   }



   /*******************************************************************************
    ** Setter for shouldOmitHiddenFields
    *******************************************************************************/
   public void setShouldOmitHiddenFields(boolean shouldOmitHiddenFields)
   {
      this.shouldOmitHiddenFields = shouldOmitHiddenFields;
   }



   /*******************************************************************************
    ** Fluent setter for shouldOmitHiddenFields
    *******************************************************************************/
   public QueryInput withShouldOmitHiddenFields(boolean shouldOmitHiddenFields)
   {
      this.shouldOmitHiddenFields = shouldOmitHiddenFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for selectDistinct
    *******************************************************************************/
   public boolean getSelectDistinct()
   {
      return (this.selectDistinct);
   }



   /*******************************************************************************
    ** Setter for selectDistinct
    *******************************************************************************/
   public void setSelectDistinct(boolean selectDistinct)
   {
      this.selectDistinct = selectDistinct;
   }



   /*******************************************************************************
    ** Fluent setter for selectDistinct
    *******************************************************************************/
   public QueryInput withSelectDistinct(boolean selectDistinct)
   {
      this.selectDistinct = selectDistinct;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timeoutSeconds
    *******************************************************************************/
   public Integer getTimeoutSeconds()
   {
      return (this.timeoutSeconds);
   }



   /*******************************************************************************
    ** Setter for timeoutSeconds
    *******************************************************************************/
   public void setTimeoutSeconds(Integer timeoutSeconds)
   {
      this.timeoutSeconds = timeoutSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for timeoutSeconds
    *******************************************************************************/
   public QueryInput withTimeoutSeconds(Integer timeoutSeconds)
   {
      this.timeoutSeconds = timeoutSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryHints
    *******************************************************************************/
   public EnumSet<QueryHint> getQueryHints()
   {
      return (this.queryHints);
   }



   /*******************************************************************************
    ** Setter for queryHints
    *******************************************************************************/
   public void setQueryHints(EnumSet<QueryHint> queryHints)
   {
      this.queryHints = queryHints;
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public QueryInput withQueryHints(EnumSet<QueryHint> queryHints)
   {
      this.queryHints = queryHints;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public QueryInput withQueryHint(QueryHint queryHint)
   {
      if(this.queryHints == null)
      {
         this.queryHints = EnumSet.noneOf(QueryHint.class);
      }
      this.queryHints.add(queryHint);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryHints
    *******************************************************************************/
   public QueryInput withoutQueryHint(QueryHint queryHint)
   {
      if(this.queryHints != null)
      {
         this.queryHints.remove(queryHint);
      }
      return (this);
   }



   /*******************************************************************************
    ** null-safely check if query hints map contains the specified hint
    *******************************************************************************/
   public boolean hasQueryHint(QueryHint queryHint)
   {
      if(this.queryHints == null)
      {
         return (false);
      }

      return (queryHints.contains(queryHint));
   }



   /*******************************************************************************
    ** Getter for fieldNamesToInclude
    *******************************************************************************/
   public Set<String> getFieldNamesToInclude()
   {
      return (this.fieldNamesToInclude);
   }



   /*******************************************************************************
    ** Setter for fieldNamesToInclude
    *******************************************************************************/
   public void setFieldNamesToInclude(Set<String> fieldNamesToInclude)
   {
      this.fieldNamesToInclude = fieldNamesToInclude;
   }



   /*******************************************************************************
    ** Fluent setter for fieldNamesToInclude
    *******************************************************************************/
   public QueryInput withFieldNamesToInclude(Set<String> fieldNamesToInclude)
   {
      this.fieldNamesToInclude = fieldNamesToInclude;
      return (this);
   }



   /*******************************************************************************
    * Getter for flags
    * @see #withFlags(Set)
    *******************************************************************************/
   public Set<ActionFlag> getFlags()
   {
      return (this.flags);
   }



   /*******************************************************************************
    * Setter for flags
    * @see #withFlags(Set)
    *******************************************************************************/
   public void setFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
   }



   /*******************************************************************************
    * Fluent setter for flags
    *
    * @param flags
    * framework, backend-module, or application defined flags that con be used to drive
    * behaviors.  For example, maybe to bypass caching actions, or to customize a
    * query, etc.
    * @return this
    *******************************************************************************/
   public QueryInput withFlags(Set<ActionFlag> flags)
   {
      this.flags = flags;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public QueryInput withFlag(ActionFlag flag)
   {
      if(this.flags == null)
      {
         this.flags = new HashSet<>();
      }
      this.flags.add(flag);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public boolean hasFlag(ActionFlag flag)
   {
      if(this.flags == null)
      {
         return (false);
      }

      return (this.flags.contains(flag));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QueryInput withInputSource(InputSource inputSource)
   {
      super.withInputSource(inputSource);
      return (this);
   }
}
