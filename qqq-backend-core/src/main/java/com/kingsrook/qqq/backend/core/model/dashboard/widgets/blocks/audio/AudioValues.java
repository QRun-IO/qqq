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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.audio;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class AudioValues implements BlockValuesInterface
{
   private String  path;
   private boolean showControls = false;
   private boolean autoPlay     = true;



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Setter for path
    *******************************************************************************/
   public void setPath(String path)
   {
      this.path = path;
   }



   /*******************************************************************************
    ** Fluent setter for path
    *******************************************************************************/
   public AudioValues withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /*******************************************************************************
    ** Getter for showControls
    *******************************************************************************/
   public boolean getShowControls()
   {
      return (this.showControls);
   }



   /*******************************************************************************
    ** Setter for showControls
    *******************************************************************************/
   public void setShowControls(boolean showControls)
   {
      this.showControls = showControls;
   }



   /*******************************************************************************
    ** Fluent setter for showControls
    *******************************************************************************/
   public AudioValues withShowControls(boolean showControls)
   {
      this.showControls = showControls;
      return (this);
   }



   /*******************************************************************************
    ** Getter for autoPlay
    *******************************************************************************/
   public boolean getAutoPlay()
   {
      return (this.autoPlay);
   }



   /*******************************************************************************
    ** Setter for autoPlay
    *******************************************************************************/
   public void setAutoPlay(boolean autoPlay)
   {
      this.autoPlay = autoPlay;
   }



   /*******************************************************************************
    ** Fluent setter for autoPlay
    *******************************************************************************/
   public AudioValues withAutoPlay(boolean autoPlay)
   {
      this.autoPlay = autoPlay;
      return (this);
   }

}
