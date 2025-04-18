package JavaBot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseHandler dbHandler = new DatabaseHandler(
                    "jdbc:postgresql://localhost:5432/AdminS_FR16",
                    "postgres",
                    "1111"
            );

            OrganizerBot bot = new OrganizerBot("7956622717:AAFA270McYbhTj6Q-US7QKr64Tnnn6zPq2s", dbHandler);
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);

            ReminderScheduler scheduler = new ReminderScheduler(bot, dbHandler);
            scheduler.start();

            System.out.println("Бот запущен!!!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}