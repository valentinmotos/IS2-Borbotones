package ar.edu.clinicaweb;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import java.util.UUID;

@Configuration
public class SesionWeb implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler) throws Exception {
                if (req.getSession().getAttribute("csrf")==null) req.getSession().setAttribute("csrf",UUID.randomUUID().toString());
                if (req.getMethod().equals("POST") && !req.getSession().getAttribute("csrf").equals(req.getParameter("csrf"))) {
                    res.sendError(403,"La sesión del formulario no es válida"); return false;
                }
                if (!req.getRequestURI().equals("/login") && req.getSession().getAttribute("usuario")==null) {
                    res.sendRedirect("/login"); return false;
                }
                return true;
            }
        }).excludePathPatterns("/css/**","/vendor/**","/error","/favicon.ico");
    }
}
