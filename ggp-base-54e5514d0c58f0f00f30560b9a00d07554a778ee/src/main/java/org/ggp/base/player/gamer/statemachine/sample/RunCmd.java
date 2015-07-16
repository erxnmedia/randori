package org.ggp.base.player.gamer.statemachine.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class RunCmd {

    public static void runCmd(String cmd, String arg1, String arg2, String arg3) throws IOException {
	Process process = new ProcessBuilder(cmd, arg1, arg2, arg3).start();
	InputStream is = process.getInputStream();
	InputStreamReader isr = new InputStreamReader(is);
	BufferedReader br = new BufferedReader(isr);
	String line;
	System.out.printf("Output of running %s is:", cmd);
	while ((line = br.readLine()) != null) {
	    System.out.println(line);
	}
    }
}
