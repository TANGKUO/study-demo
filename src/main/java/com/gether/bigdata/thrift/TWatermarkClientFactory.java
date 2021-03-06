package com.gether.bigdata.thrift;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class TWatermarkClientFactory
        extends BasePooledObjectFactory<Watermark.Client> {
    private String host;
    private int port;
    private boolean keepAlive = true;
    public int TIMEOUT = 10000;

    public TWatermarkClientFactory(String host, int port, boolean keepAlive, int timeout) {
        this.host = host;
        this.port = port;
        this.keepAlive = keepAlive;
        this.TIMEOUT = timeout;
    }

    @Override
    public Watermark.Client create() throws TTransportException {
        TTransport transport = new TFramedTransport(new TSocket(host, port, TIMEOUT));
        // 协议要和服务端一致
        TProtocol protocol = new TBinaryProtocol(transport);
        transport.open();
        Watermark.Client client = new Watermark.Client(protocol);
        return client;
    }


    @Override
    public PooledObject<Watermark.Client> wrap(Watermark.Client client) {
        return new DefaultPooledObject<>(client);
    }


    /**
     * 对象激活(borrowObject时触发）
     *
     * @param pooledObject
     * @throws TTransportException
     */
    @Override
    public void activateObject(PooledObject<Watermark.Client> pooledObject) throws TTransportException {
        if (!pooledObject.getObject().getInputProtocol().getTransport().isOpen()) {
            pooledObject.getObject().getInputProtocol().getTransport().open();
        }
        if (!pooledObject.getObject().getOutputProtocol().getTransport().isOpen()) {
            pooledObject.getObject().getOutputProtocol().getTransport().open();
        }
    }


    /**
     * 校验不通过重新连接或则回收对象
     */
    @Override
    public boolean validateObject(PooledObject<Watermark.Client> p) {
        if (p.getObject() != null) {
            if (p.getObject().getInputProtocol().getTransport().isOpen() && p.getObject().getOutputProtocol().getTransport().isOpen()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 对象钝化(即：从激活状态转入非激活状态，returnObject时触发）
     *
     * @param pooledObject
     * @throws TTransportException
     */
    @Override
    public void passivateObject(PooledObject<Watermark.Client> pooledObject) throws TTransportException {
        if (!keepAlive) {
            pooledObject.getObject().getInputProtocol().getTransport().close();
            pooledObject.getObject().getOutputProtocol().getTransport().close();
        }
    }

    /**
     * 对象销毁(clear时会触发）
     *
     * @param pooledObject
     * @throws TTransportException
     */
    @Override
    public void destroyObject(PooledObject<Watermark.Client> pooledObject) throws TTransportException {
        if (pooledObject.getObject().getInputProtocol().getTransport().isOpen()) {
            pooledObject.getObject().getInputProtocol().getTransport().close();
        }
        if (pooledObject.getObject().getOutputProtocol().getTransport().isOpen()) {
            pooledObject.getObject().getOutputProtocol().getTransport().close();
        }
    }
}