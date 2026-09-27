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
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Part of query (or count, aggregate) input, to do a Join as part of a query.
 **
 ** Conceptually, when you're adding a QueryJoin to a query, you're adding a new
 ** table to the query - this is named the `joinTable` in this class.  This table
 ** can be given an alias, which can be referenced in the rest of the query.
 **
 ** Every joinTable needs to have a `baseTable` that it is "joined" with - e.g.,
 ** the table that the joinOn clauses link up with.
 **
 ** However - the caller doesn't necessarily need to specify the `baseTable` -
 ** as the framework will look for Joins defined in the qInstance, and if an
 ** unambiguous one is found (between the joinTable and other tables in the
 ** query), then it'll use the "other" table in that Join as the baseTable.
 **
 ** For use-cases where a baseTable has been included in a query multiple times,
 ** with aliases, then the baseTableOrAlias field must be set to the appropriate alias.
 **
 ** If there are multiple Joins defined between the base & join tables, then the
 ** specific joinMetaData to use must be set.  The joinMetaData field can also be
 ** used instead of specify joinTable and baseTableOrAlias, but only for cases
 ** where the baseTable is not an alias.
 **
 ** The securityCriteria member, in general, is meant to be populated when a
 ** JoinsContext is constructed before executing a query, and not meant to be set
 ** by users.
 *******************************************************************************/
public class QueryJoin implements Cloneable
{
   private String        baseTableOrAlias;
   private String        joinTable;
   private QJoinMetaData joinMetaData;
   private String        alias;
   private boolean       select = false;
   private Type          type   = Type.INNER;

