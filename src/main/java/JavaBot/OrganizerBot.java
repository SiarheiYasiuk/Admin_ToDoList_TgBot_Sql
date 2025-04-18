package JavaBot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizerBot extends TelegramLongPollingBot {
    private final DatabaseHandler dbHandler;

    public OrganizerBot(String botToken, DatabaseHandler dbHandler) {
        super(botToken);
        this.dbHandler = dbHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleCommand(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleCommand(Update update) {
        long userId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            dbHandler.addUser(userId, update.getMessage().getChat().getUserName());

            if (messageText.startsWith("/addtask")) {
                handleAddTask(userId, messageText);
            } else if (messageText.startsWith("/tasks")) {
                showTasksMenu(userId);
            } else {
                sendMessage(userId, """
                        *Команды бота:*
                        /addtask Описание; Категория; Дедлайн (дд.мм.гггг чч:мм); Напоминание (за сколько минут до)
                        /tasks Показать задачи
                        """);
            }

        } catch (Exception e) {
            sendMessage(userId, "Ошибка: " + e.getMessage());
        }
    }

    private void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long userId = update.getCallbackQuery().getFrom().getId();

        if (callbackData.startsWith("view_task_")) {
            int taskId = Integer.parseInt(callbackData.replace("view_task_", ""));
            showTaskActions(userId, taskId);
        } else if (callbackData.startsWith("delete_task_")) {

            int taskId = Integer.parseInt(callbackData.replace("delete_task_", ""));
            System.out.println("delete " + taskId);
            try {
                if (dbHandler.deleteTask(taskId)) {
                    sendMessage(userId, "Задача удалена!");
                }
            } catch (SQLException e) {
                sendMessage(userId, "Ошибка при удалении: "+ e.getMessage());
            }
        }
    }

    private void showTaskActions(long userId, int taskId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InlineKeyboardButton.builder().text("Удалить").callbackData("delete_task_" + taskId).build()));

        keyboardMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(userId);
        message.setText("*Выберите действие:*");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "todolistjfr_bot";
    }

    private void handleAddTask(long userId, String messageText) throws SQLException {
        String[] parts = messageText.split(";");
        if (parts.length < 3) {
            sendMessage(userId, """
                    Неверный формат. Пример: 
                    `/addtask Название задачи; Категория задачи; Дата задачи в формате: ДД.ММ.ГГГГ ЧЧ:ММ; Время в минутах за сколько напомнить о задаче`
                    `/addtask Купить молоко; Еда; 31.12.2025 18:00; 30`
                    """);
            return;
        }

        String description = parts[0].replace("/addtask", "").trim();
        String category = parts[1].trim();
        String deadlineStr = parts[2].trim();
        int reminderMinutes = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 60;

        try {
            LocalDateTime deadLine = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            int taskId = dbHandler.addTask(userId, description, category, deadLine, reminderMinutes);
            sendMessage(userId, "Задача добавлена.\nID: " + taskId);
        } catch (DateTimeParseException e) {
            sendMessage(userId, "Неверный формат даты. Используйте `дд.мм.гггг чч:мм`");
        }
    }

    private void showTasksMenu(long userId) throws SQLException {
        List<Task> tasks = dbHandler.getUserTasks(userId);
        if (tasks.isEmpty()) {
            sendMessage(userId, "У вас нет задач!");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(userId);
        message.setText("*Ваши задачи:*");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Task task : tasks) {
            String taskText = String.format(
                    "[%s] %s (%s) - %s",
                    task.getIS_COMPLETED() ? "\u2714" : "\u2718",
                    task.getDESC(),
                    task.getCATEGORY(),
                    task.getDEADLINE().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(InlineKeyboardButton.builder().text(taskText).callbackData("view_task_" + task.getTASK_ID()).build());
            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
