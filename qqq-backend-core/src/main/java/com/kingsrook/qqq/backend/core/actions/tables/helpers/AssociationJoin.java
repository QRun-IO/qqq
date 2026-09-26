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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** A declared association's field pairs, oriented from parent to child.
 ** Tuple values use the child field types for both matching and assignment.
 *******************************************************************************/
public class AssociationJoin
{
   private final QTableMetaData childTable;
   private final List<JoinOn> joinOns;



   /*******************************************************************************
    ** Pair copies keep orientation changes out of registered metadata.
    *******************************************************************************/
   private AssociationJoin(QTableMetaData childTable, List<JoinOn> joinOns)
   {
      this.childTable = childTable;
      this.joinOns = joinOns;
   }



   /*******************************************************************************
    ** Resolve the supplied parent's declared graph, including personalized metadata.
    ** Self joins keep the registered left side as the parent, as validation does.
    *******************************************************************************/
   public static AssociationJoin resolve(QTableMetaData parentTable, Association association) throws QException
   {
      Association declared = parentTable.getAssociationByName(association.getName()).orElseThrow(() -> new QException("Association is not declared on parent table"));
      if(!Objects.equals(declared.getJoinName(), association.getJoinName()) || !Objects.equals(declared.getAssociatedTableName(), association.getAssociatedTableName()))
      {
         throw new QException("Association does not match parent metadata");
      }

      QTableMetaData childTable = QContext.getQInstance().getTable(declared.getAssociatedTableName());
      QJoinMetaData join = QContext.getQInstance().getJoin(declared.getJoinName());
      if(childTable == null || join == null || CollectionUtils.nullSafeIsEmpty(join.getJoinOns()))
      {
         throw new QException("Association table or join is missing");
      }
      boolean parentOnLeft = Objects.equals(join.getLeftTable(), parentTable.getName()) && Objects.equals(join.getRightTable(), childTable.getName());
      boolean parentOnRight = Objects.equals(join.getRightTable(), parentTable.getName()) && Objects.equals(join.getLeftTable(), childTable.getName());
      if(!parentOnLeft && !parentOnRight)
      {
         throw new QException("Association join does not connect the parent and child tables");
      }
      List<JoinOn> joinOns = join.getJoinOns().stream().map(joinOn -> parentOnLeft ? joinOn.clone() : joinOn.flip()).toList();
      for(JoinOn joinOn : joinOns)
      {
         if(!parentTable.getFields().containsKey(joinOn.getLeftField()) || !childTable.getFields().containsKey(joinOn.getRightField()))
         {
            throw new QException("Association join field is missing");
         }
      }
      return new AssociationJoin(childTable, joinOns);
   }



   /*******************************************************************************
    ** Return parent-left field pairs without exposing the registered join.
    *******************************************************************************/
   public List<JoinOn> getJoinOns()
   {
      return joinOns;
   }



   /*******************************************************************************
    ** Preserve explicit null components; callers decide whether membership exists.
    *******************************************************************************/
   public List<Serializable> parentValues(QRecord record)
   {
      return values(record, true);
   }



   /*******************************************************************************
    ** Use the same declared types as the parent lookup key.
    *******************************************************************************/
   public List<Serializable> childValues(QRecord record)
   {
      return values(record, false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Serializable> values(QRecord record, boolean parent)
   {
      List<Serializable> values = new ArrayList<>();
      for(JoinOn joinOn : joinOns)
      {
         String field = parent ? joinOn.getLeftField() : joinOn.getRightField();
         values.add(ValueUtils.getValueAsFieldType(childTable.getField(joinOn.getRightField()).getType(), record.getValue(field)));
      }
      return values;
   }



   /*******************************************************************************
    ** Relationship fields override child input using the actual parent values.
    *******************************************************************************/
   public void assignParentValues(QRecord parent, QRecord child)
   {
      List<Serializable> values = parentValues(parent);
      for(int i = 0; i < joinOns.size(); i++)
      {
         child.setValue(joinOns.get(i).getRightField(), values.get(i));
      }
   }
}
