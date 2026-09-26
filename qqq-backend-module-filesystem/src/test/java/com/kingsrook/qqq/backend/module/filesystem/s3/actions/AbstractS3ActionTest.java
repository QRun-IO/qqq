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

package com.kingsrook.qqq.backend.module.filesystem.s3.actions;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for AbstractS3Action
 *******************************************************************************/
class AbstractS3ActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuildAmazonS3ClientFromBackendMetaData()
   {
      String regionName = Regions.AP_SOUTHEAST_3.getName();
      S3BackendMetaData s3BackendMetaData = new S3BackendMetaData()
         .withAccessKey("Not a real access key")
         .withSecretKey("Also not a real key")
         .withRegion(regionName);
      AmazonS3 amazonS3 = new AbstractS3Action().buildAmazonS3ClientFromBackendMetaData(s3BackendMetaData);
      assertEquals(regionName, amazonS3.getRegionName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuildAmazonS3ClientFromBackendMetaDataWrongType()
   {
      assertThrows(IllegalArgumentException.class, () ->
      {
         new AbstractS3Action().buildAmazonS3ClientFromBackendMetaData(new QBackendMetaData());
      });
   }
}