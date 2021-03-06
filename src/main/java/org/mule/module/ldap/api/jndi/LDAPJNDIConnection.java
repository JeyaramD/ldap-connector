/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

/*
 * Project: Leonards Common Libraries
 * This class is member of org.mule.modules.ldap.jndi
 * File: LDAPJNDIConnection.java
 *
 * Property of Leonards / Mindpool
 * Created on Jun 22, 2006 (10:43:40 PM) 
 */

package org.mule.module.ldap.api.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.StringUtils;
import org.mule.module.ldap.api.LDAPConnection;
import org.mule.module.ldap.api.LDAPEntry;
import org.mule.module.ldap.api.LDAPEntryAttribute;
import org.mule.module.ldap.api.LDAPEntryAttributes;
import org.mule.module.ldap.api.LDAPException;
import org.mule.module.ldap.api.LDAPResultSet;
import org.mule.module.ldap.api.LDAPSearchControls;

/**
 * This class is the abstraction
 * 
 * @author mariano
 */
public class LDAPJNDIConnection extends LDAPConnection
{
    public static final int DEFAULT_MAX_POOL_CONNECTIONS = 0;
    public static final int DEFAULT_INITIAL_POOL_CONNECTIONS = 0;
    public static final long DEFAULT_POOL_TIMEOUT = 0L;
    public static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String DEFAULT_REFERRAL = "ignore";

    private static final boolean IGNORE_CASE = true;

    private static final String INITIAL_CONTEXT_FACTORY_ATTR = "initialContextFactory";
    
    /**
     * Final constants for managing JNDI Pool connections.
     */
    private static final String POOL_ENABLED_ENV_PARAM =  "com.sun.jndi.ldap.connect.pool";
    private static final String MAX_POOL_SIZE_ENV_PARAM = "com.sun.jndi.ldap.connect.pool.maxsize";
    private static final String INIT_POOL_SIZE_ENV_PARAM = "com.sun.jndi.ldap.connect.pool.initsize";
    private static final String TIME_OUT_ENV_PARAM = "com.sun.jndi.ldap.connect.pool.timeout";
    private static final String AUTHENTICATION_ENV_PARAM = "com.sun.jndi.ldap.pool.authentication";

    private String providerUrl = null;
    private int maxPoolConnections = DEFAULT_MAX_POOL_CONNECTIONS;
    private int initialPoolSizeConnections = DEFAULT_INITIAL_POOL_CONNECTIONS;
    private long poolTimeout = DEFAULT_POOL_TIMEOUT;
    private String authentication = NO_AUTHENTICATION;
    private String initialContextFactory = DEFAULT_INITIAL_CONTEXT_FACTORY;
    private String referral = DEFAULT_REFERRAL;
    private Map<String, String> extendedEnvironment = null;
        
    private LdapContext conn = null;

    /**
	 * 
	 */
    public LDAPJNDIConnection()
    {
        super();
    }

    /**
     * @param providerUrl
     * @throws LDAPException
     */
    public LDAPJNDIConnection(String providerUrl) throws LDAPException
    {
        this(providerUrl, DEFAULT_INITIAL_CONTEXT_FACTORY);
    }

    /**
     * @param providerUrl
     * @param initialContextFactory
     * @throws LDAPException
     */
    public LDAPJNDIConnection(String providerUrl, String initialContextFactory) throws LDAPException
    {
        this(providerUrl, initialContextFactory, NO_AUTHENTICATION);
    }

    /**
     * @param providerUrl
     * @param initialContextFactory
     * @param authentication
     * @param maxPoolConnections
     * @param initialPoolSizeConnections
     * @param poolTimeout
     * @throws LDAPException
     */
    public LDAPJNDIConnection(String providerUrl,
                              String initialContextFactory,
                              String authentication,
                              int maxPoolConnections,
                              int initialPoolSizeConnections,
                              long poolTimeout) throws LDAPException
    {
        this();
        setProviderUrl(providerUrl);
        setInitialContextFactory(initialContextFactory);
        setAuthentication(authentication);
        setMaxPoolConnections(maxPoolConnections);
        setInitialPoolSizeConnections(initialPoolSizeConnections);
        setPoolTimeout(poolTimeout);
    }

    /**
     * @param providerUrl
     * @param initialContextFactory
     * @param authentication
     * @throws LDAPException
     */
    public LDAPJNDIConnection(String providerUrl, String initialContextFactory, String authentication)
        throws LDAPException
    {
        this(providerUrl, initialContextFactory, authentication, DEFAULT_MAX_POOL_CONNECTIONS,
            DEFAULT_INITIAL_POOL_CONNECTIONS, DEFAULT_POOL_TIMEOUT);
    }

