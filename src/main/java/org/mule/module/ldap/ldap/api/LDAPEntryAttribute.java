/*
 * Project: Leonards Common Libraries
 * This class is member of leonards.common.ldap
 * File: LDAPEntryAttribute.java
 *
 * Property of Leonards / Mindpool
 * Created on Jun 22, 2006 (1:03:40 AM) 
 */

package org.mule.module.ldap.ldap.api;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the abstraction
 * 
 * @author mariano
 */
public abstract class LDAPEntryAttribute implements Serializable
{
    private static final long serialVersionUID = -1854622206842090871L;

    private String name = null;

    /**
	 * 
	 */
    public LDAPEntryAttribute()
    {
        super();
    }

    /**
     * @param name
     */
    public LDAPEntryAttribute(String name)
    {
        super();
        setName(name);
    }

    /**
     * @return
     */
    public abstract Object getValue();

    /**
     * @return
     */
    public abstract List<Object> getValues();

    /**
     * @return
     */
    public abstract boolean isMultiValued();

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return
     */
    public String toLDIFString()
    {
        StringBuffer buffer = new StringBuffer();
        Iterator<Object> valuesToFormat = getValues().iterator();
        while (valuesToFormat.hasNext())
        {
            Object value = valuesToFormat.next();
            buffer.append(getName());
            if (value instanceof byte[])
            {
                buffer.append(":: ");
                buffer.append(LDAPUtils.encodeBase64((byte[]) value));
            }
            else
            {
                buffer.append(": ");
                buffer.append(String.valueOf(value));
            }
            buffer.append(LDAPUtils.NEW_LINE);
        }
        return buffer.toString();
    }
}
