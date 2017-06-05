package com.apkscanner.core.installer;

import java.io.IOException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.apkscanner.core.signer.SignatureReport;
import com.apkscanner.data.apkinfo.ApkInfo;
import com.apkscanner.data.apkinfo.CompactApkInfo;
import com.apkscanner.tool.adb.AdbDeviceHelper;
import com.apkscanner.tool.adb.PackageInfo;
import com.apkscanner.tool.adb.PackageManager;
import com.apkscanner.tool.adb.SimpleOutputReceiver;
import com.apkscanner.util.Log;

public class DefaultOptionsFactory {
	private CompactApkInfo apkInfo;
	private SignatureReport signatureReport;

	private boolean hasApkInfo;
	private boolean wasSigned;
	private int minSdkVersion;

	public DefaultOptionsFactory(CompactApkInfo apkInfo, SignatureReport signatureReport) {
		this.apkInfo = apkInfo;
		this.signatureReport = signatureReport;

		hasApkInfo = (apkInfo != null && apkInfo.packageName != null && !apkInfo.packageName.isEmpty());
		wasSigned = signatureReport != null || (apkInfo.certificates != null && apkInfo.certificates.length > 0);
		minSdkVersion = (apkInfo != null && apkInfo.minSdkVersion != null) ? apkInfo.minSdkVersion : 1;
	}

	public OptionsBundle createOptions(IDevice device) {
		OptionsBundle options = new OptionsBundle();
		int blockedFlags = 0;
		int blockedCause = 0;

		if(hasApkInfo) {
			if(!wasSigned) {
				blockedFlags |= OptionsBundle.FLAG_OPT_INSTALL | OptionsBundle.FLAG_OPT_PUSH;
				blockedCause |= OptionsBundle.BLOACKED_COMMON_CAUSE_UNSIGNED;
			} else {
				if(device != null) {
					Log.v("create options for " + device.getName());
					PackageInfo packageInfo = PackageManager.getPackageInfo(device, apkInfo.packageName);
					int apiLevel = device.getApiLevel();
					if(apiLevel < minSdkVersion) {
						blockedFlags |= OptionsBundle.FLAG_OPT_INSTALL | OptionsBundle.FLAG_OPT_PUSH;
						blockedCause |= OptionsBundle.BLOACKED_COMMON_CAUSE_UNSUPPORTED_SDK_LEVEL;
					} else if(signatureReport != null && packageInfo != null) {
						String signature = packageInfo.getSignature();
						if(signature != null) {
							if(!signatureReport.contains("RAWDATA", signature)) {
								blockedFlags |= OptionsBundle.FLAG_OPT_INSTALL;
								blockedCause |= OptionsBundle.BLOACKED_INSTALL_CAUSE_MISMATCH_SIGNED;
								if(!packageInfo.isSystemApp()) {
									blockedFlags |= OptionsBundle.FLAG_OPT_PUSH;
									blockedCause |= OptionsBundle.BLOACKED_PUSH_CAUSE_MISMATCH_SIGNED_NOT_SYSTEM;
								}
							}
						}
					}

					if(!AdbDeviceHelper.isRoot(device)) {
						blockedFlags |= OptionsBundle.FLAG_OPT_PUSH;
						blockedCause |= OptionsBundle.BLOACKED_PUSH_CAUSE_NO_ROOT;
					}

					if(packageInfo != null) {
						String apkPath = null;
						if(packageInfo.isSystemApp()) {
							apkPath = packageInfo.getApkPath();
						} else if(packageInfo.getHiddenSystemPackageValue("pkg") != null) {
							apkPath = packageInfo.getHiddenSystemPackageValue("codePath");
						}

						if(apkPath != null && !apkPath.endsWith(".apk")) {
							SimpleOutputReceiver outputReceiver = new SimpleOutputReceiver();
							try {
								device.executeShellCommand("ls -l " + apkPath, outputReceiver);
							} catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
								e.printStackTrace();
							}
							for(String line: outputReceiver.getOutput()) {
								if(line.isEmpty()) continue;
								String tmp = line.replaceAll(".*\\s(\\S*\\.apk)", "/$1");
								if(!line.equals(tmp)) {
									apkPath += tmp;
									break;
								}
							}
						}

						if(apkPath != null && apkPath.startsWith("/system/") && apkPath.endsWith(".apk")) {
							options.systemPath = apkPath;
							if(apkPath.startsWith("/system/app/")) {
								options.set(OptionsBundle.FLAG_OPT_PUSH_SYSTEM);
							} else if(apkPath.startsWith("/system/priv-app/")) {
								options.set(OptionsBundle.FLAG_OPT_PUSH_PRIVAPP);
							} else {
								options.set(OptionsBundle.FLAG_OPT_PUSH_SYSTEM);
								Log.w("Unknown path : " + apkPath);
							}
						}
					}
				}

				if(apkInfo.activityList == null || apkInfo.activityList.length <= 0) {
					blockedFlags |= OptionsBundle.FLAG_OPT_INSTALL_LAUNCH;
				} else {
					int activityFlag = apkInfo.activityList[0].featureFlag;
					if((activityFlag & ApkInfo.APP_FEATURE_LAUNCHER) != ApkInfo.APP_FEATURE_LAUNCHER) {
						options.unset(OptionsBundle.FLAG_OPT_INSTALL_LAUNCH);
					}
				}
				/*
				int activityOpt = Resource.PROP_LAUNCH_ACTIVITY_OPTION.getInt();
				if(activityOpt == Resource.INT_LAUNCH_ONLY_LAUNCHER_ACTIVITY 
						&& (activityFlag & ApkInfo.APP_FEATURE_LAUNCHER) == ApkInfo.APP_FEATURE_LAUNCHER) {
					options.set(OptionsBundle.FLAG_OPT_INSTALL_LAUNCH, apkInfo.activityList[0].name);
				} else if(activityOpt == Resource.INT_LAUNCH_LAUNCHER_OR_MAIN_ACTIVITY
						&& (activityFlag & (ApkInfo.APP_FEATURE_LAUNCHER | ApkInfo.APP_FEATURE_MAIN)) != 0) {
					options.set(OptionsBundle.FLAG_OPT_INSTALL_LAUNCH, apkInfo.activityList[0].name);
				} else {
					options.unset(OptionsBundle.FLAG_OPT_INSTALL_LAUNCH);
				}
				 */
			}
		}

		options.setBlockedFlags(blockedFlags, blockedCause);

		return options;
	}
}
