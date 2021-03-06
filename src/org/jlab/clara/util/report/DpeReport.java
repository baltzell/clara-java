/*
 *   Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved. Permission
 *   to use, copy, modify, and distribute  this software and its documentation for
 *   educational, research, and not-for-profit purposes, without fee and without a
 *   signed licensing agreement.
 *
 *   IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 *   INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 *   OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 *   BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *   THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *   PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 *   PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 *   MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 *   This software was developed under the United States Government license.
 *   For more information contact author at gurjyan@jlab.org
 *   Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.util.report;

import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.util.EnvUtils;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gurjyan
 * @version 4.x
 */
public class DpeReport extends BaseReport {

    private final String host;
    private final String claraHome;
    private final String session;

    private final int coreCount;
    private int poolSize;
    private final long memorySize;

    private final String aliveData;

    private final Map<String, ContainerReport> containers = new ConcurrentHashMap<>();

    public DpeReport(ClaraBase base, String session) {
        super(base.getName(), EnvUtils.userName(), base.getDescription());

        this.host = name;
        this.claraHome = base.getClaraHome();
        this.session = session;

        this.coreCount = Runtime.getRuntime().availableProcessors();
        this.memorySize = Runtime.getRuntime().maxMemory();

        this.aliveData = aliveData();
    }

    private String aliveData() {
        JSONObject data =  new JSONObject();
        data.put("name", name);
        data.put("n_cores", coreCount);
        data.put("clara_home", claraHome);
        return data.toString();
    }

    public String getHost() {
        return host;
    }

    public String getClaraHome() {
        return claraHome;
    }

    public String getSession() {
        return session;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public double getCpuUsage() {
        return SystemStats.getCpuUsage();
    }

    public long getMemoryUsage() {
        return SystemStats.getMemoryUsage();
    }

    public double getLoad() {
        return SystemStats.getSystemLoad();
    }

    public Collection<ContainerReport> getContainers() {
        return containers.values();
    }

    public ContainerReport addContainer(ContainerReport cr) {
        return containers.putIfAbsent(cr.getName(), cr);
    }

    public ContainerReport removeContainer(ContainerReport cr) {
        return containers.remove(cr.getName());
    }

    public void removeAllContainers() {
        containers.clear();
    }

    public String getAliveData() {
        return aliveData;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}
