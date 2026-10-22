/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.producers.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;


/*******************************************************************************
 ** annotation to go on a QRecordEntity class, which you would like to be
 ** processed by MetaDataProducerHelper, to automatically produce some meta-data
 ** objects.  Specifically supports:
 **
 ** - Making a possible-value-source out of the table.
 ** - Processing child tables to create joins and childRecordList widgets
 *******************************************************************************/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public @interface QMetaDataProducingEntity
{
   boolean produceTableMetaData() default false;
   Class<? extends MetaDataCustomizerInterface> tableMetaDataCustomizer() default MetaDataCustomizerInterface.NoopMetaDataCustomizer.class;

   boolean producePossibleValueSource() default false;
   ChildTable[] childTables() default { };

}
