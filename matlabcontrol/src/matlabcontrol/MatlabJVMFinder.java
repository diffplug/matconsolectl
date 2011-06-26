package matlabcontrol;

/*
 * Copyright (c) 2011, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;


/**
 * Wrapper around {@link MatlabJVMFinder_Internal} because that class requires the {@code tools.jar} which is part of
 * the JDK (Java Development Kit) but is not part of the standard JRE (Java Runtime Environment). This class prevents
 * use of {@code MatlabJVMFinder_Internal} unless the Attach API present in {@code tools.jar} is available, the code
 * is running in a jar, and the manifest is properly configured.
 * <br><br>
 * Requires {@code manifest.mf} to have the entry {@code Agent-Class: matlabcontrol.MatlabJVMFinder_Internal}.
 * 
 * @since 4.0.0-experimental
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabJVMFinder
{   
    public static boolean connect(String receiverID) throws MatlabConnectionException
    {
        boolean connectionAttemptMade;
        if(isEligible())
        {
            connectionAttemptMade = MatlabJVMFinder_Internal.connect(receiverID);
        }
        else
        {
            System.out.println("Not eligible");
            connectionAttemptMade = false;
        }
        
        return connectionAttemptMade;
    }
    
    private static boolean isEligible()
    {   
        return isAttachAPIAvailable() && isJarProperlyConfigured();
    }
    
    private static boolean isAttachAPIAvailable()
    {
        boolean available;
        try
        {
            Class.forName("com.sun.tools.attach.VirtualMachine", false, MatlabJVMFinder.class.getClassLoader());
            available = true;
        }
        catch(ClassNotFoundException ex)
        {
            available = false;
        }
        
        return available;
    }
    
    private static boolean isJarProperlyConfigured()
    {
        boolean configuredProperly;
        
        try
        {
            JarFile supportJar = new JarFile(new File(Configuration.getSupportCodeLocation()));
            String agentClass = supportJar.getManifest().getMainAttributes().getValue("Agent-Class");
            configuredProperly = agentClass.equals(MatlabJVMFinder_Internal.class.getName());
        }
        catch(MatlabConnectionException ex)
        {
            configuredProperly = false;
        }
        catch(IOException ex)
        {
            configuredProperly = false;
        }
        
        return configuredProperly;
    }
}