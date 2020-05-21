package ro.pub.cs.systems.eim.practicaltest02.model;

public class Alarm {
    private String hour;
    private String minute;

    public Alarm() {
        this.hour = null;
        this.minute = null;
    }

    public Alarm(String hour, String minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }
}
