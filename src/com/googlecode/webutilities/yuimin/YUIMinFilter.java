/*
 * Copyright 2010 Rajendra Patil 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package com.googlecode.webutilities.yuimin;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * The <code>YUIMinFilter</code> is implemented as Servlet Filter to enable on the fly minification of JS and CSS resources
 * using YUICompressor. 
 * <p>
 * Using the <code>YUIMinFilter</code> the JS and CSS resources can be minified in realtime by adding this filter on them.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> and <b>yuicompressor-x.y.z.jar</b> (See dependency mentioned below) in your classpath (WEB-INF/lib folder of your webapp). 
 * </p>
 * <p>
 * Declare this filter in your <code>web.xml</code> ( web descriptor file)
 * </p>
 * <pre>
 * ...
 * &lt;filter&gt;
 * 	&lt;filter-name&gt;yuiMinFilter&lt;/filter-name&gt;</b>
 * 	&lt;filter-class&gt;<b>com.googlecode.webutilities.yuimin.YUIMinFilter</b>&lt;/filter-class&gt;
 * 	&lt;!-- All the init params are optional and are equivalent to YUICompressor command line options --&gt; 
 * 	&lt;init-param&gt;
 *		&lt;param-name&gt;lineBreak&lt;/param-name&gt;
 *		&lt;param-value&gt;8000&lt;/param-value&gt;
 * 	&lt;/init-param&gt;
 * &lt;/filter&gt;
 * ...
 * </pre>
 * Map this filter on your JS and CSS resources
 * <pre>
 * ...
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;yuiMinFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;<b>*.js</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.json</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.css</b>&lt;/url-pattern&gt;
 * &lt;/filter-mapping>
 * ...
 * </pre>
 * <p>
 * 	And you are all done! All your JS and CSS files should get minified on the fly.
 * </p> 
 * <h3>Init Parameters</h3>
 * <p>
 * All the init parameters are optional and explained below. 
 * </p>
 * <pre>
 *  <b>lineBreak</b> - equivalent to YUICompressor --line-break. Insert a line break after the specified column number
 *  <b>noMunge</b> - equivalent to YUICompressor --nomunge. Minify only, do not obfuscate. Default false.
 *  <b>preserveSemi</b> - equivalent to YUICompressor --preserve-semi. Preserve all semicolons. Default false. 
 *  <b>disableOptimizations</b> - equivalent to YUICompressor --disable-optimizations. Disable all micro optimizations. Default false. 
 * </pre>
 * <h3>Dependency</h3>
 * <p>The <code>YUIMinFilter</code> depends on servlet-api and YUICompressor jar to be in the classpath.</p>
 * <p><b>servlet-api.jar</b> - Must be already present in your webapp classpath</p> 
 * <p><b>yuicompressor-x.y.z.jar</b> - Download and put appropriate version of this jar in your classpath (in WEB-INF/lib)</p>
 * <h3>Limitations</h3> 
 * <p>Current version of <code>YUIMinFilter</code> <b>does not support charset</b> option.</p>
 * <p>It also currently <b>does not support caching of minified contents.</b> Every time filter runs on the resource, the minificatons process happens even if it was minified earlier.<p>
 * <p> To avoid this problem and as a best practice you should add appropriate expires header on static resources so that browser caches them and doesn't request them again and again.
 * You can use the <code>JSCSSMergeServlet</code> from <code>webutilities.jar</code> to add expires header on JS and CSS. It also helps combines multiple JS or CSS requests in one HTTP request. See <code>JSCSSMergeServlet</code> for details.   
 * </p>
 * @author rpatil
 * @version 1.0
 *
 */
public class YUIMinFilter implements Filter{

	private FilterConfig config;
	
	private String charset = "UTF-8";
	
	private int lineBreak = -1;
	
	private boolean noMunge = false;
	
	private boolean preserveSemi = false;
	
	private boolean disableOptimizations = false;
	
	@Override
	public void destroy() {
		this.config = null;
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest rq = (HttpServletRequest)req;
		HttpServletResponse rs = (HttpServletResponse)resp;
		String url = rq.getRequestURI().toLowerCase();

		if(config != null && url.endsWith(".js") || url.endsWith(".json") || url.endsWith(".css") || config == null){
			PrintWriter out = resp.getWriter();
			CharResponseWrapper wrapper = new CharResponseWrapper(rs);
			//Let the response be generated
			chain.doFilter(req, wrapper);
			StringReader sr = new StringReader(new String(wrapper.toString().getBytes(),this.charset));
			//work on generated response
			if(rq.getRequestURI().endsWith(".js") || rq.getRequestURI().endsWith(".json") || (wrapper.getContentType() != null && (wrapper.getContentType().equals("text/javascript") || wrapper.getContentType().equals("application/json")))) {
				 JavaScriptCompressor compressor =  new JavaScriptCompressor(sr,null);
				 compressor.compress(out, this.lineBreak, !this.noMunge, false, this.preserveSemi, this.disableOptimizations);
			}else if(rq.getRequestURI().endsWith(".css") || (wrapper.getContentType() != null && (wrapper.getContentType().equals("text/css")))) {
				CssCompressor compressor =  new CssCompressor(sr);
				compressor.compress(out, this.lineBreak);
			}else{
				out.write(wrapper.toString());
			}
			out.flush();
			out.close();
		}else{
			chain.doFilter(req, resp);
			return;
		}
	}
	
	@Override
	public void init(FilterConfig config) throws ServletException {
		
		this.config = config;
		
		//this.charset = ifValidString(this.config.getInitParameter("charset"), this.charset);
		this.lineBreak = ifValidNumber(this.config.getInitParameter("lineBreak"), this.lineBreak);
		
		this.noMunge = ifValidBoolean(this.config.getInitParameter("noMunge"), this.noMunge);
		this.preserveSemi = ifValidBoolean(this.config.getInitParameter("preserveSemi"), this.preserveSemi);
		this.disableOptimizations = ifValidBoolean(this.config.getInitParameter("disableOptimizations"), this.disableOptimizations);
		
	}
	
	private int ifValidNumber(String s, int def){
		if(s != null && s.matches("[0-9]+")){
			try{
				return Integer.parseInt(s);
			}catch (Exception e) {
				return def;
			}
		}
		return def;
	}
	private boolean ifValidBoolean(String s, boolean def){
		if(s != null && s.toLowerCase().matches("true|false")){
			try{
				return Boolean.parseBoolean(s);
			}catch (Exception e) {
				return def;
			}
		}
		return def;
	}
}

class CharResponseWrapper extends HttpServletResponseWrapper {
	
	private CharArrayWriter output;
	
	public String toString() {
	   return output.toString();
	}
	
	public CharResponseWrapper(HttpServletResponse response){
	   super(response);
	   output = new CharArrayWriter();
	}
	
	public PrintWriter getWriter(){
	   return new PrintWriter(output);
	}
}
