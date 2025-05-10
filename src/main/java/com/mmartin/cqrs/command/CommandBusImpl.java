package com.mmartin.cqrs.command;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
class CommandBusImpl implements CommandBus {

    private final Instance<CommandHandler<? extends CommandRequest<?>, ?>> handlers;

    private Map<String, CommandHandler<? extends CommandRequest<?>, ?>> handlerMap;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public <R> R send(CommandRequest<R> command) {
        Command annotation = command.getClass().getAnnotation(Command.class);
        return ((CommandHandler<CommandRequest<R>, R>) handlerMap.get(annotation.value())).execute(command);
    }

    @PostConstruct
    void init() {
        this.handlerMap = new HashMap<>();
        handlers.stream().forEach(this::register);
    }

    private <C extends CommandRequest<R>, R> void register(CommandHandler<C, R> handler) {
        final Class<C> clazz = extractCommandType(handler);
        final Command annotation = clazz.getAnnotation(Command.class);

        handlerMap.put(annotation.value(), handler);
    }

    @SuppressWarnings("unchecked")
    private static <C extends CommandRequest<R>, R> Class<C> extractCommandType(CommandHandler<C, R> handler) {
        for (Type intf : handler.getClass().getSuperclass().getGenericInterfaces()) {
            if (intf instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw &&
                    CommandHandler.class.isAssignableFrom(raw)) {
                return (Class<C>) pt.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException("The type of the command could not be inferred");
    }
}
