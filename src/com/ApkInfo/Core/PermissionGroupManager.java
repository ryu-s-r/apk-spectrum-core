package com.ApkInfo.Core;

import java.util.ArrayList;
import java.util.HashMap;

import com.ApkInfo.Resource.Resource;

public class PermissionGroupManager {

	public class PermissionInfo
	{
		public String permission;
		public String permGroup;
		public String label;
		public String desc;
	}
	
	public class PermissionGroup
	{
		public String permGroup;
		public String label;
		public String desc;
		public String icon;
		public String permSummary;
		public ArrayList<PermissionInfo> permList; 
	}
	
	private HashMap<String, PermissionGroup> permGroupMap;

	private MyXPath xmlPermissions;
	private MyXPath xmlPermInfoDefault;
	private MyXPath xmlPermInfoLang;
	
	public PermissionGroupManager(String[] permList)
	{
		String lang = Resource.getLanguage();
		
		System.out.println(getClass().getResource("/values/permissions.xml"));
		System.out.println(getClass().getResource("/values/permissions-info.xml"));
		System.out.println(getClass().getResource("/values/permissions-info-" + lang + ".xml"));

		xmlPermissions = new MyXPath(getClass().getResourceAsStream("/values/permissions.xml"));
		xmlPermInfoDefault = new MyXPath(getClass().getResourceAsStream("/values/permissions-info.xml"));
		if(getClass().getResource("/values/permissions-info-" + lang + ".xml") != null) {
			xmlPermInfoLang = new MyXPath(getClass().getResourceAsStream("/values/permissions-info-" + lang + ".xml"));
		}
		
		permGroupMap = new HashMap<String, PermissionGroup>();
		
		setData(permList);
	}
	
	public HashMap<String, PermissionGroup> getPermGroupMap(){
		return permGroupMap;
	}
	
	public void setData(String[] permList)
	{
		for(String perm: permList) {
			PermissionInfo permInfo = getPermissionInfo(perm);
			if(permInfo.permGroup != null) {
				PermissionGroup g = permGroupMap.get(permInfo.permGroup);
				if(g != null) {
					g.permList.add(permInfo);
					g.permSummary += "\n - " + permInfo.label;
				} else {
					g = getPermissionGroup(permInfo.permGroup);
					g.permList.add(permInfo);
					g.permSummary += "\n - " + permInfo.label;
					permGroupMap.put(permInfo.permGroup, g);
				}
			}
		}
	}
	
	public PermissionInfo getPermissionInfo(String perm)
	{
		PermissionInfo permInfo = new PermissionInfo();
		permInfo.permission = perm;

		if(xmlPermissions != null) {
			MyXPath permXPath = xmlPermissions.getNode("/permissions/permission[@name='" + perm + "']");
			if(permXPath != null) {
				permInfo.permGroup = permXPath.getAttributes("android:permissionGroup");
				permInfo.label = getInfoString(permXPath.getAttributes("android:label"));
				permInfo.desc = getInfoString(permXPath.getAttributes("android:description"));
				if(permInfo.label != null) permInfo.label = permInfo.label.replaceAll("\"", "");
				if(permInfo.desc != null) permInfo.desc = permInfo.desc.replaceAll("\"", "");
			}
		}
		System.out.println(permInfo.permission + ", " + permInfo.permGroup + ", " + permInfo.label + ", " + permInfo.desc);
		return permInfo;
	}
	
	public PermissionGroup getPermissionGroup(String group)
	{
		PermissionGroup permGroup = new PermissionGroup();
		permGroup.permGroup = group;
		permGroup.permList = new ArrayList<PermissionInfo>();

		if(xmlPermissions != null) {
			MyXPath groupXPath = xmlPermissions.getNode("/permissions/permission-group[@name='" +  group + "']");
			if(groupXPath != null) {
				permGroup.icon = getIconPath(groupXPath.getAttributes("android:icon"));
				permGroup.label = getInfoString(groupXPath.getAttributes("android:label"));
				permGroup.desc = getInfoString(groupXPath.getAttributes("android:description"));
				if(permGroup.label != null) permGroup.label = permGroup.label.replaceAll("\"", "");
				if(permGroup.desc != null) permGroup.desc = permGroup.desc.replaceAll("\"", "");
			}
		}
		permGroup.permSummary = "[" + permGroup.label + "] : " + permGroup.desc;
		
		System.out.println(permGroup.icon + ", " + permGroup.permGroup + ", " + permGroup.label + ", " + permGroup.desc);
		return permGroup;
	}
	
	public String getInfoString(String value)
	{
		if(value == null || !value.matches("^@string.*")) {
			return value;
		}
		String name = value.replace("@string/", "");
		
		String result = null;
		if(xmlPermInfoLang != null) {
			MyXPath infoXPath = xmlPermInfoLang.getNode("/permission-info/string[@name='" + name + "']");
			if(infoXPath != null) {
				result = infoXPath.getTextContent();
			}
		}

		if(result == null && xmlPermInfoDefault != null) {
			MyXPath infoXPath = xmlPermInfoDefault.getNode("/permission-info/string[@name='" + name + "']");
			if(infoXPath != null) {
				result = infoXPath.getTextContent();
			}
		}

		return result;
	}
	
	public String getIconPath(String value)
	{
		if(value == null || !value.matches("^@drawable.*")) {
			return value;
		}
		String path = value.replace("@drawable/", "");
		
		if(getClass().getResource("/icons/" + path + ".png") != null) {
			path = getClass().getResource("/icons/" + path + ".png").toString();
		} else {
			//path = getClass().getResource("/icons/perm_group_default.png").toString();
		}
		
		return path;
	}
}