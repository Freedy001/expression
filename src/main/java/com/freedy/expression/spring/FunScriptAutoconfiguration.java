package com.freedy.expression.spring;

import com.freedy.expression.core.Expression;
import com.freedy.expression.core.PureEvaluationContext;
import com.freedy.expression.entrance.cmd.TerminalExpr;
import com.freedy.expression.entrance.cmd.TerminalHandler;
import com.freedy.expression.jline.LocalJlineTerminal;
import com.freedy.expression.utils.PlaceholderParser;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * @author Freedy
 * @date 2022/8/9 0:58
 */
@Component
@EnableConfigurationProperties(ScriptStarterProperties.class)
public class FunScriptAutoconfiguration implements ApplicationContextAware, SmartLifecycle {
    private final Expression ex = new Expression(new PureEvaluationContext());

    {
        ex.getValue("def log=T(org.slf4j.LoggerFactory).getLogger(T(Class).forName('com.freedy.expression.spring.FunScriptAutoconfiguration'));");
    }

    @Autowired
    private ScriptStarterProperties properties;
    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }


    private boolean start = false;

    @Override
    public void start() {
        if (!properties.isEnableRemote() && !properties.isEnableLocal()) return;
        TerminalExpr expr;
        TerminalHandler handler;
        try {
            SpringEvaluationContext evaluationContext = new SpringEvaluationContext(ctx);
            expr = new TerminalExpr(evaluationContext);
            handler = new TerminalHandler(new LocalJlineTerminal(evaluationContext));
        } catch (Throwable e) {
            e.printStackTrace();
            ex.getValue("log.error('fail to start fun script,do you forget to add jvm options?(--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED)');");
            return;
        }

        if (properties.isEnableLocal()) {
            new Thread(() -> {
                ex.getValue("log.info('start local fun script engine on thread {}-{}', T(Thread).currentThread().getName(), T(Thread).currentThread().getId());");
                try {
                    Thread.sleep(500);
                    handler.collectScript(completeScript -> {
                        expr.eval(completeScript);
                        return true;
                    }, line -> true, "fun@spring> ");
                } catch (InterruptedException ignore) {
                } catch (Throwable e) {
                    e.printStackTrace();
                    ex.getValue("log.error('fail to start fun script,do you forget to add jvm options?(--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED)');");
                }
            }, "local-fun-thread").start();
        }

        if (properties.isEnableRemote()) {
            new Thread(() -> {
                ex.getValue("log.info('start remote fun script engine on thread {}-{}', T(Thread).currentThread().getName(), T(Thread).currentThread().getId());");
                try {
                    expr.eval(new PlaceholderParser("asService#  -p ? -aes ? -auth ?", properties.getPort(), properties.getAesKey(), properties.getAuth()).toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    ex.getValue("log.error('fail to start fun script remote server cause {},please check you config!', e.getMessage());");
                }
            }, "remote-fun-thread").start();
        }
        start = true;
    }


    @Override
    public void stop() {
        //do nothing
    }

    @Override
    public boolean isRunning() {
        return start;
    }
}
