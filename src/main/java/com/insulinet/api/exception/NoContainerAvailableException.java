package com.insulinet.api.exception;

public class NoContainerAvailableException extends RuntimeException {

    public NoContainerAvailableException() {
        super("Nenhuma caneta/frasco em estoque para essa operacao.");
    }
}
