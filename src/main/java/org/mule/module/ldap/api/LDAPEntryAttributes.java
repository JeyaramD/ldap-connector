/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

/*
 * Project: Leonards Common Libraries
 * This class is member of leonards.common.ldap
 * File: LDAPEntryAttributes.java
 *
 * Property of Leonards / Mindpool
 * Created on Jun 25, 2006 (11:40:32 AM) 
 */

package org.mule.module.ldap.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is the abstraction
 * 
 * @author mariano
 */
public class LDAPEntryAttributes implements Serializable
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 7394952573580371628L;

    /**
	 * 
	 */
    private Map<String, LDAPEntryAttribute> attributes = new HashMap<String, LDAPEntryAttribute>();

    /**
	 * 
	 */
    public LDAPEntryAttributes()
    {
        super();
    }

    /**
     * @param attribute
     */
    public void addAttribute(LDAPEntryAttribute attribute)
    {
        this.attributes.put(attribute.getName().toLowerCase(), attribute);
    }

    /**
	 * 
	 *
	 */
    public void resetAttributes()
    {
        this.attributes.clear();
    }

    /**
     * @return
     */
    public int getCount()
    {
        return this.attributes.size();
    }

    /**
     * @return
     */
    public Iterator<LDAPEntryAttribute> attributes()
    {
        return this.attributes.values().iterator();
    }

    /**
     * @param name
     * @return
     */
    public LDAPEntryAttribute getAttribute(String name)
    {
        return (LDAPEntryAttribute) this.attributes.get(name.toLowerCase());
    }
}
