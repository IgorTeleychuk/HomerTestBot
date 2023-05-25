package com.takio.HomerTestBot.service;

import com.takio.HomerTestBot.config.BotConfig;
import com.takio.HomerTestBot.model.Ads;
import com.takio.HomerTestBot.model.AdsRepository;
import com.takio.HomerTestBot.model.User;
import com.takio.HomerTestBot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
    @Autowired
    private AdsRepository adsRepository;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    static final String YES_BUTTON = "YES_BUTTON";

    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";

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

            if (messageText.contains("/send") && config.getOwnerId() == chatId) { // условие для отправки сообщений всем пользователям и проверка идентификатора владельца бота
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" "))); // используем эмоджипарсер, чтобы работали в том числе смайлики
                var users = userRepository.findAll();
                for (User user: users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {

                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage()); // передаем данные в метод регистрации нового пользователя
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName()); // передаем Id чата и имя пользователя
                        break; // не забываем об этом
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break; // не забываем естественно

                    case "/register":
                        register(chatId);
                        break;

                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognized");
                }
            }
        } else if (update.hasCallbackQuery()) { // проверяем, а что, если в обновлении передалась кнопка под сообщением
            String callBackData = update.getCallbackQuery().getData(); // получаем id кнопки
            long messageId = update.getCallbackQuery().getMessage().getMessageId(); // нужен, чтобы не отправлять новый текст по кнопке, а редактировать в с таром
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals(YES_BUTTON)) { // проверяем кнопку (вообще это простейший пример, должны быть разветвленные методы)
                String text = "You pressed YES button";
                executeEditMessageText(text, chatId, messageId);
            } else if (callBackData.equals(NO_BUTTON)) {
                String text = "You pressed NO button";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void register(long chatId) { // кнопки с сообщениями

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>(); // создаем список списков для кнопок
        List<InlineKeyboardButton> rowInLine = new ArrayList<>(); // список ряда кнопок

        var yesButton = new InlineKeyboardButton(); // создаем кнопку
        yesButton.setText("Yes"); // сетим текст в кнопку
        yesButton.setCallbackData(YES_BUTTON); // идентификатор позволяющий понять какая кнопка была нажата

        var noButton = new InlineKeyboardButton(); // создаем кнопку
        noButton.setText("No"); // сетим текст в кнопку
        noButton.setCallbackData(NO_BUTTON); // идентификатор позволяющий понять какая кнопка была нажата

        rowInLine.add(yesButton);
        rowInLine.add(noButton); // порядок имеет значение

        rowsInline.add(rowInLine); // добавляем списки в списки

        markupInLine.setKeyboard(rowsInline); // сетим клаву
        message.setReplyMarkup(markupInLine); // сетим в ответное письмо

        executeMessage(message);
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) { // проверяем существует ли
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

        executeMessage(message);
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText(); // создаем ответное сообщение
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId); // указываем что должно быть не отправлено, а заменено

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private  void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId)); // странно, но получаем long, а присваиваем для отправки string
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "${cron.scheduler}") // аннотация автоматического запуска метода и условие. * - любое значение (сек, мин, час, день, месяц, год??)
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();

        for (Ads ad: ads) {
            for (User user: users) {
                prepareAndSendMessage(user.getChatId(), ad.getAd());
            }
        }
    }
}
