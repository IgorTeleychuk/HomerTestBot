package com.takio.HomerTestBot.service;

import com.takio.HomerTestBot.config.BotConfig;
import com.takio.HomerTestBot.model.User;
import com.takio.HomerTestBot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot  { // наследуем из библиотеки телеграм
    final BotConfig config;
    @Autowired
    private UserRepository userRepository;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>(); // создаем меню для бота Класс BotCommand от библиотеки
        listOfCommands.add(new BotCommand("/start", "get a welcome massage"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null)); // добавляем созданное меню в систему
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) { // проверяем, что обновление не пустое
            String messageText = update.getMessage().getText(); // получаем сообщение
            long chatId = update.getMessage().getChatId(); // получаем Id чата

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage()); // передаем данные в метод регистрации нового пользователя
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName()); // передаем Id чата и имя пользователя
                    break; // не забываем об этом
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break; // не забываем естественно

                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        }
    }

    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()) { // проверяем существует ли
            var chatId = msg.getChatId(); // достаем данные
            var chat = msg.getChat();

            User user = new User(); // создаем нового пользователя

            user.setChatId(chatId); // присваиваем значения
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis())); // точка времени создания нового пользователя

            userRepository.save(user);
            log.info("User saved: " + user);

        }
    }

    private void startCommandReceived(long chatId, String name) { // метод ответа на команду start
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:"); // строка с библиотекой смайликов
        // смайлик можно задать shortcode с emojipedia.org
        //String answer = "Hi, " + name + ", nice to meet you!";
        log.info("Replied to user: " + name);
        sendMessage(chatId, answer);

    }

    private void sendMessage(long chatId, String textToSend) { // метод отправки сообщения
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId)); // странно, но получаем long, а присваиваем для отправки string
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(); // создаем виртуальную клавиатуру

        List<KeyboardRow> keyboardRows = new ArrayList<>(); // создаем список рядов кнопок
        KeyboardRow row = new KeyboardRow(); // создаем ряд кнопок
        row.add("weather");
        row.add("get random joke");

        keyboardRows.add(row); // порядок добавления имеет значение

        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows); // сетим ряды

        message.setReplyMarkup(keyboardMarkup); // привязываем клавиатуру к сообщению
        // это пример, и он находится в методе ответа поэтому будет показываться одно и то же всегда
        // чтобы это изменить стоит вынести в отдельный метод с соответствующими нюансами типа передачи сюда готового объекта ReplyKeyboardMarkup



        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

}
