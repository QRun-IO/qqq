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

package com.kingsrook.qqq.middleware.javalin;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Real legacy writes distinguish client validation errors from system failures.
 *******************************************************************************/
class WriteErrorStatusHttpTest extends QJavalinTestBase
{
   private UnirestInstance client;



   /*******************************************************************************
    ** Use standard required validation and a declared customizer for server errors.
    *******************************************************************************/
   @BeforeEach
   void configure() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QTableMetaData table = instance.getTable("person");
      table.getField("firstName").setIsRequired(true);
      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(WriteErrors.class));
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(WriteErrors.class));
      restartServerWithInstance(instance);
      client = Unirest.spawnInstance();
   }



   /*******************************************************************************
    ** Each restarted fixture owns its HTTP pool.
    *******************************************************************************/
   @AfterEach
   void closeClient()
   {
      if(client != null)
      {
         client.close();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsertBadInputReturns400() throws Exception
   {
      List<List<String>> before = snapshot();
      HttpResponse<String> response = client.post(BASE_URL + "/data/person").field("lastName", "Invalid").asString();
      assertResponseAndUnchanged(response, 400, "Error inserting Person: Missing value in required field: First Name", before);
   }



   /*******************************************************************************
    ** An explicit empty PATCH value invokes required-field validation.
    *******************************************************************************/
   @Test
   void testUpdateBadInputReturns400() throws Exception
   {
      List<List<String>> before = snapshot();
      HttpResponse<String> response = client.patch(BASE_URL + "/data/person/1").field("firstName", "").asString();
      assertResponseAndUnchanged(response, 400, "Error updating Person: Missing value in required field: First Name", before);
   }



   /*******************************************************************************
    ** A bad-input message cannot downgrade an accompanying system or unknown error.
    *******************************************************************************/
   @Test
   void testInsertSystemMixedAndUnknownErrorsRemain500() throws Exception
   {
      for(String mode : List.of("system", "mixed", "unknown"))
      {
         List<List<String>> before = snapshot();
         HttpResponse<String> response = client.post(BASE_URL + "/data/person").field("firstName", mode).asString();
         assertResponseAndUnchanged(response, 500, "Error inserting Person: " + expectedError(mode), before);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateSystemMixedAndUnknownErrorsRemain500() throws Exception
   {
      for(String mode : List.of("system", "mixed", "unknown"))
      {
         List<List<String>> before = snapshot();
         HttpResponse<String> response = client.patch(BASE_URL + "/data/person/1").field("firstName", mode).asString();
         assertResponseAndUnchanged(response, 500, "Error updating Person: " + expectedError(mode), before);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String expectedError(String mode)
   {
      return switch(mode)
      {
         case "mixed" -> "Invalid input and System failed";
         case "unknown" -> "Unknown failure";
         default -> "System failed";
      };
   }



   /*******************************************************************************
    ** Preserve the complete response message and verify storage separately.
    *******************************************************************************/
   private void assertResponseAndUnchanged(HttpResponse<String> response, int status, String error, List<List<String>> before) throws Exception
   {
      List<List<String>> after = snapshot();
      assertAll(() -> assertEquals(status, response.getStatus(), response.getBody()),
         () -> assertEquals(error, JsonUtils.toJSONObject(response.getBody()).getString("error")),
         () -> assertEquals(before, after));
   }



   /*******************************************************************************
    ** Direct SQL prevents an error response from concealing partial mutation.
    *******************************************************************************/
   private List<List<String>> snapshot() throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend());
          Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM person ORDER BY id"))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Produce framework record error types through the real pre-write extension.
    *******************************************************************************/
   public static class WriteErrors implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecords)
      {
         for(QRecord record : records)
         {
            String mode = record.getValueString("firstName");
            if("mixed".equals(mode))
            {
               record.addError(new BadInputStatusMessage("Invalid input"));
            }
            if("system".equals(mode) || "mixed".equals(mode))
            {
               record.addError(new SystemErrorStatusMessage("System failed"));
            }
            if("unknown".equals(mode))
            {
               record.addError(new UnknownError());
            }
         }
         return records;
      }
   }



   /*******************************************************************************
    ** An application-defined error outside the known bad-input hierarchy.
    *******************************************************************************/
   private static class UnknownError extends QErrorMessage
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      private UnknownError()
      {
         super("Unknown failure");
      }
   }
}
