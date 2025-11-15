package me.nullpoint.mod;

public class Packet {
    private final String data;

    public Packet(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "data='" + data + '\'' +
                '}';
    }
}
