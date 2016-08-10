package com.apkscanner.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.apkscanner.core.installer.ApkInstaller;
import com.apkscanner.core.installer.ApkInstaller.ApkInstallerListener;
import com.apkscanner.data.apkinfo.ApkInfo;
import com.apkscanner.gui.dialog.install.InstallDlg;
import com.apkscanner.resource.Resource;
import com.apkscanner.tool.adb.AdbDeviceManager;
import com.apkscanner.tool.adb.AdbDeviceManager.DeviceStatus;
import com.apkscanner.tool.adb.AdbPackageManager;
import com.apkscanner.tool.adb.AdbPackageManager.PackageInfo;
import com.apkscanner.tool.adb.AdbWrapper;
import com.apkscanner.util.FileUtil;
import com.apkscanner.util.Log;

public class ApkInstallWizard
{
	public static final int STATUS_INIT = 0;
	public static final int STATUS_DEVICE_SCANNING = 1;
	public static final int STATUS_SELECT_DEVICE = 2;
	public static final int STATUS_PACKAGE_SCANNING = 3;
	public static final int STATUS_CHECK_PACKAGES = 4;
	public static final int STATUS_SET_INSTALL_OPTION = 5;
	public static final int STATUS_INSTALLING = 6;
	public static final int STATUS_COMPLETED = 7;

	// UI components
	private Window wizard;
	private ProgressPanel progressPanel;
	private ContentPanel contentPanel;
	

	private int status;
	private DeviceStatus[] targetDevices;
	private PackageInfo[] installedPackage;
	private ApkInfo apkInfo;
	//private String singInfo;
	
	public class ApkInstallWizardDialog  extends JDialog
	{
		private static final long serialVersionUID = 2018466680871932348L;

		public ApkInstallWizardDialog() {
			dialog_init(null);
		}
		
		public ApkInstallWizardDialog(JFrame owner) {
			super(owner);
			dialog_init(owner);
		}
		
		public ApkInstallWizardDialog(JDialog owner) {
			super(owner);
			dialog_init(owner);
		}
		
		private void dialog_init(Component owner) {
			setTitle(Resource.STR_TITLE_INSTALL_WIZARD.getString());
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			setLocationRelativeTo(owner);
			setResizable(false);
			setModal(false);

			initialize(this);
		}
	}
	
	public class ApkInstallWizardFrame extends JFrame
	{
		private static final long serialVersionUID = -5642057585041759436L;
		
		public ApkInstallWizardFrame() {
			frame_init();
		}

		private void frame_init()
		{
			try {
				if(Resource.PROP_CURRENT_THEME.getData()==null) {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} else {
					UIManager.setLookAndFeel(Resource.PROP_CURRENT_THEME.getData().toString());
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
				e1.printStackTrace();
			}
			
			setTitle(Resource.STR_TITLE_INSTALL_WIZARD.getString());
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setResizable(false);

			initialize(this);
		}
	}
	
	private class ProgressPanel extends JPanel
	{
		private static final long serialVersionUID = 6145481552592676895L;


		public ProgressPanel() {
			super(new BorderLayout());
			
			// add progress image components...

			
			// set status
			setStatus(STATUS_INIT);
		}

		public void setStatus(int status) {
			switch(status) {
			case STATUS_INIT:
			case STATUS_DEVICE_SCANNING:
			case STATUS_SELECT_DEVICE:
			case STATUS_PACKAGE_SCANNING:
			case STATUS_CHECK_PACKAGES:
			case STATUS_SET_INSTALL_OPTION:
			case STATUS_INSTALLING:
			case STATUS_COMPLETED:
			default:
				break;
			}
		}
	}
	
	private class ContentPanel extends JPanel
	{
		private static final long serialVersionUID = -680173960208954055L;

		public static final String CONTENT_INIT = "DEVICE_SCANNING";
		public static final String CONTENT_DEVICE_SCANNING = "DEVICE_SCANNING";
		public static final String CONTENT_SELECT_DEVICE = "SELECT_DEVICE";
		public static final String CONTENT_PACKAGE_SCANNING = "PACKAGE_SCANNING";
		public static final String CONTENT_CHECK_PACKAGES = "CHECK_PACKAGES";
		public static final String CONTENT_SET_INSTALL_OPTION = "SET_INSTALL_OPTION";
		public static final String CONTENT_INSTALLING = "INSTALLING";
		public static final String CONTENT_COMPLETED = "COMPLETED";
		
