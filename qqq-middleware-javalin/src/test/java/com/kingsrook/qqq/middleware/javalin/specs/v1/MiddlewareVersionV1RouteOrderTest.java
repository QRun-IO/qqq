/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Javalin matches routes in registration order, so a route with a path
 ** parameter registered before a route with a literal segment in the same
 ** position (same method, same length) shadows it - e.g. GET
 ** /table/{t}/{primaryKey} answering GET /table/{t}/apis.  This guards the
 ** order of MiddlewareVersionV1's specs.
 *******************************************************************************/
class MiddlewareVersionV1RouteOrderTest
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testNoRouteIsShadowedByAnEarlierParameterRoute()
   {
      List<AbstractEndpointSpec<?, ?, ?>> specs    = new MiddlewareVersionV1().getEndpointSpecs();
      List<String>                        problems = new ArrayList<>();
      for(int i = 0; i < specs.size(); i++)
      {
         BasicOperation earlier = specs.get(i).defineBasicOperation();
         for(int j = i + 1; j < specs.size(); j++)
         {
            BasicOperation later = specs.get(j).defineBasicOperation();
            if(earlier.getHttpMethod() == later.getHttpMethod() && shadows(earlier.getPath(), later.getPath()))
            {
               problems.add(earlier.getHttpMethod() + " " + earlier.getPath() + " (" + specs.get(i).getClass().getSimpleName() + ") shadows "
                  + later.getPath() + " (" + specs.get(j).getClass().getSimpleName() + "): register the later one first");
            }
         }
      }
      assertThat(problems).isEmpty();
   }



   /***************************************************************************
    ** whether every request matching {@code later} also matches {@code earlier}
    ** (each segment equal, or a parameter in {@code earlier}) and the paths
    ** differ
    ***************************************************************************/
   static boolean shadows(String earlier, String later)
   {
      String[] a = earlier.split("/");
      String[] b = later.split("/");
      if(a.length != b.length || earlier.equals(later))
      {
         return (false);
      }
      for(int k = 0; k < a.length; k++)
      {
         boolean parameter = a[k].startsWith("{") && a[k].endsWith("}");
         if(!parameter && !a[k].equals(b[k]))
         {
            return (false);
         }
      }
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testShadows()
   {
      assertThat(shadows("/table/{t}/{pk}", "/table/{t}/apis")).isTrue();
      assertThat(shadows("/table/{t}/apis", "/table/{t}/{pk}")).isFalse();
      assertThat(shadows("/table/{t}/{pk}", "/table/{t}/{pk}/features")).isFalse();
      assertThat(shadows("/table/{t}/{pk}", "/table/{t}/{pk}")).isFalse();
   }

}
