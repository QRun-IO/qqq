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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.util.List;


/*******************************************************************************
 ** Model containing datastructure expected by frontend stepper widget
 **
 *******************************************************************************/
public class StepperData extends QWidgetData
{
   private String     title;
   private int        activeStep;
   private List<Step> steps;



   /*******************************************************************************
    **
    *******************************************************************************/
   public StepperData(String title, int activeStep, List<Step> steps)
   {
      this.title = title;
      this.activeStep = activeStep;
      this.steps = steps;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.STEPPER.getType();
   }



   /*******************************************************************************
    ** Getter for title
    **
    *******************************************************************************/
   public String getTitle()
   {
      return title;
   }



   /*******************************************************************************
    ** Setter for title
    **
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    **
    *******************************************************************************/
   public StepperData withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for activeStep
    **
    *******************************************************************************/
   public int getActiveStep()
   {
      return activeStep;
   }



   /*******************************************************************************
    ** Setter for activeStep
    **
    *******************************************************************************/
   public void setActiveStep(int activeStep)
   {
      this.activeStep = activeStep;
   }



   /*******************************************************************************
    ** Fluent setter for activeStep
    **
    *******************************************************************************/
   public StepperData withActiveStep(int activeStep)
   {
      this.activeStep = activeStep;
      return (this);
   }



   /*******************************************************************************
    ** Getter for steps
    **
    *******************************************************************************/
   public List<Step> getSteps()
   {
      return steps;
   }



   /*******************************************************************************
    ** Setter for steps
    **
    *******************************************************************************/
   public void setSteps(List<Step> steps)
   {
      this.steps = steps;
   }



   /*******************************************************************************
    ** Fluent setter for steps
    **
    *******************************************************************************/
   public StepperData withSteps(List<Step> steps)
   {
      this.steps = steps;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Step
   {
      private String label;
      private String linkText;
      private String linkURL;

      private String iconOverride;
      private String colorOverride;



      /*******************************************************************************
       **
       *******************************************************************************/
      public Step()
      {
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Step(String label, String linkText, String linkURL)
      {
         this.label = label;
         this.linkText = linkText;
         this.linkURL = linkURL;
      }



      /*******************************************************************************
       ** Getter for label
       **
       *******************************************************************************/
      public String getLabel()
      {
         return label;
      }



      /*******************************************************************************
       ** Setter for label
       **
       *******************************************************************************/
      public void setLabel(String label)
      {
         this.label = label;
      }



      /*******************************************************************************
       ** Fluent setter for label
       **
       *******************************************************************************/
      public Step withLabel(String label)
      {
         this.label = label;
         return (this);
      }



      /*******************************************************************************
       ** Getter for linkText
       **
       *******************************************************************************/
      public String getLinkText()
      {
         return linkText;
      }



      /*******************************************************************************
       ** Setter for linkText
       **
       *******************************************************************************/
      public void setLinkText(String linkText)
      {
         this.linkText = linkText;
      }



      /*******************************************************************************
       ** Fluent setter for linkText
       **
       *******************************************************************************/
      public Step withLinkText(String linkText)
      {
         this.linkText = linkText;
         return (this);
      }



      /*******************************************************************************
       ** Getter for linkURL
       **
       *******************************************************************************/
      public String getLinkURL()
      {
         return linkURL;
      }



      /*******************************************************************************
       ** Setter for linkURL
       **
       *******************************************************************************/
      public void setLinkURL(String linkURL)
      {
         this.linkURL = linkURL;
      }



      /*******************************************************************************
       ** Fluent setter for linkURL
       **
       *******************************************************************************/
      public Step withLinkURL(String linkURL)
      {
         this.linkURL = linkURL;
         return (this);
      }



      /*******************************************************************************
       ** Getter for iconOverride
       **
       *******************************************************************************/
      public String getIconOverride()
      {
         return iconOverride;
      }



      /*******************************************************************************
       ** Setter for iconOverride
       **
       *******************************************************************************/
      public void setIconOverride(String iconOverride)
      {
         this.iconOverride = iconOverride;
      }



      /*******************************************************************************
       ** Fluent setter for iconOverride
       **
       *******************************************************************************/
      public Step withIconOverride(String iconOverride)
      {
         this.iconOverride = iconOverride;
         return (this);
      }



      /*******************************************************************************
       ** Getter for colorOverride
       **
       *******************************************************************************/
      public String getColorOverride()
      {
         return colorOverride;
      }



      /*******************************************************************************
       ** Setter for colorOverride
       **
       *******************************************************************************/
      public void setColorOverride(String colorOverride)
      {
         this.colorOverride = colorOverride;
      }



      /*******************************************************************************
       ** Fluent setter for colorOverride
       **
       *******************************************************************************/
      public Step withColorOverride(String colorOverride)
      {
         this.colorOverride = colorOverride;
         return (this);
      }

   }

}
