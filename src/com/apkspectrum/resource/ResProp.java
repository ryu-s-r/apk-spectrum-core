package com.apkspectrum.resource;

import java.beans.PropertyChangeListener;

public interface ResProp<T> extends Res<T>
{
	public void set(T data);

	public void addPropertyChangeListener(PropertyChangeListener listener);
	public void removePropertyChangeListener(PropertyChangeListener listener);
}
