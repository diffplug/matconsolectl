package example.remote;

/*
 * Copyright (c) 2011, Joshua Kaplan
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
import java.lang.reflect.Array;

/**
 * Formats returned {@code Object}s and {@code Exception}s from MATLAB.
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
        catch(IOException ex) { }

        return stringWriter.toString();
    }
    
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
    private static String formatResult(Object result, int level)
    {
        //Message builder
        StringBuilder builder = new StringBuilder();
        
        //Tab offset for levels
        String tab = "";
        for(int i = 0; i < level + 1; i++)
        {
            tab += "  ";
        }
        
        //If the result is null
        if(result == null)
        {
            builder.append("null encountered\n");
        }
        //If the result is an array
        else if(result.getClass().isArray())
        {
            Class<?> componentClass = result.getClass().getComponentType();
            
            //Primitive array
            if(componentClass.isPrimitive())
            {
                String componentName = componentClass.toString();
                int length = Array.getLength(result);
                
                builder.append(componentName);
                builder.append(" array, length = ");
                builder.append(length);
                builder.append("\n");
                
                for(int i = 0; i < length; i++)
                {   
                    builder.append(tab);
                    builder.append("index ");
                    builder.append(i);
                    builder.append(", ");
                    builder.append(componentName);
                    builder.append(": ");
                    builder.append(Array.get(result, i));
                    builder.append("\n");
                }
            }
            //Object array
            else
            {
                Object[] array = (Object[]) result;
                
                builder.append("Object array, length = ");
                builder.append(array.length);
                builder.append("\n");
                
                for(int i = 0; i < array.length; i++)
                {   
                    builder.append(tab);
                    builder.append("index ");
                    builder.append(i);
                    builder.append(", ");
                    builder.append(formatResult(array[i], level + 1));
                }
            }
        }
        //If an Object and not an array
        else
        {   
            builder.append(result.getClass().getCanonicalName());
            builder.append(": ");
            builder.append(result);
            builder.append("\n");
        }
        
        return builder.toString();
    }
}