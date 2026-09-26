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

package com.kingsrook.qqq.backend.core.model.metadata.security;


import java.util.List;
import java.util.Set;


/*******************************************************************************
 ** standard filtering operations for lists of record security locks.
 *******************************************************************************/
public class RecordSecurityLockFilters
{

   /*******************************************************************************
    ** filter a list of locks so that we only see the ones that apply to reads.
    *******************************************************************************/
   public static List<RecordSecurityLock> filterForReadLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      if(recordSecurityLocks == null)
      {
         return (null);
      }

      return (recordSecurityLocks.stream().filter(rsl -> RecordSecurityLock.LockScope.READ_AND_WRITE.equals(rsl.getLockScope())).toList());
   }



   /*******************************************************************************
    ** filter a list of locks so that we only see the ones that apply to reads.
    *******************************************************************************/
   public static MultiRecordSecurityLock filterForReadLockTree(List<RecordSecurityLock> recordSecurityLocks)
   {
      return filterForLockTree(recordSecurityLocks, Set.of(RecordSecurityLock.LockScope.READ_AND_WRITE, RecordSecurityLock.LockScope.READ));
   }



   /*******************************************************************************
    ** filter a list of locks so that we only see the ones that apply to writes.
    *******************************************************************************/
   public static MultiRecordSecurityLock filterForWriteLockTree(List<RecordSecurityLock> recordSecurityLocks)
   {
      return filterForLockTree(recordSecurityLocks, Set.of(RecordSecurityLock.LockScope.READ_AND_WRITE, RecordSecurityLock.LockScope.WRITE));
   }



   /*******************************************************************************
    ** filter a list of locks so that we only see the ones that apply to any of the
    ** input set of scopes.
    *******************************************************************************/
   private static MultiRecordSecurityLock filterForLockTree(List<RecordSecurityLock> recordSecurityLocks, Set<RecordSecurityLock.LockScope> allowedScopes)
   {
      if(recordSecurityLocks == null)
      {
         return (null);
      }

      //////////////////////////////////////////////////////////////
      // at the top-level we build a multi-lock with AND operator //
      //////////////////////////////////////////////////////////////
      MultiRecordSecurityLock result = new MultiRecordSecurityLock();
      result.setOperator(MultiRecordSecurityLock.BooleanOperator.AND);

      for(RecordSecurityLock recordSecurityLock : recordSecurityLocks)
      {
         if(recordSecurityLock instanceof MultiRecordSecurityLock multiRecordSecurityLock)
         {
            MultiRecordSecurityLock filteredSubLock = filterForLockTree(multiRecordSecurityLock.getLocks(), allowedScopes);
            if(filteredSubLock != null && !filteredSubLock.getLocks().isEmpty())
            {
               filteredSubLock.setOperator(multiRecordSecurityLock.getOperator());
               result.withLock(filteredSubLock);
            }
         }
         else
         {
            if(allowedScopes.contains(recordSecurityLock.getLockScope()))
            {
               result.withLock(recordSecurityLock);
            }
         }
      }

      return (result);
   }



   /*******************************************************************************
    ** filter a list of locks so that we only see the ones that apply to writes.
    *******************************************************************************/
   public static List<RecordSecurityLock> filterForWriteLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      if(recordSecurityLocks == null)
      {
         return (null);
      }

      return (recordSecurityLocks.stream().filter(rsl ->
         RecordSecurityLock.LockScope.READ_AND_WRITE.equals(rsl.getLockScope())
            || RecordSecurityLock.LockScope.WRITE.equals(rsl.getLockScope()
         )).toList());
   }



   /*******************************************************************************
    ** filter a list of locks so that we only see the ones that are WRITE type only.
    *******************************************************************************/
   public static List<RecordSecurityLock> filterForOnlyWriteLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      if(recordSecurityLocks == null)
      {
         return (null);
      }

      return (recordSecurityLocks.stream().filter(rsl -> RecordSecurityLock.LockScope.WRITE.equals(rsl.getLockScope())).toList());
   }

}
