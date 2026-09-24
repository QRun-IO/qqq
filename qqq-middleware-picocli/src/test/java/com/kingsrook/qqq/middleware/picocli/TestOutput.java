/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
