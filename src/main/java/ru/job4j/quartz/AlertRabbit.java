package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

/**
 * Класс демонстрирует действия с периодичностью с помощью библиотеки Quartz
 *
 * @author ARTEM CHERNIKOV
 * @version 1.0
 */
public class AlertRabbit {
    public static void main(String[] args) {
        Properties properties = new Properties();
        try (Connection connection = init(properties)) {
            /* Конфигурирование */
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            /* Передаем ссылку на подключение к БД */
            data.put("connection", connection);
            /* Создание задачи */
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            /* Создание расписания */
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(getInterval(properties))
                    .repeatForever();
            /* Задача выполняется через триггер */
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            /* Загрузка задачи в планировщик */
            scheduler.scheduleJob(job, trigger);
            /* Метод main будет работать 10 секунд */
            Thread.sleep(10000);
            /* Закрываем scheduler */
            scheduler.shutdown();
        } catch (SchedulerException | InterruptedException | SQLException se) {
            se.printStackTrace();
        }
    }

    /**
     * Метод используется для подключения к базе данных
     * Используется метод loadProperties(), для загрузки {@link Properties}
     *
     * @return - возвращает {@link Connection} подключение к БД
     * @throws SQLException - может выбросить {@link SQLException}
     */
    private static Connection init(Properties properties) throws SQLException {
        try {
            loadProperties(properties);
            Class.forName(properties.getProperty("driver-class-name"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(
                properties.getProperty("url"),
                properties.getProperty("username"),
                properties.getProperty("password"));
    }

    /**
     * Метод используется для считывания из файла "rabbit.properties" интервала запуска
     * Используется метод validate(), для валидации интервала запуска
     *
     * @param properties - {@link Properties}
     * @return - возвращает интервал
     */
    private static int getInterval(Properties properties) {
        return intervalValidate(properties.getProperty("rabbit.interval"));
    }

    /**
     * Метод используется для загрузки данных из файла "rabbit.properties"
     *
     * @param properties - {@link Properties}
     */
    private static void loadProperties(Properties properties) {
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод используется для валидации содержимого интервала запуска
     *
     * @param interval - интервал в виде строки
     * @return - возвращает интервал в виде числа
     */
    private static int intervalValidate(String interval) {
        if ("".equals(interval)) {
            throw new IllegalArgumentException("Укажите интервал запуска!");
        }
        int rsl = Integer.parseInt(interval);
        if (rsl < 1) {
            throw new IllegalArgumentException("Интервал запуска должен быть не меньше 1!");
        }
        return rsl;
    }

    /**
     * Вложенный статический класс реализующий интерфейс {@link Job},
     * используется для описания требуемых действий с определенной периодичностью
     */
    public static class Rabbit implements Job {
        public Rabbit() {
            System.out.println(hashCode());
        }

        /**
         * Метод используется для выполнения задания
         * 1) Соединяемся с базой данных
         * 2) Отправляем запрос в базу данных
         *
         * @param context - context (условия выполнения)
         * @throws JobExecutionException - может выбросить {@link JobExecutionException}
         */
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here ...");
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO rabbit (created_date) VALUES(?);")) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
