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

package com.kingsrook.sampleapp;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;


/*******************************************************************************
 ** Runs the canonical sample with its bundled YAML and optional metadata additions.
 *******************************************************************************/
public class ConfigFileBasedSampleJavalinServer extends SampleJavalinServer
{
   /*******************************************************************************
    ** An optional directory adds metadata using QQQ's existing registration rules.
    *******************************************************************************/
   public ConfigFileBasedSampleJavalinServer(String directory)
   {
      super(new SampleMetaDataProvider(directory));
   }



   /*******************************************************************************
    ** Startup exceptions propagate so a failed launch cannot report success.
    *******************************************************************************/
   public static void main(String[] args) throws QException
   {
      if(args.length > 1)
      {
         throw new IllegalArgumentException("Usage: ConfigFileBasedSampleJavalinServer [metadata-directory]");
      }
      new ConfigFileBasedSampleJavalinServer(args.length == 0 ? null : args[0]).start();
   }
}
