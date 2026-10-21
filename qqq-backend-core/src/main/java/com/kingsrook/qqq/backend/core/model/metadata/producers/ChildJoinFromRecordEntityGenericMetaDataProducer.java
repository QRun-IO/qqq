/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.producers;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildJoin;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProductionContext;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Generic meta-data-producer, which should be instantiated (e.g., by
 ** MetaDataProducer Helper), to produce a QJoinMetaData, based on a
 ** QRecordEntity and a ChildTable sub-annotation.
 **
 ** e.g., Orders & LineItems - on the Order entity
 ** <code>
 @QMetaDataProducingEntity( childTables = { @ChildTable(
 childTableEntityClass = LineItem.class,
 childJoin = @ChildJoin(enabled = true),
 childRecordListWidget = @ChildRecordListWidget(enabled = true, label = "Order Lines"))
 }
 )
 public class Order extends QRecordEntity
 ** </code>
 **
 ** A Join will be made:
 ** - left: Order
 ** - right: LineItem
 ** - type:  ONE_TO_MANY (one order (parent table) has mny lines (child table))
 ** - joinOn:  order's primary key, lineItem's orderId field
 ** - name:  inferred, based on the table names orderJoinLineItem)
 *******************************************************************************/
public class ChildJoinFromRecordEntityGenericMetaDataProducer implements MetaDataProducerInterface<QJoinMetaData>
{
   private String childTableName; // e.g., lineItem
   private String parentTableName; // e.g., order
   private String foreignKeyFieldName; // e.g., orderId
   private boolean isOneToOne;

   private ChildJoin.OrderBy[] orderBys;

   private Class<?> sourceClass;



   /***************************************************************************
    **
    ***************************************************************************/
   public ChildJoinFromRecordEntityGenericMetaDataProducer(String childTableName, String parentTableName, String foreignKeyFieldName, ChildJoin.OrderBy[] orderBys, boolean isOneToOne)
   {
      Objects.requireNonNull(childTableName, "childTableName cannot be null");
      Objects.requireNonNull(parentTableName, "parentTableName cannot be null");
      Objects.requireNonNull(foreignKeyFieldName, "foreignKeyFieldName cannot be null");

      this.childTableName = childTableName;
      this.parentTableName = parentTableName;
      this.foreignKeyFieldName = foreignKeyFieldName;
      this.orderBys = orderBys;
      this.isOneToOne = isOneToOne;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QJoinMetaData produce(QInstance qInstance) throws QException
   {
      QTableMetaData parentTable = getTable(qInstance, parentTableName);
      QTableMetaData childTable  = getTable(qInstance, childTableName);

      QJoinMetaData join = new QJoinMetaData()
         .withLeftTable(parentTableName)
         .withRightTable(childTableName)
         .withInferredName()
         .withType(isOneToOne ? JoinType.ONE_TO_ONE : JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn(parentTable.getPrimaryKeyField(), foreignKeyFieldName));

      if(orderBys != null && orderBys.length > 0)
      {
         for(ChildJoin.OrderBy orderBy : orderBys)
         {
            join.withOrderBy(new QFilterOrderBy(orderBy.fieldName(), orderBy.isAscending()));
         }
      }
      else
      {
         //////////////////////////////////////////////////////////
         // by default, sort by the id of the child table... mmm //
         //////////////////////////////////////////////////////////
         join.withOrderBy(new QFilterOrderBy(childTable.getPrimaryKeyField()));
      }

      return (join);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private QTableMetaData getTable(QInstance qInstance, String tableName) throws QException
   {
      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null)
      {
         ///////////////////////////////////////////////////////////////////////////////
         // in case we're producing a QBit, and it's added a table to a multi-output, //
         // but not yet the instance, see if we can get table from there              //
         ///////////////////////////////////////////////////////////////////////////////
         for(MetaDataProducerMultiOutput metaDataProducerMultiOutput : QBitProductionContext.getReadOnlyViewOfMetaDataProducerMultiOutputStack())
         {
            table = CollectionUtils.nonNullList(metaDataProducerMultiOutput.getEach(QTableMetaData.class)).stream()
               .filter(t -> t.getName().equals(tableName))
               .findFirst().orElse(null);

            if(table != null)
            {
               break;
            }
         }
      }

      if(table == null)
      {
         throw (new QException("Could not find tableMetaData: " + table));
      }

      return table;
   }



   /*******************************************************************************
    ** Getter for sourceClass
    **
    *******************************************************************************/
   public Class<?> getSourceClass()
   {
      return sourceClass;
   }



   /*******************************************************************************
    ** Setter for sourceClass
    **
    *******************************************************************************/
   public void setSourceClass(Class<?> sourceClass)
   {
      this.sourceClass = sourceClass;
   }



   /*******************************************************************************
    ** Fluent setter for sourceClass
    **
    *******************************************************************************/
   public ChildJoinFromRecordEntityGenericMetaDataProducer withSourceClass(Class<?> sourceClass)
   {
      this.sourceClass = sourceClass;
      return (this);
   }

}
