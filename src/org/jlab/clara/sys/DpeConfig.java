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

package org.jlab.clara.sys;

class DpeConfig {

    private final int poolSize;
    private final int maxCores;
    private final long reportPeriod;

    DpeConfig(int poolSize, int maxCores, long reportPeriod) {
        this.poolSize = poolSize;
        this.maxCores = maxCores;
        this.reportPeriod = reportPeriod;
    }

    int poolSize() {
        return poolSize;
    }

    int maxCores() {
        return maxCores;
    }

    long reportPeriod() {
        return reportPeriod;
    }
}