    /**
     * @param conf
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#initialize(leonards.common.conf.Configuration)
     */
    protected void initialize(Map<String, String> conf) throws LDAPException
    {
        if (conf != null)
        {
            extendedEnvironment = new HashMap<String, String>(conf);
            extendedEnvironment.remove(CONNECTION_TYPE_ATTR);
            
            setAuthentication(getConfValue(conf, AUTHENTICATION_ATTR, NO_AUTHENTICATION));
            extendedEnvironment.remove(AUTHENTICATION_ATTR);
            
            setInitialContextFactory(getConfValue(conf, INITIAL_CONTEXT_FACTORY_ATTR, DEFAULT_INITIAL_CONTEXT_FACTORY));
            extendedEnvironment.remove(INITIAL_CONTEXT_FACTORY_ATTR);
            
            setInitialPoolSizeConnections(getConfValue(conf, INITIAL_POOL_CONNECTIONS_ATTR, DEFAULT_INITIAL_POOL_CONNECTIONS));
            extendedEnvironment.remove(INITIAL_POOL_CONNECTIONS_ATTR);

            setMaxPoolConnections(getConfValue(conf, MAX_POOL_CONNECTIONS_ATTR, DEFAULT_MAX_POOL_CONNECTIONS));
            extendedEnvironment.remove(MAX_POOL_CONNECTIONS_ATTR);

            setPoolTimeout(getConfValue(conf, POOL_TIMEOUT_ATTR, DEFAULT_POOL_TIMEOUT));
            extendedEnvironment.remove(POOL_TIMEOUT_ATTR);
            
            setProviderUrl(getConfValue(conf, LDAP_URL_ATTR, null));
            extendedEnvironment.remove(LDAP_URL_ATTR);
            
            setReferral(getConfValue(conf, REFERRAL_ATTR, DEFAULT_REFERRAL));
            extendedEnvironment.remove(REFERRAL_ATTR);
            
        }
    }

    private String getConfValue(Map<String, String> conf, String key, String defaultValue)
    {
        String value = conf.get(key);
        
        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }
    
    private int getConfValue(Map<String, String> conf, String key, int defaultValue)
    {
        String value = conf.get(key);
        
        return StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : defaultValue;        
    }

    private long getConfValue(Map<String, String> conf, String key, long defaultValue)
    {
        String value = conf.get(key);
        
        return StringUtils.isNotEmpty(value) ? Long.parseLong(value) : defaultValue;        
    }
    
    /**
     * @return
     */
    private void logConfiguration(String bindDn, String bindPassword)
    {
        StringBuilder conf = new StringBuilder();

        conf.append("{");
        conf.append("name: " + getName() + ", ");
        conf.append("provider_url: " + getProviderUrl() + ", ");
        conf.append("initial_ctx_factory: " + getInitialContextFactory() + ", ");
        conf.append("auth: " + getAuthentication() + ", ");
        if (!isNoAuthentication() && StringUtils.isNotEmpty(bindDn))
        {
            conf.append("bindDn: " + bindDn + ", ");
        }
        else
        {
            conf.append("bindDn: {anonymous}, ");
        }        
        if (isConnectionPoolEnabled())
        {
            conf.append("init_pool_conns: " + getInitialPoolSizeConnections() + ", ");
            conf.append("max_pool_conns: " + getMaxPoolConnections() + ", ");
            conf.append("pool_timeout: " + getPoolTimeout());
        }
        else
        {
            conf.append("pool: disabled");
        }
        if(extendedEnvironment != null && extendedEnvironment.size() > 0)
        {
            conf.append(", extended: " + extendedEnvironment);
        }
        conf.append("}");
        logger.debug(conf.toString());
    }

    /**
     * @return
     */
    public boolean isNoAuthentication()
    {
        return NO_AUTHENTICATION.equalsIgnoreCase(getAuthentication());
    }

    /**
     * @return
     * @see org.mule.module.ldap.api.LDAPConnection#isClosed()
     */
    public boolean isClosed()
    {
        return getConn() == null;
    }

    /**
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#close()
     */
    public void close() throws LDAPException
    {
        if (getConn() != null)
        {
            try
            {
                getConn().close();
                setConn(null);
                logger.info("Connection closed.");
            }
            catch (NamingException nex)
            {
                throw handleNamingException(nex, "Close connection failed.");
            }
        }
    }

