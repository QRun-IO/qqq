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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.executors.io.RecordFieldDownloadOutputInterface;


/*******************************************************************************
 ** Response wrapper for the record field download endpoint. Carries the binary
 ** content and metadata needed to produce the HTTP response.
 *******************************************************************************/
public class RecordFieldDownloadResponseV1 implements RecordFieldDownloadOutputInterface
{
   private byte[] bytes;
   private String contentType;
   private String filename;



   /*******************************************************************************
    ** Setter for bytes
    *******************************************************************************/
   @Override
   public void setBytes(byte[] bytes)
   {
      this.bytes = bytes;
   }



   /*******************************************************************************
    ** Setter for contentType
    *******************************************************************************/
   @Override
   public void setContentType(String contentType)
   {
      this.contentType = contentType;
   }



   /*******************************************************************************
    ** Setter for filename
    *******************************************************************************/
   @Override
   public void setFilename(String filename)
   {
      this.filename = filename;
   }



   /*******************************************************************************
    ** Getter for bytes
    *******************************************************************************/
   public byte[] getBytes()
   {
      return (this.bytes);
   }



   /*******************************************************************************
    ** Getter for contentType
    *******************************************************************************/
   public String getContentType()
   {
      return (this.contentType);
   }



   /*******************************************************************************
    ** Getter for filename
    *******************************************************************************/
   public String getFilename()
   {
      return (this.filename);
   }

}
