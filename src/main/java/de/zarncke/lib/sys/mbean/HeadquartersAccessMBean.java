package de.zarncke.lib.sys.mbean;

public interface HeadquartersAccessMBean {
	String getHealth();

	String getName();

	double getLoad();

	String getState();

	void minimumFor(String callerKey, int minimum);

	void importantFor(String callerKey);

	void discardableFor(String callerKey);

	void infoFor(String callerKey);

	void okFor(String callerKey);

	void warningsFor(String callerKey);

	void errorsFor(String callerKey);

	void failuresFor(String callerKey);

	void refreshModules();
}
