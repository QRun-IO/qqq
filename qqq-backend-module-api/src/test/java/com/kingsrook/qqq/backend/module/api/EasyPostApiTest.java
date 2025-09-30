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

package com.kingsrook.qqq.backend.module.api;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 **
 *******************************************************************************/
@DisabledOnOs(OS.LINUX)
public class EasyPostApiTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(EasyPostApiTest.class);



   /*******************************************************************************
    ** Supported Tracking Numbers (and their statuses)
    **
    ** EZ1000000001 : pre-transit
    ** EZ2000000002 : in transit
    ** EZ3000000003 : out for delivery
    ** EZ4000000004 : delivered
    ** EZ5000000005 : return to sender
    ** EZ6000000006 : failure
    ** EZ7000000007 : unknown
    **
    *******************************************************************************/
   @Test
   void testPostTrackerSuccess() throws QException
   {
      if(!StringUtils.hasContent(((APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.EASYPOST_BACKEND_NAME)).getApiKey()))
      {
         LOG.warn("Missing API Key for easypost backend (env.EASYPOST_API_KEY) - unable to run testPostTrackerSuccess");
         return;
      }

      QRecord record = new QRecord()
         .withValue("__ignoreMe", "123")
         .withValue("carrier", "USPS")
         .withValue("trackingNo", "EZ4000000004");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("easypostTracker");
      insertInput.setRecords(List.of(record));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      QRecord outputRecord = insertOutput.getRecords().get(0);
      assertNotNull(outputRecord.getValue("id"), "Should get a tracker id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostMultiple() throws QException
   {
      if(!StringUtils.hasContent(((APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.EASYPOST_BACKEND_NAME)).getApiKey()))
      {
         LOG.warn("Missing API Key for easypost backend (env.EASYPOST_API_KEY) - unable to run testPostMultiple");
         return;
      }

      QRecord record1 = new QRecord().withValue("carrier", "USPS").withValue("trackingNo", "EZ1000000001");
      QRecord record2 = new QRecord().withValue("carrier", "USPS").withValue("trackingNo", "EZ2000000002");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("easypostTracker");
      insertInput.setRecords(List.of(record1, record2));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      QRecord outputRecord0 = insertOutput.getRecords().get(0);
      assertNotNull(outputRecord0.getValue("id"), "Should get a tracker id");

      QRecord outputRecord1 = insertOutput.getRecords().get(1);
      assertNotNull(outputRecord1.getValue("id"), "Should get a tracker id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostTrackerEmptyInput() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("easypostTracker");
      insertInput.setRecords(List.of());
      new InsertAction().execute(insertInput);

      ////////////////////////////////////
      // just make sure we don't throw. //
      ////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostTrackerBadApiKey() throws QException
   {
      QInstance        instance = TestUtils.defineInstance();
      QBackendMetaData backend  = instance.getBackend(TestUtils.EASYPOST_BACKEND_NAME);
      ((APIBackendMetaData) backend).setApiKey("not-valid");
      reInitInstanceInContext(instance);

      QRecord record = new QRecord()
         .withValue("carrier", "USPS")
         .withValue("trackingNo", "EZ1000000001");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("easypostTracker");
      insertInput.setRecords(List.of(record));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      QRecord outputRecord = insertOutput.getRecords().get(0);
      assertNull(outputRecord.getValue("id"), "Should not get a tracker id");
      assertThat(outputRecord.getErrors()).isNotNull().isNotEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostTrackerError() throws QException
   {
      QRecord record = new QRecord()
         .withValue("carrier", "USPS")
         .withValue("trackingNo", "Not-Valid-Tracking-No");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("easypostTracker");
      insertInput.setRecords(List.of(record));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      QRecord outputRecord = insertOutput.getRecords().get(0);
      assertNull(outputRecord.getValue("id"), "Should not get a tracker id");
      assertThat(outputRecord.getErrors()).isNotNull().isNotEmpty();
   }

}
