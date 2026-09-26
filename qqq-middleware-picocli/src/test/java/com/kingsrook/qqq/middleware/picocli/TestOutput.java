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

package com.kingsrook.qqq.middleware.picocli;


/*******************************************************************************
 **
 *******************************************************************************/
class TestOutput
{
   private String   output;
   private String[] outputLines;
   private String   error;
   private String[] errorLines;



   /*******************************************************************************
    **
    *******************************************************************************/
   public TestOutput(String output, String error)
   {
      this.output = output;
      this.error = error;

      this.outputLines = output.split("\n");
      this.errorLines = error.split("\n");
   }



   /*******************************************************************************
    ** Getter for output
    **
    *******************************************************************************/
   public String getOutput()
   {
      return output;
   }



   /*******************************************************************************
    ** Setter for output
    **
    *******************************************************************************/
   public void setOutput(String output)
   {
      this.output = output;
   }



   /*******************************************************************************
    ** Getter for outputLines
    **
    *******************************************************************************/
   public String[] getOutputLines()
   {
      return outputLines;
   }



   /*******************************************************************************
    ** Setter for outputLines
    **
    *******************************************************************************/
   public void setOutputLines(String[] outputLines)
   {
      this.outputLines = outputLines;
   }



   /*******************************************************************************
    ** Getter for error
    **
    *******************************************************************************/
   public String getError()
   {
      return error;
   }



   /*******************************************************************************
    ** Setter for error
    **
    *******************************************************************************/
   public void setError(String error)
   {
      this.error = error;
   }



   /*******************************************************************************
    ** Getter for errorLines
    **
    *******************************************************************************/
   public String[] getErrorLines()
   {
      return errorLines;
   }



   /*******************************************************************************
    ** Setter for errorLines
    **
    *******************************************************************************/
   public void setErrorLines(String[] errorLines)
   {
      this.errorLines = errorLines;
   }
}