    /**
     * @param dn
     * @param password
     * @return
     * @throws LDAPException
     */
    private Hashtable<String, String> buildEnvironment(String dn, String password) throws LDAPException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        
        env.put(Context.REFERRAL, getReferral());
        env.put(Context.SECURITY_AUTHENTICATION, getAuthentication());
        if (!isNoAuthentication())
        {
            env.put(Context.SECURITY_PRINCIPAL, dn);
            env.put(Context.SECURITY_CREDENTIALS, password);
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, getInitialContextFactory());
        env.put(Context.PROVIDER_URL, getProviderUrl());


        if (isConnectionPoolEnabled())
        {
            env.put(POOL_ENABLED_ENV_PARAM, "true");
            
            env.put(AUTHENTICATION_ENV_PARAM, getAuthentication());

            if (getMaxPoolConnections() > 0)
            {
                env.put(MAX_POOL_SIZE_ENV_PARAM, String.valueOf(getMaxPoolConnections()));
            }

            if (getInitialPoolSizeConnections() > 0)
            {
                env.put(INIT_POOL_SIZE_ENV_PARAM, String.valueOf(getInitialPoolSizeConnections()));
            }

            if (getPoolTimeout() > 0)
            {
                env.put(TIME_OUT_ENV_PARAM, String.valueOf(getPoolTimeout()));
            }
        }
        else
        {
            env.put(POOL_ENABLED_ENV_PARAM, "false");
        }
        
        if(extendedEnvironment != null && extendedEnvironment.size() > 0)
        {
            env.putAll(extendedEnvironment);
        }
        