		public ContentPanel(ActionListener listener) {
			super(new CardLayout());
			
			add(new JPanel(), CONTENT_INIT);
			add(new JPanel(), CONTENT_DEVICE_SCANNING);
			add(new JPanel(), CONTENT_SELECT_DEVICE);
			add(new JPanel(), CONTENT_PACKAGE_SCANNING);
			add(new JPanel(), CONTENT_CHECK_PACKAGES);
			add(new JPanel(), CONTENT_SET_INSTALL_OPTION);
			add(new JPanel(), CONTENT_INSTALLING);
			add(new JPanel(), CONTENT_COMPLETED);

			// set status
			setStatus(STATUS_INIT);
		}
		
		public void setStatus(int status) {
			switch(status) {
			case STATUS_INIT:
				((CardLayout)getLayout()).show(this, CONTENT_INIT);
				break;
			case STATUS_DEVICE_SCANNING:
				((CardLayout)getLayout()).show(this, CONTENT_DEVICE_SCANNING);
				break;
			case STATUS_SELECT_DEVICE:
				((CardLayout)getLayout()).show(this, CONTENT_SELECT_DEVICE);
				break;
			case STATUS_PACKAGE_SCANNING:
				((CardLayout)getLayout()).show(this, CONTENT_PACKAGE_SCANNING);
				break;
			case STATUS_CHECK_PACKAGES:
				((CardLayout)getLayout()).show(this, CONTENT_CHECK_PACKAGES);
				break;
			case STATUS_SET_INSTALL_OPTION:
				((CardLayout)getLayout()).show(this, CONTENT_SET_INSTALL_OPTION);
				break;
			case STATUS_INSTALLING:
				((CardLayout)getLayout()).show(this, CONTENT_INSTALLING);
				break;
			case STATUS_COMPLETED:
				((CardLayout)getLayout()).show(this, CONTENT_COMPLETED);
				break;
			default:
				break;
			}
		}
	}

	public ApkInstallWizard(JFrame owner) {
		if(owner != null)
			wizard = new ApkInstallWizardDialog(owner);
		else 
			wizard = new ApkInstallWizardFrame();
	}
	
	public ApkInstallWizard(JDialog owner) {
		if(owner != null)
			wizard = new ApkInstallWizardDialog(owner);
		else 
			wizard = new ApkInstallWizardFrame();
	}
	
	private void setVisible(boolean visible) {
		if(wizard != null) wizard.setVisible(visible);
	}

	private void initialize(Window window)
	{
		if(window == null) return;

		window.setIconImage(Resource.IMG_APP_ICON.getImageIcon().getImage());
		window.setSize(new Dimension(480,215));
		
		//JPanel panel = new JPanel();
		progressPanel = new ProgressPanel();
		contentPanel = new ContentPanel(new UIEventHandler());
		
		window.add(progressPanel, BorderLayout.NORTH);
		window.add(contentPanel, BorderLayout.CENTER);
		
		//Log.i("initialize() register event handler");
		window.addWindowListener(new UIEventHandler());
		
		// Shortcut key event processing
		KeyboardFocusManager ky=KeyboardFocusManager.getCurrentKeyboardFocusManager();
		ky.addKeyEventDispatcher(new UIEventHandler());
	}
	
	class UIEventHandler implements ActionListener, KeyEventDispatcher, WindowListener
	{
		@Override
		public void windowActivated(WindowEvent arg0) { }

		@Override
		public void windowClosed(WindowEvent arg0) { }

		@Override
		public void windowClosing(WindowEvent arg0) { }

		@Override
		public void windowDeactivated(WindowEvent arg0) { }

		@Override
		public void windowDeiconified(WindowEvent arg0) { }

		@Override
		public void windowIconified(WindowEvent arg0) { }

		@Override
		public void windowOpened(WindowEvent arg0) { }

