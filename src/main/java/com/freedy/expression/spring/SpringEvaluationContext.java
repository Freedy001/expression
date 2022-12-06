package com.freedy.expression.spring;

import com.freedy.expression.stander.StanderEvaluationContext;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Freedy
 * @date 2022/8/9 1:28
 */
public class SpringEvaluationContext extends StanderEvaluationContext {

    private final ApplicationContext app;

    public SpringEvaluationContext(ApplicationContext app, String... packages) {
        super(packages);
        this.app = app;
        putVar("app",app);
    }

    @Override
    public Object getVariable(String name) {
        Object variable = super.getVariable(name);
        if (variable != null) return variable;
        return app.containsBean(filterName(name)) ? app.getBean(filterName(name)) : null;
    }

    @Override
    public boolean containsVariable(String name) {
        return super.containsVariable(name) || app.containsBean(filterName(name));
    }

    @Override
    public Set<String> allVariables() {
        TreeSet<String> set = new TreeSet<>();
        set.addAll(super.allVariables());
        set.addAll(Arrays.asList(app.getBeanDefinitionNames()));
        return set;
    }
}