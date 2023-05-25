package com.takio.HomerTestBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration // конфигурационный тег
@EnableScheduling // аннотация говорит, что есть методы с автоматическим запуском
@Data
@PropertySource("application.properties") // откуда подтягиваем
public class BotConfig {
    @Value("${bot.name}") // подтягиваем значения из настроек
    String botName;
    @Value("${bot.token}")
    String token;

    @Value("${bot.owner}")
    Long ownerId;
}
