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
package com.googlecode.webutilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * The <code>JSCSSMergeServet</code> is the Http Servlet to combine multiple JS or CSS static resources in one HTTP request. 
 * using YUICompressor. 
 * <p>
 * Using <code>JSCSSMergeServet</code> the multiple JS or CSS resources can grouped together (by adding comma) in one HTTP call.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> in your classpath (WEB-INF/lib folder of your webapp). 
 * </p>
 * <p>
 * Declare this servlet in your <code>web.xml</code> ( web descriptor file)
 * </p>
 * <pre>
 * ...
 * &lt;servlet&gt;
 * 	&lt;servlet-name&gt;JSCSSMergeServet&lt;/servlet-name&gt;</b>
 * 	&lt;servlet-class&gt;<b>com.googlecode.webutilities.JSCSSMergeServet</b>&lt;/servlet-class&gt;
 * 	&lt;!-- This init param is optional and default value is minutes for 7 days in future. To expire in the past use negative value. --&gt; 
 * 	&lt;init-param&gt;
 *		&lt;param-name&gt;expiresMinutes&lt;/param-name&gt;
 *		&lt;param-value&gt;7200&lt;/param-value&gt; &lt;!-- 5 days --&gt;
 * 	&lt;/init-param&gt;
 * &lt;/servlet&gt;
 * ...
 * </pre>
 * Map this servlet to serve your JS and CSS resources
 * <pre>
 * ...
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;JSCSSMergeServet&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;<b>*.js</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.json</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.css</b>&lt;/url-pattern&gt;
 * &lt;/servlet-mapping>
 * ...
 * </pre>
 * <p>
 * 	In your web pages (HTML or JSP files) combine your multiple JS or CSS in one request as shown below.
 * </p>
 * <p>To serve multiple JS files through one HTTP request</p> 
 * <pre>
 * &lt;script language="JavaScript" src="<b>/myapp/js/prototype,controls,dragdrop,myapp.js</b>"&gt;&lt;/script&gt;
 * </pre>
 * <p>To serve multiple CSS files through one HTTP request</p> 
 * <pre>
 * &lt;link rel="StyleSheet" href="<b>/myapp/css/common,calendar,aquaskin.css</b>"/&gt;
 * </pre>
 * <p>
 * Also if you wanted to serve them minified all together then you can add <code>YUIMinFilter</code> on them. See <code>YUIMinFilter</code> from <code>webutilities.jar</code> for details.
 * </p>
 * <h3>Init Parameter</h3>
 * <p>
 * The init parameter is optional and has default value of 7 days. This value is relative from current time. Use negative value to expire early in the past.
 * Ideally you should never be using negative value otherwise you won't be able to <b>take advantage of browser caching for static resources</b>. 
 * </p>
 * <pre>
 *  <b>expiresMinutes</b> - Number of minutes (added to current time) to be set as Expires header 
 * </pre>
 * <h3>Dependency</h3> 
 * <p>Servlet and JSP api (mostly provided by servlet container eg. Tomcat).</p>
 * <p><b>servlet-api.jar</b> - Must be already present in your webapp classpath</p>
 * <h3>Limitations</h3> 
 * <p>
 * The multiple JS or CSS files <b>can be combined together in one request if they are in same parent path</b>. eg. <code><b>/myapp/js/a.js</b></code>, <code><b>/myapp/js/b.js</b></code> and <code><b>/myapp/js/c.js</b></code> 
 * can be combined together as <code><b>/myapp/js/a,b,c.js</b></code>. If they are not in common path then they can not be combined in one request. Same applies for CSS too.  
 * </p>
 * @author rpatil
 * @version 1.0
 *
 */
public class JSCSSMergeServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;
	
	private long expiresMinutes = 7*24*60; //+ or - minutes to be added as expires header from current time. default 7 days
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.expiresMinutes = ifValidNumber(config.getInitParameter("expiresMinutes"), this.expiresMinutes);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String url = req.getRequestURI().toLowerCase().replace(req.getContextPath(), ""); 
		//Split multiple files with comma eg. if URL is http://server/context/js/a,b,c.js then a.js, b.js and c.js have to loaded together
		String path = super.getServletContext().getRealPath(url.substring(0,url.lastIndexOf("/")));
		String file = url.substring(url.lastIndexOf("/")+1);
		
		String[] files = file.split(",");
		for(String f : files){
			if(url.endsWith(".json") && !f.endsWith(".json")){
				f += ".json";
			}
			if(url.endsWith(".js") && !f.endsWith(".js")){
				f += ".js";
			}
			if(url.endsWith(".css") && !f.endsWith(".css")){
				f += ".css";
			}
			String fullPath = path + File.separator + f;
			FileInputStream fis = null;
			PrintWriter out = resp.getWriter();
			resp.addDateHeader("Expires", new Date().getTime() + expiresMinutes*60*1000);
			resp.addDateHeader("Last-Modified", new Date().getTime());
			if(f.endsWith(".json")){
				resp.setContentType("application/json");
			}else if(f.endsWith(".js")){
				resp.setContentType("text/javascript");
			}else if(f.endsWith(".css")){
				resp.setContentType("text/css");
			}
			try{
				fis = new FileInputStream(fullPath);
				int c;
				while((c = fis.read()) != -1){
					out.write(c);
					out.flush();
				}
			}catch (Exception e) {
				
				log("Exception in "+JSCSSMergeServlet.class.getSimpleName()+":" + e);
				
			}finally{
				if(fis != null){
					fis.close();
				}
				if(out != null){
					out.close();
				}
			}
		}
		
	}
	
	private long ifValidNumber(String s, long def){
		if(s != null && s.matches("[0-9]+")){
			try{
				return Long.parseLong(s);
			}catch (Exception e) {
				return def;
			}
		}
		return def;
	}
}
