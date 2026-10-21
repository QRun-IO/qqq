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


import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildRecordListWidget;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProductionContext;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Generic meta-data-producer, which should be instantiated (e.g., by
 ** MetaDataProducer Helper), to produce a ChildRecordList QWidgetMetaData, to
 ** produce a QJoinMetaData, based on a QRecordEntity and a ChildTable sub-annotation.
 **
 ** e.g., Orders & LineItems - on the Order entity
 <code>
 @QMetaDataProducingEntity( childTables = { @ChildTable(
 childTableEntityClass = LineItem.class,
 childJoin = @ChildJoin(enabled = true),
 childRecordListWidget = @ChildRecordListWidget(enabled = true, label = "Order Lines"))
 })
 public class Order extends QRecordEntity
 </code>
 **
 *******************************************************************************/
public class ChildRecordListWidgetFromRecordEntityGenericMetaDataProducer implements MetaDataProducerInterface<QWidgetMetaData>
{
   private String childTableName; // e.g., lineItem
   private String parentTableName; // e.g., order

   private MetaDataCustomizerInterface<QWidgetMetaData> widgetMetaDataProductionCustomizer = null;

   private ChildRecordListWidget childRecordListWidget;

   private Class<?> sourceClass;



   /***************************************************************************
    **
    ***************************************************************************/
   public ChildRecordListWidgetFromRecordEntityGenericMetaDataProducer(String childTableName, String parentTableName, ChildRecordListWidget childRecordListWidget) throws Exception
   {
      this.childTableName = childTableName;
      this.parentTableName = parentTableName;
      this.childRecordListWidget = childRecordListWidget;

      Class<? extends MetaDataCustomizerInterface<?>> genericMetaProductionCustomizer = (Class<? extends MetaDataCustomizerInterface<?>>) childRecordListWidget.widgetMetaDataCustomizer();
      if(!genericMetaProductionCustomizer.equals(MetaDataCustomizerInterface.NoopMetaDataCustomizer.class))
      {
         Class<? extends MetaDataCustomizerInterface<QWidgetMetaData>> widgetMetaProductionCustomizerClass = (Class<? extends MetaDataCustomizerInterface<QWidgetMetaData>>) genericMetaProductionCustomizer;
         this.widgetMetaDataProductionCustomizer = widgetMetaProductionCustomizerClass.getConstructor().newInstance();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      String        name = QJoinMetaData.makeInferredJoinName(parentTableName, childTableName);
      QJoinMetaData join = qInstance.getJoin(name);

      if(join == null)
      {
         for(MetaDataProducerMultiOutput metaDataProducerMultiOutput : QBitProductionContext.getReadOnlyViewOfMetaDataProducerMultiOutputStack())
         {
            join = CollectionUtils.nonNullList(metaDataProducerMultiOutput.getEach(QJoinMetaData.class)).stream()
               .filter(t -> t.getName().equals(name))
               .findFirst().orElse(null);

            if(join != null)
            {
               break;
            }
         }
      }

      if(join == null)
      {
         throw (new QException("Could not find joinMetaData: " + name));
      }

      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(join)
         .withName(name)
         .withLabel(childRecordListWidget.label())
         .withCanAddChildRecord(childRecordListWidget.canAddChildRecords())
         .getWidgetMetaData();

      if(StringUtils.hasContent(childRecordListWidget.manageAssociationName()))
      {
         widget.withDefaultValue("manageAssociationName", childRecordListWidget.manageAssociationName());
      }

      if(childRecordListWidget.maxRows() > 0)
      {
         widget.withDefaultValue("maxRows", childRecordListWidget.maxRows());
      }

      if(this.widgetMetaDataProductionCustomizer != null)
      {
         widget = this.widgetMetaDataProductionCustomizer.customizeMetaData(qInstance, widget);
      }

      return (widget);
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
   public ChildRecordListWidgetFromRecordEntityGenericMetaDataProducer withSourceClass(Class<?> sourceClass)
   {
      this.sourceClass = sourceClass;
      return (this);
   }
}