		@Override
		public boolean dispatchKeyEvent(KeyEvent arg0) {
			return false;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) { }
		
	}
	
	private void changeState(int status) {
		if(this.status == status) return;
		this.status = status;
		progressPanel.setStatus(status);
		contentPanel.setStatus(status);
		
		preExecute(status);
	}
	
	private void preExecute(int status) {
		switch(status) {
		case STATUS_DEVICE_SCANNING:
			new Thread(new Runnable() {
				public void run()
				{
					synchronized(ApkInstallWizard.this) {
						targetDevices = AdbDeviceManager.scanDevices();
						next();
					}
				}
			}).start();
			break;
		case STATUS_PACKAGE_SCANNING:
			new Thread(new Runnable() {
				public void run()
				{
					synchronized(ApkInstallWizard.this) {
						if(targetDevices != null && targetDevices.length > 0) {
							boolean existed = false;
							installedPackage = new PackageInfo[targetDevices.length];
							for(int i = 0; i < targetDevices.length; i++) {
								installedPackage[i] = AdbPackageManager.getPackageInfo(targetDevices[i].name, strPackageName);
								if(installedPackage[i] != null) existed = true;
							}
							if(!existed) {
								installedPackage = null;
							}
						}
						next();
					}
				}
			}).start();
			break;
		case STATUS_INSTALLING:
			new Thread(new Runnable() {
				public void run()
				{
					synchronized(ApkInstallWizard.this) {
						// install
						next();
					}
				}
			}).start();
			break;
		default:
			break;
		}
	}
	
	public void start() {
		if(status != STATUS_INIT) {
			Log.w("No init state : " + status);
			return;
		}
		if(apkInfo == null || apkInfo.filePath == null || 
				!(new File(apkInfo.filePath).isFile())) {
			Log.e("No such apk file...");
			return;
		}
		setVisible(true);
		changeState(STATUS_DEVICE_SCANNING);
	}
	
	private void next() {
		synchronized(this) {
			switch(status) {
			case STATUS_INIT:
				changeState(STATUS_DEVICE_SCANNING);
				break;
			case STATUS_DEVICE_SCANNING:
				if(targetDevices == null || targetDevices.length != 1) {
					changeState(STATUS_SELECT_DEVICE);
					break;
				}
			case STATUS_SELECT_DEVICE:
				if(targetDevices != null) {
					changeState(STATUS_PACKAGE_SCANNING);
				}
				break;
			case STATUS_PACKAGE_SCANNING:
				if(installedPackage != null) {
					changeState(STATUS_CHECK_PACKAGES);
					break;
				}
			case STATUS_CHECK_PACKAGES:
				changeState(STATUS_SET_INSTALL_OPTION);
				break;
			case STATUS_SET_INSTALL_OPTION:
				changeState(STATUS_INSTALLING);
				break;
			case STATUS_INSTALLING:
				changeState(STATUS_COMPLETED);
				break;
			default:
				break;
			}
		}
	}
	
	private void previous() {
		synchronized(this) {
			switch(status) {
			case STATUS_CHECK_PACKAGES:
				changeState(STATUS_SELECT_DEVICE);
				break;
			case STATUS_SET_INSTALL_OPTION:
				changeState(STATUS_CHECK_PACKAGES);
				break;
			default:
				break;
			}
		}
		
	}
	
	public void stop() {
		
	}
	
	public void restart() {
		if(status != STATUS_COMPLETED) return;
		status = STATUS_INIT;
		start();
	}
	
	public void setApkInfo(ApkInfo apkInfo) {
		this.apkInfo = apkInfo;
	}

	public void setApkFile(String apkFilePath) {

	}


	// ----------------------------------------------------------------------------------------
	
	
	
	
	
	
	
	
	
	
	
	//static private JTextArea dialogLogArea;
	//static private JDialog dlgDialog = null;

	//static private JPanel installPanel;
	//static private JPanel uninstallPanel;
	
	static private String strPackageName;
	private static String strSourcePath;
	private static String strLibPath;
	private static String tmpApkPath;
	private static boolean checkPackage;
	private static boolean samePackage;
	static private Thread t;
	
