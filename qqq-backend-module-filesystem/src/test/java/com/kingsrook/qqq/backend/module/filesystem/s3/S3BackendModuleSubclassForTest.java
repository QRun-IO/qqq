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

package com.kingsrook.qqq.backend.module.filesystem.s3;


import cloud.localstack.awssdkv1.TestUtils;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.AbstractS3Action;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;


/*******************************************************************************
 ** Subclass of the S3Backend module, meant for use in unit tests, if/where we
 ** need to make sure we use the localstack version of the S3 client.
 *******************************************************************************/
public class S3BackendModuleSubclassForTest extends S3BackendModule
{

   /*******************************************************************************
    ** Seed the AbstractS3Action with an s3Utils object that has the localstack
    ** s3 client in it
    *******************************************************************************/
   @Override
   public AbstractBaseFilesystemAction<S3ObjectSummary> getActionBase()
   {
      AbstractS3Action actionBase = (AbstractS3Action) super.getActionBase();
      S3Utils          s3Utils    = new S3Utils();
      s3Utils.setAmazonS3(TestUtils.getClientS3());
      actionBase.setS3Utils(s3Utils);
      return (actionBase);
   }

}
