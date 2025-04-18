package JavaBot;

import java.time.LocalDateTime;

public class Task {
    private int TASK_ID;
    private long USER_ID;
    private String DESC;
    private LocalDateTime DEADLINE;
    private String CATEGORY;
    private boolean IS_COMPLETED;

    public Task(int TASK_ID, long USER_ID, String DESC, LocalDateTime DEADLINE, String CATEGORY, boolean IS_COMPLETED) {
        this.TASK_ID = TASK_ID;
        this.USER_ID = USER_ID;
        this.DESC = DESC;
        this.DEADLINE = DEADLINE;
        this.CATEGORY = CATEGORY;
        this.IS_COMPLETED = IS_COMPLETED;
    }

    public int getTASK_ID() {
        return TASK_ID;
    }

    public long getUSER_ID() {
        return USER_ID;
    }

    public String getDESC() {
        return DESC;
    }

    public LocalDateTime getDEADLINE() {
        return DEADLINE;
    }

    public String getCATEGORY() {
        return CATEGORY;
    }

    public boolean getIS_COMPLETED() {
        return IS_COMPLETED;
    }
}
