/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.exchange;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class ExchangeDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final String IMAP4_NAME   = "IMAP4";
    private static final String POP3_NAME    = "POP3";
    private static final String MTA_NAME     = "MTA";
    private static final String WEB_NAME     = "Web";

    private static final String[] SERVICES = {
        IMAP4_NAME,
        POP3_NAME,
        MTA_NAME,
    };

    private static final String EXCHANGE_KEY =
        "SOFTWARE\\Microsoft\\Exchange\\Setup";

    static final String EX = "MSExchange";
    private static final String WEBMAIL = EX + " Web Mail";
    private static final String EXCHANGE_IS = EX + "IS";

    private static final Log log =
        LogFactory.getLog(ExchangeDetector.class.getName());

    private boolean isExchangeServiceRunning(String name) {
        if (name.equals(MTA_NAME)) {
            return isWin32ServiceRunning(EX + "MTA");
        }
        return
            isWin32ServiceRunning(name + "Svc") ||
            isWin32ServiceRunning(EX + name); //changed in 2007 
    }

    private ServiceResource createService(String name) {
        String svcName = name;
        if (name.equals(MTA_NAME)) {
            svcName = EX + "MTA";
        } else if (isWin32ServiceRunning(name + "Svc")) {
            svcName = name + "Svc";
        } else if (isWin32ServiceRunning(EX + name)) { //changed in 2007 
            svcName = EX + name;
        } else {
            svcName = EX + name;
        }

        ConfigResponse cfg = new ConfigResponse();
        cfg.setValue("service_name", svcName);

        ServiceResource service = new ServiceResource();
        service.setType(this, name);
        service.setServiceName(name);
        setProductConfig(service, new ConfigResponse());
        setMeasurementConfig(service, new ConfigResponse());
        setControlConfig(service, cfg);
        log.debug("="+svcName+"=> "+service.getProductConfig());
        return service;
    }

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        List servers = new ArrayList();

        String exe, installpath;
        Service exch = null;
        try {
            exch = new Service(EXCHANGE_IS);
            if (exch.getStatus() != Service.SERVICE_RUNNING) {
                log.debug("[getServerResources] service '" + EXCHANGE_IS
                        + "' is not RUNNING (status='"+exch.getStatusString()+"')");
                return null;
            }
            exe = exch.getConfig().getExe().trim();
        } catch (Win32Exception e) {
            log.debug("[getServerResources] Error getting '" + EXCHANGE_IS
                    + "' service information " + e, e);
            return null;
        } finally {
            if (exch != null) {
                exch.close();
            }
        }

        File bin = new File(exe).getParentFile();
        installpath = bin.getParent();
        if (!isInstallTypeVersion(bin.getPath())) {
            log.debug("[getServerResources] exchange on '" + bin
                    + "' is not a " + getTypeInfo().getName());
            return null;
        }

        ServerResource server = createServerResource(installpath);

        RegistryKey key = null;
        try {
            //XXX does not work for 64-bit exchange running 32-bit agent
            key = RegistryKey.LocalMachine.openSubKey(EXCHANGE_KEY);
            ConfigResponse cprops = new ConfigResponse();
            try {
                cprops.setValue("version",
                                key.getStringValue("Services Version"));
                cprops.setValue("build",
                                key.getStringValue("NewestBuild"));
                server.setCustomProperties(cprops);
            } catch (Win32Exception e) {
                log.debug(e,e);
            }
        } catch (Win32Exception e) {
            log.debug(e,e);
        } finally {
            if (key != null) {
                key.close();
            }
        }

        server.setProductConfig();
        server.setMeasurementConfig();
        servers.add(server);
        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        List services = new ArrayList();

        //POP3 + IMAP4 are disabled by default, only report the services
        //if they are enabled and running.
        for (int i=0; i<SERVICES.length; i++) {
            String name = SERVICES[i];
            if (!isExchangeServiceRunning(name)) {
                log.debug(name + " is not running");
                continue;
            }
            else {
                log.debug(name + " is running, adding to inventory");
            }
            services.add(createService(name));
        }

        try {
            String[] web = Pdh.getInstances(WEBMAIL);
            if (web.length != 0) {
                services.add(createService(WEB_NAME));
            } //else not enabled if no counters
        } catch (Win32Exception e) {
            log.debug(e,e);
        }

        return services;
    }
}
