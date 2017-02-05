package de.zarncke.lib.thread;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * This servlet just decorates the name of the currentThread with the current request.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class ThreadNameFilter implements Filter {

	/**
	 * @param request from servlet
	 * @param response from servlet
	 * @param chain from servlet
	 * @throws IOException to servlet
	 * @throws ServletException to servlet
	 */
	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final Thread currentThread = Thread.currentThread();
		final String currentThreadName = currentThread.getName();
		Thread.currentThread().setName(currentThreadName + " " + ((HttpServletRequest) request).getRequestURI());
		try {
			chain.doFilter(request, response);
		} finally {
			Thread.currentThread().setName(currentThreadName);
		}
	}

	@Override
	public void destroy() {
		// nop
	}

	@Override
	public void init(final FilterConfig arg0) throws ServletException {
		// nop
	}
}
