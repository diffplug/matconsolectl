package matlabcontrol.link;

/*
 * Copyright (c) 2013, Joshua Kaplan
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

import java.util.Arrays;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * @param <T> {@code float} array type, ex. {@code float[]}, {@code float[][]}, {@code float[][][]}, ...
 */
public class MatlabSingleArray<T> extends MatlabNumberArray<float[], T>
{
    MatlabSingleArray(float[] real, float[] imag, int[] lengths)
    {
        super(float[].class, real, imag, lengths);
    }
    
    public static <T> MatlabSingleArray<T> getInstance(T real, T imaginary)
    {
        return new MatlabSingleArray(real, imaginary);
    }
    
    private MatlabSingleArray(T real, T imaginary)
    {
        super(float[].class, real, imaginary);
    }
        
    /**
     * {@inheritDoc}
     * 
     * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public MatlabSingle getElementAtLinearIndex(int index)
    {
        return new MatlabSingle(_real[index], (_imag == null ? 0 : _imag[index]));
    }
        
    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public MatlabSingle getElementAtIndices(int row, int column, int... pages)
    {
        int linearIndex = getLinearIndex(row, column, pages);
        
        return new MatlabSingle(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
    }

    @Override
    boolean equalsRealArray(float[] other)
    {
        return Arrays.equals(_real, other);
    }

    @Override
    boolean equalsImaginaryArray(float[] other)
    {
        return Arrays.equals(_imag, other);
    }

    @Override
    int hashReal()
    {
        return Arrays.hashCode(_real);
    }

    @Override
    int hashImaginary()
    {
        return Arrays.hashCode(_imag);
    }

    @Override
    boolean containsNonZero(float[] array)
    {
        boolean contained = false;
        
        for(float val : array)
        {
            if(val != 0.0f)
            {
                contained = true;
                break;
            }
        }
        
        return contained;
    }
}