package de.zarncke.lib.time;

import org.joda.time.DateTime;

public interface Clock
{
    long getCurrentTimeMillis();

	DateTime getCurrentDateTime();
}
