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

package com.kingsrook.qqq.backend.module.filesystem.s3;


import java.util.List;
import java.util.UUID;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.AbstractS3Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for S3BackendModule
 *******************************************************************************/
public class S3BackendModuleTest extends BaseS3Test
{
   private static final String PATH_THAT_WONT_EXIST = "some/path/that/wont/exist";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testListFiles() throws QException
   {
      QInstance        qInstance = TestUtils.defineInstance();
      QTableMetaData   table     = qInstance.getTable(TestUtils.TABLE_NAME_BLOB_S3);
      QBackendMetaData backend   = qInstance.getBackendForTable(table.getName());

      //////////////////////////////////////////////////////
      // set up the backend module (e.g., for localstack) //
      //////////////////////////////////////////////////////
      S3BackendModule  s3BackendModule = new S3BackendModule();
      AbstractS3Action actionBase      = (AbstractS3Action) s3BackendModule.getActionBase();
      actionBase.setS3Utils(getS3Utils());

      //////////////////////////////////////////////////////////
      // with no filter given, all (3) files should come back //
      //////////////////////////////////////////////////////////
      List<S3ObjectSummary> files = actionBase.listFiles(table, backend);
      assertEquals(3, files.size());

      /////////////////////////////////////////
      // filter for a file name that's found //
      /////////////////////////////////////////
      files = actionBase.listFiles(table, backend, "BLOB-2.txt");
      assertEquals(1, files.size());
      assertThat(files.get(0).getKey()).contains("BLOB-2.txt");

      files = actionBase.listFiles(table, backend, "BLOB-1.txt");
      assertEquals(1, files.size());
      assertThat(files.get(0).getKey()).contains("BLOB-1.txt");

      ///////////////////////////
      // not supported anymore //
      ///////////////////////////
      // ///////////////////////////////////
      // // filter for 2 names that exist //
      // ///////////////////////////////////
      // files = actionBase.listFiles(table, backend, new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.IN, "BLOB-1.txt", "BLOB-2.txt")));
      // assertEquals(2, files.size());

      /////////////////////////////////////////////
      // filter for a file name that isn't found //
      /////////////////////////////////////////////
      files = actionBase.listFiles(table, backend, "NOT-FOUND.txt");
      assertEquals(0, files.size());

      ///////////////////////////
      // not supported anymore //
      ///////////////////////////
      // files = actionBase.listFiles(table, backend, new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.IN, "BLOB-2.txt", "NOT-FOUND.txt")));
      // assertEquals(1, files.size());

      ///////////////////////////
      // not supported anymore //
      ///////////////////////////
      // ////////////////////////////////////////////////////
      // // 2 criteria, and'ed, and can't match, so find 0 //
      // ////////////////////////////////////////////////////
      // files = actionBase.listFiles(table, backend, new QQueryFilter(
      //    new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt"),
      //    new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-2.txt")));
      // assertEquals(0, files.size());

      // //////////////////////////////////////////////////
      // // 2 criteria, or'ed, and both match, so find 2 //
      // //////////////////////////////////////////////////
      // files = actionBase.listFiles(table, backend, new QQueryFilter(
      //    new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt"),
      //    new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-2.txt"))
      //    .withBooleanOperator(QQueryFilter.BooleanOperator.OR));
      // assertEquals(2, files.size());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // note that we used to try unsupported filters here, expecting them to throw - but those are           //
      // more-or-less now implemented in the base class's query method, so, no longer expected to throw here. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDeleteFile() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_S3);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files - then delete one, then re-list, and assert that we have one fewer //
      /////////////////////////////////////////////////////////////////////////////////////////////
      List<S3ObjectSummary> s3ObjectSummariesBeforeDelete = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");

      S3BackendModule  s3BackendModule = new S3BackendModule();
      AbstractS3Action actionBase      = (AbstractS3Action) s3BackendModule.getActionBase();
      actionBase.setS3Utils(getS3Utils());
      actionBase.deleteFile(table, s3ObjectSummariesBeforeDelete.get(0).getKey());

      List<S3ObjectSummary> s3ObjectSummariesAfterDelete = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");
      Assertions.assertEquals(s3ObjectSummariesBeforeDelete.size() - 1, s3ObjectSummariesAfterDelete.size(),
         "Should be one fewer file listed after deleting one.");
   }



