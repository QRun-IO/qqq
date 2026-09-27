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

package com.kingsrook.qqq.backend.core.model.metadata.joins;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Definition of how 2 tables join together within a QQQ Instance.
 *******************************************************************************/
public class QJoinMetaData implements TopLevelMetaDataInterface, Cloneable
{
   private String   name;
   private JoinType type;
   private String   leftTable;
   private String   rightTable;

   private List<JoinOn>         joinOns;
   private List<QFilterOrderBy> orderBys;



   /*******************************************************************************
    ** Return a new QJoinMetaData, with all the same data as this one, but flipped
    ** right ←→ left, for the tables, the type, and the joinOns.
    *******************************************************************************/
   public QJoinMetaData flip()
   {
      return (new QJoinMetaData()
         .withName(name) // does this need to be different?, e.g., + "Flipped"?
         .withLeftTable(rightTable)
         .withRightTable(leftTable)
         .withType(type.flip())
         .withJoinOns(joinOns.stream().map(JoinOn::flip).toList()));
      // todo - what about order bys??
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QJoinMetaData clone()
   {
      try
      {
         QJoinMetaData clone = (QJoinMetaData) super.clone();

         if(joinOns != null)
         {
            clone.joinOns = new ArrayList<>();
            for(JoinOn joinOn : joinOns)
            {
               clone.joinOns.add(joinOn.clone());
            }
         }

         if(orderBys != null)
         {
            clone.orderBys = new ArrayList<>();
            for(QFilterOrderBy orderBy : orderBys)
            {
               clone.orderBys.add(orderBy.clone());
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
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QJoinMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public JoinType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(JoinType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public QJoinMetaData withType(JoinType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for leftTable
    **
    *******************************************************************************/
   public String getLeftTable()
   {
      return leftTable;
   }



   /*******************************************************************************
    ** Setter for leftTable
    **
    *******************************************************************************/
   public void setLeftTable(String leftTable)
   {
      this.leftTable = leftTable;
   }



   /*******************************************************************************
    ** Fluent setter for leftTable
    **
    *******************************************************************************/
   public QJoinMetaData withLeftTable(String leftTable)
   {
      this.leftTable = leftTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rightTable
    **
    *******************************************************************************/
   public String getRightTable()
   {
      return rightTable;
   }



   /*******************************************************************************
    ** Setter for rightTable
    **
    *******************************************************************************/
   public void setRightTable(String rightTable)
   {
      this.rightTable = rightTable;
   }



   /*******************************************************************************
    ** Fluent setter for rightTable
    **
    *******************************************************************************/
   public QJoinMetaData withRightTable(String rightTable)
   {
      this.rightTable = rightTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for joinOns
    **
    *******************************************************************************/
   public List<JoinOn> getJoinOns()
   {
      return joinOns;
   }



   /*******************************************************************************
    ** Setter for joinOns
    **
    *******************************************************************************/
   public void setJoinOns(List<JoinOn> joinOns)
   {
      this.joinOns = joinOns;
   }



   /*******************************************************************************
    ** Fluent setter for joinOns
    **
    *******************************************************************************/
   public QJoinMetaData withJoinOns(List<JoinOn> joinOns)
   {
      this.joinOns = joinOns;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for joinOns
    **
    *******************************************************************************/
   public QJoinMetaData withJoinOn(JoinOn joinOn)
   {
      if(this.joinOns == null)
      {
         this.joinOns = new ArrayList<>();
      }
      this.joinOns.add(joinOn);
      return (this);
   }



   /*******************************************************************************
    ** Getter for orderBys
    **
    *******************************************************************************/
   public List<QFilterOrderBy> getOrderBys()
   {
      return orderBys;
   }



   /*******************************************************************************
    ** Setter for orderBys
    **
    *******************************************************************************/
   public void setOrderBys(List<QFilterOrderBy> orderBys)
   {
      this.orderBys = orderBys;
   }



   /*******************************************************************************
    ** Fluent setter for orderBys
    **
    *******************************************************************************/
   public QJoinMetaData withOrderBys(List<QFilterOrderBy> orderBys)
   {
      this.orderBys = orderBys;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for orderBys
    **
    *******************************************************************************/
   public QJoinMetaData withOrderBy(QFilterOrderBy orderBy)
   {
      if(this.orderBys == null)
      {
         this.orderBys = new ArrayList<>();
      }
      this.orderBys.add(orderBy);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QJoinMetaData withInferredName()
   {
      return (withName(makeInferredJoinName(getLeftTable(), getRightTable())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String makeInferredJoinName(String leftTable, String rightTable)
   {
      if(!StringUtils.hasContent(leftTable) || !StringUtils.hasContent(rightTable))
      {
         throw (new IllegalStateException("Missing either a left or right table name when trying to set inferred name for join"));
      }
      return (leftTable + "Join" + StringUtils.ucFirst(rightTable));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addJoin(this);
   }
}
