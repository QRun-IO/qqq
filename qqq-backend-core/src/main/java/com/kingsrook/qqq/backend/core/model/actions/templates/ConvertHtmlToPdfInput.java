/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.templates;


import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 **
 *******************************************************************************/
public class ConvertHtmlToPdfInput extends AbstractActionInput
{
   private String       html;
   private OutputStream outputStream;

   private Path              basePath;
   private Map<String, Path> customFonts = new HashMap<>();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ConvertHtmlToPdfInput()
   {
   }



   /*******************************************************************************
    ** Getter for html
    **
    *******************************************************************************/
   public String getHtml()
   {
      return html;
   }



   /*******************************************************************************
    ** Setter for html
    **
    *******************************************************************************/
   public void setHtml(String html)
   {
      this.html = html;
   }



   /*******************************************************************************
    ** Fluent setter for html
    **
    *******************************************************************************/
   public ConvertHtmlToPdfInput withHtml(String html)
   {
      this.html = html;
      return (this);
   }



   /*******************************************************************************
    ** Getter for outputStream
    **
    *******************************************************************************/
   public OutputStream getOutputStream()
   {
      return outputStream;
   }



   /*******************************************************************************
    ** Setter for outputStream
    **
    *******************************************************************************/
   public void setOutputStream(OutputStream outputStream)
   {
      this.outputStream = outputStream;
   }



   /*******************************************************************************
    ** Fluent setter for outputStream
    **
    *******************************************************************************/
   public ConvertHtmlToPdfInput withOutputStream(OutputStream outputStream)
   {
      this.outputStream = outputStream;
      return (this);
   }



   /*******************************************************************************
    ** Getter for basePath
    **
    *******************************************************************************/
   public Path getBasePath()
   {
      return basePath;
   }



   /*******************************************************************************
    ** Setter for basePath
    **
    *******************************************************************************/
   public void setBasePath(Path basePath)
   {
      this.basePath = basePath;
   }



   /*******************************************************************************
    ** Fluent setter for basePath
    **
    *******************************************************************************/
   public ConvertHtmlToPdfInput withBasePath(Path basePath)
   {
      this.basePath = basePath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customFonts
    **
    *******************************************************************************/
   public Map<String, Path> getCustomFonts()
   {
      return customFonts;
   }



   /*******************************************************************************
    ** Setter for customFonts
    **
    *******************************************************************************/
   public void setCustomFonts(Map<String, Path> customFonts)
   {
      this.customFonts = customFonts;
   }



   /*******************************************************************************
    ** Fluent setter for customFonts
    **
    *******************************************************************************/
   public ConvertHtmlToPdfInput withCustomFonts(Map<String, Path> customFonts)
   {
      this.customFonts = customFonts;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for customFonts
    **
    *******************************************************************************/
   public ConvertHtmlToPdfInput withCustomFont(String name, Path path)
   {
      if(this.customFonts == null)
      {
         this.customFonts = new HashMap<>();
      }
      this.customFonts.put(name, path);
      return (this);
   }

}
