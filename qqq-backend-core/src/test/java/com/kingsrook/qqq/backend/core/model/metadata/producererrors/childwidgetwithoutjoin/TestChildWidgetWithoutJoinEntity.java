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

package com.kingsrook.qqq.backend.core.model.metadata.producererrors.childwidgetwithoutjoin;


import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestMetaDataProducingChildEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildRecordListWidget;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildTable;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;


/*******************************************************************************
 ** Test entity that asks for a child record list widget without the child
 ** join - which MetaDataProducerHelper doesn't allow.
 *******************************************************************************/
@QMetaDataProducingEntity(
   childTables =
      {
         @ChildTable(childTableEntityClass = TestMetaDataProducingChildEntity.class,
            childRecordListWidget = @ChildRecordListWidget(enabled = true))
      }
)
public class TestChildWidgetWithoutJoinEntity extends QRecordEntity
{
   public static final String TABLE_NAME = "testChildWidgetWithoutJoin";
}
