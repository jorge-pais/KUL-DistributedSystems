package be.kuleuven.distributedsystems.cloud.entities;

import java.util.List;

public class LocalTrainsWrapper {
    private List<LocalTrain> trains;

    public List<LocalTrain> getTrains() {
        return trains;
    }

    public void setTrains(List<LocalTrain> trains) {
        this.trains = trains;
    }
}