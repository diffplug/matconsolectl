package matlabcontrol.link;

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

import java.util.Arrays;

/**
 * MATLAB return containers.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabReturns
{
    private MatlabReturns() { }
    
    /**
     * Hidden super class of all of the {@code Return}X classes. This class is hidden because it is not a valid return
     * type from a method declared in an interface provided to {@link MatlabFunctionLinker} and there is no need for a
     * user to make use of this class.
     */
    static class ReturnN
    {
        private final Object[] _values;
        
        ReturnN(Object[] values)
        {
            _values = values;
        }
        
        Object get(int i)
        {
            return _values[i];
        }
        
        /**
         * Returns a brief description of this container. The exact details of this representation are unspecified and
         * are subject to change.
         * 
         * @return 
         */
        @Override
        public String toString()
        {
            return "[" + this.getClass().getCanonicalName() +
                    " size=" + _values.length + "," +
                    " values=" + Arrays.toString(_values) + "]";
        }
    }
    
    /**
     * Container for two MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     */
    public static class Return2<A, B> extends ReturnN
    {
        Return2(Object[] values)
        {
            super(values);
        }
        
        /**
         * The first return argument.
         * 
         * @return 
         */
        public A getFirst()
        {
            return (A) get(0);
        }
        
        /**
         * The second return argument.
         * 
         * @return 
         */
        public B getSecond()
        {
            return (B) get(1);
        }
    }
    
    /**
     * Container for three MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     */
    public static class Return3<A, B, C> extends Return2<A, B>
    {
        Return3(Object[] values)
        {
            super(values);
        }
        
        /**
         * The third return argument.
         * 
         * @return 
         */
        public C getThird()
        {
            return (C) get(2);
        }
    }
    
    /**
     * Container for four MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     * @param <D> fourth return type
     */
    public static class Return4<A, B, C, D> extends Return3<A, B, C>
    {
        Return4(Object[] values)
        {
            super(values);
        }
        
        /**
         * The fourth return argument.
         * 
         * @return 
         */
        public D getFourth()
        {
            return (D) get(3);
        }
    }
    
    /**
     * Container for five MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     * @param <D> fourth return type
     * @param <E> fifth return type
     */
    public static class Return5<A, B, C, D, E> extends Return4<A, B, C, D>
    {
        Return5(Object[] values)
        {
            super(values);
        }
        
        /**
         * The fifth return argument.
         * 
         * @return 
         */
        public E getFifth()
        {
            return (E) get(4);
        }
    }    
    
    /**
     * Container for six MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     * @param <D> fourth return type
     * @param <E> fifth return type
     * @param <F> sixth return type
     */
    public static class Return6<A, B, C, D, E, F> extends Return5<A, B, C, D, E>
    {
        Return6(Object[] values)
        {
            super(values);
        }
        
        /**
         * The sixth return argument.
         * 
         * @return 
         */
        public F getSixth()
        {
            return (F) get(5);
        }
    }   
    
    /**
     * Container for seven MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     * @param <D> fourth return type
     * @param <E> fifth return type
     * @param <F> sixth return type
     * @param <G> seventh return type
     */
    public static class Return7<A, B, C, D, E, F, G> extends Return6<A, B, C, D, E, F>
    {
        Return7(Object[] values)
        {
            super(values);
        }
        
        /**
         * The seventh return argument.
         * 
         * @return 
         */
        public G getSeventh()
        {
            return (G) get(6);
        }
    }
    
    /**
     * Container for eight MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     * @param <D> fourth return type
     * @param <E> fifth return type
     * @param <F> sixth return type
     * @param <G> seventh return type
     * @param <H> eight return type
     */
    public static class Return8<A, B, C, D, E, F, G, H> extends Return7<A, B, C, D, E, F, G>
    {
        Return8(Object[] values)
        {
            super(values);
        }
        
        /**
         * The eight return argument.
         * 
         * @return 
         */
        public H getEighth()
        {
            return (H) get(7);
        }
    }
    
    /**
     * Container for nine MATLAB return values.
     * 
     * @param <A> first return type
     * @param <B> second return type
     * @param <C> third return type
     * @param <D> fourth return type
     * @param <E> fifth return type
     * @param <F> sixth return type
     * @param <G> seventh return type
     * @param <H> eight return type
     * @param <I> ninth return type
     */
    public static class Return9<A, B, C, D, E, F, G, H, I> extends Return8<A, B, C, D, E, F, G, H>
    {
        Return9(Object[] values)
        {
            super(values);
        }
        
        /**
         * The ninth argument.
         * 
         * @return 
         */
        public I getNinth()
        {
            return (I) get(8);
        }
    }
    
    static ReturnN getMaxReturn(Object[] values)
    {
        return new Return9(values);
    }
}