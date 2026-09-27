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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunBackendStepActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      TestCallback        callback = new TestCallback();
      RunBackendStepInput request  = new RunBackendStepInput();
      request.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE);
      request.setStepName("prepare");
      request.setCallback(callback);
      RunBackendStepOutput result = new RunBackendStepAction().execute(request);
      assertNotNull(result);
      assertTrue(result.getRecords().stream().allMatch(r -> r.getValues().containsKey("mockValue")), "records should have a mock value");
      assertTrue(result.getValues().containsKey("mockValue"), "result object should have a mock value");
      assertEquals("ABC", result.getValues().get("greetingPrefix"), "result object should have value from our callback");
      assertTrue(callback.wasCalledForQueryFilter, "callback was used for query filter");
      assertTrue(callback.wasCalledForFieldValues, "callback was used for field values");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMinMaxInputRecords() throws QException
   {
      ////////////////////////////////////////////
      // put a min-input-records on the process //
      ////////////////////////////////////////////
      QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withMinInputRecords(5);

      //////////////////////////////////////////////////////////////////////////////////////
      // insert fewer than that min - then run w/ non-filtered filter, and assert we fail //
      //////////////////////////////////////////////////////////////////////////////////////
      for(int i = 0; i < 3; i++)
      {
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("firstName", String.valueOf(i))));
      }

      Supplier<RunBackendStepInput> inputSupplier = () ->
      {
         RunBackendStepInput input = new RunBackendStepInput();
         input.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE);
         input.setStepName("prepare");
         input.setCallback(QProcessCallbackFactory.forFilter(new QQueryFilter()));
         return (input);
      };

      assertThatThrownBy(() -> new RunBackendStepAction().execute(inputSupplier.get()))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Too few records");

      ////////////////////////////////////////////////////
      // insert a few more - and then it should succeed //
      ////////////////////////////////////////////////////
      for(int i = 3; i < 10; i++)
      {
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("firstName", String.valueOf(i))));
      }

      new RunBackendStepAction().execute(inputSupplier.get());

      ////////////////////////////////////////////////////////////
      // now put a max on the process, and it should fail again //
      ////////////////////////////////////////////////////////////
      QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withMaxInputRecords(8);

      assertThatThrownBy(() -> new RunBackendStepAction().execute(inputSupplier.get()))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Too many records");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class TestCallback implements QProcessCallback
   {
      private boolean wasCalledForQueryFilter = false;
      private boolean wasCalledForFieldValues = false;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QQueryFilter getQueryFilter()
      {
         wasCalledForQueryFilter = true;
         return (new QQueryFilter());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
      {
         wasCalledForFieldValues = true;
         Map<String, Serializable> rs = new HashMap<>();
         for(QFieldMetaData field : fields)
         {
            rs.put(field.getName(), switch(field.getType())
            {
               case STRING -> "ABC";
               case INTEGER -> 42;
               case LONG -> 42L;
               case DECIMAL -> new BigDecimal("47");
               case BOOLEAN -> true;
               case DATE, TIME, DATE_TIME -> null;
               case TEXT -> """
                  ABC
                  XYZ""";
               case HTML -> "<b>Oh my</b>";
               case PASSWORD -> "myPa**word";
               case BLOB -> new byte[] { 1, 2, 3, 4 };
            });
         }
         return (rs);
      }
   }
}
