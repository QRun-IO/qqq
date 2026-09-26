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

package com.kingsrook.qqq.backend.module.filesystem.local;


import java.io.File;
import java.io.IOException;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.local.actions.AbstractFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.local.actions.FilesystemActionTest;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for FilesystemBackendModule
 *******************************************************************************/
public class FilesystemBackendModuleTest
{
   private static final String PATH_THAT_WONT_EXIST = "some/path/that/wont/exist";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws IOException
   {
      new FilesystemActionTest().primeFilesystem();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   public void afterEach() throws Exception
   {
      new FilesystemActionTest().cleanFilesystem();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testListFiles() throws QException
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);

      AbstractFilesystemAction abstractFilesystemAction = new AbstractFilesystemAction();
      QBackendMetaData         backend                  = qInstance.getBackendForTable(table.getName());

      //////////////////////////////////////////////////////////
      // with no filter given, all (3) files should come back //
      //////////////////////////////////////////////////////////
      List<File> files = abstractFilesystemAction.listFiles(table, backend);
      assertEquals(3, files.size());

      /////////////////////////////////////////
      // filter for a file name that's found //
      /////////////////////////////////////////
      files = abstractFilesystemAction.listFiles(table, backend, "BLOB-2.txt");
      assertEquals(1, files.size());
      assertEquals("BLOB-2.txt", files.get(0).getName());

      files = abstractFilesystemAction.listFiles(table, backend, "BLOB-1.txt");
      assertEquals(1, files.size());
      assertEquals("BLOB-1.txt", files.get(0).getName());

      ///////////////////////////
      // not supported anymore //
      ///////////////////////////
      // ///////////////////////////////////
      // // filter for 2 names that exist //
      // ///////////////////////////////////
      // files = abstractFilesystemAction.listFiles(table, backend, new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.IN, "BLOB-1.txt", "BLOB-2.txt")));
      // assertEquals(2, files.size());

      /////////////////////////////////////////////
      // filter for a file name that isn't found //
      /////////////////////////////////////////////
      files = abstractFilesystemAction.listFiles(table, backend, "NOT-FOUND.txt");
      assertEquals(0, files.size());

      ///////////////////////////
      // not supported anymore //
      ///////////////////////////
      // files = abstractFilesystemAction.listFiles(table, backend, new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.IN, "BLOB-2.txt", "NOT-FOUND.txt")));
      // assertEquals(1, files.size());

      ///////////////////////////
      // not supported anymore //
      ///////////////////////////
      // ////////////////////////////////////////////////////
      // // 2 criteria, and'ed, and can't match, so find 0 //
      // ////////////////////////////////////////////////////
      // files = abstractFilesystemAction.listFiles(table, backend, new QQueryFilter(
      //    new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt"),
      //    new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-2.txt")));
      // assertEquals(0, files.size());

      // //////////////////////////////////////////////////
      // // 2 criteria, or'ed, and both match, so find 2 //
      // //////////////////////////////////////////////////
      // files = abstractFilesystemAction.listFiles(table, backend, new QQueryFilter(
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
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files - then delete one, then re-list, and assert that we have one fewer //
      /////////////////////////////////////////////////////////////////////////////////////////////
      List<File> filesBeforeDelete = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));

      FilesystemBackendModule filesystemBackendModule = new FilesystemBackendModule();
      filesystemBackendModule.getActionBase().deleteFile(table, filesBeforeDelete.get(0).getAbsolutePath());

      List<File> filesAfterDelete = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));
      Assertions.assertEquals(filesBeforeDelete.size() - 1, filesAfterDelete.size(),
         "Should be one fewer file listed after deleting one.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDeleteFileDoesNotExist() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files - then try to delete a fake path, then re-list, and assert that we have the same count //
      // note, our contract is that as long as the file doesn't exist after calling delete (e.g., if it wasn't there //
      // to begin with, then we're okay, and don't expect an exception                                               //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<File> filesBeforeDelete = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));

      FilesystemBackendModule filesystemBackendModule = new FilesystemBackendModule();
      filesystemBackendModule.getActionBase().deleteFile(table, PATH_THAT_WONT_EXIST);

      List<File> filesAfterDelete = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));
      Assertions.assertEquals(filesBeforeDelete.size(), filesAfterDelete.size(),
         "Should be same number of files after deleting bogus path");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMoveFile() throws Exception
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      String         basePath  = ((FilesystemBackendMetaData) qInstance.getBackendForTable(table.getName())).getBasePath();
      String         subPath   = basePath + File.separator + "subdir";

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // first list the files (non-recursively) - then move one into a sub-folder, then re-list, and    //
      // assert that we have one fewer then list again including sub-folders, and see the changed count //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      List<File> filesBeforeMove = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));

      FilesystemBackendModule filesystemBackendModule = new FilesystemBackendModule();
      String                  originalFilePath        = filesBeforeMove.get(0).getAbsolutePath();
      String                  movedFilePath           = originalFilePath.replace(basePath, subPath);

      filesystemBackendModule.getActionBase().moveFile(qInstance, table, originalFilePath, movedFilePath);

      List<File> filesAfterMove = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));
      Assertions.assertEquals(filesBeforeMove.size() - 1, filesAfterMove.size(), "Should be one fewer file in the listing after moving one.");

      //////////////////////////////////////////////////////////////////////////
      // move the file back and assert that the count goes back to the before //
      //////////////////////////////////////////////////////////////////////////
      filesystemBackendModule.getActionBase().moveFile(qInstance, table, movedFilePath, originalFilePath);

      List<File> filesAfterMoveBack = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));
      Assertions.assertEquals(filesBeforeMove.size(), filesAfterMoveBack.size(), "Should be original number of files after moving back");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMoveFileDoesNotExit() throws Exception
   {
      QInstance      qInstance        = TestUtils.defineInstance();
      QTableMetaData table            = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      String         basePath         = ((FilesystemBackendMetaData) qInstance.getBackendForTable(table.getName())).getBasePath();
      String         subPath          = basePath + File.separator + "subdir";
      List<File>     filesBeforeMove  = new AbstractFilesystemAction().listFiles(table, qInstance.getBackendForTable(table.getName()));
      String         originalFilePath = filesBeforeMove.get(0).getAbsolutePath();
      String         movedFilePath    = originalFilePath.replace(basePath, subPath);

      Assertions.assertThrows(FilesystemException.class, () ->
      {
         FilesystemBackendModule filesystemBackendModule = new FilesystemBackendModule();
         filesystemBackendModule.getActionBase().moveFile(qInstance, table, PATH_THAT_WONT_EXIST, movedFilePath);
      });

   }

}