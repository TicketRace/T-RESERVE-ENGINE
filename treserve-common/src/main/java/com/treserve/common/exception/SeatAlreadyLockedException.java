package com.treserve.common.exception;

public class SeatAlreadyLockedException extends RuntimeException {
    public SeatAlreadyLockedException(String seatLabel) {
        super("Seat already locked: " + seatLabel);
    }
}
