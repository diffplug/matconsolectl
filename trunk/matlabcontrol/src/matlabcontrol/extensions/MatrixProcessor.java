package matlabcontrol.extensions;

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

import matlabcontrol.MatlabInteractor;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

/**
 * Handles retrieving and sending MATLAB matrices.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatrixProcessor
{
    private final MatlabInteractor<Object> _interactor;
    
    /**
     * Constructs the processor. The {@code interactor} does not have to be a {@link MatlabProxy}, but in order for
     * the processor to operate properly it expects that the method calls and their return value occur as they would
     * ordinarily.
     * 
     * @param interactor 
     */
    public MatrixProcessor(MatlabInteractor<Object> interactor)
    {
        _interactor = interactor;
    }
    
    /**
     * Retrieves the matrix with the variable name {@code matrixName}.
     * <br><br>
     * In order to retrieve the necessary information, several MATLAB functions must be called. If the matrix is
     * modified in between the function calls, then issues may arise.
     * 
     * @param matrixName
     * @return
     * @throws MatlabInvocationException 
     */
    public MatlabMatrix getMatrix(String matrixName) throws MatlabInvocationException
    {
        //Retrieve real values
        Object realObject = _interactor.returningEval("real(" + matrixName + ")", 1);
        if(!realObject.getClass().equals(double[].class))
        {
            throw new MatlabProcessingException(matrixName + " is not a MATLAB array of doubles");
        }
        double[] realValues = (double[]) realObject;
        
        //Retrieve imaginary values
        Object imaginaryObject = _interactor.returningEval("imag(" + matrixName + ")", 1);
        if(!imaginaryObject.getClass().equals(double[].class))
        {
            throw new MatlabProcessingException(matrixName + " is not a MATLAB array of doubles");
        }
        double[] imaginaryValues = (double[]) imaginaryObject;
        
        //Retrieve lengths of array
        int[] lengths = this.getMatrixLengths(matrixName);
        
        return new MatlabMatrix(realValues, imaginaryValues, lengths);
    }
    
    /**
     * Stores the {@code matrix} in MATLAB with the variable name {@code matrixName}.
     * 
     * @param matrixName the variable name
     * @param matrix
     * @throws MatlabInvocationException
     */
    public void setMatrix(String matrixName, MatlabMatrix matrix) throws MatlabInvocationException
    {
        //Store real and imaginary arrays in the MATLAB environment
        String realArray = _interactor.storeObject(matrix.getRealLinearArray(), false);
        String imaginaryArray = _interactor.storeObject(matrix.getImaginaryLinearArray(), false);
        
        //Combine the real and imaginary arrays, set the proper dimension length metadata, and then store as matrixName
        String evalStatement = matrixName + " = reshape(" + realArray + " + " + imaginaryArray + " * i";
        int[] lengths = matrix.getLengths();
        for(int length : lengths)
        {
            evalStatement += ", " + length;
        }
        evalStatement += ");";
        
        System.out.println(evalStatement);
        
        _interactor.eval(evalStatement);
    }
    
    /**
     * Retrieves the lengths of the matrix named {@code matrixName}.
     * 
     * @param matrixName
     * @return
     * @throws MatlabInvocationException 
     */
    private int[] getMatrixLengths(String matrixName) throws MatlabInvocationException
    {
        double[] size = (double[]) _interactor.returningEval("size(" + matrixName + ")", 1);
        int[] lengths = new int[size.length];
        
        for(int i = 0; i < size.length; i++)
        {
            lengths[i] = (int) size[i];
        }
        
        return lengths;
    }
}