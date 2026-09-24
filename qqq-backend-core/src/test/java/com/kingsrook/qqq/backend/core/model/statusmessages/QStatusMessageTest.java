/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.statusmessages;


import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for the QStatusMessage hierarchy.
 **
 ** Covers: QStatusMessage (via concrete subtypes), QErrorMessage,
 **         BadInputStatusMessage, NotFoundStatusMessage,
 **         PermissionDeniedMessage, QWarningMessage, SystemErrorStatusMessage,
 **         DuplicateKeyBadInputStatusMessage.
 *******************************************************************************/
class QStatusMessageTest
{

   /*******************************************************************************
    ** getMessage should return the string passed to the constructor.
    *******************************************************************************/
   @Test
   void testGetMessage_returnsConstructorValue()
   {
      BadInputStatusMessage msg = new BadInputStatusMessage("required field missing");
      assertEquals("required field missing", msg.getMessage());
   }



   /*******************************************************************************
    ** toString should return the message text (used in log/display contexts).
    *******************************************************************************/
   @Test
   void testToString_returnsMessage()
   {
      BadInputStatusMessage msg = new BadInputStatusMessage("bad value");
      assertEquals("bad value", msg.toString());
   }



   /*******************************************************************************
    ** A null message should be stored and returned as null (not throw NPE).
    *******************************************************************************/
   @Test
   void testConstructor_nullMessage_storedAsNull()
   {
      BadInputStatusMessage msg = new BadInputStatusMessage(null);
      assertNull(msg.getMessage());
      assertNull(msg.toString());
   }



   /*******************************************************************************
    ** BadInputStatusMessage should be an instance of QErrorMessage (HTTP 400 signal).
    *******************************************************************************/
   @Test
   void testBadInputStatusMessage_isQErrorMessage()
   {
      BadInputStatusMessage msg = new BadInputStatusMessage("oops");
      assertThat(msg).isInstanceOf(QErrorMessage.class);
      assertThat(msg).isInstanceOf(QStatusMessage.class);
   }



   /*******************************************************************************
    ** NotFoundStatusMessage should be an instance of QErrorMessage.
    *******************************************************************************/
   @Test
   void testNotFoundStatusMessage_isQErrorMessage()
   {
      NotFoundStatusMessage msg = new NotFoundStatusMessage("record not found");
      assertThat(msg).isInstanceOf(QErrorMessage.class);
      assertEquals("record not found", msg.getMessage());
   }



   /*******************************************************************************
    ** PermissionDeniedMessage should be an instance of QErrorMessage.
    *******************************************************************************/
   @Test
   void testPermissionDeniedMessage_isQErrorMessage()
   {
      PermissionDeniedMessage msg = new PermissionDeniedMessage("access denied");
      assertThat(msg).isInstanceOf(QErrorMessage.class);
      assertEquals("access denied", msg.getMessage());
   }



   /*******************************************************************************
    ** QWarningMessage should be a QStatusMessage but NOT a QErrorMessage.
    *******************************************************************************/
   @Test
   void testQWarningMessage_isStatusMessageNotErrorMessage()
   {
      QWarningMessage msg = new QWarningMessage("soft warning");
      assertThat(msg).isInstanceOf(QStatusMessage.class);
      assertThat(msg).isNotInstanceOf(QErrorMessage.class);
      assertEquals("soft warning", msg.getMessage());
   }



   /*******************************************************************************
    ** SystemErrorStatusMessage should be a QErrorMessage (HTTP 500 signal).
    *******************************************************************************/
   @Test
   void testSystemErrorStatusMessage_isQErrorMessage()
   {
      SystemErrorStatusMessage msg = new SystemErrorStatusMessage("internal server error");
      assertThat(msg).isInstanceOf(QErrorMessage.class);
      assertEquals("internal server error", msg.getMessage());
   }



   /*******************************************************************************
    ** DuplicateKeyBadInputStatusMessage should extend BadInputStatusMessage.
    *******************************************************************************/
   @Test
   void testDuplicateKeyBadInputStatusMessage_extendsBadInput()
   {
      DuplicateKeyBadInputStatusMessage msg = new DuplicateKeyBadInputStatusMessage("duplicate key");
      assertThat(msg).isInstanceOf(BadInputStatusMessage.class);
      assertThat(msg).isInstanceOf(QErrorMessage.class);
      assertEquals("duplicate key", msg.getMessage());
   }



   /*******************************************************************************
    ** Two distinct message objects with identical text should NOT be equal
    ** (no equals() override — identity comparison by default).
    *******************************************************************************/
   @Test
   void testEquality_distinctObjects_notEqual()
   {
      BadInputStatusMessage a = new BadInputStatusMessage("same");
      BadInputStatusMessage b = new BadInputStatusMessage("same");
      assertThat(a).isNotSameAs(b);
   }

}
