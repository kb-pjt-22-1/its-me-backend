package site.benepay.config;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext container) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(RootConfig.class);
        container.addListener(new ContextLoaderListener(rootContext));
        container.setAttribute("appName", "BenePay Payment Demo - Spring MVC JSP");

        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.register(WebConfig.class);

        ServletRegistration.Dynamic dispatcher = container.addServlet(
            "dispatcher",
            new DispatcherServlet(webContext)
        );
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");

        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        FilterRegistration.Dynamic filter = container.addFilter("encodingFilter", encodingFilter);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    }
}