   /*******************************************************************************
    ** Regression check on the s3 delete method - if the file name has duplicated
    ** slashes and/or leading slash(es), those need stripped.  So, create that
    ** situation and assert delete works.
    *******************************************************************************/
   @Test
   public void testDeleteFileExtraSlashes() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_S3);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files - then delete one, then re-list, and assert that we have one fewer //
      /////////////////////////////////////////////////////////////////////////////////////////////
      List<S3ObjectSummary> s3ObjectSummariesBeforeDelete = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");

      S3BackendModule  s3BackendModule = new S3BackendModule();
      AbstractS3Action actionBase      = (AbstractS3Action) s3BackendModule.getActionBase();
      actionBase.setS3Utils(getS3Utils());
      String path = "//" + s3ObjectSummariesBeforeDelete.get(0).getKey().replaceAll("/", "//");
      actionBase.deleteFile(table, "//" + path);

      List<S3ObjectSummary> s3ObjectSummariesAfterDelete = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");
      Assertions.assertEquals(s3ObjectSummariesBeforeDelete.size() - 1, s3ObjectSummariesAfterDelete.size(),
         "Should be one fewer file listed after deleting one.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDeleteFileDoesNotExist() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_S3);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files - then try to delete a fake path, then re-list, and assert that we have the same count //
      // note, we'd like to detect the non-delete, but there's no such info back from aws it appears?                //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<S3ObjectSummary> s3ObjectSummariesBeforeDelete = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");

      S3BackendModule  s3BackendModule = new S3BackendModule();
      AbstractS3Action actionBase      = (AbstractS3Action) s3BackendModule.getActionBase();
      actionBase.setS3Utils(getS3Utils());
      actionBase.deleteFile(table, PATH_THAT_WONT_EXIST);

      List<S3ObjectSummary> s3ObjectSummariesAfterDelete = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");
      Assertions.assertEquals(s3ObjectSummariesBeforeDelete.size(), s3ObjectSummariesAfterDelete.size(),
         "Should be same number of files after deleting bogus path");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMoveFile() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_S3);
      String         subPath   = TEST_FOLDER + "/" + SUB_FOLDER;

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files (non-recursively) - then move one into a sub-folder, then re-list, and    //
      // assert that we have one fewer then list again including sub-folders, and see the changed count //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      List<S3ObjectSummary> s3ObjectSummariesBeforeMove            = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");
      List<S3ObjectSummary> s3ObjectSummariesInSubFolderBeforeMove = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, subPath, "");
      List<S3ObjectSummary> s3ObjectSummariesRecursiveBeforeMove   = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/**");

      S3BackendModule  s3BackendModule = new S3BackendModule();
      AbstractS3Action actionBase      = (AbstractS3Action) s3BackendModule.getActionBase();
      actionBase.setS3Utils(getS3Utils());
      String key = s3ObjectSummariesBeforeMove.get(0).getKey();
      actionBase.moveFile(qInstance, table, key, key.replaceFirst(TEST_FOLDER, subPath));

      List<S3ObjectSummary> s3ObjectSummariesAfterMove            = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "");
      List<S3ObjectSummary> s3ObjectSummariesRecursiveAfterMove   = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/**");
      List<S3ObjectSummary> s3ObjectSummariesInSubFolderAfterMove = getS3Utils().listObjectsInBucketMatchingGlob(BUCKET_NAME, subPath, "");

      Assertions.assertEquals(s3ObjectSummariesBeforeMove.size() - 1, s3ObjectSummariesAfterMove.size(),
         "Should be one fewer file in the non-recursive listing after moving one.");
      Assertions.assertEquals(s3ObjectSummariesRecursiveBeforeMove.size(), s3ObjectSummariesRecursiveAfterMove.size(),
         "Should be same number of files in the recursive listing before and after the move");
      Assertions.assertEquals(s3ObjectSummariesInSubFolderBeforeMove.size() + 1, s3ObjectSummariesInSubFolderAfterMove.size(),
         "Should be one move file in the sub-folder listing after moving one.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMoveFileDoesNotExit() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_S3);
      String         subPath   = TEST_FOLDER + "/" + SUB_FOLDER;

      S3BackendModule  s3BackendModule = new S3BackendModule();
      AbstractS3Action actionBase      = (AbstractS3Action) s3BackendModule.getActionBase();
      actionBase.setS3Utils(getS3Utils());

      Assertions.assertThrows(FilesystemException.class, () ->
         actionBase.moveFile(qInstance, table, PATH_THAT_WONT_EXIST, subPath + "/" + UUID.randomUUID())
      );
   }

}