	//window position
	//static private int nPositionX, nPositionY;
	
	public interface InstallButtonStatusListener
	{
		public void SetInstallButtonStatus(Boolean Flag);
		public void OnOpenApk(String path);
	}
	
	public interface InstallDlgFuncListener {
		public void Complete(String str);
		public int ShowQuestion(Runnable runnable, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue);
		public void AddLog(String str);
		public int getResult();
		public void SetResult(int i);
		public int ShowDeviceList(Runnable runnable);
		public void AddCheckList(String name, String t, InstallDlg.CHECKLIST_MODE mode);		
		DeviceStatus getSelectDev();
		public int getValue(String text);
	}
	private static InstallButtonStatusListener Listener;
	private static InstallDlgFuncListener InstallDlgListener;
	
	
	public ApkInstallWizard(Frame owner, Boolean isOnlyInstall, String PackageName, String apkPath, String libPath, 
			final boolean samePackage, final boolean checkPackage, final InstallButtonStatusListener Listener)
	{

		strPackageName = PackageName;
		strSourcePath = apkPath;
		strLibPath = libPath;
		ApkInstallWizard.checkPackage = checkPackage;
		ApkInstallWizard.samePackage = samePackage;
		
		//ShowSetupLogDialog();
		//dialogLogArea.setText("");
		
		ApkInstallWizard.Listener = Listener; 
		
		
		InstallDlg dlg = new InstallDlg(owner, isOnlyInstall);
		ApkInstallWizard.InstallDlgListener = dlg.getInstallDlgFuncListener();
		
		
		
		t = new InstallThread();
		t.start();
	}
	
	static class InstallThread extends Thread {
		
		final ImageIcon Appicon = Resource.IMG_QUESTION.getImageIcon();
        final Object[] options = {Resource.STR_BTN_PUSH.getString(), Resource.STR_BTN_INSTALL.getString(), Resource.STR_BTN_CANCEL.getString()};
        final Object[] checkPackOptions = {Resource.STR_BTN_OPEN.getString(), Resource.STR_BTN_INSTALL.getString(), Resource.STR_BTN_CANCEL.getString()};
        final Object[] checkPackDelOptions = {Resource.STR_BTN_OPEN.getString(), Resource.STR_BTN_INSTALL.getString(), Resource.STR_BTN_DEL.getString(), Resource.STR_BTN_CANCEL.getString()};
        final Object[] yesNoOptions = {Resource.STR_BTN_YES.getString(), Resource.STR_BTN_NO.getString()};
		
		public InstallThread() {
			
		}
		
		private class AdbWrapperObserver implements ApkInstallerListener
		{
			
			private final ImageIcon QuestionAppicon;
			private final ImageIcon WaringAppicon;
			private final ImageIcon SucAppicon;
			
			public AdbWrapperObserver()
			{
				QuestionAppicon = Resource.IMG_QUESTION.getImageIcon();
				WaringAppicon = Resource.IMG_WARNING.getImageIcon();
				SucAppicon = Resource.IMG_SUCCESS.getImageIcon();
			}
			
			@Override
			public void OnMessage(String msg) {
				printlnLog(msg);
			}

			@Override
			public void OnError(int cmdType, String device) {
				if(cmdType == ApkInstallerListener.CMD_PUSH) {
					printlnLog("Failure...");
					//JOptionPane.showMessageDialog(null, "Failure...", "Error",JOptionPane.ERROR_MESSAGE, WaringAppicon);
				} else if(cmdType == ApkInstallerListener.CMD_INSTALL) {
					//JOptionPane.showMessageDialog(null, "Failure...", "Error", JOptionPane.ERROR_MESSAGE, WaringAppicon);
				} else if(cmdType == ApkInstallerListener.CMD_PULL) {
					//JOptionPane.showMessageDialog(null, "Failure...", "Error", JOptionPane.ERROR_MESSAGE, WaringAppicon);
				}
				InstallDlgListener.AddCheckList("Install", "fail" , InstallDlg.CHECKLIST_MODE.ERROR);
				
				ShowQuestion(t, Resource.STR_MSG_FAILURE_INSTALLED.getString(), Resource.STR_LABEL_ERROR.getString(), JOptionPane.ERROR_MESSAGE, JOptionPane.ERROR_MESSAGE, WaringAppicon,
			    		new String[] {Resource.STR_BTN_OK.getString()}, Resource.STR_BTN_OK.getString());
			}

