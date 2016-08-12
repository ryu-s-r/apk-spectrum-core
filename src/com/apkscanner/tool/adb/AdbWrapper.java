package com.apkscanner.tool.adb;

import java.io.File;

import com.apkscanner.resource.Resource;
import com.apkscanner.util.ConsolCmd;
import com.apkscanner.util.ConsolCmd.ConsoleOutputObserver;
import com.apkscanner.util.Log;

public class AdbWrapper
{
	protected static final String adbCmd = getAdbCmd();
	private static String version;
	
	private ConsoleOutputObserver listener;
	private String device;

	public AdbWrapper(String device, ConsoleOutputObserver listener) {
		this.device = device;
		this.listener = listener;
	}
	
	public void setDevice(String device) {
		this.device = device;
	}
	
	public void setListener(ConsoleOutputObserver listener) {
		this.listener = listener;
	}

	static String getAdbCmd() {
		String cmd = adbCmd;
		if(cmd == null) {
			cmd = Resource.BIN_ADB_LNX.getPath();
			if(cmd.matches("^[A-Z]:.*")) {
				cmd = Resource.BIN_ADB_WIN.getPath();
			}
	
			if(!(new File(cmd)).exists()) {
				Log.e("no such adb tool" + adbCmd);
				cmd = null;
			}
		}
		return cmd;
	}
	
	public String version() {
		return version(listener);
	}

	static public String version(ConsoleOutputObserver listener) {
		if(version == null) {
			String adb = getAdbCmd();
			if(adb == null) return null;
			String[] result = ConsolCmd.exc(new String[] {adb, "version"}, false, listener);
			version = result[0];
		}
		return version;
	}
	
	public boolean startServer() {
		return startServer(listener);
	}
	
	static public boolean startServer(ConsoleOutputObserver listener) {
		if(adbCmd == null) return false;
		String[] result = ConsolCmd.exc(new String[] {adbCmd, "start-server"}, false, listener);
		return result[1].matches(".*daemon started successfully.*");
	}
	
	public void killServer() {
		killServer(listener);
	}
	
	static public void killServer(ConsoleOutputObserver listener) {
		if(adbCmd == null) return;
		ConsolCmd.exc(new String[] {adbCmd, "kill-server"}, false, null);
	}

	public boolean restartServer() {
		return restartServer(listener);
	}

	static public boolean restartServer(ConsoleOutputObserver listener) {
		if(adbCmd == null) return false;
		killServer(listener);
		return startServer(listener);
	}
	
	public String[] devices() {
		return devices(listener);
	}
	
	static public String[] devices(ConsoleOutputObserver listener) {
		if(adbCmd == null) return null;
		return ConsolCmd.exc(new String[] {adbCmd, "devices", "-l"}, false, listener);
	}
	
	public String getProp(String tag) {
		return getProp(device, tag, listener);
	}
	
	static public String getProp(String device, String tag, ConsoleOutputObserver listener) {
		if(adbCmd == null) return null;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "shell", "getprop", tag};
		} else {
			param = new String[] {adbCmd, "-s", device, "shell", "getprop", tag};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		return result[0];
	}
	
	public boolean root() {
		return root(device, listener);
	}

	static public boolean root(String device, ConsoleOutputObserver listener) {
		if(adbCmd == null) return false;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "root"};
		} else {
			param = new String[] {adbCmd, "-s", device, "root"};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		if(result == null || result.length == 0 || !result[0].endsWith("running as root")) {
			return false;
		}
		return true;
	}
	
	public boolean remount() {
		return remount(device, listener);
	}

	static public boolean remount(String device, ConsoleOutputObserver listener) {
		if(adbCmd == null) return false;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "remount"};
		} else {
			param = new String[] {adbCmd, "-s", device, "root"};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		if(result == null || !result[0].endsWith("remount succeeded")) {
			return false;
		}
		return true;
	}

	public String[] shell(String[] param) {
		return shell(device, param, listener);
	}
	
	static public String[] shell(String device, String[] param, ConsoleOutputObserver listener) {
		if(adbCmd == null) return null;
		String[] cmd;
		if(device == null || device.isEmpty()) {
			cmd = new String[] {adbCmd, "shell"};
		} else {
			cmd = new String[] {adbCmd, "-s", device, "shell"};
		}
		String[] shellcmd = new String[cmd.length + param.length];
		System.arraycopy(cmd, 0, shellcmd, 0, cmd.length);
		System.arraycopy(param, 0, shellcmd, cmd.length, param.length);
		String[] result = ConsolCmd.exc(shellcmd, false, null);
		return result;
	}
	
	public void reboot() {
		reboot(device, listener);
	}

	static public void reboot(String device, ConsoleOutputObserver listener) {
		if(adbCmd == null) return;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "reboot"};
		} else {
			param = new String[] {adbCmd, "-s", device, "reboot"};
		}
		ConsolCmd.exc(param, false, null);
	}
	
	public boolean pull(String srcApkPath, String destApkPath) {
		return pull(device, srcApkPath, destApkPath, listener);
	}
	
	static public boolean pull(String device, String srcApkPath, String destApkPath, ConsoleOutputObserver listener) {
		if(adbCmd == null) return false;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "pull", srcApkPath, destApkPath};
		} else {
			param = new String[] {adbCmd, "-s", device, "pull", srcApkPath, destApkPath};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		if(result == null || result.length == 0 || !result[0].endsWith("s)")) {
			return false;
		}
		return true;
	}
	
	public boolean push(String srcApkPath, String destApkPath) {
		return push(device, srcApkPath, destApkPath, listener);
	}
	
	static public boolean push(String device, String srcApkPath, String destApkPath, ConsoleOutputObserver listener) {
		if(adbCmd == null) return false;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "push", srcApkPath, destApkPath};
		} else {
			param = new String[] {adbCmd, "-s", device, "push", srcApkPath, destApkPath};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		if(result == null || !result[0].endsWith("s)")) {
			return false;
		}
		return true;
	}

	public String[] install(String apkPath) {
		return install(device, apkPath, listener);
	}
	
	static public String[] install(String device, String apkPath, ConsoleOutputObserver listener) {
		if(adbCmd == null) return null;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "install", "-r", "-d", apkPath};
		} else {
			param = new String[] {adbCmd, "-s", device, "install", "-r", "-d", apkPath};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		return result;
	}
	
	public String[] uninstall(String packageName) {
		return uninstall(device, packageName, listener);
	}
	
	static public String[] uninstall(String device, String packageName, ConsoleOutputObserver listener) {
		if(adbCmd == null) return null;
		String[] param;
		if(device == null || device.isEmpty()) {
			param = new String[] {adbCmd, "uninstall", packageName};
		} else {
			param = new String[] {adbCmd, "-s", device, "uninstall", packageName};
		}
		String[] result = ConsolCmd.exc(param, false, null);
		return result;
	}
}
