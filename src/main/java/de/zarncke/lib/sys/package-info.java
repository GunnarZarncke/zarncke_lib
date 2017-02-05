/**
 * Provides an infrastructure for representing and monitoring the application/installation/running system.
 * The system consists of {@link de.zarncke.lib.sys.Module}s which have an {@link de.zarncke.lib.sys.Health}-state.
 * All Modules together form an {@link de.zarncke.lib.sys.Installation} which is monitored by
 * {@link de.zarncke.lib.sys.Headquarters}.
 * A {@link de.zarncke.lib.sys.DefaultInstallation} is always {@link de.zarncke.lib.sys.Headquarters#HEADQUARTERS available} but
 * may be {@link de.zarncke.lib.ctx.Context#runWith replaced} by a custom one.
 * Some default Modules exist: {@link de.zarncke.lib.sys.JavaLangModule}, {@link de.zarncke.lib.sys.module.LogModule}.
 * Headquarters plugs into {@link de.zarncke.lib.log.Log} and accept {@link de.zarncke.lib.log.group.GroupingLog}s
 * {@link de.zarncke.lib.log.Report}s via a {@link de.zarncke.lib.log.group.ReportListener}.
 */
package de.zarncke.lib.sys;

