package com.mycompany.modbusmaster;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Master {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final List<ModbusTcpMaster> masters = new CopyOnWriteArrayList<>();
    private volatile boolean started = false;

    private final int nMasters;
    private final int nRequests;
    private final int nRegister;

    public Master(int nMasters, int nRequests, int nRegister) {
        this.nMasters = nMasters;
        this.nRequests = nRequests;
        this.nRegister = nRegister;
    }

    public void start() {
        started = true;
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("localhost")
            .setPort(50200)
            .build();

        new Thread(() -> {
            while (started) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double mean = 0.0;
                double oneMinute = 0.0;
                for (ModbusTcpMaster master : masters) {
                    mean += master.getResponseTimer().getMeanRate();
                    oneMinute += master.getResponseTimer().getOneMinuteRate();
                }
                logger.info("Mean rate={}, 1m rate={}", mean, oneMinute);
            }
        }).start();

        for (int i = 0; i < nMasters; i++) {
            ModbusTcpMaster master = new ModbusTcpMaster(config);
            master.connect();
            masters.add(master);
            for (int j = 0; j < nRequests; j++) {
                sendAndReceive(master);
            }
        }
    }

    private void sendAndReceive(ModbusTcpMaster master) {
        if (!started) return;
        CompletableFuture<ReadHoldingRegistersResponse> future =
            master.sendRequest(new ReadHoldingRegistersRequest(0, nRegister), 0);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ByteBuf content = response.content();
                for (int i = 0; i < 10; i++) {
                    logger.info("Register " + i + ": " + Integer.toString(content.getShort(i*2)));
                }
                ReferenceCountUtil.release(response); 
            } else {
                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
            }
        }, Modbus.sharedExecutor());
    }

    public void stop() {
        started = false;
        masters.forEach(ModbusTcpMaster::disconnect);
        masters.clear();
    }

}