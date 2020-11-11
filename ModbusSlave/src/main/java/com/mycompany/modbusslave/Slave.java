package com.mycompany.modbusslave;

import java.util.concurrent.ExecutionException;

import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.slave.ModbusTcpSlave;
import com.digitalpetri.modbus.slave.ModbusTcpSlaveConfig;
import com.digitalpetri.modbus.slave.ServiceRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slave {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Random rand = new Random();
    
    private final ModbusTcpSlaveConfig config = new ModbusTcpSlaveConfig.Builder().build();
    private final ModbusTcpSlave slave = new ModbusTcpSlave(config);

    public Slave() {}

    public void start() throws ExecutionException, InterruptedException {
        slave.setRequestHandler(new ServiceRequestHandler() {
            @Override
            public void onReadHoldingRegisters(ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse> service) {
                String clientRemoteAddress = service.getChannel().remoteAddress().toString();
                logger.info("got request from: " + clientRemoteAddress);
                ReadHoldingRegistersRequest request = service.getRequest();
                ByteBuf registers = PooledByteBufAllocator.DEFAULT.buffer(request.getQuantity());
                for (int i = 0; i < request.getQuantity(); i++) {
                    int value = rand.nextInt(200);
                    logger.info("Write to Register " + i + ": " + Integer.toString(value));
                    registers.writeShort(value);
                }
                service.sendResponse(new ReadHoldingRegistersResponse(registers));
                ReferenceCountUtil.release(request);
            }
        });
        slave.bind("localhost", 50200).get();
    }

    public void stop() {
        slave.shutdown();
    }
}