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

package com.kingsrook.qqq.backend.core.model.metadata.security;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Subclass of RecordSecurityLock, for combining multiple locks using a boolean
 ** (AND/OR) condition.  Note that the combined locks can themselves also be
 ** Multi-locks, thus creating a tree of locks.
 *******************************************************************************/
public class MultiRecordSecurityLock extends RecordSecurityLock implements Cloneable
{
   private List<RecordSecurityLock> locks = new ArrayList<>();
   private BooleanOperator          operator;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public MultiRecordSecurityLock clone()
   {
      MultiRecordSecurityLock clone = (MultiRecordSecurityLock) super.clone();

      /////////////////////////
      // deep-clone the list //
      /////////////////////////
      if(locks != null)
      {
         clone.locks = new ArrayList<>();
         for(RecordSecurityLock lock : locks)
         {
            clone.locks.add(lock.clone());
         }
      }

      return (clone);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum BooleanOperator
   {
      AND,
      OR;



      /*******************************************************************************
       **
       *******************************************************************************/
      public QQueryFilter.BooleanOperator toFilterOperator()
      {
         return switch(this)
         {
            case AND -> QQueryFilter.BooleanOperator.AND;
            case OR -> QQueryFilter.BooleanOperator.OR;
         };
      }
   }



   ////////////////////////////////
   // todo - remove, this is POC //
   ////////////////////////////////
   static
   {
      new QTableMetaData()
         .withName("savedReport")
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withLocks(List.of(
               new RecordSecurityLock()
                  .withFieldName("userId")
                  .withSecurityKeyType("user")
                  .withNullValueBehavior(NullValueBehavior.DENY)
                  .withLockScope(LockScope.READ_AND_WRITE),
               new RecordSecurityLock()
                  .withFieldName("sharedReport.userId")
                  .withJoinNameChain(List.of("reportJoinSharedReport"))
                  .withSecurityKeyType("user")
                  .withNullValueBehavior(NullValueBehavior.DENY)
                  .withLockScope(LockScope.READ_AND_WRITE), // dynamic, from a value...
               new RecordSecurityLock()
                  .withFieldName("sharedReport.groupId")
                  .withJoinNameChain(List.of("reportJoinSharedReport"))
                  .withSecurityKeyType("group")
                  .withNullValueBehavior(NullValueBehavior.DENY)
                  .withLockScope(LockScope.READ_AND_WRITE) // dynamic, from a value...
            )));

   }

   /*******************************************************************************
    ** Getter for locks
    *******************************************************************************/
   public List<RecordSecurityLock> getLocks()
   {
      return (this.locks);
   }



   /*******************************************************************************
    ** Setter for locks
    *******************************************************************************/
   public void setLocks(List<RecordSecurityLock> locks)
   {
      this.locks = locks;
   }



   /*******************************************************************************
    ** Fluent setter for locks
    *******************************************************************************/
   public MultiRecordSecurityLock withLocks(List<RecordSecurityLock> locks)
   {
      this.locks = locks;
      return (this);
   }



   /*******************************************************************************
    ** Fluently add one lock
    *******************************************************************************/
   public MultiRecordSecurityLock withLock(RecordSecurityLock lock)
   {
      if(this.locks == null)
      {
         this.locks = new ArrayList<>();
      }
      this.locks.add(lock);
      return (this);
   }



   /*******************************************************************************
    ** Getter for operator
    *******************************************************************************/
   public BooleanOperator getOperator()
   {
      return (this.operator);
   }



   /*******************************************************************************
    ** Setter for operator
    *******************************************************************************/
   public void setOperator(BooleanOperator operator)
   {
      this.operator = operator;
   }



   /*******************************************************************************
    ** Fluent setter for operator
    *******************************************************************************/
   public MultiRecordSecurityLock withOperator(BooleanOperator operator)
   {
      this.operator = operator;
      return (this);
   }

}
