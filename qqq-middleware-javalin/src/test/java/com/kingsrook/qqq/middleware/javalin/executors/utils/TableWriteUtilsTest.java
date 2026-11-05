/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A typed not-found record error is a not-found exception (404 at the edge);
 ** every other error shape keeps its existing mapping. No message matching.
 *******************************************************************************/
class TableWriteUtilsTest
{
   /*******************************************************************************
    ** Table metadata is needed for the message label.
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      QContext.init(TestUtils.defineInstance(), new QSystemUserSession());
   }



   /*******************************************************************************
    ** A record that was not found (absent, or hidden by record security: the update
    ** action reports both the same way) is a not-found exception.
    *******************************************************************************/
   @Test
   void notFoundRecordErrorIsNotFoundException()
   {
      QRecord record = new QRecord().withError(new NotFoundStatusMessage("No record was found to update for Id = 999999"));
      QNotFoundException exception = assertThrows(QNotFoundException.class, () -> TableWriteUtils.throwIfRecordErrors("updating", "person", record));
      assertTrue(exception.getMessage().contains("No record was found"));
   }



   /*******************************************************************************
    ** Bad input remains a bad request.
    *******************************************************************************/
   @Test
   void badInputStaysBadRequest()
   {
      QRecord record = new QRecord().withError(new BadInputStatusMessage("First name is required"));
      assertThrows(QBadRequestException.class, () -> TableWriteUtils.throwIfRecordErrors("updating", "person", record));
   }



   /*******************************************************************************
    ** A mix of not-found and anything else is NOT reported as not-found, and a
    ** permission-denied record error keeps its existing (non not-found) mapping.
    *******************************************************************************/
   @Test
   void mixedAndOtherErrorsAreNotNotFound()
   {
      QRecord mixed = new QRecord().withError(new NotFoundStatusMessage("missing")).withError(new BadInputStatusMessage("bad"));
      QUserFacingException mixedException = assertThrows(QUserFacingException.class, () -> TableWriteUtils.throwIfRecordErrors("updating", "person", mixed));
      assertFalse(mixedException instanceof QNotFoundException);
      assertFalse(mixedException instanceof QBadRequestException);

      QRecord denied = new QRecord().withError(new PermissionDeniedMessage("denied"));
      QUserFacingException deniedException = assertThrows(QUserFacingException.class, () -> TableWriteUtils.throwIfRecordErrors("updating", "person", denied));
      assertFalse(deniedException instanceof QNotFoundException);
   }



   /*******************************************************************************
    ** No errors: nothing is thrown.
    *******************************************************************************/
   @Test
   void noErrorsNoException()
   {
      assertDoesNotThrow(() -> TableWriteUtils.throwIfRecordErrors("updating", "person", new QRecord()));
   }
}