   private List<QFilterCriteria> securityCriteria = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryJoin clone()
   {
      try
      {
         QueryJoin clone = (QueryJoin) super.clone();

         if(joinMetaData != null)
         {
            clone.joinMetaData = joinMetaData.clone();
         }

         if(securityCriteria != null)
         {
            clone.securityCriteria = new ArrayList<>();
            for(QFilterCriteria securityCriterion : securityCriteria)
            {
               clone.securityCriteria.add(securityCriterion.clone());
            }
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** define the types of joins - INNER, LEFT, RIGHT, or FULL.
    *******************************************************************************/
   public enum Type
   {
      INNER,
      LEFT,
      RIGHT,
      FULL;



      /*******************************************************************************
       ** check if a join is an OUTER type (LEFT or RIGHT).
       *******************************************************************************/
      public static boolean isOuter(Type type)
      {
         return (LEFT == type || RIGHT == type);
      }
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QueryJoin()
   {
   }



   /*******************************************************************************
    ** Constructor that only takes a joinTable.  Unless you also set the baseTableOrAlias,
    ** the framework will attempt to ascertain the baseTableOrAlias, based on Joins
    ** defined in the instance and other tables in the query.
    **
    *******************************************************************************/
   public QueryJoin(String joinTable)
   {
      this.joinTable = joinTable;
   }



   /*******************************************************************************
    ** Constructor that takes baseTableOrAlias and joinTable.  Useful if it's not
    ** explicitly clear what the base table should be just from the joinTable.  e.g.,
    ** if the baseTable has an alias, or if there's more than 1 join in the instance
    ** that matches the joinTable and the other tables in the query.
    **
    *******************************************************************************/
   public QueryJoin(String baseTableOrAlias, String joinTable)
   {
      this.baseTableOrAlias = baseTableOrAlias;
      this.joinTable = joinTable;
   }



   /*******************************************************************************
    ** Constructor that takes a joinMetaData - the rightTable in the joinMetaData will
    ** be used as the joinTable.  The leftTable in the joinMetaData will be used as
    ** the baseTable.
    **
    ** This is probably (only?) what you want to use if you have a table that joins
    ** more than once to another table (e.g., order.shipToCustomerId and order.billToCustomerId).
    **
    ** Alternatively, you could just do new QueryJoin("customer").withJoinMetaData("orderJoinShipToCustomer").
    **
    *******************************************************************************/
   public QueryJoin(QJoinMetaData joinMetaData)
   {
      setJoinMetaData(joinMetaData);
   }



   /*******************************************************************************
    ** Getter for baseTableOrAlias
    **
    *******************************************************************************/
   public String getBaseTableOrAlias()
   {
      return baseTableOrAlias;
   }



   /*******************************************************************************
    ** Setter for baseTableOrAlias
    **
    *******************************************************************************/
   public void setBaseTableOrAlias(String baseTableOrAlias)
   {
      this.baseTableOrAlias = baseTableOrAlias;
   }



   /*******************************************************************************
    ** Fluent setter for baseTableOrAlias
    **
    *******************************************************************************/
   public QueryJoin withBaseTableOrAlias(String baseTableOrAlias)
   {
      this.baseTableOrAlias = baseTableOrAlias;
      return (this);
   }



   /*******************************************************************************
    ** Getter for joinTable
    **
    *******************************************************************************/
   public String getJoinTable()
   {
      return joinTable;
   }



   /*******************************************************************************
    ** Setter for joinTable
    **
    *******************************************************************************/
   public void setJoinTable(String joinTable)
   {
      this.joinTable = joinTable;
   }



   /*******************************************************************************
    ** Fluent setter for joinTable
    **
    *******************************************************************************/
   public QueryJoin withJoinTable(String joinTable)
   {
      this.joinTable = joinTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for alias
    **
    *******************************************************************************/
   public String getAlias()
   {
      return alias;
   }



   /*******************************************************************************
    ** Setter for alias
    **
    *******************************************************************************/
   public void setAlias(String alias)
   {
      this.alias = alias;
   }



   /*******************************************************************************
    ** Fluent setter for alias
    **
    *******************************************************************************/
   public QueryJoin withAlias(String alias)
   {
      this.alias = alias;
      return (this);
   }



   /*******************************************************************************
    ** Getter for select
    **
    *******************************************************************************/
   public boolean getSelect()
   {
      return select;
   }



   /*******************************************************************************
    ** Setter for select
    **
    *******************************************************************************/
   public void setSelect(boolean select)
   {
      this.select = select;
   }



   /*******************************************************************************
    ** Fluent setter for select
    **
    *******************************************************************************/
   public QueryJoin withSelect(boolean select)
   {
      this.select = select;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getJoinTableOrItsAlias()
   {
      if(StringUtils.hasContent(alias))
      {
         return (alias);
      }
      return (joinTable);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public Type getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(Type type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public QueryJoin withType(Type type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for joinMetaData
    **
    *******************************************************************************/
   public QJoinMetaData getJoinMetaData()
   {
      return joinMetaData;
   }



   /*******************************************************************************
    ** Setter for joinMetaData
    **
    *******************************************************************************/
   public void setJoinMetaData(QJoinMetaData joinMetaData)
   {
      Objects.requireNonNull(joinMetaData, "JoinMetaData was null.");
      this.joinMetaData = joinMetaData;

      if(!StringUtils.hasContent(this.baseTableOrAlias) && !StringUtils.hasContent(this.joinTable))
      {
         setBaseTableOrAlias(joinMetaData.getLeftTable());
         setJoinTable(joinMetaData.getRightTable());
      }
   }



   /*******************************************************************************
    ** Fluent setter for joinMetaData
    **
    *******************************************************************************/
   public QueryJoin withJoinMetaData(QJoinMetaData joinMetaData)
   {
      setJoinMetaData(joinMetaData);
      return (this);
   }



   /*******************************************************************************
    ** Getter for securityCriteria
    *******************************************************************************/
   public List<QFilterCriteria> getSecurityCriteria()
   {
      return (this.securityCriteria);
   }



   /*******************************************************************************
    ** Setter for securityCriteria
    *******************************************************************************/
   public void setSecurityCriteria(List<QFilterCriteria> securityCriteria)
   {
      this.securityCriteria = securityCriteria;
   }



   /*******************************************************************************
    ** Fluent setter for securityCriteria
    *******************************************************************************/
   public QueryJoin withSecurityCriteria(List<QFilterCriteria> securityCriteria)
   {
      this.securityCriteria = securityCriteria;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for securityCriteria
    *******************************************************************************/
   public QueryJoin withSecurityCriteria(QFilterCriteria securityCriteria)
   {
      if(this.securityCriteria == null)
      {
         this.securityCriteria = new ArrayList<>();
      }
      this.securityCriteria.add(securityCriteria);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "QueryJoin{base="
         + baseTableOrAlias + ", joinTable='"
         + joinTable + ", joinMetaData="
         + (joinMetaData == null ? null : joinMetaData.getName()) + ", alias='"
         + alias + ", select="
         + select + ", type="
         + type + '}';
   }
}
