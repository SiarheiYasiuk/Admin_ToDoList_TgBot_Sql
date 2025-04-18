package JavaBot;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderScheduler {
    private final OrganizerBot bot;
    private final DatabaseHandler dbHandler;

    public ReminderScheduler(OrganizerBot bot, DatabaseHandler dbHandler) {
        this.bot = bot;
        this.dbHandler = dbHandler;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    private void checkReminders() {
        try {
            List<Task> tasks = dbHandler.getUpcomingTasks(60);

            for(Task task : tasks) {
                String message = "*Напоминание*\n" +
                        "Задача: "+ task.getDESC() +" \n" +
                        "Дедлайн: " + task.getDEADLINE().format(DateTimeFormatter.ofPattern("dd.MM.yyyy" +
                        " HH:mm"));

                bot.sendMessage(task.getUSER_ID(), message);
                dbHandler.markReminderSent(task.getTASK_ID());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