			@Override
			public void OnSuccess(int cmdType, String device) {
				if(cmdType == ApkInstallerListener.CMD_PUSH) {
					final Object[] yesNoOptions = {Resource.STR_BTN_YES.getString(), Resource.STR_BTN_NO.getString()};
					InstallDlgListener.AddCheckList("Push", "Success" , InstallDlg.CHECKLIST_MODE.DONE);
					
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_REBOOT.getString(), "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);
					int reboot = ShowQuestion(t, Resource.STR_MSG_SUCCESS_INSTALLED.getString() + "\n" + Resource.STR_QUESTION_REBOOT_DEVICE.getString(), Resource.STR_LABEL_INFO.getString(), JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE, QuestionAppicon, yesNoOptions, yesNoOptions[1]);
					
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_REBOOT.getString(), (reboot==0)?"true":"false" , InstallDlg.CHECKLIST_MODE.DONE);
					
					if(reboot == 0){
						printlnLog("Wait for reboot...");
						AdbWrapper.reboot(device, null);
						printlnLog("Reboot...");
					}
				} else if(cmdType == ApkInstallerListener.CMD_INSTALL) {
					//JOptionPane.showMessageDialog(null, "Success", "Complete", JOptionPane.INFORMATION_MESSAGE, SucAppicon);
						InstallDlgListener.AddCheckList("Install", "Success" , InstallDlg.CHECKLIST_MODE.DONE);
						ShowQuestion(t, Resource.STR_MSG_SUCCESS_INSTALLED.getString(), Resource.STR_LABEL_INFO.getString(), JOptionPane.INFORMATION_MESSAGE, JOptionPane.INFORMATION_MESSAGE, SucAppicon,
				    		new String[] {Resource.STR_BTN_OK.getString()}, Resource.STR_BTN_OK.getString());
				} else if(cmdType == ApkInstallerListener.CMD_PULL) {
					InstallDlgListener.AddCheckList("Pull success", "Done" , InstallDlg.CHECKLIST_MODE.DONE);					
					if(Listener != null) Listener.OnOpenApk(tmpApkPath);
					InstallDlgListener.AddCheckList("Open APK", "Done" , InstallDlg.CHECKLIST_MODE.ADD);
				} 
			}

