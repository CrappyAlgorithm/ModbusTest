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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slave {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final Slave slaveExample = new Slave();

        slaveExample.start();

        Runtime.getRuntime().addShutdownHook(new Thread("modbus-slave-shutdown-hook") {
            @Override
            public void run() {
                slaveExample.stop();
            }
        });

        Thread.sleep(Integer.MAX_VALUE);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModbusTcpSlaveConfig config = new ModbusTcpSlaveConfig.Builder().build();
    private final ModbusTcpSlave slave = new ModbusTcpSlave(config);

    public Slave() {}

    public void start() throws ExecutionException, InterruptedException {
        slave.setRequestHandler(new ServiceRequestHandler() {
            @Override
            public void onReadHoldingRegisters(ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse> service) {
//                String clientRemoteAddress = service.getChannel().remoteAddress().toString();
//                String clientIp = clientRemoteAddress.replaceAll(".*/(.*):.*", "$1");
//                String clientPort = clientRemoteAddress.replaceAll(".*:(.*)", "$1");

                ReadHoldingRegistersRequest request = service.getRequest();

                ByteBuf registers = PooledByteBufAllocator.DEFAULT.buffer(request.getQuantity());

                for (int i = 0; i < request.getQuantity(); i++) {
                    logger.info("Write to Register " + i + ": " + Integer.toString(i+1));
                    registers.writeShort(i+1);
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