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

package com.kingsrook.qqq.backend.core.model.data;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 * Annotation to place onto fields in a QRecordEntity, to mark them as virtual
 * fields.  Not as full featured (in this original implementation at least) as
 * {@link QField}, where many of the field's attributes can be set, and this can
 * be used to construct {@link QFieldMetaData}.  Instead (at least for now), just
 * adding this here to address getting these passed back and forth when QRecord
 * and QRecordEntity objects are populated from one another.
 *
 *******************************************************************************/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QVirtualField
{
}
