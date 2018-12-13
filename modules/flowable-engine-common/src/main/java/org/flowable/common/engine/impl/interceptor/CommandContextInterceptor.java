/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.common.engine.impl.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class CommandContextInterceptor extends AbstractCommandInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandContextInterceptor.class);

    protected CommandContextFactory commandContextFactory;
    protected String currentEngineConfigurationKey;
    protected Map<String, AbstractEngineConfiguration> engineConfigurations = new HashMap<>();
    protected Map<String, AbstractServiceConfiguration> serviceConfigurations = new HashMap<>();

    public CommandContextInterceptor() {
    }

    public CommandContextInterceptor(CommandContextFactory commandContextFactory) {
        this.commandContextFactory = commandContextFactory;
    }

    @Override
    public <T> T execute(CommandConfig config, Command<T> command) {
        CommandContext commandContext = Context.getCommandContext();

        boolean contextReused = false;
        AbstractEngineConfiguration previousEngineConfiguration = null;
        
        // We need to check the exception, because the transaction can be in a
        // rollback state, and some other command is being fired to compensate (eg. decrementing job retries)
        if (!config.isContextReusePossible() || commandContext == null || commandContext.getException() != null) {
            // 在这里为当前command创建context
            commandContext = commandContextFactory.createCommandContext(command);
            commandContext.setEngineConfigurations(engineConfigurations);
            
        } else {
            // TODO:: 命令上下文重用？
            LOGGER.debug("Valid context found. Reusing it for the current command '{}'", command.getClass().getCanonicalName());
            contextReused = true;
            commandContext.setReused(true);
            // 因为有可能出现command context重用的情况，而在这里还没有覆盖context当前engine，因此此处记录下的engine是调用当前engine发起命令的那个engine，也就是上一个engine
            //     previousEngine -> currentEngine -> command
            //           |
            // context.currentEngine
            previousEngineConfiguration = commandContext.getCurrentEngineConfiguration();
        }

        try {

            // 标识此命令是由当哪个engine发起的
            // currentEngineConfigurationKey的值是engine在构造拦截器链的时候确定的
            commandContext.setCurrentEngineConfiguration(engineConfigurations.get(currentEngineConfigurationKey));
            // Push on stack
            Context.setCommandContext(commandContext);

            // 执行拦截器链的下一个节点
            return next.execute(config, command);

        } catch (Exception e) {

            commandContext.exception(e);

        } finally {
            try {
                if (!contextReused) {
                    // TODO:: 了解close方法
                    commandContext.close();
                }
            } finally {

                // Pop from stack
                // 命令执行完毕后，移除当前的命令上下文
                Context.removeCommandContext();
                // 将当前engine设置为上一个engine，TODO:: 似乎仅在命令上下文重用的情况下有意义
                commandContext.setCurrentEngineConfiguration(previousEngineConfiguration);
            }
        }

        return null;
    }
    
    public CommandContextFactory getCommandContextFactory() {
        return commandContextFactory;
    }

    public void setCommandContextFactory(CommandContextFactory commandContextFactory) {
        this.commandContextFactory = commandContextFactory;
    }
    
    public String getCurrentEngineConfigurationKey() {
        return currentEngineConfigurationKey;
    }

    public void setCurrentEngineConfigurationKey(String currentEngineConfigurationKey) {
        this.currentEngineConfigurationKey = currentEngineConfigurationKey;
    }

    public Map<String, AbstractEngineConfiguration> getEngineConfigurations() {
        return engineConfigurations;
    }

    public void setEngineConfigurations(Map<String, AbstractEngineConfiguration> engineConfigurations) {
        this.engineConfigurations = engineConfigurations;
    }
    
    public Map<String, AbstractServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

    public void setServiceConfigurations(Map<String, AbstractServiceConfiguration> serviceConfigurations) {
        this.serviceConfigurations = serviceConfigurations;
    }
    
}