        return env;

    }

    /**
     * 
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#rebind()
     */
    public void rebind() throws LDAPException
    {
        if(isClosed())
        {
            throw new LDAPException("Cannot rebind a close connection. You must first bind.");
        }
        else
        {
            String dn = getBindedUserDn();
            String password = getBindedUserPassword();
            bind(dn, password);
        }
    }
    
    /**
     * @param dn
     * @param password
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#bind(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void bind(String dn, String password) throws LDAPException
    {
        try
        {
            if(!isClosed())
            {
                String currentUrl = (String) getConn().getEnvironment().get(Context.PROVIDER_URL);
                String currentAuth = (String) getConn().getEnvironment().get(Context.SECURITY_AUTHENTICATION);
                String currentDn = getBindedUserDn();
                
                logger.info("Already binded to " + currentUrl + " with " + currentAuth + " authentication as " + (currentDn != null ? currentDn : "anonymous") + ". Closing connection first.");
                
                close();
                
                logger.info("Re-binding to " + getProviderUrl() + " with " + getAuthentication() + " authentication as " + (dn != null ? dn : "anonymous"));
            }
            
            logConfiguration(dn, password);
            setConn(new InitialLdapContext(buildEnvironment(dn, password), null));
            logger.info("Binded to " + getProviderUrl() + " with " + getAuthentication() + " authentication as " + (dn != null ? dn : "anonymous"));

        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Bind failed.");
        }
    }

    private String getBindedUserPassword() throws LDAPException
    {
        try
        {
            return (String) getConn().getEnvironment().get(Context.SECURITY_CREDENTIALS);
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Cannot get binded user password.");
        }
    }
    
    @Override
    public String getBindedUserDn() throws LDAPException
    {
        if(!isClosed())
        {
            try
            {
                return (String) getConn().getEnvironment().get(Context.SECURITY_PRINCIPAL);
            }
            catch (NamingException nex)
            {
                throw handleNamingException(nex, "Cannot get binded user DN.");
            }
        }
        else
        {
            return null;
        }
    }
    
    /**
     * @param baseDn
     * @param filter
     * @param controls
     * @return
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#search(java.lang.String,
     *      java.lang.String, org.mule.module.ldap.api.LDAPSearchControls)
     */
    @Override
    public LDAPResultSet search(String baseDn, String filter, LDAPSearchControls controls)
        throws LDAPException
    {
        return doSearch(baseDn, filter, null, controls);
    }

    /**
     * @param baseDn
     * @param filter
     * @param filterArgs
     * @param controls
     * @return
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#search(java.lang.String,
     *      java.lang.String, java.lang.Object[],
     *      org.mule.module.ldap.api.LDAPSearchControls)
     */
    @Override
    public LDAPResultSet search(String baseDn, String filter, Object[] filterArgs, LDAPSearchControls controls)
        throws LDAPException
    {
        return doSearch(baseDn, filter, filterArgs, controls);
    }

    private LDAPResultSet doSearch(String baseDn, String filter, Object[] filterArgs, LDAPSearchControls controls) throws LDAPException
    {
        LdapContext searchConn = null;
        try
        {
            searchConn = controls.isPagingEnabled() ? getConn().newInstance(LDAPJNDIUtils.buildRequestControls(controls, null)) : getConn();
            
            NamingEnumeration<SearchResult> entries;
            if(filterArgs != null && filterArgs.length > 0)
            {
                entries = searchConn.search(baseDn, filter, filterArgs, LDAPJNDIUtils.buildSearchControls(controls));
            }
            else
            {
                entries = searchConn.search(baseDn, filter, LDAPJNDIUtils.buildSearchControls(controls));
            }
            
            return LDAPResultSetFactory.create(baseDn, filter, filterArgs, searchConn, controls, entries);
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Search failed.");
        }
    }
    
    /**
     * @param dn
     * @return
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#lookup(java.lang.String)
     */
    public LDAPEntry lookup(String dn) throws LDAPException
    {
        try
        {
            return LDAPJNDIUtils.buildEntry(dn, getConn().getAttributes(dn));
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Lookup failed.");
        }
    }

    /**
     * @param dn
     * @param attributes
     * @return
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#lookup(java.lang.String,
     *      java.lang.String[])
     */
    public LDAPEntry lookup(String dn, String[] attributes) throws LDAPException
    {
        try
        {
            return LDAPJNDIUtils.buildEntry(dn, getConn().getAttributes(dn, attributes));
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Lookup failed.");
        }
    }

    /**
     * @param entry
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#addEntry(org.mule.module.ldap.api.LDAPEntry)
     */
    public void addEntry(LDAPEntry entry) throws LDAPException
    {
        try
        {
            getConn().bind(entry.getDn(), null, buildAttributes(entry));
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Add entry failed.");
        }
    }

    
    private LDAPException handleNamingException(NamingException nex, String logMessage)
    {
        logger.error(logMessage, nex);
        
        return LDAPException.create(nex);
    }
    
    /**
     * @param entry
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#updateEntry(org.mule.module.ldap.api.LDAPEntry)
     */
    public void updateEntry(LDAPEntry entry) throws LDAPException
    {
        try
        {
            ModificationItem[] mods = new ModificationItem[entry.getAttributeCount()];
            Iterator<LDAPEntryAttribute> it = entry.attributes();
            for (int i = 0; it.hasNext() && i < mods.length; i++)
            {
                mods[i] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    buildBasicAttribute(((LDAPEntryAttribute) it.next())));
            }
            getConn().modifyAttributes(entry.getDn(), mods);
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Update entry failed.");
        }
    }

    /**
     * @param entry
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#deleteEntry(org.mule.module.ldap.api.LDAPEntry)
     */
    public void deleteEntry(LDAPEntry entry) throws LDAPException
    {
        deleteEntry(entry.getDn());
    }

    /**
     * @param dn
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#deleteEntry(java.lang.String)
     */
    public void deleteEntry(String dn) throws LDAPException
    {
        try
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("About to delete entry " + dn );
            } 
            
            getConn().unbind(dn);
            
            if(logger.isInfoEnabled())
            {
                logger.info("Deleted entry " + dn);
            }             
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Delete entry failed.");
        }
    }

    /**
     * 
     * @param oldDn
     * @param newDn
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#renameEntry(java.lang.String, java.lang.String)
     */
    public void renameEntry(String oldDn, String newDn) throws LDAPException
    {
        try
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("About to rename entry " + oldDn + " to " + newDn);
            }
            
            getConn().rename(oldDn, newDn);
            
            if(logger.isInfoEnabled())
            {
                logger.info("Renamed entry " + oldDn + " to " + newDn);
            }            
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Rename entry failed.");
        }
    }
    
    /**
     * @param dn
     * @param attribute
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#addAttribute(java.lang.String,
     *      org.mule.module.ldap.api.LDAPEntryAttribute)
     */
    public void addAttribute(String dn, LDAPEntryAttribute attribute) throws LDAPException
    {
        try
        {
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, buildBasicAttribute(attribute));
            getConn().modifyAttributes(dn, mods);
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Add attribute failed.");
        }
    }

    /**
     * @param dn
     * @param attribute
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#updateAttribute(java.lang.String,
     *      org.mule.module.ldap.api.LDAPEntryAttribute)
     */
    public void updateAttribute(String dn, LDAPEntryAttribute attribute) throws LDAPException
    {

        try
        {
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, buildBasicAttribute(attribute));
            getConn().modifyAttributes(dn, mods);
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Update attribute failed.");
        }
    }

    /**
     * @param dn
     * @param attribute
     * @throws LDAPException
     * @see org.mule.module.ldap.api.LDAPConnection#deleteAttribute(java.lang.String,
     *      org.mule.module.ldap.api.LDAPEntryAttribute)
     */
    public void deleteAttribute(String dn, LDAPEntryAttribute attribute) throws LDAPException
    {
        try
        {
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, buildBasicAttribute(attribute));
            getConn().modifyAttributes(dn, mods);
        }
        catch (NamingException nex)
        {
            throw handleNamingException(nex, "Delete attribute failed.");
        }
    }

    /**
     * @return Returns the authentication.
     */
    public String getAuthentication()
    {
        return authentication;
    }

    /**
     * @param authentication The authentication to set.
     */
    public void setAuthentication(String authentication)
    {
        this.authentication = authentication;
    }

    /**
     * @return Returns the initialPoolSizeConnections.
     */
    public int getInitialPoolSizeConnections()
    {
        return initialPoolSizeConnections;
    }

    /**
     * @param initialPoolSizeConnections The initialPoolSizeConnections to set.
     */
    public void setInitialPoolSizeConnections(int initialPoolSizeConnections)
    {
        this.initialPoolSizeConnections = initialPoolSizeConnections;
    }

    /**
     * @return Returns the maxPoolConnections.
     */
    public int getMaxPoolConnections()
    {
        return maxPoolConnections;
    }

    /**
     * @param maxPoolConnections The maxPoolConnections to set.
     */
    public void setMaxPoolConnections(int maxPoolConnections)
    {
        this.maxPoolConnections = maxPoolConnections;
    }

    /**
     * @return Returns the poolTimeout.
     */
    public long getPoolTimeout()
    {
        return poolTimeout;
    }

    /**
     * @param poolTimeout The poolTimeout to set.
     */
    public void setPoolTimeout(long poolTimeout)
    {
        this.poolTimeout = poolTimeout;
    }

    /**
     * @return Returns the providerUrl.
     */
    public String getProviderUrl()
    {
        return providerUrl;
    }

    /**
     * @param providerUrl The providerUrl to set.
     */
    public void setProviderUrl(String provider)
    {
        this.providerUrl = provider;
    }

    public boolean isConnectionPoolEnabled()
    {
        return getInitialPoolSizeConnections() > 0;
    }

    /**
     * @return Returns the initialContextFactory.
     */
    public String getInitialContextFactory()
    {
        return initialContextFactory;
    }

    /**
     * @param initialContextFactory The initialContextFactory to set.
     */
    public void setInitialContextFactory(String initialContextFactory)
    {
        this.initialContextFactory = initialContextFactory;
    }
    /**
     * @return Returns the conn.
     */
    private LdapContext getConn()
    {
        return conn;
    }

    /**
     * @param conn The conn to set.
     */
    private void setConn(LdapContext conn)
    {
        this.conn = conn;
    }



    /**
     * @param attrs
     * @return
     * @throws LDAPException
     */
    private Attributes buildAttributes(LDAPEntryAttributes attrs) throws LDAPException
    {
        Attributes attributes = new BasicAttributes(IGNORE_CASE);

        for (Iterator<LDAPEntryAttribute> it = attrs.attributes(); it.hasNext();)
        {
            attributes.put(buildBasicAttribute((LDAPEntryAttribute) it.next()));
        }

        return attributes;
    }

    /**
     * @param entry
     * @return
     * @throws LDAPException
     */
    private Attributes buildAttributes(LDAPEntry entry) throws LDAPException
    {
        return buildAttributes(entry.getAttributes());
    }

    /**
     * @param attribute
     * @return
     * @throws LDAPException
     */
    private BasicAttribute buildBasicAttribute(LDAPEntryAttribute attribute) throws LDAPException
    {
        if (attribute != null)
        {
            if (attribute.isMultiValued())
            {
                BasicAttribute basicAttribute = new BasicAttribute(attribute.getName());
                for (Iterator<Object> it = attribute.getValues().iterator(); it.hasNext();)
                {
                    basicAttribute.add(it.next());
                }
                return basicAttribute;
            }
            else
            {
                return new BasicAttribute(attribute.getName(), attribute.getValue());
            }
        }
        else
        {
            return null;
        }
    }

    public String getReferral()
    {
        return referral;
    }

    public void setReferral(String referral)
    {
        this.referral = referral;
    }
}
