package com.apkscanner.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassFinder {

	//https://dzone.com/articles/get-all-classes-within-package

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public static Class[] getClasses(String packageName)
			throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		ArrayList<Class> classes = new ArrayList<Class>();
		Enumeration<URL> resources = classLoader.getResources(path);
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			if("file".equals(resource.getProtocol())) {
				classes.addAll(findClasses(new File(resource.getFile()), packageName));
			} else if("jar".equals(resource.getProtocol())) {
				classes.addAll(findClasses(resource, packageName));
			} else {
				Log.e("Unknown protocol " + resource);
			}
		}
		return classes.toArray(new Class[classes.size()]);
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("rawtypes")
	static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}

	@SuppressWarnings("rawtypes")
	static List<Class> findClasses(URL jarURL, String packageName) throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();

		String[] jarPath = jarURL.getFile().split("!");
		if(jarPath == null || jarPath.length != 2) {
			Log.e("IllegalArgument " + jarPath);
			return classes;
		}

		String[] classPaths = ZipFileUtil.findFiles(jarPath[0].substring(5), ".class", "^"+jarPath[1].substring(1) + "/.*");
		for(String classPath : classPaths) {
			classes.add(Class.forName(classPath.replace("/", ".").substring(0, classPath.length() - 6)));
		}

		return classes;
	}
}
