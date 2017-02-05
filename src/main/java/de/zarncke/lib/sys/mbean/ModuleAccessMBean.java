package de.zarncke.lib.sys.mbean;


public interface ModuleAccessMBean {
	String getHealth();

	String getName();

	String getState();

	double getLoad();

	int getNumberOfErrorReports();

	int getNumberOfErrorMessages();

	int getNumberOfInfoReports();

	int getNumberOfInfoMessages();

	String getLastErrorSummary();

	String getTimeOfNextReset();

	void clearStatus();

	void shutdown();

	void kill();

	void tryRecovery();

	void startOrRestart();
}
