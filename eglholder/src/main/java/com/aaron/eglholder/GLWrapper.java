package com.aaron.eglholder;

import javax.microedition.khronos.opengles.GL;

/**
 * An interface used to wrap a GL interface.
 * <p>Typically
 * used for implementing debugging and tracing on top of the default
 * GL interface. You would typically use this by creating your own class
 * that implemented all the GL methods by delegating to another GL instance.
 * Then you could add your own behavior before or after calling the
 * delegate. All the GLWrapper would do was instantiate and return the
 * wrapper GL instance:
 * <pre class="prettyprint">
 * class MyGLWrapper implements GLWrapper {
 *     GL wrap(GL gl) {
 *         return new MyGLImplementation(gl);
 *     }
 *     static class MyGLImplementation implements GL,GL10,GL11,... {
 *         ...
 *     }
 * }
 * </pre>
 *
 */
public interface GLWrapper {
    /**
     * Wraps a gl interface in another gl interface.
     *
     * @param gl a GL interface that is to be wrapped.
     * @return either the input argument or another GL object that wraps the input argument.
     */
    GL wrap(GL gl);
}