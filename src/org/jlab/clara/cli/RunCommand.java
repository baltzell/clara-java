/*
 *   Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved. Permission
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

package org.jlab.clara.cli;

import org.jline.terminal.Terminal;

public class RunCommand extends Command {

    private final RunConfig runConfig;

    public RunCommand(Terminal terminal, RunConfig runConfig) {
        super(terminal, "run", "Start data processing");
        this.runConfig = runConfig;
    }

    @Override
    public void execute(String[] args) {

        if (args.length == 1) {
            terminal.writer().println("Missing arguments.");
        } else if ("local".equals(args[1])) {
            terminal.writer().println("running run local.");
        } else if ("farm".equals(args[1])) {
            terminal.writer().println("running run farm.");
        } else {
            terminal.writer().println("Invalid command: " + args[1]);
        }
    }
}
