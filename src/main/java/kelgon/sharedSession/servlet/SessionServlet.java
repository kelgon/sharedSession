package kelgon.sharedSession.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

@SuppressWarnings("serial")
public class SessionServlet extends HttpServlet {
	final static Logger logger = LoggerFactory.getLogger(SessionServlet.class);
	
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		String op = request.getParameter("op");
		
		if("set".equals(op)) {
			Map<String, String[]> paramMap = request.getParameterMap();
			for(Entry<String, String[]> e : paramMap.entrySet()) {
				if("op".equals(e.getKey()))
					continue;
				request.getSession().setAttribute(e.getKey(), e.getValue()[0]);
				logger.info("Setting attr {} to {}", e.getKey(), e.getValue());
			}
			out.print("{\"ret\": \"ok\"}");
		} else {
			Enumeration<String> names = request.getSession().getAttributeNames();
			Map<String, String> attrs = new HashMap<String, String>();
			while(names.hasMoreElements()) {
				String name = names.nextElement();
				attrs.put(name, String.valueOf(request.getSession().getAttribute(name)));
			}
			out.print(JSON.toJSONString(attrs));
		}
	}
}
