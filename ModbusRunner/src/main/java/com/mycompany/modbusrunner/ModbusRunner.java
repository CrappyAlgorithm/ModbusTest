package com.mycompany.modbusrunner;

import java.util.concurrent.ExecutionException;

import com.mycompany.modbusmaster.Master;
import com.mycompany.modbusslave.Slave;

public class ModbusRunner {

    public static final int N_MASTERS = 2;
    public static final int N_REQUESTS = 3;
    public static final int REGISTER_COUNT = 2;
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        new Slave().start();
        new Master(N_MASTERS, N_REQUESTS, REGISTER_COUNT).start();
    }
}
