package matlabcontrol.extensions;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;

/**
 * Hidden superclass of all Java classes which convert between MATLAB types.
 */
abstract class MatlabType<T extends MatlabType>
{
    /**
     * All {@code MatlabType} subclasses that can return information from MATLAB (typically all types, but for instance
     * not {@link MatlabVariable}) must be annotated with this.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface MatlabTypeSerializationProvider
    {
        Class<? extends MatlabTypeSerializedGetter> value();
    }
    
    abstract MatlabTypeSerializedSetter<T> getSerializedSetter();
    
    /**
     * Returns the serialized getter associated with the {@link MatlabType} subclass. This is done by creating an
     * instance of the {@link MatlabTypeSerializedGetter} that {@code clazz} specified in its
     * {@link MatlabTypeSerializationProvider}. 
     * 
     * @param <U>
     * @param clazz
     * @return 
     * @throws IllegalArgumentException if the serialized getter cannot be created
     */
    static <U extends MatlabType> MatlabTypeSerializedGetter<U> createSerializedGetter(Class<U> clazz)
    {
        try
        {
            MatlabTypeSerializationProvider provider = clazz.getAnnotation(MatlabTypeSerializationProvider.class);
            Class<? extends MatlabTypeSerializedGetter> getterClass = provider.value();
            
            return getterClass.newInstance();
        }
        catch(Exception ex)
        {
            throw new IllegalArgumentException("Unable to create serialized getter for " + clazz.getName(), ex);
        }
    }
    
    /**
     * Retrieves in MATLAB the information necessary to create the associated {@code MatlabType} from a given MATLAB
     * variable.
     * <br><br>
     * Must have an accessible no argument constructor.
     * 
     * @param <U> 
     */
    static interface MatlabTypeSerializedGetter<U extends MatlabType> extends Serializable
    {
        /**
         * Takes the information retrieved by the
         * {@link #getInMatlab(matlabcontrol.MatlabProxy.MatlabThreadProxy, java.lang.String)} and creates the
         * associated {@code MatlabType}.
         * 
         * @return 
         */
        public U deserialize();
        
        /**
         * Retrieves the data it needs from the variable in MATLAB. So that after retrieving this information
         * {@link #deserialize()} can be called to create the appropriate {@code MatlabType}.
         * 
         * @param proxy
         * @param variableName 
         */
        public void getInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException;
    }
    
    static interface MatlabTypeSerializedSetter<U extends MatlabType> extends Serializable
    {
        public void setInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException;
    }
}