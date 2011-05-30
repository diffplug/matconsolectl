package example.remote;

/*
 * Copyright (c) 2010, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Formats returned <code>Object</code>s and <code>Exception</code>s
 * from MATLAB.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class ReturnFormatter
{
	private ReturnFormatter() { }
	
	/**
	 * Format the exception into a string that can be displayed.
	 * 
	 * @param exc the exception
	 * @return the exception as a string
	 */
	public static String formatException(Exception exc)
	{
		StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exc.printStackTrace(printWriter);
        try
        {
        	stringWriter.close();
        }
        catch (IOException ex) { }

        return stringWriter.toString();
	}
	
	//Class signatures of base type arrays
	@SuppressWarnings("unchecked")
	private static final Class BOOLEAN_ARRAY = new boolean[0].getClass(),
							   DOUBLE_ARRAY = new double[0].getClass(),
							   FLOAT_ARRAY = new float[0].getClass(),
							   BYTE_ARRAY = new byte[0].getClass(),
							   SHORT_ARRAY = new short[0].getClass(),
							   INT_ARRAY = new int[0].getClass(),
							   LONG_ARRAY = new long[0].getClass(),
							   CHAR_ARRAY = new char[0].getClass();
	
	/**
	 * Takes in the result from MATLAB and turns it into an easily readable format.
	 * 
	 * @param result
	 * @return description
	 */
	public static String formatResult(Object result)
	{
		return formatResult(result, 0);
	}
	
	/**
	 * Takes in the result from MATLAB and turns it into an easily readable format.
	 * 
	 * @param result
	 * @param level, pass in 0 to initialize, used recursively
	 * @return description
	 */
	@SuppressWarnings("unchecked")
	private static String formatResult(Object result, int level)
	{
		//Message to the built
		String msg = "";
		
		//Tab offset for levels
		String tab = "";
		for(int i = 0; i < level+1; i++)
		{
			tab += "  ";
		}
		
		//If the result is null
		if(result == null)
		{
			msg += "null encountered" + "\n";
		}
		//If the result is an array
		else if(result.getClass().isArray())
		{
			//Check for all of the different types of arrays
			//For each array type, read out all of the array elements
			Class classType = result.getClass();
			
			if(classType.equals(BOOLEAN_ARRAY))
			{
				boolean[] array = (boolean[]) result;
				msg += "boolean array, length = " + array.length + "\n";	
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", boolean: " + array[i] + "\n";
				}
			}
			else if(classType.equals(DOUBLE_ARRAY))
			{
				double[] array = (double[]) result;
				msg += "double array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", double: " + array[i] + "\n";
				}
			}
			else if(classType.equals(FLOAT_ARRAY))
			{
				float[] array = (float[]) result;
				msg += "float array, length = " + array.length + "\n";		
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", float: " + array[i] + "\n";
				}
			}
			else if(classType.equals(BYTE_ARRAY))
			{
				byte[] array = (byte[]) result;
				msg += "byte array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", byte: " + array[i] + "\n";
				}
			}
			else if(classType.equals(SHORT_ARRAY))
			{
				short[] array = (short[]) result;
				msg += "short array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", short: " + array[i] + "\n";
				}
			}
			else if(classType.equals(INT_ARRAY))
			{
				int[] array = (int[]) result;
				msg += "int array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", int: " + array[i] + "\n";
				}
			}
			else if(classType.equals(LONG_ARRAY))
			{
				long[] array = (long[]) result;
				msg += "long array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", long: " + array[i] + "\n";
				}
			}
			else if(classType.equals(CHAR_ARRAY))
			{
				char[] array = (char[]) result;
				msg += "char array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", char: " + array[i] + "\n";
				}
			}
			//Otherwise it must be an array of Objects
			else
			{
				Object[] array = (Object[]) result;
				msg += "Object array, length = " + array.length + "\n";
				for(int i = 0; i < array.length; i++)
				{
					msg += tab + "index " + i + ", " + formatResult(array[i], level+1);
				}
			}
		}
		//If an Object and not an Array
		else
		{
			msg += result.getClass().getCanonicalName() + ": " + result + "\n";
		}
		
		return msg;
	}
}