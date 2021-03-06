/*
 * Copyright (c) 2009 The Jackson Laboratory
 * 
 * This software was developed by Gary Churchill's Lab at The Jackson
 * Laboratory (see http://research.jax.org/faculty/churchill).
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jax.r.jriutilities;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A factory for getting a handle on the R interface instance.
 * @author <A HREF="mailto:keith.sheppard@jax.org">Keith Sheppard</A>
 */
public final class RInterfaceFactory
{
    /**
     * our logger
     */
    private static final Logger LOG =
        Logger.getLogger(RInterfaceListener.class.getName());
    
    /**
     * the static reference that we hand everybody
     */
    private static final RInterface rInterfaceInstance;
    
    // static init block
    static
    {
        BasicRInterface basicRInterface = null;
        try
        {
            basicRInterface = new BasicRInterface();
        }
        catch(Exception ex)
        {
            LOG.log(Level.SEVERE,
                    "received exception while initializing the R interface",
                    ex);
            basicRInterface = null;
        }
        finally
        {
            rInterfaceInstance = basicRInterface;
        }
    }
    
    /**
     * private constructor. instantiation isn't allowed
     */
    private RInterfaceFactory()
    {
    }
    
    /**
     * Get a handle on the R interface that we're using.
     * @return
     *          a handle on the R interface
     */
    public static RInterface getRInterfaceInstance()
    {
        // some day we may do more fancy class loading. for
        // now, this is good enough
        return RInterfaceFactory.rInterfaceInstance;
    }
}
