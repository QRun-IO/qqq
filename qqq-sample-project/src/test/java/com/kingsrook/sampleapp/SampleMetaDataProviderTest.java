/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.module.filesystem.local.actions.FilesystemQueryAction;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
public class SampleMetaDataProviderTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCityFileTable() throws Exception
   {
      QTableMetaData fileTable       = SampleMetaDataProvider.defineTableCityFile();
      File           destinationFile = copyTestFileToRandomNameUnderTable(fileTable);

      try
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(fileTable.getName());

         QueryOutput queryOutput = new FilesystemQueryAction().execute(queryInput);
         System.out.println(queryOutput);
         Assertions.assertEquals(3, queryOutput.getRecords().size(), "Should load all records from the file");
      }
      finally
      {
         destinationFile.delete();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private File copyTestFileToRandomNameUnderTable(QTableMetaData fedExTable) throws IOException
   {
      File destinationDir = new File(SampleMetaDataProvider.defineFilesystemBackend().getBasePath() + File.separator
         + ((FilesystemTableBackendDetails) fedExTable.getBackendDetails()).getBasePath());
      destinationDir.mkdirs();
      File destinationFile = new File(destinationDir.getAbsolutePath() + File.separator + UUID.randomUUID());

      FileUtils.writeStringToFile(destinationFile, """
         id,name,state
         1,Chester,IL
         2,Red Bud,IL
         3,Sparta,IL""", StandardCharsets.UTF_8);

      return destinationFile;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testGreetProcess() throws Exception
   {
      QInstance       qInstance   = SampleMetaDataProvider.defineTestInstance();
      QTableMetaData  personTable = SampleMetaDataProvider.defineTablePerson();
      RunProcessInput request     = new RunProcessInput();
      request.setProcessName(SampleMetaDataProvider.PROCESS_NAME_GREET);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(personTable.getName());
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      request.setRecords(queryOutput.getRecords());
      request.addValue(MockBackendStep.FIELD_GREETING_PREFIX, "Hello");
      request.addValue(MockBackendStep.FIELD_GREETING_SUFFIX, "sir");

      RunProcessOutput result = new RunProcessAction().execute(request);
      assertNotNull(result);
      assertTrue(result.getRecords().stream().allMatch(r -> r.getValues().containsKey("id")), "records should have an id, set by the process");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testThrowProcess() throws Exception
   {
      QInstance       qInstance = SampleMetaDataProvider.defineTestInstance();
      RunProcessInput request   = new RunProcessInput();
      request.setProcessName(SampleMetaDataProvider.PROCESS_NAME_SIMPLE_THROW);
      request.addValue(SampleMetaDataProvider.ThrowerStep.FIELD_SLEEP_MILLIS, 10);

      assertThrows(QException.class, () ->
      {
         new RunProcessAction().execute(request);
      });
   }

}