			@Override
			public void OnCompleted(int cmdType, String device) {
				Listener.SetInstallButtonStatus(true);
//				ShowQuestion(t, "완료", Resource.STR_LABEL_INFO.getString(), JOptionPane.INFORMATION_MESSAGE, JOptionPane.INFORMATION_MESSAGE, SucAppicon,
//			    		new String[] {Resource.STR_BTN_OK.getString()}, Resource.STR_BTN_OK.getString());
				//installPanel.setVisible(false);
			}
		}
		
		
		private int ShowQuestion(Runnable runnable, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
			Object[] temp = new Object[options.length];
			
			for(int i=0; i<options.length; i++) {
				temp[options.length-1-i] = options[i];
			}		
			@SuppressWarnings("unused")
			int result = InstallDlgListener.ShowQuestion(runnable,message,title,optionType,messageType, icon, temp, initialValue);
			
			if(runnable!=null) {
				synchronized (runnable) {
					try {
						runnable.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return InstallDlgListener.getResult();
		}
		
		private void printlnLog(String msg)
		{
			Log.i(msg);
			if(InstallDlgListener != null) {
				InstallDlgListener.AddLog(msg);
			}
		}
		
		private int showDeviceList(Runnable runnable) {
			
			@SuppressWarnings("unused")
			int result = InstallDlgListener.ShowDeviceList(runnable);
			
			synchronized (runnable) {
				try {
					runnable.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			return InstallDlgListener.getResult();
		}
		
		public void run(){
			try {
				DeviceStatus[] DeviceList;
				
				do {
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_DEVICE.getString(), "", InstallDlg.CHECKLIST_MODE.WATING);
					printlnLog("scan devices...");
					DeviceList = AdbDeviceManager.scanDevices();
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_DEVICE.getString(), "", InstallDlg.CHECKLIST_MODE.DONE);
					
					if(DeviceList.length == 0) {
						printlnLog("Device not found!\nplease check device");
						Listener.SetInstallButtonStatus(true);
						final ImageIcon Appicon = Resource.IMG_WARNING.getImageIcon();
						
						Log.d("show Question");
						
						int n = ShowQuestion(this, Resource.STR_MSG_DEVICE_NOT_FOUND.getString(), Resource.STR_LABEL_WARNING.getString(), JOptionPane.WARNING_MESSAGE, JOptionPane.WARNING_MESSAGE, Appicon,
					    		new String[] {Resource.STR_BTN_REFRESH.getString(), Resource.STR_BTN_CANCEL.getString()}, Resource.STR_BTN_REFRESH.getString());
						
						//InstallDlgListener.AddCheckList(Resource.STR_MSG_DEVICE_NOT_FOUND.getString(), "-", InstallDlg.CHECKLIST_MODE.ERROR);
						
						//int n = InstallDlgListener.getResult();
						Log.d(n+"");
						
						if(n==-1 || n==1) {								
							return;
						}
					} else {
						break;
					}
				} while(true);
				DeviceStatus dev = DeviceList[0];
								
				if(DeviceList.length > 1 || (DeviceList.length == 1 && !dev.status.equals("device"))) {
					//int selectedValue = DeviceListDialog.showDialog();
					//Log.i("Seltected index : " + selectedValue);
					
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_DEVICE.getString() + " List", "", InstallDlg.CHECKLIST_MODE.QEUESTION);
					int selectedValue = showDeviceList(this);
					if(selectedValue == -1) {
						Listener.SetInstallButtonStatus(true);
						
						return;
					}
					dev = InstallDlgListener.getSelectDev();
					
//					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_DEVICE.getString() + " List", dev.name +
//							"(" + dev.device + ")", InstallDlg.CHECKLIST_MODE.DONE);
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_DEVICE.getString()+ " List", dev.name +
							"(" + dev.device + ")", InstallDlg.CHECKLIST_MODE.DONE);
				} else {
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_DEVICE.getString(), dev.name +
							"(" + dev.device + ")", InstallDlg.CHECKLIST_MODE.DONE);
				}

				printlnLog(dev.getSummary());
				
				
				
				boolean alreadyCheak = false;
				printlnLog("getPackageInfo() " + strPackageName);
				PackageInfo pkgInfo = AdbPackageManager.getPackageInfo(dev.name, strPackageName);
				
				if(pkgInfo==null) {
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_VERSION.getString(), "not install", InstallDlg.CHECKLIST_MODE.ADD);
				} else {
					InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_VERSION.getString(), pkgInfo.versionName + "/"+pkgInfo.versionCode , InstallDlg.CHECKLIST_MODE.ADD);
				}
				
