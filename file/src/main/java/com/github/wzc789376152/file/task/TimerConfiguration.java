package com.github.wzc789376152.file.task;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 后台任务配置及初始化
 */
public abstract class TimerConfiguration {
    static Logger logger = Logger.getLogger(TimerConfiguration.class.getName());
    private ScheduledExecutorService scheduledExecutorService;

    public TimerConfiguration(int startTime, int period, String unit) {
        init(startTime, period, unit);
    }

    public void init(int startTime, int period, String unit) {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
            Runnable runnable = () -> runable();
            int calendarType = Calendar.MONTH;
            boolean is = true;//是否定时清理临时文件夹
            if (unit != null) {
                switch (unit) {
                    case "year":
                        calendarType = Calendar.YEAR;
                        break;
                    case "month":
                        calendarType = Calendar.MONTH;
                        break;
                    case "day":
                        calendarType = Calendar.DATE;
                        break;
                    case "second":
                        calendarType = Calendar.SECOND;
                        break;
                    default:
                        is = false;
                        break;
                }
            } else {
                is = false;
            }
            if (is) {
                Calendar calendar = Calendar.getInstance();
                Date now = calendar.getTime();
                calendar.add(calendarType, period);
                Date time = calendar.getTime();
                long period1 = (time.getTime() - now.getTime()) / 1000;//相隔时间：秒
                long delay = 0;
                if (startTime != -1) {
                    calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, 1);
                    calendar.set(Calendar.HOUR, startTime);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    time = calendar.getTime();
                    delay = (time.getTime() - now.getTime()) / 1000;//相隔时间：秒;开始时间默认早上4点
                }
                //设置后台任务周期
                scheduledExecutorService.scheduleAtFixedRate(runnable, delay, period1, TimeUnit.SECONDS);
            }
        }
    }

    public abstract void runable();
}
