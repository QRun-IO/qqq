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

package com.kingsrook.qqq.backend.core.instances;


/*******************************************************************************
 ** Version of AbstractQQQApplication that assumes all meta-data is produced
 ** by MetaDataProducers in (or under) a single package (where you can pass that
 ** package into the constructor, vs. the abstract base class, where you extend
 ** it and override the getMetaDataPackageName method.
 *******************************************************************************/
public class MetaDataProducerBasedQQQApplication extends AbstractMetaDataProducerBasedQQQApplication
{
   private final String metaDataPackageName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MetaDataProducerBasedQQQApplication(String metaDataPackageName)
   {
      this.metaDataPackageName = metaDataPackageName;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MetaDataProducerBasedQQQApplication(Class<?> aClassInMetaDataPackage)
   {
      this(aClassInMetaDataPackage.getPackageName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getMetaDataPackageName()
   {
      return (this.metaDataPackageName);
   }
}