				if(checkPackage) {
					alreadyCheak = true;
					if(pkgInfo != null) {
						String strLine = "━━━━━━━━━━━━━━━━━━━━━━\n";
						boolean isDeletePossible = true;
						if(pkgInfo.isSystemApp == true && AdbWrapper.root(dev.name, null) != true) {
							isDeletePossible = false;
						}
						
						InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_ROOT.getString(), ""+isDeletePossible , InstallDlg.CHECKLIST_MODE.ADD);
						
						
						int n;
						if(isDeletePossible) {
							InstallDlgListener.AddCheckList(""+checkPackDelOptions[0] +"/"+ checkPackDelOptions[1]+"/" + checkPackDelOptions[2], "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);							
							n=ShowQuestion(this, Resource.STR_MSG_ALREADY_INSTALLED.getString() + "\n"  +  strLine + pkgInfo + strLine + Resource.STR_QUESTION_OPEN_OR_INSTALL.getString(),
									Resource.STR_LABEL_WARNING.getString(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Appicon, checkPackDelOptions, checkPackDelOptions[3]);
							InstallDlgListener.AddCheckList(""+checkPackDelOptions[n], ""+checkPackDelOptions[n] , InstallDlg.CHECKLIST_MODE.DONE);
							
							
						} else {
							InstallDlgListener.AddCheckList(""+checkPackOptions[0] +"/"+ checkPackOptions[1] , "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);
							n=ShowQuestion(this, Resource.STR_MSG_ALREADY_INSTALLED.getString() + "\n"  +  strLine + pkgInfo + strLine + Resource.STR_QUESTION_OPEN_OR_INSTALL.getString(),
									Resource.STR_LABEL_WARNING.getString(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Appicon, checkPackOptions, checkPackOptions[2]);
							InstallDlgListener.AddCheckList(""+checkPackOptions[n], ""+checkPackOptions[n] , InstallDlg.CHECKLIST_MODE.DONE);
						}
						
						
						Log.i("Seltected index : " + n);
						if(n==-1 || (!isDeletePossible && n==2) || (isDeletePossible && n==3)) {
							Listener.SetInstallButtonStatus(true);
							
							return;
						}
						ApkInstaller apkInstaller = new ApkInstaller(dev.name, new AdbWrapperObserver());
						if(n==0) {
							String tmpPath = "/" + dev.name + pkgInfo.apkPath;
							tmpPath = tmpPath.replaceAll("/", File.separator+File.separator).replaceAll("//", "/");
							tmpPath = FileUtil.makeTempPath(tmpPath)+".apk";
							tmpApkPath = tmpPath; 
							//Log.i(tmpPath);
							InstallDlgListener.AddCheckList("Pull APK", "working" , InstallDlg.CHECKLIST_MODE.WATING);
							apkInstaller.PullApk(pkgInfo.apkPath, tmpPath);							
							return;
						}
						if(n==2) {
							//uninstallPanel.setVisible(true);
							if(pkgInfo.isSystemApp) {
								printlnLog("adb shell rm " + pkgInfo.codePath);
								
								InstallDlgListener.AddCheckList("remove APK", "working" , InstallDlg.CHECKLIST_MODE.WATING);
								apkInstaller.removeApk(pkgInfo.codePath);
								InstallDlgListener.AddCheckList("remove APK", "Done" , InstallDlg.CHECKLIST_MODE.DONE);
								
								InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_REBOOT.getString(), "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);
								final Object[] yesNoOptions = {Resource.STR_BTN_YES.getString(), Resource.STR_BTN_NO.getString()};
								int reboot = ShowQuestion(this, Resource.STR_QUESTION_REBOOT_DEVICE.getString(), Resource.STR_LABEL_INFO.getString(), JOptionPane.YES_NO_OPTION, 
										JOptionPane.QUESTION_MESSAGE, Appicon, yesNoOptions, yesNoOptions[1]);
								InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_REBOOT.getString(), (reboot==0)?"true":"false" , InstallDlg.CHECKLIST_MODE.DONE);
								if(reboot == 0){
									printlnLog("Wait for reboot...");									
									AdbWrapper.reboot(dev.name, null);
									printlnLog("Reboot...");
								}
								
							} else {
								InstallDlgListener.AddCheckList("Uninstall APK", "-" , InstallDlg.CHECKLIST_MODE.WATING);
								printlnLog("adb uninstall " + pkgInfo.pkgName);
								apkInstaller.uninstallApk(pkgInfo.pkgName);
								InstallDlgListener.AddCheckList("Uninstall APK", "Done" , InstallDlg.CHECKLIST_MODE.DONE);
							}
							printlnLog("compleate");
							//uninstallPanel.setVisible(false);
							Listener.SetInstallButtonStatus(true);
							return;
						}
					} else {
						//JOptionPane.showMessageDialog(null, "동일 패키지가 설치되어 있지 않습니다.", "Info", JOptionPane.INFORMATION_MESSAGE, Appicon);
						InstallDlgListener.AddCheckList("Install", "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);
						int n = ShowQuestion(this, Resource.STR_MSG_NO_SUCH_PACKAGE.getString() + "\n" + Resource.STR_QUESTION_CONTINUE_INSTALL.getString(), Resource.STR_LABEL_INFO.getString(), JOptionPane.INFORMATION_MESSAGE, JOptionPane.INFORMATION_MESSAGE, Appicon,
								yesNoOptions, yesNoOptions[1]);
						InstallDlgListener.AddCheckList("Install", (n==0)?"Install":"not install" , InstallDlg.CHECKLIST_MODE.DONE);
						if(n==-1 || n==1) {
							Listener.SetInstallButtonStatus(true);
							
							return;
						}
					}
				}
				if(pkgInfo != null) {
					printlnLog(pkgInfo.toString());
					if(pkgInfo.isSystemApp == true) {
						if(AdbWrapper.root(dev.name, null) == true) {
							printlnLog("adbd is running as root");
							String strLine = "━━━━━━━━━━━━━━━━━━━━━━\n";
							if(!checkPackage)InstallDlgListener.AddCheckList(Resource.STR_TREE_MESSAGE_ROOT.getString(), ""+AdbWrapper.root(dev.name, null) , InstallDlg.CHECKLIST_MODE.ADD);
							
							InstallDlgListener.AddCheckList("" + options[0] +"/"+ options[1], "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);
							int n = ShowQuestion(this, Resource.STR_MSG_ALREADY_INSTALLED.getString() + "\n"  +  strLine + pkgInfo + strLine + Resource.STR_QUESTION_PUSH_OR_INSTALL.getString(),
									Resource.STR_LABEL_WARNING.getString(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Appicon, options, options[1]);
							//Log.i("Seltected index : " + n);
							
							InstallDlgListener.AddCheckList(options[n] + "", (n==0) ?"push":"install", InstallDlg.CHECKLIST_MODE.DONE);
							
							if(n==-1 || n==2) {
								Listener.SetInstallButtonStatus(true);
								
								InstallDlgListener.AddCheckList("Cancel", "cancel", InstallDlg.CHECKLIST_MODE.DONE);
								return;
							} 
							if(n==0) {
								printlnLog("Start push APK");
								//installPanel.setVisible(true);
								InstallDlgListener.AddCheckList("Push", "-" , InstallDlg.CHECKLIST_MODE.WATING);
								new ApkInstaller(dev.name, new AdbWrapperObserver()).PushApk(strSourcePath, pkgInfo.apkPath, strLibPath);
								
								return;
							}
							alreadyCheak = true;
						} else {
							printlnLog("adbd cannot run as root in production builds");
						}
					}
					if(samePackage && !alreadyCheak) {
						String strLine = "━━━━━━━━━━━━━━━━━━━━━━\n";
						InstallDlgListener.AddCheckList("Install", "-" , InstallDlg.CHECKLIST_MODE.QEUESTION);
						int n = ShowQuestion(this, Resource.STR_MSG_ALREADY_INSTALLED.getString() + "\n"  +  strLine + pkgInfo + strLine + Resource.STR_QUESTION_CONTINUE_INSTALL.getString(),
								Resource.STR_LABEL_WARNING.getString(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Appicon, yesNoOptions, yesNoOptions[1]);
						//Log.i("Seltected index : " + n);
						
						InstallDlgListener.AddCheckList("Install", (n==-1 || n==1)?"Cancel":"Install" , InstallDlg.CHECKLIST_MODE.DONE);
						
						if(n==-1 || n==1) {
							Listener.SetInstallButtonStatus(true);
							
							return;
						}
					}
				}
				printlnLog("Start install APK");
				//installPanel.setVisible(true);
				InstallDlgListener.AddCheckList("Install", "Install" , InstallDlg.CHECKLIST_MODE.WATING);
				new ApkInstaller(dev.name, new AdbWrapperObserver()).InstallApk(strSourcePath);				
			} finally {
				//Listener.SetInstallButtonStatus(true);
				InstallDlgListener.Complete("END");				
			}		
		}
	}

	@SuppressWarnings("deprecation")
	static public void StopThead() {
		
		Listener.SetInstallButtonStatus(true);
		t.stop();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	static public void RestartThread() {
		
		Listener.SetInstallButtonStatus(false);
		//t = new InstallThread();
		if(!t.isAlive()){
			t = new InstallThread();
			t.start();
		}
	}
}


