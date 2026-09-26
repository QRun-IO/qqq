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

package com.kingsrook.qqq.backend.core.instances.loaders;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MetaDataLoaderHelper 
 *******************************************************************************/
class MetaDataLoaderHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());

      writeFile("myTable", ".yaml", tempDirectory, """
         class: QTableMetaData
         version:  1
         name: myTable
         label: This is My Table
         primaryKeyField:  id
         fields:
            id:
               name: id
               type: INTEGER
            name:
               name: name
               type: STRING
            createDate:
               name: createDate
               type: DATE_TIME
         """);

      writeFile("yourTable", ".yaml", tempDirectory, """
         class: QTableMetaData
         version:  1
         name: yourTable
         label: Someone else's table
         primaryKeyField:  id
         fields:
            id:
               name: id
               type: INTEGER
            name:
               name: name
               type: STRING
         """);

      QInstance qInstance = new QInstance();
      MetaDataLoaderHelper.processAllMetaDataFilesInDirectory(qInstance, tempDirectory.toFile().getAbsolutePath());

      assertEquals(2, qInstance.getTables().size());

      QTableMetaData myTable = qInstance.getTable("myTable");
      assertEquals("This is My Table", myTable.getLabel());
      assertEquals(3, myTable.getFields().size());
      assertEquals("id", myTable.getField("id").getName());
      assertEquals(QFieldType.INTEGER, myTable.getField("id").getType());

      QTableMetaData yourTable = qInstance.getTable("yourTable");
      assertEquals("Someone else's table", yourTable.getLabel());
      assertEquals(2, yourTable.getFields().size());
   }



   /*******************************************************************************
    ** A missing directory must not produce an apparently successful empty load.
    *******************************************************************************/
   @Test
   void testMissingDirectoryFails() throws Exception
   {
      Path missing = Files.createTempDirectory(getClass().getSimpleName()).resolve("missing");
      assertThrows(QException.class, () -> MetaDataLoaderHelper.processAllMetaDataFilesInDirectory(new QInstance(), missing.toString()));
   }



   /*******************************************************************************
    ** A misspelled property must not silently become partial application metadata.
    *******************************************************************************/
   @Test
   void testInvalidMetadataFailsWithFileAndProperty() throws Exception
   {
      Path directory = Files.createTempDirectory(getClass().getSimpleName());
      writeFile("invalid-table", ".yaml", directory, "class: QTableMetaData\nname: person\nmisspelledProperty: true\n");
      QException failure = assertThrows(QException.class,
         () -> MetaDataLoaderHelper.processAllMetaDataFilesInDirectory(new QInstance(), directory.toString()));
      assertTrue(failure.getMessage().contains("invalid-table"));
      assertTrue(failure.getMessage().contains("misspelledProperty"));
   }



   /*******************************************************************************
    ** Problems from a registered nested loader must reach the stream boundary.
    *******************************************************************************/
   @Test
   void testNestedStepProblemsFailLoading() throws Exception
   {
      String valid = "class: QProcessMetaData\nname: nested\nstepList:\n- name: start\n  stepType: backend\n  code:\n    name: example.BackendStep\n";
      try(ByteArrayInputStream input = new ByteArrayInputStream(valid.getBytes(StandardCharsets.UTF_8)))
      {
         QProcessMetaData process = (QProcessMetaData) MetaDataLoaderHelper.readMetaDataFile(new QInstance(), input, "valid-process.yaml");
         assertEquals("example.BackendStep", process.getBackendStep("start").getCode().getName());
      }
      String invalid = valid.replace("  stepType: backend", "  stepType: backend\n  misspelledProperty: true");
      try(ByteArrayInputStream input = new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8)))
      {
         QException failure = assertThrows(QException.class,
            () -> MetaDataLoaderHelper.readMetaDataFile(new QInstance(), input, "invalid-process.yaml"));
         assertTrue(failure.getMessage().contains("invalid-process.yaml"));
         assertTrue(failure.getMessage().contains("stepList"));
         assertTrue(failure.getMessage().contains("misspelledProperty"));
      }
   }



   /*******************************************************************************
    ** Wrong collection/object shapes must not silently discard fields or elements.
    *******************************************************************************/
   @Test
   void testWrongShapesFailLoading() throws Exception
   {
      String[][] cases = {
         { "QAppMetaData", "sections", "not-a-list" },
         { "QTableMetaData", "fields", "not-a-map" },
         { "QTableMetaData", "icon", "not-an-object" },
         { "QProcessMetaData", "stepList", "[not-a-step]" }
      };
      for(String[] item : cases)
      {
         String yaml = "class: " + item[0] + "\nname: badShape\n" + item[1] + ": " + item[2] + "\n";
         try(ByteArrayInputStream input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
         {
            QException failure = assertThrows(QException.class,
               () -> MetaDataLoaderHelper.readMetaDataFile(new QInstance(), input, "bad-shape.yaml"), item[1]);
            assertTrue(failure.getMessage().contains("bad-shape.yaml"));
            assertTrue(failure.getMessage().contains(item[1]));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   void writeFile(String prefix, String suffix, Path directory, String content) throws IOException
   {
      FileUtils.writeStringToFile(File.createTempFile(prefix, suffix, directory.toFile()), content, StandardCharsets.UTF_8);
   }
}
