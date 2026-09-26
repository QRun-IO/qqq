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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock.BooleanOperator.AND;
import static com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock.BooleanOperator.OR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for RecordSecurityLockFilters 
 *******************************************************************************/
class RecordSecurityLockFiltersTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      MultiRecordSecurityLock nullBecauseNull = RecordSecurityLockFilters.filterForReadLockTree(null);
      assertNull(nullBecauseNull);

      MultiRecordSecurityLock emptyBecauseEmptyList = RecordSecurityLockFilters.filterForReadLockTree(List.of());
      assertEquals(0, emptyBecauseEmptyList.getLocks().size());

      MultiRecordSecurityLock emptyBecauseAllWrite = RecordSecurityLockFilters.filterForReadLockTree(List.of(
         new RecordSecurityLock().withFieldName("A").withLockScope(RecordSecurityLock.LockScope.WRITE),
         new RecordSecurityLock().withFieldName("B").withLockScope(RecordSecurityLock.LockScope.WRITE)
      ));
      assertEquals(0, emptyBecauseAllWrite.getLocks().size());

      MultiRecordSecurityLock onlyA = RecordSecurityLockFilters.filterForReadLockTree(List.of(
         new RecordSecurityLock().withFieldName("A").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
         new RecordSecurityLock().withFieldName("B").withLockScope(RecordSecurityLock.LockScope.WRITE)
      ));
      assertMultiRecordSecurityLock(onlyA, AND, "A");

      MultiRecordSecurityLock twoOutOfThreeTopLevel = RecordSecurityLockFilters.filterForReadLockTree(List.of(
         new RecordSecurityLock().withFieldName("A").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
         new RecordSecurityLock().withFieldName("B").withLockScope(RecordSecurityLock.LockScope.WRITE),
         new RecordSecurityLock().withFieldName("C").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
      ));
      assertMultiRecordSecurityLock(twoOutOfThreeTopLevel, AND, "A", "C");

      MultiRecordSecurityLock treeOfAllReads = RecordSecurityLockFilters.filterForReadLockTree(List.of(
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new RecordSecurityLock().withFieldName("A").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
            new RecordSecurityLock().withFieldName("B").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
         )),
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new RecordSecurityLock().withFieldName("C").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
            new RecordSecurityLock().withFieldName("D").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
         ))
      ));
      assertEquals(2, treeOfAllReads.getLocks().size());
      assertEquals(AND, treeOfAllReads.getOperator());
      assertMultiRecordSecurityLock((MultiRecordSecurityLock) treeOfAllReads.getLocks().get(0), OR, "A", "B");
      assertMultiRecordSecurityLock((MultiRecordSecurityLock) treeOfAllReads.getLocks().get(1), OR, "C", "D");

      MultiRecordSecurityLock treeWithOneBranchReadsOneBranchWrites = RecordSecurityLockFilters.filterForReadLockTree(List.of(
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new RecordSecurityLock().withFieldName("A").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
            new RecordSecurityLock().withFieldName("B").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
         )),
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new RecordSecurityLock().withFieldName("C").withLockScope(RecordSecurityLock.LockScope.WRITE),
            new RecordSecurityLock().withFieldName("D").withLockScope(RecordSecurityLock.LockScope.WRITE)
         ))
      ));
      assertEquals(1, treeWithOneBranchReadsOneBranchWrites.getLocks().size());
      assertEquals(AND, treeWithOneBranchReadsOneBranchWrites.getOperator());
      assertMultiRecordSecurityLock((MultiRecordSecurityLock) treeWithOneBranchReadsOneBranchWrites.getLocks().get(0), OR, "A", "B");

      MultiRecordSecurityLock deepSparseTree = RecordSecurityLockFilters.filterForReadLockTree(List.of(
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
               new RecordSecurityLock().withFieldName("A").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
               new RecordSecurityLock().withFieldName("B").withLockScope(RecordSecurityLock.LockScope.WRITE)
            )),
            new RecordSecurityLock().withFieldName("C").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
            new RecordSecurityLock().withFieldName("D").withLockScope(RecordSecurityLock.LockScope.WRITE)
         )),
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
               new RecordSecurityLock().withFieldName("E").withLockScope(RecordSecurityLock.LockScope.WRITE),
               new RecordSecurityLock().withFieldName("F").withLockScope(RecordSecurityLock.LockScope.WRITE)
            )),
            new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
               new RecordSecurityLock().withFieldName("G").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
               new RecordSecurityLock().withFieldName("H").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
            ))
         )),
         new RecordSecurityLock().withFieldName("I").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE),
         new RecordSecurityLock().withFieldName("J").withLockScope(RecordSecurityLock.LockScope.WRITE)
      ));

      assertEquals(3, deepSparseTree.getLocks().size());
      assertEquals(AND, deepSparseTree.getOperator());
      MultiRecordSecurityLock deepChild0 = (MultiRecordSecurityLock) deepSparseTree.getLocks().get(0);
      assertEquals(2, deepChild0.getLocks().size());
      assertEquals(OR, deepChild0.getOperator());
      MultiRecordSecurityLock deepGrandChild0 = (MultiRecordSecurityLock) deepChild0.getLocks().get(0);
      assertMultiRecordSecurityLock(deepGrandChild0, AND, "A");
      assertEquals("C", deepChild0.getLocks().get(1).getFieldName());

      MultiRecordSecurityLock deepChild1 = (MultiRecordSecurityLock) deepSparseTree.getLocks().get(1);
      assertEquals(1, deepChild1.getLocks().size());
      assertEquals(OR, deepChild1.getOperator());
      MultiRecordSecurityLock deepGrandChild1 = (MultiRecordSecurityLock) deepChild1.getLocks().get(0);
      assertMultiRecordSecurityLock(deepGrandChild1, AND, "G", "H");

      assertEquals("I", deepSparseTree.getLocks().get(2).getFieldName());
   }



   /*******************************************************************************
    ** An inapplicable empty AND branch must not become a successful alternative
    ** inside OR. Retained scopes keep their nested operators and source tree.
    *******************************************************************************/
   @Test
   void testReadAndWriteScopePruningPreservesNestedOperators() throws Exception
   {
      MultiRecordSecurityLock readBranch = new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new RecordSecurityLock().withFieldName("readOnlyA").withLockScope(RecordSecurityLock.LockScope.READ)
         ))
      ));
      MultiRecordSecurityLock mixedBranch = new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
         new RecordSecurityLock().withFieldName("writeOnlyB").withLockScope(RecordSecurityLock.LockScope.WRITE),
         new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
            new RecordSecurityLock().withFieldName("readOnlyC").withLockScope(RecordSecurityLock.LockScope.READ),
            new RecordSecurityLock().withFieldName("sharedD").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
         ))
      ));
      MultiRecordSecurityLock original = new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(readBranch, mixedBranch));
      String originalJson = JsonUtils.toJson(original);

      MultiRecordSecurityLock writes = RecordSecurityLockFilters.filterForWriteLockTree(List.of(original));
      assertEquals(AND, writes.getOperator());
      assertEquals(1, writes.getLocks().size());
      MultiRecordSecurityLock writeAlternatives = (MultiRecordSecurityLock) writes.getLocks().get(0);
      assertEquals(OR, writeAlternatives.getOperator());
      assertEquals(1, writeAlternatives.getLocks().size());
      MultiRecordSecurityLock writeConjunction = (MultiRecordSecurityLock) writeAlternatives.getLocks().get(0);
      assertEquals(AND, writeConjunction.getOperator());
      assertEquals(2, writeConjunction.getLocks().size());
      assertEquals("writeOnlyB", writeConjunction.getLocks().get(0).getFieldName());
      assertMultiRecordSecurityLock((MultiRecordSecurityLock) writeConjunction.getLocks().get(1), OR, "sharedD");

      MultiRecordSecurityLock reads = RecordSecurityLockFilters.filterForReadLockTree(List.of(original));
      assertEquals(AND, reads.getOperator());
      assertEquals(1, reads.getLocks().size());
      MultiRecordSecurityLock readAlternatives = (MultiRecordSecurityLock) reads.getLocks().get(0);
      assertEquals(OR, readAlternatives.getOperator());
      assertEquals(2, readAlternatives.getLocks().size());
      MultiRecordSecurityLock readConjunction = (MultiRecordSecurityLock) readAlternatives.getLocks().get(0);
      assertEquals(AND, readConjunction.getOperator());
      assertEquals(1, readConjunction.getLocks().size());
      assertMultiRecordSecurityLock((MultiRecordSecurityLock) readConjunction.getLocks().get(0), OR, "readOnlyA");
      MultiRecordSecurityLock mixedReadConjunction = (MultiRecordSecurityLock) readAlternatives.getLocks().get(1);
      assertEquals(AND, mixedReadConjunction.getOperator());
      assertEquals(1, mixedReadConjunction.getLocks().size());
      assertMultiRecordSecurityLock((MultiRecordSecurityLock) mixedReadConjunction.getLocks().get(0), OR, "readOnlyC", "sharedD");
      assertEquals(originalJson, JsonUtils.toJson(original));
   }



   /*******************************************************************************
    ** Removal of the final applicable leaf must prune every empty ancestor.
    *******************************************************************************/
   @Test
   void testWriteScopePrunesEntireReadOnlySubtree()
   {
      MultiRecordSecurityLock readBranch = new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
         new MultiRecordSecurityLock().withOperator(AND).withLocks(List.of(
            new MultiRecordSecurityLock().withOperator(OR).withLocks(List.of(
               new RecordSecurityLock().withFieldName("readOnly").withLockScope(RecordSecurityLock.LockScope.READ)
            ))
         ))
      ));
      assertMultiRecordSecurityLock(RecordSecurityLockFilters.filterForWriteLockTree(List.of(readBranch)), AND);
      assertEquals(1, readBranch.getLocks().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMultiRecordSecurityLock(MultiRecordSecurityLock lock, MultiRecordSecurityLock.BooleanOperator operator, String... lockFieldNames)
   {
      assertEquals(lockFieldNames.length, lock.getLocks().size());
      assertEquals(operator, lock.getOperator());

      for(int i = 0; i < lockFieldNames.length; i++)
      {
         assertEquals(lockFieldNames[i], lock.getLocks().get(i).getFieldName());
      }
   }

}
